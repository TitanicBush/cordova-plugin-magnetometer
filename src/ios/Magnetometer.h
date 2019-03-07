//
//  Magnetometer.h
//
//  Obj-C code for the Cordova Magnetometer Plugin
//
//  Created by Rameez Raja<mrameezraja@gmail.com> on 8/20/15.
//
//


#import <UIKit/UIKit.h>
#import <Cordova/CDVPlugin.h>
#import <CoreMotion/CoreMotion.h>

@interface Magnetometer : CDVPlugin
{
    double x;
    double y;
    double z;
}

@property (readonly, assign) BOOL isRunning;
@property (nonatomic, strong) NSString* callbackId;

- (Magnetometer*)init;

- (void)start:(CDVInvokedUrlCommand*)command;
- (void)stop:(CDVInvokedUrlCommand*)command;
@end
