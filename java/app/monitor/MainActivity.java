package app.monitor;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.io.IOException;
import java.lang.Number;
import android.util.Log;

public class MainActivity extends Activity {

    private static final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter bAdapter;
    ConnectThread myConnectThread;
    ConnectedThread myConnectedThread;
    String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB";
    String MAC = "98:D3:31:F5:A0:3D";
    private UUID myUUID;
    String TAG = "MainActivity_climaMonitor";
    private StringBuilder sb = new StringBuilder();
    String id_str;
    TextView label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);
        // Получаем блютуз адаптер.
//         В данном случае BluetoothManager не используется. BluetoothManager для более ранних //         версий API.
        // Log.d(TAG, "bluetoothManager");
        // BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        Log.d(TAG, "bAdapter");
        BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bAdapter==null)
        {
            Toast.makeText(this, "Блютуз не поддерживается", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // Выводим на экран имя блютуз адаптера.

        String stInfo = bAdapter.getName() + "\n" + bAdapter.getAddress();
        TextView label = (TextView)findViewById(R.id.label);
        label.setText(String.format("Это устройство:\n%s", stInfo));
        if (!bAdapter.isEnabled()) {
            Log.d(TAG, "bAdapter is not Enabled");
            // Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1);
        }
        Intent dIntent =  new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        dIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(dIntent);
        BluetoothDevice device2 = bAdapter.getRemoteDevice(MAC);
        myConnectThread = new ConnectThread(device2);
        myConnectThread.start();  // Запускаем поток для подключения Bluetooth
    }

    // Закрытие приложения
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(myConnectThread!=null) myConnectThread.cancel();
    }

    // Поток для коннекта с Bluetooth
    private class ConnectThread extends Thread {
        private BluetoothSocket bluetoothSocket = null;
        private String TAG = "ConnectThread_climaMonitor";

        private ConnectThread(BluetoothDevice device) {

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
                bluetoothSocket.connect();
                myConnectedThread = new ConnectedThread(bluetoothSocket);
                myConnectedThread.start();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel(){
            try{
                bluetoothSocket.close();
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream
        private String TAG = "ConnectedThread_climaMonitor";
        private String sbprint;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run(){
            mmBuffer = new byte[1];
            int numBytes; // bytes returned from read()
            long digit;
            while(true){
                try{
                    numBytes = mmInStream.read(mmBuffer);    // Читаем входящие байты в буффер.
                    // Log.d(TAG, "numBytes is " + Integer.toString(numBytes));
                    String strIncom = new String(mmBuffer, 0, numBytes);
                    sb.append(strIncom);                     // собираем символы в строку
                    int endOfLineIndex = sb.indexOf("\r\n"); // определяем конец строки
                    if(endOfLineIndex > 0){
                        sbprint = sb.substring(0, endOfLineIndex); // Вычленям искомую строку.
                        sb.delete(0, sb.length());
                        if(sbprint.charAt(0) == 't'){ //
                            id_str = sbprint;
                            Log.d(TAG, "id_str is " + id_str);
                        }else{
                            runOnUiThread(new Runnable(){
                                @Override
                                public void run(){
                                    switch(id_str){
                                        case "t_value_0":
                                            label = (TextView)findViewById(R.id.t_value_0);
                                            break;
                                        case "t_value_1":
                                            label = (TextView)findViewById(R.id.t_value_1);
                                            break;
                                        case "t_value_2":
                                            label = (TextView)findViewById(R.id.t_value_2);
                                    }
                                    label.setText(sbprint);
                                }
                            });
                        }
                        // Log.d(TAG, "sbprint is " + sbprint.charAt(0));
                        try{
                            mmOutStream.write("ok".getBytes());
                        }catch(IOException e){
                            Log.e(TAG, "Error occurred when write to output stream");
                        }
                    }
                }catch(IOException e){
                    Log.e(TAG, "Error occurred when read from input stream");
                }
            }
        }
    }
}
