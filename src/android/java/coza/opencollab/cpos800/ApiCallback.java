package coza.opencollab.cpos800;

public interface ApiCallback<T> {
    void success(T parameter);
    void failed(ApiFailure failure);
}
