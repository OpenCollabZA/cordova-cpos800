package coza.opencollab.cpos800;

public interface ApiPrintingCallback {
    void success();
    void failed(ApiFailure failure);
}
