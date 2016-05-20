package com.qmuzik.finclude.printer;

import java.io.IOException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

public class FincludePrinter extends CordovaPlugin {

	private static final String ACTION_IS_ENABLED = "isEnabled";
	private static final String ACTION_PRINT_TEXT = "printText";

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

		if (ACTION_IS_ENABLED.equals(action)) {
			boolean isEnabled = isEnabled();
			callbackContext.success(Boolean.toString(isEnabled));
			return true;
		}

		else if(ACTION_PRINT_TEXT.equals(action)){
			try {
				printText(args.getString(0));
				callbackContext.success();
			}
			catch(IOException ioe){
				callbackContext.error("Failed to parse arguments");
			}
			return true;
		}

		return false;
	}

	private boolean isEnabled(){
		return true;
	}

	private void printText(String text) throws IOException, JSONException {
		System.out.println("Printing: " + text);
	}

}
