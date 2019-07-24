package com.jht.heartratetest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.jht.heartratetest.utils.DateUtils;
import com.jht.heartratetest.utils.ToastUtils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static UUID UUID_SERVICE = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    public static UUID UUID_CHARACTERISTIC_ONE = UUID.fromString("00002A39-0000-1000-8000-00805f9b34fb");
    public static UUID UUID_CHARACTERISTIC_TWO = UUID.fromString("00002A38-0000-1000-8000-00805f9b34fb");
    public static UUID UUID_CHARACTERISTIC_THREE = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");
    public static UUID UUID_CHARACTERISTIC_THREE_Descriptor = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private String TAG = "TestSelf";
    private boolean Flag = false;
    private TextView tvDataMonitoring;
    private Button btnSendAd, btnStopSendAd;
    private EditText heartRateValue;
    private ToggleButton tbStart;//广播开关

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private AdvertiseSettings mAdvertiseSettings;
    private AdvertiseData mAdvertiseData;
    private HRAdvertiseCallback mAdvertiseCallback = new HRAdvertiseCallback();
    //蓝牙设备，代表一个具体的蓝牙外设。
    private BluetoothDevice mBluetoothDevice;
    //BLE广播操作类
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    //Gatt服务，一个服务多个特征
    private BluetoothGattService mGattService;
    //Gatt特征
    private BluetoothGattCharacteristic mGattCharacteristic, mGattCharacteristicTwo, mGattCharacteristicThree;
    //GattService回调
    private BluetoothGattServerCallback mGattServerCallback;
    //
    private BluetoothGattServer mGattServer;
    private BluetoothManager mBluetoothManager;
    //特征的描述
    private BluetoothGattDescriptor mBluetoothGattDescriptor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvDataMonitoring = findViewById(R.id.dataMonitoring);
        tvDataMonitoring.setMovementMethod(ScrollingMovementMethod.getInstance());
        btnSendAd = findViewById(R.id.sendAd);
        btnStopSendAd = findViewById(R.id.stopSendAd);
        tbStart = findViewById(R.id.startAd);
        heartRateValue = findViewById(R.id.heartRate);
        initBle();//蓝牙
        //扫描广播开关
        tbStart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    startAdvertise();//广播
                    addGattService();//服务
                    ToastUtils.showBottomToast(MainActivity.this, "请连接本设备");
                } else {
                    mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
                    Log.d(TAG, "广播关闭");
                    ToastUtils.showBottomToast(MainActivity.this, "已隐藏本设备");
                }
            }
        });

        //开始模拟
        btnSendAd.setOnClickListener(this);
        //关闭模拟
        btnStopSendAd.setOnClickListener(this);

    }

    /*
     * 获取用户输入的值
     * */
    private String getHeartRateValue() {
        if (TextUtils.isEmpty(heartRateValue.getEditableText()) || Integer.valueOf(heartRateValue.getEditableText().toString().trim()) > 255) {
            Looper.prepare();
            ToastUtils.showBottomToast(MainActivity.this, "请输人正确的心率值 (0-255)");
            Looper.loop();
            return "0";
        } else {
            //此处要使用getEditableText()，直接使用getText()会报错。
            return heartRateValue.getEditableText().toString();
        }
    }

    /*
     *初始化蓝牙
     * */
    private void initBle() {
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }

    //开启广播
    private void startAdvertise() {
        //设置蓝牙名称
        mBluetoothAdapter.setName("FakeHeartRate");
        //初始化广播设置
        mAdvertiseSettings = new AdvertiseSettings.Builder()
                //设置广播模式，以控制广播的功率和延迟。
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                //发射功率级别
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                //不得超过180000毫秒。值为0将禁用时间限制。
                .setTimeout(0)
                //设置是否可以连接
                .setConnectable(true)
                .build();
        //初始化广播包
        mAdvertiseData = new AdvertiseData.Builder()
                //设置广播设备名称
                .setIncludeDeviceName(true)
                //设置发射功率级别
                .setIncludeDeviceName(true)
                //设置UUID
                // .addServiceUuid()
                .build();

        //获取BLE广播的操作对象。
        //如果蓝牙关闭或此设备不支持蓝牙LE广播，则返回null。
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        //mBluetoothLeAdvertiser不为空，且蓝牙已开打
        if (mBluetoothAdapter.isEnabled()) {
            if (mBluetoothLeAdvertiser != null) {
                //开启广播
                mBluetoothLeAdvertiser.startAdvertising(mAdvertiseSettings,
                        mAdvertiseData, mAdvertiseCallback);
            } else {
                Log.d(TAG, "该手机不支持ble广播");
            }
        } else {
            Log.d(TAG, "手机蓝牙未开启");
        }
    }

    /*
     * 广播完成之后还需添加GATT进行数据传输
     * */
    private void addGattService() {
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //初始化Gattd的Service
        mGattService = new BluetoothGattService(UUID_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        //初始化特征值
        mGattCharacteristic = new BluetoothGattCharacteristic(UUID_CHARACTERISTIC_ONE, BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY);
        mGattCharacteristicTwo = new BluetoothGattCharacteristic(UUID_CHARACTERISTIC_TWO, BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY);
        mGattCharacteristicThree = new BluetoothGattCharacteristic(UUID_CHARACTERISTIC_THREE, BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY);
        //为service 添加特征
        mGattService.addCharacteristic(mGattCharacteristic);
        mGattService.addCharacteristic(mGattCharacteristicTwo);
        mGattService.addCharacteristic(mGattCharacteristicThree);
        //为特征添加描述
        mBluetoothGattDescriptor = new BluetoothGattDescriptor(UUID_CHARACTERISTIC_THREE_Descriptor, BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
        mGattCharacteristicThree.addDescriptor(mBluetoothGattDescriptor);

        //添加服务
        mGattServerCallback = new HRBluetoothGattServerCallback();
        mGattServer = mBluetoothManager.openGattServer(MainActivity.this, mGattServerCallback);
        mGattServer.addService(mGattService);


    }

    /*
     *打开蓝牙请求的回调
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                ToastUtils.showBottomToast(this, "蓝牙已经开启");
            } else if (resultCode == RESULT_CANCELED) {
                ToastUtils.showBottomToast(this, "没有蓝牙权限");
                finish();
            }
        }
    }


    /*
     *广播回调
     * */
    private class HRAdvertiseCallback extends AdvertiseCallback {
        //开启广播成功回调
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "开启广播成功");
        }

        //无法启动广播回调。
        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d(TAG, "开启服务失败，失败码 = " + errorCode);
        }
    }


    /*
     * Gatt回调
     * */
    private class HRBluetoothGattServerCallback extends BluetoothGattServerCallback {

        //设备连接/断开连接回调
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, final int newState) {
            mBluetoothDevice = device;
            Log.i(TAG, device.toString());
            //直接将判断语句写在主线程中，会导致跳过判断
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (newState == 2) {
                        addText(tvDataMonitoring, "设备已连接！" + DateUtils.getTime());
                    } else {
                        addText(tvDataMonitoring, "连接已断开！" + DateUtils.getTime());
                    }
                }
            });
            super.onConnectionStateChange(device, status, newState);
        }

        //添加本地服务回调
        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {

            super.onServiceAdded(status, service);
        }

        //特征值读取回调
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        //特征值写入回调
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        }

        //描述读取回调
        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {

            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        //描述写入回调
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }
    }

    /*
     * log打印
     * */
    private void addText(TextView textView, String content) {
        textView.append(content);
        textView.append("\n");
    }

    /*
     * log清屏
     * */
    private void emptyText(TextView textView) {
        textView.setText("");
    }

    /*
     * 数据发送
     * */
    @Override
    public void onClick(View view) {
        final Timer timer = new Timer(true);
        switch (view.getId()) {
            case R.id.sendAd:
                Flag = false;
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            mGattCharacteristicThree.setValue(Integer.valueOf(getHeartRateValue()),
                                    BluetoothGattCharacteristic.FORMAT_UINT8,
                                    /* offset */ 1);
                            mGattServer.notifyCharacteristicChanged(mBluetoothDevice, mGattCharacteristicThree, false);//true表示从客户端请求确认（指示），false表示发送通知
                        } catch (NullPointerException e) {
                            Looper.prepare();
                            ToastUtils.showBottomToast(MainActivity.this, "请先连接设备！！！");
                            Looper.loop();
                        }
                        if (Flag == true) timer.cancel();//停止
                    }
                }, 0, 1000);
                break;
            case R.id.stopSendAd:
                Flag = true;
                addText(tvDataMonitoring, "模拟心跳已停止！ " + DateUtils.getTime());
                break;
        }

    }

}
