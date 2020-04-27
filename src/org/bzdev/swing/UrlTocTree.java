package org.bzdev.swing;
import java.net.URL;
import java.io.InputStream;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.FactoryConfigurationError;
import org.xml.sax.SAXException;
import java.net.MalformedURLException;


/**
 * Interface for Table of Contents whose values are URLs.
 * The methods allows the TOC to be specified via XML files
 * accessed via input streams or URLs.
 *
 * @author Bill Zaumen
 * @version $Revision: 1.3 $, $Date: 2005/05/25 05:59:14 $
 */
public interface UrlTocTree extends TocTree {

    /**
     * Add an entry.
     * This will define a new TOC entry.
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
	throws MalformedURLException;

    /**
     * Add an entry.
     * This will define a new TOC entry.
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
	throws MalformedURLException;


    /**
     * Set the table of contents from a URL.
     * This method should be used as as an alternative to
     * <code>addEntry</code>, <code>nextLevel</code>, <code>prevLevel</code>
     * and <code>entriesCompleted</code>
     * @param url the URL containing the table of contents.
     * @param expand True if the nodes should be expanded; false otherwise
     * @param validating True if the parser is validating; false otherwise
     * @throws FactoryConfigurationError the XML parser cannot be configured
     * @throws SAXException the XML data in the input stream in not well formed
     *         or is not valid.
     * @throws IOException an IO error was seen
     * @throws ParserConfigurationException the XML parser cannot be configured
     */
    public void setToc(URL url, boolean expand, boolean validating)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException, MalformedURLException;


    /**
     * Set the table of contents from a URL represented as a String.
     * This method should be used as as an alternative to
     * <code>addEntry</code>, <code>nextLevel</code>, <code>prevLevel</code>
     * and <code>entriesCompleted</code>
     * @param url the URL containing the table of contents.
     * @param expand True if the nodes should be expanded; false otherwise
     * @param validating True if the parser is validating; false otherwise
     * @throws FactoryConfigurationError the XML parser cannot be configured
     * @throws SAXException the XML data in the input stream in not well formed
     *         or is not valid.
     * @throws IOException an IO error was seen
     * @throws ParserConfigurationException the XML parser cannot be configured
     * @throws MalformedURLException the string representing the URL was
     *         illformed.
     */
    public void setToc(String url, boolean expand, boolean validating)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException;

    /**
     * Set the table of contents from a URL represented as a String.
     * This method should be used as as an alternative to
     * <code>addEntry</code>, <code>nextLevel</code>, <code>prevLevel</code>
     * and <code>entriesCompleted</code>
     * @param is the Inputstream containing the table of contents.
     * @param expand True if the nodes should be expanded; false otherwise
     * @param validating True if the parser is validating; false otherwise
     * @throws FactoryConfigurationError the XML parser cannot be configured
     * @throws SAXException the XML data in the input stream in not well formed
     *         or is not valid.
     * @throws IOException an IO error was seen
     * @throws ParserConfigurationException the XML parser cannot be configured
     * @throws MalformedURLException the string representing the URL was
     *         illformed.
     */
    public void setToc(InputStream is, boolean expand, boolean validating)
	throws FactoryConfigurationError, SAXException, IOException,
	       ParserConfigurationException;
}
