package org.bzdev.swing.text;

import java.lang.reflect.UndeclaredThrowableException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.AttributeSet;
import javax.swing.text.Segment;
import javax.swing.text.BadLocationException;
import javax.swing.event.UndoableEditListener;
import javax.swing.event.DocumentListener;
import javax.swing.SwingUtilities;

// import javax.swing.text.PlainDocument;

/**
 * A Document class that is thread safe.
 * Operations are performed on the event-dispatch thread.
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
public class SafeDocument implements Document {
    Document doc;
    /**
     * Constructor.
     * @param doc a document to encapsulate
     */
    public SafeDocument(Document doc) {
	this.doc = doc;
    }

    /**
     * Get the encapsulated document.
     * @return the encapsulated document
     */
    public Document getEncapsulatedDocument() {
	return doc;
    }

    /**
     * SafeDocument runtime exception class for interrupted exceptions.
     */
    public class RIException extends java.lang.RuntimeException {
	RIException(InterruptedException ie) {
	    super((Throwable)ie);
	}
    }

    /**
     * SafeDocument runtime-exception class.
     * For an instance of this class to be constructed, a call
     * to SwingUtilities.invokeAndWait would have to be interrupted.
     */
    public class RTException extends java.lang.RuntimeException {
	RTException (Throwable cause) {
	    super(cause);
	}
    }

    /**
     * Runnable that tracks runtime exceptions.
     * This class is used internally by SafeDocument and SafeStyledDocument.
     */
    abstract class RunnableWithRE implements Runnable {
	RuntimeException rexception;
	/**
	 * The operation to perform.
	 */
	abstract protected void doit();
	public void run(){
	    try {
		doit();
	    }  catch (RuntimeException re) {
		rexception = re;
	    }
	}
    }

    abstract class RunnableWithInt extends RunnableWithRE {
	int intval;
    }

    /**
     * Perform an operation on the event dispatch thread and return an
     * int.
     * @param r an object providing the code to execute in a method named
     *        doit()
     * @return an integer value
     */
    protected int doitInt(RunnableWithInt r) {
	try {
	    SwingUtilities.invokeAndWait(r);
	    if (r.rexception != null) throw r.rexception;
	    return r.intval;
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

    public int getLength() {
	if (SwingUtilities.isEventDispatchThread()) {
	    return doc.getLength();
	} else {
	    return doitInt(new RunnableWithInt() {
		    protected void doit() {
			intval = doc.getLength();
		    }
		});
	}
    }

    /**
     * Perform an operation on the event dispatch thread with no
     * return value.
     * @param r an object providing the code to execute in a method named
     *        doit()
     */
    protected void doitVoid(RunnableWithRE r) {
	try {
	    SwingUtilities.invokeAndWait(r);
	    if (r.rexception != null) throw r.rexception;
	    return;
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

    public void addDocumentListener(DocumentListener listener) {
	if (SwingUtilities.isEventDispatchThread()) {
	    doc.addDocumentListener(listener);
	    return;
	} else {
	    final DocumentListener lis = listener;
	    doitVoid(new RunnableWithRE() {
		    protected void doit() {
			    doc.addDocumentListener(lis);
		    }
		});
	}
    }
    
    public void removeDocumentListener(DocumentListener listener) {
	if (SwingUtilities.isEventDispatchThread()) {
	    doc.removeDocumentListener(listener);
	    return;
	} else {
	    final DocumentListener lis = listener;
	    doitVoid(new RunnableWithRE() {
		    protected void doit() {
			doc.removeDocumentListener(lis);
		    }
		});
	}
    }

    public void addUndoableEditListener(UndoableEditListener listener) {
	if (SwingUtilities.isEventDispatchThread()) {
	    doc.addUndoableEditListener(listener);
	    return;
	} else {
	    final UndoableEditListener lis = listener;
	    doitVoid(new RunnableWithRE() {
		    protected void doit() {
			doc.addUndoableEditListener(lis);
		    }
		});
	}
    }

    public void removeUndoableEditListener(UndoableEditListener listener) {
	if (SwingUtilities.isEventDispatchThread()) {
	    doc.removeUndoableEditListener(listener);
	} else {
	    final UndoableEditListener lis = listener;
	    doitVoid(new RunnableWithRE() {
		    protected void doit() {
			    doc.removeUndoableEditListener(lis);
		    }
		});
	}
    }

    abstract class RunnableWithObj extends RunnableWithRE {
	Object obj;
    }

    /**
     * Perform an operation on the event dispatch thread and return an
     * object.
     * @param r an object providing the code to execute in a method named
     *        doit() that will store its return value in r.obj
     */
    protected Object doitObject(RunnableWithObj r) {
	try {
	    SwingUtilities.invokeAndWait(r);
	    if (r.rexception != null) throw r.rexception;
	    return r.obj;
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

    public Object getProperty(Object key) {
	if (SwingUtilities.isEventDispatchThread()) {
	    return doc.getProperty(key);
	} else {
	    final Object k = key;
	    return doitObject(new RunnableWithObj() {
		    protected void doit() {
			    obj = doc.getProperty(k);
		    }
		});
	}
    }

    public void putProperty(Object key,  Object value) {
	if (SwingUtilities.isEventDispatchThread()) {
	    doc.putProperty(key, value);
	    return;
	} else {
	    final Object val = value;
	    final Object k = key;
	    doitVoid(new RunnableWithRE() {
		    protected void doit() {
			    doc.putProperty(k,val);
		    }
		});
	}
    }
    /**
     * Class to run a thread while logging any BadLocationException that occurs.
     */
    abstract public class RunnableWithBadLocException extends RunnableWithRE {
	BadLocationException exception;
	protected void doit() {throw new UnsupportedOperationException();}
	/**
	 * The code the runnable will execute.
	 * @exception BadLocationException a BadLocationException was thrown
	 */
	abstract protected void doitBLE() throws BadLocationException;
	public void run() {
	    try {
		doitBLE();
	    } catch (BadLocationException  e) {
		exception = e; 
	    } catch (RuntimeException re) {
		rexception = re;
	    }
	}
    }

    /**
     * Perform an operation on the event dispatch thread, possibly
     * throwing a BadLocationException.
     * @param r an object whose doitBLE() method provides the code to execute
     * @exception BadLocationException a BadLocationException occurred
     */
    protected void doitVoidBLE(RunnableWithBadLocException r) 
	throws BadLocationException {
	try {
	    SwingUtilities.invokeAndWait(r);
	    if (r.exception != null) throw r.exception;
	    if (r.rexception != null) throw r.rexception;
	    return;
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

    public void remove(int offs, int len) throws BadLocationException {
	if (SwingUtilities.isEventDispatchThread()) {
	    doc.remove(offs, len);
	} else {
	    final int xoffs = offs;
	    final int xlen = len;
	    doitVoidBLE(new RunnableWithBadLocException() {
		    protected void doitBLE() throws BadLocationException {
			doc.remove(xoffs, xlen);
		    }
		});
	}
    }

    public void insertString(int offset, String str, AttributeSet a)
	throws BadLocationException {
	if (SwingUtilities.isEventDispatchThread()) {
	    doc.insertString(offset, str, a);
	} else {
	    final int xoffset = offset;
	    final String xstr = str;
	    final AttributeSet xa = a;
	    doitVoidBLE(new RunnableWithBadLocException() {
		    protected void doitBLE() throws BadLocationException {
			doc.insertString(xoffset, xstr, xa);
		    }
		});
	}
    }

    abstract class RunnableWithString extends RunnableWithBadLocException {
	String string;
    }

    /**
     * Perform an operation on the event dispatch thread returning a
     * String, possibly throwing a BadLocationException.
     * @param r an object whose doitBLE() method provides the code to execute
     *    with the return value stored in r.string
     * @return a string
     * @exception BadLocationException a BadLocationException occurred
     */
    protected String doitStringBLE(RunnableWithString r) 
	throws BadLocationException {
	try {
	    SwingUtilities.invokeAndWait(r);
	    if (r.exception != null) throw r.exception;
	    if (r.rexception != null) throw r.rexception;
	    return r.string;
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

    public String getText(int offset, int length) throws BadLocationException {
	if (SwingUtilities.isEventDispatchThread()) {
	    return doc.getText(offset, length);
	} else {
	    final int xoffset = offset;
	    final int xlen = length;
	    return doitStringBLE(new RunnableWithString() {
		    protected void doitBLE() throws BadLocationException {
			string = doc.getText(xoffset, xlen);
		    }
		});
	}
    }
    
    public void getText(int offset, int length, Segment txt)
	throws BadLocationException {
	if (SwingUtilities.isEventDispatchThread()) {
	    doc.getText(offset, length, txt);
	} else {
	    final int xoffset = offset;
	    final int xlength = length;
	    final Segment xtxt = txt;
	    doitVoidBLE(new RunnableWithBadLocException() {
		    protected void doitBLE() throws BadLocationException {
			doc.getText(xoffset, xlength, xtxt);
		    }
		});
	}
    }

    abstract class RunnableWithPosition extends RunnableWithRE {
	Position position;
    }

    abstract class RunnableWithPositionBLE
	extends RunnableWithBadLocException {
	Position position;
    }

    /**
     * Execute an operation on the event dispatch thread and return
     * a Position.
     * @param r an object whose doitBLE() method provides the code to execute
     *        and that will store a return value in r.position
     * @return the position
     */
    protected Position doitPosition(RunnableWithPosition r) {
	try {
	    SwingUtilities.invokeAndWait(r);
	    if (r.rexception != null) throw r.rexception;
	    return r.position;
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

    public Position getStartPosition() {
	if (SwingUtilities.isEventDispatchThread()) {
	    return doc.getStartPosition();
	} else {
	    return doitPosition(new RunnableWithPosition() {
		    protected void doit() {
			position = doc.getStartPosition();
		    }
		});
	}
    }

    public Position getEndPosition() {
	if (SwingUtilities.isEventDispatchThread()) {
	    return doc.getEndPosition();
	} else {
	    return doitPosition(new RunnableWithPosition() {
		    protected void doit() {
			position = doc.getEndPosition();
		    }
		});
	}
    }

    /**
     * Execute a task on the event dispatch thread, returning a position,
     * throwing any BadLocationException that occurs.
     * @param r an object whose doitBLE() method provides the code to execute
     *        and that will store its return value in r.position
     * @return the position
     * @exception BadLocationException a BadLocationException occurred
     */
    protected Position doitPositionBLE(RunnableWithPositionBLE r) 
	throws BadLocationException {
	try {
	    SwingUtilities.invokeAndWait(r);
	    if (r.exception != null) throw r.exception;
	    if (r.rexception != null) throw r.rexception;
	    return r.position;
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

    public Position createPosition(int offs) throws BadLocationException {
	if (SwingUtilities.isEventDispatchThread()) {
	    return doc.createPosition(offs);
	} else {
	    final int xoffs = offs;
	    return doitPositionBLE(new RunnableWithPositionBLE() {
		    protected void doitBLE() throws
			BadLocationException {
			position = doc.createPosition(xoffs);
		    }
		});
	}
    }
    
    abstract class RunnableWithElements extends RunnableWithRE {
	Element[] elements;
    }

    /**
     * Execute a task on the event dispatch  thread, returning an array
     * of Element.
     * @param r an object whose doit() method provides the code to execute
     *        and that will store the element array it computes in r.elements
     * @return the elements computed
     */
    protected Element[] doitElements(RunnableWithElements r) {
	try {
	    SwingUtilities.invokeAndWait(r);
	    if (r.rexception != null) throw r.rexception;
	    return r.elements;
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
    
    public Element[] getRootElements() {
	if (SwingUtilities.isEventDispatchThread()) {
	    return doc.getRootElements();
	} else {
	    return doitElements(new RunnableWithElements() {
		    protected void doit() {
			    elements = doc.getRootElements();
		    }
		});
	}
    }

    abstract class RunnableWithElement extends RunnableWithRE {
	Element element;
    }

    /**
     * Execute a task on the event dispatch  thread, returning a
     * Element.
     * @param r an object whose doit() method provides the code to execute
     *        and that will store the element it computes in r.element
     * @return the element computed
     */
    protected Element doitElement(RunnableWithElement r) {
	try {
	    SwingUtilities.invokeAndWait(r);
	    if (r.rexception != null) throw r.rexception;
	    return r.element;
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

    public Element getDefaultRootElement() {
	if (SwingUtilities.isEventDispatchThread()) {
	    return doc.getDefaultRootElement();
	} else {
	    return doitElement(new RunnableWithElement() {
		    public void doit() {
			element = doc.getDefaultRootElement();
		    }
		});
	}
    }

    public void render(Runnable runnable) {
	if (SwingUtilities.isEventDispatchThread()) {
	    doc.render(runnable);
	    return;
	} else {
	    final Runnable x = runnable;
	    doitVoid(new RunnableWithRE() {
		    protected void doit() {
			doc.render(x);
		    }
		});
	}
    }

    /*
    static public void main(String argv[]) {
	Document doc = new PlainDocument();
	Document sdoc = new SafeDocument(doc);

	System.out.println(doc.getLength());
	try {
	    doc.insertString(0, "hello", null);
	    System.out.println("length of " +doc.getText(0, doc.getLength()) 
			       +" = "  +doc.getLength());
	} catch (BadLocationException e) {
	    e.printStackTrace();
	}
	try {
	    doc.insertString(100,"hello", null);
	    System.out.println("Exception not raised");
	} catch (BadLocationException e) {
	    System.out.println("caught expected BadLocationException");
	}
    }
    */
}
