package coza.opencollab.cpos800.cordova;
import coza.opencollab.cpos800.ApiCallback;
import coza.opencollab.cpos800.ApiFailure;

public class NoopApiCallback<T> implements ApiCallback<T> {

	public void success(T parameter){}
	public void failed(ApiFailure failure){}
}
