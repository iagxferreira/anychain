package dev.anychain.core

sealed class AnychainException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class DatabaseException(cause: Throwable) : AnychainException("database error: ${cause.message}", cause)

class SerializationException(cause: Throwable) : AnychainException("serialization error: ${cause.message}", cause)
