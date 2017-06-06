package coza.opencollab.cpos800;

/**
 * Created by charl on 2017/06/01.
 */

public class DataTools {

    public static String byteArrayToHex(byte[] a) {
       return byteArrayToHex(a, a.length);
    }

    public static String byteArrayToHex(byte[] a, int length) {
        return byteArrayToHex(a, length, false);
    }

    public static String byteArrayToHex(byte[] a, int length, boolean spacer) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(int i = 0 ; i < length ; i++) {
            sb.append(String.format("%02x", a[i]));
            if(spacer && i < length - 1){
                sb.append(":");
            }
        }
        return sb.toString();
    }

}
