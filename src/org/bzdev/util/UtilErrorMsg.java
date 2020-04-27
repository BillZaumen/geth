package org.bzdev.util;
import java.util.ResourceBundle;
import org.bzdev.util.SafeFormatter;

class UtilErrorMsg {

    private static ResourceBundle exbundle = ResourceBundle.getBundle
	("org.bzdev.util.lpack.Util");

    static String errorMsg(String key, Object... args) {
	return (new SafeFormatter()).format(exbundle.getString(key), args)
	    .toString();
    }
}
