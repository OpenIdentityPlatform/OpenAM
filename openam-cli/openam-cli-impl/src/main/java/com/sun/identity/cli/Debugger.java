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
 * $Id: Debugger.java,v 1.2 2008/06/25 05:42:08 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli;


import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * This class is responsible for printing debug message to standard output
 * if the --debug option is set in the command line.
 */
public class Debugger {
    private Debugger() {
    }

    /**
     * Prints messages only when the debug state is either
     * Debug.MESSAGE or Debug.ON.
     *
     * <p><b>NOTE:</b> Debugging is an IO intensive operation and may hurt
     * application performance when abused. Particularly, note that Java
     * evaluates arguments to <code>message()</code> even when
     * debugging is turned off. So when the argument to this method involves
     * the String concatenation operator '+' or any other method invocation,
     * <code>messageEnabled</code> <b>MUST</b> be used. It is recommended that
     * the debug state be checked by invoking <code>messageEnabled()</code>
     * before invoking any <code>message()</code> methods to avoid
     * unnecessary argument evaluation and maximize application performance.</p>
     *
     * @param mgr Command Manager Object.
     * @param msg debug message.
     * @see com.sun.identity.shared.debug.Debug#message(String, Throwable)
     */
    public static void message(CommandManager mgr, String msg) {
        dumpToOutput(mgr, msg, null);
        mgr.getDebugger().message(msg, null);
    }

    /**
     * <p> Prints debug and exception messages only when the debug
     * state is either Debug.MESSAGE or Debug.ON. If the debug file is not
     * accessible and debugging is enabled, the message along with a time stamp
     * and thread info will be printed on <code>System.out</code>.</p>
     *
     * <p>This method creates the debug file if does not exist; otherwise it
     * starts appending to the existing debug file. When invoked for the first
     * time on this object, the method writes a line delimiter of '*'s.</p>
     *
     * <p>Note that the debug file will remain open until <code>destroy()</code>
     * is invoked. To conserve file resources, you should invoke
     * <code>destroy()</code> explicitly rather than wait for the garbage
     * collector to clean up.</p>
     *
     * <p><b>NOTE:</b> Debugging is an IO intensive operation and may hurt
     * application performance when abused. Particularly, note that
     * Java evaluates arguments to <code>message()</code> even when
     * debugging is turned off. It is recommended that the debug state be
     * checked by invoking <code>messageEnabled()</code> before invoking any
     * <code>message()</code> methods to avoid unnecessary argument evaluation
     * and to maximize application performance.</p>
     *
     * @param mgr Command Manager Object.
     * @param msg message to be printed. A newline will be appended to the
     *        message before printing either to <code>System.out</code>
     *        or to the debug file. If <code>msg</code> is null, it is
     *        ignored.
     * @param t <code>Throwable</code>, on which <code>printStackTrace</code>
     *        will be invoked to print the stack trace. If <code>t</code> is
     *        null, it is ignored.
     * @see com.sun.identity.shared.debug.Debug#error(String, Throwable)
     */
    public static void message(CommandManager mgr, String msg, Throwable t) {
        dumpToOutput(mgr, msg, t);
        mgr.getDebugger().message(msg, t);
    }

    /**
     * Prints warning messages only when debug level is greater than
     * Debug.ERROR.
     *
     * <p><b>NOTE:</b> Debugging is an IO intensive operation and may hurt
     * application performance when abused. Particularly, note that
     * Java evaluates arguments to <code>warning()</code> even when
     * debugging is turned off. So when the argument to this method involves
     * the String concatenation operator '+' or any other method invocation,
     * <code>warningEnabled</code> <b>MUST</b> be used. It is recommended that
     * the debug state be checked by invoking <code>warningEnabled()</code>
     * before invoking any <code>warning()</code> methods to avoid
     * unnecessary argument evaluation and to maximize application
     * performance.</p>
     *
     * @param mgr Command Manager Object.
     * @param msg message to be printed. A newline will be appended to the
     *        message before printing either to <code>System.out</code>
     *        or to the debug file. If <code>msg</code> is null, it is
     *        ignored.
     *
     * @see com.sun.identity.shared.debug.Debug#warning(String, Throwable)
     */
    public static void warning(CommandManager mgr, String msg) {
        dumpToOutput(mgr, msg, null);
        mgr.getDebugger().warning(msg, null);
    }

    /**
     * Prints warning messages only when debug level is greater than
     * Debug.ERROR.
     *
     * <p><b>NOTE:</b> Debugging is an IO intensive operation and may hurt
     * application performance when abused. Particularly, note that
     * Java evaluates arguments to <code>warning()</code> even when
     * debugging is turned off. It is recommended that the debug state be
     * checked by invoking <code>warningEnabled()</code> before invoking any
     * <code>warning()</code> methods to avoid unnecessary argument evaluation
     * and to maximize application performance.</p>
     *
     * <p>If the debug file is not accessible and debugging is enabled, the
     * message along with a time stamp and thread info will be printed on
     * <code>System.out</code>.</p>
     *
     * <p>This method creates the debug file if does not exist; otherwise it
     * starts appending to the existing debug file. When invoked for the first
     * time on this object, the method writes a line delimiter of '*'s.</p>
     *
     * <p>Note that the debug file will remain open until <code>destroy()</code>
     * is invoked. To conserve file resources, you should invoke
     * <code>destroy()</code> explicitly rather than wait for the garbage
     * collector to clean up.</p>
     *
     * @param mgr Command Manager Object.
     * @param msg message to be printed. A newline will be appended to the
     *        message before printing either to <code>System.out</code>
     *        or to the debug file. If <code>msg</code> is null, it is
     *        ignored.
     *
     * @param t <code>Throwable</code>, on which <code>printStackTrace()</code>
     *        will be invoked to print the stack trace. If <code>t</code> is
     *        null, it is ignored.
     */
    public static void warning(CommandManager mgr, String msg, Throwable t) {
        dumpToOutput(mgr, msg, t);
        mgr.getDebugger().warning(msg, t);
    }

    /**
     * Prints error messages only when debug level is greater than DEBUG.OFF.
     *
     * @param mgr Command Manager Object.
     * @param msg  message to be printed. A newline will be appended to the
     *             message before printing either to <code>System.out</code>
     *             or to the debug file. If <code>msg</code> is null, it is
     *             ignored.
     *
     * @see com.sun.identity.shared.debug.Debug#error(String, Throwable)
     */
    public static void error(CommandManager mgr, String msg) {
        dumpToOutput(mgr, msg, null);
        mgr.getDebugger().error(msg, null);
    }

    /**
     * Prints error messages only if debug state is greater than
     * Debug.OFF. If the debug file is not accessible and debugging is enabled,
     * the message along with a time stamp and thread info will be printed on
     * <code>System.out</code>.</p>
     *
     * <p>This method creates the debug file if does not exist; otherwise it
     * starts appending to the existing debug file. When invoked for the first
     * time on this object, the method writes a line delimiter of '*'s.</p>
     *
     * <p>Note that the debug file will remain open until <code>destroy()</code>
     * is invoked. To conserve file resources, you should invoke
     * <code>destroy()</code> explicitly rather than wait for the garbage
     * collector to clean up.</p>
     *
     * @param mgr Command Manager Object.
     * @param msg message to be printed. A newline will be appended to the
     *        message before printing either to <code>System.out</code>
     *        or to the debug file. If <code>msg</code> is null, it is
     *        ignored.
     *
     * @param t <code>Throwable</code>, on which <code>printStackTrace()</code>
     *        will be invoked to print the stack trace. If <code>t</code> is
     *        null, it is ignored.
     */
    public static void error(CommandManager mgr, String msg, Throwable t) {
        dumpToOutput(mgr, msg, t);
        mgr.getDebugger().error(msg, t);
    }
    
    /**
     * Returns stack trace of an exception.
     *
     * @param e Exception object.
     * @return Stack trace of an exception.
     */
    public static String getStackTrace(Throwable e) {
        StringBuilder buf = new StringBuilder();
        if (e != null) {
            StringWriter stBuf = new StringWriter(300);
            PrintWriter stackStream = new PrintWriter(stBuf);
            e.printStackTrace(stackStream);
            stackStream.flush();
            buf.append(stBuf.toString());
        }
        return buf.toString();
    }

    private static void dumpToOutput(
        CommandManager mgr,
        String msg,
        Throwable t
    ) {
        if (mgr.isDebugOn()) {
            IOutput writer = mgr.getOutputWriter();
            writer.printlnMessage(msg);
            if (t != null) {
                writer.printlnMessage(getStackTrace(t));
            }
        }
    }
}
