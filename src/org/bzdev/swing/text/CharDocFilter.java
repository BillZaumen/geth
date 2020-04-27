package org.bzdev.swing.text;

import javax.swing.text.DocumentFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.Toolkit;

/**
 * Document Filter to implement character-based constraints.
 * This class provides the ability to control which characters
 * can be entered into a text field on a character-by-character
 * basis, and whether the character will be the only character
 * in the text field, a starting character, and ending character,
 * or a character that can appear in any position.  Specific
 * starting or ending characters are optional.  Generally
 * additional constraints on text field will be needed. During
 * editing, however, it is useful to limit the characters that
 * can entered without over-constraining the user: sometimes
 * during editing, the contents of the text field may be
 * temporarily invalid.
 * <p>
 * The class is controlled by several methods:
 * <UL>
 *  <li>The method <code>optSingleChar</code>
 *     determines if a character can be used as the full contents
 *     of a  text field. In a text field representing a number,
 *     for example, one might want to enter '*' to indicate that
 *     any number is suitable.  A character for which this method
 *     returns true must not be one for which <code>optLeadingChar</code>,
 *     <code>allowedChar</code>, or <code>optTrailingChar</code> return
 *     true.
 *  <li>The method <code>mapSingleChar</code> allows one single
 *     character entered to be replaced with another automatically.
 *     for example, if a text field contains the maximum number of
 *     lines to be displayed in a window, then one might want to
 *     use '*' or infinity to indicate that there is no limit,
 *     replacing a '*' with the Unicode character for 'infinity'.
 *  <li>The method <code>optLeadingChar</code> determines if a
 *     character is one that can appear only at the start of a
*     text field.
 *  <li>The method <code>allowedChar</code> determines if a
 *     character is allowed in general (i.e., in any position,
 *     but not before an optional leading character and not
 *     after an optional trailing character.
 *  <li>The method <code>optTrailingChar</code> determines if
 *     a character can be the last character in the text field.
 * </UL>
 * These methods may be subclassed to implement the desired
 * behavior.  Alternatively, one may call a series of methods
 * (<code>setOptSingleChars</code>, <code>setSingleCharMap</code>
 * <code>setOptLeadingChars</code>, <code>setAllowedChar</code>,
 * or <code>setOptTrailingChars</code> to specify the behavior
 * with control strings (when suitable, this will simply localization
 * and internationalization.)
 * <p>
 * This is sufficient for handling cases such as numbers.  An integer
 * may, for example, start with a '-' but otherwise it will just
 * contain digits.  Accountants, however, prefer to surround a number
 * with '(' and ')' to indicate negative numbers.  This class can
 * handle such uses.
 *
 * @see org.bzdev.swing.VTextField
 */

public class CharDocFilter extends DocumentFilter {

    private String optSingleCharString = null;
    private String singleCharMapString = null;
    private String optLeadingCharString = null;
    private String allowedCharString = null;
    private String optTrailingCharString = null;

    private char optSingleCharArray[] = null;
    private char singleCharMapArray[] = null;
    private char optLeadingCharArray[] = null;
    private char allowedCharArray[] = null;
    private char optTrailingCharArray[] = null;

    /**
     * Constructor.
     */
    public CharDocFilter() {
	super();
    }

    /**
     * Set the characters for which <code>optSingleChar</code> will 
     * return true.
     * @param string a string containing the legal characters; null
     *        if optSingleChar should always return false
     */
    public void setOptSingleChars(String string) {
	optSingleCharString = string;
	optSingleCharArray = string.toCharArray();
    }

    /**
     * Set the replacement map for optional single characters.
     * Both the characters and the replacements should be such that
     * <code>optSingleChar(ch)</code> returns true for each character
     * <code>ch</code> in <code>string</code>; otherwise the pair
     * will be ignored when <code>mapSingleChar(ch)</code> is called.
     * This replacement applies only to optional single characters.
     * For example, one might want to allow a user to enter a '*' to
     * indicate an unbounded value and have a text box display the 
     * unicode character representing infinity.
     * @param string a string containing pairs of characters, the
     *        first of a pair being a character to replace and the
     *        second in the pair being the replacement; null if
     *        no replacement is desired.
     */
    public void setSingleCharMap(String string) {
	singleCharMapString = string;
	singleCharMapArray = string.toCharArray();
    }

    /**
     * Set the characters for which optLeadingChar will return true.
     * @param string a string containing the legal characters; null
     *        if optLeadingChar should always return false
     */
    public void setOptLeadingChars(String string) {
	optLeadingCharString = string;
	optLeadingCharArray = string.toCharArray();
    }

    /**
     * Set the characters for which allowedChar will return true.
     * @param string a string containing pairs of legal characters,
     *        each pair delimiting a range of characters for which
     *        if allowedChar should always return true; an empty
     *        string if allowedChar should always return false; a
     *        null string if allowedChar should always return true
     */
    public void setAllowedChars(String string) {
	allowedCharString = string;
	allowedCharArray = string.toCharArray();
    }

    /**
     * Set the characters for which optTrailingChar will return true.
     * @param string a string containing the legal characters; null
     *        if optSingleChar should always return false.
     */
    public void setOptTrailingChars(String string) {
	optTrailingCharString = string;
 	optTrailingCharArray = string.toCharArray();
    }

    /**
     * Check for optional single character.
     * A matching character will be allowed as the only character
     * in the text field, and may be entered only when the text
     * field is empty.
     * @param ch the last character to be placed into the text field
     * @return true if an optional character; false otherwise
     */
    protected boolean optSingleChar(char ch) {
	int i;
	if (optSingleCharArray == null) return false;
	for (i = 0; i < optSingleCharArray.length; i++) {
	    if (ch == optSingleCharArray[i]) return true;
	}
	return false;
    }

    /**
     * Get a replacement character for an optional single character.
     * The replacement character <code>ch</code> must also be one for which
     * <code>optSingleChar(ch)</code> will return true.
     * @param ch the character entered into the text field.
     * @return the replacement character if any; otherwise the original
     *         character passed in the argument
     */
    protected char mapSingleChar (char ch) {
	int i;
	if (singleCharMapArray == null) return ch;
	for (i = 0; i < singleCharMapArray.length-1; i += 2) {
	    if (singleCharMapArray[i] == ch) {
		char newch =  singleCharMapArray[i+1];
		if (optSingleChar(newch)) return newch;
		else return ch;
	    }
	}
	return ch;
    }


    /**
     * Check for optional leading character.
     * An optional leading character must be inserted at
     * the start of a text field, and a character cannot be
     * inserted before an optional leading character.
     * @param ch the last character typed
     * @return true if an optional character; false otherwise
     */
    protected boolean optLeadingChar(char ch) {
	int i;
	if (optLeadingCharArray == null) return false;
	for (i = 0; i < optLeadingCharArray.length; i++) {
	    if (ch == optLeadingCharArray[i]) return true;
	}
	return false;
    }


    /**
     * Check for a legal character.
     * @param ch the last character typed.
     * @return true if an allowed character; false otherwise
     */
    protected boolean allowedChar(char ch) {
	int i;
	if (allowedCharArray == null) return true;
	for (i = 0; i < allowedCharArray.length - 1; i += 2) {
	    if ((ch >= allowedCharArray[i]) && 
		(ch <= allowedCharArray[i+1]))
		return true;
	}
	return false;
    }

    /**
     * Check for an optional trailing character.
     * @param ch the last character typed.
     * @return true if an optional character; false otherwise
     */
    protected boolean optTrailingChar(char ch) {
	int i;
	if (optTrailingCharArray == null) return false;
	for (i = 0; i < optTrailingCharArray.length; i++) {
	    if (ch == optTrailingCharArray[i]) return true;
	}
	return false;
    }

    private boolean ok(String str, 
		       boolean hasLeadingText,
		       boolean hasTrailingText,
		       char first, char last)
    {
	int i;
	char ch;
	int slength = str.length();
	if (hasLeadingText && hasTrailingText) {
	    for (i = 0; i < slength; i++) {
		if (!allowedChar(str.charAt(i))) return false;
	    }
	} else if (hasLeadingText && !hasTrailingText) {
	    if (optSingleChar(first)) return false;
	    if (optTrailingChar(last)) {
		return false;
	    } else {
		for (i = 0; i < slength-1; i++) {
		    if (!allowedChar(str.charAt(i))) return false;
		}
		ch = str.charAt(i);
		if (!allowedChar(ch) && !optTrailingChar(ch)) {
		    return false;
		}
	    }
	} else if (!hasLeadingText && hasTrailingText) {
	    if (optLeadingChar(first)) {
		return false;
	    } else {
		ch = str.charAt(0);
		if (!(optLeadingChar(ch) ||
		      allowedChar(ch))) return false;
		for (i = 1; i < slength; i++) {
		    if (!allowedChar(str.charAt(i))) return false;
		}
	    }
	} else if (!hasLeadingText && !hasTrailingText) {
	    if (slength == 1) {
		ch = str.charAt(0);
		return (optSingleChar(ch) || optLeadingChar(ch) ||
			allowedChar(ch));
	    } else if (slength == 0) {
		// replacing with empty string.
		return true;
	    } else {
		ch = str.charAt(0);
		if(optSingleChar(ch)) return false;
		if (!(optLeadingChar(ch) || allowedChar(ch))) return false;
		for (i = 1; i < slength-1; i++) {
		    if (!allowedChar(str.charAt(i))) return false;
		}
		ch = str.charAt(i);
		if (!allowedChar(ch) && !optTrailingChar(ch)) {
		    return false;
		}
	    }
	}
	return true;
    }

    @Override
    public void insertString(DocumentFilter.FilterBypass fb,
			     int offs,
			     String str,
			     AttributeSet a)
	throws BadLocationException 
    {
	Document doc = fb.getDocument();
	int dlength = doc.getLength();
	int slength = str.length();
	boolean hasLeadingText = (offs > 0);
	boolean hasTrailingText =  (offs < dlength);
	char first = (dlength == 0)? '\0': doc.getText(0,dlength).charAt(0);
	char last = (dlength == 0)? '\0': 
	    doc.getText(0,dlength).charAt(dlength-1);
	
	if (dlength == 0 && str.length() == 1) {
	    char ch = str.charAt(0);
	    if (optSingleChar(ch) && !optLeadingChar(ch)) {
		ch = mapSingleChar(ch);
		str = Character.toString(ch);
	    }
	}

	if (ok(str, hasLeadingText, hasTrailingText, first, last)) {
	    super.insertString(fb, offs, str, a);
	} else {
	    Toolkit.getDefaultToolkit().beep();
	}
    }

    @Override
    public void replace(DocumentFilter.FilterBypass fb,
			int offs, int length,
			String str, AttributeSet a)
	throws BadLocationException 
    {
	Document doc = fb.getDocument();
	int alength = doc.getLength();
	int dlength = alength - length;
	int slength = str.length();
	boolean hasLeadingText = (offs > 0);
	boolean hasTrailingText =  (offs < dlength-1);
	char first = (dlength == 0)? '\0': doc.getText(0,1).charAt(0);
	char last = (dlength > 0)? 
	    doc.getText(alength-1,1).charAt(0):'\0';
	if (dlength == 0 && slength == 1) {
	    char ch = str.charAt(0);
	    if (optSingleChar(ch) && !optLeadingChar(ch)) {
		ch = mapSingleChar(ch);
		str = Character.toString(ch);
	    }
	}
	if (ok(str, hasLeadingText, hasTrailingText, first, last)) {
	    super.replace(fb, offs, length, str, a);
	} else
	    Toolkit.getDefaultToolkit().beep();
			    
    }

    @Override
    public void remove(DocumentFilter.FilterBypass fb,
		       int offs, int length) 
	throws BadLocationException 
    {
	Document doc = fb.getDocument();
	super.remove(fb, offs, length);
    }
}
