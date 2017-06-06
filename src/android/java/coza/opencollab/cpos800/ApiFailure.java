package coza.opencollab.cpos800;

import org.json.JSONObject;
import org.json.JSONException;

public class ApiFailure{

	private final int code;
	private final String message;

	public ApiFailure(final int code, final String message){
		this.code = code;
		this.message = message;
	}

	public String getMessage(){
		return this.message;
	}

	public JSONObject toJsonObject(){
		try{
			JSONObject json = new JSONObject();
			json.accumulate("code", code);
			json.accumulate("message", message);
			return json;
		}catch(JSONException e){
			return null;
		}
	}

}
