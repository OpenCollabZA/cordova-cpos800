package coza.opencollab.cpos800;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import coza.opencollab.cpos800.nfc.NfcApi;
import coza.opencollab.cpos800.DataTools;
import coza.opencollab.cpos800.AsyncCallback;

public class CPOS800Plugin extends CordovaPlugin {

	private static final String TAG = "CPOS800";
	private static final String EXEC_GET_CARD_ID = "getCardId";
	private static final String EXEC_CANCEL_CARD_ID = "cancelReadTagId";

	@Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

		if (EXEC_GET_CARD_ID.equals(action)) {
			NfcApi.getInstance().getCardId(new AsyncCallback<byte[]>() {
				@Override
				public void success(final byte[] parameter) {
					String serial = DataTools.byteArrayToHex(parameter);
					Log.d(TAG, "Got card  " + serial);
					callbackContext.success(serial);
				}

				@Override
				public void failed(final Throwable t) {
					Log.d(TAG, "Exception while trying to read card", t);
					callbackContext.success("Failed");
				}
			});
			return true;
		}
		else if (EXEC_CANCEL_CARD_ID.equals(action)) {
			callbackContext.success("");
			return true;
		}
		return false;
	}

}
