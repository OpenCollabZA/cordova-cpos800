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

public class CPOS800 extends CordovaPlugin {

	private static final String TAG = "CPOS800";
	private static final String ACTION_READ_TAG_ID = "readTagId";

	@Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

		if (ACTION_READ_TAG_ID.equals(action)) {
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
		return false;
	}

}
