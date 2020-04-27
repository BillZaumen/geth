package org.bzdev.swing;

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.GraphicsConfiguration;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.*;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Stack;

//@exbundle org.bzdev.swing.lpack.Swing

/**
 * A widget for displaying HTML documents.
 * This class wraps a {@link JEditorPane} in a {@link JScrollPane} and
 * provides a set of buttons for navigation.  Links that appear in the
 * HTML may be clicked to visit a URL.  The buttons provide the
 * following functions.
 * <ul>
 *   <li>The "start" button takes you to the URL provided by the
 *       <code>setPage</code> method.  It is equivalent to the maximum
 *       possible number of repetitions of the "back" button.
 *   <li> The "back" button returns to the page that was last visited
 *        by clicking on a link.
 *   <li> The "forw" button moves forward, undoing the effects of pushing
 *        the "back" button.
 *   <li> The "end" button is equivalent to the repeated use of the
 *        "forw" button.
 *   <li> The "reload" button reloads the link currently visited.
 * </ul> 
 * Note: the use of these buttons does not restore any scrolling that
 * the user may have performed.  The buttons contain icons instead of
 * text, but have text-based tooltips. The icons are in resources named
 * org/bzdev/swing/icons/fleft.gif, org/bzdev/swing/icons/left.gif,
 * org/bzdev/swing/icons/redo.gif, org/bzdev/swing/icons/right.gif,
 * org/bzdev/swing/icons/fright.gif, and org/bzdev/swing/icons/rlredo.gif.
 * <p>
 * The MIME types supported are those that a {@link JEditorPane}
 * supports.
 * <p>
 * Internationalization is handled by the resource
 * <code>org/bzdev/swing/HtmlPaneBundle.properties</code>.  This defines
 * the strings startText, backText,  reloadText, frwdText, endText. as
 * values to use in buttons if icons are not provided.  The strings
 * startTip, backTip, reloadTip, frwdTip, and endTip provide corresponding
 * tool tips.  Finally, the string direction indicates if the components
 * should be laid out LR (left to right) or RL (right to left.) This
 * also has an effect on the how icons are assigned to buttons.  The
 * icon rlredo.gif is the mirror image of redo.gif and is provided for
 * right-to-left layouts.
 * <P>
 * One can set the HTML page displayed by calling one of the setPage
 * methods or by using a constructor that takes a URL or a String
 * specifying a URL as an argument.
 * <P>
 * This component allows HTML pages to be displayed but does not allow
 * them to be edited.
 * @author Bill Zaumen
 */
public class HtmlPane extends JComponent {

    static String errorMsg(String key, Object... args) {
	return SwingErrorMsg.errorMsg(key, args);
    }

    private class StackElement {
	URL url;
	HTMLFrameHyperlinkEvent event;
	StackElement(URL url, HTMLFrameHyperlinkEvent env) {
	    this.url = url;
	    event = env;
	}
    }
    private HtmlPane me = null;
    private JEditorPane editorPane = new JEditorPane();
    private JScrollPane editorScrollPane = new JScrollPane(editorPane);
    JPanel top = new JPanel();

    public void setLocale(Locale locale) {
	super.setLocale(locale);
	editorScrollPane.setLocale(locale);
	editorPane.setLocale(locale);
    }

    /** Set the local of the content pane.
     * This method allows the locale of the content panes (the HTML
     * pane) to be set without changing the layout of navigation
     * controls.
     * @param locale the locale.
     */
    public void setContentLocale(Locale locale) {
	editorPane.setLocale(locale);
    }

    private KeyAdapter editorKeyAdapter = new KeyAdapter() {
	    private Rectangle toRect(Rectangle2D r) {
		double x = r.getX();
		double y = r.getY();
		double w = r.getWidth();
		double h = r.getHeight();
		int ix = (int)Math.round(x);
		int iy = (int)Math.round(y);
		int iw =(int)Math.ceil(w);
		int ih =(int)Math.ceil(h);
		return new Rectangle(ix, iy, iw, ih);
	    }

	    public void keyPressed(KeyEvent e) {
		int keycode = e.getKeyCode();
		int mod = e.getModifiersEx();
		if (mod != 0) return;
		switch (keycode) {
		case KeyEvent.VK_HOME:
		    editorPane.setCaretPosition(0);
		    try {
			editorPane.scrollRectToVisible
			    (toRect(editorPane.modelToView2D(0)));
		    } catch (Exception badloc) {}
		    break;
		case KeyEvent.VK_END:
		    int len = editorPane.getDocument().getLength();
		    editorPane.setCaretPosition(len);
		    try {
			editorPane.scrollRectToVisible
			    (toRect(editorPane.modelToView2D(len)));
		    } catch (Exception badloc) {}
		    
		    break;
		}
	    }
	};
    // private Stack urlstack = new Stack();
    // private Stack revUrlstack = new Stack();
    private Stack<StackElement> urlstack = new Stack<StackElement>();
    private Stack<StackElement> revUrlstack = new Stack<StackElement>();

    // use for internationalization - appears in tooltips.
    private static String startTip = "startTip";
    private static String backTip = "backTip";
    private static String reloadTip = "reloadTip";
    private static String frwdTip = "frwdTip";
    private static String endTip = "endTip";

    // internationalization:  button strings;
    final private static String startText = "startText";
    final private static String backText = "backText";
    final private static String reloadText = "reloadText";
    final private static String frwdText = "frwdText";
    final private static String endText = "endText";

    // internationalization: direction
    final private String direction = "direction";
    final private String bundleName = "org.bzdev.swing.lpack.HtmlPaneBundle";

    ImageIcon frightIcon = null;
    ImageIcon rightIcon = null;
    ImageIcon reloadIcon = null;
    ImageIcon rlReloadIcon = null;
    ImageIcon leftIcon = null;
    ImageIcon fleftIcon = null;
    private boolean iconsInitialized = false;

    private void initIcons() {
	if (iconsInitialized) return;
	AccessController.doPrivileged
	    (new PrivilegedAction<Void>() {
		    public Void run() {
			URL frightUrl = HtmlPane.class
			    .getResource("/org/bzdev/swing/icons/fright.gif");
			URL rightUrl = HtmlPane.class
			    .getResource("/org/bzdev/swing/icons/right.gif");
			URL reloadUrl = HtmlPane.class
			    .getResource("/org/bzdev/swing/icons/redo.gif");
			URL rlReloadUrl = HtmlPane.class
			    .getResource("/org/bzdev/swing/icons/rlredo.gif");
			URL leftUrl = HtmlPane.class
			    .getResource("/org/bzdev/swing/icons/left.gif");
			URL fleftUrl = HtmlPane.class
			    .getResource("/org/bzdev/swing/icons/fleft.gif");
			/*
			URL frightUrl = ClassLoader.getSystemClassLoader()
			    .getResource("org/bzdev/swing/icons/fright.gif");
			URL rightUrl = ClassLoader.getSystemClassLoader()
			    .getResource("org/bzdev/swing/icons/right.gif");
			URL reloadUrl = ClassLoader.getSystemClassLoader()
			    .getResource("org/bzdev/swing/icons/redo.gif");
			URL rlReloadUrl = ClassLoader.getSystemClassLoader()
			    .getResource("org/bzdev/swing/icons/rlredo.gif");
			URL leftUrl = ClassLoader.getSystemClassLoader()
			    .getResource("org/bzdev/swing/icons/left.gif");
			URL fleftUrl = ClassLoader.getSystemClassLoader()
			    .getResource("org/bzdev/swing/icons/fleft.gif");
			*/
			frightIcon = (frightUrl == null)? null:
			    new ImageIcon(frightUrl);
			rightIcon = (frightUrl == null)? null:
			    new ImageIcon(rightUrl);
			reloadIcon = (reloadUrl == null)? null:
			    new ImageIcon(reloadUrl);
			rlReloadIcon = (reloadUrl == null)? null:
			    new ImageIcon(rlReloadUrl);
			leftIcon = (frightUrl == null)? null:
			    new ImageIcon(leftUrl);
			fleftIcon = (fleftUrl == null)? null:
			    new ImageIcon(fleftUrl);
			return (Void)null;
		    }
		});
	iconsInitialized = true;
    }

    private JButton startButton;
    private JButton backButton;
    private JButton reloadButton;
    private JButton frwdButton;
    private JButton endButton;

    HTMLFrameHyperlinkEvent event = null;
    private HyperlinkListener hl = new HyperlinkListener () {
	    public void hyperlinkUpdate(HyperlinkEvent e) {
		try {
		    JEditorPane pane = (JEditorPane) e.getSource();
		    URL current = /* pane. */getPage();
		    if (e.getEventType() == 
			HyperlinkEvent.EventType.ACTIVATED) {
			if (e instanceof HTMLFrameHyperlinkEvent) {
			    HTMLFrameHyperlinkEvent  evt = 
				(HTMLFrameHyperlinkEvent)e;
			    HTMLDocument doc =
				(HTMLDocument)pane.getDocument();
			    doc.processHTMLFrameHyperlinkEvent(evt);
			    urlstack.push(new StackElement(current, event));
			    revUrlstack.clear();
			    startButton.setEnabled(true);
			    backButton.setEnabled(true);
			    frwdButton.setEnabled(false);
			    endButton.setEnabled(false);
			    event = evt;
			} else {
			    pane.setPage(e.getURL());
			    page = e.getURL();
			    urlstack.push(new StackElement(current, event));
			    revUrlstack.clear();
			    startButton.setEnabled(true);
			    backButton.setEnabled(true);
			    frwdButton.setEnabled(false);
			    endButton.setEnabled(false);
			    event = null;
			}
		    }
		} catch (Throwable t) {
		    String msg = errorMsg("cannotOpenURL", e.getURL());
		    JOptionPane.showMessageDialog(me,
						  msg,
						  errorTitle,
						  JOptionPane.ERROR_MESSAGE);
		}
	    }
	};

    private void setPageAux() {
	urlstack.clear();
	revUrlstack.clear();
	reloadButton.setEnabled(true);
	startButton.setEnabled(false);
	frwdButton.setEnabled(false);
	backButton.setEnabled(false);
	endButton.setEnabled(false);
    }

    /**
     * Set the page to display and display it.
     * @param page the URL of the page to display.
     * @throws IOException an error occurred reading the page.
     */
    public void setPage(String page) throws IOException {
	setPage (new URL(page));
    }

    // Java 1.4.1 does not keep track of the reference or fragment
    // field when setPage is called, so we keep track of it just in
    // case.
    private URL page = null;
    /**
     * Set the page to display and display it.
     * @param page the URL of the page to display.
     * @throws IOException an error occurred reading the page.
     */
    public void setPage(URL page) throws IOException {
	editorPane.setPage(page);
	this.page = page;
	setPageAux();
    }

    /**
     * get the URL for the page being displayed.
     * @return the URL for the page being displayed;  null if there is none.
     */
    public URL getPage() {return page /* editorPane.getPage()*/;}

    /**
     * The string that will be displayed as a frame title when a dialog
     * box reporting an error is displayed.  The default is obtained
     * from the resource bundle org.bzdev.swing.lpack.HtmlPaneBundle
     */
    protected String errorTitle;

    /**
     * Set the title to use in error message dialog boxes.
     * This title will be used as a title for various dialog boxes.
     * @param title the title to display; null to use the default
     *        (which is locale specific)
     */
    public final void setErrorTitle (String title) {
	if (title == null) {
	    ResourceBundle bundle =
		ResourceBundle.getBundle(bundleName, getLocale());
	    errorTitle = bundle.getString("errorTitle");
	} else {
	    errorTitle = title;
	}
    }

    /**
     * Get the title used in error message dialog boxes.
     * @return the current title.
     */
    public final String getErrorTitle() {return errorTitle;}

    boolean lrmode = true;
    public void setComponentOrientation(ComponentOrientation o) {
	initIcons();
	boolean oldlrmode = lrmode;
	super.setComponentOrientation(o);

	// editorScrollPane.applyComponentOrientation(o);
	// top.applyComponentOrientation(o);
	lrmode = o.isLeftToRight();


	if (lrmode != oldlrmode) {
	    /*
	     * change icons.
	     */
	    ResourceBundle bundle = 
		ResourceBundle.getBundle(bundleName, getLocale());
	    top.remove(startButton);
	    top.remove(backButton);
	    top.remove(reloadButton);
	    top.remove(frwdButton);
	    top.remove(endButton);
	    startButton.setIcon(null);
	    backButton.setIcon(null);
	    reloadButton.setIcon(null);
	    frwdButton.setIcon(null);
	    endButton.setIcon(null);
	    startButton.setDisabledIcon(null);
	    backButton.setDisabledIcon(null);
	    reloadButton.setDisabledIcon(null);
	    frwdButton.setDisabledIcon(null);
	    endButton.setDisabledIcon(null);
	    if (lrmode) {
		startButton.setIcon(fleftIcon);
		backButton.setIcon(leftIcon);
		reloadButton.setIcon(reloadIcon);
		frwdButton.setIcon(rightIcon);
		endButton.setIcon(frightIcon);
	    } else {
		startButton.setIcon(frightIcon);
		backButton.setIcon(rightIcon);
		reloadButton.setIcon(rlReloadIcon);
		frwdButton.setIcon(leftIcon);
		endButton.setIcon(fleftIcon);
	    }
	    startButton.setToolTipText(bundle.getString(startTip));
	    backButton.setToolTipText(bundle.getString(backTip));
	    reloadButton.setToolTipText(bundle.getString(reloadTip));
	    frwdButton.setToolTipText(bundle.getString(frwdTip));
	    endButton.setToolTipText(bundle.getString(endTip));

	    top.add(startButton);
	    top.add(backButton);
	    top.add(reloadButton);
	    top.add(frwdButton);
	    top.add(endButton);
	}
    }

    /**
     * Class constructor.
     */
    public HtmlPane()  {
	super();
	me = this;
	initIcons();
	Locale locale = getLocale();
	ResourceBundle bundle = 
	    ResourceBundle.getBundle(bundleName, locale);

	errorTitle = bundle.getString("errorTitle");

	if (frightIcon != null && rightIcon != null && reloadIcon != null &&
	    leftIcon != null && fleftIcon != null) {
	    startButton = new JButton(fleftIcon);
	    backButton = new JButton(leftIcon);
	    reloadButton = new JButton(reloadIcon);
	    frwdButton = new JButton(rightIcon);
	    endButton = new JButton(frightIcon);
	} else {
	    // Some icons not included - use text instead.
	    startButton = new JButton(bundle.getString(startText));
	    backButton = new JButton(bundle.getString(backText));
	    reloadButton = new JButton(bundle.getString(reloadText));
	    frwdButton = new JButton(bundle.getString(frwdText));
	    endButton = new JButton(bundle.getString(endText));
	}
	startButton.setToolTipText(bundle.getString(startTip));
	backButton.setToolTipText(bundle.getString(backTip));
	reloadButton.setToolTipText(bundle.getString(reloadTip));
	frwdButton.setToolTipText(bundle.getString(frwdTip));
	endButton.setToolTipText(bundle.getString(endTip));

	startButton.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    
		    URL current = /*editorPane.*/getPage();
		    StackElement element = (StackElement)urlstack.elementAt(0);
		    HTMLDocument doc =
			(HTMLDocument)editorPane.getDocument();
		    try {
			editorPane.setPage(element.url);
			page = element.url;
			if (element.event != null) {
			    doc.processHTMLFrameHyperlinkEvent(element.
							       event);
			}
			StackElement el = new StackElement(current, event);
			do {
			    revUrlstack.push(el);
			    el = (StackElement)urlstack.pop();
			} while (el != element);
			event = element.event;
			frwdButton.setEnabled(true);
			endButton.setEnabled(true);
			if (urlstack.empty()) {
			    backButton.setEnabled(false);
			    startButton.setEnabled(false);
			}
		    } catch (Exception ee) {
			boolean fixed = true;
			try {
			    editorPane.setPage(current);
			    page = current;
			    if (event != null) {
				doc.processHTMLFrameHyperlinkEvent(event);
			    }
			    urlstack.push(element);
			} catch (Exception eee){fixed = false;}
			if (fixed) {
			    JOptionPane.
				showMessageDialog(me,
						  "Cannot go back to " 
						  +element.url,
						  errorTitle,
						  JOptionPane.ERROR_MESSAGE);
			} else {
			    JOptionPane.
				showMessageDialog(me,
						  "Navigation fault: "
						  +"cannot recover",
						  errorTitle,
						  JOptionPane.ERROR_MESSAGE);
			}
		    }
		}
	    });

	reloadButton.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    URL current = /*editorPane.*/getPage();
		    try {
			editorPane.setPage(current);
			page = current;
			if (event != null) {
			    HTMLDocument doc =
				(HTMLDocument)editorPane.getDocument();
			    doc.processHTMLFrameHyperlinkEvent(event);
			}
		    } catch (Exception ee) {
			    JOptionPane.
				showMessageDialog(me,
						  "Cannot reload page",
						  errorTitle,
						  JOptionPane.ERROR_MESSAGE);
			
		    }
		}
	    });
	backButton.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    
		    URL current = /*editorPane.*/getPage();
		    StackElement element = (StackElement)urlstack.pop();
		    HTMLDocument doc =
			(HTMLDocument)editorPane.getDocument();
		    try {
			editorPane.setPage(element.url);
			page = element.url;
			if (element.event != null) {
			    doc.processHTMLFrameHyperlinkEvent(element.
							       event);
			}
			revUrlstack.push(new StackElement(current, event));
			event = element.event;
			frwdButton.setEnabled(true);
			endButton.setEnabled(true);
			if (urlstack.empty()) {
			    backButton.setEnabled(false);
			    startButton.setEnabled(false);
			}
		    } catch (Exception ee) {
			boolean fixed = true;
			try {
			    editorPane.setPage(current);
			    page = current;
			    if (event != null) {
				doc.processHTMLFrameHyperlinkEvent(event);
			    }
			    urlstack.push(element);
			} catch (Exception eee){fixed = false;}
			if (fixed) {
			    JOptionPane.
				showMessageDialog(me,
						  "Cannot go back to " 
						  +element.url,
						  errorTitle,
						  JOptionPane.ERROR_MESSAGE);
			} else {
			    JOptionPane.
				showMessageDialog(me,
						  "Navigation fault: "
						  +"cannot recover",
						  errorTitle,
						  JOptionPane.ERROR_MESSAGE);
			}
		    }
		}
	    });
	frwdButton.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    URL current = /*editorPane.*/getPage();
		    StackElement element = (StackElement)revUrlstack.pop();
		    HTMLDocument doc =
			(HTMLDocument)editorPane.getDocument();
		    try {
			editorPane.setPage(element.url);
			page = element.url;
			if (element.event != null) {
			    doc.processHTMLFrameHyperlinkEvent(element.
							       event);
			}
			urlstack.push(new 
				      StackElement(current, event));
			event = element.event;
			backButton.setEnabled(true);
			startButton.setEnabled(true);
			if (revUrlstack.empty()) {
			    frwdButton.setEnabled(false);
			    endButton.setEnabled(false);
			}
		    } catch (Exception eee) {
			boolean fixed = true;
			try {
			    editorPane.setPage(current);
			    page = current;
			    if (event != null) {
				doc.processHTMLFrameHyperlinkEvent(event);
			    }
			    revUrlstack.push(element);
			} catch (Exception eeee) {fixed = false;}
			if (fixed) {
			    JOptionPane.
				showMessageDialog(me,
						  "Cannot go to " 
						  +element.url,
						  errorTitle,
						  JOptionPane.ERROR_MESSAGE);
			} else {
			    JOptionPane.
				showMessageDialog(me,
						  "Navigation fault: "
						  +"cannot recover",
						  errorTitle,
						  JOptionPane.ERROR_MESSAGE);
			}
		    }
		}
	    });
	endButton.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    URL current = /*editorPane.*/getPage();
		    StackElement element = 
			(StackElement)revUrlstack.elementAt(0);
		    HTMLDocument doc =
			(HTMLDocument)editorPane.getDocument();
		    try {
			editorPane.setPage(element.url);
			page = element.url;
			if (element.event != null) {
			    doc.processHTMLFrameHyperlinkEvent(element.
							       event);
			}
			StackElement el = new StackElement(current, event);
			do  {
			    urlstack.push(el);
			    el = (StackElement)revUrlstack.pop();
			}
			while(el != element);
			event = element.event;
			backButton.setEnabled(true);
			startButton.setEnabled(true);
			if (revUrlstack.empty()) {
			    frwdButton.setEnabled(false);
			    endButton.setEnabled(false);
			}
		    } catch (Exception eee) {
			boolean fixed = true;
			try {
			    editorPane.setPage(current);
			    page = current;
			    if (event != null) {
				doc.processHTMLFrameHyperlinkEvent(event);
			    }
			    revUrlstack.push(element);
			} catch (Exception eeee) {fixed = false;}
			if (fixed) {
			    JOptionPane.
				showMessageDialog(me,
						  "Cannot go to " 
						  +element.url,
						  errorTitle,
						  JOptionPane.ERROR_MESSAGE);
			} else {
			    JOptionPane.
				showMessageDialog(me,
						  "Navigation fault: "
						  +"cannot recover",
						  errorTitle,
						  JOptionPane.ERROR_MESSAGE);
			}
		    }
		}
	    });
	editorPane.setEditable(false);
	editorPane.addHyperlinkListener(hl);
	setLayout(new BorderLayout());
	// top.setComponentOrientation(co);

	top.add(startButton);
	top.add(backButton);
	top.add(reloadButton);
	top.add(frwdButton);
	top.add(endButton);

	startButton.setEnabled(false);
	backButton.setEnabled(false);
	reloadButton.setEnabled(false);
	frwdButton.setEnabled(false);
	endButton.setEnabled(false);
	add(top, "North");
	add(editorScrollPane, "Center");
	editorPane.addKeyListener(editorKeyAdapter);
    }

    /**
     * Class constructor based on a url.
     * @param url the URL that the pane should initially display
     * @throws IOException an error setting the page occured.
     */
    public HtmlPane(String url) throws IOException {
	this();
	setPage(url);
    }

    /**
     * Class constructor based on a url.
     * @param url the URL that the pane should initially display
     * @throws IOException an error setting the page occured.
     */
    public HtmlPane(URL url) throws IOException {
	this();
	setPage(url);
    }
}
