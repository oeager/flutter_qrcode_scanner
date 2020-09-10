//
//  ScanViewController.m
//  Scanner
//
//  Created by 文柏胜 on 2020/9/10.
//

#import <Foundation/Foundation.h>
#import "ScanViewController.h"

@interface ScanViewController()<AVCaptureMetadataOutputObjectsDelegate>
@property (nonatomic, strong) AVCaptureSession *captureSession;
@property (nonatomic, strong) AVCaptureVideoPreviewLayer *videoPreviewLayer;
@end

@implementation ScanViewController{
    UIView* _qrcodeview;
    int64_t _viewId;
    FlutterMethodChannel* _channel;
    NSObject<FlutterPluginRegistrar>* _registrar;
    BOOL isOpenFlash;
    BOOL _isReading;
    AVCaptureDevice *captureDevice;
}


- (instancetype)initWithFrame:(CGRect)frame
               viewIdentifier:(int64_t)viewId
                    arguments:(id _Nullable)args
              binaryRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar
{
    if ([super init]) {
        _registrar = registrar;
        _viewId = viewId;
        NSString *channelName = [NSString stringWithFormat:@"com.qr.scanner.view_%lld", viewId];
        _channel = [FlutterMethodChannel methodChannelWithName:channelName binaryMessenger:registrar.messenger];
        __weak __typeof__(self) weakSelf = self;
        [_channel setMethodCallHandler:^(FlutterMethodCall* call, FlutterResult result) {
            [weakSelf onMethodCall:call result:result];
        }];
        _qrcodeview= [[UIView alloc] initWithFrame:CGRectMake(0, 0, [[UIScreen mainScreen] bounds].size.width, [[UIScreen mainScreen] bounds].size.height)];
        _qrcodeview.opaque = NO;
        _qrcodeview.backgroundColor = [UIColor blackColor];
        isOpenFlash = NO;
        _isReading = NO;
    }
    return self;
}

- (void)onMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result
{
    if ([call.method isEqualToString:@"toggleFlashlight"]) {
        NSNumber* status = call.arguments[@"status"];
        BOOL turnOn =status!=nil&&[status boolValue]==YES;
        if(turnOn==isOpenFlash){
            result([[NSNumber alloc]initWithBool:NO ]);
            return;
        }
        [self setFlashlight];
    }else if ([call.method isEqualToString:@"startPreView"]) {
        [self startReading];
        result([[NSNumber alloc]initWithBool:YES ]);
    } else if ([call.method isEqualToString:@"stopPreView"]) {
        [self stopReading];
        result([[NSNumber alloc]initWithBool:YES ]);
    }else if([call.method isEqualToString:@"startScan"]){
         [self startReading];
        result([[NSNumber alloc]initWithBool:YES ]);
    }else if([call.method isEqualToString:@"stopScan"]){
        [self stopReading];
        result([[NSNumber alloc]initWithBool:YES ]);
    }else{
         result(FlutterMethodNotImplemented);
    }
}

- (nonnull UIView *)view {
    return _qrcodeview;
}

- (BOOL)startReading {
    if (_isReading) return NO;
    _isReading = YES;
    NSError *error;
    _captureSession = [[AVCaptureSession alloc] init];
    captureDevice = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    AVCaptureDeviceInput *input = [AVCaptureDeviceInput deviceInputWithDevice:captureDevice error:&error];
    if (!input) {
        NSLog(@"%@", [error localizedDescription]);
        return NO;
    }
    [_captureSession addInput:input];
    AVCaptureMetadataOutput *captureMetadataOutput = [[AVCaptureMetadataOutput alloc] init];
    [_captureSession addOutput:captureMetadataOutput];
    dispatch_queue_t dispatchQueue;
    dispatchQueue = dispatch_queue_create("myQueue", NULL);
    [captureMetadataOutput setMetadataObjectsDelegate:self queue:dispatchQueue];
    [captureMetadataOutput setMetadataObjectTypes:[NSArray arrayWithObject:AVMetadataObjectTypeQRCode]];
    _videoPreviewLayer = [[AVCaptureVideoPreviewLayer alloc] initWithSession:_captureSession];
    [_videoPreviewLayer setVideoGravity:AVLayerVideoGravityResizeAspectFill];
    [_videoPreviewLayer setFrame:_qrcodeview.layer.bounds];
    [_qrcodeview.layer addSublayer:_videoPreviewLayer];
    [_captureSession startRunning];
    return YES;
}


-(void)captureOutput:(AVCaptureOutput *)captureOutput didOutputMetadataObjects:(NSArray *)metadataObjects fromConnection:(AVCaptureConnection *)connection{
    if (metadataObjects != nil && [metadataObjects count] > 0) {
        AVMetadataMachineReadableCodeObject *metadataObj = [metadataObjects objectAtIndex:0];
        if ([[metadataObj type] isEqualToString:AVMetadataObjectTypeQRCode]) {
            NSMutableDictionary *dic = [[NSMutableDictionary alloc] init];
            [dic setObject:[metadataObj stringValue] forKey:@"text"];
            [dic setObject:[NSNumber numberWithInt:1] forKey:@"status"];
            [_channel invokeMethod:@"onResult" arguments:dic];
            [self performSelectorOnMainThread:@selector(stopReading) withObject:nil waitUntilDone:NO];
            _isReading = NO;
        }
    }
}


-(void)stopReading{
    [_captureSession stopRunning];
    _captureSession = nil;
    [_videoPreviewLayer removeFromSuperlayer];
    _isReading = NO;
}

// 手电筒开关
- (void) setFlashlight
{
    [captureDevice lockForConfiguration:nil];
    if (isOpenFlash == NO) {
        [captureDevice setTorchMode:AVCaptureTorchModeOn];
        isOpenFlash = YES;
    } else {
        [captureDevice setTorchMode:AVCaptureTorchModeOff];
        isOpenFlash = NO;
    }
    
    [captureDevice unlockForConfiguration];
}

@end


@implementation ScanViewFactory{
    NSObject<FlutterPluginRegistrar>* _registrar;
}
- (instancetype)initWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar
{
    self = [super init];
    if (self) {
        _registrar = registrar;
    }
    return self;
}

- (NSObject<FlutterMessageCodec>*)createArgsCodec {
    return [FlutterStandardMessageCodec sharedInstance];
}

- (NSObject<FlutterPlatformView>*)createWithFrame:(CGRect)frame
                                   viewIdentifier:(int64_t)viewId
                                        arguments:(id _Nullable)args
{
    ScanViewController* viewController = [[ScanViewController alloc] initWithFrame:frame
                                                                            viewIdentifier:viewId
                                                                                 arguments:args
                                                                           binaryRegistrar:_registrar];
    return viewController;
}
@end


