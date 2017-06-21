package coza.opencollab.cpos800.serial;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android_serialport_api.SerialPort;
import coza.opencollab.cpos800.DataTools;

/**
 * Serial Manager
 */
public class SerialManager {

    /**
     * Logging tag.
     */
    private static final String TAG = "SerialManager";

    /**
     * Reference to the singleton serial manager
     */
    private static SerialManager serialManager;

    /**
     * Reference to the current serial interface.
     */
    private SerialConnection serialConnection;

    /**
     * GPIO file to configure for the printer
     */
    private static final String GPIO_PRINTER = "/sys/class/cw_gpios/printer_en/enable";

    /**
     * Data to write to enable a GPIO.
     */
    private static final byte[] GPIO_ENABLE = { '1' };

    /**
     * Data to write to disable a GPIO.
     */
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
    private SerialInterface currentInterface = SerialInterface.NONE;

    private SerialPort serialPort;

    /**
     * Get an instance of the serial manager
     * @return instance of the serial manager.
     */
    public static SerialManager getInstance(){
        if(serialManager == null){
            synchronized (SerialManager.class) {
                if(serialManager == null) {
                    serialManager = new SerialManager();
                }
            }
        }
        return serialManager;
    }

    /**
     * Private constructor to the serial manager
     */
    private SerialManager(){}


    /**
     * Open a serial interface.
     * @param serialInterface Serial interface to open.
     * @return True if the serial port was already opened
     * @throws IOException If there is IOException trying to open the serial port.
     */
    public synchronized boolean openSerialPort(SerialInterface serialInterface) throws IOException {
        // If we have a running connection
        if(serialConnection != null) {
            if (serialInterface == currentInterface) {
                Log.d(TAG, "Serial port already open");
                return true;
            }
            else if(currentInterface != SerialInterface.NONE){
                Log.d(TAG, "Other serial connection is currently open, closing");
                closeSerialPort();
            }
        }
        currentInterface = serialInterface;
        setGPIO(currentInterface, true);

        if(serialPort != null){
            Log.w(TAG, "Other serial connection is STILL OPEN!");
        }

        if(serialInterface == SerialInterface.NFC){
            serialPort = new SerialPort(new File("/dev/ttyHSL1"), 230400, 0);
        }else{
            serialPort = new SerialPort(new File("/dev/ttyHSL0"), 230400, 0);
        }

        serialConnection = new SerialConnection(serialPort);
        return false;
    }

    /**
     * Close the serial port.
     */
	public synchronized void closeSerialPort(){
		if(serialConnection == null){
            Log.d(TAG, "Serial port is not open");
            return;
        }
		serialConnection.close();
        serialConnection = null;
        setGPIO(currentInterface, false);
        currentInterface = SerialInterface.NONE;
        serialPort = null;
	}

    /**
     * Enable/Disable the GPIO configuration for the specified serial interface.
     * @param serialInterface Serial interface to enable/disable
     * @param enable Flag if the interface should be enabled or disable.
     */
	private void setGPIO(SerialInterface serialInterface, boolean enable){
        String gpioFile = null;
        Log.i(TAG, String.format("Setting GPIO for %s to %s", serialInterface.toString(), enable ? "ENABLED" : "DISABLED"));

        if(serialInterface == SerialInterface.PRINTER){
            gpioFile = GPIO_PRINTER;
        }

        if(gpioFile != null){
            FileOutputStream fw = null;
            FileInputStream fi = null;
            byte[] buffer = new byte[10];
            try {

                fw = new FileOutputStream(gpioFile);
                fi = new FileInputStream(gpioFile);
                fw.write(enable ? GPIO_ENABLE : GPIO_DISABLE);
                fw.flush();
                int size = fi.read(buffer);
                Log.d(TAG, "GPIO=" + DataTools.byteArrayToHex(buffer, size, true));
            }catch (IOException e){
                Log.e(TAG, "Exception while trying to set GPIO", e);
            }
            finally {
                if(fw != null) {
                    try {
                        fw.close();
                    } catch (IOException e) {}
                    try {
                        fi.close();
                    } catch (IOException e) {}
                }
            }

        }
    }

    /**
     * Get the size of the buffer currently read from the serial port.
     * @return Number of bytes that has been read
     */
    public int getReadBufferSize(){
        return this.serialConnection.getReadBufferSize();
    }

    /**
     * Get the contents currently in the read buffer.
     * @return The current contents of the read buffer.
     */
    public byte[] getReadBuffer(){
        return this.serialConnection.getReadBuffer();
    }

    /**
     * Write bytes to the currently open serial port.
     * @param bytes Bytes to write.
     * @throws IOException
     */
    public void write(byte[] bytes) throws IOException {
        serialConnection.write(bytes);
    }

    /**
     * Reads a number of bytes from the serial port.
     * @param buffer buffer to read into.
     * @param processingTime Time to allow for processing before the transmission starts.
     * @param transmitInterval Time to allow between data on the transmission.
     * @return The number of bytes that has been read into the buffer.
     */
    public int read(byte buffer[], int processingTime, int transmitInterval){
        return serialConnection.read(buffer, processingTime, transmitInterval);
    }
}
