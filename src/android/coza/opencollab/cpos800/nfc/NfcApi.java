package coza.opencollab.cpos800.nfc;

import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import coza.opencollab.cpos800.DataTools;
import coza.opencollab.cpos800.serial.SerialManager;
import coza.opencollab.cpos800.AsyncCallback;

/**
 * API to work with the NFC
 */
public class NfcApi {

    private static final String TAG = "NfcApi";
    private static final byte[] CMD_GET_ID = {0x08, 0x00, 0x01, 0x01, (byte)0xe3};
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static NfcApi instance;

    public static NfcApi getInstance(){
        if(instance == null){
            instance = new NfcApi();
        }
        return instance;
    }

    public void getCardId(final AsyncCallback<byte[]> callback){
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "getCardId()");
                final SerialManager serialManager = SerialManager.getInstance();
                boolean foundCard = false;
                try {
                    serialManager.openSerialPort();
                    int tries = 100, attempts = 0;

                    final byte[] readBuffer = new byte[1024];
                    while(!foundCard && attempts < tries) {
                        attempts++;
                        SystemClock.sleep(100);
                        serialManager.write(CMD_GET_ID);
                        int length = serialManager.read(readBuffer, 3000, 100);
                        if (length == 4 && readBuffer[0]==8 && readBuffer[1] == 1 && readBuffer[3]==4) {
                            // No card read
                        } else {
                            // Copy the data to a new buffer that only contains the read bytes
                            byte[] data = new byte[length];
                            System.arraycopy(readBuffer, 0, data, 0, length);
                            callback.success(data);
                            foundCard = true;
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Exception while trying to read card", e);
                    callback.failed(e);
                }

                if(!foundCard) {
                    Log.i(TAG, "Never got a tag in time");
                    callback.failed(null);
                }
            }
        });


    }
}
