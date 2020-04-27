package org.bzdev.swing.io;

import javax.swing.text.*;
import java.io.*;
import java.util.ResourceBundle;
import org.bzdev.util.SafeFormatter;

//@exbundle org.bzdev.swing.io.lpack.IO

/**
 * Reader whose source is an instance of java.swing.text.Document.
 * Several Java APIs (e.g., the ones for printing) can get their
 * inputs from a Reader, but not from a Document.  This class
 * provides a bridge between the two.  The reader constructed will
 * not provide any style information associated with a Document.
 * It does, however, make it easy to print the text of a document,
 * which is useful for debugging and for other purposes where a
 * plain-text printout will suffice.
 *
 * @author Bill Zaumen
 * @version $Revision: 1.3 $, $Date: 2005/05/25 05:59:14 $
 */
public class DocumentReader extends Reader {

    private static ResourceBundle exbundle = ResourceBundle.getBundle
	("org.bzdev.swing.io.lpack.IO");

    static String errorMsg(String key, Object... args) {
	return (new SafeFormatter()).format(exbundle.getString(key), args)
	    .toString();
    }

    Document doc;
    Segment seg = new Segment();
    int pos = 0;
    public int read(char array[], int offset, int len) throws IOException {
	if (doc == null) throw new EOFException(errorMsg("noDoc"));
	synchronized(lock) {
	    int doclen = doc.getLength();
	    if (pos > doclen) {throw new IOException(errorMsg("pastLen"));}
	    else if (pos == doclen) {
		return (-1);
	    } else {
		try {
		    int maxlen = doclen - pos;
		    int ourlen = maxlen < len? maxlen: len;
		    doc.getText(pos, ourlen, seg);
		    int i = offset;;
		    int n = offset + ourlen;
		    char ch = seg.first();
		    array[i] = seg.first();
		    while (++i < n) {
			array[i] = seg.next();
		    }
		    pos += seg.count;
		    return seg.count;
		} catch (BadLocationException e) {
		    e.printStackTrace();
		    throw new IOException(errorMsg("badLoc"));
		}
	    }
	}
    }
    public void close() throws IOException {
	synchronized(lock) {
	    doc = null; seg = null;
	}
    }

    public DocumentReader(Document doc) {
	super();
	this.doc = doc;
	seg.setPartialReturn(true);
    }
}
