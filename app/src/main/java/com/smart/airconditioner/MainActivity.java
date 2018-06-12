package com.smart.airconditioner;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.airconditioner.network.BluetoothClient;
import com.smart.airconditioner.network.DustInfo;
import com.smart.airconditioner.network.WeatherInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    BluetoothClient client;
    DustInfo dInfo;
    WeatherInfo wInfo;

    TextView mWeather;
    TextView mDust;

    String weather; // 날씨 저장
    String dust; // 미세먼지 저장

    @Override
    public void onCreate(Bundle onSaveStateInstance) {
        super.onCreate(onSaveStateInstance);
        setContentView(R.layout.activity_main);

        init();
    }
    public void init() {
        mWeather = findViewById(R.id.weather);
        mDust =  findViewById(R.id.dust);

        initDustInfo();
        initWeatherInfo();
        //initClient();
    }
    public void initDustInfo() {
        dInfo = new DustInfo(this);
        dInfo.getCurrentDust();
    }
    public void notifyDustChange(String data){
        mDust.setText(data + "㎍/㎥");
    }
    public void initWeatherInfo() {
        wInfo = new WeatherInfo(this);
        wInfo.getCurrentWeather();
    }
    public void notifyWeatherChange(JSONObject obj){ // WeatherTask 작업 끝을 알림
        try {
            JSONArray weatherArr = obj.getJSONArray("weather");
            String weather = weatherArr.getJSONObject(0).getString("id");
            Toast.makeText(this, weather, Toast.LENGTH_SHORT).show();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void initClient() {
        client = BluetoothClient.getInstance();
        if(client == null) { // 블루투스를 사용할 수 없는 장비일 경우 null.
            Toast.makeText(getApplicationContext(), "블루투스를 사용할 수 없는 기기입니다.", Toast.LENGTH_LONG).show();
            finish();
        }
        else {
            if(!client.isEnabled())
                enableBluetooth();
            else {
                getPairedDevice();
            }
        }
    }

    public void enableBluetooth() {
        client.enableBluetooth(this, new BluetoothClient.OnBluetoothEnabledListener() {
            @Override
            public void onBluetoothEnabled(boolean success) {
                if(success){
                    getPairedDevice();
                }
                else{
                    Toast.makeText(getApplicationContext(), "블루투스를 활성화하지 못했습니다.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
    }

    public void getPairedDevice() {
        final List<String> deviceListString = new ArrayList<>();

        final Set<BluetoothDevice> pairedDevices = client.getPairedDevices(); //페어링 된 디바이스만 가져옴
        final ArrayList<BluetoothDevice> deviceList = new ArrayList<>(pairedDevices);

        final int count = deviceList.size();
        for(BluetoothDevice bd : deviceList) {
            deviceListString.add(bd.getName());
        }
        deviceListString.add("찾기");

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("연결할 블루투스 기기를 선택해주세요.");
        builder.setItems(deviceListString.toArray(new CharSequence[deviceListString.size()]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if(which == count){ //찾기
                    findDevice();
                    dialog.dismiss();
                }

                else {
                    connectDevice(deviceList.get(which));
                }
            }
        }).show();
        // 근처 디바이스를 스캔한다.
    }

    BluetoothClient.BluetoothStreamingHandler handler = new BluetoothClient.BluetoothStreamingHandler() {
        @Override
        public void onError(Exception e) {
            // 에러 발생. 연결이 종료된다.
        }

        @Override
        public void onConnected() {
            // 연결 이벤트.
        }

        @Override
        public void onDisconnected() {
            // 연결 종료 이벤트.
        }
        ByteBuffer mmByteBuffer = ByteBuffer.allocate(1024);
        @Override
        public void onData(byte[] buffer, int length) {
            if(length == 0) return;
            if(mmByteBuffer.position() + length >= mmByteBuffer.capacity()) {
                ByteBuffer newBuffer = ByteBuffer.allocate(mmByteBuffer.capacity() * 2);
                newBuffer.put(mmByteBuffer.array(), 0,  mmByteBuffer.position());
                mmByteBuffer = newBuffer;
            }
            mmByteBuffer.put(buffer, 0, length);
            if(buffer[length - 1] == '\0' && length != 1) {
                Toast.makeText(MainActivity.this, new String(mmByteBuffer.array(), 0, mmByteBuffer.position()), Toast.LENGTH_SHORT).show();
                mmByteBuffer.clear();
            }
        }
    };
    public void connectDevice(BluetoothDevice device){
        if(!client.connect(this, device, handler)) { // 블루투스가 사용 가능한 상태가 아니려면 false 리턴.
            Toast.makeText(this, "블루투스를 사용할 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }
    public void findDevice(){
        final List<String> deviceListString = new ArrayList<>();
        client.scanDevices(MainActivity.this, new BluetoothClient.OnScanListener() {
            ProgressDialog progressDialog;
            final ArrayList<BluetoothDevice> deviceList_2 = new ArrayList<>();

            @Override
            public void onStart() { // 스캔 시작.
                Toast.makeText(MainActivity.this, "CALL", Toast.LENGTH_SHORT).show();

                progressDialog.show();
            }
            @Override public void onFoundDevice(BluetoothDevice bluetoothDevice) { // 스캔이 완료된 디바이스를 받아온다.
                deviceList_2.add(bluetoothDevice);
            }
            @Override public void onFinish() { // 스캔 종료.
                progressDialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("연결할 블루투스 기기를 선택해주세요.");
                builder.setItems(deviceListString.toArray(new CharSequence[deviceListString.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        connectDevice(deviceList_2.get(which));
                    }
                });
            }
        });
    }
    @Override
    protected void onDestroy() {
        if(client!=null)
            client.clear();
        super.onDestroy();
    }
}
