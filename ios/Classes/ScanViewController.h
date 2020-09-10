//
//  Header.h
//  Scanner
//
//  Created by 文柏胜 on 2020/9/10.
//

#ifndef ScanViewController_h
#define ScanViewController_h
#import <Foundation/Foundation.h>
#import <Flutter/Flutter.h>
#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#endif /* Header_h */

NS_ASSUME_NONNULL_BEGIN

@interface ScanViewController : NSObject<FlutterPlatformView>
@end

@interface ScanBox : UIView

@end

@interface ScanViewFactory : NSObject <FlutterPlatformViewFactory>
- (instancetype)initWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar;
@end

NS_ASSUME_NONNULL_END
