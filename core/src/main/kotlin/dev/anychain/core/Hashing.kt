package dev.anychain.core

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal fun Long.toLittleEndianBytes(): ByteArray =
    ByteBuffer.allocate(Long.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(this).array()

internal fun ByteArray.toHexString(): String =
    joinToString(separator = "") { "%02x".format(it) }
