/**
*   Magnetometer.java
*
*   A Java Class for the Cordova Magnetometer Plugin
*
*   @by Steven de Salas (desalasworks.com | github/sdesalas)
*   @licence MIT
*
*   @see https://github.com/sdesalas/cordova-plugin-magnetometer
*   @see https://github.com/apache/cordova-plugin-device-orientation
*   @see http://www.techrepublic.com/article/pro-tip-create-your-own-magnetic-compass-using-androids-internal-sensors/
*   
*/

package org.apache.cordova.magnetometer;

import java.util.List;
import java.util.ArrayList;
import java.lang.Math;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Context;

import android.os.Handler;
import android.os.Looper;


public class Magnetometer extends CordovaPlugin implements SensorEventListener  {
    // Define the sensor of interest
    public static final int ANDROID_MAGNETIC_SENSOR_TYPE = Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED;
    public static final int ANDROID_ACTIVE_SENSOR_ACCURACY = SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM;

    public static final int STOPPED = 0;
    public static final int STARTING = 1;
    public static final int RUNNING = 2;
    public static final int ERROR_FAILED_TO_START = 3;

    private float x,y,z;
    private long  timestamp;
    private int   status;
    private int   accuracy = Magnetometer.ANDROID_ACTIVE_SENSOR_ACCURACY;

    private SensorManager sensorManager;    // Sensor manager
    Sensor mSensor;                         // Magnetic sensor returned by sensor manager

    private CallbackContext callbackContext;

    private Handler mainHandler = null;
    private Runnable mainRunnable = new Runnable() {
        public void run() {
            Magnetometer.this.timeout();
        }
    };

    public Magnetometer() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.timestamp = 0;
        this.setStatus(Magnetometer.STOPPED);
    }

    //--------------------------------------------------------------------------
    // Cordova Plugin Methods
    //--------------------------------------------------------------------------
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.sensorManager = (SensorManager) cordova.getActivity().getSystemService(Context.SENSOR_SERVICE);
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        if (action.equals("start")) {
            this.callbackContext = callbackContext;
            if (this.status != Magnetometer.RUNNING) {
                this.start();
            }
        }
        else if (action.equals("stop")) {
            if (this.status == Magnetometer.RUNNING) {
                this.stop();
            }
        } else {
            return false;
        }
        
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT, "");
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
        return true;
    }

    public void OnDestroy() {
        this.stop();
    }


    //--------------------------------------------------------------------------
    // Local Methods
    //--------------------------------------------------------------------------

    /**
     * Start listening for compass sensor.
     *
     * @return          status of listener
     */
    private int start() {
        // If already starting or running, then just return
        if ((this.status == Magnetometer.RUNNING) || (this.status == Magnetometer.STARTING)) {
            startTimeout();
            return this.status;
        }

        // Get magnetic field sensor from sensor manager
        this.setStatus(Magnetometer.STARTING);
        @SuppressWarnings("deprecation")
        List<Sensor> list = this.sensorManager.getSensorList(Magnetometer.ANDROID_MAGNETIC_SENSOR_TYPE);

        // If found, then register as listener
        if (list != null && list.size() > 0) {
            this.mSensor = list.get(0);
            // check this
            if (this.sensorManager.registerListener(this, this.mSensor, SensorManager.SENSOR_DELAY_NORMAL)) {
                this.setStatus(Magnetometer.STARTING);
                this.accuracy = Magnetometer.ANDROID_ACTIVE_SENSOR_ACCURACY;
            } else {
                this.setStatus(Magnetometer.ERROR_FAILED_TO_START);
                this.fail(Magnetometer.ERROR_FAILED_TO_START, "Device sensor returned an error.");
                return this.status;
            };
        } else {
            this.setStatus(Magnetometer.ERROR_FAILED_TO_START);
            this.fail(Magnetometer.ERROR_FAILED_TO_START, "No sensors found to register to.");
            return this.status;
        }

        startTimeout();
        return this.status;
    }

    private void startTimeout() {
        stopTimeout();
        mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.postDelayed(mainRunnable, 2000);
    }

    private void stopTimeout() {
        if (mainHandler != null) {
            mainHandler.removeCallbacks(mainRunnable);
        }
    }
    /**
     * Stop listening to compass sensor.
     */
    public void stop() {
        if (this.status != Magnetometer.STOPPED) {
            this.sensorManager.unregisterListener(this);
        }
        this.setStatus(Magnetometer.STOPPED);
        this.accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
    }

    /**
     * Called after a delay to time out if the listener has not attached fast enough.
     */
    private void timeout() {
        if (this.status == Magnetometer.STARTING &&
            this.accuracy >= Magnetometer.ANDROID_ACTIVE_SENSOR_ACCURACY) {
            this.timestamp = System.currentTimeMillis();
            this.win();
        }
    }

    //--------------------------------------------------------------------------
    // SensorEventListener Interface
    //--------------------------------------------------------------------------

    /**
     * Required by SensorEventListener
     * @param sensor
     * @param accuracy
     */
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() != Magnetometer.ANDROID_MAGNETIC_SENSOR_TYPE) {
            return;
        }

        if (this.status == Magnetometer.STOPPED) {
            return;
        }
        this.accuracy = accuracy;
    }

    /**
     * Sensor listener event.
     *
     * @param event
     */
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Magnetometer.ANDROID_MAGNETIC_SENSOR_TYPE) {
            return;
        }

        if (this.status == Magnetometer.STOPPED) {
            return;
        }
        this.setStatus(Magnetometer.RUNNING);

        if (this.accuracy >= Magnetometer.ANDROID_ACTIVE_SENSOR_ACCURACY) {
        {
            // Save reading
            this.timestamp = System.currentTimeMillis();
            this.x = event.values[0];
            this.y = event.values[1];
            this.z = event.values[2];
    
            this.win();
        }
    }
    // ------------------------------------------------
    // JavaScript Interaction
    // ------------------------------------------------

    @Override
    public void onReset() {
        if (this.status == Magnetometer.RUNNING) {
            this.stop();
        }
    }

    private void fail(int code, String message) {
        JSONObject errorObj = new JSONObject();
        try {
            errorObj.put("code", code);
            errorObj.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PluginResult err = new PluginResult(PluginResult.Status.ERROR, errorObj);
        err.setKeepCallback(true);
        callbackContext.sendPluginResult(err);
    }

    private void win() {
        PluginResult result = new PluginResult(PluginResult.Status.OK, this.getReadingJSON());
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
    }

    private void setStatus(int status) {
        this.status = status;
    }

    private JSONObject getReadingJSON() {
        JSONObject r = new JSONObject();
        try {
            r.put("x", this.x);
            r.put("y", this.y);
            r.put("z", this.z);
            r.put("timestamp", this.timestamp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return r;
    }
}
