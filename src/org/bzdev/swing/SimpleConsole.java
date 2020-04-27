package org.bzdev.swing;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.io.*;
import java.lang.reflect.UndeclaredThrowableException;
import java.lang.reflect.InvocationTargetException;
import java.security.*;
import java.text.MessageFormat;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.text.*;
import java.util.*;
import java.util.function.Consumer;

//@exbundle org.bzdev.swing.lpack.Swing

/**
 * Simplified console for logging error messages and warnings.  While
 * this Swing component provides a text pane in which messages will be
 * displayed, it also implements the {@link java.lang.Appendable}
 * interface, which allows text to be added.  The class
 * {@link org.bzdev.io.AppendableWriter} can be used to create a 
 * {@link java.io.Writer Writer} that in turn can be used to create a
 * {@link java.io.PrintWriter PrintWriter} that will simplify creating
 * formatted text:
 * <blockquote><code>
 *     SimpleConsole console = new SimpleConsole();
 *     // .... add this component to a frame
 *     PrintWriter w = new PrintWriter(new AppendableWriter(console));
 * </code></blockquote>
 * <P>
 * The class {@link ErrorMessage} can be used to create error messages
 * and can be configured to pass those messages to an instance of
 * this class.  The methods that {@link ErrorMessage} provides are all
 * static methods so that error-printing code does not have to keep
 * explicitly keep track of where a message should be displayed.
 * For use with a GUI, the following code will create a menu item that
 * will make a console visible:
 * <blockquote><code><pre>
 *    JFrame frame = new JFrame("Application Frame");
 *    menubar = new JMenuBar();
 *    menu = new JMenu("Tools");
 *    ...
 *    SimpleConsole console = new SimpleConsole();
 *    JMenuItem consoleMenuItem =
 *        console.createMenuItem("Open Console", "Console",
 *                               800, 600);
 *    menu.add(consoleMenuItem);
 *    ...
 *    menubar.add(menu);
 *    frame.setJMenuBar(menubar);
 *    frame.setSize(...,...);
 *    frame.setVisible(true);
 *    ErrorMessage.setAppendable(console);
 *    ErrorMessage.setComponent(frame);
 * </pre></code></blockquote>
 * This code will create a menu named "Tools" with an item named
 * "Open Console", which will open a window whose title is "Console"
 * with a width of 800 pixels and a height of 600 pixels. The console window
 * can be resized.  The SimpleConsole component itself includes a button
 * to clear the contents of the console. The lines
 * <blockquote><code>
 *    ErrorMessage.setAppendable(console);
 *    ErrorMessage.setComponent(frame);
 * </code></blockquote>
 * result in ErrorMessage output going to the console, and with messages
 * that should be shown in a dialog box being displayed in a dialog box
 * centered on the application frame.  When ErrorMessage is used, one
 * will most likely add a menu item to turn stack traces for exceptions
 * on and off:
 * <blockquote><code>
 *    JMenuItem stackTraceMenuItem =
 *          new StackTraceMenuItem("Show Stacktrace");
 *    menu.add(stackTraceMenuItem);
 * </code></blockquote>
 * <P>
 * This class makes use of the static method
 * {@link SwingUtilities#invokeLater(Runnable)} internally. One should
 * avoid the use of synchronized methods that call methods in this
 * class when those synchronized methods might be called from tasks
 * waiting on the AWT event dispatch queue, as there is a possibility
 * of deadlock: If for some class methods m1 and m2 are synchronized
 * and call one of the methods in this class, and m1 is called, a call
 * to {@link SwingUtilities#invokeLater(Runnable)} may process other
 * entries on its event queue first, causing m2 to be called, but m2
 * will wait until m1 returns, which cannot occur until m2 returns.
 * An experiment indicated that the behavior of the event queue can
 * change if, for example, a security manager is installed, so initial
 * testing can easily miss cases that could lead to deadlocks.
 * <P>

 * While the SimpleConsole class has Java Swing components
 * associated with it, the newFramedInstance methods are thread safe -
 * their use is not restricted to the event dispatch
 * thread. Consequently, it is easy to create a command-line program
 * that will open a window to display its output and exit when that
 * window is closed.  Unlike {@link AnimatedPanelGraphics} and
 * {@link PanelGraphics}, the constructors for this class, and the
 * method {@link SimpleConsole#createMenuItem(String,String,int,int)},
 * are not thread safe: these methods must be run on the event
 * dispatch thread as is the standard requirement for Java Swing
 * components.
 * @see ErrorMessage
 * @see StackTraceMenuItem
 */
public class SimpleConsole extends JComponent implements Appendable {
    static private final String resourceBundleName = 
	"org.bzdev.swing.lpack.SimpleConsole";
    static ResourceBundle bundle = 
	ResourceBundle.getBundle(resourceBundleName);
    static String localeString(String name) {
	return bundle.getString(name);
    }

    static String errorMsg(String key, Object... args) {
	return SwingErrorMsg.errorMsg(key, args);
    }


    private SimpleJTextPane console;
    private JScrollPane cscrollPane;

    /**
     * SimpleJTextPane runtime-exception class.
     * For an instance of this class to be constructed, a call
     * to SwingUtilities.invokeAndWait would have to be interrupted.
     */
    public static class RTException extends java.lang.RuntimeException {
	RTException (Throwable cause) {
	    super(cause);
	}
    }

    /**
     * Constructor.
     */
    public SimpleConsole() {
	console = new SimpleJTextPane();
	console.setSwingSafe(true);
	console.setEditable(false);
	cscrollPane =
	    new JScrollPane(console,
			    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	setLayout(new BorderLayout());
	JPanel controlPane = new JPanel();
	FlowLayout fl = new FlowLayout(FlowLayout.LEADING);
	fl.setHgap(10);
	controlPane.setLayout(fl);
	JButton clearButton = new JButton(localeString("clear"));
	controlPane.add(clearButton);
	clearButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    console.clear();
		    needNL = false;
		    needSeparator = false;
		}
	    });
	JButton printButton = new JButton(localeString("print"));
	controlPane.add(printButton);
	printButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    try {
			AccessController.doPrivileged
			    (new PrivilegedExceptionAction<Boolean>() {
				    public Boolean run()
					throws PrinterException
				    {
					return console.print
					    (null, new MessageFormat
					     ("- {0} -"));
				    }
				});
		    } catch (PrivilegedActionException pae) {
			String msg = errorMsg("pfailed", pae.getMessage());
			ErrorMessage.display(console, null, msg);
			ErrorMessage.display(pae.getCause());
		    }
		}
	    });
	JButton fileButton = new JButton(localeString("writeToFile"));
	controlPane.add(fileButton);
	final File currentDir = AccessController.doPrivileged
	    (new PrivilegedAction<File>() {
		    public File run() {
			return new File(System.getProperty("user.dir"));
		    }
		});
	fileButton.addActionListener(new ActionListener() {
		private void doit() {
		    JFileChooser fc = new JFileChooser(currentDir);
		    for (javax.swing.filechooser.FileFilter f:
			     fc.getChoosableFileFilters()) {
			fc.removeChoosableFileFilter(f);
		    }
		    FileNameExtensionFilter fnef =
			new FileNameExtensionFilter
			(localeString("textFiles"), "txt", "TXT");
		    fc.addChoosableFileFilter(fnef);
		    int status = fc.showSaveDialog(console);
		    if (status == JFileChooser.APPROVE_OPTION) {
			try {
			    File of = fc.getSelectedFile();
			    String saveFileName = of.getCanonicalPath();
			    int ind = saveFileName.lastIndexOf('.');
			    if (ind == -1) {
				saveFileName = saveFileName + ".txt";
				File nf = new File(saveFileName
						   + ".txt");
				if (nf.exists()){
				    String title = errorMsg("errorTitle");
				    String msg = errorMsg("noExt");
				    ErrorMessage.display(console, title, msg);
				    return;
				}
			    } else {
				ind++;
				String suffix =
				    saveFileName.substring(ind);
				if (!(suffix.equals("txt")
				      || suffix.equals("TXT"))) {
				    String msg =
					localeString("wrongExtension");
				    String title = errorMsg("errorTitle");
				    ErrorMessage.display(console, title, msg);
				    return;
				}
			    }
			    OutputStream os =
				new FileOutputStream(saveFileName);
			    Document doc = console.getDocument();
			    String text;
			    for (;;) {
				try {
				    text = doc.getText(0,
						       doc.getLength());
				    break;
				} catch (BadLocationException ble) {
				    continue;
				}
			    }
			    OutputStreamWriter osw =
				new OutputStreamWriter(os, "UTF-8");
			    osw.write(text, 0, text.length());
			    osw.close();
			    os.close();
			} catch(IOException eio) {
			    String msg =
				errorMsg("fileError", eio.getMessage());
			    String title = errorMsg("errorTitle");
			    ErrorMessage.display(console, title, msg);
			    ErrorMessage.display(eio);
			}
		    }
		}
		public void actionPerformed(ActionEvent e) {
		    AccessController.doPrivileged(new PrivilegedAction<Void>() {
			    public Void run() {
				doit();
				return (Void)null;
			    }
			});
		}
	    });
	add(controlPane, "North");
	add(cscrollPane, "Center");
    }

    private boolean needNL = false;


    /**
     * Run a sequence of operations atomically.
     * The argument c is typically a lambda express that will be called with
     * its single argument set to the SimpleJTextPane that this
     * console uses to display its output. Because this
     * method is run on the event dispatch queue, the operations
     *  the lambda expression
     * performs will not be interleaved with calls from
     * other threads. Alternatively, one can implement the consumer's
     * accept method instead of using a lambda expression.
     * Any changes to this console's attributes will be restored when
     * this method exits.
     * <P>
     * This method is thread safe.
     * @param c the consumer
     */
    public void perform(Consumer<SimpleConsole> c) {
	if (SwingUtilities.isEventDispatchThread()) {
	    try (SimpleJTextPane.State saved = console.saveAttributeState()) {
		c.accept(this);
	    }
	} else {
	    try {
		SwingUtilities.invokeAndWait(() -> {
			try (SimpleJTextPane.State saved =
			     console.saveAttributeState()) {
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
		    throw new SimpleConsole.RTException(thr);
	    }
	}
    }

    /**
     * Append a character to the end of the component's document.
     * <P>
     * This method is thread safe.
     * @param c the character to append
     * @return the current object cast as an Appendable
     */
    public Appendable append(char c) {
	if (SwingUtilities.isEventDispatchThread()) {
	    console.append(c);
	    needSeparator = true;
	    needNL = (c != '\n');
	} else {
	    try {
		SwingUtilities.invokeAndWait(() -> {
			console.append(c);
			needSeparator = true;
			needNL = (c != '\n');
		    });
	    } catch (InterruptedException ie) {
		throw new RuntimeException(ie);
	    } catch (InvocationTargetException ite) {
		Throwable thr = ite.getCause();
		if (thr instanceof RuntimeException)
		    throw (RuntimeException) thr;
		else
		    throw new SimpleConsole.RTException(thr);
	    }

	}
	return (Appendable) this;
    }

    /**
     * Append text to the end of the component's document.
     * <P>
     * This method is thread safe.
     * @param csq the text to append
     * @return the current object cast as an Appendable
     */
    public  Appendable append(CharSequence csq) {
	if (csq == null) return (Appendable) this;
	if (SwingUtilities.isEventDispatchThread()) {
	    console.append(csq);
	    needSeparator = true;
	    int length = csq.length();
	    if (length > 0) {
		needNL = (csq.charAt(length-1) != '\n');
	    }
	} else {
	    try {
		SwingUtilities.invokeAndWait(() -> {
			console.append(csq);
			needSeparator = true;
			int length = csq.length();
			if (length > 0) {
			    needNL = (csq.charAt(length-1) != '\n');
			}
		    });
	    } catch (InterruptedException ie) {
		throw new RuntimeException(ie);
	    } catch (InvocationTargetException ite) {
		Throwable thr = ite.getCause();
		if (thr instanceof RuntimeException)
		    throw (RuntimeException) thr;
		else
		    throw new SimpleConsole.RTException(thr);
	    }
	}
	return (Appendable) this;
    }

    /**
     * Append text to the end of the component's document.
     * <P>
     * This method is thread safe.
     * @param csq  a CharSequence containing the text to append
     * @param start the offset to the start of the text in csq
     * @param end the position just past the end of the text to
     *        in csq to append
     * @return the current object cast as an Appendable
     */
    public  Appendable append(CharSequence csq,
					  int start, int end)
    {
	if (csq == null) return (Appendable) this;
	if (SwingUtilities.isEventDispatchThread()) {
	    console.append(csq, start, end);
	    needSeparator = true;
	    if (start < end) {
		needNL = (csq.charAt(end-1) != '\n');
	    }
	} else {
	    try {
		SwingUtilities.invokeAndWait(() -> {
			console.append(csq, start, end);
			needSeparator = true;
			if (start < end) {
			    needNL = (csq.charAt(end-1) != '\n');
			}
		    });
	    } catch (InterruptedException ie) {
		throw new RuntimeException(ie);
	    } catch (InvocationTargetException ite) {
		Throwable thr = ite.getCause();
		if (thr instanceof RuntimeException)
		    throw (RuntimeException) thr;
		else
		    throw new SimpleConsole.RTException(thr);
	    }
	}
	return (Appendable) this;
    }

    private boolean needSeparator = false;

    /** Determine if there is new text to display since the
     * the last separator was added.
     * <P>
     * This method is thread safe.
     * @return true if there is text after the last separator;
     *          false if there is no text after the last
     *          separator
     */
    public boolean hasNewTextToDisplay() {
	return needSeparator;
    }


    /**
     * Add separator if needed.
     * <P>
     * This method is thread safe.
     */
    public  void addSeparatorIfNeeded() {
	if (SwingUtilities.isEventDispatchThread()) {
	    if (needSeparator) {
		addSeparator();
	    }
	    needSeparator = false;
	} else {
	    try {
		SwingUtilities.invokeAndWait(() -> {
			if (needSeparator) {
			    addSeparator();
			}
			needSeparator = false;
		    });
	    } catch (InterruptedException ie) {
		throw new RuntimeException(ie);
	    } catch (InvocationTargetException ite) {
		Throwable thr = ite.getCause();
		if (thr instanceof RuntimeException)
		    throw (RuntimeException) thr;
		else
		    throw new SimpleConsole.RTException(thr);
	    }
	}
    }

    Color separatorColor = Color.BLUE;

    /**
     * Get the color for a separator.
     * <P>
     * This method is thread safe.
     * @return the color for the separator
     */

    public Color getSeparatorColor() {
	return separatorColor;
    }

    /**
     * Set the color for the separator.
     * <P>
     * This method is thread safe.
     * @param c the color
     */
    public void setSeparatorColor(Color c) {
	separatorColor = (c == null)? Color.BLUE: c;
    }


    /**
     * Add a separator.
     */
    public  void addSeparator() {
	if (SwingUtilities.isEventDispatchThread()) {
	    Color prev = console.getTextForeground();
	    console.setTextForeground(separatorColor);
	    if (needNL) append('\n');
	    console.append("________________________________________________");
	    console.append('\n');
	    console.append('\n');
	    console.setTextForeground(prev);
	    needSeparator = false;
	} else {
	    try {
		SwingUtilities.invokeAndWait(() -> {
			Color prev = console.getTextForeground();
			console.setTextForeground(separatorColor);
			if (needNL) append('\n');
			console.append
			   ("________________________________________________");
			console.append('\n');
			console.append('\n');
			console.setTextForeground(prev);
			needSeparator = false;
		    });
	    } catch (InterruptedException ie) {
		throw new RuntimeException(ie);
	    } catch (InvocationTargetException ite) {
		Throwable thr = ite.getCause();
		if (thr instanceof RuntimeException)
		    throw (RuntimeException) thr;
		else
		    throw new SimpleConsole.RTException(thr);
	    }
	}
    }

    /**
     * Get this console's frame.
     * <P>
     * If {@link #createMenuItem(String,String,int,int)} is used, a frame
     * will be created if necessary (with restrictions). The main reason
     * to call this method otherwise is to programmatically resize a frame
     * or so that {@link Window#setIconImages} can be called.
     * @return the console's frame; null if it does not have one.
     */
    public JFrame getFrame() {
	Container c = getParent();
	while (!(c == null || c instanceof JFrame)) {
	    c = c.getParent();
	}
	if (c == null) return null;
	return (JFrame)c;
    }


    /**
     * Class to determine how an application can exit when a
     * security manager is installed and a window created with
     * {@link SimpleConsole#newFramedInstance(int,int,String,boolean,ExitAccessor)}
     * is closed.  When an instance of this class is the last argument
     * for
     * {@link SimpleConsole#newFramedInstance(int,int,String,boolean,ExitAccessor)},
     * the application will exit if the console's frame is closed and
     * an instance of this class was created when there is no security
     * manager installed.  If a security manager was installed when an
     * instance of this class was created, closing the frame will result
     * in a dialog box asking if the application should exit unless the
     * permission {@link ExitPermission} was granted with the target
     * "org.bzdev.swing.SimpleConsole" (in which case the application will
     * immediately exit).
     * @see ExitPermission
     */
    public static class ExitAccessor {
	private boolean allow;

	/**
	 * Constructor.
	 */
	public ExitAccessor() {
	    SecurityManager sm = System.getSecurityManager();
	    if (sm == null) {
		allow = true;
	    } else {
		try {
		    sm.checkPermission(new ExitPermission
				       ("org.bzdev.swing.SimpleConsole"));
		    allow = true;
		} catch (SecurityException se) {
		    allow = false;
		}
	    }
	}

	/**
	 * Return true if closing a frame created with
	 * {@link SimpleConsole#newFramedInstance(int,int,String,boolean,ExitAccessor)}
	 * can cause an application to exit; false if the user must confirm
	 * that the application will exit.
	 */
	boolean allow() {return allow;}
    }

    /**
     * Mode determining what happens when a frame created by
     * {@link SimpleConsole#newFramedInstance(int,int,String,boolean,SimpleConsole.ExitMode)}
     * closes.
     */
    public static enum ExitMode {
	/**
	 * The application always exits. If there was a security-manager
	 * installed when
	 * {@link SimpleConsole#newFramedInstance(int,int,String,boolean,SimpleConsole.ExitMode)}
	 * was called and the permission {@link ExitPermission} was not
	 * granted for the target "org.bzdev.swing.SimpleConsole", the
	 * mode is treated as if it were {@link SimpleConsole.ExitMode#ASK}.
	 */
	ALWAYS,
	/**
	 * Ask if the application should exit. If the user chooses
	 * 'OK', the application will exit; otherwise the frame will
	 * remain visible.
	 */
	ASK,
	/**
	 * The application never exits. In this case, the
	 * frame's visibility will be changed so that the frame
	 * is not visible.
	 */
	NEVER
    }

    static class Frame extends JFrame {
	SimpleConsole console;

	public Frame(String title, int width, int height,
		     SimpleConsole console, boolean visibility)
	{
	    this(title, width, height, console, visibility, ExitMode.NEVER);
	}

	public static Frame
	    newInstance(String title, int width, int height,
			SimpleConsole console, boolean visibility,
			ExitMode exitMode, ExitAccessor accessor)
	{
	    if (accessor == null) {
		if (exitMode == ExitMode.ALWAYS) {
		    SecurityManager sm = System.getSecurityManager();
		    if (sm != null) {
			try {
			    sm.checkPermission
				(new ExitPermission
				 ("org.bzdev.swing.SimpleConsole"));
			} catch (SecurityException se) {
			    exitMode = ExitMode.ASK;
			}
		    }
		}
	    } else if (accessor.allow() == false) {
		exitMode = ExitMode.ASK;
	    } else {
		exitMode = ExitMode.ALWAYS;
	    }
	    final ExitMode exitOnClose = exitMode;
	    return AccessController.doPrivileged(new PrivilegedAction<Frame>() {
		    Frame frame = null;
		    public Frame run() {
			if (SwingUtilities.isEventDispatchThread()) {
			    return new Frame(title, width, height, console,
					     visibility, exitOnClose);
			} else {
			    try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
					    frame = new Frame(title,
							      width, height,
							      console,
							      visibility,
							      exitOnClose);
					}
				    });
				return frame;
			    } catch (InterruptedException ie) {
				throw new RuntimeException(ie);
			    } catch (InvocationTargetException ite) {
				Throwable thr = ite.getCause();
				if (thr instanceof RuntimeException)
				    throw (RuntimeException) thr;
				else
				    throw new SimpleConsole.RTException(thr);
			    }
			}
		    }
		});
	}

	public Frame(String title, int width, int height,
		     SimpleConsole console, boolean visibility,
		     ExitMode exitMode/*, ExitAccessor accessor*/) {
	    super(title);
	    this.console = console;
	    Container cpane = getContentPane();
	    /*
	    if (accessor == null) {
		if (exitMode == ExitMode.ALWAYS) {
		    SecurityManager sm = System.getSecurityManager();
		    if (sm != null) {
			try {
			    sm.checkPermission
				(new ExitPermission
				 ("org.bzdev.swing.SimpleConsole"));
			} catch (SecurityException se) {
			    exitMode = ExitMode.ASK;
			}
		    }
		}
	    } else if (accessor.allow() == false) {
		exitMode = ExitMode.ASK;
	    } else {
		exitMode = ExitMode.ALWAYS;
	    }
	    final ExitMode exit = exitMode;
	    */
	    console.setPreferredSize(new Dimension(width, height));
	    addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {
			if (exitMode == ExitMode.ASK) {
			    AccessController.doPrivileged
				(new PrivilegedAction<Void>() {
					public Void run() {
					    int status =
						JOptionPane.showConfirmDialog
						(console,
						 localeString("exit"),
						 localeString("exitTitle"),
						 JOptionPane.OK_CANCEL_OPTION);
					    if (status
						== JOptionPane.OK_OPTION) {
						Frame.this.setVisible(false);
						System.exit(0);
					    } else {
						// Delay until all
						// the current events have
						// finished: otherwise the
						// code run after
						// this method was called
						// can make the window
						// invisible.
						SwingUtilities.invokeLater
						    (new Runnable() {
							    public void run() {
								Frame.this.
								    setVisible
								    (true);
							    }
							});
					    }
					    return (Void) null;
					}
				    });
			} else if (exitMode == ExitMode.NEVER) {
			    Frame.this.setVisible(false);
			} else if (exitMode == ExitMode.ALWAYS) {
			    AccessController.doPrivileged
				(new PrivilegedAction<Void>() {
					public Void run() {
					    System.exit(0);
					    return (Void)null;
					}
				    });
			}
		    }
		});
	    cpane.setLayout(new BorderLayout());
	    cpane.add(console, "Center");
	    pack();
	    setVisible(visibility);
	}
    }

    /**
     * Create a console with a surrounding frame.
     * When the frame is closed, it will no longer be visible.
     * This method must be called on the event dispatch thread.
     * Its documented behavior could be altered if the caller
     * adds additional window listeners.
     * @param width the width of the console in points
     * @param height the height of the console  in points
     * @param title the title of the frame
     * @param visibility true if the frame is initially visible;
     *        false otherwise.
     * @return a console whose pane is a component of a JFrame.
     * @see SwingUtilities#invokeAndWait(Runnable)
     * @see ExitPermission
     */
    public static SimpleConsole
	newFramedInstance(int width, int height, String title,
			  boolean visibility)
    {
	SimpleConsole console = new SimpleConsole();
	new SimpleConsole.Frame(title, width, height, console, visibility,
				ExitMode.NEVER);
	return console;
    }


    /**
     * Create a console with a surrounding frame, specifying an exit
     * policy.
     * The exit policy is describe by the documentation for the
     * enumeration type {@link SimpleConsole.ExitMode}.
     * This method must be called on the event dispatch thread.
     * Its documented behavior could be altered if the caller
     * adds additional window listeners.
     * @param width the width of the console in points
     * @param height the height of the console  in points
     * @param title the title of the frame
     * @param visibility true if the frame is initially visible;
     *        false otherwise.
     * @param exit the exit mode
     * @return a console whose pane is a component of a JFrame.
     * @see SwingUtilities#invokeAndWait(Runnable)
     * @see SimpleConsole.ExitMode
     * @see ExitPermission
     */
    public static SimpleConsole
	newFramedInstance(int width, int height, String title,
			  boolean visibility, ExitMode exit)
    {
	SimpleConsole console = new SimpleConsole();
	SimpleConsole.Frame.newInstance(title, width, height, console,
					visibility, exit, null);
	return console;
    }

    /**
     * Create a console with a surrounding frame and option to exit
     * based on an exit accessor.
     * If the exit accessor argument was created when no security
     * manager is installed, closing the frame will cause the application
     * to exit.  If the exit accessor argument was created when a
     * security manager is installed, closing the frame will result in
     * the appearance of a dialog box asking if the application should
     * exit, and canceling that operation will result in the window
     * being reopened (i.e., becoming visible again).
     * <P>
     * This method must be called on the event dispatch thread.
     * Its documented behavior could be altered if the caller
     * adds additional window listeners.
     * @param width the width of the console in points
     * @param height the height of the console  in points
     * @param title the title of the frame
     * @param visibility true if the frame is initially visible;
     *        false otherwise.
     * @param accessor an exit accessor, used to determine how and if
     *        an application exits when a console's frame is closed
     * @return a console whose pane is a component of a JFrame.
     * @see SwingUtilities#invokeAndWait(Runnable)
     * @see ExitPermission
     */
    public static SimpleConsole
	newFramedInstance(int width, int height, String title,
			  boolean visibility, ExitAccessor accessor)
    {
	SimpleConsole console = new SimpleConsole();
	SimpleConsole.Frame.newInstance(title, width, height, console,
					visibility, ExitMode.ALWAYS, accessor);
	return console;
    }

    /**
     * Menu Item for opening a SimpleConsole.
     * The menu item should be created before an application needs
     * to use a proxy when this menu item is going to be used, unless
     * org.bzdev.swing.proxyconf.ProxyComponent.setProxies is called.
     * Otherwise, the preference database will not be read before a
     * network connection is attempted.
     */
    static class OurMenuItem extends JMenuItem {
	JFrame consoleFrame;

	/**
	 * Constructor.
	 * @param label the label of the menu item
	 * @param frame the frame contains the  console.
	 */
	public OurMenuItem(String label, JFrame frame)
	{
	    super(label);
	    consoleFrame = frame;

	    addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			consoleFrame.setVisible(true);
		    }
		});
	}
    }

    /**
     * Create a new menu item that will open a frame containing
     * this console.
     * <P>
     * This method will provide a frame for this console if such a frame
     * does not already exist. This method must not be called if the
     * console was added to a component and does not have a JFrame as
     * an ancestor.  Otherwise it may be called multiple times (e.g., to
     * add a menu item to open a console in multiple windows).
     * @param label the label to use for the menu item
     * @param title the  title to use if a new frame is created
     * @param width the minimum frame-width to use (if there is an
     *        existing frame width that is larger, that will be used
     *        instead)
     * @param height the minimum frame-height to use (if there is an
     *        existing frame height that is larger, that will be used
     *        instead)
     * @return the menu item for this console
     * @exception IllegalStateException the console was added to a component
     *            but does not have a frame
     */
    public JMenuItem createMenuItem(String label, String title,
				    int width, int height)
	throws IllegalStateException
    {
	JFrame frame = getFrame();
	if (frame == null) {
	    if (getParent() != null) {
		throw new IllegalStateException();
	    }
	    frame = new Frame(title, width, height, this, false);
	} else {
	    int fw = frame.getWidth();
	    int fh = frame.getHeight();
	    boolean sizeChanged = false;
	    if (width < fw) {
		width = fw;
		sizeChanged = true;
	    }
	    if (height < fh) {
		height = fh;
		sizeChanged = true;
	    }
	    if (sizeChanged) frame.setSize(width, height);
	}
	return new OurMenuItem(label, frame);
    }
    // Replicate operations from SimpleJTextPane

    /**
     *  Determine if newly inserted text will use a bold font.
     * <P>
     * This method is thread safe.
     * @return true if newly inserted text will be bold; false
     *         otherwise
     */
    public boolean isBold() {
	return console.isBold();
    }

    /**
     * Set the font weight for subsequently inserted text to either
     * normal or bold.
     * <P>
     * This method is thread safe.
     * @param value true if the font is bold; false if not.
     */
    public void setBold(boolean value) {
	console.setBold(value);
    }

    /**
     * Determine if newly inserted text will be in italics.
     * <P>
     * This method is thread safe.
     * @return true if newly inserted text will be in italics; false
     *         otherwise
     */
    public boolean isItalic() {
	return console.isItalic();
    }

    /**
     * Set the font slant for subsequently inserted text.
     * <P>
     * This method is thread safe.
     * @param value true if the font is italic; false if not.
     */
    public void setItalic(boolean value) {
	console.setItalic(value);
    }

    /**
     * Get the color of subsequently inserted or appended text.
     * <P>
     * This method is thread safe.
     * @return The color that will be used for new text.
     */
    public Color getTextForeground() {
	return console.getTextForeground();
    }

    /**
     * Set the text color for new text.
     * <P>
     * This method is thread safe.
     * @param fg the color for subsequently inserted or appended text.
     */
    public void setTextForeground(Color fg) {
	console.setTextForeground(fg);
    }

    /**
     * Get the background color for subsequently inserted or appended text.
     * <P>
     * This method is thread safe.
     * @return the color that will be used for the background for  new text.
     */
    public Color getTextBackground() {
	return console.getTextBackground();
    }

    /**
     * Set the background color for new text.
     * <P>
     * This method is thread safe.
     * @param bg the background color for subsequently inserted or
     *            appended text.
     */
    public void setTextBackground(Color bg) {
       console.setTextBackground(bg);
    }

    /**
     * Get the background color for the console.
     * @return the background color
     */
    public Color getBackground() {
	return console.getBackground();
    }

    /**
     * Set the background color for the console.
     * @param bg the background color for subsequently inserted or
     *            appended text.
     */
    public void setBackground(Color bg) {
	console.setBackground(bg);
    }
}

//  LocalWords:  exbundle PrintWriter blockquote SimpleConsole pre fg
//  LocalWords:  AppendableWriter ErrorMessage JFrame menubar JMenu
//  LocalWords:  JMenuBar JMenuItem consoleMenuItem createMenuItem bg
//  LocalWords:  setJMenuBar setVisible setAppendable setComponent sm
//  LocalWords:  stackTraceMenuItem Stacktrace SwingUtilities AWT dir
//  LocalWords:  invokeLater Runnable experiement SimpleJTextPane txt
//  LocalWords:  runtime invokeAndWait pfailed writeToFile textFiles
//  LocalWords:  errorTitle noExt wrongExtension UTF fileError csq se
//  LocalWords:  Appendable CharSequence  setIconImages PanelGraphics
//  LocalWords:  newFramedInstance boolean ExitAccessor ExitMode
//  LocalWords:  exitTitle accessor IllegalStateException exitMode
//  LocalWords:  programmatically AnimatedPanelGraphics
//  LocalWords:  ExitPermission SecurityManager getSecurityManager
//  LocalWords:  checkPermission SecurityException
