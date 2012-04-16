/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: UMSException.java,v 1.5 2009/01/28 05:34:51 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.ums;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ResourceBundle;

import com.sun.identity.shared.ldap.LDAPException;

/**
 * <PRE>
 * 
 * This class is the super-class for all UMS <B>checked</B> exceptions.
 * 
 * 
 * Some Exception throwing guidelines: -------------------------------------
 * 
 * <B> Checked exceptions </B> are sub-classes of java.lang.Exception; methods
 * throwing this type of exception are forced to define a throws clause in the
 * method signature and client programmers need to catch and handle the
 * exception with a try/catch block or declare the throws clause in their
 * methods. <B> Unchecked exceptions </B> are sub-classes of
 * java.lang.RuntimeException. Client programmers don't have to deal with the
 * exception using a try/catch block and the method throwing it does not have to
 * define it in its signature.
 *  - If your method encounters an abnormal condition which causes it to be
 * unable to fulfill its contract, or throw a checked or unchecked exception
 * (either UMSException or RuntimeException).
 *  - If your method discovers that a client has breached its contract, for
 * example, passing a null as a parameter where a non-null value is required,
 * throw an unchecked exception (RuntimeException).
 *  - If your method is unable to fulfill its contract and you feel client
 * programmers should consciously decide how to handle, throw checked exceptions
 * (UMSException).
 * 
 * 
 * Embedded/Nested Exceptions: --------------------------
 * 
 * An exception of type UMSException can embed any exception of type Throwable.
 * Embedded exceptions ensure traceability of errors in a multi-tiered
 * application. For example, in a simple 3- Tier model - presentation/client
 * tier, middle/domain tier and database/persistence tier - the real cause of
 * error might be lost by the time control, which is passed back from the
 * persistence tier to the client tier. To ensure tracking info, the constructor
 * UMSException(message,Throwable) should be used while throwing the exception.
 * Normally, the first object at each tier/module will have generic exceptions
 * defined, for example, LDAPException, RelationalDBException,
 * ConfigManagerException. Client programs can then invoke the #getRootCause()
 * method to get the underlying cause.
 * 
 * Exception hierarchy should be defined: -------------------------------------
 * An exception for each abnormal cause should be created. For example,
 * LDAPSearchException, LDAPArchiveException, etc. UMSException should probably
 * be thrown only by external API's. Even these should have embedded exceptions
 * from lower level tiers. For example, UMSException will have LDAPException
 * embedded in it, LDAPException will have LDAPSearchException nested, and so
 * on. Every package should define its own exception hierarchies specific to its
 * context, for example, policy-related exceptions should be defined in the
 * policy package.
 * 
 * Localizing Error Messages ------------------------- The java resource bundle
 * mechanism is used to implement localization. The ResourceSet and
 * ResourceSetManager classes are used to implement localization.
 * 
 * Steps for creating UMSException Sub-classes and messages
 * ------------------------------------------------------
 * 
 * 1. Identify the package this exception will belong to. A policy-related
 * exception, PolicyNotFoundException, should be part of the policy package.
 * 
 * 2. Each package should have its own properties file to store error messages.
 * For example policy.properties in package policy #policy.properties #
 * Resources for com.iplanet.ums.policy policy-nopolicyfound=Cannot find this
 * Policy
 * 
 * 3. Create a sub-class of UMSException and override the constructors.
 * 
 * public class PolicyNotFoundException extends UMSException { public
 * PolicyNotFoundException() { super(); } public PolicyNotFoundException(String
 * msg) { super(msg); } public PolicyNotFoundExceptin(String msg, Throwable t) {
 * super(msg,t); }
 * 
 * 
 * Throwing/Catching Exception Examples: ------------------------------------
 * 
 * 1. Throwing a non-nested Exception <B>(not recommended, use Ex. 3 below)</B>
 * UMSException ux = new UMSException("Some weird error!..."); throw ux;
 * 
 * 2. Throwing a nested Exception <B>(not recommended, use Ex. 3 below)</B> 
 * try { ....... ....... } catch (LDAPException le) { UMSException ux = new
 * UMSException("Some weird error!...", le); throw ux; }
 * 
 * 3. Throwing an Exception using the ResourceSetManager
 * 
 * ...... ...... public static final String PKG =
 * "com.iplanet.ums.policy.policy"; public static final String PREFIX =
 * "policy"; public static final String NO_POLICY_DOMAIN = "nopolicydomain";
 * public static final String POLICY_NOT_FOUND = "nopolicyfound"; ...... ......
 * if( policyDomainName == null || policyDomainName.length() == 0) { 
 * String msg = ResourceSetManager.getString( PKG, PREFIX, NO_POLICY_DOMAIN ); 
 * // RuntimeException 
 * throw new IllegalArgumentException( msg ); } ...... ......
 * if (policy not found ) { String msg = ResourceSetManager.getString( PKG,
 * PREFIX, POLICY_NOT_FOUND); // RuntimeException throw new
 * InvalidPolicyException(msg); }
 * 
 * 
 * The properties file (com/iplanet/ums/policy/policy.properties) looks like
 * this: # Resources for com.iplanet.ums.policy policy-nopolicydomain=Policy
 * Domain name cannot be null or blank policy-nopolicyfound=Cannot find this
 * Policy
 * 
 *  - Logging/Dealing with an Exception, inclunding all nested exceptions try {
 * ....... ....... } catch (UMSException ux) {
 * 
 * if (ux.getRootCause() instanceof LDAPException) { PrintWriter pw = new
 * PrintWriter(<some file stream>); ux.log(pw); } else {
 * System.out.println(ux.getMessage()); }
 *  }
 * 
 * </PRE>
 * 
 * @see #UMSException(String, Throwable)
 * @see #getRootCause()
 * @see java.lang.Exception
 * @see java.lang.RuntimeException
 * @supported.api
 */
public class UMSException extends java.lang.Exception {

    private static final long serialVersionUID = -7043204896844472780L;

    static ResourceBundle xcptMsgs = null;

    protected String xcptMessage;

    protected Throwable rootCause;

    /**
     * Constructs a UMSException with a detailed message.
     * 
     * @param message
     *            Detailed message for this exception.
     * @supported.api
     */
    public UMSException(String message) {
        super(message);
        xcptMessage = message;
    }

    /**
     * Constructs a UMSException with a message and an embedded exception.
     * 
     * @param message
     *            Detailed message for this exception.
     * @param rootCause
     *            An embedded exception
     * @supported.api
     */
    public UMSException(String message, Throwable rootCause) {
        super(message);
        xcptMessage = message;
        this.rootCause = rootCause;
    }

    /**
     * Constructs a UMSException with no details.
     */
    protected UMSException() {
        super();
        xcptMessage = null;
    }

    /**
     * Returns the detail message of this exception and all embedded exceptions.
     * @supported.api
     */
    public String getMessage() {

        // if there's no nested exception,
        // return the main message
        if (getRootCause() == null)
            return xcptMessage;

        StringBuilder theMsg = new StringBuilder();

        // get the root cause message
        String nestedMsg;
        if (rootCause instanceof LDAPException) {
            nestedMsg = ((LDAPException) rootCause).getLDAPErrorMessage();
        } else {
            nestedMsg = rootCause.getMessage();
        }

        if (xcptMessage != null)
            theMsg.append(xcptMessage).append("::").append(nestedMsg);
        else
            theMsg.append(nestedMsg);

        return theMsg.toString();
    }

    /**
     * Returns the embedded exception.
     * @supported.api
     */
    public Throwable getRootCause() {
        return rootCause;
    }

    /**
     * Format this UMSException to a PrintWriter.
     * 
     * @param out
     *            PrintWriter to write exception to.
     * 
     * @return The out parameter passed in.
     * @see java.io.PrintWriter
     * @supported.api
     */
    public PrintWriter log(PrintWriter out) {
        return log(this, out);
    }

    /**
     * A utility method to format an Exception to a PrintWriter.
     * 
     * @param xcpt
     *            Exception to log.
     * @param out
     *            PrintWriter to write exception to.
     * 
     * @return The out parameter passed in.
     * @see java.io.PrintWriter
     * @supported.api
     */
    static public PrintWriter log(Throwable xcpt, PrintWriter out) {

        out.println("-----------");
        out.println(xcpt.toString());
        out.println("Stack Trace:");
        out.print(getStackTrace(xcpt));
        out.println("-----------");
        out.flush();
        return out;
    }

    /**
     * Formats a UMSException exception message; includes embedded exceptions.
     * @supported.api
     */
    public String toString() {

        StringBuilder buf = new StringBuilder();
        buf.append("--------------------------------------");
        buf.append("Got UMS Exception\n");

        String msg = getMessage();
        if (msg != null && msg.length() > 0) {
            buf.append("Message: ").append(getMessage());
        }

        // Invoke toString() of rootCause first
        if (rootCause != null) {
            buf.append("\nLower level exception: ");
            buf.append(getRootCause());
        }

        return buf.toString();
    }

    /**
     * Prints this exception's stack trace to <tt>System.err</tt>. If this
     * exception has a root exception; the stack trace of the root exception is
     * printed to <tt>System.err</tt> instead.
     * @supported.api
     */
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    /**
     * Prints this exception's stack trace to a print stream. If this exception
     * has a root exception, the stack trace of the root exception is printed to
     * the print stream instead.
     * 
     * @param ps
     *            The non-null print stream to which to print.
     * @supported.api
     */
    public void printStackTrace(java.io.PrintStream ps) {
        if (rootCause != null) {
            String superString = super.toString();
            synchronized (ps) {
                ps.print(superString + (superString.endsWith(".") ? "" : ".")
                        + "  Root exception is ");
                rootCause.printStackTrace(ps);
            }
        } else {
            super.printStackTrace(ps);
        }
    }

    /**
     * Prints this exception's stack trace to a print writer. If this exception
     * has a root exception; the stack trace of the root exception is printed to
     * the print writer instead.
     * 
     * @param pw The non-null print writer to which to print.
     * @supported.api
     */
    public void printStackTrace(java.io.PrintWriter pw) {
        if (rootCause != null) {
            String superString = super.toString();
            synchronized (pw) {
                pw.print(superString + (superString.endsWith(".") ? "" : ".")
                        + "  Root exception is ");
                rootCause.printStackTrace(pw);
            }
        } else {
            super.printStackTrace(pw);
        }
    }

    /**
     * Get exception stack trace as a string.
     * 
     * java.lang.Throwable java.lang.Exception UMSException <name of exception
     * being thrown>
     */
    static private String getStackTrace(Throwable xcpt) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        xcpt.printStackTrace(pw);

        return sw.toString();
    }
}
