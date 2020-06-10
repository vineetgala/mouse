package com.example.sunshine;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mSensorAcc;
    private TextView mTextAcc;
    private CompanionDeviceManager deviceManager;
    private AssociationRequest pairingRequest;
    private BluetoothDeviceFilter deviceFilter;

    private static final int SELECT_DEVICE_REQUEST_CODE = 42;
    float currentValuex = 0;
    float currentValuey = 0;
    float currentValuez = 0;
    float c = 0;
    StringBuilder data = new StringBuilder();
    BluetoothAdapter bluetoothAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!(bluetoothAdapter.isEnabled())) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        deviceManager = getSystemService(CompanionDeviceManager.class);
        deviceFilter = new BluetoothDeviceFilter.Builder().build();
        pairingRequest = new AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .setSingleDevice(false)
                .build();
        deviceManager.associate(pairingRequest,
                new CompanionDeviceManager.Callback() {
                    @Override
                    public void onDeviceFound(IntentSender chooserLauncher) {
                        try {
                            startIntentSenderForResult(chooserLauncher,
                                    SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(CharSequence error) {

                    }
                },
                null);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mTextAcc = findViewById(R.id.label_light);

        mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        String sensor_error = getResources().getString(R.string.error_no_sensor);
        if (mSensorAcc == null) {
            mTextAcc.setText(sensor_error);
        }
        data.append("X,Y,Z");
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_DEVICE_REQUEST_CODE &&
                resultCode == Activity.RESULT_OK) {
            // User has chosen to pair with the Bluetooth device.
            final BluetoothDevice deviceToPair =
                    data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
            deviceToPair.createBond();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ConnectThread ct = new ConnectThread(deviceToPair);
                    // Cancel discovery because it otherwise slows down the connection.
                    bluetoothAdapter.cancelDiscovery();
                    ct.start();
                    ConnectedThread cted = new ConnectedThread(ct.mmSocket);
                    cted.start();
                    try {
                        ct.mmSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    cted.cancel();
                }
            },15000);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mSensorAcc != null)
            mSensorManager.registerListener(this, mSensorAcc, SensorManager.SENSOR_DELAY_GAME);

    }

    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this);
    }

    public void export(View view) {
        try {
            //saving the file into device
            FileOutputStream out = openFileOutput("data.csv", Context.MODE_PRIVATE);
            out.write((data.toString()).getBytes());
            out.close();

            //exporting
            Context context = getApplicationContext();
            File filelocation = new File(getFilesDir(), "data.csv");
            Uri path = FileProvider.getUriForFile(context, "com.example.sunshine.fileprovider", filelocation);
            Intent fileIntent = new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/csv");
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, "Data");
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(fileIntent, "Send mail"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static byte [] float2ByteArray (float value)
    {
        return ByteBuffer.allocate(4).putFloat(value).array();
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                currentValuex = event.values[0];
                currentValuey = event.values[1];
                currentValuez = event.values[2];
                mTextAcc.setText(getResources().getString(R.string.label_light, currentValuex, currentValuey, currentValuez));
                break;
            default:
        }
        if (c < 1000) {
            data.append("\n" + currentValuex + "," + currentValuey + "," + currentValuez);
            c++;
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }
}
class ConnectThread extends Thread {
    private static final String TAG = "MainActivity";
    private final BluetoothDevice mmDevice;
    public BluetoothSocket mmSocket;
    public ConnectThread(BluetoothDevice device) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        mmDevice = device;

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            tmp = device.createRfcommSocketToServiceRecord(mmDevice.getUuids()[0].getUuid());
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mmSocket = tmp;
    }

    public void run() {

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket =(BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(mmDevice,1);
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            Log.e("", "Couldn't establish Bluetooth connection!");
            return;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}

class ConnectedThread extends Thread {
    private final InputStream mmInStream ;
    private final OutputStream mmOutStream;
    private byte[] mmBuffer; // mmBuffer store for the stream
    private Handler handler=new Handler();
    private static final String TAG2 = "MY_APP_DEBUG_TAG";
    public ConnectedThread(BluetoothSocket socket) {
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams; using temp objects because
        // member streams are final.
        try {
            tmpIn = socket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG2, "Error occurred when creating input stream", e);
        }
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG2, "Error occurred when creating output stream", e);
        }
        mmOutStream = tmpOut;
        mmInStream = tmpIn;
    }

    public void run() {
        byte[] array = new byte[8];
        for (int i = 0; i <= 6; i++)
            array[i] = 0;
        array[7] = 1;
        write(array);
    }

    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
            // Share the sent message with the UI activity.
            Message writtenMsg = handler.obtainMessage(
                    MainActivity.MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
            writtenMsg.sendToTarget();
        } catch (IOException e) {
            Log.e(TAG2, "Error occurred when sending data", e);

            // Send a failure message back to the activity.
            Message writeErrorMsg =
                    handler.obtainMessage(MainActivity.MessageConstants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString("toast",
                    "Couldn't send data to the other device");
            writeErrorMsg.setData(bundle);
            handler.sendMessage(writeErrorMsg);
        }
    }
    public void cancel() {
        try {
            mmInStream.close();
            mmOutStream.close();
        } catch (IOException e) {
            Log.e(TAG2, "Could not close the connect socket", e);
        }
    }
}
