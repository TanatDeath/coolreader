package org.coolreader.crengine;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import org.coolreader.CoolReader;

public class MotionActivity extends Activity implements SensorEventListener {
    private float mSensorX;
    private float mSensorY;
    private Display mDisplay;
    private SensorManager sm;
    private PowerManager mPowerManager;
    private WindowManager mWindowManager;
    public CoolReader mCoolReader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get an instance of the SensorManager
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        if(sm.getSensorList(Sensor.TYPE_ACCELEROMETER).size()!=0){
            Sensor s = sm.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
            sm.registerListener(this,s, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Get an instance of the PowerManager
        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);

        // Get an instance of the WindowManager
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();

    }

    @Override
    public void onSensorChanged(SensorEvent event) {


        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;

        switch (mDisplay.getRotation()) {
            case Surface.ROTATION_0:
                mSensorX = event.values[0];
                mSensorY = event.values[1];
                if (mCoolReader!=null) mCoolReader.showToast("ROTATION_0");
                //textview.setText(String.valueOf( mSensorX));
                break;
            case Surface.ROTATION_90:
                mSensorX = -event.values[1];
                mSensorY = event.values[0];
                if (mCoolReader!=null) mCoolReader.showToast("ROTATION_90");
                //textview.setText(String.valueOf( mSensorX));
                break;
            case Surface.ROTATION_180:
                mSensorX = -event.values[0];
                mSensorY = -event.values[1];
                //textview.setText(String.valueOf( mSensorX));
                if (mCoolReader!=null) mCoolReader.showToast("ROTATION_180");
                break;
            case Surface.ROTATION_270:
                mSensorX = event.values[1];
                mSensorY = -event.values[0];
                //textview.setText(String.valueOf( mSensorX));
                if (mCoolReader!=null) mCoolReader.showToast("ROTATION_270");
                break;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }
    @Override
    protected void onResume() {
        super.onResume();

        if(sm.getSensorList(Sensor.TYPE_ACCELEROMETER).size()!=0){
            Sensor s = sm.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
            sm.registerListener(this,s, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {

        sm.unregisterListener(this);
        super.onPause();
    }

}