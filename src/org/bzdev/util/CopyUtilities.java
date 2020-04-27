package org.bzdev.util;
import java.io.*;
import java.net.*;
import java.util.zip.*;
import java.nio.charset.Charset;
import java.nio.CharBuffer;
import java.util.ArrayList;

//@exbundle org.bzdev.util.lpack.Util

/**
 * Utility class for various copy operations: file to file, resource to file,
 * object referenced by a URL to a file, resource to output stream, file to
 * output stream, URL to output stream, input stream to output stream,
 * resource to a ZIP stream.
 * <P>
 * This class will also copy array lists whose type parameters are 
 * Integer, Long, Short, Character, Byte, Float, and Double to newly
 * allocated arrays with the corresponding primitive types.
 * <P>
 * All the methods are static.
 */
public class CopyUtilities {

    static String errorMsg(String key, Object... args) {
	return UtilErrorMsg.errorMsg(key, args);
    }

    /**
     * Copy a resource to a file.
     * The resource is accessed via the system class loader.
     * @param resource the resource
     * @param outputFile the output file
     * @exception IOException an IO error occurred
     */
    public static void copyResourceToFile(String resource, File outputFile)
	throws IOException
    {
	OutputStream os = new FileOutputStream(outputFile);
	copyResourceToStream(resource, os);
    }

    /**
     * Copy a resource to an output stream.
     * The resource is accessed via the system class loader.
     * @param resource the resource
     * @param target the output stream
     * @exception IOException an IO error occurred
     */
    public static void copyResourceToStream(String resource,
					    OutputStream target) 
	throws IOException
    {
	InputStream is = null;
	try {
	    is = ClassLoader.getSystemResourceAsStream(resource);
	} catch (Exception  e) {}
	if (is == null) {
	    String r = resource;
	    if (!r.startsWith("/")) r = "/" + r;
	    is = CopyUtilities.class.getResourceAsStream(r);
	}
	if (is == null) {
	    throw new IOException(errorMsg("noResource", resource));
	}
	byte[] buffer = new byte[4096];
	int len;
	while ((len = is.read(buffer)) != -1) {
	    target.write(buffer, 0, len);
	}
	target.flush();
    }


    /**
     * Copy a resource containing character data to an Appendable
     * The resource is accessed via the system class loader.
     * @param resource the resource
     * @param a is the object used to store the copy
     * @param charset the character encoding used by the resource
     * @exception IOException an IO error occurred
     * @exception NullPointerException an argument was null
     */
    public static void copyResource(String resource, Appendable a,
				    Charset charset)
	throws IOException, NullPointerException
    {
	if (resource == null || a == null || charset == null) {
	    throw new NullPointerException("nullArgument");
	}
	InputStream is = null;
	try {
	    is = ClassLoader.getSystemResourceAsStream(resource);
	} catch (Exception e) {}
	if (is == null)  {
	    String r = resource;
	    if (!r.startsWith("/")) r = "/" + r;
	    is = CopyUtilities.class.getResourceAsStream(r);
	}
	if (is == null) {
	    throw new IOException(errorMsg("missingResource", resource));
	}
	copyStream(is, a, charset);
    }

    /**
     * Copy a file to a file.
     * @param iFile the input file
     * @param oFile the output file
     * @exception IOException an IO error occurred
     */
    public static void copyFile(File iFile, File oFile) throws IOException {
	FileInputStream is = new FileInputStream(iFile);
	FileOutputStream os = new FileOutputStream(oFile);
	copyStream(is, os);
	is.close();
	os.close();
    }

    /**
     * Copy a file to an output stream.
     * @param iFile the input file
     * @param os the output stream
     * @exception IOException an IO error occurred
     */
    public static void copyFile(File iFile,
				OutputStream os) 
	throws IOException
    {
	FileInputStream is = new FileInputStream(iFile);
	copyStream(is, os);
	is.close();
    }

    /**
     * Copy a network resource to an output stream 
     * @param url the resource's URL
     * @param os the output stream
     * @exception IOException an IO error occurred
     */
    public static void copyURL(URL url, OutputStream os) throws IOException {
	InputStream is = url.openStream();
	copyStream(is, os);
	is.close();
    }

    /**
     * Copy a network resource to a file 
     * @param url the resource's URL
     * @param oFile the output file
     * @exception IOException an IO error occurred
     */
    public static void copyURL(URL url, File oFile) throws IOException {
	InputStream is = url.openStream();
	OutputStream os = new FileOutputStream(oFile);
	copyStream(is, os);
	is.close();
	os.close();
    }

    /**
     * Copy a resource referenced by a URL and  containing character
     * data to an Appendable.
     * @param url the resource's URL
     * @param a is the object used to store the copy
     * @param charset the character encoding used by the resource
     * @exception IOException an IO error occurred
     * @exception NullPointerException an argument was null
     */
    public static void copyURL(URL url, Appendable a,
			       Charset charset)
	throws IOException, NullPointerException
    {
	if (url == null || a == null || charset == null) {
	    throw new NullPointerException(errorMsg("nullArgument"));
	}
	InputStream is = url.openStream();
	if (is == null) {
	    String msg = errorMsg("missingResource", url.toString());
	    throw new IOException(msg);
	}
	copyStream(is, a, charset);
    }


    /**
     * Copy an input stream to an output stream
     * @param is the input stream
     * @param os the output stream
     * @exception IOException an IO error occurred
     */
    public static void copyStream(InputStream is, OutputStream os) 
	throws IOException 
    {
	byte[] buffer = new byte[4096];
	int len = 0;
	while ((len = is.read(buffer)) != -1) {
	    os.write(buffer, 0, len);
	}
	os.flush();
    }

    /**
     * Copy an input stream to an Appendable.
     * The input stream is copied from its current position to its
     * end.
     * @param is the input stream
     * @param a is the object used to store the copy
     * @param charset the character encoding used by the input stream.
     */
    public static void copyStream(InputStream is, Appendable a,
				  Charset charset)
	throws IOException
    {
	int sz = 4096;
	char[] buffer = new char[sz];
	CharBuffer cbuf = CharBuffer.wrap(buffer);
	int len = 0;
	long total = 0;

	InputStreamReader r = new InputStreamReader(is, charset);

	while ((len = r.read(buffer, 0, sz)) != -1) {
	    cbuf.position(0);
	    cbuf.limit(len);
	    a.append(cbuf, 0, len);
	    total += len;
	}
	if (a instanceof Flushable) {
	    ((Flushable) a).flush();
	}
	// System.out.println("total bytes sent = " + total);
    }


    /**
     * Copy a resource to a zip output stream.
     * @param resource the resource
     * @param zipEntryName the name of the zip-file entry under which to
     *        store the resource
     * @param zos the zip output stream
     * @param stored true if stored as is; false if compressed
     * @exception IOException an IO error occurred
     */
    public static void copyResourceToZipStream(String resource,
					       String zipEntryName,
					       ZipOutputStream zos,
					       boolean stored)
	throws IOException
    {
	ZipEntry ze = new ZipEntry(zipEntryName);
	if (stored) {
	    zos.setLevel(0);
	    zos.setMethod(ZipOutputStream.STORED);
	    ByteArrayOutputStream bos =
		new ByteArrayOutputStream(2<<16);
	    copyResourceToStream(resource, bos);
	    int sz = bos.size();
	    byte[] array = bos.toByteArray();
	    CRC32 crc = new CRC32();
	    crc.update(array);
	    ze.setSize(sz);
	    ze.setCompressedSize(sz);
	    ze.setCrc(crc.getValue());
	    zos.putNextEntry(ze);
	    zos.write(array, 0, sz);
	    zos.closeEntry();
	} else {
	    zos.setMethod(ZipOutputStream.DEFLATED);
	    zos.setLevel(9);
	    zos.putNextEntry(ze);
	    copyResourceToStream(resource, zos);
	    zos.closeEntry();
	}
    }

    /**
     * Copy an array list to an array of double.
     * One of ArrayList's toArray methods allows one to copy an
     * ArrayList to an array, but the type of that array cannot
     * be a primitive type.  The ArrayList must not be modified
     * while this method is executing.
     * @param list the array list to copy
     * @return an array containing the elements of the list
     */

    public static double[] toDoubleArray(ArrayList<Double> list) 
    {
	double[] result = new double[list.size()];
	int i = 0;
	for (Double d: list) {
	    result[i++] = d;
	}
	return result;
    }

    /**
     * Copy an array list to an array of double with a specified range.
     * One of ArrayList's toArray methods allows one to copy an
     * ArrayList to an array, but the type of that array cannot
     * be a primitive type.  The ArrayList must not be modified
     * while this method is executing.
     * @param list the array list to copy
     * @param start the starting index to copy
     * @param  end the index just past the last element to copy
     * @return an array containing the elements of the list
     */

    public static double[] toDoubleArray(ArrayList<Double> list,
					 int start, int end) 
    {
	int size = list.size();
	if (size < end || start >= size || start < 0) {
	    throw new IllegalArgumentException(errorMsg("argsOutOfRange"));
	}
	size = end - start;
	double[] result = new double[size];
	
	int i = 0;
	for (Double d: list) {
	    if (start-- > 0) continue;
	    if (i < size) {
		result[i++] = d;
	    } else {
		break;
	    }
	}
	return result;
    }


    /**
     * Copy an array list to an array of float.b
     * One of ArrayList's toArray methods allows one to copy an
     * ArrayList to an array, but the type of that array cannot
     * be a primitive type.  The ArrayList must not be modified
     * while this method is executing.
     * @param list the array list to copy
     * @return an array containing the elements of the list
     */

    public static float[] toFloatArray(ArrayList<Float> list) 
    {
	float[] result = new float[list.size()];
	int i = 0;
	for (Float f: list) {
	    result[i++] = f;
	}
	return result;
    }

    /**
     * Copy an array list to an array of float with a specified range.
     * One of ArrayList's toArray methods allows one to copy an
     * ArrayList to an array, but the type of that array cannot
     * be a primitive type.  The ArrayList must not be modified
     * while this method is executing.
     * @param list the array list to copy
     * int start the starting offset
     * int end the index just past the last element to copy
     * @return an array containing the elements of the list
     */
    public static float[] toFloatArray(ArrayList<Float> list,
				       int start, int end) 
    {
	int size = list.size();
	if (size < end || start >= size || start < 0) {
	    throw new IllegalArgumentException(errorMsg("argsOutOfRange"));
	}
	size = end - start;

	float[] result = new float[size];
	int i = 0;
	for (Float f: list) {
	    if (start -- > 0) continue;
	    if (i < size) {
		result[i++] = f;
	    } else {
		break;
	    }
	}
	return result;
    }

    /**
     * Copy an array list to an array of int.
     * One of ArrayList's toArray methods allows one to copy an
     * ArrayList to an array, but the type of that array cannot
     * be a primitive type.  The ArrayList must not be modified
     * while this method is executing.
     * @param list the array list to copy
     * @return an array containing the elements of the list
     */

    public static int[] toIntArray(ArrayList<Integer> list) 
    {
	int[] result = new int[list.size()];
	int i = 0;
	for (Integer f: list) {
	    result[i++] = f;
	}
	return result;
    }

    /**
     * Copy an array list to an array of int.
     * One of ArrayList's toArray methods allows one to copy an
     * ArrayList to an array, but the type of that array cannot
     * be a primitive type.  The ArrayList must not be modified
     * while this method is executing.
     * @param list the array list to copy
     * @param start the starting index to copy
     * @param  end the index just past the last element to copy
     * @return an array containing the elements of the list
     */

    public static int[] toIntArray(ArrayList<Integer> list,
				   int start, int end) 
    {
	int size = list.size();
	if (size < end || start >= size || start < 0) {
	    throw new IllegalArgumentException(errorMsg("argsOutOfRange"));
	}
	size = end - start;
	int[] result = new int[size];
	int i = 0;
	for (Integer f: list) {
	    if (start-- > 0) continue;
	    if (i < size) {
		result[i++] = f;
	    } else {
		break;
	    }
	}
	return result;
    }




    /**
     * Copy an array list to an array of long.
     * One of ArrayList's toArray methods allows one to copy an
     * ArrayList to an array, but the type of that array cannot
     * be a primitive type.  The ArrayList must not be modified
     * while this method is executing.
     * @param list the array list to copy
     * @return an array containing the elements of the list
     */

    public static long[] toLongArray(ArrayList<Long> list) 
    {
	long[] result = new long[list.size()];
	int i = 0;
	for (Long f: list) {
	    result[i++] = f;
	}
	return result;
    }

    /**
     * Copy an array list to an array of long.
     * One of ArrayList's toArray methods allows one to copy an
     * ArrayList to an array, but the type of that array cannot
     * be a primitive type.  The ArrayList must not be modified
     * while this method is executing.
     * @param list the array list to copy
     * @param start the starting index to copy
     * @param  end the index just past the last element to copy
     * @return an array containing the elements of the list
     */

    public static long[] toLongArray(ArrayList<Long> list,
				     int start, int end) 
    {
	int size = list.size();
	if (size < end || start >= size || start < 0) {
	    throw new IllegalArgumentException(errorMsg("argsOutOfRange"));
	}
	size = end - start;
	long[] result = new long[size];
	int i = 0;
	for (Long f: list) {
	    if (start-- > 0) continue;
	    if (i < size) {
		result[i++] = f;
	    } else {
		break;
	    }
	}
	return result;
    }

    /**
     * Copy an array list to an array of short.
     * One of ArrayList's toArray methods allows one to copy an
     * ArrayList to an array, but the type of that array cannot
     * be a primitive type.  The ArrayList must not be modified
     * while this method is executing.
     * @param list the array list to copy
     * @return an array containing the elements of the list
     */

    public static short[] toShortArray(ArrayList<Short> list) 
    {
	short[] result = new short[list.size()];
	int i = 0;
	for (Short f: list) {
	    result[i++] = f;
	}
	return result;
    }

    /**
     * Copy an array list to an array of short.
     * One of ArrayList's toArray methods allows one to copy an
     * ArrayList to an array, but the type of that array cannot
     * be a primitive type.  The ArrayList must not be modified
     * while this method is executing.
     * @param list the array list to copy
     * @param start the starting index to copy
     * @param  end the index just past the last element to copy
     * @return an array containing the elements of the list
     */

    public static short[] toShortArray(ArrayList<Short> list,
				       int start, int end) 
    {
	int size = list.size();
	if (size < end || start >= size || start < 0) {
	    throw new IllegalArgumentException(errorMsg("argsOutOfRange"));
	}
	size = end - start;
	short[] result = new short[size];
	int i = 0;
	for (Short f: list) {
	    if (start-- > 0) continue;
	    if (i < size) {
		result[i++] = f;
	    } else {
		break;
	    }
	}
	return result;
    }

    /**
     * Copy an array list to an array of byte.
     * One of ArrayList's toArray methods allows one to copy an
     * ArrayList to an array, but the type of that array cannot
     * be a primitive type.  The ArrayList must not be modified
     * while this method is executing.
     * @param list the array list to copy
     * @return an array containing the elements of the list
     */

    public static byte[] toByteArray(ArrayList<Byte> list) 
    {
	byte[] result = new byte[list.size()];
	int i = 0;
	for (Byte f: list) {
	    result[i++] = f;
	}
	return result;
    }

    /**
     * Copy an array list to an array of byte.
     * One of ArrayList's toArray methods allows one to copy an
     * ArrayList to an array, but the type of that array cannot
     * be a primitive type.  The ArrayList must not be modified
     * while this method is executing.
     * @param list the array list to copy
     * @param start the starting index to copy
     * @param  end the index just past the last element to copy
     * @return an array containing the elements of the list
     */

    public static byte[] toByteArray(ArrayList<Byte> list,
				     int start, int end) 
    {
	int size = list.size();
	if (size < end || start >= size || start < 0) {
	    throw new IllegalArgumentException(errorMsg("argsOutOfRange"));
	}
	size = end - start;
	byte[] result = new byte[size];
	int i = 0;
	for (Byte f: list) {
	    if (start-- > 0) continue;
	    if (i < size) {
		result[i++] = f;
	    } else {
		break;
	    }
	}
	return result;
    }


    /**
     * Copy an array list to an array of char.
     * One of ArrayList's toArray methods allows one to copy an
     * ArrayList to an array, but the type of that array cannot
     * be a primitive type.  The ArrayList must not be modified
     * while this method is executing.
     * @param list the array list to copy
     * @return an array containing the elements of the list
     */

    public static char[] toCharArray(ArrayList<Character> list) 
    {
	char[] result = new char[list.size()];
	int i = 0;
	for (Character f: list) {
	    result[i++] = f;
	}
	return result;
    }

    /**
     * Copy an array list to an array of char specifying a range.
     * One of ArrayList's toArray methods allows one to copy an
     * ArrayList to an array, but the type of that array cannot
     * be a primitive type.  The ArrayList must not be modified
     * while this method is executing.
     * @param list the array list to copy
     * @param start the starting index to copy
     * @param  end the index just past the last element to copy
     * @return an array containing the elements of the list
     */

    public static char[] toCharArray(ArrayList<Character> list,
				     int start, int end) 
    {
	int size = list.size();
	if (size < end || start >= size || start < 0) {
	    throw new IllegalArgumentException(errorMsg("argsOutOfRange"));
	}
	size = end - start;
	char[] result = new char[size];
	int i = 0;
	for (Character f: list) {
	    if (start-- > 0) continue;
	    if (i < size) {
		result[i++] = f;
	    } else {
		break;
	    }
	}
	return result;
    }

    /**
     * Copy an array list to an array of boolean.
     * One of ArrayList's toArray methods allows one to copy an
     * ArrayList to an array, but the type of that array cannot
     * be a primitive type.  The ArrayList must not be modified
     * while this method is executing.
     * @param list the array list to copy
     * @return an array containing the elements of the list
     */

    public static boolean[] toBooleanArray(ArrayList<Boolean> list) 
    {
	boolean[] result = new boolean[list.size()];
	int i = 0;
	for (Boolean f: list) {
	    result[i++] = f;
	}
	return result;
    }

    /**
     * Copy an array list to an array of boolean.
     * One of ArrayList's toArray methods allows one to copy an
     * ArrayList to an array, but the type of that array cannot
     * be a primitive type.  The ArrayList must not be modified
     * while this method is executing.
     * @param list the array list to copy
     * @param start the starting index to copy
     * @param  end the index just past the last element to copy
     * @return an array containing the elements of the list
     */

    public static boolean[] toBooleanArray(ArrayList<Boolean> list,
					   int start, int end) 
    {
	int size = list.size();
	if (size < end || start >= size || start < 0) {
	    throw new IllegalArgumentException(errorMsg("argsOutOfRange"));
	}
	size = end - start;
	boolean[] result = new boolean[size];
	int i = 0;
	for (Boolean f: list) {
	    if (start-- > 0) continue;
	    if (i < size) {
		result[i++] = f;
	    } else {
		break;
	    }
	}
	return result;
    }
}
