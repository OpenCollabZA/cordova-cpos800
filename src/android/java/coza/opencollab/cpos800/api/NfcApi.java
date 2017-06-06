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

	private boolean cancelled = false;

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

    public void getCardId(final ApiCallback<byte[]> callback){
        executorService.execute(new Runnable() {
            @Override
            public void run() {
				cancelled = false;
                Log.d(TAG, "getCardId()");
                final SerialManager serialManager = SerialManager.getInstance();
                boolean foundCard = false;
                try {
                    serialManager.openSerialPort(SerialManager.SerialInterface.NFC);
                    int tries = 50, attempts = 0;

                    final byte[] readBuffer = new byte[1024];
                    while(!cancelled && !foundCard && attempts < tries) {
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
					if(cancelled){
						Log.i(TAG, "Reading card got cancelled");
	                    callback.failed(new ApiFailure(ERROR_CANCELLED, "Cancelled reading card"));
					}
					else{
	                    Log.e(TAG, "Exception while trying to read card", e);
	                    callback.failed(new ApiFailure(ERROR_IO, "IO Error while reading card ID"));
					}
                }

                if(!foundCard) {
					if(cancelled){
						Log.i(TAG, "Reading card got cancelled");
	                    callback.failed(new ApiFailure(ERROR_CANCELLED, "Cancelled reading card"));
					}
					else{
	                    Log.i(TAG, "Timeout reading Tag");
	                    callback.failed(new ApiFailure(ERROR_TIMEOUT, "Timeout reading Tag"));
					}
                }
            }
        });


    }
}
