/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: EncryptionException.java,v 1.3 2008/06/25 05:48:16 qcheng Exp $
 *
 */


package com.sun.identity.xmlenc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Exception;
import java.util.Enumeration;
import java.util.StringTokenizer;

import com.sun.identity.shared.locale.L10NMessageImpl;

/**
 * <PRE>
 * This class is the super-class for all Federation <B>checked</B> exceptions.
 *
 *
 * Some Exception throwing guidelines:
 * -------------------------------------
 *
 * <B> Checked exceptions </B> are sub-classes of java.lang.Exception; methods
 * throwing this type of exception are forced to define a throws clause
 * in the method signature and client programmers need to catch and
 * handle the exception with a try/catch block or declare the throws clause
 * in their methods.
 * <B> Unchecked exceptions </B> are sub-classes of java.lang.RuntimeException.
 * Client programmers don't have to deal with the exception using a
 * try/catch block and the method throwing it does not have to define it
 * in its signature.
 *
 * - If your method encounters an abnormal condition which causes it
 *   to be unable to fulfill its contract, or throw a checked or
 *   unchecked exception (either EncryptionException or RuntimeException).
 *
 * - If your method discovers that a client has breached its contract,
 *   for example, passing a null as a parameter where a non-null value is
 *   required, throw an unchecked exception (RuntimeException).
 *
 * - If your method is unable to fulfill its contract and you feel client
 *   programmers should consciously decide how to handle, throw checked
 *   exceptions (EncryptionException).
 *
 *
 * Embedded/Nested Exceptions:
 * --------------------------
 *
 *  An exception of type EncryptionException can embed any
 *  exception of type Throwable. Embedded exceptions ensure traceability
 *  of errors in a multi-tiered application. For example, in a simple 3-
 *  Tier model - presentation/client tier, middle/domain tier and
 *  database/persistence tier - the real cause of error might be lost by the
 *  time control, which is passed back from the persistence tier to the client
 *  tier. To ensure tracking info, the constructor EncryptionException(message,Throwable)
 *  should be used while throwing the exception.
 *  Client programs can then invoke the #getRootCause() method
 *  to get the underlying cause.
 *
 * Exception hierarchy should be defined:
 * -------------------------------------
 * An exception for each abnormal cause should be created.
 * EncryptionException should probably be thrown only by external API's.
 * Even these should have embedded exceptions from lower level tiers.
 * Every package should define its own exception hierarchies specific
 * to its context, for example, account management exceptions should be
 * defined in the accountmgmt package.
 *
 * Localizing Error Messages
 * -------------------------
 * The java resource bundle mechanism is used to implement localization.
 * The ResourceSet and ResourceSetManager classes are used to implement
 * localization.
 *
 * Steps for creating EncryptionException Sub-classes and messages
 * ------------------------------------------------------
 *
 * 1. Identify the package this exception will belong to.
 *	account management related exception
 *	should be part of the accountmgmt package.
 *
 * 2. Each package should have its own properties file to store
 *	error messages.
 *	For example accountmgmt.properties in package accountmgmt
 
 * 3. Create a sub-class of EncryptionException and override the constructors.
 *
 *    public class FSAccountManagementException extends EncryptionException {
 *        public FSAccountManagementException() {
 *                super();
 *        }
 *        public FSAccountManagementException(String msg) {
 *                super(msg);
 *        }
 *        public FSAccountManagementException(String msg, Throwable t) {
 *                super(msg,t);
 *        }
 *
 *
 * Throwing/Catching Exception Examples:
 * ------------------------------------
 *
 * 1. Throwing a non-nested Exception
 *	<B>(not recommended, use Ex. 3 below)</B>
 *       EncryptionException ux = new EncryptionException("Some weird error!...");
 *       throw ux;
 *
 * 2. Throwing a nested Exception
 *	<B>(not recommended, use Ex. 3 below)</B>
 *       try {
 *               .......
 *               .......
 *       } catch (UMSException umse) {
 *         EncryptionException fse =
 *                       new EncryptionException("Some weird error!...", le);
 *        throw ux;
 *       }
 *
 * 3. Throwing an Exception using the ResourceSetManager
 * <TBD : write some eg & format for properties file>
 * 
 * - Logging/Dealing with an Exception, inclunding all nested exceptions
 *       try {
 *               .......
 *               .......
 *       } catch (EncryptionException fse) {
 *
 *               if (fse.getRootCause() instanceof UMSException) {
 *                       PrintWriter pw = new PrintWriter(<some file stream>);
 *                       fse.log(pw);
 *               } else {
 *                      System.out.println(fse.getMessage());
 *               }
 *       }
 *
 * </PRE>
 *
 * @see #EncryptionException(String, Object[], Throwable)
 * @see #getRootCause()
 * @see java.lang.Exception
 * @see java.lang.RuntimeException
 */
public class EncryptionException extends L10NMessageImpl {

    protected String _message = null;
    protected Throwable rootCause = null;

    /**
     * Constructor
     * @param errorCode Key of the error message in resource bundle.
     * @param args Arguments to the message.
     */
    public EncryptionException(String errorCode, Object[] args) {
        super(EncryptionUtils.BUNDLE_NAME, errorCode, args);
    }

    /**
     * Constructor
     * @param errorCode Key of the error message in resource bundle.
     * @param args Arguments to the message.
     * @param rootCause  An embedded exception
     */
    public EncryptionException(String errorCode, Object[] args, Throwable rootCause) {
        super(EncryptionUtils.BUNDLE_NAME, errorCode, args);
	this.rootCause = rootCause;
    }

    /**
     * Constructs a <code>EncryptionException</code> with a detailed message.
     *
     * @param message
     * Detailed message for this exception.
     */
    public EncryptionException(String message) {
        super(message);
        _message = message;
    }
    
    /**
     * Constructs a <code>EncryptionException</code> with a message and
     * an embedded exception.
     *
     * @param message  Detailed message for this exception.
     * @param rootCause  An embedded exception
     */
    public EncryptionException(Throwable rootCause, String message) {
        super(message);
        _message = message;
        this.rootCause = rootCause;
    }

    public EncryptionException(Exception ex) {
       super(ex);
    }
    
    /**
     * Returns the embedded exception.
     *
     * @return the embedded exception.
     */
    public Throwable getRootCause() {
        return rootCause;
    }
    
    /**
     * Format this <code>EncryptionException</code> to a <code>PrintWriter</code>.
     *
     * @param out <code>PrintWriter</code> to write exception to.
     *
     * @return The out parameter passed in.
     * @see java.io.PrintWriter
     */
    public PrintWriter log(PrintWriter out) {
        return log(this, out);
    }
    
    /**
     * A utility method to format an Exception to a <code>PrintWriter</code>.
     *
     * @param xcpt <code>Exception</code> to log.
     * @param out <code>PrintWriter</code> to write exception to.
     *
     * @return The out parameter passed in.
     * @see java.io.PrintWriter
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
     * Returns a formatted <code>EncryptionException</code> exception message;
     * includes embedded exceptions.
     *
     * @return a formatted <code>EncryptionException</code> exception message.
     */
    public String toString() {
        
        StringBuffer buf = new StringBuffer();
        buf.append("--------------------------------------");
        buf.append("Got Federation Exception\n");
        
        String msg = getMessage();
        if(msg != null && msg.length() > 0) {
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
     * Prints this exception's stack trace to <tt>System.err</tt>.
     * If this exception has a root exception; the stack trace of the
     * root exception is printed to <tt>System.err</tt> instead.
     */
    public void printStackTrace() {
        printStackTrace( System.err );
    }
    
    /**
     * Prints this exception's stack trace to a print stream.
     * If this exception has a root exception, the stack trace of the
     * root exception is printed to the print stream instead.
     * @param ps The non-null print stream to which to print.
     */
    public void printStackTrace(java.io.PrintStream ps) {
        if ( rootCause != null ) {
            String superString = super.toString();
            synchronized ( ps ) {
                ps.print(superString
                + (superString.endsWith(".") ? "" : ".")
                + "  Root exception is ");
                rootCause.printStackTrace( ps );
            }
        } else {
            super.printStackTrace( ps );
        }
    }
    
    /**
     * Prints this exception's stack trace to a print writer.
     * If this exception has a root exception; the stack trace of the
     * root exception is printed to the print writer instead.
     * @param pw The non-null print writer to which to print.
     */
    public void printStackTrace(java.io.PrintWriter pw) {
        if ( rootCause != null ) {
            String superString = super.toString();
            synchronized (pw) {
                pw.print(superString
                + (superString.endsWith(".") ? "" : ".")
                + "  Root exception is ");
                rootCause.printStackTrace( pw );
            }
        } else {
            super.printStackTrace( pw );
        }
    }
    
    /**
     * Get exception stack trace as a string.
     * <pre>
     *   java.lang.Throwable
     *   java.lang.Exception
     *   EncryptionException
     *   <name of exception being thrown>
     * </pre>
     */
    static private String getStackTrace(Throwable xcpt) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        xcpt.printStackTrace(pw);
        
        return sw.toString();
    }
}
