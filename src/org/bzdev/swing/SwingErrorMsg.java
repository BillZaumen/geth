package org.bzdev.swing;
import java.util.ResourceBundle;
import org.bzdev.util.SafeFormatter;

class SwingErrorMsg {

    private static ResourceBundle exbundle = ResourceBundle.getBundle
	("org.bzdev.swing.lpack.Swing");

    static String errorMsg(String key, Object... args) {
	return (new SafeFormatter()).format(exbundle.getString(key), args)
	    .toString();
    }
}
