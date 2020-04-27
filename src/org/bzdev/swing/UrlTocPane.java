package org.bzdev.swing;

import org.bzdev.protocols.Handlers;


import java.awt.*;
// import java.awt.event.ActionListener;
// import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Stack;
import java.util.Iterator;
import java.util.LinkedList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.FactoryConfigurationError;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

import java.io.InputStream;
import java.io.IOException;

//@exbundle org.bzdev.swing.lpack.Swing

/**
 * UrlTocPane provides a table of contents that associates each entry
 * in a table of contents with a URL.  An instance of the class can
 * be initialized in the same way as an instance of {@link ObjTocPane}
 * and in addition can be initialized from an {@link InputStream} 
 * containing an XML file.  When an XML file is used, the following
 * DTD should be used:
 * <blockquote><pre>
 * &lt;!ELEMENT toc (node) &gt;
 * &lt;!ELEMENT node (node)*&gt;
 * &lt;!ATTLIST node title CDATA #IMPLIED
 *              uri CDATA #IMPLIED
 *              href CDATA #IMPLIED
 * &gt;
 * </pre></blockquote>
 * A DOCTYPE directive is not necessary.
 * For the attributes of each node, the <CODE>title</CODE> attribute
 * provides the title the GUI will display. The <code>uri</code>
 * attribute provides the URI that will be selected. The attribute
 * <CODE>href</CODE> is ignored. The <CODE>href</CODE> attribute is
 * provided for cases in which the XML file will used by a web server
 * and represents the URL that the web server will see after the
 * XML file is converted to HTML (e.g., by an XSTL style sheet).
 * This class ignores the <CODE>href</CODE> field.
 * One may wish to call the method
 * {@link ObjTocPane#setSelectionWithAction(int)} in order to set an
 * initial selection and to notify any previously configured
 * action listeners of the selection. The documentation for the class
 * {@link ObjTocPane} contains details regarding action events.
 * <P>
 * To configure a table of contents programatically, use the method
 * {@link UrlTocPane#addEntry(String,URL)} or
 * {@link UrlTocPane#addEntry(String,String)} and related methods
 * defined in the class {@link ObjTocPane}.  To replace the entries
 * with a new set, use one of the setToc methods or call
 * {@link UrlTocPane#clearToc()} and then programatically add the new
 * entries.  The method {@link ObjTocPane#setSelectionWithAction(int)}
 * calls {@link UrlTocPane#clearToc()} so an explicit call is not
 * necessary in this case.
 * @author Bill Zaumen
 * @see ObjTocPane
 */
public class UrlTocPane extends ObjTocPane implements UrlTocTree
{

    static String errorMsg(String key, Object... args) {
	return SwingErrorMsg.errorMsg(key, args);
    }

    /**
     * Add an entry.
     * This will define a new node in the tree.
     * 
     * @param title A string naming the entry.
     * @param url The URL to associate with the entry;
     * @throws IllegalStateException if this method was called after
     *         a call to <code>entriesCompleted</code>.
     * @see ObjTocPane#entriesCompleted()
     * @see ObjTocPane#nextLevel()
     * @see ObjTocPane#prevLevel()
     */
    public void addEntry(String title, String url) 
	throws MalformedURLException
    {
	super.addEntry(title, (Object)((url == null)? null: (new URL(url))));
    }

    /**
     * Add an entry.
     * This will define a new node in the tree.
     * 
     * @param title A string naming the entry.
     * @param url The URL to associate with the entry;
     * @throws IllegalStateException if this method was called after
     *         a call to <code>entriesCompleted</code>.
     * @see ObjTocPane#entriesCompleted()
     * @see ObjTocPane#nextLevel()
     * @see ObjTocPane#prevLevel()
     */
    public void addEntry(String title, URL url) 
	// throws MalformedURLException
    {
	super.addEntry(title, (Object)(url));
    }


    /**
     *  Class Constructor.
     */
    public UrlTocPane() {
	super();
    }

    /**
     * Class Constructor specifying an input stream for initialization.
     * The input stream contains an XML document.
     * @param url The URL for the page
     * @throws FactoryConfigurationError the XML parser cannot be configured
     * @throws SAXException the XML data in the input stream in not well formed
     *         or is not valid.
     * @throws IOException an IO error was seen
     * @throws ParserConfigurationException the XML parser cannot be configured
     */
    public UrlTocPane(URL url)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException
    {
	this();
	setToc(url, false, false);
    }
    /**
     * Class Constructor specifying an input stream for initialization and
     * a flag indicating if the nodes should be expanded or not.
     * The input stream contains an XML document.
     * @param url The URL for the page
     * @param expand True if the nodes should be expanded; false otherwise
     * @throws FactoryConfigurationError the XML parser cannot be configured
     * @throws SAXException the XML data in the input stream in not well formed
     *         or is not valid.
     * @throws IOException an IO error was seen
     * @throws ParserConfigurationException the XML parser cannot be configured
     */
    public UrlTocPane(URL url, boolean expand)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException
    {
	this();
	setToc(url, expand, false);
    }

    /**
     * Constructor for UrlTocPane.
     * The input stream contains an XML document.
     * @param url The URL for the page
     * @param expand True if the nodes should be expanded; false otherwise
     * @param validating True if the parser is validating; false otherwise
     * @throws FactoryConfigurationError the XML parser cannot be configured
     * @throws SAXException the XML data in the input stream in not well formed
     *         or is not valid.
     * @throws IOException an IO error was seen
     * @throws ParserConfigurationException the XML parser cannot be configured
     */
    public UrlTocPane(URL url, boolean expand, boolean validating)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException
    {
	this();
	setToc(url, expand, validating);
    }


    /**
     * Set the Table of Contents from a string providing a URL.
     * @param url The URL for the page
     * @param expand True if the nodes should be expanded; false otherwise
     * @param validating True if the parser is validating; false otherwise
     * @throws FactoryConfigurationError the XML parser cannot be configured
     * @throws SAXException the XML data in the input stream in not well formed
     *         or is not valid.
     * @throws IOException an IO error was seen
     * @throws ParserConfigurationException the XML parser cannot be configured
     * @throws MalformedURLException the URL argument has a syntax error
     */
    public void setToc (String url, boolean expand, boolean validating)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException, MalformedURLException
    {
	setToc (new URL(url), expand, validating);
    }

    /**
     * Set the Table of Contents from a URL.
     * @param url The URL for the page
     * @param expand True if the nodes should be expanded; false otherwise
     * @param validating True if the parser is validating; false otherwise
     * @throws FactoryConfigurationError the XML parser cannot be configured
     * @throws SAXException the XML data in the input stream in not well formed
     *         or is not valid.
     * @throws IOException an IO error was seen
     * @throws ParserConfigurationException the XML parser cannot be configured
     */
    public void setToc (URL url, boolean expand, boolean validating)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException
    {
	setToc(url.openStream(), expand, validating);
    }

    private static final int NEXTLEVEL = 0;
    private static final int PREVLEVEL = 1;
    private static final int ADDENTRY = 2;
    class ListEntry {
	int cmd;
	String title;
	URL url;
	ListEntry(int c, String t, URL u) {
	    cmd = c;
	    title = t;
	    url = u;
	}
    }
    private LinkedList<ListEntry> list = new LinkedList<ListEntry>();


    /**
     * Set the Table of Contents from an input stream.
     */
    public void setToc(InputStream is, boolean expand, boolean validating)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException
    {
	clearToc();
	list.clear();
	SAXParserFactory factory = SAXParserFactory.newInstance();
	factory.setNamespaceAware(false);
	factory.setValidating(validating);
	SAXParser parser = factory.newSAXParser();
	parser.parse(is, new DefaultHandler() {
		private int depth = 0;
		int stack[] = new int[256];
		public void startElement(String uri,
					 String localname,
					 String qname,
					 Attributes attributes)
		    throws SAXException
		{
		    if (qname.equals("toc")) {
			stack[depth] = 0;
			return;
		    }
		    String title = null;
		    String link = null;
		    for (int i = 0; i < attributes.getLength(); i++) {
			if (attributes.getQName(i).equals("title")) {
			    title = attributes.getValue(i);
			} else if (attributes.getQName(i).equals("uri")) {
			    link = attributes.getValue(i);
			} else if (!attributes.getQName(i).equals("href")) {
			    // we ignore an 'href' attribute; but any other
			    // attribute causes an exception.
			    throw new SAXException
				(errorMsg("unknownAttr",attributes.getQName(i)));
			}
		    }
		    if (title == null) title = "";
		    if (qname.equals("node")) {
			if (depth > 0 && stack[depth] == 0) {
			    // nextLevel();
			    list.add(new ListEntry(NEXTLEVEL, null, null));
			} else if (depth == 0 && stack[depth] > 0) {
			    throw new 
				SAXException(errorMsg("multipleTopLevelNodes"));
			}
			try {
			    URL url = 
				(link == null)? null: new URL(link);
			    // addEntry(title, url);
			    list.add(new ListEntry(ADDENTRY, title, url));
			} catch (MalformedURLException ue) {
			    String s = (link==null)? "<null>": link;
			    throw new SAXException
				(errorMsg("malformedURL", s));
			}
			stack[depth]++;
			stack[++depth] = 0;
		    } else {
			throw new SAXException
			    (errorMsg("unknownElement", qname));
		    }
		}

		public void endElement(String uri, String localname,
				       String qname)
		    throws SAXException
		{
		    if (qname.equals("toc")) {
			return;
		    } else if (qname.equals("node")) {
			if (stack[depth] != 0) {
			    // prevLevel();
			    list.add(new ListEntry(PREVLEVEL, null, null));
			}
			depth--;
			return;
		    } else {
			throw new SAXException
			    (errorMsg("unknownElement", qname));
		    }
		}
	    });
	Iterator it = list.iterator();
	while (it.hasNext()) {
	    ListEntry entry = (ListEntry) it.next();
	    switch (entry.cmd) {
	    case NEXTLEVEL:
		nextLevel();
		break;
	    case PREVLEVEL:
		prevLevel();
		break;
	    case ADDENTRY:
		addEntry(entry.title, entry.url);
		break;
	    }
	}
	list.clear();
	entriesCompleted(expand);
    }
}
