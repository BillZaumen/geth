package org.bzdev.swing.text;

import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.AttributeSet;
import javax.swing.text.Segment;
import javax.swing.text.BadLocationException;
import javax.swing.event.UndoableEditListener;
import javax.swing.event.DocumentListener;
import javax.swing.SwingUtilities;

import javax.swing.text.Style;
import java.awt.Color;
import java.awt.Font;

import java.lang.reflect.UndeclaredThrowableException;
import java.lang.reflect.InvocationTargetException;

/**
 * A StyledDocument class that is thread safe.
 * Operations are performed on the Swing event-dispatching thread.
 * An existing document is encapsulated, with access to it controlled
 * so as to be thread safe.
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
public class SafeStyledDocument extends SafeDocument implements StyledDocument {
    StyledDocument sdoc;

    /**
     * Constructor.
     * @param document the document to encapsulate
     */
    public SafeStyledDocument(StyledDocument document) {
	super(document);
	sdoc = document;
    }

    abstract class RunnableWithStyle extends RunnableWithRE {
	Style style;
    }

    /**
     * Execute code on the event dispatch thread and return a Style.
     */
    protected Style doitStyle(RunnableWithStyle r) {
	try {
	    SwingUtilities.invokeAndWait(r);
	    if (r.rexception != null) throw r.rexception;
	    return r.style;
	} catch (InterruptedException ie) {
	    throw new SafeDocument.RIException(ie);
	} catch (InvocationTargetException ite) {
	    Throwable thr = ite.getCause();
	    if (thr instanceof RuntimeException)
		throw (RuntimeException) thr;
	    else
		throw new SafeDocument.RTException(thr);
	}
    }
    
    public Style addStyle(String nm, Style parent) {
	if (SwingUtilities.isEventDispatchThread()) {
	    return sdoc.addStyle(nm, parent);
	} else {
	    final String xnm = nm;
	    final Style xparent = parent;
	    return doitStyle(new RunnableWithStyle () {
		    protected void doit() {
			sdoc.addStyle(xnm, xparent);
		    }
		});
	}
    }

    public void removeStyle(String nm) {
	if (SwingUtilities.isEventDispatchThread()) {
	    sdoc.removeStyle(nm);
	    return;
	} else {
	    final String xnm = nm;
	    doitVoid(new RunnableWithRE() {
		    protected void doit() {
			sdoc.removeStyle(xnm);
		    }
		});
	}
    }

    public Style getStyle(String nm) {
	if (SwingUtilities.isEventDispatchThread()) {
	    return sdoc.getStyle(nm);
	} else {
	    final String xnm = nm;
	    return doitStyle(new RunnableWithStyle () {
		    protected void doit() {
			sdoc.getStyle(xnm);
		    }
		});
	}
    }
    
    public void setCharacterAttributes(int offset, int length,
				       AttributeSet attr,
				       boolean replace) {
	if (SwingUtilities.isEventDispatchThread()) {
	    sdoc.setCharacterAttributes(offset, length, attr, replace);
	    return;
	} else {
	    final int xoffset = offset;
	    final int xlength = length;
	    final AttributeSet xattr = attr;
	    final boolean xreplace = replace;
	    doitVoid(new RunnableWithRE() {
		    protected void doit() {
			sdoc.setCharacterAttributes(xoffset, xlength,
						    xattr, xreplace);
		    }
		});
	}
    }
				       
    public void setParagraphAttributes(int offset, int length,
				       AttributeSet attr,
				       boolean replace) {
	if (SwingUtilities.isEventDispatchThread()) {
	    sdoc.setParagraphAttributes(offset, length, attr, replace);
	    return;
	} else {
	    final int xoffset = offset;
	    final int xlength = length;
	    final AttributeSet xattr = attr;
	    final boolean xreplace = replace;
	    doitVoid(new RunnableWithRE() {
		    protected void doit() {
			sdoc.setParagraphAttributes(xoffset, xlength,
						    xattr, xreplace);
		    }
		});
	}
    }

    public void setLogicalStyle(int pos, Style s) {
	if (SwingUtilities.isEventDispatchThread()) {
	    sdoc.setLogicalStyle(pos, s);
	} else {
	    final int xpos = pos;
	    final Style xs = s;
	    doitVoid(new RunnableWithRE() {
		    protected void doit() {
			sdoc.setLogicalStyle(xpos, xs);
		    }
		});
	}
    }

    public Style getLogicalStyle(int p) {
	if (SwingUtilities.isEventDispatchThread()) {
	    return sdoc.getLogicalStyle(p);
	} else {
	    final int xpos = p;
	    return doitStyle(new RunnableWithStyle () {
		    protected void doit() {
			sdoc.getLogicalStyle(xpos);
		    }
		});
	}
    }

    public Element getParagraphElement(int pos) {
	if (SwingUtilities.isEventDispatchThread()) {
	    return sdoc.getParagraphElement(pos);
	} else {
	    final int xpos = pos;
	    return doitElement(new RunnableWithElement() {
		    public void doit() {
			element = sdoc.getParagraphElement(xpos);
		    }
		});
	}
    }

    public Element getCharacterElement(int pos) {
	if (SwingUtilities.isEventDispatchThread()) {
	    return sdoc.getCharacterElement(pos);
	} else {
	    final int xpos = pos;
	    return doitElement(new RunnableWithElement() {
		    public void doit() {
			element = sdoc.getCharacterElement(xpos);
		    }
		});
	}
    }

    /*
     * Assume following are just convenience methods so
     * that we don't have to put these on the event dispatch
     * thread.
     */
    public Color getForeground(AttributeSet attr) {
	return sdoc.getForeground(attr);
    }

    public Color getBackground(AttributeSet attr) {
	return sdoc.getBackground(attr);
    }

    public Font getFont(AttributeSet attr) {
	return sdoc.getFont(attr);
    }
}
