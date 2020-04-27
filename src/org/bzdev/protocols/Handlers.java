package org.bzdev.protocols;
import java.net.NetPermission;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.security.*;

/**
 * Configuration class for protocol handlers.  This class enables
 * various protocol handlers by setting or modifying a system property
 * for handlers defined in subpackages of the current package.  Only
 * the subpackages actually intended for use should be included in an
 * application.  Protocol Properties are set by setting system properties
 * whose names start with "org.bzdev.protocols." followed by the name
 * of the protocol or scheme, followed by a period and then the name of the
 * property.
 * When protocol handlers are enabled by calling <code>Handlers.enable()</code>,
 * system properties starting with "<code>org.bzdev.protocols.</code>" are
 * read and stored under a key consisting of the remainder of the
 * system-property key. Thus, a system property named
 * <code>org.bzdev.protocols.resource.path</code> is used to set the
 * <em>path</em>property for the <em>resource</em> protocol. A call
 * to Handlers.get("resource.path") will return th is property.
 * <P>
 * The supported schemes are
 * <ul>
 *  <li> resource. This scheme looks up resources based on a class
 *       path whose entries are separated by '|' and that is stored in a
 *       system property named org.bzdev.protocols.resource.path. If this
 *       system property is not provided, the system class loader
 *       will be used as a default. The path entry
 *       $classpath indicates that the system class loader
 *       will be used to look for the resource, and may be included
 *       as a component in the class path. It should be the first
 *       entry when it is desirable to prevent other classes from
 *       overriding a resource that the system class loader would
 *       otherwise provide. The resource path's components are directories,
 *       jar files, or zip files, and the path following the "resource"
 *       protocol starts at the top level of these directories or archives.
 *  <li> sresource. This scheme looks up resources using the
 *       system class loader.
 * </ul>
 * For the <code>sresource</code> scheme, the handler simply uses
 * the method {@link java.lang.ClassLoader#getSystemClassLoader()} to
 * find the system class loader and then uses the method
 * {@link java.lang.ClassLoader#getResource(String)} to obtain a URL
 * that will allow access to the resource. This URL is used instead of
 * the 'sresource' URL, so security is provided by the standard Java
 * mechanisms.  For the <code>resource</code> scheme, additional
 * permissions have to be granted.  The locations for the resource
 * (typically JAR files, ZIP files, or directories, but possibly resources
 * accessible over a network connection) are provided by the system
 * property described above, and cannot be changed once
 * {@link Handlers#enable()} is called.  Furthermore, unless a specific
 * permission is granted, {@link Handlers#enable()} will fail if called
 * after a security manager is installed. In addition, the path portion
 * of the URL (the part after "resource:"), is checked so that
 * <ul>
 *  <li> the path does not contain any illegal characters: ones not
 *       allowed by RFC 3986.  The character '\' specifically is not
 *       allowed as a literal character in the URL.
 *  <li> ".." entries in the path will not result in access to an
 *       ancestor of an entry in the list of resource locations.
 * </ul>
 * After Java 8, a module system was added to Java and this places
 * some constraints on the visibility of resources. Generally, a module
 * must declare a package to be "open" for its resources to be accessible.
 * The unnamed module (i.e., the code accessed via a classpath) is
 * open. For named modules, the module-info.java file must explicitly
 * indicate that a package is open (a Java command-line option can also
 * be used, but there is no guarantee that this option will not be
 * eliminated in future releases.)  For the "resource" protocol, however,
 * the files or directories explicitly listed by the
 * <CODE>org.bzdev.protocols.resource.path</CODE> property are accessed
 * through the file system, bypassing any constraints imposed by the
 * Java module system with one exception: the path component
 * <CODE>$classpath</CODE> will have its resources searched using the
 * system class loader.
 */
public class Handlers {
    static private final String KEY = "java.protocol.handler.pkgs";
    static private final String PKG = "org.bzdev.protocols";
    static private Map<String,String> map = new HashMap<String,String>();

    static private final String PREFIX = PKG + "."; 
    static private final int PREFIXLEN = PREFIX.length();
    static private Set<String> appendOnlySet =
	new HashSet<String>();

    static {
	Handlers.setProperty("resource.path", "$classpath");
	// Handlers.appendOnlySet.add("resource.path");
    }

    static boolean enabled = false;

    /**
     * Enables protocol/scheme handlers.
     * The handlers enabled are ones in the subpackages of
     * org.bzdev.protocols. The property list that Handlers maintains
     * is also initialized by searching the system properties for properties
     * whose names start with "org.bzdev.protocols." with the remainder of
     * the name used as the property name in the Handlers property list
     * and the value of the system property as the corresponding value in the
     * Handlers property list.
     * Calls to enable() should be made only once - after the first call,
     * subsequent calls are ignored.
     * <P>

     * When a security manager is in place, the permission
     * org.bzdev.protocols.HandlersEnablePermission must have been granted
     * with a target of "org.bzev.protocols.enable" if Handlers.enable()
     * is called:
     * <blockquote><code><pre>
     *    grant ... {
     *      permission org.bzdev.protocols.HandlersEnablePermission
     *          "org.bzdev.protocols.enable";
     *    };
     * </pre></code></blockquote>
     * If possible <code>Handlers.enable()</code>
     * should be called before a security manager is installed, and as
     * early as possible in a program if it is called at all.
     * @throws SecurityException the operation was not allowed
     */
    static public void enable() throws SecurityException
    {
	if (enabled) return;
	final SecurityManager sm = System.getSecurityManager();
	if (sm != null) {
	    sm.checkPermission(new HandlersEnablePermission
			       ("org.bzdev.protocols.enable"));
	}
	AccessController.doPrivileged
	    (new PrivilegedAction<Void>() {
		    public Void run() {
			enabled = true;
			// set up the handler properties table
			// based on system properties
			for (String name:
				 System.getProperties().stringPropertyNames()) {
			    if (name.startsWith(PREFIX)) {
				String value = System.getProperty(name);
				name = name.substring(PREFIXLEN);
				int index = name.indexOf('.');
				if (index > 0) {
				    String protocol = name.substring(0, index);
				    if (appendOnlySet.contains(protocol)) {
					appendPathProperty(name, value);
				    } else {
					setProperty(name, value);
				    }
				}
			    }
			}

			// now enable the handler
			String value = System.getProperty(KEY);
	
			if (value == null) value = "";
			if (value.indexOf(PKG) == -1) {
			    if (value.length() != 0) {
				value = value + "|" + PKG;
			    } else {
				value = PKG;
			    }
			}
			System.setProperty(KEY, value);
			return null;
		    }
		});
    }

    /**
     * Set a property.
     * Properties are optionally used by schemes whose handlers are
     * in subpackages of org.bzdev.protocols. This method is used to
     * set the value of one of these properties.  A property set
     * by this method is independent of ones set by 
     * java.lang.System.setProperty.
     *
     * @param key the key for a property
     * @param value the value for a property
     */
    private static void setProperty(String key, String value)
    {
	if (value == null) map.remove(key);
	map.put(key, value);
    }

    /**
     * Get a property.
     * Properties are optionally used by schemes whose handlers are in
     * subpackages of org.bzdev.protocols. This method is used to retrieve 
     * the value of one of these properties. A property obtained 
     * by this method is independent of one with the same key obtained by 
     * calling java.lang.System.getProperty.
     * @param key the key for the property
     * @return the property stored under the key; null if there is none.
     */
    static public String getProperty(String key) {
	return (String) map.get(key);
    }

    /**
     * Append an entry to a Handler property if it does not already exist
     * The property is expected to contain a series of entries separated by
     * a "|" character.
     * @param key the key for a property
     * @param value the value to append to the property
     */

    private static void appendPathProperty(String key, String value) {
	value = value.trim();
	String old = getProperty(key);
	if (old == null) {
	    setProperty(key, value);
	    return;
	} else {
	    old = old.trim();
	    if (old.length() == 0) {
		setProperty(key, value);
		return;
	    } else {
		setProperty(key, "|" + value);
	    }
	}
    }
}

//  LocalWords:  classpath sresource getSystemClassLoader getResource
//  LocalWords:   blockquote pre SecurityException
