package net.umittadelen.bleneo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import androidx.appcompat.widget.SwitchCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String SERVICE_UUID = "19b10000-e8f2-537e-4f6c-d104768a1214";
    private static final String COLOR_UUID = "19b10001-e8f2-537e-4f6c-d104768a1214";
    private static final String BRIGHTNESS_UUID = "19b10002-e8f2-537e-4f6c-d104768a1214";
    private static final String EFFECT_UUID = "19b10003-e8f2-537e-4f6c-d104768a1214";
    private static final String POWER_UUID = "19b10004-e8f2-537e-4f6c-d104768a1214";
    private static final String SPEED_UUID = "19b10005-e8f2-537e-4f6c-d104768a1214";
    private static final String DIRECTION_UUID = "19b10006-e8f2-537e-4f6c-d104768a1214";

    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner btScanner;
    private BluetoothGatt btGatt;
    private final List<BluetoothDevice> deviceList = new ArrayList<>();
    private ArrayAdapter<String> deviceAdapter;

    private Button btnScan, btnConnect, btnSetColor, btnSetBrightness;
    private Button btnSetEffect, btnSetPower, btnSetSpeed, btnSetDirection;
    private Spinner spinnerDevices, spinnerEffect;
    private EditText etColor;
    private SeekBar seekBrightness, seekSpeed;
    private SwitchCompat switchPower, switchDirection;
    private TextView tvStatus, tvBrightness, tvSpeed;

    private boolean isScanning = false;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initBluetooth();
        setupListeners();
        checkPermissions();
    }

    private void initViews() {
        btnScan = findViewById(R.id.btnScan);
        btnConnect = findViewById(R.id.btnConnect);
        btnSetColor = findViewById(R.id.btnSetColor);
        btnSetBrightness = findViewById(R.id.btnSetBrightness);
        btnSetEffect = findViewById(R.id.btnSetEffect);
        btnSetPower = findViewById(R.id.btnSetPower);
        btnSetSpeed = findViewById(R.id.btnSetSpeed);
        btnSetDirection = findViewById(R.id.btnSetDirection);

        spinnerDevices = findViewById(R.id.spinnerDevices);
        spinnerEffect = findViewById(R.id.spinnerEffect);
        etColor = findViewById(R.id.etColor);
        seekBrightness = findViewById(R.id.seekBrightness);
        seekSpeed = findViewById(R.id.seekSpeed);
        switchPower = findViewById(R.id.switchPower);
        switchDirection = findViewById(R.id.switchDirection);

        tvStatus = findViewById(R.id.tvStatus);
        tvBrightness = findViewById(R.id.tvBrightness);
        tvSpeed = findViewById(R.id.tvSpeed);

        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDevices.setAdapter(deviceAdapter);

        String[] effects = {"Solid", "Rainbow", "Pulse", "Chase", "Strobe", "Theater Chase"};
        ArrayAdapter<String> effectAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, effects);
        effectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEffect.setAdapter(effectAdapter);

        setControlsEnabled(false);
    }

    private void initBluetooth() {
        BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        if (btAdapter != null) {
            btScanner = btAdapter.getBluetoothLeScanner();
        }
    }

    private void setupListeners() {
        btnScan.setOnClickListener(v -> toggleScan());
        btnConnect.setOnClickListener(v -> connectToDevice());

        btnSetColor.setOnClickListener(v -> setColor());
        btnSetBrightness.setOnClickListener(v -> setBrightness());
        btnSetEffect.setOnClickListener(v -> setEffect());
        btnSetPower.setOnClickListener(v -> setPower());
        btnSetSpeed.setOnClickListener(v -> setSpeed());
        btnSetDirection.setOnClickListener(v -> setDirection());

        seekBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvBrightness.setText(getString(R.string.brightness_label, progress));
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSpeed.setText(getString(R.string.speed_label, progress));
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void checkPermissions() {
        List<String> permissionsList = new ArrayList<>();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // API 31+
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        } else {
            // API < 31
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(Manifest.permission.BLUETOOTH);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (!permissionsList.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsList.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    private void toggleScan() {
        if (isScanning) {
            stopScan();
        } else {
            startScan();
        }
    }

    private void startScan() {
        if (btAdapter == null || btScanner == null) {
            showToast("Bluetooth not available");
            return;
        }

        if (!btAdapter.isEnabled()) {
            showToast("Please enable Bluetooth");
            return;
        }

        // Check permissions based on Android version
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                showToast("Bluetooth scan permission required");
                checkPermissions();
                return;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                showToast("Location permission required");
                checkPermissions();
                return;
            }
        }

        deviceList.clear();
        deviceAdapter.clear();
        btScanner.startScan(scanCallback);
        isScanning = true;
        btnScan.setText(getString(R.string.scan_stop));
        tvStatus.setText(getString(R.string.scan_scanning));
    }

    private void stopScan() {
        if (btScanner == null) return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        btScanner.stopScan(scanCallback);
        isScanning = false;
        btnScan.setText(getString(R.string.scan_btn_label));
        tvStatus.setText(getString(R.string.scan_stopped));
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            String name = device.getName();
            if (name != null && !deviceList.contains(device)) {
                deviceList.add(device);
                deviceAdapter.add(name + "\n" + device.getAddress());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            runOnUiThread(() -> {
                showToast("Scan failed with error: " + errorCode);
                isScanning = false;
                btnScan.setText(getString(R.string.scan_btn_label));
                tvStatus.setText(getString(R.string.scan_failed));
            });
        }
    };

    private void connectToDevice() {
        int position = spinnerDevices.getSelectedItemPosition();
        if (position < 0 || position >= deviceList.size()) {
            showToast("Select a device");
            return;
        }

        if (isScanning) stopScan();

        BluetoothDevice device = deviceList.get(position);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                showToast("Bluetooth connect permission required");
                checkPermissions();
                return;
            }
        }

        tvStatus.setText(getString(R.string.scan_connecting));
        btGatt = device.connectGatt(this, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;
                runOnUiThread(() -> {
                    tvStatus.setText(getString(R.string.scan_connected));
                    setControlsEnabled(true);
                });

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false;
                runOnUiThread(() -> {
                    tvStatus.setText(getString(R.string.scan_disconnected));
                    setControlsEnabled(false);
                });
            }
        }
    };

    private void setControlsEnabled(boolean enabled) {
        btnSetColor.setEnabled(enabled);
        btnSetBrightness.setEnabled(enabled);
        btnSetEffect.setEnabled(enabled);
        btnSetPower.setEnabled(enabled);
        btnSetSpeed.setEnabled(enabled);
        btnSetDirection.setEnabled(enabled);
    }

    private void writeCharacteristic(String charUuid, byte[] value) {
        if (!isConnected || btGatt == null) {
            showToast("Not connected");
            return;
        }

        BluetoothGattService service = btGatt.getService(UUID.fromString(SERVICE_UUID));
        if (service == null) {
            showToast("Service not found");
            return;
        }

        BluetoothGattCharacteristic characteristic =
                service.getCharacteristic(UUID.fromString(charUuid));
        if (characteristic == null) {
            showToast("Characteristic not found");
            return;
        }

        characteristic.setValue(value);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                showToast("Bluetooth connect permission required");
                return;
            }
        }

        btGatt.writeCharacteristic(characteristic);
    }

    private void setColor() {
        String colorHex = etColor.getText().toString().replace("#", "");
        if (colorHex.length() != 6) {
            showToast("Enter valid hex color (e.g., FF0000)");
            return;
        }

        try {
            byte[] bytes = hexStringToByteArray(colorHex);
            writeCharacteristic(COLOR_UUID, bytes);
            showToast("Color set");
        } catch (Exception e) {
            showToast("Invalid color format");
        }
    }

    private void setBrightness() {
        int brightness = seekBrightness.getProgress();
        byte[] bytes = new byte[]{(byte) brightness};
        writeCharacteristic(BRIGHTNESS_UUID, bytes);
        showToast("Brightness set: " + brightness);
    }

    private void setEffect() {
        int effect = spinnerEffect.getSelectedItemPosition();
        byte[] bytes = new byte[]{(byte) effect};
        writeCharacteristic(EFFECT_UUID, bytes);
        showToast("Effect set");
    }

    private void setPower() {
        boolean isOn = switchPower.isChecked();
        byte[] bytes = new byte[]{(byte) (isOn ? 0xFF : 0x00)};
        writeCharacteristic(POWER_UUID, bytes);
        showToast("Power " + (isOn ? "ON" : "OFF"));
    }

    private void setSpeed() {
        int speed = seekSpeed.getProgress();
        byte[] bytes = new byte[]{(byte) speed};
        writeCharacteristic(SPEED_UUID, bytes);
        showToast("Speed set: " + speed);
    }

    private void setDirection() {
        boolean isReversed = switchDirection.isChecked();
        byte[] bytes = new byte[]{(byte) (isReversed ? 0xFF : 0x00)};
        writeCharacteristic(DIRECTION_UUID, bytes);
        showToast("Direction " + (isReversed ? "Reversed" : "Forward"));
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (btGatt != null) {
            try {
                btGatt.close();
            } catch (SecurityException e) {
                // Permission not granted, but we're closing anyway
            }
            btGatt = null;
        }
    }
}