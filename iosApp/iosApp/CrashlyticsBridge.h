#ifndef CrashlyticsBridge_h
#define CrashlyticsBridge_h

void crashlytics_recordError(const char *message, const char *stackTrace);
void crashlytics_log(const char *message);
void crashlytics_setCustomKey(const char *key, const char *value);
void crashlytics_setUserId(const char *userId);

#endif
