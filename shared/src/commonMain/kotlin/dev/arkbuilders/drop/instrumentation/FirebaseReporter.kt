package dev.arkbuilders.drop.instrumentation

expect class FirebaseReporter {
    fun recordError(message: String, throwable: Throwable? = null)
    fun log(message: String)
    fun setCustomKey(key: String, value: String)
    fun setUserId(userId: String)
}
