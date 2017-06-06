package coza.opencollab.cpos800.serial;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

    private static final String GPIO_PRINTER = "/sys/class/cw_gpios/printer_en/enable";
    private static final byte[] GPIO_ENABLE = { '1' };
    private static final byte[] GPIO_DISABLE = { '0' };

    /**
     * Enumeration type indicating which type of serial port should be openned
     */
    public enum SerialInterface{
        NFC,
        PRINTER,
        NONE
    }

    /**
     * Serial interface that is currently open
     */
    private SerialInterface currentInterace = SerialInterface.NONE;

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
    public synchronized void openSerialPort(SerialInterface serialInterface) throws IOException {
        // If we have a running connection
        if(serialConnection != null) {
            if (serialInterface == currentInterace) {
                Log.d(TAG, "Serial port already open");
                return;
            }
            else if(currentInterace != SerialInterface.NONE){
                Log.d(TAG, "Other serial connection is currently open, closing");
                closeSerialPort();
            }
        }
        currentInterace = serialInterface;
        setGPIO(currentInterace, true);
        SerialPort serialPort;
        if(serialInterface == SerialInterface.NFC){
            serialPort = new SerialPort(new File("/dev/ttyHSL1"), 230400, 0);
        }else{
            serialPort = new SerialPort(new File("/dev/ttyHSL0"), 230400, 0);
        }

        serialConnection = new SerialConnection(serialPort);
    }

	public synchronized void closeSerialPort(){
		if(serialConnection == null){
            Log.d(TAG, "Serial port is not open");
            return;
        }
		serialConnection.close();
        serialConnection = null;
        setGPIO(currentInterace, false);
        currentInterace = SerialInterface.NONE;
	}

	private void setGPIO(SerialInterface serialInterface, boolean enable){
        String gpioFile = null;
        if(serialInterface == SerialInterface.PRINTER){
            gpioFile = GPIO_PRINTER;
        }

        if(gpioFile != null){
            FileOutputStream fw = null;
            try {
                fw = new FileOutputStream(gpioFile);
                fw.write(enable ? GPIO_ENABLE : GPIO_DISABLE);
            }catch (IOException e){
                Log.e(TAG, "Exception while trying to set GPIO", e);
            }
            finally {
                if(fw != null) {
                    try {
                        fw.close();
                    } catch (IOException e) {}
                }
            }

        }
    }

    public void write(byte[] bytes) throws IOException {
        serialConnection.write(bytes);
    }

    public int read(byte buffer[], int processingTime, int transmitInterval){
        return serialConnection.read(buffer, processingTime, transmitInterval);
    }
}
