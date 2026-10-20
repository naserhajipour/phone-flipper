#!/usr/bin/env python3
"""Minimal, dependency-free zipalign implementation.

Rewrites a zip so that every STORED entry starts on a 4-byte boundary
(4096 for shared libraries), which is what Android's zipalign does.
Compressed entries are recompressed with zlib level 9.
"""
import sys
import struct
import time
import zlib
import zipfile


def align_for(name: str) -> int:
    return 4096 if name.endswith(".so") else 4


def recompress(raw: bytes, method: int):
    if method == zipfile.ZIP_STORED:
        return zipfile.ZIP_STORED, raw, len(raw), zlib.crc32(raw) & 0xFFFFFFFF
    co = zlib.compressobj(9, zlib.DEFLATED, -15)
    data = co.compress(raw) + co.flush()
    return zipfile.ZIP_DEFLATED, data, len(raw), zlib.crc32(raw) & 0xFFFFFFFF


def build_extra(pad: int) -> bytes:
    # zipalign-style padding: a single well-formed extra field with id 0xD935
    if pad <= 0:
        return b""
    if pad < 4:
        # pad with an unused field id that carries no payload is impossible;
        # instead grow to 4 bytes (caller re-computes), keeping the zip legal.
        pad = 4
    return struct.pack("<HH", 0xD935, pad - 4) + b"\x00" * (pad - 4)


def main(src: str, dst: str) -> None:
    zin = zipfile.ZipFile(src, "r")
    entries = []
    for info in zin.infolist():
        raw = zin.read(info.filename)
        method, payload, usize, crc = recompress(raw, info.compress_type)
        entries.append({
            "name": info.filename.encode("utf-8"),
            "method": method,
            "payload": payload,
            "usize": usize,
            "crc": crc,
            "date": info.date_time,
            "attr": info.external_attr,
            "align": align_for(info.filename),
        })

    out = bytearray()
    central = bytearray()
    for e in entries:
        offset = len(out)
        name = e["name"]
        pad = (-(offset + 30 + len(name))) % e["align"]
        extra = build_extra(pad)
        # recompute after extra construction
        pad = (-(offset + 30 + len(name) + len(extra))) % e["align"]
        if pad:
            extra = build_extra(pad + max(0, 4 - pad))
        if len(extra) == 0:
            extra = b""
        extra = extra + b"\x00" * (((-(offset + 30 + len(name) + len(extra))) % e["align"]))

        dt = e["date"]
        dos_time = (dt[3] << 11) | (dt[4] << 5) | (dt[5] // 2)
        dos_date = ((dt[0] - 1980) << 9) | (dt[1] << 5) | dt[2]

        local = struct.pack(
            "<IHHHHHIIIHH",
            0x04034B50, 20, 0, e["method"], dos_time, dos_date, e["crc"],
            len(e["payload"]), e["usize"], len(name), len(extra),
        )
        out += local + name + extra + e["payload"]

        central += struct.pack(
            "<IHHHHHHIIIHHHHHII",
            0x02014B50, 20, 20, 0, e["method"], dos_time, dos_date, e["crc"],
            len(e["payload"]), e["usize"], len(name), len(extra), 0, 0, 0,
            e["attr"] >> 16, offset,
        ) + name + extra

    cd_offset = len(out)
    out += central
    out += struct.pack(
        "<IHHHHIIH", 0x06054B50, 0, 0, len(entries), len(entries),
        len(central), cd_offset, 0,
    )
    with open(dst, "wb") as fh:
        fh.write(out)
    zin.close()

    # verify alignment
    check = zipfile.ZipFile(dst)
    bad = 0
    for info in check.infolist():
        if info.compress_type == zipfile.ZIP_STORED:
            header_offset = info.header_offset
            with open(dst, "rb") as fh:
                fh.seek(header_offset)
                head = fh.read(30)
            name_len, extra_len = struct.unpack("<HH", head[26:30])
            data_off = header_offset + 30 + name_len + extra_len
            target = align_for(info.filename)
            if data_off % target != 0:
                bad += 1
                print("misaligned:", info.filename, data_off, target)
    if bad:
        sys.exit("alignment failed for %d entries" % bad)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
