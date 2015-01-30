package fr.sebcreme.gethealth;

import android.app.Activity;
import android.content.IntentSender;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;


import java.util.Collection;
import java.util.HashSet;

public class MainActivity extends Activity implements SensorEventListener2, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    SensorManager sensorManager;
    Sensor heartSensor;
    GoogleApiClient mGoogleApiClient;
    private TextView mTextView;
    private TextView heartbeatTextView;
    private ImageButton actionBtn;
    private static final String COUNT_KEY = "fr.sebcreme.gethealth.bpm";
    private boolean sensorListen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        heartSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                heartbeatTextView = (TextView) stub.findViewById(R.id.heartbeat);
                actionBtn = (ImageButton) stub.findViewById(R.id.action);

                actionBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.i("GetHealth", "Restart");
                        if (sensorListen){
                            unregisterListener();
                            actionBtn.setImageResource(R.drawable.ic_play);
                            Log.i("GetHealth", "Stop sensor");
                        } else {
                            registerListener();
                            actionBtn.setImageResource(R.drawable.ic_pause);
                            Log.i("GetHealth", "Start sensor");
                        }
                    }
                });
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.i("GetHealth", "onConnected: " + connectionHint);
                        // Now you can use the Data Layer API



                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.i("GetHealth", "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.i("GetHealth", "onConnectionFailed: " + result);
                    }
                })
                        // Request access only to the Wearable API
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);



    }

    private void putBpm(int bpm, long timestamp) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/bpm");
        putDataMapReq.getDataMap().putInt("fr.sebcreme.gethealth.bpm", bpm);
        putDataMapReq.getDataMap().putLong("fr.sebcreme.gethealth.bpm.ts", timestamp);

        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
    }
    private void registerListener() {
        //Register the listener
        if (sensorManager != null){
            sensorManager.registerListener(this, heartSensor, 500000, 1000000);
            sensorListen = true;
        }
    }

    private void unregisterListener(){
        if (sensorManager != null){
            sensorManager.flush(this);
            sensorManager.unregisterListener(this, heartSensor);
            sensorListen = false;
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        Log.i("GetHealth", "start");
        mGoogleApiClient.connect();
        registerListener();
    }

    @Override
    protected void onStop() {
        Log.i("GetHealth", "stop");
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        unregisterListener();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

            Log.i("GetHealth", "accuracy changed " + getAccuracy(accuracy));

        }
    private String getAccuracy(int accuracy){
        switch(accuracy){
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW : return "ACCURACY_LOW";
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM: return "ACCURACY_MEDIUM";
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH: return "ACCURACY_HIGH";
            case SensorManager.SENSOR_STATUS_UNRELIABLE: return "UNRELIABLE";
            case SensorManager.SENSOR_STATUS_NO_CONTACT: return "NO_CONTACT";
        }
        return "NOT_FOUND";
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.i("GetHealth", "resume");
        registerListener();
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.i("GetHealth", "pause");
        //Unregister the listener
        if (sensorManager!=null)
            sensorManager.unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {


        //Update your data. This check is very raw. You should improve it when the sensor is unable to calculate the heart rate
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            //Log.i("GetHealth", "MaxFifoEventCount : "+event.sensor.getFifoMaxEventCount());
            //Log.i("GetHealth", "ReservedEventCount : "+event.sensor.getFifoReservedEventCount());
            //Log.i("GetHealth", "Accuracy : "+getAccuracy(event.accuracy));
            if ((int)event.values[0]>0) {
                Log.i("GetHealth", "sensor changed " + event.values[0] + " acc : "+getAccuracy(event.accuracy));
                mTextView.setText("" + getAccuracy(event.accuracy));
                heartbeatTextView.setText("♡" + (int) event.values[0]);
                putBpm((int) event.values[0], event.timestamp);
            }
            if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE || event.accuracy == SensorManager.SENSOR_STATUS_NO_CONTACT){
                mTextView.setText("Lost Connection");
                heartbeatTextView.setText("♡ 0");
            }
        }

    }

    @Override
    public void onFlushCompleted(Sensor sensor) {
        Log.i("GetHealth", "flush");
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i("GetHealth", "Connected to Google API Service");

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("GetHealth", "Connection to Google service Suspended with integer : "+i);
    }



    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i("GetHealth", "Connection to Google service failed with error code : "+connectionResult.getErrorCode());

    }


}
