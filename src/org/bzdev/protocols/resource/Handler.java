package org.bzdev.protocols.resource;
import org.bzdev.util.SafeFormatter;
import java.security.*;
import org.bzdev.net.EncapURLConnection;
import org.bzdev.protocols.Handlers;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.ResourceBundle;

//@exbundle org.bzdev.protocols.resource.lpack.Handler

public class Handler extends URLStreamHandler {

    // This URLStreamHandler implements the "resource" protocol,
    // which searches for resources in JAR or ZIP files or via a
    // network connection. The search path for resources is set up
    // via system properties, and these are read when
    // org.bzdev.protocols.Handlers.enable() is called.  That
    // method has no effect if called more than once.  It also
    // checks a permission in case it is called after a security
    // manager is installed.
    //
    // The use of doPrivileged blocks allows these resources to be
    // accessed - read only unless additional permissions are obtained.
    //
    static class ResourceURLConnection extends EncapURLConnection {
	ResourceURLConnection(URLConnection urlc) {
	    super(urlc);
	}

	@Override
	public void connect() throws IOException {
	    try {
		AccessController.doPrivileged
		    (new PrivilegedExceptionAction<Void>() {
			public Void run() throws IOException {
			    ResourceURLConnection.super.connect();
			    return null;
			}
		    });
	    } catch (PrivilegedActionException e) {
		throw (IOException)e.getException();
	    }
	}

	@Override
	public InputStream getInputStream() throws IOException {
	    try {
		return AccessController.doPrivileged
		    (new PrivilegedExceptionAction<InputStream>() {
			public InputStream run() throws IOException {
			    return
				ResourceURLConnection.super.getInputStream();
			}
		    });
	    } catch (PrivilegedActionException e) {
		throw (IOException)e.getException();
	    }
	}
    }

    static ResourceBundle exbundle = ResourceBundle.getBundle
	("org.bzdev.protocols.resource.lpack.Handler");

    static String errorMsg(String key, Object... args) {
	return (new SafeFormatter()).format(exbundle.getString(key), args)
	    .toString();
    }

    static String pathsep = "|";
    static String RE =
	"([-[a-zA-Z0-9.+~!_$&'()*+,;=]|]|%[0-9A-Za-z][0-9A-Za-z])+";
    static Pattern compiledRE = Pattern.compile(RE);

    static String normalize(String path) throws IOException {
	if (path.endsWith("/")) {
	    throw new IOException(errorMsg("slashAtEnd", path));
	}
	String[] components = path.split("/");
	ArrayList<String> alist = new ArrayList<>(components.length);
	int index = 0;
	try {
	    for (String component: components) {
		if (component.length() == 0) {
		    throw new IOException(errorMsg("nullComponent", path));
		}
		if (!compiledRE.matcher(component).matches()) {
		    throw new IOException(errorMsg("badPath",component, path));
		}
		if (component.equals(".")
		    || component.equals("%2E")) {
		    continue;
		} else if (component.equals("..")
			   || component.equals("%2E.")
			   || component.equals(".%2E")
			   || component.equals("%2E%2E")) {
		    index--;
		    alist.remove(index);
		} else {
		    alist.add(index++, component);
		}
	    }
	    StringBuilder sb = new StringBuilder(path.length());
	    boolean first = true;
	    for (String component: alist) {
		if (first) {
		    first = false;
		} else {
		    sb.append("/");
		}
		sb.append(component);
	    }
	    return sb.toString();
	} catch (IndexOutOfBoundsException e) {
	    throw new IOException(errorMsg("pathDotDot", path));
	}
    }


    protected URLConnection openConnection(final URL u) throws IOException {
	try {
	    return new ResourceURLConnection
		(AccessController.doPrivileged
		 (new PrivilegedExceptionAction<URLConnection>() {
		     public URLConnection run() throws IOException {
			 return openConnectionAux(u);
		     }
		 }));
	} catch (PrivilegedActionException e) {
	    throw (IOException)e.getException();
	}
    }

    private URLConnection openConnectionAux(URL u) throws IOException {
	String spath = (pathsep == null)? null: 
	    Handlers.getProperty("resource.path");
	String path = u.getPath();
	String ref = u.getRef();
	if (path == null) throw new MalformedURLException(errorMsg("pathNull"));
	String resource = path.startsWith("/")? path.substring(1): path;
	resource = normalize(resource);
	// String resource = resource1 +((ref == null)? "": ("#" + ref));
	URL url = null;
	if (spath == null) {
	    // use the system class loader as a default.
	    spath = "$classpath";
	} else {
	    String locs[] = spath.split(Pattern.quote(pathsep));
	    String urlstr = null;
	    for (int i = 0; i < locs.length; i++) {
		// System.out.println("got here: " + locs[i]);
		if (locs[i].equals("$classpath")) {
		    // search the system class path.
		    url = ClassLoader.getSystemClassLoader()
			.getResource(resource);
		    if (ref != null) url = new URL(url, "#" + ref);
		    try {
			return url.openConnection();
		    } catch(Exception e0) {
			continue;
		    }
		} else if (locs[i].matches("^(http|https|ftp|file):.*")) {
		    // System.out.println("got here [1]");
		    if (locs[i].endsWith(".zip") || locs[i].endsWith(".jar")) {
			urlstr = "jar:" + locs[i] + "!/" + resource;
		    } else {
			urlstr = (locs[i].endsWith("/")? locs[i]:
				  locs[i] + "/")
			    + resource;
		    }
		    try {
			url = new URL(urlstr);
			if (ref != null) url = new URL(url, "#" + ref);
			URLConnection urlc = url.openConnection();
			// Need to check that this adequately detects a
			// missing resource - if none found, we want to
			// try the remainder of the search path.
			urlc.connect();
			InputStream is = urlc.getInputStream();
			if (is == null) {
			    // System.out.println("got here [2]");
			    continue;
			} else {
			    // System.out.println("got here [3]");
			    is.close();
			    return urlc;
			}
		    } catch (Exception e) {
			// System.out.println("got here [4]");
			continue;
		    }
		} else {
		    File f = new File(locs[i]).getAbsoluteFile();
		    if (f.isDirectory()) {
			URL furl = f.toURI().toURL();
			try {
			furl = new URL(furl, resource);
			    f = new File(furl.toURI());
			} catch (URISyntaxException e) {
			    continue;
			}
			if (f.exists() && f.canRead()) {
			    urlstr = furl.toString();
			    break;
			}
		    } else {
			f = new File(locs[i]).getAbsoluteFile();
			if (f.isFile()) {
			    /*
			     * Assume it is a jar file, regardless of
			     * the extension. We should get an exception
			     * if this doesn't work.
			     */
			    JarFile jf = new JarFile(f);
			    if (jf.getEntry(resource) != null) {
				urlstr = "jar:" +
				    (f.toURI().toURL()).toString()
				    +"!/" + resource;
				/*
				urlstr = "jar:file:"+f.getAbsolutePath()
				    +"!/" +resource;
				*/
				break;
			    }
			}
		    }
		}
	    }
	    if (urlstr != null) {
		url = new URL(urlstr);
		if (ref != null) url = new URL(url, "#" + ref);
	    }
	}
	if (url == null) {
	    throw new FileNotFoundException(errorMsg("resourceNotFound"));
	}
	return url.openConnection();
    }
}
