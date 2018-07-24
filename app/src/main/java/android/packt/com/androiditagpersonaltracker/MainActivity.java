package android.packt.com.androiditagpersonaltracker;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener{
    //蓝牙部分
    private static final String TAG = "蓝牙";
    ListView deviceListView;
    Button startScanningButton;
    Button stopScanningButton;
    Button button2;
    public double jing;
    public double wei;
    ListView RssiView;
    //mqtt部分
    private MqttAsyncClient mqttClient;
    private final static String host = "iot.eclipse.org:1883";
    private final static String username = "Nw6sTc3Uo9MomuT";
    private final static String Topic = "inTopic";
    //蓝牙部分
    ArrayAdapter<String> listAdapter;
    ArrayList<BluetoothDevice> deviceList;
    //***
    //LIST OF ARRAY STRINGS WHICH WILL SERVE AS LIST ITEMS
    ArrayList<String> listItems=new ArrayList<String>();
    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    ArrayAdapter<String> adapter;
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    BluetoothLeScanner bluetoothLeScanner;
    BluetoothGatt bluetoothGatt;
    Timer timer;
    ArrayList<Double> distanceList = new ArrayList<>();
    private final static int MAX_DISTANCE_VALUES = 10;
    private final static int REQUEST_ENABLE_BT = 1;
    //gps部分
    public LocationClient mLocationClient;
  //  private TextView positionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //gps
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        //positionText = (TextView) findViewById(R.id.position_text_view);

        button2 = (Button)findViewById(R.id.button2);
        deviceListView = (ListView) findViewById(R.id.deviceListView);
        RssiView = (ListView) findViewById(R.id.RssiView);
        startScanningButton = (Button) findViewById(R.id.startScanButton);
        stopScanningButton = (Button) findViewById(R.id.stopScanButton);
        Switch switch1 = findViewById(R.id.switch1);
        switch1.setOnCheckedChangeListener(this);
        connectBroker();

        adapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        RssiView.setAdapter(adapter);
        listAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1);
        deviceList = new ArrayList<>();
        deviceListView.setAdapter(listAdapter);
        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()) {
            String [] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        } else {
            requestLocation();
        }
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                intent.putExtra("jing", jing);
                intent.putExtra("wei", wei);
                startActivity(intent);
            }
        });
        startScanningButton.setOnClickListener(new View.OnClickListener() {//监听startbutton
            public void onClick(View v) {
                startScanning();
            }
        });
        stopScanningButton.setOnClickListener(new View.OnClickListener() {//监听stopbutton
            public void onClick(View v) {
                stopScanning();
            }
        });

        initialiseBluetooth();//初始化蓝牙

        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                stopScanning();
                listAdapter.clear();
                BluetoothDevice device = deviceList.get(position);
                device.connectGatt(MainActivity.this, true, gattCallback);

            }
        });

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.show();
        }
    }

    private void connectBroker() {
        try {
            mqttClient = new MqttAsyncClient("tcp://" + host,
                    "ClientID" + username, new MemoryPersistence());
            mqttClient.connect(getOptions(), null);

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    private MqttConnectOptions getOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);//重连不保持状态
        options.setConnectionTimeout(10);//设置连接超时时间
        options.setKeepAliveInterval(30);//设置保持活动时间，超过时间没有消息收发将会触发ping消息确认
        return options;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            try {
                mqttClient.publish(Topic, "1".getBytes(), 1, false);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            try {
                mqttClient.publish(Topic, "0".getBytes(), 1, false);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }
    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if(result.getDevice() != null) {
                if(!isDuplicate(result.getDevice())) {
                    synchronized (result.getDevice()) {
                       if(result.getDevice().getName() != null ) {//&& result.getDevice().getName().toLowerCase().contains(iTAG)
                            listAdapter.add(result.getDevice().getName());
                            deviceList.add(result.getDevice());
                       }
                    }
                }
            }
        }
    };
    protected BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.i(TAG, "onConnectionStateChange() - STATE_CONNECTED");
                bluetoothGatt = gatt;
                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {

                                          @Override
                                          public void run() {
                                              //Called each time when 5000 milliseconds (5 seconds) (the period parameter)
                                              boolean rssiStatus = bluetoothGatt.readRemoteRssi();
                                              //Start a timer here
                                          }

                                      },
//Set how long before to start calling the TimerTask (in milliseconds)
                        0,
//Set the amount of time between each execution (in milliseconds)
                        5000);


            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.i(TAG, "onConnectionStateChange() - STATE_DISCONNECTED");
                timer.cancel();
                timer = null;
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status){
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, String.format("BluetoothGatt ReadRssi[%d]", rssi));
                listItems.add("BluetoothGatt ReadRssi: "+ rssi);
                adapter.notifyDataSetChanged();
                double distance = getDistance(rssi, 1);
                Log.i(TAG, "Distance is: "+distance);
                if(distanceList.size() == MAX_DISTANCE_VALUES) {
                    double sum = 0;
                    for(int i = 0; i < MAX_DISTANCE_VALUES; i++) {
                        sum = sum + distanceList.get(i);
                    }
                    final double averageDistance = sum / MAX_DISTANCE_VALUES;
                    distanceList.clear();
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference myRef = database.getReference("proximity");
                    myRef.setValue(new Double(averageDistance).toString());
                    showToast("BT is "+averageDistance+" mts. away");
                    listItems.add("BT is "+averageDistance+" mts. away");
                    adapter.notifyDataSetChanged();
                } else {
                    showToast("Gathering Data");
                    distanceList.add(distance);
                }

            }
        }
    };
    final void showToast(final String message) {
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                          }
                      }
        );
    }
    private void initialiseBluetooth() {
        bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }
    private boolean isDuplicate(BluetoothDevice device){
        for(int i = 0; i < listAdapter.getCount(); i++) {
            String addedDeviceDetail = listAdapter.getItem(i);
            if(addedDeviceDetail.equals(device.getAddress()) || addedDeviceDetail.equals(device.getName())) {
                return true;
            }
        }
        return false;
    }
    public void startScanning() {
        listAdapter.clear();
        deviceList.clear();
        if (bluetoothLeScanner == null) {
            initialiseBluetooth();
        }

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bluetoothLeScanner.startScan(leScanCallback);
            }
        });
    }
    public void stopScanning() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bluetoothLeScanner.stopScan(leScanCallback);
            }
        });
    }
    double getDistance(int rssi, int txPower) {
        /*
         * RSSI = TxPower - 10 * n * lg(d)
         * n = 2 (in free space)
         *
         * d = 10 ^ ((TxPower - RSSI) / (10 * n))
         */
        return (Math.pow(10, ((double) (-81.5) - rssi) / (10 * 2)));
    }

    //GPS!!!!
    private void requestLocation() {
        // initLocation();
        mLocationClient.start();
    }
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "必须同意所有权限才能使用本程序", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }
    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            wei = location.getLatitude();
            jing = location.getLongitude();
        }
    }


}
