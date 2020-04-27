package org.bzdev.swing;

import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.InputVerifier;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.Toolkit;
import java.awt.event.FocusListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.text.Document;
import javax.swing.text.AbstractDocument;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

//@exbundle org.bzdev.swing.lpack.Swing

 /**
  * Verified Text field.
  * This class implements a text field that handles invalid text
  * according to an error policy and that calls a method
  * {@link VTextField#onAccepted()} when text is valid and
  * either the focus changes or the text was changed by calling
  * {@link VTextField#setText(String)}, or when the user presses a
  * key such as the "Enter"/"Return" key. The method
  * {@link VTextField#handleError()} is called
  * when input verification fails after the keyboard
  * focus is lost, but not after {@link VTextField#setText(String)} is called
  * and input verification fails.
  * The default behavior for {@link VTextField#handleError()} (if
  * there is an error) simply restores the previous value and (if
  * possible) keeps the keyboard focus on the text field.
  * <P>
  * To configure this text field to verify its contents, one must
  * call the method {@link VTextField#setInputVerifier(InputVerifier)}.
  * To restrict the input to particular characters, this text field's
  * document should be a subclass of {@link AbstractDocument} and the
  * document's
  * {@link AbstractDocument#setDocumentFilter(javax.swing.text.DocumentFilter)}
  * should be called. If a document filter is used, illegal characters
  * must not be included in a string passed to
  * {@link JTextField#setText(String)} as the the new text will be
  * ignored. Characters that will be illegal once a document filter
  * is installed can be passed to a contructor, but doing this can
  * result in unexpected behavior if {@link JTextField#getText()} is
  * called and that text is reused in calls to
  * {@link JTextField#setText(String)}. Consequently, it is advisable
  * for strings passed to the constructor to contain only characters
  * that will be allowable after the document filter is installed.
  *
  * @author Bill Zaumen
  */

 public class VTextField extends JTextField {

     static String errorMsg(String key, Object... args) {
	 return SwingErrorMsg.errorMsg(key, args);
     }

     /**
      * Error Policy.
      * These options determine how validation errors are handled.
      */
     public enum ErrorPolicy {
	 /**
	  * Indicates that the new value in the text field should be
	  * preserved so that the user can edit it. An invalid new
	  * value will not be returned by
	  * {@link VTextField#getValidatedText()} or used by
	  * {@link VTextField#fireActionPerformed()}.
	  */
	 USE_NEW_VALUE, 
	 /**
	  * Indicates that an erroneous new value should be replaced with
	  * the old value so the user can start over from a known value.
	  */
	 USE_OLD_VALUE,
	 /**
	  * Indicates that an erroneous new value should be completely
	  * removed.
	  */
	 CLEAR_VALUE
     };

     ErrorPolicy errorPolicy = ErrorPolicy.USE_NEW_VALUE;

     /**
      * Set the error policy
      * @param ep the error policy
      * @exception IllegalStateException empty text fields allowed, but
           the argument was ErrorPolicy.CLEAR_VALUE
      * @see VTextField.ErrorPolicy
      */
     public void setErrorPolicy(ErrorPolicy ep) throws IllegalStateException {
	 if (!allowEmptyTF && ep == ErrorPolicy.CLEAR_VALUE) {
	     throw new IllegalStateException(errorMsg("errorPolicy"));
	 }
	 errorPolicy = ep;
     }

     /**
      * Handle an error.
      * If this method is overridden it should return true only
      * if it has attempted to correct the error. Correcting the
      * error will typically require calling {@link #setText(String)}
      * with the corrected value (this method will check its argument).
      * @return return true if a value has been provided; false 
      *        to keep the old value.
      * @see #setText(String)
      */
     protected boolean handleError() {return false;}

     /**
      * Accept input.
      * This method will be called when input is accepted, either
      * by terminating the input with the "Enter" or "Return" key,
      * or by changing the focus to another component in this
      * application. Note that selecting a window in a separate
      * application may not trigger this method.
      * <P>
      * The default method does nothing, but may be overridden
      * to perform some operation when the value of the text field 
      * changes to a new accepted value. During error correction,
      * this may happen multiple times.  It can also happen if
      * the method {@link #setText(String)} is called.
      * While this method can throw an exception, it is called
      * in such a way that the exception will normally be caught
      * by this class' methods and processed silently if at all.
      * <P>
      * When an instance of this class is constructed and the
      * text field is empty, an initial call to this method is
      * suppressed.
      * @exception Exception an error occurred
      */
     protected void onAccepted() throws Exception {}

     // prevent recursive calls, so we can set the text to
     // during a call to onAccepted.
     private boolean onAcceptedCalled = false;
     private void doOnAccepted() {
	 try {
	     if (onAcceptedCalled == false) {
		 onAcceptedCalled = true;
		 onAccepted();
	     }
	 } catch (Exception e) {
	 } finally {
	     onAcceptedCalled = false;
	 }
     }

     private String oldvalue = "";

     private void doSetText(String str) {
	 try {
	     super.setText(str);
	     onAccepted();
	     oldvalue = str;
	 } catch (Exception e) {
	     super.setText(oldvalue);
	 }
     }

     /**
      * Set the text field if the field would be accepted.
      * As a special case, when {@link #getAllowEmptyTextField()}
      *  returns <CODE>true</CODE>the text may be set to an empty
      * string in order to clear the field.  Otherwise, if
      * the input is valid, {@link #onAccepted()} will be called,
      * but if the text is not valid, the existing value will
      * be preserved. The method {@link #onAccepted()} must handle
      * the case where the text field is empty when
      * {@link #getAllowEmptyTextField()} returns <CODE>true</CODE>.
      * <P>
      * If the input is not valid, as determined by the value of
      * {@link #getAllowEmptyTextField()} or by an input verifier,
      * the previous value is preserved.
      * <P>
      * While the method {@link onAccepted()} is being called,
      * this method simply sets the text with no constraints on
      * its value. In this case, the caller must ensure that the
      * argument is a valid one.
      * @param str the value for the string.
      * @exception IllegalArgumentException The input is not valid
      */
     @Override
     synchronized public void setText(String str) {
	 if (onAcceptedCalled) {
	     // no constraints or manipulations of old values
	     // when onAccepted was called.
	     super.setText(str);
	     return;
	 }
	 if (str.equals("")) {
	     if (allowEmptyTF) {
		 super.setText(str);
		 oldvalue = "";
		 doOnAccepted();
	     } else {
		 throw new IllegalArgumentException("emptyStringNotValid");
	     }
	 } else {
	     if (providedInputVerifier != null) {
		 String existing = getText();
		 super.setText(str);
		 if (providedInputVerifier.verify(this)) {
		     oldvalue = str;
		     doOnAccepted();
		 } else {
		     super.setText(existing);
		     throw new IllegalArgumentException
			 (errorMsg("stringNotValid", str));
		 }
	     } else {
		 super.setText(str);
		 oldvalue = str;
		 doOnAccepted();
	     }
	 }
     }

     private boolean allowEmptyTF = true;

     /**
      * Set whether or not empty text fields are allowed.
      * The default setting is <CODE>true</CODE>.
      * This method affects how text is accepted. Fields may be
      * temporarily empty while editing them. In addition, a field
      * may be empty when an instance of this class is constructed,
      * in which case {@link #onAccepted()} will not be called until
      * the text is changed.
      * @param value true if empty text fields are allowed; false otherwise
      */
     public void setAllowEmptyTextField(boolean value)
	 throws IllegalStateException
     {
	 if (!value && errorPolicy == ErrorPolicy.CLEAR_VALUE) {
	     throw new IllegalStateException(errorMsg("errorPolicy"));
	 }
	 allowEmptyTF = value;
     }

     /**
      * Determine if empty text fields are allowed.
      * @return true if empty text fields are allowed; false otherwise.
      */
     public boolean getAllowEmptyTextField() {
	 return allowEmptyTF;
     }

     /**
      * Get the text field.
      * This does a validity test and if the text is not valid, the
      * old value is used. If handleError is overridden, the value
      * may be corrected if not valid.
      * <P>
      * The method {@link #onAccepted()} will be called if the input
      * is valid. If this method is called when the text field is empty
      * and when {@link #getAllowEmptyTextField()} returns <CODE>false</CODE>,
      * {@link #handleError()} will be called.
      * @return the text displayed if it is valid; otherwise the text
      *         before the last editing operation.
      */
     synchronized public String getValidatedText() {
	 String svalue = getText();
	 boolean error = false;
	 if (svalue.length() == 0) {
	     if (allowEmptyTF) {
		 oldvalue = svalue;
		 doOnAccepted();
	     } else {
		 error = true;
	     }
	 } else if (providedInputVerifier == null
	     || providedInputVerifier.verify(this)) {
	     oldvalue = svalue;
	     doOnAccepted();
	 } else {
	     error = true;
	 }
	 if (error) {
	     while (true) {
		 if (handleError()) {
		     svalue = getText();
		     if (svalue.length() == 0) {
			 if (allowEmptyTF) {
			     oldvalue = svalue;
			     doOnAccepted();
			     break;
			 }
		     } else if (providedInputVerifier == null
			 || providedInputVerifier.verify(this)) {
			 oldvalue = svalue;
			 doOnAccepted();
			 break;
		     }
		 } else {
		     switch (errorPolicy) {
		     case USE_NEW_VALUE:
			 break;
		     case USE_OLD_VALUE:
			 setText(oldvalue);
			 break;
		     case CLEAR_VALUE:
			 setText("");
			 break;
		     }
		     break;
		 }
	     }
	 }
	 return oldvalue;
     }

     @Override
     protected void fireActionPerformed() {
	 String svalue = getText();
	 boolean error = false;
	 if (svalue.length() == 0) {
	     if (allowEmptyTF) {
		 oldvalue = svalue;
		 doOnAccepted();
		 super.fireActionPerformed();
	     } else {
		 error = true;
	     }
	 }
	 if (providedInputVerifier == null
	     || providedInputVerifier.verify(this)) {
	     oldvalue = svalue;
	     doOnAccepted();
	     super.fireActionPerformed();
	 } else {
	     error = true;
	 }
	 if (error) {
	     while (true) {
		 if (handleError()) {
		     svalue = getText();
		     error = false;
		     if (svalue.length() == 0) {
			 if (allowEmptyTF) {
			     oldvalue = svalue;
			     doOnAccepted();
			     super.fireActionPerformed();
			     break;
			 } else {
			     error = true;
			 }
		     } else if (providedInputVerifier == null
			 || providedInputVerifier.verify(this)) {
			 oldvalue = svalue;
			 doOnAccepted();
			 super.fireActionPerformed();
			 break;
		     } else {
			 error = true;
		     }
		     if (error) {
			 switch (errorPolicy) {
			 case USE_NEW_VALUE:
			     break;
			 case USE_OLD_VALUE:
			     setText(oldvalue);
			     break;
			 case CLEAR_VALUE:
			     setText("");
			     break;
			 }
			 requestFocus();
		     }
		 } else {
		     switch (errorPolicy) {
		     case USE_NEW_VALUE:
			 break;
		     case USE_OLD_VALUE:
			 setText(oldvalue);
			 break;
		     case CLEAR_VALUE:
			 setText("");
			 break;
		     }
		     requestFocus();
		     break;
		 }
	     }
	 }
     }
     
     // do-nothing action listener - we need one at all times because
     // fireActionPerformed is not called by swing when there are no
     // action listeners present.
     private ActionListener ourActionListener = new ActionListener() {
	     public void actionPerformed(ActionEvent e) {
	     }
	 };

     @Override
     public void removeActionListener(ActionListener listener) {
	 if (listener != ourActionListener) {
	     super.removeActionListener(listener);
	 }
     }

     private InputVerifier providedInputVerifier = null;

     /**
      * Set the input verifier.
      * This should be used to determine the validity of the
      * text field's contents.  An internal input verifier is
      * actually used and that input verifier uses the one
      * set by this method, with the internal verifier handling
      * some aspects of a change of focus.
      *
      * @param verifier the input verifier; null if the input
      *        should always be accepted.
      */
     @Override
     public void setInputVerifier(InputVerifier verifier) {
	 providedInputVerifier = verifier;
     }

     private InputVerifier ourInputVerifier = new InputVerifier() {
	     String svalue;
	     public boolean verify(JComponent input) {
		 return (providedInputVerifier == null)
		     || providedInputVerifier.verify(input);
	     }
	     public boolean shouldYieldFocus(JComponent input,
					     JComponent target)
	     {
		 boolean result = super.shouldYieldFocus(input, target);
		 if (result == true) {
		     String svalue = getText();
		     oldvalue = svalue;
		     doOnAccepted();
		     return (providedInputVerifier == null)
			 || providedInputVerifier.shouldYieldFocus(input,
								   target);
		 } else {
		     while (true) {
			 if (handleError()) {
			     svalue = getText();
			     if (verify(input)) {
				 doOnAccepted();
				 oldvalue = svalue;
				 return (providedInputVerifier == null) ||
				     providedInputVerifier.shouldYieldFocus
				     (input, target);
			     } 
			 } else {
			     switch (errorPolicy) {
			     case USE_NEW_VALUE:
				 requestFocus();
				 break;
			     case USE_OLD_VALUE:
				 setText(oldvalue);
				 break;
			     case CLEAR_VALUE:
				 setText("");
				 break;
			     }
			     return false;
			 }
		     } 
		 }
	     }
	 };

     private void init() {
	 String text = super.getText();
	 if (text != null && text.length() != 0) {
	     doOnAccepted();
	 }
	 // do-nothing action listener - fireActionPerformed is not called
	 // if there are no action listeners.  We want one to be always
	 // present, so we provide one that simply returns.
	 addActionListener(ourActionListener);
	 super.setInputVerifier(ourInputVerifier);
    }

    /**
     * Class constructor specifying the field size..
     * @param ncols the number of columns in the text field.
     */
    public VTextField(int ncols) {
	super(ncols);
	init();
    }

    /**
     * Class constructor.
     */
    public VTextField() {
	super();
	init();
    }

    /**
     * Class constructor giving an initial string.
     * @param text the initial text
     */
    public VTextField(String text) {
	super();
	init();
	if (text != null && text.length() != 0) {
	    setText(text);
	}
    }

    /**
     * Class constructor giving an initial string and field size.
     * @param text the initial text
     * @param ncols the number of columns in the text field.
     */
    public VTextField(String text, int ncols) {
	super(ncols);
	init();
	if (text != null && text.length() != 0) {
	    setText(text);
	}
    }

    /**
     * Class constructor for a document model, initial string and field size.
     * @param doc the document model
     * @param text the initial text
     * @param ncols the number of columns in the text field.
     */
    public VTextField(Document doc, String text, int ncols) {
	super(doc, "", ncols);
	init();
	if (text != null && text.length() != 0) {
	    setText(text);
	}
    }
}
