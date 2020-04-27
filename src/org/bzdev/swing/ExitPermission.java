package org.bzdev.swing;
import java.security.*;
/**
 * Permission to allow a window to cause an application to
 * exit without confirming this operation with the user.
 * One target names supported by the BZDev class library are:
 * <UL>
 *    <LI> "<CODE>org.bzdev.swing.SimpleConsole</CODE>".
 *    <LI> "<CODE>org.bzdev.swing.PanelGraphics</CODE>".
 *    <LI> "<CODE>org.bzdev.swing.AnimatedPanelGraphics</CODE>".
 * <P>
 * To grant this permission, the security file should contain
 * lines such as
 * <blockquote><pre><code>
 *     grant CODEBASE {
 *        permission org.bzdev.swing.SimpleConsole.ExitPermission
 *                   "org.bzdev.swing.SimpleConsole"
 *     };
 * </code></pre></blockquote>
 * This permission is used by the {@link AnimatedPanelGraphics},
 * {@link AnimatedPanelGraphics.ExitAccessor}, {@link PanelGraphics},
 * {@link PanelGraphics}, {@link SimpleConsole} and
 * {@link SimpleConsole.ExitAccessor} classes from the BZDev class
 * library.
 */
public final class ExitPermission extends BasicPermission {

    /**
     * Constructor given a target name.
     * @param name the target name ("org.bzdev.swing.SimpleConsole")
     */
    public ExitPermission(String name) {
	super(name);
    }

    /**
     * Constructor with actions.
     * @param name the target name ("org.bzdev.swing.SimpleConsole")
     * @param actions a parameter that is ignored in this case although
     *        the constructor is needed
     */
    public ExitPermission(String name, String actions) {
	super(name, actions);
    }
}
