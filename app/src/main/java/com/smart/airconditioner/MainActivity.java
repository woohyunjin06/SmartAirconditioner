package com.smart.airconditioner;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.airconditioner.model.Weather;
import com.smart.airconditioner.network.BluetoothClient;
import com.smart.airconditioner.network.DustInfo;
import com.smart.airconditioner.network.WeatherInfo;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    BluetoothClient client;
    DustInfo dInfo;
    WeatherInfo wInfo;

    TextView mWeather;
    TextView mDust;
    TextView mTemp;

    Button auto;
    Button custom;
    SeekBar power;
    Button timer;
    Switch onoff;



    @Override
    public void onCreate(Bundle onSaveStateInstance) {
        super.onCreate(onSaveStateInstance);
        setContentView(R.layout.activity_main);

        init();
    }
    public void init() {
        mWeather = findViewById(R.id.weather);
        mDust = findViewById(R.id.dust);
        mTemp = findViewById(R.id.temperature);

        auto = findViewById(R.id.auto);
        custom = findViewById(R.id.custom);
        power = findViewById(R.id.power);
        timer = findViewById(R.id.timer);

        onoff = findViewById(R.id.onoff);

        initInterface();
        initDustInfo();
        initWeatherInfo();
        initClient();
    }

    public void initInterface() {
        auto.setOnClickListener(this);
        custom.setOnClickListener(this);
        timer.setOnClickListener(this);
        onoff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int isCheck = isChecked ? 1 : 0;
                try {
                    handler.write(("P" + isCheck +"#").getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                auto.setEnabled(isChecked);
                custom.setEnabled(isChecked);
                power.setEnabled(isChecked);
                timer.setEnabled(isChecked);
            }

        });
        power.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                try {
                    handler.write(("P" + (progress+1) +"#").getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
    /**
     * refresh :
     * dInfo.getCurrentDust();
     * wInfo.getCurrentWeather();
     */
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
    public void notifyWeatherChange(Weather weather){ // WeatherTask 작업 끝을 알림
        int weatherId = weather.getWeaatherId();
        int resID = getResources().getIdentifier("@string/weather_"+weatherId, "string", "com.smart.airconditioner");
        String weatherString = getString(resID);
        mWeather.setText(weatherString);
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
        final ArrayList<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>(); // 이미 페어링된 디바이스 리스트를 가져온다.
        Set<BluetoothDevice> pairedDevices = client.getPairedDevices();
        deviceList.addAll(pairedDevices);

        deviceList.addAll(pairedDevices); // 근처 디바이스를 스캔한다.
        client.scanDevices(this, new BluetoothClient.OnScanListener() {
            ProgressDialog pd = new ProgressDialog(MainActivity.this);
            @Override public void onStart() {//스캔 시작.
                pd.setMessage("블루투스 기기를 찾는중... ");
                pd.setCancelable(false);
                pd.show();
            } @Override public void onFoundDevice(BluetoothDevice bluetoothDevice) { // 스캔이 완료된 디바이스를 받아온다.
                if(deviceList.contains(bluetoothDevice)) {
                    deviceList.remove(bluetoothDevice);
                } deviceList.add(bluetoothDevice);
            }
            @Override public void onFinish() { // 스캔 종료.
                for(BluetoothDevice bd : deviceList) {
                    deviceListString.add(bd.getName());
                }
                pd.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("연결할 블루투스 기기를 선택해주세요.");
                builder.setItems(deviceListString.toArray(new CharSequence[deviceListString.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        connectDevice(deviceList.get(which));
                    }
                }).show();

            }
        });
    }


    public void connectDevice(BluetoothDevice device){
        if(!client.connect(this, device, handler)) { // 블루투스가 사용 가능한 상태가 아니려면 false 리턴.
            Toast.makeText(this, "블루투스를 사용할 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if(client!=null)
            client.clear();
        super.onDestroy();
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
        ByteBuffer mmByteBuffer = ByteBuffer.allocate(50);

        @Override
        public void onData(byte[] buffer, int length) {
            if(length == 0) return;
            if(mmByteBuffer.position() + length >= mmByteBuffer.capacity()) {
                ByteBuffer newBuffer = ByteBuffer.allocate(mmByteBuffer.capacity() * 2);
                newBuffer.put(mmByteBuffer.array(), 0,  mmByteBuffer.position());
                mmByteBuffer = newBuffer;
            }
            mmByteBuffer.put(buffer, 0, length);
            String data = new String(mmByteBuffer.array());
            if(data.trim().length()<0)
                return;
            if(buffer[length - 1] == '>' && length != 1) {
                Toast.makeText(MainActivity.this, new String(mmByteBuffer.array(), 0, mmByteBuffer.position()), Toast.LENGTH_SHORT).show();
                mmByteBuffer.clear();
            }
        }
    };

    @Override
    public void onClick(View v) {
        String message;
        try {
            switch (v.getId()) {
                case R.id.auto:
                    message = "M0#";
                    handler.write(message.getBytes("UTF-8"));
                    break;

                case R.id.custom:
                    message = "M1#";
                    handler.write(message.getBytes("UTF-8"));
                    break;

                case R.id.timer:
                    showEditDialog();
                    break;
            }
        }catch(UnsupportedEncodingException e){
            e.printStackTrace();
        }
    }

    public void showEditDialog(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("전원을 몇 시간 뒤에 끄시겠습니까? (1시간 단위)");

        final EditText name = new EditText(this);
        alert.setView(name);

        alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String time = name.getText().toString();
                int time_int = 0;
                try {
                    time_int = Integer.parseInt(time);
                } catch(NumberFormatException e){
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "올바른 숫자가 아닙니다.", Toast.LENGTH_SHORT).show();
                }

                if(time_int>20||time_int<1){
                    Toast.makeText(MainActivity.this, "1이상, 20이하의 숫자를 입력해주세요.", Toast.LENGTH_SHORT).show();
                }
                try {
                    handler.write(("T"+time+"#").getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });


        alert.setNegativeButton("취소",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        alert.show();

    }
}