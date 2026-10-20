#!/usr/bin/env python3
"""Merges classes.dex into the resource-only APK produced by aapt2 link.

Keeps entries uncompressed when aapt2 made them uncompressed and stores the
dex uncompressed, which is what modern AGP does and what Android prefers.
"""
import sys
import zipfile


def main(base: str, dex: str, out: str) -> None:
    src = zipfile.ZipFile(base, "r")
    with zipfile.ZipFile(out, "w") as dst:
        for info in src.infolist():
            data = src.read(info.filename)
            if info.filename == "classes.dex":
                continue
            method = info.compress_type
            if method not in (zipfile.ZIP_STORED, zipfile.ZIP_DEFLATED):
                method = zipfile.ZIP_DEFLATED
            zi = zipfile.ZipInfo(info.filename, date_time=(1980, 1, 1, 0, 0, 0))
            zi.compress_type = method
            zi.external_attr = info.external_attr
            dst.writestr(zi, data)
        with open(dex, "rb") as fh:
            dex_data = fh.read()
        zi = zipfile.ZipInfo("classes.dex", date_time=(1980, 1, 1, 0, 0, 0))
        zi.compress_type = zipfile.ZIP_STORED
        dst.writestr(zi, dex_data)
    src.close()


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3])
