package org.bzdev.swing;

import javax.swing.JMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JComponent;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import java.util.Locale;
import java.util.ResourceBundle;
import java.net.URL;
import java.io.IOException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.ComponentOrientation;

/**
 * A menu item for displaying help.
 * Menu items of this type should be installed in a 'help' menu.  A
 * URL provides the contents of a help window, which contains either
 * an HTML file suitable for an HtmlPane (basically HTML 3.2) or an
 * XML file suitable for an HtmlWithTocPane.  If the URL's path ends
 * with the suffix"xml" (".xml"), an HtmlWithTocPane is used; otherwise
 * an HtmlPane is used. A description of the XML format can be found
 * in the documentation for {@link HtmlWithTocPane}.
 * <P>
 * Once the frame is created, it will persist for
 * the lifetime of an application. Closing a window simply makes the
 * frame invisible.  This allows the help window to maintain its state
 * as the application runs.  Constructors may also provide a
 * resource-bundle name and keys for the menu-item title, URL, and
 * help-frame title.
 * <P>
 * If some or all of the Help HTML files are stored in an
 * application's JAR file or JAR files, on can use the "sresource" or
 * "resource" URL scheme to access them.  In this case one should call
 * the method {@link org.bzdev.protocols.Handlers#enable()} in order
 * to enable the "resource" and "sresource" URL schemes.  It may also
 * be necessary to set some Java properties (e.g., with the java -D
 * option).
 * <P>
 * As an example, Suppose the file toc.xml is accessed using the
 * URL file:///PATH/toc.xml, where PATH shoudl be replace by a path
 * from the root directory to the directory that contains toc.xml, and
 * that toc.xml contains the following:
 * <blockquote><code><pre>
 *  &lt;?xml version="1.0" ?&gt;
 * &lt;!DOCTYPE toc SYSTEM "sresource:/org/bzdev/swing/toc.dtd"&gt;
 * &lt;toc&gt;
 *   &lt;node title="table of contents" uri="file:///PATH/manual.html" &gt;
 *     &lt;node title="chapter1" uri="file:///PATH/manual.html#gui" /&gt;
 *     &lt;node title="chapter2" uri="file:///PATH/manual.html#output" &gt;
 *       &lt;node title="Section1" uri="file:///PATH/manual.html#request" /&gt;
 *       &lt;node title="Section2" uri="file:///PATH/manual.html#prefs" /&gt;
 *       &lt;node title="Section3" uri="file:///PATH/manual.html#menus" /&gt;
 *     &lt;/node&gt;
 *     &lt;node title="chapter3" uri="file:///PATH/manual.html#terminal" /&gt;
 *   &lt;/node&gt;
 * &lt;/toc&gt;
 * </pre></code></blockquote>
 * Also suppose that the file manual.html is accessed using the URL
 * file:///PATH/manual.html and that manual.html is an HTML 3.2 file
 * with anchors names "gui", "output", "request", "prefs", menus", and
 * "terminal". Then the following code will create on-line help for an
 * application and make it accessible from a "Help" menu.
 * <blockquote><code><pre>
 *    JFrame frame = new JFrame("Application");
 *    JMenuBar menubar = new JMenuBar();
 *    ...
 *    JMenuItem helpMenuItem =
 *       new HelpMenuItem("Help", "file:///PATH/toc.xml",
                          "Help", 800, 600);
 *    JMenu helpMenu = new JMenu("Help");
 *    helpMenu.add(helpMenuItem);
 *    menubar.add(helpMenu);
 *    ...
 *    frame.setJMenuBar(menubar);
 *    ...
 * </pre></code></blockquote>
 * Other constructors can be used when internationalization is needed.
 * For a HelpMenuItem, the internationalized constructors uses a
 * resource bundle to look up the titles and uris based on a locale.
 * @see HtmlPane
 * @see HtmlWithTocPane
 * @see org.bzdev.protocols.Handlers
 * @author Bill Zaumen
 */
public class HelpMenuItem extends JMenuItem {
    JFrame helpframe = null;

    private String bundleName = null;
    private String titleKey = null;
    private String urlKey = null;
    private String menuItemNameKey = null;

    private String menuItemName = null;
    private String title;
    private int xsize;
    private int ysize;
    private URL url;

    JComponent helpPane = null;

    private void setURL(URL url) 
	throws IOException, 
	       org.xml.sax.SAXException, 
	       javax.xml.parsers.ParserConfigurationException
    {
	Container hpane = helpframe.getContentPane();
	boolean tocmode =  url.getPath().endsWith(".xml");
	if (tocmode) {
	    if (helpPane instanceof HtmlWithTocPane) {
		((HtmlWithTocPane)helpPane).setToc(url, 
						   false, false);
	    } else {
		hpane.remove(helpPane);
		helpPane = new HtmlPane(url);
		hpane.add (helpPane);
	    }
	} else {
	    if (helpPane instanceof HtmlWithTocPane) {
		hpane.remove(helpPane);
		helpPane = new HtmlWithTocPane(url, false, false);
		hpane.add(helpPane);
	    } else {
		((HtmlPane)helpPane).setPage(url);
	    }
	}
    }

    /**
     * Set the locale.
     * @param locale the locale
     */
    public void setLocale(Locale locale) {
	super.setLocale(locale);
	ResourceBundle bundle = (bundleName != null)?
	    ResourceBundle.getBundle(bundleName, locale): null;
	String newtitle = null;
	String newlabel = null;
	String newurl = null;
	if (bundle != null) {
	    newtitle = bundle.getString(titleKey);
	    newlabel = bundle.getString(menuItemNameKey);
	    newurl = bundle.getString(urlKey);
	}
	URL oldurl = url;
	if (newurl !=  null) {
	    try {
		url = new URL(newurl);
	    } catch (Exception e) {
		url = null;
	    }
	}
	if (newtitle != null) {
	    title = newtitle;
	    helpframe.setTitle(title);
	} else {
	    helpframe.setTitle("");
	}
	if (newlabel != null) {
	    setText(newlabel);
	} else {
	    setText("");
	}
	if (helpPane != null) {
	    if (url != null) {
		try {
		    setURL(url);
		} catch (IOException e) {
		    url = oldurl;
		    return;
		} catch (org.xml.sax.SAXException ee) {
		    url = oldurl;
		    return;
		} catch (javax.xml.parsers.ParserConfigurationException eee) {
		    url = oldurl;
		    return;
		}
		if (helpframe != null && helpframe.isDisplayable()) {
		    helpframe.validate();
		}
	    } else {
		Container hpane = helpframe.getContentPane();
		hpane.remove(helpPane);
		helpPane = null;
	    }
	}
    }

    public void setComponentOrientation(ComponentOrientation o)
    {
	super.setComponentOrientation(o);
	if (helpframe != null)
	    helpframe.applyComponentOrientation(getComponentOrientation());
    }


    private void showHelp() {
	ResourceBundle bundle;
	Locale locale = getLocale();
	URL oldurl = url;
	if (bundleName != null && (url == null || title == null)) {
	    bundle = ResourceBundle.getBundle(bundleName, locale);
	    if (url == null) {
		try {
		    url = new URL(bundle.getString(urlKey));
		} catch (Exception e) {
		    ErrorMessage.display(e);
		    return;
		}
	    }
	    if (title == null) {
		title = bundle.getString(titleKey);
	    }
	}
	// if (url == null && helpframe == null) return;
	if (helpframe == null) {
	    helpframe = new JFrame (title);
	    helpframe.addWindowListener(new WindowAdapter () {
		    public void windowClosing(WindowEvent e) {
			helpframe.setVisible(false);
		    }
		});
	    helpframe.setSize(xsize, ysize);
	}
	Container hpane = helpframe.getContentPane();
	if (url != null) {
	    try {
		boolean tocmode =  url.getPath().endsWith(".xml");
		if (helpPane == null) {
		    helpPane = (tocmode)?
			(JComponent)(new HtmlWithTocPane(url)):
			(JComponent)(new HtmlPane(url));
		    hpane.setLayout(new BorderLayout());
		    hpane.add(helpPane, "Center");
		    if (tocmode) {
			HtmlWithTocPane tocpane = (HtmlWithTocPane)helpPane;
			int width = tocpane.getTocPreferredSize().width;
		    
			// int percent = (width * 100) / xsize;
			tocpane.setDividerLocation(width+10);
			// tocpane.setResizeWeight((double)(percent/100.0));
			((HtmlWithTocPane)helpPane).
			    setSelectionWithAction(0);
		    }
		} else {
		    if (!url.equals(oldurl)) {
			setURL(url);
		    }
		}
		// by default, we want the VM's locale, not the one set
		// by some other means (which will typically be set to
		// make the frame resemble one for a set of URL's locales.
		// if you need another locale, it will have to be set
		// explicitly.
		helpframe.applyComponentOrientation(getComponentOrientation());
	    } catch (IOException e) {
		ErrorMessage.display(e);
		if (helpPane != null) hpane.remove(helpPane);
		helpPane = null;
		return;
	    } catch (org.xml.sax.SAXException ee) {
		ErrorMessage.display(ee);
		if (helpPane != null) hpane.remove(helpPane);
		helpPane = null;
		return;
	    } catch (javax.xml.parsers.ParserConfigurationException eee) {
		ErrorMessage.display(eee);
		if (helpPane != null) hpane.remove(helpPane);
		helpPane = null;
		return;
	    }
	} else {
	    if (helpPane != null) hpane.remove(helpPane);
	}
	helpframe.setVisible(true);
    }
    
    /**
     * Constructor.
     * @param menuItemName the name to use in the menu item created
     * @param url the URL for the help-frame's HTML or XML file
     * @param title the title of the help frame
     * @param xsize the horizontal size of the frame in points
     * @param ysize the vertical size of the frame in points
     * @throws IOException the URL could not be loaded
     */
    public HelpMenuItem(String menuItemName, String url,
			String title, int xsize, int ysize)
	throws IOException
    {
	super(menuItemName);
	this.menuItemName = menuItemName;
	this. title = title;
	this.url = new URL(url);
	this.xsize = xsize;
	this.ysize = ysize;
	bundleName = null;
	addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    showHelp();
		}
	    });
    }


    /**
     * Constructor using resource bundles.
     * A resource bundle is used to look up the menu-title, help-frame
     * URL, and help-frame title, from keys, with the names of the keys
     * provided as arguments.
     * @param bundleName the name of the resource bundle used to configure
     *        the help frame.
     * @param menuItemNameKey the bundle's key for the title to use in the menu 
     *                     item created
     * @param urlKey the bundle's key for the  URL
     * @param titleKey the bundle's key for the title of the help frame
     * @param xsize the horizontal size of the help frame in points
     * @param ysize the vertical size of the help frame in points
     * @throws IOException the URL could not be loaded
     */
    public HelpMenuItem(String bundleName, String menuItemNameKey, 
			String urlKey, String titleKey, int xsize, int ysize)
	throws IOException
    {
	this(JComponent.getDefaultLocale(), bundleName, menuItemNameKey,
	     urlKey, titleKey, xsize, ysize);
    }

    /**
     * Constructor given a locale and resource bundle.
     * A resource bundle is used to look up the menu-title, help-frame
     * URL, and help-frame title, from keys, with the names of the keys
     * provided as arguments.
     * @param locale the locale
     * @param bundleName the name of the resource bundle
     * @param menuItemNameKey the bundle's key for the title to use in the 
     *        menu item created
     * @param urlKey the bundle's key for the content's URL
     * @param titleKey the bundle's key for the title of the help frame
     * @param xsize the horizontal size of the help frame in points
     * @param ysize the vertical size of the help frame in points
     * @throws IOException the URL could not be loaded
     */
    public HelpMenuItem(Locale locale, 
			String bundleName, String menuItemNameKey, 
			String urlKey, String titleKey, int xsize, int ysize)
	throws IOException
    {
	super(ResourceBundle.getBundle(bundleName, locale).getString
	      (menuItemNameKey));
	this.bundleName = bundleName;
	this.menuItemNameKey = menuItemNameKey;
	this.titleKey = titleKey;
	this.xsize = xsize;
	this.ysize = ysize;
	this.urlKey = urlKey;
	addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    showHelp();
		}
	    });
    }
}
