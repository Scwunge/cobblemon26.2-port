package com.cobblemon.mod.util

/**
 * Official Cobblemon bit helpers (`BitUtilitiesKt`).
 * Bit indices are **1-based** (bit 1 = least significant bit).
 */
object BitUtils {
    @JvmStatic
    fun setBitForByte(value: Byte, bitNumber: Int, on: Boolean): Byte {
        require(bitNumber in 1..8) { "bitNumber must be 1..8, got $bitNumber" }
        val mask = (1 shl (bitNumber - 1)).toByte()
        return if (on) {
            (value.toInt() or mask.toInt()).toByte()
        } else {
            (value.toInt() and mask.toInt().inv()).toByte()
        }
    }

    @JvmStatic
    fun getBitForByte(value: Byte, bitNumber: Int): Boolean {
        require(bitNumber in 1..8) { "bitNumber must be 1..8, got $bitNumber" }
        val mask = 1 shl (bitNumber - 1)
        return (value.toInt() and mask) != 0
    }
}
