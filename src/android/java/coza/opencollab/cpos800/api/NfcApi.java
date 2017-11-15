package coza.opencollab.cpos800.api;

import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import coza.opencollab.cpos800.serial.SerialManager;
import coza.opencollab.cpos800.ApiCallback;
import coza.opencollab.cpos800.ApiFailure;
import coza.opencollab.cpos800.DataTools;

/**
 * API to work with the NFC
 */
public class NfcApi {

    private static final String TAG = "NfcApi";
    private static final byte[] CMD_GET_ID = {0x08, 0x00, 0x01, 0x01, (byte)0xe3};
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Error code when there was a timeout waiting for a tag to be read.
     */
    private static final int ERROR_TIMEOUT = 1;

    /**
     * Error code when the user cancelled reading a tag while we where still waiting
     * for a tag.
     */
    private static final int ERROR_CANCELLED = 2;

    /**
     * IO Exception trying to read card id.
     */
    private static final int ERROR_IO = 3;

    private static NfcApi instance;

    private volatile boolean cancelled = false;

    public static NfcApi getInstance(){
        if(instance == null){
            instance = new NfcApi();
        }
        return instance;
    }

    public void cancel(final ApiCallback<String> callback){
        this.cancelled = true;
        callback.success("cancelled");
    }

    private boolean testValidSerial(byte[] data){
        for (byte aData : data) {
            if (aData != 0) {
                return true;
            }
        }
        return false;
    }

    public void getCardId(final ApiCallback<byte[]> callback){
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                cancelled = false;
                Log.d(TAG, "getCardId()");
                final SerialManager serialManager = SerialManager.getInstance();
                boolean foundCard = false;
                int length = 0; // Length of the data that we have read
                final byte[] readBuffer = new byte[1024];
                try {
                    serialManager.openSerialPort(SerialManager.SerialInterface.NFC);
                    int tries = 50, attempts = 0;
                    SystemClock.sleep(100);
                    while(!cancelled && attempts < tries) {
                        attempts++;
                        serialManager.write(CMD_GET_ID);
                        length = serialManager.read(readBuffer, 500, 100);
                        if (length < 4 || (length == 4 && readBuffer[0]==8 && readBuffer[1] == 1 && readBuffer[3]==4)) {
                            length = 0;
                        }
                        else{
                            break;
                        }
                        // No card read
                    }
                } catch (IOException e) {
                    if(cancelled){
                        Log.i(TAG, "Reading card got cancelled");
                        callback.failed(new ApiFailure(ERROR_CANCELLED, "Cancelled reading card"));
                    }
                    else{
                        Log.e(TAG, "Exception while trying to read card", e);
                        callback.failed(new ApiFailure(ERROR_IO, "IO Error while reading card ID"));
                    }
                }
                finally{
                    serialManager.closeSerialPort();
                }

                if(cancelled){
                    Log.i(TAG, "Reading card got cancelled");
                    callback.failed(new ApiFailure(ERROR_CANCELLED, "Cancelled reading card"));
                    return;
                }

                // We read way to many bytes to be a legit card serial
                if(length > 8){
                    Log.e(TAG, "Serial returned too much data to be a serial number: " + length);
                    callback.failed(new ApiFailure(ERROR_IO, "IO Error while reading card ID - invalid serial data."));
                    return;
                }

                if(length < 4){
                    Log.i(TAG, "Timeout reading Tag");
                    callback.failed(new ApiFailure(ERROR_TIMEOUT, "Timeout reading Tag"));
                    return;
                }

                // Copy the data to a new buffer that only contains the read bytes
                byte[] data = new byte[length];
                System.arraycopy(readBuffer, 0, data, 0, length);

                if(testValidSerial(data)){
                    callback.success(data);
                }
                else{
                    Log.i(TAG, "Invalid card serial");
                    callback.failed(new ApiFailure(ERROR_IO, "Invalid card serial"));
                }
            }
        });


    }
}
