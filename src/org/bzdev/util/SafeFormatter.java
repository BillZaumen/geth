package org.bzdev.util;
import java.util.*;
import java.util.regex.*;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.FileNotFoundException;
import java.io.Closeable;
import java.io.Flushable;


/**
 * An interpreter for printf-style format strings that will replace
 * format directives with ones that accept strings if there is a failure.
 * This class is identical 
 * {@link java.util.Formatter java.util.Formatter} in terms of the
 * methods and constructors provided.  It is intended to provide fail-safe
 * text formating for purposes such as creating localized exception messages.
 * In these cases, a poorly formatted message is preferrable to throwing an
 * exception during exception handling.
 * <P>
 * The method and constructor documentation has been copied verbatim from
 * the Open JDK 1.7 documentation as we cannot use inheritance to find it.
 * @see java.util.Formatter
 */
public final class SafeFormatter {
    Formatter formatter;
    Formatter sformatter;
    StringBuffer sb = new StringBuffer();
    
    /**
     * Constructs a new formatter.
     *
     * <p> The destination of the formatted output is a {@link StringBuilder}
     * which may be retrieved by invoking {@link #out out()} and whose
     * current content may be converted into a string by invoking {@link
     * #toString toString()}.  The locale used is the {@linkplain
     * java.util.Locale#getDefault() default locale} for this instance of the Java
     * virtual machine.
     */
    public SafeFormatter() {
	formatter = new Formatter();
	sformatter = new Formatter(sb);
    }
   /**
     * Constructs a new formatter with the specified destination.
     *
     * <p> The locale used is the {@linkplain java.util.Locale#getDefault() default
     * locale} for this instance of the Java virtual machine.
     *
     * @param  a
     *         Destination for the formatted output.  If {@code a} is
     *         {@code null} then a {@link StringBuilder} will be created.
     */
    public SafeFormatter(Appendable a) {
	formatter = new Formatter(a);
	sformatter = new Formatter(sb);
    }
    /**
     * Constructs a new formatter with the specified destination and locale.
     *
     * @param  a
     *         Destination for the formatted output.  If {@code a} is
     *         {@code null} then a {@link StringBuilder} will be created.
     *
     * @param  l
     *         The {@linkplain java.util.Locale locale} to apply during
     *         formatting.  If {@code l} is {@code null} then no localization
     *         is applied.
     */
    public SafeFormatter(Appendable a, Locale l) {
	formatter = new Formatter(a, l);
	sformatter = new Formatter(sb, l);
    }

    /**
     * Constructs a new formatter with the specified file.
     *
     * <p> The charset used is the {@linkplain
     * java.nio.charset.Charset#defaultCharset() default charset} for this
     * instance of the Java virtual machine.
     *
     * <p> The locale used is the {@linkplain java.util.Locale#getDefault() default
     * locale} for this instance of the Java virtual machine.
     *
     * @param  file
     *         The file to use as the destination of this formatter.  If the
     *         file exists then it will be truncated to zero size; otherwise,
     *         a new file will be created.  The output will be written to the
     *         file and is buffered.
     *
     * @throws  SecurityException
     *          If a security manager is present and {@link
     *          SecurityManager#checkWrite checkWrite(file.getPath())} denies
     *          write access to the file
     *
     * @throws  FileNotFoundException
     *          If the given file object does not denote an existing, writable
     *          regular file and a new regular file of that name cannot be
     *          created, or if some other error occurs while opening or
     *          creating the file
     */
    public SafeFormatter(File file)  throws FileNotFoundException {
	formatter = new Formatter(file);
	sformatter = new Formatter(sb);
    }

    /**
     * Constructs a new formatter with the specified file and charset.
     *
     * <p> The locale used is the {@linkplain java.util.Locale#getDefault default
     * locale} for this instance of the Java virtual machine.
     *
     * @param  file
     *         The file to use as the destination of this formatter.  If the
     *         file exists then it will be truncated to zero size; otherwise,
     *         a new file will be created.  The output will be written to the
     *         file and is buffered.
     *
     * @param  csn
     *         The name of a supported {@linkplain java.nio.charset.Charset
     *         charset}
     *
     * @throws  FileNotFoundException
     *          If the given file object does not denote an existing, writable
     *          regular file and a new regular file of that name cannot be
     *          created, or if some other error occurs while opening or
     *          creating the file
     *
     * @throws  SecurityException
     *          If a security manager is present and {@link
     *          SecurityManager#checkWrite checkWrite(file.getPath())} denies
     *          write access to the file
     *
     * @throws  UnsupportedEncodingException
     *          If the named charset is not supported
     */
    public SafeFormatter(File file, String csn)
	throws FileNotFoundException, UnsupportedEncodingException
    {
	formatter = new Formatter(file, csn);
	sformatter = new Formatter(sb);
    }

    /**
     * Constructs a new formatter with the specified file, charset, and
     * locale.
     *
     * @param  file
     *         The file to use as the destination of this formatter.  If the
     *         file exists then it will be truncated to zero size; otherwise,
     *         a new file will be created.  The output will be written to the
     *         file and is buffered.
     *
     * @param  csn
     *         The name of a supported {@linkplain java.nio.charset.Charset
     *         charset}
     *
     * @param  l
     *         The {@linkplain java.util.Locale locale} to apply during
     *         formatting.  If {@code l} is {@code null} then no localization
     *         is applied.
     *
     * @throws  FileNotFoundException
     *          If the given file object does not denote an existing, writable
     *          regular file and a new regular file of that name cannot be
     *          created, or if some other error occurs while opening or
     *          creating the file
     *
     * @throws  SecurityException
     *          If a security manager is present and {@link
     *          SecurityManager#checkWrite checkWrite(file.getPath())} denies
     *          write access to the file
     *
     * @throws  UnsupportedEncodingException
     *          If the named charset is not supported
     */
    public SafeFormatter(File file, String csn, Locale l)
	throws FileNotFoundException, UnsupportedEncodingException
    {
	formatter = new Formatter(file, csn, l);
	sformatter = new Formatter(sb, l);
    }

    /**
     * Constructs a new formatter with the specified locale.
     *
     * <p> The destination of the formatted output is a {@link StringBuilder}
     * which may be retrieved by invoking {@link #out out()} and whose current
     * content may be converted into a string by invoking {@link #toString
     * toString()}.
     *
     * @param  l
     *         The {@linkplain java.util.Locale locale} to apply during
     *         formatting.  If {@code l} is {@code null} then no localization
     *         is applied.
     */
    public SafeFormatter(Locale l) {
	formatter = new Formatter(l);
	sformatter = new Formatter(sb, l);
    }

    /**
     * Constructs a new formatter with the specified output stream.
     *
     * <p> The charset used is the {@linkplain
     * java.nio.charset.Charset#defaultCharset() default charset} for this
     * instance of the Java virtual machine.
     *
     * <p> The locale used is the {@linkplain java.util.Locale#getDefault() default
     * locale} for this instance of the Java virtual machine.
     *
     * @param  os
     *         The output stream to use as the destination of this formatter.
     *         The output will be buffered.
     */
    public SafeFormatter(OutputStream os) {
	formatter = new Formatter(os);
	sformatter = new Formatter(sb);
    }

    /**
     * Constructs a new formatter with the specified output stream and
     * charset.
     *
     * <p> The locale used is the {@linkplain java.util.Locale#getDefault default
     * locale} for this instance of the Java virtual machine.
     *
     * @param  os
     *         The output stream to use as the destination of this formatter.
     *         The output will be buffered.
     *
     * @param  csn
     *         The name of a supported {@linkplain java.nio.charset.Charset
     *         charset}
     *
     * @throws  UnsupportedEncodingException
     *          If the named charset is not supported
     */
    public SafeFormatter(OutputStream os, String csn)
	throws UnsupportedEncodingException
    {
	formatter = new Formatter(os, csn);
	sformatter = new Formatter(sb);
    }

    /**
     * Constructs a new formatter with the specified output stream, charset,
     * and locale.
     *
     * @param  os
     *         The output stream to use as the destination of this formatter.
     *         The output will be buffered.
     *
     * @param  csn
     *         The name of a supported {@linkplain java.nio.charset.Charset
     *         charset}
     *
     * @param  l
     *         The {@linkplain java.util.Locale locale} to apply during
     *         formatting.  If {@code l} is {@code null} then no localization
     *         is applied.
     *
     * @throws  UnsupportedEncodingException
     *          If the named charset is not supported
     */
    public SafeFormatter(OutputStream os, String csn, Locale l)
	throws UnsupportedEncodingException
    {
	formatter = new Formatter(os, csn, l);
	sformatter = new Formatter(sb, l);
    }

    /**
     * Constructs a new formatter with the specified print stream.
     *
     * <p> The locale used is the {@linkplain java.util.Locale#getDefault() default
     * locale} for this instance of the Java virtual machine.
     *
     * <p> Characters are written to the given {@link java.io.PrintStream
     * PrintStream} object and are therefore encoded using that object's
     * charset.
     *
     * @param  ps
     *         The stream to use as the destination of this formatter.
     */
    public SafeFormatter(PrintStream ps) {
	formatter = new Formatter(ps);
	sformatter = new Formatter(sb);
    }

    /**
     * Constructs a new formatter with the specified file name.
     *
     * <p> The charset used is the {@linkplain
     * java.nio.charset.Charset#defaultCharset() default charset} for this
     * instance of the Java virtual machine.
     *
     * <p> The locale used is the {@linkplain java.util.Locale#getDefault() default
     * locale} for this instance of the Java virtual machine.
     *
     * @param  fileName
     *         The name of the file to use as the destination of this
     *         formatter.  If the file exists then it will be truncated to
     *         zero size; otherwise, a new file will be created.  The output
     *         will be written to the file and is buffered.
     *
     * @throws  SecurityException
     *          If a security manager is present and {@link
     *          SecurityManager#checkWrite checkWrite(fileName)} denies write
     *          access to the file
     *
     * @throws  FileNotFoundException
     *          If the given file name does not denote an existing, writable
     *          regular file and a new regular file of that name cannot be
     *          created, or if some other error occurs while opening or
     *          creating the file
     */
    public SafeFormatter(String fileName) throws FileNotFoundException {
	formatter = new Formatter(fileName);
	sformatter = new Formatter(sb);
    }

    /**
     * Constructs a new formatter with the specified file name and charset.
     *
     * <p> The locale used is the {@linkplain java.util.Locale#getDefault default
     * locale} for this instance of the Java virtual machine.
     *
     * @param  fileName
     *         The name of the file to use as the destination of this
     *         formatter.  If the file exists then it will be truncated to
     *         zero size; otherwise, a new file will be created.  The output
     *         will be written to the file and is buffered.
     *
     * @param  csn
     *         The name of a supported {@linkplain java.nio.charset.Charset
     *         charset}
     *
     * @throws  FileNotFoundException
     *          If the given file name does not denote an existing, writable
     *          regular file and a new regular file of that name cannot be
     *          created, or if some other error occurs while opening or
     *          creating the file
     *
     * @throws  SecurityException
     *          If a security manager is present and {@link
     *          SecurityManager#checkWrite checkWrite(fileName)} denies write
     *          access to the file
     *
     * @throws  UnsupportedEncodingException
     *          If the named charset is not supported
     */
    public SafeFormatter(String fileName, String csn)
	throws FileNotFoundException, UnsupportedEncodingException
    {
	formatter = new Formatter(fileName, csn);
	sformatter = new Formatter(sb);
    }

    /**
     * Constructs a new formatter with the specified file name, charset, and
     * locale.
     *
     * @param  fileName
     *         The name of the file to use as the destination of this
     *         formatter.  If the file exists then it will be truncated to
     *         zero size; otherwise, a new file will be created.  The output
     *         will be written to the file and is buffered.
     *
     * @param  csn
     *         The name of a supported {@linkplain java.nio.charset.Charset
     *         charset}
     *
     * @param  l
     *         The {@linkplain java.util.Locale locale} to apply during
     *         formatting.  If {@code l} is {@code null} then no localization
     *         is applied.
     *
     * @throws  FileNotFoundException
     *          If the given file name does not denote an existing, writable
     *          regular file and a new regular file of that name cannot be
     *          created, or if some other error occurs while opening or
     *          creating the file
     *
     * @throws  SecurityException
     *          If a security manager is present and {@link
     *          SecurityManager#checkWrite checkWrite(fileName)} denies write
     *          access to the file
     *
     * @throws  UnsupportedEncodingException
     *          If the named charset is not supported
     */
    public SafeFormatter(String fileName, String csn, Locale l)
	throws FileNotFoundException, UnsupportedEncodingException
    {
	formatter = new Formatter(fileName, csn, l);
	sformatter = new Formatter(sb, l);
    }

    /**
     * Closes this formatter.  If the destination implements the {@link
     * java.io.Closeable} interface, its {@code close} method will be invoked.
     *
     * <p> Closing a formatter allows it to release resources it may be holding
     * (such as open files).  If the formatter is already closed, then invoking
     * this method has no effect.
     *
     * <p> Attempting to invoke any methods except {@link #ioException()} in
     * this formatter after it has been closed will result in a {@link
     * java.util.FormatterClosedException}.
     */
    public void close() {
	formatter.close();
    }

    /**
     * Flushes this formatter.  If the destination implements the {@link
     * java.io.Flushable} interface, its {@code flush} method will be invoked.
     *
     * <p> Flushing a formatter writes any buffered output in the destination
     * to the underlying stream.
     *
     * @throws  FormatterClosedException
     *          If this formatter has been closed by invoking its {@link
     *          #close()} method
     */
    public void flush() {
	formatter.flush();
    }
    
    /**
     * Returns the locale set by the construction of this formatter.
     *
     * <p> The {@link #format(java.util.Locale,String,Object...) format} method
     * for this object which has a locale argument does not change this value.
     *
     * @return  {@code null} if no localization is applied, otherwise a
     *          locale
     *
     * @throws  FormatterClosedException
     *          If this formatter has been closed by invoking its {@link
     *          #close()} method
     */
    public Locale locale() {
	return formatter.locale();
    }
    /**
     * The regular expression used by SafeFormater to match
     * formatting directives provided by {@link java.util.Formatter}. 
     */
    public static final String pattern = "((%%)+)|(((%%)*%)"
	+ "([1-9][0-9]*[$])?([#+ O,(<-]*)([0-9.])*([bBhHsScCdoxXeEfgGaA]|"
	+ "([tT][HIklMSLNpzZsQBbhAaCYyjmdeRTrDFc]?)|n))";
    static final Pattern p = Pattern.compile(pattern);

    /**
     * Get a modified format that accepts any type of argument
     * @param format the original format
     * @return the modified format
     */
    public static String modify(String format) {
	Matcher m = p.matcher(format);
	StringBuffer sb = new StringBuffer(format.length());
	while (m.find()) {
	    String group = m.group(1);
	    if(group != null && group.length() != 0) {
		m.appendReplacement(sb, "$1");
	    } else {
		if (m.group(7).contains("<")) {
		    m.appendReplacement(sb,"$4<s");
		} else {
		    m.appendReplacement(sb, "$4$6s");
		}
	    }
	}
	m.appendTail(sb);
	return sb.toString();
    }

    public static int getDirectiveCount(String format) {
	int count = 0;
	Matcher m = p.matcher(format);
	while (m.find()) {
	    String full = m.group(0);
	    String group = m.group(1);
	    if (!full.endsWith("n") && (group == null|| group.length() == 0)) {
		count++;
	    }
	}
	return count;
    }

    /**
     * Writes a formatted string to this object's destination using the
     * specified locale, format string, and arguments.  If an error
     * occurs, a separate attempt is made by making all formatting
     * directives that take arguments "%s" directives, with positional
     * parameters maintained.  For example, after a failure, the directive
     * "%3$8.3g" will be replaced with "%3$s" for the second attempt.
     *
     * @param  l
     *         The {@linkplain java.util.Locale locale} to apply during
     *         formatting.  If {@code l} is {@code null} then no localization
     *         is applied.  This does not change this object's locale that was
     *         set during construction.
     *
     * @param  format
     *         A format string as described in 
     *         {@link java.util.Formatter Formatter}
     *
     * @param  args
     *         Arguments referenced by the format specifiers in the format
     *         string.  If there are more arguments than format specifiers, the
     *         extra arguments are ignored.  The maximum number of arguments is
     *         limited by the maximum dimension of a Java array as defined by
     *         <cite>The Java&trade; Virtual Machine Specification</cite>.
     *
     * @throws  IllegalFormatException
     *          If a format string contains an illegal syntax, a format
     *          specifier that is incompatible with the given arguments,
     *          insufficient arguments given the format string, or other
     *          illegal conditions.  For specification of all possible
     *          formatting errors, see the {@link java.util.Formatter Formatter}
     *          section of the formatter class specification.
     *
     * @throws  FormatterClosedException
     *          If this formatter has been closed by invoking its {@link
     *          #close()} method
     *
     * @return  This formatter
     */
    public SafeFormatter format(Locale l, String format, Object... args)
	throws IllegalFormatException, FormatterClosedException
    {
	try {
	    sb.setLength(0);
	    sformatter.format(l, format, args);
	    formatter.format("%s", sb.toString());
	    if (sb.length() > 128) {
		sb.setLength(128);
		sb.trimToSize();
	    }
	    sb.setLength(0);
	} catch (IllegalFormatException e) {
	    format = modify(format);
	    formatter.format(l, format, args);
	}
	return this;
    }

    /**
     * Writes a formatted string to this object's destination using the
     * specified format string and arguments.  The locale used is the one
     * defined during the construction of this formatter.
     * <P>
     * If an error occurs, a separate attempt is made by making all
     * formatting directives that take arguments "%s" directives, with
     * positional parameters maintained.  For example, after a
     * failure, the directive "%3$8.3g" will be replaced with "%3$s"
     * for the second attempt.
     * @param  format
     *         A format string as described in {@link java.util.Formatter Formatter}
     *
     * @param  args
     *         Arguments referenced by the format specifiers in the format
     *         string.  If there are more arguments than format specifiers, the
     *         extra arguments are ignored.  The maximum number of arguments is
     *         limited by the maximum dimension of a Java array as defined by
     *         <cite>The Java&trade; Virtual Machine Specification</cite>.
     *
     * @throws  IllegalFormatException
     *          If a format string contains an illegal syntax, a format
     *          specifier that is incompatible with the given arguments,
     *          insufficient arguments given the format string, or other
     *          illegal conditions.  For specification of all possible
     *          formatting errors, see the {@link java.util.Formatter Formatter}
     *          section of the formatter class specification.
     *
     * @throws  FormatterClosedException
     *          If this formatter has been closed by invoking its {@link
     *          #close()} method
     *
     * @return  This formatter
     */
    public SafeFormatter format(String format, Object... args)
	throws IllegalFormatException, FormatterClosedException
    {
	return format(formatter.locale(), format, args);
    }

    /**
     * Returns the {@code IOException} last thrown by this formatter's {@link
     * Appendable}.
     *
     * <p> If the destination's {@code append()} method never throws
     * {@code IOException}, then this method will always return {@code null}.
     *
     * @return  The last exception thrown by the Appendable or {@code null} if
     *          no such exception exists.
     */
    public IOException ioException() {
	return formatter.ioException();
    }

    /**
     * Returns the destination for the output.
     *
     * @return  The destination for the output
     *
     * @throws  FormatterClosedException
     *          If this formatter has been closed by invoking its {@link
     *          #close()} method
     */
    public Appendable out() {
	return formatter.out();
    }


    /**
     * Returns the result of invoking {@code toString()} on the destination
     * for the output.  For example, the following code formats text into a
     * {@link StringBuilder} (created by Formatter's zero-argument
     * constructor) and then retrieves the resultant string:
     *
     * <blockquote><pre>
     *   Formatter f = new Formatter();
     *   f.format("Last reboot at %tc", lastRebootDate);
     *   String s = f.toString();
     *   // -&gt; s == "Last reboot at Sat Jan 01 00:00:00 PST 2000"
     * </pre></blockquote>
     *
     * <p> An invocation of this method behaves in exactly the same way as the
     * invocation
     *
     * <pre>
     *     out().toString() </pre>
     *
     * <p> Depending on the specification of {@code toString} for the {@link
     * Appendable}, the returned string may or may not contain the characters
     * written to the destination.  For instance, buffers typically return
     * their contents in {@code toString()}, but streams cannot since the
     * data is discarded.
     *
     * @return  The result of invoking {@code toString()} on the destination
     *          for the output
     *
     * @throws  FormatterClosedException
     *          If this formatter has been closed by invoking its {@link
     *          #close()} method
     */
    public String toString() {
	return formatter.toString();
    }
    

}


