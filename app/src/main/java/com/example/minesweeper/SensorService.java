package com.example.minesweeper;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

interface SensorServiceListener {

    enum ALARM_STATE {
        ON,OFF
    }

    void alarmStateChanged(ALARM_STATE state);
}

public class SensorService extends Service implements SensorEventListener {

    private final static String TAG = "SENSOR_SERVICE";
    public static final double RAD_TO_DEG_FACTOR = (180 / Math.PI);

    private final double THRESHOLD = 1;

    private SensorServiceListener.ALARM_STATE currentAlarmState = SensorServiceListener.ALARM_STATE.OFF;

    private final IBinder mBinder = new SensorServiceBinder();

    private int mOrientation = 0;

    SensorServiceListener mListener;

    SensorManager mSensorManager;
    Sensor mSensor;

    SensorEvent mFirstEvent;

    public class SensorServiceBinder extends Binder {
        void registerListener(SensorServiceListener listener){
            Log.d("Binder", "registering...");
        }

        void startSensors(){
            mSensorManager.registerListener(SensorService.this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        void stopSensors(){
            mSensorManager.unregisterListener(SensorService.this);
        }
    }

    public SensorService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (mSensorManager != null){
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

            if (mSensor != null){
                Log.d(TAG , "Game rotation vector available");
            } else {
                Log.d(TAG , "Game rotation vector not available");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mFirstEvent == null){
            mFirstEvent = event;
        } else {
            float[] eventArr = getRotationArray(event.values);
            float[] neweventArr = getRotationArray(mFirstEvent.values);

            double absDiffX = neweventArr[0] - eventArr[0];
            double absDiffY = neweventArr[1] - eventArr[1];
            double absDiffZ = neweventArr[2] - eventArr[2];

            Log.d("DIFFS", "" + absDiffX + " " + absDiffY + " " + absDiffZ);

            SensorServiceListener.ALARM_STATE previousState = currentAlarmState;
            if (absDiffX > THRESHOLD || absDiffY > THRESHOLD || absDiffZ > THRESHOLD ) {
                this.currentAlarmState = SensorServiceListener.ALARM_STATE.ON;
            } else {
                this.currentAlarmState = SensorServiceListener.ALARM_STATE.OFF;
            }

            if(previousState != currentAlarmState) {
                mListener.alarmStateChanged(currentAlarmState);
            }
        }

        Log.d(TAG,event.values[0] + " " + event.values[1] + " " + event.values[2]);
    }

    private float[] getRotationArray(float[] vectors) {
//        double theta = Math.acos(values[3])*2;
//        double sinv = Math.sin(theta/2);
//
//        double xa = values[0]/sinv;
//        double ya = values[1]/sinv;
//        double za = values[2]/sinv;

        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, vectors);
        int worldAxisX = SensorManager.AXIS_X;
        int worldAxisZ = SensorManager.AXIS_Z;
        float[] adjustedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisX, worldAxisZ, adjustedRotationMatrix);
        float[] orientation = new float[3];
        SensorManager.getOrientation(adjustedRotationMatrix, orientation);
        orientation[0] = (float) Math.toDegrees(orientation[0]);
        orientation[1] = (float) Math.toDegrees(orientation[1]);
        orientation[2] = (float) Math.toDegrees(orientation[2]);

        return orientation;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}