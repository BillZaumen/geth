package org.bzdev.protocols;
import java.security.*;

/**
 * Permission for calling Handlers.enable().
 * If <code>Handlers.enable()</code> is called after a security
 * manager is installed, the policy file should grant the caller's
 * codebase this permission.  For example,
 * <blockquote><code><pre>
 *    grant codebase "..." {
 *      permission org.bzdev.protocols.HandlersEnablePermission
 *           "org.bzdev.protocols.enable";
 *    }
 * </pre></code></blockquote>
 */
public class HandlersEnablePermission extends BasicPermission {
    /**
     * Constructor.
     * @param name the target name ("org.bzdev.protocols.enable")
     */
    public HandlersEnablePermission(String name) {
	super(name);
    }

    /**
     * Constructor with actions.
     * This constructor is needed by the Java security implementation
     * but its second argument is ignored.
     * @param name the target name ("org.bzdev.protocols.enable")
     * @param actions a string that will be ignored
     */
    public HandlersEnablePermission(String name, String actions) {
	super(name, actions);
    }
}