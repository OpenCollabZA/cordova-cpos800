package coza.opencollab.cpos800.api;

import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import coza.opencollab.cpos800.ApiCallback;
import coza.opencollab.cpos800.ApiFailure;
import coza.opencollab.cpos800.ApiPrintingCallback;
import coza.opencollab.cpos800.DataTools;
import coza.opencollab.cpos800.serial.SerialManager;

/**
 * API to work with the NFC
 */
public class PrinterApi {

    private static final String TAG = "PrinterApi";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final byte[] CMD_INIT_PRINTER = { 0x1B, 0x40 };// Initialize the printer
    private static final byte[] CMD_ALIGN = { 0x1B, 0x61, 0x00 }; // Align command, default is left

    private static final int MAX_DATA_SIZE = 1000;
    /**
     * Error code when there was a timeout waiting for a tag to be read.
     */
    private static final int ERROR_TIMEOUT = 1;

    /**
     * Error code when the user cancelled printing while busy.
     */
    private static final int ERROR_CANCELLED = 2;

    /**
     * IO Exception trying to print
     */
    private static final int ERROR_IO = 3;

    private static PrinterApi instance;

    private boolean cancelled = false;

    public static PrinterApi getInstance(){
        if(instance == null){
            instance = new PrinterApi();
        }
        return instance;
    }

    public void cancel(final ApiCallback<String> callback){
        this.cancelled = true;
        callback.success("cancelled");
    }

    /**
     * Package the instructions to send to the printer
     *
     * @param sendingData Instructions being sent to the printer
     * @return The packaged instructrion
     */
    private static final byte[] packageData(byte[] sendingData) {
        return packageData(sendingData, sendingData.length);
    }
    /**
     * Package the instructions to send to the printer
     *
     * @param sendingData Instructions being sent to the printer
     * @return The packaged instructrion
     */
    private static final byte[] packageData(byte[] sendingData, int len) {
        final byte[] sizeBytes = integerToBytes(len);
        final byte[] header = { (byte) 0xCA, (byte) 0xDF, (byte) 0x00, (byte) 0x35 };

        // Full command that will be returned
        final byte[] cmd = new byte[header.length + 2 + len + 1];

        // Copy the header
        System.arraycopy(header, 0, cmd, 0, header.length);

        // Add the first 2 bytes of the packet size
        cmd[header.length] = sizeBytes[1];
        cmd[header.length + 1] = sizeBytes[0];

        // Now add the data
        System.arraycopy(sendingData, 0, cmd, header.length + 2, len);

        // Add the end of data command
        cmd[cmd.length - 1] = (byte) 0xE3;

        // Return the packaged command
        return cmd;
    }

    /**
     * Convert an integer into 4 bytes
     * @param res
     * @return
     */
    private static final byte[] integerToBytes(int res) {
        byte[] targets = new byte[4];

        targets[0] = (byte) (res & 0xff);
        targets[1] = (byte) ((res >> 8) & 0xff);
        targets[2] = (byte) ((res >> 16) & 0xff);
        targets[3] = (byte) (res >>> 24);
        return targets;
    }

    private void print(final byte[] data) throws IOException {
        // Total length of the data we are sending
        final int length = data.length;
        Log.d(TAG, "print() - Total Length: " + length);
        Log.d(TAG, "print() - Data: " + DataTools.byteArrayToHex(data, true));
        final SerialManager serialManager = SerialManager.getInstance();
        // Buffer for a grouping of data
        byte[] dataGroup = new byte[MAX_DATA_SIZE];
        int dataGroupSize = MAX_DATA_SIZE;
        if(length > MAX_DATA_SIZE){
            // The number of groups of data we are going to send
            int numGroups = length / MAX_DATA_SIZE + (length % MAX_DATA_SIZE == 0 ? 0 : 1);
            Log.d(TAG, "print() - Groups: " + numGroups);
            for (int index = 0; index < numGroups; index++){
                // If it is the last group, and the data remaining data does not fit into a full
                // group, we have to calculate the remaining data to send
                if(index == numGroups - 1 && (length % MAX_DATA_SIZE != 0)){
                    dataGroupSize = length % MAX_DATA_SIZE;
                }
                Log.d(TAG, "print() - Data Group Size: " + dataGroupSize);
                System.arraycopy(data, MAX_DATA_SIZE * index, dataGroup, 0, dataGroupSize);
                Log.d(TAG, "print() - Group Data: " + DataTools.byteArrayToHex(dataGroup, true));
                serialManager.write(packageData(dataGroup, dataGroupSize));
            }

        }
        else{
            serialManager.write(packageData(data));
        }
    }

    public void printText(final String text, final ApiPrintingCallback callback){
        Log.d(TAG, "printing() - text: " + text);
        // Printing must end with a new line
        final String printingText = text.endsWith("\n") ? text : text + "\n";
        Log.d(TAG, "printing() - printingText: " + printingText);

        Log.d(TAG, "printText()");
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                cancelled = false;
                Log.d(TAG, "printing()");
                final SerialManager serialManager = SerialManager.getInstance();
                try {
                    final boolean wasOpen = serialManager.openSerialPort(SerialManager.SerialInterface.PRINTER);

                    // Give the serial port time to settle before start writing
                    if(!wasOpen) {
                        SystemClock.sleep(500);
                        Log.d(TAG, "Setting default alignment");
                        serialManager.write(packageData(CMD_ALIGN));
                        SystemClock.sleep(200);
                    }
                    // Package and write the commands to the serial port
                    print(printingText.getBytes("GBK"));

                    // The printer almost immediately return a 0x02, and only when it is completed
                    // does it return the 0x08
                    final long startTime = System.currentTimeMillis();
                    final int MAX_PRINTING_TIME = 30000; // 30 seconds max time to print the text
                    int readSize = serialManager.getReadBufferSize();
                    boolean isDone = false;
                    while(  // There is still more time allowed to wait for the response
                            System.currentTimeMillis() - startTime < MAX_PRINTING_TIME &&
                                    // We received the 2 bytes we are waiting for
                                    !isDone) {

                        readSize = serialManager.getReadBufferSize();
                        if(readSize >= 2) {
                            isDone = true;
                            final byte[] readBuffer = serialManager.getReadBuffer();
                            if (readBuffer[0] == 2) {
                                if (readBuffer[1] == 1) {
                                    Log.i(TAG, "Printer out of paper");
                                    callback.failed(new ApiFailure(1, "Printer out of paper"));
                                } else if (readBuffer[1] == 4) {
                                    Log.i(TAG, "Printer too hot");
                                    callback.failed(new ApiFailure(1, "Printer too hot"));
                                } else if (readBuffer[1] == 8) {
                                    Log.i(TAG, "Success printing");
                                    callback.success();
                                }
                            } else {
                                String returnCode = DataTools.byteArrayToHex(readBuffer, readSize, true);
                                Log.i(TAG, "Unexpected return code" + returnCode);
                                serialManager.closeSerialPort();
                                callback.failed(new ApiFailure(ERROR_IO, "Unknown printing response code : " + returnCode));
                            }
                        }
                    }
                    // If we got here without being done, we timed out waiting for a response
                    if(!isDone) {
                        String returnCode = DataTools.byteArrayToHex(serialManager.getReadBuffer(), readSize, true);
                        Log.i(TAG, "Unexpected return code" + returnCode);
                        callback.failed(new ApiFailure(ERROR_TIMEOUT, "Unknown printing response code : " + returnCode));
                    }
                } catch (IOException e) {
                    if(cancelled){
                        Log.i(TAG, "Printing got cancelled");
                        callback.failed(new ApiFailure(ERROR_CANCELLED, "Cancelled printing"));
                    }
                    else{
                        Log.e(TAG, "Exception while printing", e);
                        serialManager.closeSerialPort();
                        callback.failed(new ApiFailure(ERROR_IO, "IO Error while printing"));
                    }
                }
                finally{
                    serialManager.closeSerialPort();
                }
            }
        });


    }
}
