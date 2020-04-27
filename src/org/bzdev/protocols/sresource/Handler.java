package org.bzdev.protocols.sresource;
//@exbundle org.bzdev.protocols.sresource.lpack.Handler

import org.bzdev.protocols.Handlers;
import org.bzdev.util.SafeFormatter;

import java.net.*;
import java.io.*;
import java.util.jar.JarFile;
import java.util.ResourceBundle;

public class Handler extends URLStreamHandler {

    static ResourceBundle exbundle = ResourceBundle.getBundle
	("org.bzdev.protocols.sresource.lpack.Handler");

    static String errorMsg(String key, Object... args) {
	return (new SafeFormatter()).format(exbundle.getString(key), args)
	    .toString();
    }

    static String pathsep;
    {
	try {
	    pathsep = System.getProperty("path.separator");
	} catch (Throwable t) {
	    pathsep = null;
	}
    }
    protected URLConnection openConnection(URL u) throws IOException {
	String path = u.getPath();
	String ref = u.getRef();
	if (path == null) throw new MalformedURLException(errorMsg("pathNull"));
	String resource = path.startsWith("/")? path.substring(1): path;
					   
	URL url  = ClassLoader.getSystemClassLoader().getResource(resource);
	if (ref != null) url = new URL(url, "#" + ref);
	if (url == null) {
	    throw new FileNotFoundException(errorMsg("resourceNotFound"));
	}
	return url.openConnection();
    }
}
