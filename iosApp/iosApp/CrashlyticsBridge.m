#import "CrashlyticsBridge.h"
@import FirebaseCrashlytics;

void crashlytics_recordError(const char *message, const char *stackTrace) {
    FIRCrashlytics *crashlytics = [FIRCrashlytics crashlytics];

    NSString *msg = [NSString stringWithUTF8String:message];
    NSMutableDictionary *userInfo = [NSMutableDictionary dictionary];
    userInfo[NSLocalizedDescriptionKey] = msg;

    if (stackTrace != NULL) {
        NSString *stack = [NSString stringWithUTF8String:stackTrace];
        userInfo[@"KotlinStackTrace"] = stack;
    }

    NSError *error = [NSError errorWithDomain:@"DropKMP"
                                         code:-1
                                     userInfo:userInfo];
    [crashlytics recordError:error];
}

void crashlytics_log(const char *message) {
    if (message == NULL) return;
    NSString *msg = [NSString stringWithUTF8String:message];
    [[FIRCrashlytics crashlytics] logWithFormat:@"%@", msg];
}

void crashlytics_setCustomKey(const char *key, const char *value) {
    if (key == NULL) return;
    NSString *k = [NSString stringWithUTF8String:key];
    NSString *v = value != NULL ? [NSString stringWithUTF8String:value] : @"";
    [[FIRCrashlytics crashlytics] setCustomValue:v forKey:k];
}

void crashlytics_setUserId(const char *userId) {
    if (userId == NULL) return;
    NSString *uid = [NSString stringWithUTF8String:userId];
    [[FIRCrashlytics crashlytics] setUserID:uid];
}
