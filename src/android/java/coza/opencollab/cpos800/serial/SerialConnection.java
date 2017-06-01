package coza.opencollab.cpos800.serial;

import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import android_serialport_api.SerialPort;
import coza.opencollab.cpos800.DataTools;

/**
 * Created by charl on 2017/05/31.
 */

public class SerialConnection {


    /**
     * Tag for logging.
     */
    private static final String TAG = "SerialConnection";

    /**
     * Reference tot the currently open serial port
     */
    private final SerialPort serialPort;

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
     * Creates a new instance of the <code>SerialConnection</code>.
     * @param serialPort
     */
    public SerialConnection(SerialPort serialPort){
        this.serialPort = serialPort;
        this.readThread = new SerialReadThread(this.serialPort.getInputStream());
        this.readThread.start();
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
     * Close the serial connection
     */
    public void close(){
        this.readThread.interrupt();
        this.serialPort.close();
    }

    /**
     * Reset the read buffer to the start
     */
    private void resetReadBuffer(){
        Log.d(TAG, "Reset read buffer to start");
        readBufferSize = 0;
    }

    /**
     * Writes a new command over the serial port
     * @param bytes
     * @throws IOException
     */
    public void write(byte[] bytes) throws IOException {
        resetReadBuffer();
        serialPort.getOutputStream().write(bytes);
    }

    /**
     * Thread responsible for reading data from the serial port.
     */
    private class SerialReadThread extends Thread {

        private static final String TAG = "SerialReadThread";

        /**
         * Reference to the input stream we are reading from
         */
        private final InputStream inputStream;


        public SerialReadThread(InputStream inputStream){
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            while (!isInterrupted()) {

                try {
                    int length = inputStream.read(buffer);
                    if(length > 0) {
                        Log.d(TAG, String.format("Read %d bytes", length));

                        // Copy read bytes into buffer
                        System.arraycopy(buffer, 0, readBuffer, readBufferSize, length);
                        readBufferSize+= length;
                        Log.d(TAG, "ReadBuffer=" + DataTools.byteArrayToHex(readBuffer, readBufferSize));
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Exception while reading input stream", e);
                }
            }
            // Close the input stream when the thread stops
            try {
                this.inputStream.close();
            } catch (IOException e) {
            }
        }
    }
}
