package org.bzdev.net;
import java.net.URLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Permission;
import java.net.URL;
import java.util.Map;
import java.util.List;

/**
 * Base class for encapsulating URL connections.
 * This class stores a URL connection and delegates all the
 * standard methods for a URL connection to the stored URL
 * connection.
 * <P>
 * Subclasses will typically override a few methods, perhaps
 * wrapping them in a doPrivileged block or merely tracing some
 * methods by printing their arguments on an output stream for
 * debugging purposes.
 */
abstract public class EncapURLConnection extends URLConnection {
    private URLConnection urlc;

    /**
     * Constructor.
     * @param urlc the URL connection to encapsulate
     */
    protected EncapURLConnection(URLConnection urlc) {
	super(urlc.getURL());
	this.urlc = urlc;
    }

    /**
     * Get the encapsulated URL Connection
     * @return the URLConnection passed to the constructor.
     */
    public URLConnection getEncapsulatedURLConnection() {
	return urlc;
    }

    @Override
    public void connect() throws IOException {
	urlc.connect();
    }

    @Override
    public void setConnectTimeout(int timeout) {
	urlc.setConnectTimeout(timeout);
    }

    @Override
    public int getConnectTimeout() {
	return urlc.getConnectTimeout();
    }

    @Override
    public void setReadTimeout(int timeout) {
	urlc.setReadTimeout(timeout);
    }

    @Override
    public int getReadTimeout() {
	return urlc.getReadTimeout();
    }

    @Override
    public URL getURL() {
	return urlc.getURL();
    }

    @Override
    public int getContentLength() {
	return urlc.getContentLength();
    }

    @Override
    public long getContentLengthLong() {
	return urlc.getContentLengthLong();
    }

    @Override
    public String getContentType() {
	return urlc.getContentType();
    }

    @Override
    public String getContentEncoding() {
	return urlc.getContentEncoding();
    }

    @Override
    public long getExpiration() {
	return urlc.getExpiration();
    }

    @Override
    public long getLastModified() {
	return urlc.getLastModified();
    }

    @Override
    public String getHeaderField(String name) {
	return urlc.getHeaderField(name);
    }

    @Override
    public Map<String,List<String>> getHeaderFields() {
	return urlc.getHeaderFields();
    }

    @Override
    public int getHeaderFieldInt(String name, int Default) {
	return urlc.getHeaderFieldInt(name, Default);
    }

    @Override
    public long getHeaderFieldLong(String name, long Default) {
	return urlc.getHeaderFieldLong(name, Default);
    }
	
    @Override
    public long getHeaderFieldDate(String name, long Default) {
	return urlc.getHeaderFieldDate(name, Default);
    }

    @Override
    public String getHeaderFieldKey(int n) {
	return urlc.getHeaderFieldKey(n);
    }

    @Override
    public String getHeaderField(int n) {
	return urlc.getHeaderField(n);
    }

    @Override
    public Object getContent() throws IOException {
	return urlc.getContent();
    }

    @Override
    public Object getContent(Class[] classes) throws IOException {
	return urlc.getContent(classes);
    }

    @Override
    public Permission getPermission() throws IOException {
	return urlc.getPermission();
    }


    @Override
    public InputStream getInputStream() throws IOException {
	return urlc.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
	return urlc.getOutputStream();
    }

    @Override
    public String toString() {
	return urlc.toString();
    }


    @Override
    public void setDoInput(boolean doinput) {
	urlc.setDoInput(doinput);
    }


    @Override
    public boolean getDoInput() {
	return urlc.getDoInput();
    }
	
    @Override
    public void setDoOutput(boolean dooutput) {
	urlc.setDoOutput(dooutput);
    }

    @Override
    public boolean getDoOutput() {
	return urlc.getDoOutput();
    }

    @Override
    public void setAllowUserInteraction(boolean allowuserinteraction) {
	urlc.setAllowUserInteraction(allowuserinteraction);
    }

    @Override
    public boolean getAllowUserInteraction() {
	return urlc.getAllowUserInteraction();
    }

    @Override
    public void setUseCaches(boolean usecaches)  {
	urlc.setUseCaches(usecaches);
    }

    @Override
    public boolean getUseCaches() {
	return urlc.getUseCaches();
    }

    @Override
    public void setIfModifiedSince(long ifmodifiedsince) {
	urlc.setIfModifiedSince(ifmodifiedsince);
    }

    @Override
    public long getIfModifiedSince() {
	return urlc.getIfModifiedSince();
    }

    @Override
    public boolean getDefaultUseCaches() {
	return urlc.getDefaultUseCaches();
    }

    @Override
    public void setDefaultUseCaches(boolean defaultusecaches) {
	urlc.setDefaultUseCaches(defaultusecaches);
    }

    @Override
    public void setRequestProperty(String key, String value) {
	urlc.setRequestProperty(key, value);
    }


    @Override
    public void addRequestProperty(String key, String value) {
	urlc.addRequestProperty(key, value);
    }

    @Override
    public String getRequestProperty(String key) {
	return urlc.getRequestProperty(key);
    }

    @Override
    public Map<String,List<String>> getRequestProperties() {
	return urlc.getRequestProperties();
    }
}
