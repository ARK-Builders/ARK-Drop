package dev.arkbuilders.drop.instrumentation

import com.google.firebase.crashlytics.FirebaseCrashlytics

actual class FirebaseReporter {
    private val instance = FirebaseCrashlytics.getInstance()

    actual fun recordError(message: String, throwable: Throwable?) {
        if (throwable != null) {
            instance.recordException(throwable)
        } else {
            instance.recordException(Exception(message))
        }
    }

    actual fun log(message: String) {
        instance.log(message)
    }

    actual fun setCustomKey(key: String, value: String) {
        instance.setCustomKey(key, value)
    }

    actual fun setUserId(userId: String) {
        instance.setUserId(userId)
    }
}
