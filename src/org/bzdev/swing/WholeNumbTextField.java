package org.bzdev.swing;

import javax.swing.JTextField;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.text.Document;
import javax.swing.text.AbstractDocument;
import org.bzdev.swing.text.CharDocFilter;

//@exbundle org.bzdev.swing.lpack.Swing

/**
 * Text field constrained to allow only non-negative integers.
 *
 * @author Bill Zaumen
 */
public class WholeNumbTextField extends VTextField {
    
    static String errorMsg(String key, Object... args) {
	return SwingErrorMsg.errorMsg(key, args);
    }

    private int defaultValue = 0;
    private int value;

    private CharDocFilter cdf = new
	CharDocFilter() {
	    protected boolean allowedChar(char ch) {
		return Character.isDigit(ch);
	    }
	};

    /**
     * Accept input.
     * This method will be called when input is accepted, either
     * by terminating the input with the "Enter" or "Return" key,
     * or by changing the focus to another component in this
     * application. Note that selecting a window in a separate
     * application may not trigger this method.
     * <P>
     * During error correction, this method may be called
     * multiple times.  That can also happen if
     * the method {@link #setText(String)} is called.
     * While this method can throw an exception, it is called
     * in such a way that the exception will normally be caught
     * by this class' methods and processed silently if at all.
     * <P>
     * Note: if a subclass implements this method, it must call
     * super.onAccepted().  If the text field is empty, the
     * corresponding value is the default value, which can be set by
     * calling {@link #setDefaultValue(int)}.  The default value may
     * be a negative number (e.g., to indicate that the field has not
     * been set). Otherwise the text field's value (as an int) is
     * used.
     * @exception Exception an error occurred
     */
    protected synchronized void onAccepted() throws Exception {
	String text = getText();
	if (text.length() == 0) {
	    value = defaultValue;
	} else {
	    value = Integer.parseInt(text);
	}
    }

    /**
     * Set the value of the text field.
     * The value must be non-negative. If a subclass imposes additional
     * constraints on the value, it should override this method,
     * perform a test to ensure that the value is allowed,
     * throw an {@link IllegalArgumentException} if the test fails,
     * and then call <CODE>super.setValue(value)</CODE>.
     * @param value the text-field's value.
     * @throws IllegalArgumentException the argument is out of range.
     */
    public synchronized void setValue(int value)
	throws IllegalArgumentException
    {
	if (value < 0)
	    throw new IllegalArgumentException
		(errorMsg("valueNegative", value));
	setText(Integer.toString(value));
	// redundant but doesn't hurt.
	this.value = value;
    }

    /*
     * *** REMOVED - never used and not necessary. It was intended to
     * *** handle the empty string case but could be misused.
     * Set the value of the text field.
     * If a subclass adds further restrictions on the value, it
     * should preform a test to see if those restrictions are
     * satisfied and should then call
     * <CODE>super.setValue(value,text)</CODE>.
     * @param value the text-field's value.
     * @param text the text to display.
     * @throws IllegalArgumentException the argument is out of range.
    protected synchronized void setValue(int value, String text)
	throws IllegalArgumentException
    {
	if (value < 0)
	    throw new IllegalArgumentException
		(errorMsg("valueNegative", value));
	setText(text);
	this.value = value;
    }
     */

    /**
     * Get the text field's value.
     * The value returned will not reflect any changes in the text field
     * while it is being edited: the text field's value is updated when
     * a loss of focus is detected or when the user explicitly uses the
     * Return/Enter key.
     * @return the value of the text field, or the default value
     *         when the text field is empty
     * @see #setDefaultValue(int)
     */
    synchronized public int getValue() {
	// String str = mustValidate()? getValid atedText(): getText();
	/*
	String str = getText();
	if (str.equals("")) {
	    if (acceptEmpty ZTF) {
		return (defaultValue);
	    } else {
		// getValidatedText();
		return value;
	    }
	}
	*/
	return value;
    }

    /**
     * Set the default value when the text-string is empty.
     * The value may be any integer, although legal values will
     * always be non-negative. This allows getValue to return a negative
     * value as an indication that the field is empty: the default
     * value can be any int.
     * @param value the default value to use
     */
    public void setDefaultValue(int value) {defaultValue = value;}

    /**
     * Class constructor.
     */
    public WholeNumbTextField() {
	super();

    }

    /**
     * Class constructor for a document model, initial string and field size.
     * @param doc the document model (must be an AbstractDocument)
     * @param text the initial text
     * @param ncols the number of columns in the text field.
     */
    public WholeNumbTextField(Document doc, String text, int ncols) {
	super(doc, text , ncols);
	AbstractDocument adoc = (AbstractDocument)doc;
	adoc.setDocumentFilter(cdf);
    }

    /**
     * Class constructor specifying the field size..
     * @param ncols the number of columns in the text field.
     */
    public WholeNumbTextField(int ncols) {
	super(ncols);
	((AbstractDocument)getDocument()).setDocumentFilter(cdf);
    }

    /**
     * Class constructor giving an initial string.
     * @param text the initial text
     */
    public WholeNumbTextField(String text) {
	super(text);
	((AbstractDocument)getDocument()).setDocumentFilter(cdf);
    }

    /**
     * Class constructor giving an initial string and field size.
     * @param text the initial text
     * @param ncols the number of columns in the text field.
     */
    public WholeNumbTextField(String text, int ncols) {
	super(text, ncols);
	((AbstractDocument)getDocument()).setDocumentFilter(cdf);
    }
}
