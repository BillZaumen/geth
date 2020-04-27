package org.bzdev.swing;

import javax.swing.text.Document;
import javax.swing.JOptionPane;
import java.util.ResourceBundle;
import javax.swing.InputVerifier;
import javax.swing.JComponent;


/**
 * Text Field that accepts values in a range suitable for a TCP
 * or UDP port.
 * To indicate if TCP or UDP is being used, call the method
 * {@link PortTextField#setTCP(boolean)}.
 * <P>
 * The class {@link VTextField} describes most of the usage of
 * this class.
 *
 * @author Bill Zaumen
 * @version $Revision: 1.4 $, $Date: 2005/06/06 06:49:16 $
 */

public class PortTextField extends WholeNumbTextField {
    static final int MIN = 1;
    static final int MAX = 65535;

    static private final String resourceBundleName =
	"org.bzdev.swing.lpack.PortTextField";
    static ResourceBundle bundle = 
	ResourceBundle.getBundle(resourceBundleName);
    static String localeString(String name) {
	return bundle.getString(name);
    }


    String portname = null;
    /**
     * Set a name of the port to use in error dialog boxes (the default
     *     name is TCP Port)
     * @param name The name to display; null for the default
     */
    public void setPortName(String name) {portname = name;}
    
    String proto = localeString("setTCP");

    /**
     * Set the protocol (TCP or UDP)
     * @param value true if the string to use in a dialog is
     *        "TCP Port" and false if it is "UDP Port", both in the
     *        default locale
     */
    public void setTCP(boolean value) {
	proto = value? localeString("setTCP"): localeString("setUDP");
	    
    }

    /**
     * Check that the contents are valid.
     * The text should either be empty or it should be a
     * base-10 positive integer between 1 and 65535 (2^16-1)
     * inclusive.
     * @param text the contents of the text field
     * @return true if valid; false otherwise
     */
    private boolean acceptText(String text) {
	if (text.length() == 0) return getAllowEmptyTextField();
	try {
	    int val = Integer.parseInt(text);
	    return (val >= MIN && val <= MAX);
	} catch (Exception e) {
	    return false;
	}
    }

    private void init() {
	setInputVerifier(new InputVerifier() {
		public boolean verify(JComponent input) {
		    VTextField tf = (VTextField) input;
		    return acceptText(tf.getText());
		}
		public boolean shouldYieldFocus(JComponent input,
						JComponent target)
		{
		    return true;
		}
	    });
    }

    /**
     * Handle an input error.
     * An error is handled by creating a dialog box prompting
     * for a legal value.
     */
    protected boolean handleError() {
	String svalue;
	int port;
	do {
	    String range = getAllowEmptyTextField()?
		localeString("rangeWithBlank"):
		localeString("range");

	    svalue = JOptionPane.showInputDialog(this,
						 ((portname == null)? 
						  (proto +" " +range):
						  portname));

	    if (svalue == null) {
		return false;
	    }
	    if (svalue.equals("")) {
		if (getAllowEmptyTextField()) {
		    setText(svalue);
		    return true;
		} else {
		    continue;
		}
	    }
	    port = Integer.parseInt(svalue);
	    if (port < MIN || port > MAX) {
		continue;
	    } else {
		setText(svalue);
		return true;
	    }
	} while (true);
    }


    /**
     * Class constructor.
     */
    public PortTextField() {
	super();
    }

    /**
     * Class constructor for a document model, initial string and field size.
     * @param doc the document model
     * @param text the initial text
     * @param ncols the number of columns in the text field.
     */
    public PortTextField(Document doc, String text, int ncols) {
	super(doc, text, ncols);
	init();
    }

    /**
     * Class constructor specifying the field size..
     * @param ncols the number of columns in the text field.
     */
    public PortTextField(int ncols) {
	super(ncols);
	init();
    }

    /**
     * Class constructor giving an initial string.
     * @param text the initial text
     */
    public PortTextField(String text) {
	super(text);
	init();
    }
    
    /**
     * Class constructor giving an initial string and field size.
     * @param text the initial text
     * @param ncols the number of columns in the text field.
     */
    public PortTextField(String text, int ncols) {
	super(text, ncols);
	init();
    }
}
