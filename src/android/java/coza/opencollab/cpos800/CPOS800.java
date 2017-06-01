package coza.opencollab.cpos800;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

public class CPOS800 extends CordovaPlugin {

	private static final String ACTION_READ_TAG_ID = "readTagId";

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

		if (ACTION_READ_TAG_ID.equals(action)) {
			callbackContext.success("SAMPLE ID");
			return true;
		}
		return false;
	}

}
