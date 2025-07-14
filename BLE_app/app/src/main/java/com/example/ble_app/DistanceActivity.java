package com.example.ble_app;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;

import java.util.UUID;

public class DistanceActivity extends AppCompatActivity {

    private View bar1, bar2, bar3, bar4, bar5;
    private TextView distanceTextView;
    private TextView powerTextView;

    private BluetoothDevice device;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic writeCharacteristic;

    private Handler handler = new Handler();
    private static final long RSSI_UPDATE_INTERVAL = 1000;

    private static final UUID SERVICE_UUID = UUID.fromString("dc94bd87-6388-4a52-a158-804e405df089");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("2ec0cef9-9e97-48a5-8d92-07bb60c0eae6");

    private final Runnable rssiUpdater = new Runnable() {
        @Override
        public void run() {
            if (bluetoothGatt != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(DistanceActivity.this,
                            android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                bluetoothGatt.readRemoteRssi();
                handler.postDelayed(this, RSSI_UPDATE_INTERVAL);
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(DistanceActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    writeCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread(() -> updateDistanceUI(rssi));
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distance);

        bar1 = findViewById(R.id.bar1);
        bar2 = findViewById(R.id.bar2);
        bar3 = findViewById(R.id.bar3);
        bar4 = findViewById(R.id.bar4);
        bar5 = findViewById(R.id.bar5);
        distanceTextView = findViewById(R.id.distanceTextView);
        powerTextView = findViewById(R.id.powerTextView);

        String deviceAddress = getIntent().getStringExtra("device_address");
        if (deviceAddress == null) {
            finish();
            return;
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                finish();
                return;
            }
        }

        device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        connectToDevice();
    }

    private void connectToDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                finish();
                return;
            }
        }
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
        handler.postDelayed(rssiUpdater, RSSI_UPDATE_INTERVAL);
    }

    private void updateDistanceUI(int rssi) {
        int txPower = -50;
        double n = 2.0;
        double distance = Math.pow(10d, ((txPower - rssi) / (10 * n)));

        String distanceStr = String.format("Distanța: %.2f m", distance);
        String powerStr = String.format("Puterea: %d dB", rssi);
        distanceTextView.setText(distanceStr);
        powerTextView.setText(powerStr);

        int barsToColor;
        if (distance < 0.5) {
            barsToColor = 5;
        } else if (distance < 1.0) {
            barsToColor = 4;
        } else if (distance < 2.0) {
            barsToColor = 3;
        } else if (distance < 3.0) {
            barsToColor = 2;
        } else {
            barsToColor = 1;
        }

        int defaultColor = 0xFFCCCCCC;
        int activeColor = 0xFF01579B;

        bar1.setBackgroundColor(barsToColor >= 1 ? activeColor : defaultColor);
        bar2.setBackgroundColor(barsToColor >= 2 ? activeColor : defaultColor);
        bar3.setBackgroundColor(barsToColor >= 3 ? activeColor : defaultColor);
        bar4.setBackgroundColor(barsToColor >= 4 ? activeColor : defaultColor);
        bar5.setBackgroundColor(barsToColor >= 5 ? activeColor : defaultColor);

        if (writeCharacteristic != null) {
            String message = "RSSI:" + rssi + ",Dist.:" + String.format("%.2f", distance);
            writeCharacteristic.setValue(message);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            bluetoothGatt.writeCharacteristic(writeCharacteristic);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(rssiUpdater);
        if (bluetoothGatt != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }
}
