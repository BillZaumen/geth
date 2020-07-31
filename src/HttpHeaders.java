import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.TextListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.print.PrinterException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;

import java.lang.Exception;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.MessageFormat;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.prefs.*;
import java.util.Iterator;
import java.util.Collection;
import java.util.Locale;

import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.Document;
import javax.swing.text.AbstractDocument;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.util.ResourceBundle;

/*
import dev.wtz.swing.DocumentReader;
import dev.wtz.swing.SimpleJTextPane;
import dev.wtz.swing.PortTextField;
import dev.wtz.swing.WholeNumbTextField;
import dev.wtz.swing.VTextField;
import dev.wtz.swing.CharDocFilter;
import dev.wtz.swing.HtmlWithTocPane;
*/
import org.bzdev.swing.io.DocumentReader;
import org.bzdev.swing.SimpleJTextPane;
import org.bzdev.swing.PortTextField;
import org.bzdev.swing.WholeNumbTextField;
import org.bzdev.swing.VTextField;
import org.bzdev.swing.text.CharDocFilter;
import org.bzdev.swing.HtmlWithTocPane;
import org.bzdev.util.CopyUtilities;
import org.bzdev.util.SafeFormatter;


class HeaderEnumeration implements Enumeration {
    HttpURLConnection conn;
    int index = 0;
    boolean stop = false;
    String key;
    String value;
    URL context;
    static String separator = "-- Redirect to: ";
    int statusCode = -1;
    int redirectCount = 0;
    String redirectString;
    static String exception = "-- Exception";
    static String warning = "-- Warning";

    private boolean hasException = false;

    public void closeConnection() {
	try {
	    conn.disconnect();
	} catch (Exception e) {
	} finally {
	    stop = true;
	}
    }

    boolean terminatedByException() {return hasException;}

    void doNext() {
	if (key == exception) {
	    key = null;
	    hasException = true;
	    return;
	}
	index++;
	if (key == separator) {
	    redirectCount++;
	    try {
		boolean absolute = false;
		URL newurl;
		try {
		    newurl = new URL(redirectString);
		    absolute = true;
		} catch (Exception e) {
		    newurl = new URL(context, redirectString);
		}
		context = newurl;
		if (stop) return;
		conn = (HttpURLConnection)(context.openConnection());
		conn.setConnectTimeout(HttpHeaders.connTimeout);
		conn.setReadTimeout(HttpHeaders.readTimeout);
		setHeaders(conn, hdrs);
		conn.connect();
		if (absolute) {
		    index = 0;
		    key = null;
		} else {
		    key = warning;
		    value = redirectString +" is not an absolute URL";
		}
	    } catch (Exception e) {
		key = exception;
		value = "redirect failed (" + e.getMessage() +")";
	    }
	} else {
	    key = conn.getHeaderFieldKey(index);
	    value = conn.getHeaderField(index);
	    if (key == null && statusCode >= 300 && statusCode < 400 &&
		redirectCount < 5) {
		redirectString = conn.getHeaderField("Location");
		key = separator;
		if (redirectString == null) {
		    key = separator;
		    redirectString = "<nothing provided>";
		}
	    }
	}
    }

    TableModel hdrs = null;

    void setHeaders(HttpURLConnection connection, TableModel  headers) {
	if (headers != null) {
	    int i;
	    int n = headers.getRowCount();
	    for (i = 0; i < n; i++) {
		String key = (String)headers.getValueAt(i,0);
		String value = (String)headers.getValueAt(i,1);
		if (key != null) key = key.trim();
		if (value != null) value = value.trim();
		if (key != null && value != null &&
		    key.length() != 0 && value.length() != 0) {
		    connection.setRequestProperty(key, value);
		}
	    }
	}
    }

    public HeaderEnumeration(String urlstring,
			     String method,
			     String mimetype,
			     TableModel headers,
			     InputStream data)
	throws IOException
    {
	this(new URL(urlstring), method, mimetype, headers, data);
    }

    public HeaderEnumeration(URL url,
			     String method,
			     String mimetype,
			     TableModel headers,
			     InputStream data)
	throws IOException
    {
	this((HttpURLConnection)url.openConnection(),
	     method, mimetype, headers, data);
	context = url;
    }

    public HeaderEnumeration(HttpURLConnection connection,
			     String method,
			     String mimetype,
			     TableModel headers,
			     InputStream data)
	throws IOException
    {
	connection.setConnectTimeout(HttpHeaders.connTimeout);
	connection.setReadTimeout(HttpHeaders.readTimeout);
	connection.setRequestMethod(method);
	if ((method.equals("POST") || method.equals("PUT")) && data != null) {
	    connection.setDoOutput(true);
	    if (mimetype != null && mimetype.length() > 0) {
		connection.setRequestProperty("Content-Type", mimetype);
	    }
	}
	hdrs = headers;
	setHeaders(connection, headers);
	connection.connect();
	conn = connection;
	if ((method.equals("POST") || method.equals("PUT")) && data != null) {
	    OutputStream out = connection.getOutputStream();
	    byte[] buffer = new byte[4048];
	    int n;
	    while((n = data.read(buffer)) != -1) {
		out.write(buffer, 0, n);
	    }
	    out.close();
	}
    }
    public boolean hasMoreElements() {
	if (stop) return false;
	if (key == exception) {
	    key = null;
	    hasException = true;
	}
	return (key != null || index == 0 );
    }
    String contentType;

    private Charset getCharset() {
	if (contentType == null) return null;
	boolean valid = false;
	String mtype = contentType.toLowerCase();
	int ind = mtype.indexOf(';');
	if (ind == 0) return null;
	if (ind > 0) mtype = mtype.substring(0, ind);
	mtype = mtype.trim();

	if (mtype.startsWith("text/")) valid = true;
	else if (mtype.equals("application/x-javascript")) valid = true;
	else if (mtype.equals("application/json")) valid = true;
	else if (mtype.equals("application/x-sh") ||
		 mtype.equals("application/x-csh") ||
		 mtype.equals("application/x-perl") ||
		 mtype.equals("application/x-tcl")) valid = true;
	else if (mtype.equals("application/x-latex") ||
		 mtype.equals("application/x-tex") ||
		 mtype.equals("application/x-troff") ||
		 mtype.equals("application/x-troff-man") ||
		 mtype.equals("application/x-troff-me") ||
		 mtype.equals("application/x-troff-ms")) valid = true;
	else valid = false;

	if (valid) {
	    ind = contentType.indexOf(';');
	    String name = (ind < 0)? "US-ASCII":
		contentType.substring(ind);
	    try {
		return Charset.forName(name);
	    } catch (Exception e) {
		return Charset.forName("US-ASCII");
	    }
	} else {
	    return null;
	}
    }

    boolean returnKey = true;
    boolean delayDoNext = false;
    public Object nextElement() {
	if (delayDoNext) {
	    delayDoNext = false;
	    doNext();
	}
	if (index == 0) {
	    key = "Status";
	    if (returnKey) {
		returnKey = !returnKey;
		return key +": ";
	    } else {
		try {
		    statusCode = conn.getResponseCode();
		    String resp = conn.getResponseMessage();
		    if (resp == null) resp = "<no response message>";
		    value = statusCode + " " + conn.getResponseMessage();
		    contentType = conn.getContentType();
		} catch (IOException eio) {
		    statusCode = -1;
		    value = "<could not get response code or message>";
		}
	    }
	}
	if (key == null) throw new NoSuchElementException();
	String result;
	if (returnKey) {
	    result = ((key == separator)? separator: (key +": "));
	} else {
	    result = ((key == separator)? redirectString: value);
	}
	if (returnKey && key == warning) {
	    returnKey = !returnKey;
	} else if (key == warning) {
		index = 0;
		key = null;
		returnKey = !returnKey;
	} else {
	    returnKey = !returnKey;
	    if (returnKey) {
		if (key == separator) delayDoNext = true;
		else doNext();
	    }
	}
	return result;
    }
    InputStreamReader getInputStreamReader() {
	InputStream is;
	if (conn.getContentLength() <= 0) {
	    String transferEncoding =
		conn.getHeaderField("transfer-encoding");
	    transferEncoding = (transferEncoding == null)? null:
		transferEncoding.toLowerCase().trim();
	    if (transferEncoding == null ||
		transferEncoding.equals("chunked") ||
		transferEncoding.equals("identity") ||
		transferEncoding.equals("gzip") ||
		transferEncoding.equals("deflate")) {
	    } else {
		return null;
	    }
	}
	if (statusCode == -1) {
	    return(null);
	}
	try {
	    is = conn.getErrorStream();
	    if (is == null) {
		is =  conn.getInputStream();
	    }
	    if (is == null) {
		return null;
	    }
	} catch (IOException e) {
	    return null;
	}
	Charset cs = getCharset();
	if (cs == null) {
	    return null;
	} else {
	    return new InputStreamReader(is, cs);
	}
    }
}

class FormReader extends Reader {
    CharArrayReader car;
    FormReader(Reader r) throws IOException {
	BufferedReader reader = new BufferedReader(r);
	StringBuffer tmp = new StringBuffer();
	String line = reader.readLine();
	boolean more = false;
	try {
	    while (line != null && line.trim().length() == 0) {
		line = reader.readLine();
	    }
	    if (line == null) {
		car = new CharArrayReader(new char[0]);
	    }
	    String encoded = URLEncoder.encode(line.trim(), "UTF-8");
	    tmp.append(encoded);
	    tmp.append("=");
	    while ((line = reader.readLine()) != null) {
		if (line.charAt(0) == '\t') {
		    if (more) {
			line = "\n" + line.substring(1);
		    } else {
			line = line.substring(1);
			more = true;
		    }
		    encoded = URLEncoder.encode(line, "UTF-8");
		    tmp.append(encoded);
		} else {
		    more = false;
		    while (line != null && line.trim().length() == 0) {
			line = reader.readLine();
		    }
		    if (line == null) break;
		    tmp.append("&");
		    encoded = URLEncoder.encode(line.trim(), "UTF-8");
		    tmp.append(encoded);
		    tmp.append("=");
		}
	    }
	} catch (java.io.UnsupportedEncodingException e) {}
	String contents = tmp.toString();
	car = new CharArrayReader(contents.toCharArray());
    }
    public int read(char cbuf[], int offset, int len) throws IOException {
	return car.read(cbuf, offset, len);
    }
    public void close() throws IOException {
	car.close();
    }
}

interface OurRunnable extends Runnable {
    Thread getThread();
}

public class HttpHeaders extends JPanel implements ActionListener {

    static boolean sawp = false;
    static boolean sawP = false;



    private static ResourceBundle exbundle =
	ResourceBundle.getBundle("lpack.HttpHeaders");

    static String errorMsg(String key, Object... args) {
	return (new SafeFormatter()).format(exbundle.getString(key), args)
	    .toString();
    }

    static String localeString(String key, Object... args) {
	return (new SafeFormatter()).format(exbundle.getString(key), args)
	    .toString();
    }


   public static HeaderEnumeration getHeaders(String urlstring,
					       String method,
					       String mimetype,
					       TableModel headers,
					       InputStream data)
	throws Exception {
	return new HeaderEnumeration(urlstring, method,
				     mimetype, headers, data);
    }

    /*
     * Allow use as a stand-alone program
     * Properties handled by preferences:
     *   http.agent
     *   http.proxyHost
     *   http.proxyPort
     *   http.nonProxyHosts
     *   ftp.proxyHost
     *   ftp.proxyPort
     *   ftp.nonProxyHosts
     */

    JFrame frame = null;
    public HttpHeaders(JFrame frame) {
	super();
	createMenu();
	frame.setJMenuBar(menubar);
	this.frame = frame;
    }
    public HttpHeaders() {super();}
    static Properties startupProperties = System.getProperties();

    static public void main(String argv[]) {
	HttpURLConnection.setFollowRedirects(false);
	/*
	try {
	    org.bzdev.protocols.Handlers.enable();
	} catch (ClassNotFoundException e) {
	}
	*/
	org.bzdev.protocols.Handlers.enable();
	boolean useTheProxy = false;

	Locale locale = Locale.forLanguageTag(localeString("forLanguageTag"));
	javax.swing.JComponent.setDefaultLocale(locale);
	int index = 0;
	if (index < argv.length && argv[0].equals("-P")) {
	    // the -P flag is tested below as well. This is a
	    // special case: with no additioal arguments, the GUI
	    // should start with the preferences disabled.
	    if ((index + 1) < argv.length) try {
		    index++;
		    Properties props = new Properties();
		    props.load(new FileInputStream(argv[index]));
		    Enumeration keys = props.propertyNames();
		    while (keys.hasMoreElements()) {
			String key = (String)keys.nextElement();
			String value = props.getProperty(key);
			System.setProperty(key, value);
		    }
		    useTheProxy = shouldUseProxy(props);
		    sawP = true;
		    index++;
		} catch (Exception ex) {
		    System.err
			.println(errorMsg("loadProps", argv[index+1]));
		    System.exit (1);
		}
	}

	if (argv.length == index) {
	    javax.swing.SwingUtilities.invokeLater(new Runnable() {
		    public void run() {
			JFrame frame = new JFrame(localeString("frameTitle"));
			Container fpane = frame.getContentPane();
			frame.addWindowListener(new WindowAdapter () {
				public void windowClosing(WindowEvent e) {
				    System.exit(0);
				}
			    });
			int width =
			    Integer.parseInt(localeString("frameWidth"));
			int height =
			    Integer.parseInt(localeString("frameHeight"));
			Toolkit tk = frame.getToolkit();
			Dimension screensize = tk.getScreenSize();
			long sw = Math.round(screensize.getWidth());
			long sh = (int)Math.round(screensize.getHeight());
			sw = (800*sw)/1000;
			sh = (800*sh)/1000;
			if (width > sw) width = (int)sw;
			if (height > sh) height = (int)sh;
			frame.setSize(width,height);
			HttpHeaders hpanel = new HttpHeaders(frame);
			fpane.setLayout(new BorderLayout());
			fpane.add(hpanel, "Center");
			hpanel.init();
 			hpanel.setVisible(true);
			frame.setVisible(true);
			hpanel.start();
		    }
		});
	} else {
	    int  bvalue = 0;
	    String mtype = null;
	    String chset = null;
	    String method = "GET";
	    boolean urlEncode = false;
	    String dFileName = null;
	    String hFileName = null;
	    while (index < argv.length &&
		   argv[index].startsWith("-") &&
		   argv[index].length() > 1) {
		switch (argv[index].charAt(1)) {
		case 'm':	// method (GET, POST, PUT)
		    if ((index + 1) < argv.length) {
			index++;
			method = argv[index].trim().toUpperCase();
			int i;
			for (i = 0; i < rmethods.length; i++) {
			    if (rmethods[i].equals(method)) break;
			}
			if (i == rmethods.length) {
			    System.err.println
				(errorMsg("unknownMethod", method));
			    System.exit(1);
			}
		    }
		    break;
		case 'c':	// charset for reading from data file
		    if ((index + 1) < argv.length) {
			index++;
			chset = argv[index];
		    }
		    break;
		case 't':	// Mime Type
		    if ((index + 1) < argv.length) {
			index++;
			if (urlEncode == false) {
			    mtype = argv[index];
			}
		    }
		    break;
		case 'u':	// URL encode (implies mimetype)
		    urlEncode = true;
		    mtype = "application/x-www-form-urlencoded";
		    break;
		case 'i':	// input file for POST or PUT
		    if ((index + 1) < argv.length) {
			index++;
			dFileName = argv[index];
		    }
		    break;
		case 'h':	// extra headers
		    if ((index + 1) < argv.length) {
			index++;
			hFileName = argv[index];
		    } else if ((index + 1) == argv.length) {
			System.out.println
			    ("usage: geth [-u] [-p]"
			     +" [-m post|get|put|head] \\\n"
			     +"            [-c CHARSET]"
			     +" [-t MEDIATYPE]"
			     +" [-i INPUTFILE]"
			     +" [-h HEADERFILE] \\\n"
			     +"            [-P PROPERTYFILE]"
			     +" [-n NUMBCHARS|all]"
			     +" URL\n"
			     +"       geth -h\n"
			     +"       geth");
			System.exit(0);
		    }
		    break;
		case 'p':
		    if (sawP) {
			System.err.println(errorMsg("sawP"));
			System.exit(1);
		    }
		    getPreferences(false);
		    System.setProperties(setupEnv((Properties)
						  startupProperties.clone(),
						  true));
		    useTheProxy = true;
		    sawp = true;
		    break;
		case 'P':
		    if (sawp) {
			System.err.println(errorMsg("sawp"));
			System.exit(1);
		    }
		    if ((index + 1) < argv.length) try {
			index++;
			Properties props = new Properties();
			props.load(new FileInputStream(argv[index]));
			Enumeration keys = props.propertyNames();
			while (keys.hasMoreElements()) {
			    String key = (String)keys.nextElement();
			    String value = props.getProperty(key);
			    System.setProperty(key, value);
			}
			useTheProxy = shouldUseProxy(props);
			sawP = true;
		    } catch (Exception ex) {
			    System.err
				.println(errorMsg("loadProps", argv[index+1]));
			System.exit (1);
		    }
		    break;
		case 'n':
		    if ((index + 1) < argv.length) {
			try {
			    index++;
			    if (argv[index].equals("all")) {
				bvalue = Integer.MAX_VALUE;
			    } else {
				bvalue = Integer.parseInt(argv[index]);
				if (bvalue < 0) {
				    System.err.println(errorMsg("notNegVal"));
				    System.exit(1);
				}
			    }
			} catch (Exception ex) {
			    String n = ex.getClass().getName();
			    String msg = ex.getMessage();
			    System.err.println(errorMsg("intParse", n, msg));
			    System.exit(1);
			}
		    }
		    break;
		case 'C':
		    if ((index + 1) < argv.length) {
			try {
			    index++;
			    connTimeout = Integer.parseInt(argv[index]) * 1000;
			} catch (Exception e) {
			    String n = e.getClass().getName();
			    String msg = e.getMessage();
			    System.err.println(errorMsg("intParseC", n, msg));
			    System.exit(1);
			}
		    }
		    break;
		case 'R':
		    if ((index + 1) < argv.length) {
			try {
			    index++;
			    readTimeout = Integer.parseInt(argv[index]) * 1000;
			} catch (Exception e) {
			    String n = e.getClass().getName();
			    String msg = e.getMessage();
			    System.err.println(errorMsg("intParseR", n, msg));
			    System.exit(1);
			}
		    }
		    break;
		default:
		    System.err.println(errorMsg("unknownOption", argv[index]));
		    System.exit(1);
		}
		index++;
	    }
	    if (index == argv.length) {
		System.err.println(errorMsg("notURL"));
		System.exit(1);
	    }
	    if (!useTheProxy && !sawP) {
		    getPreferences(false);
		    System.setProperties(setupEnv((Properties)
						  startupProperties.clone(),
						  false));
	    }
	    if (hFileName != null) {
		try {
		    readHeadersFromFile(hFileName);
		} catch (Exception e) {
		    System.err.println(errorMsg("rdHdrFailed", e.getMessage()));
		    System.exit(1);
		}
	    }
	    if (dFileName != null) {
		try {
		    fullMimeType = mtype;
		    File f;
		    if ((new File(dFileName)).isAbsolute()) {
			f = new File(dFileName);
		    } else {
			f = new File(System.getProperty("user.dir"), dFileName);
		    }
		    String charsetName = (chset != null)?
			chset.trim().toUpperCase(): "";
		    if (isTextMimeType(fullMimeType)) {
			String cset = getCharsetName(fullMimeType);
			// if no charset provided, use system default.
			BufferedReader in =
			    new
			    BufferedReader((charsetName.length() > 0)?
					   (new
					    InputStreamReader(new
							      FileInputStream(f),
							      charsetName)):
					   (new
					    InputStreamReader
					    (new FileInputStream(f))));
			String line;
			boolean notfirst = false;
			StringBuffer inputbuf = new StringBuffer(1024);
			while ((line = in.readLine()) != null) {
			    if (notfirst) {
				inputbuf.append("\n");
			    } else {
				notfirst = true;
			    }
			    inputbuf.append(line);
			}
			if (urlEncode) {
			    BufferedReader reader =
				new BufferedReader
				(new FormReader
				 (new StringReader(inputbuf.toString())));
			    // no line breaks due to encoding, and we
			    // can use US_ASCII for the encoding as the UTF-8
			    // is itself URL Encoded.
			    String encodedData = reader.readLine();
			    dataInputStream =
				new ByteArrayInputStream
				(encodedData.getBytes("US-ASCII"));
			} else {
			    dataInputStream =
				new ByteArrayInputStream(inputbuf.toString().
							 getBytes(cset));
			}
		    } else {
			dataInputStream = new FileInputStream(f);
		    }
		} catch (Exception e) {
		    System.err.println(errorMsg("rdInFailed", e.getMessage()));
		    System.exit(1);
		}
	    }

	    try {
		HeaderEnumeration
		    lines = getHeaders(argv[index],
				       method, fullMimeType,
				       hdrmodel, dataInputStream);

		if (method.equals("HEAD")) bvalue = 0;
		while (lines.hasMoreElements()) {
		    String key = (String)lines.nextElement();
		    String value = (String)lines.nextElement();
		    if (bvalue > 0 &&
			key.startsWith(HeaderEnumeration.separator)) {
			System.out.println();
			InputStreamReader isr = lines.getInputStreamReader();
			if (isr != null) {
			    try {
				appendContentsToSystemOut(isr, bvalue);
				isr.close();
			    } catch (Exception e) {
				handleExceptionToSystemOut(e);
			    }
			}

		    }
		    System.out.println(key + value);
		}
		if (bvalue > 0 && !lines.terminatedByException()) {
		    System.out.println();
		    InputStreamReader isr = lines.getInputStreamReader();
		    if (isr != null) {
			appendContentsToSystemOut(isr, bvalue);
			System.out.println();
		    }
		    try {
			isr.close();
		    } catch (Exception e) {
			handleExceptionToSystemOut (e);
		    }
		}
		System.exit(0);
	    } catch (Exception e) {
		if (e instanceof RuntimeException) {
		    e.printStackTrace(System.err);
		} else {
		    String msg = e.getMessage();
		    System.err.println(localeString("geth") +
				       e.getClass().getName()
				       +((msg == null)?"":(" - " +msg)));

		}
		System.exit(-1);
	    }
	}
    }


    JPanel top = new JPanel();

    JTextField input = new VTextField("", 55) {
	    protected void onEnterKey() throws Exception {
		doAction();
	    }
	};
    JButton button = new JButton(localeString("button"));
    static final Color CONTENT_COLOR = new Color((int)0,(int)96,(int)0);
    static final Color EXCEPTION_COLOR = new Color((int)96,(int)0,(int)0);

    static final int BUF_SIZE = 8092;


    public class OurTextPane extends SimpleJTextPane {
	public OurTextPane() {
	    super();
	}

	void handleException(Throwable e) {
	    Color savedcolor = getTextForeground();
	    setTextForeground(EXCEPTION_COLOR);
	    if (e instanceof java.lang.reflect.UndeclaredThrowableException) {
		e = ((java.lang.reflect.UndeclaredThrowableException)e).
		     getCause();
	    }
	    String msg = e.getMessage();
	    String nm = e.getClass().getName();
	    String delim = (msg == null)? "": ": ";
	    appendString(errorMsg("appendExMsg",nm, delim, msg) + "\n");
	    /*
	    appendString(e.getClass().getName() +" while reading contents"
		   +((msg == null)? "\n": ": " +msg +"\n"));
	    */
	    if (e instanceof RuntimeException) {
		result.appendString(errorMsg("callFrm") + "\n");
		// result.appendString("called from:\n");
		int i;
		StackTraceElement ste[] = e.getStackTrace();
		for (i = 0; i < ste.length; i++) {
		    result.appendString("    " +ste[i].toString() +"\n");
		}
	    }
	    setTextForeground(savedcolor);
	}

	private void appendContents(InputStreamReader isr, int nbytes,
				    Thread ourThread) {
	    // int nbytes = bcount.getValue();
	    char buffer[] = new char[BUF_SIZE];
	    int n = (nbytes < BUF_SIZE)? nbytes: BUF_SIZE;
	    int off = 0;
	    int i;
	    int mark = -1;
	    int charcount = 0;
	    Document doc = getDocument();
	    Color savedfg = getTextForeground();
	    boolean savedItalic = isItalic();
	    try {
		String addition;
		if (thread == ourThread) {
		    setItalic(!savedItalic);
		    if (nbytes == Integer.MAX_VALUE) {
			appendString(localeString("appendContents1"));
			// appendString("read the full content (");
			mark = doc.getLength();
			appendString(" " + localeString("appendContents2"));
			// appendString(" characters)\n");
		    } else {
			appendString(localeString("appendContents3") + " ");
			// appendString("read ");
			mark = doc.getLength();
			appendString(" " + localeString("appendContents4"));
			// appendString(" characters from content\n");
		    }
		    setItalic(savedItalic);
		    setTextForeground(CONTENT_COLOR);
		}
		while (off != n &&
		       (i = isr.read(buffer, off, n - off)) != -1) {
		    off += i;
		    if (off == BUF_SIZE) {
			nbytes -= BUF_SIZE;
			n = (nbytes < BUF_SIZE)? nbytes: BUF_SIZE;
			addition = new String(buffer, 0, off);
			charcount += addition.length();
			if (thread == ourThread) {
			    appendString(addition);
			}
			off = 0;
		    }
		}
		addition = new String(buffer, 0, off);
		charcount += addition.length();
		if (thread == ourThread) {
		    appendString(addition); appendString("\n");
		    setTextForeground(savedfg);
		    setItalic(!savedItalic);
		    insertString(mark, "" +charcount);
		    setItalic(savedItalic);
		}
	    } catch (Exception e) {
		if (thread == ourThread) {
		    setItalic(savedItalic);
		    setTextForeground(savedfg);
		    handleException(e);
		}
	    } finally {
		if (thread == ourThread) {
		    setTextForeground(savedfg);
		}
	    }
	}


	void setText(HeaderEnumeration lines, int nbytes, Thread ourThread) {
	    // int nbytes = bcount.getValue();
	    setBold(false);
	    setItalic(false);
	    setTextForeground(Color.BLACK);
	    setText("");
	    while (lines.hasMoreElements()) {
		String key = (String) lines.nextElement();
		String value =  (String) lines.nextElement();
		if (nbytes > 0 &&
		    key.startsWith(HeaderEnumeration.separator)) {
		    if (thread == ourThread) appendString("\n");
		    InputStreamReader isr = lines.getInputStreamReader();
		    if (isr != null) {
			try {
			    appendContents(isr, nbytes, ourThread);
			    isr.close();
			} catch (Exception e) {
			    if (thread == ourThread)
				handleException(e);
			}
		    }
		}
		if (thread == ourThread) {
		    boolean savedbold = isBold();
		    setBold(!savedbold);
		    boolean keyflag =
			key.startsWith(HeaderEnumeration.exception)
			|| key.startsWith(HeaderEnumeration.warning) ;
		    Color savedcolor = getTextForeground();
		    if (keyflag) {
			setTextForeground(EXCEPTION_COLOR);
		    }
		    appendString(key);
		    setBold(savedbold);
		    appendString(value);
		    setTextForeground(savedcolor);
		    appendString("\n");
		}
	    }
	    if (nbytes > 0 && !lines.terminatedByException()) {
		if (thread == ourThread)
		    appendString("\n");
		InputStreamReader isr = lines.getInputStreamReader();
		if (isr != null) {
		    appendContents(isr, nbytes, ourThread);
		    if (thread == ourThread)
			appendString("\n");
		    try {
			isr.close();
		    } catch (Exception e) {
			if (thread == ourThread)
			    handleException (e);
		    }
		}
	    }
	}
    }

    static void handleExceptionToSystemOut(Throwable e) {
	if (e instanceof java.lang.reflect.UndeclaredThrowableException) {
	    e = ((java.lang.reflect.UndeclaredThrowableException)e).
		getCause();
	}
	String msg = e.getMessage();
	String nm = e.getClass().getName();
	String delim = (msg == null)? "": ": ";
	System.out.println(localeString("appendExMsg", nm, delim, msg));
	/*
	System.out.println(e.getClass().getName()
			   +" while reading contents"
			   +((msg == null)? "": ": " +msg));
	*/
	if (e instanceof RuntimeException) {
	    System.out.println(localeString("callFrm"));
	    // System.out.println("called from:");
	    int i;
	    StackTraceElement ste[] = e.getStackTrace();
	    for (i = 0; i < ste.length; i++) {
		System.out.println("    " +ste[i].toString());
	    }
	}
    }

    static private void appendContentsToSystemOut(InputStreamReader isr,
						  int nbytes) {
	char buffer[] = new char[BUF_SIZE];
	int n = (nbytes < BUF_SIZE)? nbytes: BUF_SIZE;
	int off = 0;
	int i;
	int mark;
	int charcount = 0;
	try {
	    String addition;
	    if (nbytes == Integer.MAX_VALUE) {
		System.out.println(localeString("rdFull"));
		// System.out.println("*** read the full content");
	    } else {
		System.out.println(localeString("rdAtMost", nbytes));
		/*
		System.out.println("*** read at most "
				   +nbytes +" characters");
		*/
	    }
	    while (off != n &&
		   (i = isr.read(buffer, off, n - off)) != -1) {
		off += i;
		if (off == BUF_SIZE) {
		    nbytes -= BUF_SIZE;
		    n = (nbytes < BUF_SIZE)? nbytes: BUF_SIZE;
		    addition = new String(buffer, 0, off);
		    charcount += addition.length();
		    System.out.print(addition);
		    off = 0;
		}
	    }
	    addition = new String(buffer, 0, off);
	    charcount += addition.length();
	    System.out.println(addition);
	} catch (Exception e) {
	    handleExceptionToSystemOut(e);
	}
    }



    OurTextPane result = new OurTextPane();
    JScrollPane scrollpane = new JScrollPane(result);
    boolean editPrefs = false;
    JPanel centerPanel = new JPanel();
    JPanel prefs = new JPanel();
    JPanel prefs1 = new JPanel();
    JScrollPane prefsScrollpane = new JScrollPane(prefs1);

    JPanel rdata = new JPanel();
    JPanel rdata1 = new JPanel();
    JPanel rdataButtonPanel = new JPanel();


    String cardname = "rdata";
    String savedCardname = null;
    JScrollPane currentPane = prefsScrollpane;

    JMenuBar menubar = new JMenuBar();
    JMenu fileMenu = new JMenu(localeString("fileMenu"));
    JMenu viewMenu = new JMenu(localeString("viewMenu"));
    JMenu goMenu = new JMenu(localeString("goMenu"));
    JMenu help = new JMenu(localeString("help"));

    CardLayout cardlayout = new CardLayout();

    private void addComponent(JPanel panel, Component comp,
			      GridBagLayout bag,
			      GridBagConstraints c) {
	bag.setConstraints(comp, c);
	panel.add(comp);
    }

    JMenuItem output = new JMenuItem(localeString("output"));
    JMenuItem preferences = new JMenuItem(localeString("preferences"));
    JMenuItem requestData = new JMenuItem(localeString("requestData"));
    JMenuItem save = new JMenuItem(localeString("save"));
    JMenuItem toStdout = new JMenuItem(localeString("toStdout"));
    JMenuItem print = new JMenuItem(localeString("print"));
    JMenuItem exportPrefs = new JMenuItem(localeString("exportPrefs"));
    JMenuItem quit = new JMenuItem(localeString("quit"));
    JMenuItem pageDown = new JMenuItem(localeString("pageDown"));
    JMenuItem pageUp = new JMenuItem(localeString("pageUp"));
    JMenuItem gotoTop = new JMenuItem(localeString("gotoTop"));
    JMenuItem gotoEnd = new JMenuItem(localeString("gotoEnd"));
    JMenuItem manual = new JMenuItem(localeString("manual"));
    JMenuItem printManMI = new JMenuItem(localeString("printManual"));

    /*
    JEditorPane helpPane = new JEditorPane();
    JScrollPane helpScrollPane = new JScrollPane(helpPane);
    */
    HtmlWithTocPane helpPane = new HtmlWithTocPane();
    JFrame helpframe;

    /*
    KeyAdapter helpKeyAdapter = new KeyAdapter() {
	    public void keyPressed(KeyEvent e) {
		int keycode = e.getKeyCode();
		int mod = e.getModifiers();
		if (mod != 0) return;
		switch (keycode) {
		case KeyEvent.VK_HOME:
		    helpPane.setCaretPosition(0);
		    try {
			helpPane.scrollRectToVisible(helpPane.modelToView(0));
		    } catch (Exception badloc) {}
		    break;
		case KeyEvent.VK_END:
		    int len = helpPane.getDocument().getLength();
		    helpPane.setCaretPosition(len);
		    try {
			helpPane.scrollRectToVisible(helpPane.
						     modelToView(len));
		    } catch (Exception badloc) {}

		    break;
		}
	    }
	};
    */

    private void printManual() {
	try {
	    URL url = ClassLoader.getSystemClassLoader()
		.getResource("manual.html");
	    if (url != null) {
		JEditorPane pane = new JEditorPane();
		pane.setPage(url);
		EditorKit ekit = pane.getEditorKit();
		if (ekit instanceof HTMLEditorKit) {
		    HTMLEditorKit hkit = (HTMLEditorKit)ekit;
		    StyleSheet stylesheet = hkit.getStyleSheet();
		    StyleSheet oursheet = new StyleSheet();
		    StringBuilder sb = new StringBuilder(512);
		    CopyUtilities.copyResource
			("manual.css", sb, Charset.forName("UTF-8"));
		    oursheet.addRule(sb.toString());
		    stylesheet.addStyleSheet(oursheet);
		}
		pane.print(null, new MessageFormat("- {0} -"));
	    }
	} catch  (PrinterException e) {
	} catch (IOException e) {
	}
    }

    private void showHelp () {
	if (helpframe == null) {
	    helpframe = new JFrame(localeString("helpframe"));
	    Container hpane = helpframe.getContentPane();
	    int width = Integer.parseInt(localeString("helpFrameWidth"));
	    int height = Integer.parseInt(localeString("helpFrameHeight"));
	    Toolkit tk = helpframe.getToolkit();
	    Dimension screensize = tk.getScreenSize();
	    long sw = Math.round(screensize.getWidth());
	    long sh = (int)Math.round(screensize.getHeight());
	    sw = (800*sw)/1000;
	    sh = (800*sh)/1000;
	    if (width > sw) width = (int)sw;
	    if (height > sh) height = (int)sh;
	    helpframe.setSize(width, height);
	    helpframe.addWindowListener(new WindowAdapter () {
		    public void windowClosing(WindowEvent e) {
			helpframe.setVisible(false);
		    }
		});
	    URL url =
		ClassLoader.getSystemClassLoader().getResource("manual.xml");
	    if (url != null) {
		try {
		    // helpPane.setEditable(true);
		    // helpPane.setPage(url);
		    // helpPane.setEditable(false);
		    helpPane.setToc(url, true, false);
		    helpPane.setSelectionWithAction(0);
		} catch (IOException e) {
		    helpframe = null;
		    return;
		}
		catch (org.xml.sax.SAXException ee) {
		    helpframe = null;
		    return;
		}
		catch (javax.xml.parsers.ParserConfigurationException  eee) {
		    helpframe = null;
		    return;
		}
	    } else {
		helpframe = null;
		return;
	    }

	    hpane.setLayout(new BorderLayout());
	    // hpane.add(helpScrollPane, "Center");
	    hpane.add(helpPane, "Center");
	    // helpPane.addKeyListener(helpKeyAdapter);
	    helpframe.setVisible(true);
	} else {
	    helpframe.setVisible(true);
	}
    }

    private void createMenu() {
	output.setAccelerator(KeyStroke.getKeyStroke
			      (KeyEvent.VK_2, InputEvent.CTRL_DOWN_MASK));
	requestData.
	    setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1,
						  InputEvent.CTRL_DOWN_MASK));
	preferences.
	    setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3,
						  InputEvent.CTRL_DOWN_MASK));

	pageDown.setAccelerator(KeyStroke.
				getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0));
	pageUp.setAccelerator(KeyStroke.
				getKeyStroke(KeyEvent.VK_PAGE_UP, 0));

	gotoTop.setAccelerator(KeyStroke.getKeyStroke
			       (KeyEvent.VK_PAGE_UP,
				InputEvent.CTRL_DOWN_MASK));
	gotoEnd.setAccelerator(KeyStroke.getKeyStroke
			       (KeyEvent.VK_PAGE_DOWN,
				InputEvent.CTRL_DOWN_MASK));
	quit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
						   InputEvent.CTRL_DOWN_MASK));

	fileMenu.add(save);
	fileMenu.add(toStdout);
	fileMenu.add(print);
	fileMenu.add(exportPrefs);
	fileMenu.add(quit);

	viewMenu.add(requestData);
	viewMenu.add(output);
	if (!sawP) viewMenu.add(preferences);
	goMenu.add(pageUp);
	goMenu.add(pageDown);
	goMenu.add(gotoTop);
	goMenu.add(gotoEnd);
	menubar.add(fileMenu);
	menubar.add(viewMenu);
	menubar.add(goMenu);

	help.add(manual);
	help.add(printManMI);
	menubar.add(help);


	save.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    JFileChooser chooser = new JFileChooser();
		    File cwd = new File(System.getProperty("user.dir"));
		    chooser.setCurrentDirectory(cwd);
		    switch (chooser.showSaveDialog(frame)) {
		    case JFileChooser.CANCEL_OPTION:
			break;
		    case JFileChooser.APPROVE_OPTION:
			File f = chooser.getSelectedFile();
			try {
			    FileWriter fw = new FileWriter(f);
			    result.write(fw);
			    fw.close();
			} catch (Exception ee) {
			    String msg = ee.getMessage();
			    /*
			    if (msg != null) {
				msg = ": " +msg;
			    } else {
				msg = "";
			    }
			    */
			    if (msg == null) msg = "[ No Message ]";
			    JOptionPane.showMessageDialog
				(frame, localeString("writeFile", msg),
				 localeString("fileErr"),
				 JOptionPane.ERROR_MESSAGE);
			}
			break;
		    case JFileChooser.ERROR_OPTION:
			break;
		    }
		}
	    });

	exportPrefs.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    JFileChooser chooser = new JFileChooser();
		    File cwd = new File(System.getProperty("user.dir"));
		    chooser.setCurrentDirectory(cwd);
		    switch (chooser.showSaveDialog(frame)) {
		    case JFileChooser.CANCEL_OPTION:
			break;
		    case JFileChooser.APPROVE_OPTION:
			File f = chooser.getSelectedFile();
			try {
			    // setPreferences();
			    OutputStream os = new FileOutputStream(f);
			    setupEnv(new Properties(),
				     true).store(os,
						 "geth property settings");
			    os.close();
			} catch (Exception ee) {
			    String msg = ee.getMessage();
			    /*
			    if (msg != null) {
				msg = ": " +msg;
			    } else {
				msg = "";
			    }
			    */
			    if (msg == null) msg = "[ No Message ]";
			    JOptionPane.showMessageDialog
				(frame, localeString("writeFile", msg),
				 localeString("fileErr"),
				 JOptionPane.ERROR_MESSAGE);

			}
			break;
		    case JFileChooser.ERROR_OPTION:
			break;
		    }
		}
	    });

	quit.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    System.exit(0);
		}
	    });


	output.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    cardname = "main";
		    cardlayout.show(centerPanel, "main");
		    currentPane = scrollpane;
		}
	    });

	requestData.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    cardname = "rdata";
		    cardlayout.show(centerPanel, "rdata");
		    currentPane = prefsScrollpane;
		}
	    });

	preferences.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    cardname = "prefs";
		    cardlayout.show(centerPanel, "prefs");
		    currentPane = prefsScrollpane;
		}
	    });

	toStdout.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    try {
			result.write(new PrintWriter(System.out));
		    } catch (IOException ee){
			    String msg = ee.getMessage();
			    /*
			    if (msg != null) {
				msg = ": " +msg;
			    } else {
				msg = "";
			    }
			    */
			    if (msg == null) msg = "[ No Message ]";
			    JOptionPane.showMessageDialog
				(frame, localeString("writeOut", msg),
				 localeString("outErr"),
				 JOptionPane.ERROR_MESSAGE);
		    }
		}
	    });

	print.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    try {
			BufferedReader reader =
			    new
			    BufferedReader(new
					   DocumentReader(result.
							  getDocument()));
			DocFlavor flavor = DocFlavor.READER.TEXT_PLAIN;
			Doc myDoc = new SimpleDoc(reader, flavor, null);
			PrintService service =
			    PrintServiceLookup.lookupDefaultPrintService();
			PrintRequestAttributeSet aset = new
			    HashPrintRequestAttributeSet();
			aset.add(new Copies(1));
			aset.add(Sides.ONE_SIDED);
			DocPrintJob job = service.createPrintJob();
			job.print(myDoc, aset);
		    } catch (Exception ee) {
			    String msg = ee.getMessage();
			    /*
			    if (msg != null) {
				msg = ": " +msg;
			    } else {
				msg = "";
			    }
			    */
			    if (msg == null) msg = "[ No Message ]";
			    JOptionPane.showMessageDialog
				(frame, localeString("noprint", msg),
				 localeString("printErr"),
				 JOptionPane.ERROR_MESSAGE);
		    }
		}
	    });

	pageUp.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    pageUpHandler();
		}
	    });
	pageDown.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    pageDownHandler();
		}
	    });
	gotoTop.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    toStartHandler();
		}
	    });
	gotoEnd.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    toEndHandler();
		}
	    });

	manual.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    showHelp();
		}
	    });
	printManMI.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    printManual();
		}
	    });

    }

    static String rmethods[] = {"GET", "PUT", "POST", "HEAD"};
    static String mtypes[] = {
	"application/x-www-form-urlencoded",
	"text/plain", "text/html", "text/xml",
	"application/x-sh", "application-csh", "application/x-perl",
	"application/x-tcl", "application/x-tex",
	"application/x-troff", "application/x-troff-man",
	"application/x-troff-me", "application/x-troff-ms",
	"multipart/form-data", "application/json"
    };
    static boolean isTextMimeType(String mtype) {
	int i;
	int ind = mtype.indexOf(";");
	if (ind < 0) ind = mtype.length();
	String str = mtype.substring(0, ind).trim().toLowerCase();
	if (mtype.equals("multipart/form-data")) return false;
	for (i = 0; i < mtypes.length; i++) {
	    if (mtypes[i].equals(str)) {
		return true;
	    }
	}
	if (mtype.equals("text/")) return true;
	return false;
    }
    static String chooseMimeType(Component parent) {
	return (String)JOptionPane.
	    showInputDialog(parent,
			    localeString("mimetype"),
			    localeString("mtTitle"),
			    JOptionPane.PLAIN_MESSAGE,
			    (Icon)null,
			    mtypes, mtypes[0]);
    }

    static String chsets[] = null;
    static String chsetFilters[] = {"Big5", "EUC", "euc", "ISO", "US", "UTF"};
    static boolean filterCharset(Charset chset) {
	int i;
	if (chset.isRegistered()) {
	    String name = chset.displayName();
	    for (i = 0;  i < chsetFilters.length; i++) {
		if (name.startsWith(chsetFilters[i])) return true;
	    }
	}
	return false;
    }
    static void  initChsets() {
	if (chsets != null) return;
	Collection c = Charset.availableCharsets().values();
	Iterator it = c.iterator();
	int n = c.size();
	while (it.hasNext()) {
	    Charset chset = (Charset)it.next();
	    if(!filterCharset(chset)) n--;
	}
	chsets = new String[n+1];
	int i = 0;
	chsets[i++] = localeString("none");
	it = c.iterator();
	while(it.hasNext()) {
	    Charset chset = (Charset)it.next();
	    if (filterCharset(chset)) {
		chsets[i++] = chset.displayName();
	    }
	}
    }

    static String chooseCharset(Component parent) {
	return (String)JOptionPane.
	    showInputDialog(parent,
			    localeString("charset"),
			    localeString("mtTitle"),
			    JOptionPane.PLAIN_MESSAGE,
			    (Icon)null,
			    chsets, chsets[0]);
    }

    static JLabel connTimeoutLabel;
    static JLabel readTimeoutLabel;
    static int connTimeout = 0;
    static int readTimeout = 0;
    static WholeNumbTextField connTimeoutTF;
    static WholeNumbTextField readTimeoutTF;

    static JLabel methodLabel;
    static JComboBox<String> requestMethod;
    static JCheckBox urlEncodeRequestData;
    static JLabel inputChsetLabel;
    static JButton inputChsetButton;
    static JTextField inputChset;
    static JLabel mtypeLabel;
    static JButton requestMimeTypeButton;
    static JTextField requestMimeType;
    static JLabel rdLabel;
    static JButton  requestDataFileButton;
    static JTextField requestDataFileName;
    static JLabel rhLabel;
    static JButton requestHeaderFileButton;
    static JTextField requestHeaderFileName;
    static boolean loaded = false;
    static boolean dataLoaded = false;
    static boolean hdrsLoaded = false;
    static JButton  requestLoad;
    static JButton  requestClear;
    static JButton hdrAddRow;
    static JButton hdrInsRow;
    static JButton hdrDelRow;
    static final int N_HDR_ROWS = 10;

    static String hdrnames[] = {localeString("Header"), localeString("Value")};
    static DefaultTableModel hdrmodel = new
	DefaultTableModel(hdrnames, N_HDR_ROWS);

    static JTable hdrTable = new JTable(hdrmodel);

    static void clearHdrTable() {
	int i, j;
	hdrmodel.setRowCount(N_HDR_ROWS);
	if (hdrTable.isEditing())
	    ((DefaultCellEditor)hdrTable.getCellEditor()).
		cancelCellEditing();
	for (i = 0; i < N_HDR_ROWS; i++) {
	    for (j = 0; j < 2; j++) {
		hdrmodel.setValueAt(null, i, j);
	    }
	}
    }

    static SimpleJTextPane request = new SimpleJTextPane();
    static JScrollPane hdrScrollPane = new
	JScrollPane(hdrTable);
    static JPanel inputPanel = new JPanel(new BorderLayout());

    static JScrollPane reqScrollPane = new
	JScrollPane(request);

    static {
	inputPanel.add(new JLabel(localeString("inputData")),
		       BorderLayout.NORTH);
	inputPanel.add(reqScrollPane, BorderLayout.CENTER);
    }

    static JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
						 hdrScrollPane, inputPanel);


    static final String prefname = "org/bzdev/geth";
    static Preferences userPrefs = Preferences.userRoot().node(prefname);

    static JCheckBox useHttpProxy;
    static String useHttpProxyPref = "useHttpProxy";
    static boolean useHttpProxyFlag;

    static JCheckBox useHttpsProxy;
    static String useHttpsProxyPref = "useHttpsProxy";
    static boolean useHttpsProxyFlag;

    static JCheckBox useFtpProxy;
    static String useFtpProxyPref = "useFtpProxy";
    static boolean useFtpProxyFlag;

    static JCheckBox useSocksProxy;
    static String useSocksProxyPref = "useSocksProxy";
    static boolean useSocksProxyFlag;

    static JCheckBox useUserAgent;
    static String useUserAgentPref = "useAgent";
    static boolean useUserAgentFlag;

    static JCheckBox useProxyDefault;
    static String useProxyDefaultPref = "useProxyDefault";
    static boolean useProxyDefaultFlag;

    static String emptyString = "";

    static JTextField httpProxyHostField;
    static String httpProxyHostPref = "http.proxyHost";
    static String httpProxyHost;

    static PortTextField httpProxyPortField;
    static String httpProxyPortPref = "http.proxyPort";
    static String httpProxyPort;

    static JTextField httpNonProxyHostsField;
    static String httpNonProxyHostsPref = "http.nonProxyHosts";
    static String httpNonProxyHosts;


    static JTextField httpsProxyHostField;
    static String httpsProxyHostPref = "https.proxyHost";
    static String httpsProxyHost;


    static PortTextField httpsProxyPortField;
    static String httpsProxyPortPref = "https.proxyPort";
    static String httpsProxyPort;

    /*
    static JTextField httpsNonProxyHostsField;
    static String httpsNonProxyHostsPref = "https.nonProxyHosts";
    static String httpsNonProxyHosts;
    */

    static JTextField ftpProxyHostField;
    static String ftpProxyHostPref = "ftp.proxyHost";
    static String ftpProxyHost;


    static PortTextField ftpProxyPortField;
    static String ftpProxyPortPref = "ftp.proxyPort";
    static String ftpProxyPort;


    static JTextField ftpNonProxyHostsField;
    static String ftpNonProxyHostsPref = "ftp.nonProxyHosts";
    static String ftpNonProxyHosts;


    static JTextField socksProxyHostField;
    static String socksProxyHostPref = "socksProxyHost";
    static String socksProxyHost;


    static PortTextField socksProxyPortField;
    static String socksProxyPortPref = "socksProxyPort";
    static String socksProxyPort;


    static JTextField userAgentField;
    static String userAgentPref = "http.agent";
    static String userAgent;

    //  preferences that correspond to Java properties
    static final String[] propPrefs = {
	httpProxyHostPref, httpProxyPortPref, httpNonProxyHostsPref,
	httpsProxyHostPref, httpsProxyPortPref,
	ftpProxyHostPref, ftpProxyPortPref, ftpNonProxyHostsPref,
	socksProxyHostPref, socksProxyPortPref,
	/*userAgentPref*/
    };

    static boolean shouldUseProxy(Properties props) {
	for (String key: propPrefs) {
	    if (props.containsKey(key)) {
		return true;
	    }
	}
	return false;
    }

    static private void setPreferences() {
	useHttpProxyFlag = useHttpProxy.isSelected();
	userPrefs.putBoolean(useHttpProxyPref, useHttpProxyFlag);

	useHttpsProxyFlag = useHttpsProxy.isSelected();
	userPrefs.putBoolean(useHttpsProxyPref, useHttpsProxyFlag);

	useFtpProxyFlag = useFtpProxy.isSelected();
	userPrefs.putBoolean(useFtpProxyPref, useFtpProxyFlag);

	useSocksProxyFlag = useSocksProxy.isSelected();
	userPrefs.putBoolean(useSocksProxyPref, useSocksProxyFlag);

	useUserAgentFlag = useUserAgent.isSelected();
	userPrefs.putBoolean(useUserAgentPref, useUserAgentFlag);

	useProxyDefaultFlag = useProxyDefault.isSelected();
	userPrefs.putBoolean(useProxyDefaultPref, useProxyDefaultFlag);

	httpProxyHost = httpProxyHostField.getText();
	userPrefs.put(httpProxyHostPref, httpProxyHost);

	httpProxyPort = httpProxyPortField.getText();
	userPrefs.put(httpProxyPortPref, httpProxyPort);

	httpNonProxyHosts = httpNonProxyHostsField.getText();
	userPrefs.put(httpNonProxyHostsPref, httpNonProxyHosts);

	httpsProxyHost = httpsProxyHostField.getText();
	userPrefs.put(httpsProxyHostPref, httpsProxyHost);

	httpsProxyPort = httpsProxyPortField.getText();
	userPrefs.put(httpsProxyPortPref, httpsProxyPort);

	/*
	httpsNonProxyHosts = httpsNonProxyHostsField.getText();
	userPrefs.put(httpsNonProxyHostsPref, httpsNonProxyHosts);
	*/

	ftpProxyHost = ftpProxyHostField.getText();
	userPrefs.put(ftpProxyHostPref, ftpProxyHost);

	ftpProxyPort = ftpProxyPortField.getText();
	userPrefs.put(ftpProxyPortPref, ftpProxyPort);

	ftpNonProxyHosts = ftpNonProxyHostsField.getText();
	userPrefs.put(ftpNonProxyHostsPref, ftpNonProxyHosts);

	socksProxyHost = socksProxyHostField.getText();
	userPrefs.put(socksProxyHostPref, socksProxyHost);

	socksProxyPort = socksProxyPortField.getText();
	userPrefs.put(socksProxyPortPref, socksProxyPort);

	userAgent = userAgentField.getText();
	userPrefs.put(userAgentPref, userAgent);
    }

    static private void getPreferences(boolean usingGui) {
	useHttpProxyFlag = userPrefs.getBoolean(useHttpProxyPref, false);
	useHttpsProxyFlag = userPrefs.getBoolean(useHttpsProxyPref, false);
	useFtpProxyFlag = userPrefs.getBoolean(useFtpProxyPref, false);
	useSocksProxyFlag = userPrefs.getBoolean(useSocksProxyPref, false);
	useUserAgentFlag = userPrefs.getBoolean(useUserAgentPref, false);
	useProxyDefaultFlag = userPrefs.getBoolean(useProxyDefaultPref, false);

	httpProxyHost = userPrefs.get(httpProxyHostPref, emptyString);
	httpProxyPort = userPrefs.get(httpProxyPortPref, emptyString);
	httpNonProxyHosts = userPrefs.get(httpNonProxyHostsPref, emptyString);

	httpsProxyHost = userPrefs.get(httpsProxyHostPref, emptyString);
	httpsProxyPort = userPrefs.get(httpsProxyPortPref, emptyString);
	/*
	httpsNonProxyHosts =
	    userPrefs.get(httpsNonProxyHostsPref, emptyString);
	*/

	ftpProxyHost = userPrefs.get(ftpProxyHostPref, emptyString);
	ftpProxyPort = userPrefs.get(ftpProxyPortPref, emptyString);
	ftpNonProxyHosts = userPrefs.get(ftpNonProxyHostsPref, emptyString);

	socksProxyHost = userPrefs.get(socksProxyHostPref, emptyString);
	socksProxyPort = userPrefs.get(socksProxyPortPref, emptyString);

	userAgent = userPrefs.get(userAgentPref, emptyString);

	if (usingGui) {
	    useHttpProxy.setSelected(useHttpProxyFlag);
	    useHttpsProxy.setSelected(useHttpsProxyFlag);
	    useFtpProxy.setSelected(useFtpProxyFlag);
	    useSocksProxy.setSelected(useSocksProxyFlag);
	    useUserAgent.setSelected(useUserAgentFlag);
	    useProxyDefault.setSelected(useProxyDefaultFlag);

	    httpProxyHostField.setText(httpProxyHost);
	    httpProxyPortField.setText(httpProxyPort);
	    httpNonProxyHostsField.setText(httpNonProxyHosts);

	    httpsProxyHostField.setText(httpsProxyHost);
	    httpsProxyPortField.setText(httpsProxyPort);
	    // httpsNonProxyHostsField.setText(httpsNonProxyHosts);

	    ftpProxyHostField.setText(ftpProxyHost);
	    ftpProxyPortField.setText(ftpProxyPort);
	    ftpNonProxyHostsField.setText(ftpNonProxyHosts);

	    socksProxyHostField.setText(socksProxyHost);
	    socksProxyPortField.setText(socksProxyPort);

	    userAgentField.setText(userAgent);
	}
    }


    Properties pruneProperties(Properties props) {
	Enumeration names = startupProperties.propertyNames();
	while (names.hasMoreElements()) {
	    String name = (String)names.nextElement();
	    props.remove(name);
	}
	return props;
    }

    static void ourSetProperty(Properties props, String key, String value) {
	if (value == null || value.equals(emptyString)) {
	    props.remove(key);
	} else {
	    props.setProperty(key, value);
	}
    }

    static Properties setupEnv(Properties props, boolean useProxy) {
	if (useProxy && useHttpProxyFlag) {
	    ourSetProperty(props, httpProxyHostPref, httpProxyHost);
	    ourSetProperty(props, httpProxyPortPref, httpProxyPort);
	    ourSetProperty(props, httpNonProxyHostsPref, httpNonProxyHosts);
	} else {
	    ourSetProperty(props, httpProxyHostPref, null);
	    ourSetProperty(props, httpProxyPortPref, null);
	    ourSetProperty(props, httpNonProxyHostsPref, null);
	}

	if (useProxy && useHttpsProxyFlag) {
	    ourSetProperty(props, httpsProxyHostPref, httpsProxyHost);
	    ourSetProperty(props, httpsProxyPortPref, httpsProxyPort);
	    ourSetProperty(props, httpNonProxyHostsPref, httpNonProxyHosts);
	} else {
	    ourSetProperty(props, httpsProxyHostPref, null);
	    ourSetProperty(props, httpsProxyPortPref, null);
	    if (!useHttpProxyFlag) {
		ourSetProperty(props, httpNonProxyHostsPref, null);
	    }
	}

	if (useProxy && useFtpProxyFlag) {
	    ourSetProperty(props, ftpProxyHostPref, ftpProxyHost);
	    ourSetProperty(props, ftpProxyPortPref, ftpProxyPort);
	    ourSetProperty(props, ftpNonProxyHostsPref, ftpNonProxyHosts);
	} else {
	    ourSetProperty(props, ftpProxyHostPref, null);
	    ourSetProperty(props, ftpProxyPortPref, null);
	    ourSetProperty(props, ftpNonProxyHostsPref, null);
	}

	if (useProxy && useSocksProxyFlag) {
	    ourSetProperty(props, socksProxyHostPref, socksProxyHost);
	    ourSetProperty(props, socksProxyPortPref, socksProxyPort);
	} else {
	    ourSetProperty(props, socksProxyHostPref, null);
	    ourSetProperty(props, socksProxyPortPref, null);
	}

	if (useUserAgentFlag) {
	    ourSetProperty(props, userAgentPref, userAgent);
	} else {
	    ourSetProperty(props, userAgentPref, null);
	}
	return props;
    }

    GridBagConstraints c0 = new GridBagConstraints();
    GridBagConstraints c0a = new GridBagConstraints();
    GridBagConstraints c1 = new GridBagConstraints();
    GridBagConstraints c2 = new GridBagConstraints();
    GridBagConstraints c2c = new GridBagConstraints();
    GridBagConstraints c3 = new GridBagConstraints();
    GridBagConstraints c3a = new GridBagConstraints();
    GridBagConstraints c4 = new GridBagConstraints();

    private void initConstraints() {
	c0.weightx = 1.0;
	c0a.weightx = 1.0;
	c0a.anchor = GridBagConstraints.WEST;
	c1.fill = GridBagConstraints.BOTH;
	c2.fill = GridBagConstraints.BOTH;
	c2c.fill = GridBagConstraints.BOTH;
	c1.weightx = 1.0;
	c2.gridwidth = GridBagConstraints.REMAINDER;
	c2c.gridwidth = GridBagConstraints.RELATIVE;
	c3.weightx = 0.0;
	c3.gridwidth = GridBagConstraints.REMAINDER;
	c3a.weightx = 0.0;
	c3a.gridwidth = GridBagConstraints.REMAINDER;
	c3a.anchor = GridBagConstraints.WEST;
	c4.weightx = 1.0;
	c4.gridwidth = GridBagConstraints.REMAINDER;

    }

    static JFileChooser fchooser = null;
    static JFileChooser hfchooser = null;
    void createRdataPanel() {
	rdata.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
	GridBagLayout layout = new GridBagLayout();
	rdata1.setLayout(layout);
	//rdata1.setBackground(Color.lightGray);

	connTimeoutLabel = new JLabel(localeString("connTimeout"));
	connTimeoutTF = new WholeNumbTextField("0", 5) {
		protected void onAccepted() {
		    try {
			super.onAccepted();
		    } catch (Exception e) {}
		    connTimeout = getValue()*1000;
		}
	    };
	connTimeoutTF.setToolTipText(localeString("timeoutTip"));

	readTimeoutLabel = new JLabel(localeString("readTimeout"));
	readTimeoutTF = new WholeNumbTextField("0", 5) {
		protected void onAccepted() {
		    try {
			super.onAccepted();
		    } catch (Exception e) {}
		    readTimeout = getValue()*1000;
		}
	    };
	readTimeoutTF.setToolTipText(localeString("timeoutTip"));

	methodLabel = new JLabel(localeString("methodLabel"));
	requestMethod = new JComboBox<String>(rmethods);
	urlEncodeRequestData =
	    new JCheckBox(localeString("urlEncodeRequestData"));
	inputChsetLabel = new JLabel(localeString("inputChsetLabel"));
	inputChsetButton = new JButton(localeString("inputChsetButton"));
	inputChset = new JTextField(40);
	mtypeLabel = new JLabel(localeString("mtypeLabel"));
	requestMimeTypeButton =
	    new JButton(localeString("requestMimeTypeButton"));
	requestMimeType = new JTextField(40);
	rdLabel = new JLabel(localeString("rdLabel"));
	requestDataFileButton =
	    new JButton(localeString("requestDataFileButton"));
	requestDataFileName = new JTextField(40);
	rhLabel = new JLabel(localeString("rhLabel"));
	requestHeaderFileButton =
	    new JButton(localeString("requestHeaderFileButton"));
	requestHeaderFileName = new JTextField(40);
	requestLoad = new JButton(localeString("requestLoad"));
	requestClear = new JButton(localeString("requestClear"));
	hdrAddRow = new JButton(localeString("hdrAddRow"));
	hdrInsRow = new JButton(localeString("hdrInsRow"));
	hdrDelRow = new JButton(localeString("hdrDelRow"));

	Color oldbg = rdata1.getBackground();
	int incr = 10;
	Color newbg = new Color(oldbg.getRed() + incr,
				oldbg.getGreen() + incr,
				oldbg.getBlue() + incr);

	rdata1.setBackground(newbg);
	requestMethod.setBackground(newbg);
	urlEncodeRequestData.setBackground(newbg);
	inputChsetButton.setBackground(newbg);
	requestMimeTypeButton.setBackground(newbg);
	requestDataFileButton.setBackground(newbg);
	requestHeaderFileButton.setBackground(newbg);

	JPanel timeoutPanel = new JPanel();
	GridBagLayout layout1 = new GridBagLayout();
	GridBagLayout layout2 = new GridBagLayout();
	JPanel ctPanel = new JPanel();
	ctPanel.setLayout(layout1);
	addComponent(ctPanel, connTimeoutLabel, layout1, c0a);
	addComponent(ctPanel, new JLabel(" "), layout1, c0a);
	addComponent(ctPanel, connTimeoutTF, layout1, c4);
	JPanel rtPanel = new JPanel();
	rtPanel.setLayout(layout2);
	addComponent(rtPanel, readTimeoutLabel, layout1, c0a);
	addComponent(rtPanel, new JLabel(" "), layout1, c0a);
	addComponent(rtPanel, readTimeoutTF, layout1, c4);
	timeoutPanel.add(ctPanel);
	timeoutPanel.add(new JLabel("        "));
	timeoutPanel.add(rtPanel);
	addComponent(rdata1, timeoutPanel, layout, c3a);
	/*
	addComponent(rdata1, ctPanel, layout, c0a);
	addComponent(rdata1, rtPanel, layout, c0a);
	addComponent(rdata1, new JLabel("    "), layout, c4);
	*/
	addComponent(rdata1, methodLabel, layout, c0a);
	addComponent(rdata1, requestMethod, layout, c0a);
	addComponent(rdata1, urlEncodeRequestData , layout, c3);
	addComponent(rdata1, inputChsetLabel, layout, c0a);
	addComponent(rdata1, inputChsetButton, layout, c0a);
	addComponent(rdata1, inputChset, layout, c3);
	addComponent(rdata1, mtypeLabel, layout, c0a);
	addComponent(rdata1, requestMimeTypeButton , layout, c0a);
	addComponent(rdata1, requestMimeType , layout, c3);
	addComponent(rdata1, rhLabel, layout, c0a);
	addComponent(rdata1, requestHeaderFileButton , layout, c0a);
	addComponent(rdata1, requestHeaderFileName, layout, c3);
	addComponent(rdata1, rdLabel, layout, c0a);
	addComponent(rdata1, requestDataFileButton , layout, c0a);
	addComponent(rdata1, requestDataFileName , layout, c3);

	rdataButtonPanel.add(hdrAddRow);
	rdataButtonPanel.add(hdrInsRow);
	rdataButtonPanel.add(hdrDelRow);
	rdataButtonPanel.add(requestLoad);
	rdataButtonPanel.add(requestClear);
	rdataButtonPanel.setBackground(Color.BLUE.darker());
	addComponent(rdata1, rdataButtonPanel, layout, c3);

	rdata.setLayout(new BorderLayout());
	rdata.add(rdata1, "North");
	rdata.add(splitPane, "Center");
	int y = hdrTable.getPreferredSize().height;
	y += hdrTable.getTableHeader().getPreferredSize().height;
	y += 4;
	splitPane.setDividerLocation(y);
	hdrTable.setColumnSelectionAllowed(false);
	hdrTable.setRowSelectionAllowed(true);
	hdrTable.getTableHeader().setReorderingAllowed(false);

	// Now implement the buttons.
	inputChsetButton.addActionListener(new AbstractAction () {
		public void actionPerformed(ActionEvent e) {
		    String charset = chooseCharset(frame);
		    if (charset != null) {
			if (charset != chsets[0]) {
			    inputChset.setText(charset);
			} else {
			    inputChset.setText("");
			}
		    }
		}
	    });
	requestMimeTypeButton.addActionListener(new AbstractAction () {
		public void actionPerformed(ActionEvent e) {
		    String mimetype = chooseMimeType(frame);
		    if (mimetype != null) {
			String charset = chooseCharset(frame);
			if (charset != null) {
			    if (charset != chsets[0]) {
				requestMimeType.setText(mimetype
							+"; charset="
							+charset);
			    } else {
				requestMimeType.setText(mimetype);
			    }
			}
		    }
		}
	    });
	fchooser = new JFileChooser(new File(System.getProperty("user.dir")));
	hfchooser = new JFileChooser(new File(System.getProperty("user.dir")));
	requestDataFileButton.addActionListener(new AbstractAction () {
		public void actionPerformed(ActionEvent e) {
		    int status =
			fchooser.showOpenDialog(frame);
		    if (status == JFileChooser.APPROVE_OPTION) {
			String name =
			    fchooser.getSelectedFile().getAbsolutePath();
			requestDataFileName.setText(name);
			hfchooser.
			    setCurrentDirectory(fchooser.
						getCurrentDirectory());
		    }
		}
	    });
	requestHeaderFileButton.addActionListener(new AbstractAction () {
		public void actionPerformed(ActionEvent e) {
		    int status =
			hfchooser.showOpenDialog(frame);
		    if (status == JFileChooser.APPROVE_OPTION) {
			String name =
			    hfchooser.getSelectedFile().getAbsolutePath();
			requestHeaderFileName.setText(name);
			fchooser.setCurrentDirectory(hfchooser.
						     getCurrentDirectory());
		    }
		}
	    });
	hdrAddRow.addActionListener(new AbstractAction () {
		public void actionPerformed(ActionEvent e) {
		    hdrTable.clearSelection();
		    hdrmodel.addRow((Object [])null);
		}
	    });
	hdrInsRow.addActionListener(new AbstractAction () {
		public void actionPerformed(ActionEvent e) {
		    int selind = hdrTable.getSelectedRow();
		    int n = hdrTable.getSelectedRowCount();
		    if (n > 1 || n == 0) {
			    JOptionPane
				.showMessageDialog(frame,
						   localeString("selectOne"),
						   localeString("selectTitle"),
						  JOptionPane.ERROR_MESSAGE);
			    return;

		    }
		    hdrmodel.insertRow(selind, (Object [])null);
		    hdrTable.setRowSelectionInterval(selind, selind);
		}
	    });
	hdrDelRow.addActionListener(new AbstractAction () {
		public void actionPerformed(ActionEvent e) {
		    int selected[] =  hdrTable.getSelectedRows();
		    int i = (selected == null)? 0: selected.length;
		    while (i-- > 0) {
			hdrmodel.removeRow(selected[i]);
		    }
		}
	    });
	requestClear.addActionListener(new AbstractAction () {
		public void actionPerformed(ActionEvent e) {
		    // requestMimeType.setText(null);
		    // requestDataFileName.setText(null);
		    // requestHeaderFileName.setText(null);
		    Object[] options = {
			localeString("rcoption1"),
			localeString("rcoption2"),
			localeString("rcoption3")
		    };
		    Object option =
			JOptionPane.showInputDialog(frame,
						    localeString("clearText"),
						    localeString("clearTitle"),
						    JOptionPane.PLAIN_MESSAGE,
						    null,
						    options, options[0]);
		    if (option == null) {
			return;
		    } else if (option.equals(options[0])) {
			clearHdrTable();
			hdrsLoaded = false;
			request.setText("");
			dataLoaded = false;
		    } else if (option.equals(options[1])) {
			clearHdrTable();
			hdrsLoaded = false;
		    } else if (option.equals(options[2])) {
			request.setText("");
			dataLoaded = false;
		    }
		    /*
		    clearHdrTable();
		    request.setText("");
		    */
		    fullMimeType = null;
		    loaded = false;
		}
	    });
	requestLoad.addActionListener(new AbstractAction () {
		public void actionPerformed(ActionEvent e) {
		    String hname = requestHeaderFileName.getText();
		    String dname = requestDataFileName.getText();
		    try {
			if (hname != null && hname.length() > 0) {
			    readHeadersFromFile(hname);
			    hdrsLoaded = true;
			}
		    } catch (Exception ee) {
			    JOptionPane.
				showMessageDialog(frame,
						  ee.getMessage(),
						  localeString("hdrFileErr"),
						  JOptionPane.ERROR_MESSAGE);
			return;
		    }
		    try {
			if (dname != null && dname.length() > 0) {
			    readDataFromFile(dname);
			    dataLoaded = true;
			}
		    } catch (Exception ee) {
			    JOptionPane.
				showMessageDialog(frame,
						  ee.getMessage(),
						  localeString("dataFileErr"),
						  JOptionPane.ERROR_MESSAGE);
			return;
		    }
		    loaded = true;
		    dataLoaded = true;
		    hdrsLoaded = true;
		}
	    });
    }


    static String getCharsetName(String mimetype) {
	mimetype = mimetype.toUpperCase().trim() ;
	int ind = mimetype.indexOf(';');
	if (ind >= 0) {
	    ind = mimetype.indexOf("CHARSET", ind);
	    if (ind >= 0) {
		ind += 7;
		char ch = ' ';;
		int n = mimetype.length();
		while (ind < n && ((ch = mimetype.charAt(ind)) == ' ' ||
				   ch == '\t'))
		    ind++;
		if (ind < n & ch == '=') {
		    ind++;
		    while (ind < n && ((ch = mimetype.charAt(ind)) == ' ' ||
				       ch == '\t'))
			ind++;
		    String parts[] = mimetype.substring(ind).split("[;, \t]");
		    if (parts.length > 0) {
			return parts[0];
		    }
		}
	    }

	}
	return "US-ASCII";
    }

    static void readHeadersFromFile(String name) throws Exception {
	clearHdrTable();
	File f;
	if ((new File(name)).isAbsolute()) {
	    f = new File(name);
	} else {
	    f = new File(System.getProperty("user.dir"), name);
	}
	BufferedReader in =
	    new BufferedReader(new InputStreamReader(new FileInputStream(f)));
	String line;
	int i = 0;
	int n = hdrmodel.getRowCount();
	while ((line = in.readLine()) != null) {
	    int ind = line.indexOf(':');
	    if (ind < 1) throw new Exception(localeString("missingColon"));
	    String key = line.substring(0,ind).trim();
	    if (ind+1 >= line.length())
		throw new Exception(localeString("noVal4Key", key));
	    String value = line.substring(ind+1).trim();
	    if ((--n) < 0) {
		hdrmodel.addRow((Object [])null);
	    }
	    hdrmodel.setValueAt(key, i, 0);
	    hdrmodel.setValueAt(value, i, 1);
	    i++;
	}
	return;
    }
    static String fullMimeType = null;
    void readDataFromFile(String name) throws Exception {
	fullMimeType = requestMimeType.getText();
	if (fullMimeType != null) fullMimeType = fullMimeType.trim();
	request.setText("");
	File f;
	if ((new File(name)).isAbsolute()) {
	    f = new File(name);
	} else {
	    f = new File(System.getProperty("user.dir"), name);
	}
	String str = inputChset.getText();
	String charsetName = (str != null)?
	    inputChset.getText().trim().toUpperCase(): "";
	if (isTextMimeType(fullMimeType)) {
	    // if no charset provided, use system default.
	    BufferedReader in =
		new
		BufferedReader((charsetName.length() > 0)?
			       (new
				InputStreamReader(new FileInputStream(f),
						  charsetName)):
			       (new
				InputStreamReader(new FileInputStream(f))));
	    String line;
	    boolean notfirst = false;
	    while ((line = in.readLine()) != null) {
		if (notfirst) {
		    request.appendString("\n");
		} else {
		    notfirst = true;
		}
		request.appendString(line);
	    }
	} else {
	    request.setTextForeground(CONTENT_COLOR);
	    request.appendString("<file:" +f.getAbsolutePath() +">");
	    request.setTextForeground(Color.BLACK);
	}
    }

    void createPrefPanel() {

	prefs1.setLayout(new BorderLayout());
	prefs1.add(prefs, "North");

	GridBagLayout layout = new GridBagLayout();
	prefs.setLayout(layout);


	useHttpProxy = new JCheckBox(localeString("useHttpProxy"));
	addComponent(prefs, useHttpProxy, layout, c2);
	useHttpProxy.addActionListener(new AbstractAction () {
		public void actionPerformed(ActionEvent e) {
		    useHttpProxyFlag = useHttpProxy.isSelected();
		}
	});

	useHttpsProxy = new JCheckBox(localeString("useHttpsProxy"));
	addComponent(prefs, useHttpsProxy, layout, c2);
	useHttpsProxy.addActionListener(new AbstractAction () {
		public void actionPerformed(ActionEvent e) {
		    useHttpsProxyFlag = useHttpsProxy.isSelected();
		}
	});

	useFtpProxy = new JCheckBox(localeString("useFtpProxy"));
	addComponent(prefs, useFtpProxy, layout, c2);
	useFtpProxy.addActionListener(new AbstractAction () {
		public void actionPerformed(ActionEvent e) {
		    useFtpProxyFlag = useFtpProxy.isSelected();
		}
	});

	useSocksProxy = new JCheckBox(localeString("useSocksProxy"));
	addComponent(prefs, useSocksProxy, layout, c2);
	useSocksProxy.addActionListener(new AbstractAction () {
		public void actionPerformed(ActionEvent e) {
		    useSocksProxyFlag = useSocksProxy.isSelected();
		}
	});

	useProxyDefault = new JCheckBox(localeString("useProxyDefault"));
	addComponent(prefs, useProxyDefault, layout, c2);
	useProxyDefault.addActionListener(new AbstractAction () {
		public void actionPerformed(ActionEvent e) {
		    useProxyDefaultFlag = useProxyDefault.isSelected();
		}
	});

	useUserAgent = new JCheckBox(localeString("useUserAgent"));
	addComponent(prefs, useUserAgent, layout, c2);
	useUserAgent.addActionListener(new AbstractAction () {
		public void actionPerformed(ActionEvent e) {
		    useUserAgentFlag = useUserAgent.isSelected();
		}
	});


	addComponent(prefs, new JLabel(" "), layout, c3);

	JLabel label1 = new JLabel(localeString("label1"));
	addComponent(prefs, label1, layout, c1);
	httpProxyHostField = new VTextField(40) {
		protected void onAccepted() {
		    httpProxyHost = getText();
		}
	    };
	addComponent(prefs, httpProxyHostField, layout, c2);


	JLabel label2 = new JLabel(localeString("label2"));
	addComponent(prefs, label2, layout, c1);
	httpProxyPortField = new PortTextField(40) {
		protected void onAccepted(int chartype) {
		    httpProxyPort = getText();
		}
	    };
	addComponent(prefs, httpProxyPortField, layout, c2);

	JLabel label3 = new JLabel(localeString("label3"));
	addComponent(prefs, label3, layout, c1);
	httpNonProxyHostsField =
	    new VTextField(40) {
		protected void onAccepted() {
		    httpNonProxyHosts = getText();
		}
	    };
	addComponent(prefs, httpNonProxyHostsField, layout, c2);

	addComponent(prefs, new JLabel(" "), layout, c3);

	JLabel label4 = new JLabel(localeString("label4"));
	addComponent(prefs, label4, layout, c1);
	httpsProxyHostField = new VTextField(40)  {
		protected void onAccepted() {
		    httpsProxyHost = getText();
		}
	    };
	addComponent(prefs, httpsProxyHostField, layout, c2);

	JLabel label5 = new JLabel(localeString("label5"));
	addComponent(prefs, label5, layout, c1);
	httpsProxyPortField = new PortTextField(40) {
		protected void onAccepted(int chartype) {
		    httpsProxyPort = getText();
		}
	    };
	addComponent(prefs, httpsProxyPortField, layout, c2);

	/*
	JLabel label6 = new JLabel(localeString("label6"));
	addComponent(prefs, label6, layout, c1);
	httpsNonProxyHostsField = new VTextField(40) {
		protected void onAccepted() {
		    httpsNonProxyHosts = getText();
		}
	    };
	addComponent(prefs, httpsNonProxyHostsField, layout, c2);
	*/
	addComponent(prefs, new JLabel(" "), layout, c3);

	JLabel label7 = new JLabel(localeString("label7"));
	addComponent(prefs, label7, layout, c1);
	ftpProxyHostField = new VTextField(40) {
		protected void onAccepted() {
		    ftpProxyHost = getText();
		}
	    };
	addComponent(prefs, ftpProxyHostField, layout, c2);

	JLabel label8 = new JLabel(localeString("label8"));
	addComponent(prefs, label8, layout, c1);
	ftpProxyPortField = new PortTextField(40) {
		protected void onAccepted(int chartype) {
		    ftpProxyPort = getText();
		}
	    };
	addComponent(prefs, ftpProxyPortField, layout, c2);

	JLabel label9 = new JLabel(localeString("label9"));
	addComponent(prefs, label9, layout, c1);
	ftpNonProxyHostsField = new VTextField(40) {
		protected void onAccepted() {
		    ftpNonProxyHosts = getText();
		}
	    };
	addComponent(prefs, ftpNonProxyHostsField, layout, c2);

	addComponent(prefs, new JLabel(" "), layout, c3);

	JLabel label10 = new JLabel(localeString("label10"));
	addComponent(prefs, label10, layout, c1);
	socksProxyHostField = new VTextField(40) {
		protected void onAccepted() {
		    socksProxyHost = getText();
		}
	    };
	addComponent(prefs, socksProxyHostField, layout, c2);

	JLabel label11 = new JLabel(localeString("label11"));
	addComponent(prefs, label11, layout, c1);
	socksProxyPortField = new
	    PortTextField(40) {
		protected void onAccepted(int chartype) {
		    socksProxyPort = getText();
		}
	    };
	addComponent(prefs, socksProxyPortField, layout, c2);

	addComponent(prefs, new JLabel(" "), layout, c3);

	JLabel label12 = new JLabel(localeString("label12"));
	addComponent(prefs, label12, layout, c1);
	userAgentField = new VTextField(40) {
		protected void onAccepted() {
		    userAgent = getText();
		}
	    };
	addComponent(prefs, userAgentField, layout, c2);

	addComponent(prefs, new JLabel(" "), layout, c3);

	JButton saveButton = new JButton(localeString("saveButton"));
	addComponent(prefs, saveButton, layout, c0);
	saveButton.addActionListener(new AbstractAction () {
		public void actionPerformed(ActionEvent e) {
		    setPreferences();
		}
	    });

	JButton applyButton = new JButton(localeString("applyButton"));
	addComponent(prefs, applyButton, layout, c2c);
	applyButton.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    System.setProperties(setupEnv(startupProperties, true));
		}
	    });

	JButton cancelChanges = new JButton(localeString("cancelChanges"));
	addComponent(prefs, cancelChanges, layout, c3);
	cancelChanges.addActionListener(new AbstractAction () {
		public void actionPerformed(ActionEvent e) {
		    getPreferences(true);
		}
	    });
	getPreferences(true);
    }

    JCheckBox useproxy = new JCheckBox(localeString("useproxy"));

    WholeNumbTextField bcount = new WholeNumbTextField(6) {
	    protected void onAccepted() throws Exception {
		String text = getText();
		// The call to the superclass will fail
		// for the following text value, so we
		// have to handle this case explicitly.
		if (text.equals("\u221E")) {
		    // setValue(Integer.MAX_VALUE, text);
		    setValue(Integer.MAX_VALUE);
		    // The text is not verified when setText
		    // is called during a call to onAccepted()
		    // and instead simply sets the text field itself.
		    setText(text);
		} else {
		    super.onAccepted();
		}
	    }
	};

    private void pageUpHandler() {
	if (currentPane == prefsScrollpane) return;
	JViewport vp = scrollpane.getViewport();
	Dimension size = vp.getExtentSize();
	int delta = size.height;
	Point p = vp.getViewPosition();
	p.translate(0, -delta);
	int offset = result.viewToModel2D(toPoint2D(p));
	result.setEditable(true);
	result.setCaretPosition(offset);
	try {
	    result.scrollRectToVisible(new Rectangle(p, size));
	} catch(Exception badloc) {}
	result.setEditable(false);
    }

    private void pageDownHandler() {
	if (currentPane == prefsScrollpane) return;
	JViewport vp = scrollpane.getViewport();
	Dimension size = vp.getExtentSize();
	int delta = size.height;
	Point p = vp.getViewPosition();
	p.translate(0, delta);
	int offset = result.viewToModel2D(toPoint2D(p));
	result.setEditable(true);
	result.setCaretPosition(offset);
	try {
	    result.scrollRectToVisible(new Rectangle(p, size));
	} catch(Exception badloc) {}
	result.setEditable(false);
    }



    // Needed to fix a Java API change
    private Rectangle toRect(Rectangle2D r) {
	if (r == null) return null;
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
    private Point2D toPoint2D(Point p) {
	if (p == null) return null;
	return new Point2D.Double(p.getX(), p.getY());
    }


    private void toStartHandler() {
	if (currentPane == prefsScrollpane) return;
	result.setCaretPosition(0);
	try {
	    result.scrollRectToVisible(toRect(result.modelToView2D(0)));
	} catch (Exception badloc) {}
    }

    private void toEndHandler() {
	if (currentPane == prefsScrollpane) return;
	int len = result.getDocument().getLength();
	result.setCaretPosition(len);
	try {
	    result.scrollRectToVisible(toRect(result.modelToView2D(len)));
	} catch (Exception badloc) {}
    }

    class Cleanup implements Runnable {
	Thread ourthread;
	Cleanup(Thread xthread) {
	    ourthread = xthread;
	}
	public void run() {
	    if (ourthread != thread) {
		try {
		    if (SwingUtilities.isEventDispatchThread()) {
			JOptionPane.showMessageDialog
			    (frame, localeString("reset"),
			     localeString("resetTitle"),
			     JOptionPane.ERROR_MESSAGE);
		    } else {
			SwingUtilities.invokeAndWait(()-> {
				JOptionPane.showMessageDialog
				    (frame, localeString("reset"),
				     localeString("resetTitle"),
				     JOptionPane.ERROR_MESSAGE);
			    });
		    }
		} catch (Exception e) {}
		return;
	    }
	    try {
		SwingUtilities.invokeLater(() -> {
			result.setSwingSafe(false);
			result.setVisible(true);
			result.setCaretPosition(0);
			try {
			    result.scrollRectToVisible
				(toRect(result.modelToView2D(0)));
			} catch (Exception badloc) {}
			if (dataInputStream != null) {
			    try {
				dataInputStream.close();
			    } catch (Exception e) {}
			    dataInputStream = null;
			}
			result.setEditable(false);
			cancel.setEnabled(false);
			input.setEnabled(true);
			input.setEditable(true);
			hdrTable.setEnabled(true);
			button.setEnabled(true);
			request.setEnabled(true);
			useproxy.setEnabled(true);
			if (savedCardname != null &&
			    ourthread.isInterrupted()) {
			    // in we ran due to a  cancel.
			    cardname = savedCardname;
			    cardlayout.show(centerPanel, cardname);
			    savedCardname = null;
			}
			input.requestFocus();
		    });
	    } catch (Exception ee) {}
	    thread = null;
	}
    }

    Runnable cleanup = null;

    public void init() {
	Container panel = this;

	initConstraints();
	initChsets();

	result.setEditable(false);

	CharDocFilter cdf = (CharDocFilter)
	    ((AbstractDocument)bcount.getDocument()).getDocumentFilter();
	cdf.setOptSingleChars("*\u221E");
	cdf.setSingleCharMap("*\u221E");

	bcount.setToolTipText(localeString("bcountTip"));

	panel.setLayout(new BorderLayout());
	top.setLayout(new FlowLayout(FlowLayout.LEFT) {
		public Dimension minimumLayoutSize(Container parent) {
		    Dimension d = super.minimumLayoutSize(parent);
		    d.height *= 2;
		    return d;
		}
		public Dimension preferredLayoutSize(Container parent) {
		    Dimension d = super.preferredLayoutSize(parent);
		    d.height *= 2;
		    return d;
		}
	    });
	top.add(new JLabel(localeString("url")));
	top.add(input);
	top.add(useproxy);
	top.add(new JLabel("    "));
	top.add(new JLabel(localeString("maxCC")));
	top.add(bcount);
	top.add(button);
	top.add(cancel);
	cancel.setEnabled(false);

	input.addActionListener(this);
	input.setEnabled(true);

	useproxy.setEnabled(true);
	bcount.setEnabled(true);

	button.addActionListener(this);
	button.addFocusListener(new FocusAdapter() {
		public void focusLost(FocusEvent e) {
		    Component c = e.getOppositeComponent();
		    if (cancel == c || result == c) {
			if (input.isEnabled()) input.requestFocus();
		    }
		}
	    });
	input.addFocusListener(new FocusAdapter() {
		public void focusLost(FocusEvent e) {
		    Component c = e.getOppositeComponent();
		    if (c != null) {
			if (c == result) {
			    button.requestFocus();
			}
		    }
		}
	    });

	cancel.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    try {
			if (thread != null) thread.interrupt();
			if (elements != null) elements.closeConnection();
		    } catch (Exception ee) {
		    } finally {
			result.clear();
			cancel.setEnabled(false);
			useproxy.setEnabled(true);
			result.setEditable(false);
			input.setEditable(true);
			input.setEnabled(true);
			button.setEnabled(true);
			input.requestFocus();
			if (savedCardname != null) {
			    cardname = savedCardname;
			    cardlayout.show(centerPanel, cardname);
			    savedCardname = null;
			}
		    }
		}
	    });

	top.revalidate();
	panel.add(top, "North");
	centerPanel.setLayout(cardlayout);
	centerPanel.add(rdata, "rdata");
	if (!sawP) createPrefPanel();
	centerPanel.add(scrollpane, "main");
	createRdataPanel();
	if (!sawP) {
	    centerPanel.add(prefsScrollpane, "prefs");
	    useproxy.setSelected(useProxyDefaultFlag);
	}
	// panel.add(scrollpane, "Center");
	panel.add(centerPanel, "Center");
    }

    public void start() {
	setVisible(true);
	input.requestFocus();
    }

    public void actionPerformed(ActionEvent e) {
	doAction();
    }
    Thread thread = null;
    JButton cancel = new JButton(localeString("cancel"));
    static InputStream dataInputStream = null;;
    HeaderEnumeration elements = null;
    void doAction() {
	input.setEnabled(false);
	button.setEnabled(false);
	useproxy.setEnabled(false);
	dataInputStream = null;
	savedCardname = null;
	try {
	    if (thread != null) thread.interrupt();
	} catch (Exception ee) {}
	result.setSwingSafe(true);
	final String method = (String)requestMethod.getSelectedItem();
	if (hdrTable.isEditing()) {
	    hdrTable.getCellEditor().stopCellEditing();
	}
	hdrTable.setEnabled(false);
	request.setEnabled(false);
	if (method.equals("POST") || method.equals("PUT")) {
	    if (dataLoaded || (isTextMimeType(fullMimeType)
			       && request.getText().length() > 0)) {
		try {
		    if (isTextMimeType(fullMimeType)) {
			boolean urlEncode = urlEncodeRequestData.isSelected();
			String cset = getCharsetName(fullMimeType);
			// data is held in request text area.
			if (urlEncode) {
			    BufferedReader reader =
				new BufferedReader
				(new FormReader
				 (new DocumentReader(request.getDocument())));
			    // no line breaks due to encoding, and we
			    // can use US_ASCII for the encoding as the UTF-8
			    // is itself URL Encoded.
			    String encodedData = reader.readLine();
			    dataInputStream =
				new ByteArrayInputStream
				(encodedData.getBytes("US-ASCII"));
			} else {
			    dataInputStream =
				new ByteArrayInputStream(request.getText().
							 getBytes(cset));
			}
		    } else {
		    // data is in the input file.
			String dname = requestDataFileName.getText();
			File f;
			if ((new File(dname)).isAbsolute()) {
			    f = new File(dname);
			} else {
			    f = new
				File(System.getProperty("user.dir"), dname);
			}
			dataInputStream = new FileInputStream(f);
		    }
		} catch (Exception eee) {
		    JOptionPane.
		    showMessageDialog(frame,
				      eee.getMessage(),
				      "Input Error",
				      JOptionPane.ERROR_MESSAGE);
		    return;
		}
	    } else {
		JOptionPane.
		showMessageDialog(frame,
				  "Input dataset not loaded",
				  "Input Error",
				  JOptionPane.ERROR_MESSAGE);
		return;
	    }
	}
	savedCardname = cardname;
	cardname = "main";
	cardlayout.show(centerPanel, "main");
	currentPane = scrollpane;
	cancel.setEnabled(true);
	cancel.requestFocus(true);
	result.setEditable(true);
	final Object syncPoint = new Object();
	thread = new Thread() {
		public void run() {
		    Thread ourThread = Thread.currentThread();
		    Cleanup ourcleanup = new Cleanup(ourThread);
		    cleanup = ourcleanup;
		    // HeaderEnumeration elements = null;
		    synchronized (HeaderEnumeration.class) {
			if (elements != null) {
			    elements.closeConnection();
			}
			elements = null;
		    }
		    synchronized (syncPoint) {
			syncPoint.notify();
		    }
		    try {
			System.
			    setProperties
			    (setupEnv((Properties)
				      startupProperties.clone(),
				      useproxy.isSelected()));
			elements = getHeaders(input.getText(),
					      method,
					      fullMimeType,
					      hdrmodel, dataInputStream);
			int bvalue = bcount.getValue();
			if (method.equals("HEAD")) bvalue = 0;
			result.setText(elements, bvalue, ourThread);
		    } catch (java.lang.InterruptedException ei) {
			if (elements != null) elements.closeConnection();
		    } catch (Exception ee) {
			if (thread  == ourThread) {
			    try {
				// any unexpected exception.
				// clear so we only show the
				// exception message.
				result.clear();
			    } catch (Throwable t) {}
			    result.handleException(ee);
			}
		    } finally {
			javax.swing.SwingUtilities.invokeLater(cleanup);
		    }
		}
	    };
	    result.clear();
	thread.start();
	try {
	    syncPoint.wait();
	} catch (Exception e) {}
    }
}
