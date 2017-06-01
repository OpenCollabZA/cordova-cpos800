package coza.opencollab.cpos800;

/**
 * Created by charl on 2017/06/01.
 */

public interface AsyncCallback<T> {

    void success(T parameter);

    void failed(Throwable t);
}
