#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

// Forward declarations
@protocol ArkDropSendFilesBubble;
@protocol ArkDropReceiveFilesBubble;
@protocol ArkDropSendFilesSubscriber;
@protocol ArkDropReceiveFilesSubscriber;
@protocol ArkDropSenderFileData;

// Request types
@interface ArkDropSenderProfile : NSObject
@property (nonatomic, strong) NSString *name;
@property (nonatomic, strong, nullable) NSString *avatarB64;
- (instancetype)initWithName:(NSString *)name avatarB64:(nullable NSString *)avatarB64;
@end

@interface ArkDropSenderFile : NSObject
@property (nonatomic, strong) NSString *name;
@property (nonatomic, strong) id<ArkDropSenderFileData> data;
- (instancetype)initWithName:(NSString *)name data:(id<ArkDropSenderFileData>)data;
@end

@interface ArkDropSenderConfig : NSObject
@property (nonatomic, assign) uint64_t chunkSize;
@property (nonatomic, assign) uint64_t parallelStreams;
- (instancetype)initWithChunkSize:(uint64_t)chunkSize parallelStreams:(uint64_t)parallelStreams;
@end

@interface ArkDropSendFilesRequest : NSObject
@property (nonatomic, strong) ArkDropSenderProfile *profile;
@property (nonatomic, strong) NSArray<ArkDropSenderFile *> *files;
@property (nonatomic, strong, nullable) ArkDropSenderConfig *config;
- (instancetype)initWithProfile:(ArkDropSenderProfile *)profile
                          files:(NSArray<ArkDropSenderFile *> *)files
                          config:(nullable ArkDropSenderConfig *)config;
@end

@interface ArkDropReceiverProfile : NSObject
@property (nonatomic, strong) NSString *name;
@property (nonatomic, strong, nullable) NSString *avatarB64;
- (instancetype)initWithName:(NSString *)name avatarB64:(nullable NSString *)avatarB64;
@end

@interface ArkDropReceiverConfig : NSObject
@property (nonatomic, assign) uint64_t chunkSize;
@property (nonatomic, assign) uint64_t parallelStreams;
- (instancetype)initWithChunkSize:(uint64_t)chunkSize parallelStreams:(uint64_t)parallelStreams;
@end

@interface ArkDropReceiveFilesRequest : NSObject
@property (nonatomic, strong) NSString *ticket;
@property (nonatomic, assign) uint8_t confirmation;
@property (nonatomic, strong) ArkDropReceiverProfile *profile;
@property (nonatomic, strong) ArkDropReceiverConfig *config;
- (instancetype)initWithTicket:(NSString *)ticket
                  confirmation:(uint8_t)confirmation
                        profile:(ArkDropReceiverProfile *)profile
                         config:(ArkDropReceiverConfig *)config;
@end

// SenderFileData protocol
@protocol ArkDropSenderFileData <NSObject>
- (uint64_t)len;
- (nullable NSNumber *)read;
- (NSData *)readChunkWithSize:(int32_t)size;
@end

// SendFilesBubble protocol
@protocol ArkDropSendFilesBubble <NSObject>
- (NSString *)getTicket;
- (uint8_t)getConfirmation;
- (void)cancelWithCompletion:(void (^)(NSError * _Nullable))completion;
- (BOOL)isFinished;
- (BOOL)isConnected;
- (NSString *)getCreatedAt;
- (void)subscribeWithSubscriber:(id<ArkDropSendFilesSubscriber>)subscriber;
- (void)unsubscribeWithSubscriber:(id<ArkDropSendFilesSubscriber>)subscriber;
@end

// ReceiveFilesBubble protocol
@protocol ArkDropReceiveFilesBubble <NSObject>
- (void)startWithError:(NSError * _Nullable * _Nullable)error;
- (void)cancel;
- (BOOL)isFinished;
- (BOOL)isCancelled;
- (void)subscribeWithSubscriber:(id<ArkDropReceiveFilesSubscriber>)subscriber;
- (void)unsubscribeWithSubscriber:(id<ArkDropReceiveFilesSubscriber>)subscriber;
@end

// Subscriber protocols
@protocol ArkDropSendFilesSubscriber <NSObject>
- (NSString *)getId;
- (void)logWithMessage:(NSString *)message;
- (void)notifySendingWithName:(NSString *)name sent:(uint64_t)sent remaining:(uint64_t)remaining;
- (void)notifyConnectingWithReceiverName:(NSString *)receiverName receiverAvatarB64:(nullable NSString *)receiverAvatarB64;
@end

@protocol ArkDropReceiveFilesSubscriber <NSObject>
- (NSString *)getId;
- (void)logWithMessage:(NSString *)message;
- (void)notifyReceivingWithFileId:(NSString *)fileId data:(NSData *)data;
- (void)notifyConnectingWithSenderName:(NSString *)senderName
                        senderAvatarB64:(nullable NSString *)senderAvatarB64
                                  files:(NSArray<NSDictionary<NSString *, id> *> *)files;
@end

// Bridge class
@class ArkDropBridge;

@interface ArkDropBridge : NSObject
+ (void)sendFilesWithRequest:(ArkDropSendFilesRequest *)request
                       bubble:(id<ArkDropSendFilesBubble> _Nullable * _Nonnull)bubble
                        error:(NSError * _Nullable * _Nullable)error;

+ (void)receiveFilesWithRequest:(ArkDropReceiveFilesRequest *)request
                          bubble:(id<ArkDropReceiveFilesBubble> _Nullable * _Nonnull)bubble
                           error:(NSError * _Nullable * _Nullable)error;
@end

NS_ASSUME_NONNULL_END
