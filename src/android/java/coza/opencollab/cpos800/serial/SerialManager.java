package coza.opencollab.cpos800.serial;

import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
     * GPIO file to configure for the printer
     */
    private static final String GPIO_PRINTER = "/sys/class/cw_gpios/printer_en/enable";
    private static final String GPIO_STM32 = "/sys/class/stm32_gpio/stm32_en/enable";

    private boolean isStm32 = fileIsExists(GPIO_STM32);

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
        STM32,
        NONE
    }

    /**
     * Serial interface that is currently open
     */
    private SerialInterface currentInterface = SerialInterface.NONE;

    private SerialPort serialPort;

    private InputStream inputStream = null;

    private OutputStream outputStream = null;

    /**
     * Thread for reading the input stream
     */
    private SerialReadThread readThread;

    /**
     * Buffer of bytes that has been read
     */
    private byte[] readBuffer = new byte[50 * 1024];

    /**
     * Current number of bytes read into the buffer, also the current index to add
     * the next item in the buffer
     */
    private int readBufferSize = 0;


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


    private boolean fileIsExists(String strFile) {
        try {
            File f = new File(strFile);
            if (!f.exists()) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Open a serial interface.
     * @param serialInterface Serial interface to open.
     * @return True if the serial port was already opened
     * @throws IOException If there is IOException trying to open the serial port.
     */
    public synchronized boolean openSerialPort(SerialInterface serialInterface) throws IOException {
        // If we have a running connection
        if(serialPort != null) {
            if (serialInterface == currentInterface) {
                Log.d(TAG, "Serial port already open");
                return true;
            }
            else if(currentInterface != SerialInterface.NONE){
                Log.d(TAG, "Other serial connection is currently open, closing");
                closeSerialPort();
				SystemClock.sleep(700);
            }
        }
        currentInterface = serialInterface;
        setGPIO(currentInterface, true);


        if (serialInterface == SerialInterface.PRINTER && isStm32) {
            SystemClock.sleep(100);
            setGPIO(SerialInterface.STM32, true);
        }

        if(serialPort != null){
            Log.w(TAG, "Other serial connection is STILL OPEN!");
        }




        if(serialInterface == SerialInterface.NFC){
            serialPort = new SerialPort(new File("/dev/ttyHSL1"), 230400, 0);
        }else{
            serialPort = new SerialPort(new File("/dev/ttyHSL0"), 230400, 0);
        }
        this.outputStream = serialPort.getOutputStream();
        this.inputStream = serialPort.getInputStream();
        this.readThread = new SerialReadThread();
        this.readThread.start();
        return false;
    }

    /**
     * Close the serial port.
     */
	public synchronized void closeSerialPort(){
		if(serialPort != null){
	        this.readThread.interrupt();
	        this.readThread = null;
	        try {
	            this.outputStream.close();
	        } catch (IOException e) {
	        }
	        try {
	            this.inputStream.close();
	        } catch (IOException e) {
	        }
	        this.serialPort.close();
	        setGPIO(currentInterface, false);
	        if (currentInterface == SerialInterface.PRINTER && isStm32) {
	            setGPIO(SerialInterface.STM32, false);
	        }
	        currentInterface = SerialInterface.NONE;
	        this.outputStream = null;
	        this.inputStream = null;
	        serialPort = null;
		}
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
        else if(serialInterface == SerialInterface.STM32){
            gpioFile = GPIO_STM32;
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
     *
     * @param buffer Buffer to fill
     * @param processingTime Initial time to wait for a response (waiting for device to process and respond to request)
     * @param transmitInterval Interval in which data can be transmitted
     * @return Returns the number of bytes that has been saved to the buffer.
     */
    public synchronized int read(byte buffer[], int processingTime, int transmitInterval) {
        // Here we try and shorten the processing time by actively checking early if the processing is done
        final int sleepTime = 5;

        // Number of early loops we can do to check for output
        final int length = processingTime / sleepTime;

        // Flag if we should stop reading for input
        boolean shutDown = false;

        for (int i = 0; i < length; i++) {
            // If we still haven't read anything, sleep a little bit more
            if (readBufferSize == 0) {
                SystemClock.sleep(sleepTime);
                continue;
            }
            // We finally got the start of the transmission
            else {
                break;
            }
        }

        // After the processingTime timeout, we expect some data to be in the buffer already
        if (readBufferSize > 0) {

            // Last time data was read from the serial port
            long lastBufferChangeTime = System.currentTimeMillis();

            // Time right now
            long currentTime;

            // Previous number of bytes in the buffer
            int previousBufferSize = 0;

            // Current number of bytes in the buffer
            int currentBufferSize;

            // While we are not stopping, keep reading
            while (!shutDown) {
                currentTime = System.currentTimeMillis();
                currentBufferSize = readBufferSize;

                // Check if the buffer size has changed
                if (currentBufferSize > previousBufferSize) {
                    lastBufferChangeTime = currentTime;
                    previousBufferSize = currentBufferSize;
                }
                // If the buffer size hasn't changed, and we waited more than the transmit interval, the transmission is complete
                else if (currentBufferSize == previousBufferSize && currentTime - lastBufferChangeTime >= transmitInterval) {
                    shutDown = true;
                }
            }
            // Copy the read buffer into the output buffer
            if (readBufferSize <= buffer.length) {
                System.arraycopy(readBuffer, 0, buffer, 0, readBufferSize);
            }
            else{
                Log.w(TAG, "We have read more bytes than what can fit in the output buffer");
            }
        }
        return readBufferSize;
    }


    /**
     * Reset the read buffer to the start
     */
    private synchronized void resetReadBuffer(){
        Log.d(TAG, "Reset read buffer to start");
        readBufferSize = 0;
    }

    /**
     * Writes a new command over the serial port
     * @param bytes
     * @throws IOException
     */
    public synchronized void write(byte[] bytes) throws IOException {
        resetReadBuffer();
        this.outputStream.write(bytes);
    }

    /**
     * Returns the current size of the read buffer.
     * @return
     */
    public int getReadBufferSize(){
        return this.readBufferSize;
    }

    /**
     * Get the current buffer
     * @return
     */
    public byte[] getReadBuffer(){
        return this.readBuffer;
    }

    /**
     * Thread responsible for reading data from the serial port.
     */
    private class SerialReadThread extends Thread {

        private static final String TAG = "SerialReadThread";

        public SerialReadThread(){
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            while (!isInterrupted()) {
                if (inputStream == null) {
                    return;
                }
                try {
                    int length = inputStream.read(buffer);
                    if(length > 0) {
                        Log.d(TAG, String.format("Read %d bytes", length));

                        // Copy read bytes into buffer
                        System.arraycopy(buffer, 0, readBuffer, readBufferSize, length);
                        readBufferSize+= length;
                        Log.d(TAG, "ReadBuffer=" + DataTools.byteArrayToHex(readBuffer, readBufferSize, true));
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Exception while reading input stream", e);
                    return;
                }
            }
            // Close the input stream when the thread stops
            serialPort.close();
        }
    }
}
