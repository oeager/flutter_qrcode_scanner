#import "ScannerPlugin.h"
#import "ScanViewController.h"


@implementation ScannerPlugin

static const int status_recognize_failed = 0;
static const int status_success = 1;
static const int status_file_not_exist = 2;
static const int status_platform_exception = 3;
static const int status_permission_denied = 4;

+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    // 注册原生视图
    ScanViewFactory *viewFactory = [[ScanViewFactory alloc] initWithRegistrar:registrar];
    [registrar registerViewFactory:viewFactory withId:@"com.qr.scanner.view"];
    
    FlutterMethodChannel* channel = [FlutterMethodChannel
                                     methodChannelWithName:@"com.qr.scanner"
                                     binaryMessenger:[registrar messenger]];
    ScannerPlugin* instance = [[ScannerPlugin alloc] init];
    [registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
    if ([@"fromImage" isEqualToString:call.method]) {
        [self scanQRCode:call result:result];
    } else {
        result(FlutterMethodNotImplemented);
    }
}

- (void)scanQRCode:(FlutterMethodCall*)call result:(FlutterResult)result{
    NSString *path = call.arguments[@"file"];
    if (path==nil) {
        result([FlutterError errorWithCode:[NSString stringWithFormat:@"%d",status_file_not_exist] message:@"file path can not be null" details:nil]);
        return;
    }
    UIImage *image = [UIImage imageWithContentsOfFile:path];
    CIDetector *detector = [CIDetector detectorOfType:CIDetectorTypeQRCode context:nil options:@{ CIDetectorAccuracy : CIDetectorAccuracyHigh }];
    
    
    NSArray *features = [detector featuresInImage:[CIImage imageWithCGImage:image.CGImage]];
    if (features.count > 0) {
        CIQRCodeFeature *feature = [features objectAtIndex:0];
        NSString *qrData = feature.messageString;
        NSDictionary * map = [NSDictionary dictionaryWithObjectsAndKeys:@"status",[NSNumber numberWithInt:status_success],@"result",qrData, nil];
        result(map);
    } else {
        NSDictionary * map = [NSDictionary dictionaryWithObjectsAndKeys:@"status",[NSNumber numberWithInt:status_recognize_failed],@"message",@"status recognize failed", nil];
        result(map);
    }
}
@end
