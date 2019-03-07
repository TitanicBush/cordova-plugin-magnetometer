//
//  Magnetometer.m
//
//  Obj-C code for the Cordova Magnetometer Plugin
//
//  Created by Rameez Raja<mrameezraja@gmail.com> on 8/20/15.
//
//


#import "Magnetometer.h"


@interface Magnetometer ()
    @property (readwrite, assign) BOOL isRunning;
    @property (readwrite, assign) BOOL haveReturnedResult;
    @property (readwrite, strong) CMMotionManager* motionManager;
    @property (readwrite, assign) double x;
    @property (readwrite, assign) double y;
    @property (readwrite, assign) double z;
    @property (readwrite, assign) NSTimeInterval timestamp;
@end

@implementation Magnetometer

@synthesize callbackId, isRunning, x, y, z, timestamp;

// Default to a 10 ms interval
#define DEFAULT_INTERVAL_MS 10

- (Magnetometer*)init
{
    self = [super init];
    if (self) {
        self.x = 0;
        self.y = 0;
        self.z = 0;
        self.timestamp = 0;
        self.callbackId = nil;
        self.isRunning = NO;
        self.haveReturnedResult = YES;
        self.motionManager = nil;
    } else {
        NSLog(@"Magnetometer initialize");
    }
    return self;
}

- (void)dealloc
{
    [self stop:nil];
}

- (void)start:(CDVInvokedUrlCommand*)command
{
    self.haveReturnedResult = NO;
    self.callbackId = command.callbackId;
    
    if (!self.motionManager) {
        self.motionManager = [[CMMotionManager alloc] init];
    }

    if ([self.motionManager isMagnetometerAvailable] == YES) {
        [self.motionManager setMagnetometerUpdateInterval: (DEFAULT_INTERVAL_MS / 1000)];
        __weak Magnetometer* weakSelf = self;
        [self.motionManager startMagnetometerUpdatesToQueue:[NSOperationQueue mainQueue] withHandler: ^(CMMagnetometerData *magnetometerData, NSError *error) {
            weakSelf.x         = magnetometerData.magneticField.x;
            weakSelf.y         = magnetometerData.magneticField.y;
            weakSelf.z         = magnetometerData.magneticField.z;
            weakSelf.timestamp = ([[NSDate date] timeIntervalSince1970] * 1000);
            [weakSelf returnMagnetInfo];
        }];
        
        if (!self.isRunning) {
            self.isRunning = YES;
        }
    }
    else {
        NSLog(@"Can't get magnetometer data!");
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_INVALID_ACTION messageAsString:@"Error. Magnetometer not available."];
        
        [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
    }
}

- (void)onReset
{
    [self stop:nil];
}

- (void)stop:(CDVInvokedUrlCommand*)command
{
    if ([self.motionManager isMagnetometerAvailable] == YES) {
        if (self.haveReturnedResult == NO) {
            [self returnMagnetInfo];
        }
        [self.motionManager stopMagnetometerUpdates];
    }
    self.isRunning = NO;
}

- (void)returnMagnetInfo
{
    NSMutableDictionary* magnetProps = [NSMutableDictionary dictionaryWithCapacity:4];

    [magnetProps setValue: [NSNumber numberWithDouble: self.x]         forKey:@"x"];
    [magnetProps setValue: [NSNumber numberWithDouble: self.y]         forKey:@"y"];
    [magnetProps setValue: [NSNumber numberWithDouble: self.z]         forKey:@"z"];
    [magnetProps setValue: [NSNumber numberWithDouble: self.timestamp] forKey:@"timestamp"];

    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:magnetProps];
    [result setKeepCallback:[NSNumber numberWithBool:YES]];
    [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
    self.haveReturnedResult = YES;
}
@end
