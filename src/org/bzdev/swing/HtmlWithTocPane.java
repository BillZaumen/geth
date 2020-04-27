
package org.bzdev.swing;
import javax.swing.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.InputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.FactoryConfigurationError;
import org.xml.sax.SAXException;

import java.awt.*;
import java.awt.event.*;

import java.awt.ComponentOrientation;
import java.util.Locale;
import java.util.ResourceBundle;
import java.awt.Component;

//@exbundle org.bzdev.swing.lpack.Swing

/**
 * Class providing an HTML Pane combined with a side panel showing a
 * table of contents.  One may initialize the class or set the table
 * of contents by providing an XML document with the following DTD:
 * <blockquote><pre>
 * &lt;!ELEMENT toc (node) &gt;
 * &lt;!ELEMENT node (node)*&gt;
 * &lt;!ATTLIST node 
 *              title CDATA #IMPLIED
 *              uri   CDATA #IMPLIED
 *              href  CDATA #IMPLIED
 * &gt;
 * </pre></blockquote>
 * A DOCTYPE directive is not necessary - the DTD above will be
 * assumed. If a DOCTYPE directive is desired (e.g, for documentation
 * reasons), one should use "sreource:/org/bzdev/swing/toc.dtd".
 * For each <code>node</code> element, the <code>title</code>attribute
 * will be the title that appears in a table-of-contents panel and
 * the <code>uri</code> attribute provides the URI for the contents
 * for that title.  The contents should be an HTML document using HTML 3.2
 * the HTML version that Java supports (Java is slowly migrating to full
 * HTML 4.0 support but this class uses whatever the standard Java runtime
 * environment supports). The <code>href</code> attribute is ignored by
 * HtmlWithTocPane and is provided in case the XML file is also  used by
 * a web server, in which case the URL it needs to  use may be
 * different than the one provided by the <code>uri</code> attribute.
 *<P>
 * After the document is loaded (e.g., by using one of the
 * constructors for this class), one should call
 * <code>setSelectionWithAction(row)</code> to select the initial row
 * and the corresponding URL. This will almost always be row 0. If
 * <code>setSelectionWithAction</code> is not called, no initial
 * contents will appear in the HTML portion of the pane.
 * <p>
 * To initialize the class manually, use the same procedure as for
 * an instance of the class {@link org.bzdev.swing.UrlTocPane}.
 * A number of methods are borrowed from the JSplitPane class
 * (including some of the Javadoc comments) and simply call the
 * corresponding methods (but with 'left' and 'right' replaced by
 * the edges closest to the TOC pane and HTML pane respectively.
 *
 * @author Bill Zaumen
 */
public class HtmlWithTocPane extends JComponent implements UrlTocTree {
    private JSplitPane splitPane;
    private UrlTocPane tocPane;
    private JScrollPane tocScrollPane;
    private HtmlPane htmlPane;

    static String errorMsg(String key, Object... args) {
	return SwingErrorMsg.errorMsg(key, args);
    }


    /**
     *  Class Constructor.
     */
    public HtmlWithTocPane () {
	tocPane = new UrlTocPane();
	finishInit(false);
    }

    /**
     * Class Constructor specifying an input stream for initialization.
     * The input stream contains an XML document.
     * @param is The input stream
     * @throws FactoryConfigurationError the XML parser cannot be configured
     * @throws SAXException the XML data in the input stream in not well formed
     *         or is not valid.
     * @throws IOException an IO error was seen
     * @throws ParserConfigurationException the XML parser cannot be configured
     */
    public HtmlWithTocPane(InputStream is)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException
    {
	this(is, false, false);
    }


    /**
     * Class Constructor specifying an input stream for initialization and
     * a flag indicating if the nodes should be expanded or not.
     * The input stream contains an XML document.
     * @param is The input stream
     * @param expand True if the nodes should be expanded; false otherwise
     * @throws FactoryConfigurationError the XML parser cannot be configured
     * @throws SAXException the XML data in the input stream in not well formed
     *         or is not valid.
     * @throws IOException an IO error was seen
     * @throws ParserConfigurationException the XML parser cannot be configured
     */
    public HtmlWithTocPane(InputStream is, boolean expand)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException
    {
	this(is, expand, false);
    }

     /**
     * Class constructor specifying and input stream for initialization, a flag
     * indicating if the nodes should be expanded or not, and a flag 
     * indicating if a validating parser should be used.
     * The input stream contains an XML document.
     * @param is The input stream
     * @param expand True if the nodes should be expanded; false otherwise
     * @param validating True if the parser is validating; false otherwise
     * @throws FactoryConfigurationError the XML parser cannot be configured
     * @throws SAXException the XML data in the input stream in not well formed
     *         or is not valid.
     * @throws IOException an IO error was seen
     * @throws ParserConfigurationException the XML parser cannot be configured
     */
    public HtmlWithTocPane(InputStream is, boolean expand, boolean validating)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException
    {
	tocPane = new UrlTocPane();
	tocPane.setToc(is, expand, validating);
	finishInit(true);
    }

    /**
     * Class constructor specifying an URL for initialization.
     * The URL contains an XML document.
     * @param url The URL
     * @throws FactoryConfigurationError the XML parser cannot be configured
     * @throws SAXException the XML data in the URL in not well formed
     *         or is not valid.
     * @throws IOException an IO error was seen
     * @throws ParserConfigurationException the XML parser cannot be configured
     */
    public HtmlWithTocPane(URL url)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException
    {
	this(url, false, false);
    }


    /**
     * Class Constructor specifying an URL for initialization and
     * a flag indicating if the nodes should be expanded or not.
     * The URL contains an XML document.
     * @param url The URL
     * @param expand True if the nodes should be expanded; false otherwise
     * @throws FactoryConfigurationError the XML parser cannot be configured
     * @throws SAXException the XML data in the URL in not well formed
     *         or is not valid.
     * @throws IOException an IO error was seen
     * @throws ParserConfigurationException the XML parser cannot be configured
     */
    public HtmlWithTocPane(URL url, boolean expand)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException
    {
	this(url, expand, false);
    }

     /**
     * Class Constructor specifying and URL for initialization, a flag
     * indicating if the nodes should be expanded or not, and a flag 
     * indicating if a validating parser should be used.
     * The URL contains an XML document.
     * @param url The URL
     * @param expand True if the nodes should be expanded; false otherwise
     * @param validating True if the parser is validating; false otherwise
     * @throws FactoryConfigurationError the XML parser cannot be configured
     * @throws SAXException the XML data in the URL in not well formed
     *         or is not valid.
     * @throws IOException an IO error was seen
     * @throws ParserConfigurationException the XML parser cannot be configured
     */
    public HtmlWithTocPane(URL url, boolean expand, boolean validating)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException
    {
	this(url.openStream(), false, false);
    }


    /**
     * Class constructor specifying an URL represented as a String for
     * initialization.
     * The URL contains an XML document.
     * @param url The URL
     * @throws FactoryConfigurationError the XML parser cannot be configured
     * @throws SAXException the XML data in the URL in not well formed
     *         or is not valid.
     * @throws IOException an IO error was seen
     * @throws ParserConfigurationException the XML parser cannot be configured
     */
    public HtmlWithTocPane(String url)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException, MalformedURLException
    {
	this(url, false, false);
    }


    /**
     * Class constructor specifying an URL represented as a String for
     * initialization and a flag indicating if the nodes should be
     * expanded or not.
     * The URL contains an XML document.
     * @param url The URL
     * @param expand True if the nodes should be expanded; false otherwise
     * @throws FactoryConfigurationError the XML parser cannot be configured
     * @throws SAXException the XML data in the URL in not well formed
     *         or is not valid.
     * @throws IOException an IO error was seen
     * @throws ParserConfigurationException the XML parser cannot be configured
     */
    public HtmlWithTocPane(String url, boolean expand)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException, MalformedURLException
    {
	this(url, expand, false);
    }

     /**
     * Class Constructor specifying and URL represented as a String for
     * initialization, a flag indicating if the nodes should be
     * expanded or not, and a flag indicating if a validating parser
     * should be used.
     * The URL contains an XML document.
     * @param url The URL
     * @param expand True if the nodes should be expanded; false otherwise
     * @param validating True if the parser is validating; false otherwise
     * @throws FactoryConfigurationError the XML parser cannot be configured
     * @throws SAXException the XML data in the URL in not well formed
     *         or is not valid.
     * @throws IOException an IO error was seen
     * @throws ParserConfigurationException the XML parser cannot be configured
     */
    public HtmlWithTocPane(String url, boolean expand, boolean validating)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException, MalformedURLException
    {
	this((new URL(url)).openStream(), expand, validating);
    }

    // The JSplitPane methods seem to use left/right top/down explicitly,
    // but I can't seem to get it to work properly, so we'll just ignore
    // orientation for now (setting it for components, but not trying
    // to switch right and left explicitly.
    

    public void setComponentOrientation(ComponentOrientation o) {
	super.setComponentOrientation(o);

	splitPane.setComponentOrientation(o);
	tocScrollPane.setComponentOrientation(o);
	tocPane.setComponentOrientation(o);
	htmlPane.setComponentOrientation(o);
    }

    public void setLocale(Locale locale) {
	super.setLocale(locale);
	splitPane.setLocale(locale);
	tocScrollPane.setLocale(locale);
	tocPane.setLocale(locale);
	htmlPane.setLocale(locale);
    }

    /**
     * Set the local of the content pane.
     * This method allows the locale of the content panes (the table
     * of contents and its corresponding HTML pane) to be set without
     * changing the layout of navigation controls.
     *
     *@param locale the locale.
     */
    public void setContentLocale(Locale locale) {
	tocPane.setLocale(locale);
	htmlPane.setLocale(locale);
    }

    /**
     * Get the top inset.
     * @return the top inset for the split pane this component contains
     */
    public int getTopInset() {
	return splitPane.getInsets().top;
    }

    /**
     * Get the bottom inset.
     * @return the bottom inset for the split pane this component contains
     */
    public int getBottomInset() {
	return splitPane.getInsets().bottom;
    }

    /**
     * Get the left or right inset adjacent to the table of contents pane.
     * Whether the table of contents appears on the left or right depends
     * on localization.
     * @return the inset for the split pane this component contains
     */
    public int getTocInset() {
	return splitPane.getInsets().left;
    }

    /**
     * Get the left or right inset adjacent to HTML pane.
     * Whether the HTML pane appears on the left or right depends
     * on localization.
     * @return the inset for the split pane this component contains
     */
    public int getHtmlInset() {
	return splitPane.getInsets().right;
    }

    /**
     * Get the preferred size for the TOC pane.
     * @return the preferred size
     */
    public Dimension getTocPreferredSize() {
	return tocScrollPane.getPreferredSize();
    }

    /**
     * Get the preferred size for the HTML pane.
     * @return the preferred size
     */
    public Dimension getHtmlPanePreferredSize() {
	return htmlPane.getPreferredSize();
    }

    /**
     * Set the preferred size for the Table of Contents pane.
     * @param preferredSize the preferred size
     */
    public void setTocPreferredSize(Dimension preferredSize) {
	tocScrollPane.setPreferredSize(preferredSize);
    }

    /**
     * Set the preferred size for the HTML pane.
     * @param preferredSize the preferred size
     */
    public void setHtmlPanePreferredSize(Dimension preferredSize) {
	htmlPane.setPreferredSize(preferredSize);
    }

    /**
     * Rest the split pane to the preferred sizes.
     * This must be called after individual preferred sizes are set
     * for those to have any effect. The divider may move as a result
     * of using this method.
     */
    public void resetToPreferredSizes() {
	splitPane.resetToPreferredSizes();
    }

    /**
     * Sets the divider location as a percentage of the HtmlWithTocPane's size.
     * <p>
     * This method is implemented in terms of {@link #setDividerLocation(int)}.
     * This method immediately changes the size of the split pane
     * based on its current size. If the split pane is not correctly
     * realized and on screen, this method will have no effect (new
     * divider location will become (current size * proportionalLocation) 
     * which is 0).
     * @param proportionalLocation a double-precision floating point 
     * value that specifies a percentage, from zero (The TOC side of the
     * component) to 1.0 (the HTML Pane side of the component)
     */
    public void setDividerLocation(double proportionalLocation) {
	splitPane.setDividerLocation(proportionalLocation);
    }

    /**
     *  Returns the last value passed to setDividerLocation.  The
     * value returned from this method may differ from the actual
     * divider location (if setDividerLocation was passed a value
     * bigger than the current size).
     * @return an integer specifying the location of the divider
     */
    public int getDividerLocation() {
	return splitPane.getDividerLocation();
    }

    /**
     * Sets the location of the divider. 
     * This is passed off to the look and feel implementation, and
     * then listeners are notified. A value less than 0 implies the
     * divider should be reset to a value that attempts to honor the
     * preferred size of the TOC component.
     * @param location  an integer specifying the location of the divider
     * measured from right or left hand edge adjacent to the TOC pane.
     */
    public void setDividerLocation(int location) {
	splitPane.setDividerLocation(location);
    }


    /**
     * Specifies how to distribute extra space when the size of 
     * the split pane changes. 
     * A value of 0, the default, indicates the right/bottom component
     * gets all the extra space (the left/top component acts fixed),
     * where as a value of 1 specifies the left/top component gets all
     * the extra space (the right/bottom component acts
     * fixed). Specifically, the left/top component gets (weight *
     * diff) extra space and the right/bottom component gets (1 -
     * weight) * diff extra space.
     * @param weight as described above
     */
    public void  setResizeWeight(double weight) {
	splitPane.setResizeWeight(weight);
    }

    /**
     * Returns the number that determines how extra space is distributed.
     * @return how extra space is to be distributed on a resize of the 
     * split pane contained in this component.
     */
    public double getResizeWeight() {
	return splitPane.getResizeWeight();
    }

    private void finishInit(boolean hasPage) {
	tocScrollPane = new JScrollPane(tocPane);
	htmlPane = new HtmlPane();
	splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				   tocScrollPane, htmlPane);
	splitPane.setOneTouchExpandable(true);
	if (hasPage) {
	    int tocwidth = tocScrollPane.getPreferredSize().width;
	    splitPane.setDividerLocation(tocwidth + 5);
	}
	splitPane.setResizeWeight((0.0));
	tocPane.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    ObjTocPane.Entry entry = (ObjTocPane.Entry)
			e.getSource();
		    if (entry == null || entry.getValue() == null) return;
		    try {
			htmlPane.setPage((URL)entry.getValue());
			int tocwidth = tocScrollPane.getPreferredSize().width;
			splitPane.setDividerLocation(tocwidth+5);
		    } catch (java.io.IOException ee) {
			JOptionPane.
			    showMessageDialog(splitPane,
					      ee.getMessage(),
					      "Loading Error",
					      JOptionPane.ERROR_MESSAGE);
		    }
		}
	    });
	setLayout(new BorderLayout());
	add(splitPane, "Center");
    }


    public void addEntry(String title, Object obj) {
	tocPane.addEntry(title, obj);
    }

    public void addEntry (String title, String url) 
	throws MalformedURLException
    {
	tocPane.addEntry(title, url);
    }
    
    public void addEntry(String title, URL url)
	throws MalformedURLException  
    {
	tocPane.addEntry(title, url);
    }

    public void nextLevel() {tocPane.nextLevel();}

    public void prevLevel() {tocPane.prevLevel();}

    public void entriesCompleted() {tocPane.entriesCompleted();}

    public void entriesCompleted(boolean expand) {
	tocPane.entriesCompleted(expand);
    }

    public void setSelectionWithAction(int row) {
	tocPane.setSelectionWithAction(row);
    }

    public void clearSelection() {
	tocPane.clearSelection();
    }

    public void addActionListener(ActionListener l) {
	tocPane.addActionListener(l);
    }

    public void removeActionListener(ActionListener l) {
	tocPane.removeActionListener(l);
    }

    public void collapseRow(int row) {
	tocPane.collapseRow(row);
    }

    public void expandRow(int row) {
	tocPane.expandRow(row);
    }

    public boolean isCollapsed(int row) {
	return tocPane.isCollapsed(row);
    }

    public boolean isExpanded(int row) {
	return tocPane.isExpanded(row);
    }

    public void clearToc() {
	tocPane.clearToc();
    }

    public void setToc(URL url, boolean expand, boolean validating)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException, MalformedURLException
    {
	tocPane.setToc(url, expand, validating);
	setDividerLocation(tocPane.getPreferredSize().width);
    }

    public void setToc(String url, boolean expand, boolean validating)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException
    {
	tocPane.setToc(url, expand, validating);
	setDividerLocation(tocPane.getPreferredSize().width);
    }

    public void setToc(InputStream is, boolean expand, boolean validating)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException
    {
	tocPane.setToc(is, expand, validating);
	setDividerLocation(tocPane.getPreferredSize().width);
    }

    /**
     * Set the title to use in error message dialog boxes associated
     * with the HTML pane.
     * This title will be used as a title for various dialog boxes.
     * @param title the title to display.
     */
    public final void setHtmlErrorTitle (String title) {
	htmlPane.setErrorTitle(title);
    }

    /**
     * Get the title used in error message dialog boxes  associated
     * with the HTML pane.
     * @return the current title.
     */
    public final String getHtmlErrorTitle() {
	return htmlPane.getErrorTitle();
    }
}
