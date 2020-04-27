package org.bzdev.swing;

import java.awt.Color;
import java.lang.reflect.UndeclaredThrowableException;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import org.bzdev.swing.text.SafeDocument;
import org.bzdev.swing.text.SafeStyledDocument;
import javax.swing.text.StyleContext;
import javax.swing.text.StyleConstants;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.SwingUtilities;
import java.util.function.Consumer;

//@exbundle org.bzdev.swing.lpack.Swing

/**
 * A JTextPane with convenience methods and a small number of styles.
 * It uses a default font, but lets you make the font bold, italic,
 * or appear in various colors.  This is useful for displaying source
 * code or similar outputs where keywords might appear in a different
 * font or color from variables, or where key/value pairs show keys
 * in bold fonts and values in normal fonts.
 * <P>
 * The method {@link SimpleJTextPane#setSwingSafe(boolean)} can be used
 * to configure this component so that it can be modified from any thread,
 * not just the event dispatch thread.  Allowing the component to be
 * modified from any thread is useful for applications with long running
 * times that change the contents of this component.
 * <P>
 * This class makes use of the static method
 * {@link SwingUtilities#invokeLater(Runnable)}. One should avoid the
 * use of synchronized methods that call methods in this class when
 * those synchronized methods might be called from tasks waiting on
 * the AWT event dispatch queue, as there is a possibility of
 * deadlock: If for some class methods m1 and m2 are synchronized and
 * call one of the methods in this class, and m1 is called, a call to
 * {@link SwingUtilities#invokeLater(Runnable)} may process other
 * entries on its event queue first, causing m2 to be called, but m2
 * will wait until m1 returns, which cannot occur until m2 returns.
 * An experiement indicated that the behavior of the event queue can
 * change if, for example, a security manager is installed, so initial
 * testing can easily miss cases that could lead to deadlocks.
 */
public class SimpleJTextPane extends JTextPane implements Appendable {
    MutableAttributeSet aset = StyleContext.getDefaultStyleContext().
	getStyle(StyleContext.DEFAULT_STYLE);

    static String errorMsg(String key, Object... args) {
	return SwingErrorMsg.errorMsg(key, args);
    }

    /**
     * SimpleJTextPane runtime-exception class.
     * For an instance of this class to be constructed, a call
     * to SwingUtilities.invokeAndWait would have to be interrupted.
     */
    public class RTException extends java.lang.RuntimeException {
	RTException (Throwable cause) {
	    super(cause);
	}
    }

    /**
     * Saved attribute state for a SimpleJTextPane.
     * The method {@link SimpleJTextPane#saveAttributeState()} will
     * store this text pane's default attribute set and restore it
     * when {@link State#restore()} is called. The {@link State#close()}
     * method just calls {@link State#restore()}. The class
     * {@link AutoCloseable} is implemented so that this class can be
     * used to create a try-with-resources block.
     */
    public class State implements AutoCloseable {
	boolean bold;
	boolean italic;
	Color tfg;
	Color tbg;
	boolean isSafe;

	State() {
	    bold = isBold();
	    italic = isItalic();
	    tfg = getTextForeground();
	    tbg = getTextBackground();
	    isSafe = isSwingSafe();
	}
	/**
	 * Restore the state to its initial value.
	 */
	public void restore() {
	    synchronized(aset) {
		setBold(bold);
		setItalic(italic);
		setTextForeground(tfg);
		setTextBackground(tbg);
		setSwingSafe(isSafe);
	    }
	}

	@Override
	public void close() {
	    restore();
	}
    }

    /**
     * Save an attribute state so that it can be restored later.
     * The value returned may be used in  a try-with-resources block
     * so that the state will be automatically restored when the
     * block exits for whatever reason.
     * <P>
     * This method is thread safe.
     * @return an object storing the attribute state
     */
    public State saveAttributeState() {
	synchronized(aset) {
	    return new State();
	}
    }

    /**
     * Default Constructor.
     */
    public SimpleJTextPane() {
	super();
	StyleConstants.setBackground(aset, getBackground());
	StyleConstants.setForeground(aset, getForeground());
	// initAset();
    }

    /**
     * Constructor with a specific underlying document.
     * @param doc the document for the component's model.
     */
    public SimpleJTextPane(StyledDocument doc) {
	super(doc);
	StyleConstants.setBackground(aset, getBackground());
	StyleConstants.setForeground(aset, getForeground());
	// initAset();
    }

    /*
    private void initAset() {
        StyleConstants.setFontFamily(aset, "SansSerif");
	setBold(false);
	setItalic(false);
    }
    */

    /**
     * Run a sequence of operations atomically.
     * The argument c is a lambda express that will be called with
     * its single argument set to this SimpleJTextPane. Because this
     * method is synchronized, the opertions the lambda expression
     * performs will not be interleaved with synchronized calls from
     * other threads.  Alternatively, one can implement the consumer's
     * accept method instead of using a lambda expression.
     * <P>
     * This method is thread safe.
     * @param c the consumer
     */
    public void perform(Consumer<SimpleJTextPane> c) {
	if (SwingUtilities.isEventDispatchThread()) {
	    try (State saved = saveAttributeState()) {
		c.accept(this);
	    }
	} else {
	    try {
		SwingUtilities.invokeAndWait(() -> {
			try (State saved = saveAttributeState()) {
			    c.accept(this);
			}
		    });
	    } catch (InterruptedException ie) {
		throw new RuntimeException(ie);
	    } catch (InvocationTargetException ite) {
		Throwable thr = ite.getCause();
		if (thr instanceof RuntimeException)
		    throw (RuntimeException) thr;
		else
		    throw new SimpleJTextPane.RTException(thr);
	    }
	}
    }

    /**
     * Determine if the font weight is appropriate for bold text.
     * <P>
     * This method is thread safe.
     * @return true if newly inserted text will be bold; false 
     *         otherwise
     */
    public boolean isBold() {
	synchronized(aset) {
	    return StyleConstants.isBold(aset);
	}
    }

    /**
     * Set the font weight for subsequently inserted text to bold or
     * normal.
     * <P>
     * This method is thread safe.
     * @param value true if the font is bold; false if it's weight
     *        is normal.
     */
    public void setBold(boolean value) {
	synchronized(aset) {
	    StyleConstants.setBold(aset, value);
	}
    }


    /**
     * Determine if newly inserted text will be in italics.
     * <P>
     * This method is thread safe.
     * @return true if newly inserted text will be italics; false
     *         otherwise
     */
    public boolean isItalic() {
	synchronized(aset) {
	    return StyleConstants.isItalic(aset);
	}
    }


    /**
     * Set the font slant for subsequently inserted text.
     * <P>
     * This method is thread safe.
     * @param value  true if the font is italic; false if not.
     */
    public void setItalic(boolean value) {
	synchronized(aset) {
	    StyleConstants.setItalic(aset, value);
	}
    }


    /**
     * Get the color of subsequently inserted or appended text.
     * The initial value is the foreground color of this pane
     * when it is created (if not set, it inherits the color for
     * this pane's parent).
     * <P>
     * This method is thread safe.
     * @return The color that will be used for new text.
     */
    public Color getTextForeground() {
	synchronized(aset) {
	    return StyleConstants.getForeground(aset);
	}
    }


    /**
     * Set the text color for new text.
     * <P>
     * This method is thread safe.
     * @param fg  the color for subsequently inserted or appended text.
     */
    public void setTextForeground(Color fg) {
	synchronized(aset) {
	    StyleConstants.setForeground(aset, fg);
	}
    }
    

    /**
     * Get the background color for subsequently inserted or appended text.
     * The initial value is the background color of this pane
     * when it is created (if not set, it inherits the color for
     * this pane's parent).
     * <P>
     * This method is thread safe.
     * @return The color that will be used for the background for new text.
     */
    public Color getTextBackground() {
	synchronized(aset) {
	    return StyleConstants.getBackground(aset);
	}
    }


    /**
     * Set the background color for new text.
     * <P>
     * This method is thread safe.
     * @param bg the background color for subsequently inserted or
     *           appended text.
     */
    public void setTextBackground(Color bg) {
	synchronized(aset) {
	    StyleConstants.setBackground(aset, bg);
	}
    }


    private boolean safe = false;

    /**
     * Set whether or not this component is thread-safe.
     * @param flag true if this component is thread safe; false if
     *        modifications to this component must be made from
     *        the event dispatch thread.
     */
    public void setSwingSafe(boolean flag) {
	if (flag) {
	    if (safe) return;
	    setDocument(new SafeStyledDocument((StyledDocument)getDocument()));
	} else {
	    if (!safe) return;
	    setDocument(((SafeStyledDocument)getDocument()).
			getEncapsulatedDocument());
	}
	safe = !safe;
    }



    /**
     * Test if this component is thread safe.
     * <P>
     * This method is thread safe.

     * @return true if this component is thread-safe; false if changes
     *         must be made from the event dispatch thread
     */
    public boolean isSwingSafe() {
	return safe;
    }

    @Override
    public void setDocument(Document doc) {
	super.setDocument(doc);
	safe = (doc instanceof SafeDocument);
    }

    @Override
    public void setStyledDocument(StyledDocument doc) {
	super.setStyledDocument(doc);
	safe = (doc instanceof SafeStyledDocument);
    }

    /*
     * Convenience methods
     */

    /**
     * Insert text into the component's document.
     * <P>
     * This method is thread safe if when {@link #isSwingSafe()} returns
     * true.
     * @param off  the offset into the document at which the new text should
     *             be inserted
     * @param str  the text to insert.
     * @exception BadLocationException attempted to insert text at a
     *            non-existent location
     */
    public void insertString(int off, String str)
	throws BadLocationException
    {
	getDocument().insertString(off, str, aset);
    }
    

    /**
     * Get the number of characters in this text pane.
     * <P>
     * This method is thread safe if when {@link #isSwingSafe()} returns
     * true.
     * @return the number of characters
     */
    public int getLength() {
	return getDocument().getLength();
    }
    /**
     * Insert text at the end of the component's document.
     * <P>
     * This method is thread safe.
     * @param str  the text to insert.
     */
    public void appendString(String str) {
	if (SwingUtilities.isEventDispatchThread()) {
	    for(;;) {
		try {
		    Document doc = getDocument();
		    doc.insertString(doc.getLength(), str, aset);
		    return;
		} catch (BadLocationException e) {
		    // This should rarely happen due use of doc.getLength():
		    // something would have to modify the length of the
		    // document between the call to doc.getLength() and
		    // the call to doc.insertString.
		    continue;
		}
	    }
	} else {
	    try {
		SwingUtilities.invokeAndWait(() -> {
			for(;;) {
			    try {
				Document doc = getDocument();
				doc.insertString(doc.getLength(), str, aset);
				return;
			    } catch (BadLocationException e) {
				// This should rarely happen due use of
				// doc.getLength(): something would have
				// to modify the length of the document
				// between the call to doc.getLength() and
				// the call to doc.insertString.
				continue;
			    }
			}
		    });
	    } catch (InterruptedException ie) {
		throw new RuntimeException(ie);
	    } catch (InvocationTargetException ite) {
		Throwable thr = ite.getCause();
		if (thr instanceof RuntimeException)
		    throw (RuntimeException) thr;
		else
		    throw new SimpleJTextPane.RTException(thr);
	    }
	}
    }


    // Appendable-interface implementation

    /**
     * Append a character to the end of the component's document.
     * <P>
     * This method is thread safe.
     * @param c  the character to append
     * @return the current object cast as an Appendable
     */
    public Appendable append(char c) {
	if (SwingUtilities.isEventDispatchThread()) {
	    for (;;) {
		try {
		    Document doc = getDocument();
		    String str = Character.toString(c);
		    doc.insertString(doc.getLength(), str, aset);
		    return (Appendable) this;
		} catch (BadLocationException e) {
		    // This should rarely happen due use of doc.getLength():
		    // something would have to modify the length of the
		    // document between the call to doc.getLength() and
		    // the call to doc.insertString.
		    continue;
		}
	    }
	} else {
	    try {
		SwingUtilities.invokeAndWait(() -> {
			for (;;) {
			    try {
				Document doc = getDocument();
				String str = Character.toString(c);
				doc.insertString(doc.getLength(), str, aset);
				return;
			    } catch (BadLocationException e) {
				// This should rarely happen due use of
				// doc.getLength(): something would have
				// to modify the length of the document
				// between the call to doc.getLength() and
				// the call to doc.insertString.
				continue;
			    }
			}
		    });
	    } catch (InterruptedException ie) {
		throw new RuntimeException(ie);
	    } catch (InvocationTargetException ite) {
		Throwable thr = ite.getCause();
		if (thr instanceof RuntimeException)
		    throw (RuntimeException) thr;
		else
		    throw new SimpleJTextPane.RTException(thr);
	    }
	    return (Appendable) this;
	}
    }

    /**
     * Append text to the end of the component's document.
     * <P>
     * This method is thread safe.
     * @param csq  the text to append
     * @return the current object cast as an Appendable
     */
    public Appendable append(CharSequence csq) {
	if (SwingUtilities.isEventDispatchThread()) {
	    for (;;) {
		try {
		    Document doc = getDocument();
		    String str = csq.toString();
		    doc.insertString(doc.getLength(), str, aset);
		    return (Appendable) this;
		} catch (BadLocationException e) {
		    System.err.println("bad location");
		    // This should rarely happen due use of doc.getLength():
		    // something would have to modify the length of the
		    // document between the call to doc.getLength() and
		    // the call to doc.insertString.
		    continue;
		}
	    }
	} else {
	    try {
		SwingUtilities.invokeAndWait(() -> {
			for (;;) {
			    try {
				Document doc = getDocument();
				String str = csq.toString();
				doc.insertString(doc.getLength(), str, aset);
				return;
			    } catch (BadLocationException e) {
				System.err.println("bad location");
				// This should rarely happen due use of
				// doc.getLength(): something would have
				// to modify the length of the document
				// between the call to doc.getLength() and
				// the call to doc.insertString.
				continue;
			    } catch (Throwable t) {
				t.printStackTrace();
				System.exit(1);
			    }
			}
		    });
	    } catch (InterruptedException ie) {
		throw new RuntimeException(ie);
	    } catch (InvocationTargetException ite) {
		Throwable thr = ite.getCause();
		if (thr instanceof RuntimeException)
		    throw (RuntimeException) thr;
		else
		    throw new SimpleJTextPane.RTException(thr);
	    }
	    return (Appendable) this;
	}
    }

    /**
     * Append text to the end of the component's document.
     * <P>
     * This method is thread safe.
     * @param csq  a CharSequence containing the text to append
     * @param start the offset to the start of the text in csq
     * @param end The position just past the end of the text to
     *        in csq to append
     * @return the current object cast as an Appendable
     */
    public Appendable append(CharSequence csq, int start, int end)
    {
	int count = 0;
	if (SwingUtilities.isEventDispatchThread()) {
	    for (;;) {
		try {
		    Document doc = getDocument();
		    String str = csq.subSequence(start, end).toString();
		    doc.insertString(doc.getLength(), str, aset);
		    return (Appendable) this;
		} catch (BadLocationException e) {
		    // This should rarely happen due use of doc.getLength():
		    // something would have to modify the length of the
		    // document between the call to doc.getLength() and
		    // the call to doc.insertString.
		    if (count++ == 10) {
			System.err.println("append failed - cannot find end");
		    }
		    continue;
		}
	    }
	} else {
	    try {
		SwingUtilities.invokeAndWait(() -> {
			for (;;) {
			    try {
				Document doc = getDocument();
				String str =
				    csq.subSequence(start, end).toString();
				doc.insertString(doc.getLength(), str, aset);
				return;
			    } catch (BadLocationException e) {
				// This should rarely happen due use of
				// doc.getLength(): something would have
				// to modify the length of the document
				// between the call to doc.getLength() and
				// the call to doc.insertString.
				continue;
			    }
			}
		    });
	    } catch (InterruptedException ie) {
		throw new RuntimeException(ie);
	    } catch (InvocationTargetException ite) {
		Throwable thr = ite.getCause();
		if (thr instanceof RuntimeException)
		    throw (RuntimeException) thr;
		else
		    throw new SimpleJTextPane.RTException(thr);
	    }
	    return (Appendable) this;
	}
    }

    /**
     * Remove text from the document the component displays.
     * <P>
     * This method is thread safe if when {@link #isSwingSafe()} returns
     * true.
     * @param start  the starting offset into the document for the text
     *               to be removed..
     * @param length  the length of the text to be removed.
     * @exception BadLocationException  the arguments cover a range of text
     *                                  that is not part of the document.
     */
    public void remove(int start, int length)
	throws BadLocationException
    {
	getDocument().remove(start, length);
    }


    /**
     * Remove all the text from a document.
     * <P>
     * This method is thread safe.
     */
    public void clear() {
	if (SwingUtilities.isEventDispatchThread()) {
	    for (;;) {
		try {
		    Document doc = getDocument();
		    doc.remove(0, doc.getLength());
		    return;
		} catch (BadLocationException e) {
		    // This should rarely happen due use of doc.getLength():
		    // something would have to modify the length of the
		    // document between the call to doc.getLength() and
		    // the call to doc.remove.
		    continue;
		}
	    }
	} else {
	    try {
		SwingUtilities.invokeAndWait(() -> {
			for (;;) {
			    try {
				Document doc = getDocument();
				doc.remove(0, doc.getLength());
				return;
			    } catch (BadLocationException e) {
				// This should rarely happen due use of
				// doc.getLength(): something would have
				// to modify the length of the document
				// between the call to doc.getLength() and
				// the call to doc.remove.
				continue;
			    }
			}
		    });
	    } catch (InterruptedException ie) {
		throw new RuntimeException(ie);
	    } catch (InvocationTargetException ite) {
		Throwable thr = ite.getCause();
		if (thr instanceof RuntimeException)
		    throw (RuntimeException) thr;
		else
		    throw new SimpleJTextPane.RTException(thr);
	    }
	}
    }
}
