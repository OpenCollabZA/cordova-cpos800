package coza.opencollab.cpos800.serial;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import android_serialport_api.SerialPort;

/**
 * Created by charl on 2017/05/31.
 */

public class SerialManager {
    private static final String TAG = "SerialManager";


    /**
     * Reference to the singleton serial manager
     */
    private static SerialManager serialManager;

    private SerialConnection serialConnection;

    /**
     * Get an instance of the serial manager
     * @return
     */
    public static SerialManager getInstance(){
        if(serialManager == null){
            serialManager = new SerialManager();
        }
        return serialManager;
    }

    /**
     * Private constructor to the serial manager
     */
    private SerialManager(){}


    /**
     * Open the NFC Serial port
     */
    public synchronized void openSerialPort() throws IOException {
        if(serialConnection != null){
            Log.d(TAG, "Serial port already open");
            return;
        }
        SerialPort serialPort = new SerialPort(new File("/dev/ttyHSL1"), 230400, 0);
        serialConnection = new SerialConnection(serialPort);
    }

    public void write(byte[] bytes) throws IOException {
        serialConnection.write(bytes);
    }

    public int read(byte buffer[], int processingTime, int transmitInterval){
        return serialConnection.read(buffer, processingTime, transmitInterval);
    }
}
