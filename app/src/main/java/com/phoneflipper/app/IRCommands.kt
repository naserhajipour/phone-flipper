package com.phoneflipper.app

// Small helper to send IR commands via Bluetooth from the ChatActivity UI
// This file is intentionally minimal; the ChatActivity calls BluetoothService.send with standard commands:
// RECORD_IR_START, PLAY_IR

object IRCommands {
    const val RECORD_START = "RECORD_IR_START"
    const val PLAY = "PLAY_IR"
}
