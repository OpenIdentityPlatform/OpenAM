/*
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
 * $Id: Debug.java,v 1.5 2008/06/25 05:47:47 qcheng Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.sun.identity.saml2.idpdiscovery;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

// NOTE: Since JVM specs guarantee atomic access/updates to int variables
// (actually all variables except double and long), the design consciously
// avoids synchronized methods, particularly for message(). This is done to
// reduce the performance overhead of synchronized message() when debugging
// is disabled. This does not have serious side-effects other than an occasional
// invocation of message() missing concurrent update of 'debugLevel'.


/*******************************************************************************
<p>Allows a uniform interface to file debug and exception information in a
uniform format. <code>Debug</code> supports different levels/states of
filing debug information (in the ascending order): <code>OFF</code>, 
<code>ERROR</code>, <code>WARNING</code>, <code>MESSAGE</code> and 
<code>ON</code>. A given debug level/state is enabled if the debug
state/level is set to at least that state/level. For example, if the debug
state is <code>ERROR</code>, only errors will be filed. If the debug state is 
<code>WARNING</code>, only errors and warnings will be filed. If the debug
state is <code>MESSAGE</code>, everything will be filed.
<code>MESSAGE</code> and <code>ON</code> are of the same levels; 
the difference between them being <code>MESSAGE</code> writes to a file, 
whereas <code>ON</code> writes to System.out.</p>
<p>
Debug service uses the property file, <code>DebugConfig.properties</code>, to
set the default debug level and the output directory where the debug files
will be placed. The properties file is located (using
{@link java.util.ResourceBundle} semantics) from one of the directories
in the CLASSPATH.
<p>
The following keys are used to configure the Debug service.
Possible values for the key 'level' are: off | error | warning | message
The key 'directory' specifies the output directory where the debug files will
be created.

<blockquote><pre>
com.iplanet.services.debug.level
com.iplanet.services.debug.directory
</pre></blockquote>

If there is an error reading or loading the properties, 
debug service will redirect all debug information to <code>System.out</code>

If these properties are changed, the server must be restarted for the
changes to take effect.

<p><b>NOTE:</b> Debugging is an IO intensive operation and may hurt
application performance when abused. Particularly, note that Java evaluates 
the arguments to <code>message()</code> and <code>warning()</code> even 
when debugging is turned off.
It is recommended that the debug state be checked before invoking any 
<code>message()</code> or <code>warning()</code> methods to avoid unnecessary
argument evaluation and to maximize application performance.</p>
******************************************************************************/
public class Debug {
    /** flags the disabled debug state.  */
    public static final int OFF = 0;

    /** flags the state where error debugging is enabled. When debugging is set 
     * to less than <code>ERROR</code>, error debugging is also disabled.
     */
    public static final int ERROR = 1;

    /** flags the state where warning debugging is enabled, but message 
     * debugging is disabled. When debugging is set to less than
     * <code>WARNING</code>, warning debugging is also disabled.
     */
    public static final int WARNING = 2;

    /** This state enables debugging of messages, warnings and errors. */
    public static final int MESSAGE = 3;

    /** flags the enabled debug state for warnings, errors and messages. 
     * Printing to a file is disabled. All printing is done on System.out.
     */ 
    public static final int ON = 4;

    /** debugMap is a container of all active Debug objects. Log file name is
     * the key and Debug is the value of this map.
     */
    private static Map debugMap = new HashMap();

    private static DateFormat dateFormat;

    /** The default debug level for the entire service and the level that is
     * used when a Debug object is first created and before its level is
     * modified. Don't initialize the following two variables in a static
     * initializer/block because other components may initialize Debug in their
     * static initializers (as opposed to constructors or methods). The
     * order of execution of static blocks is not guaranteed by JVM. So if we
     * set the following two static variables to some default values here, then
     * it will interfere with the execution of {@link #initService}.
     */
    private static String debugLevelStr;
    private static String debugDirectory;

    private final String debugName;
    private PrintWriter debugFile = null;
    private int debugLevel;

    private static boolean validInit() {
        return IDPDiscoveryConstants.DEBUG_DIR.equals(debugDirectory)
                && IDPDiscoveryConstants.DEBUG_LEVEL.equals(debugLevelStr);
    }

    /** Initializes the Debug service so that Debug objects can be created. At
     * startup (when the first Debug object is ever created in a JVM), this
     * method reads <code>DebugConfig.properties</code> file (using 
     * {@link java.util.ResourceBundle} semantics) from one of the directories
     * in the CLASSPATH, and loads the properties. It creates the debug
     * directory. If all the directories in output dir don't have adequate
     * permissions then the creation of the debug directory will fail and all
     * the debug files will be located in the "current working directory" of
     * the process running debug code
     * If there is an error reading or loading the properties, it will set the
     * debug service to redirect all debug information to
     * <code>System.out</code>
     */
    private static void initService() {
        /* We will use the double-checked locking pattern. Rarely entered
         * block. Push synchronization inside it. This is the first check.
         */
        if (!validInit()) {
             /* Only 1 thread at a time gets past the next point. Rarely
             * executed synchronization statement and hence synchronization
             * penalty is not paid every time this method is called.
             */
            synchronized (Debug.class) {
                /* If a second thread was waiting to get here, it will now
                 * find that the instance has already been initialized, and
                 * it will not re-initialize the instance variable. This is the
                 * (second) double-check.
                 */
                if (!validInit()) {
                    dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss:SSS a zzz");
                    try {
                        debugLevelStr = SystemProperties.get(IDPDiscoveryConstants.DEBUG_LEVEL);
                        debugDirectory = SystemProperties.get(IDPDiscoveryConstants.DEBUG_DIR);
                        if (debugDirectory != null ) {
                            File createDir = new File(debugDirectory);
                            if ((!createDir.exists()) && (!createDir.mkdirs()))
                            {
                                System.err.println("could not create debug dir /var/opt/SUNWam/debug");
                            }
                        }
                    } catch (MissingResourceException e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace();

                        // If there is any error in getting the level or
                        // outputDirectory, defaultDebugLevel will be set to
                        // ON so that output will go to
                        // System.out
                        debugLevelStr = "on";
                        debugDirectory = null;
                    } catch (SecurityException se) {
                        System.err.println(se.getMessage());
                    }
                }
            }
        }
    }

    /** This constructor takes as an argument the name of the debug file. The 
     * debug file is neither created nor opened until the first time 
     * <code>message()</code>, <code>warning()</code> or <code>error()</code> 
     * is invoked and the debug state is neither <code>OFF</code> nor 
     * <code>ON</code>.
     * <p><b>NOTE:</b>The recommended and preferred method to create Debug 
     * objects is <code>getInstance(String)</code>. This constructor may be
     * deprecated in future.</p>
     *
     * @param debugName name of the debug file to create or use
     * @deprecated  Use {@link #getInstance}
     */
    public Debug(String debugName) {
        // Initialize the debug service the first time a Debug object is
        // created.
        initService();

        // Now initialize this instance itself

        this.debugName = debugName;
        setDebug(debugLevelStr);

        synchronized (debugMap) {
            // explicitly ignore any duplicate instances.
            debugMap.put(debugName, this);
        }
    }

    /**
     * Returns an existing instance of Debug for the specified debug file or a
     * new one if no such instance already exists. If a Debug object has to be
     * created, its level is set to the level defined in the 
     * <code>DebugConfig.properties</code> file. The level can be changed later
     * by using {@link #setDebug(int)} or {@link #setDebug(String)}
     *
     * @param debugName name of debug instance.
     * @return an instance of <code>Debug</code>.
     */
    public static synchronized Debug getInstance(String debugName) {
        Debug debugObj = (Debug) debugMap.get(debugName);
        if (debugObj == null ||
                (debugDirectory != null &&
                !debugDirectory.equals(SystemPropertiesManager.get(IDPDiscoveryConstants.DEBUG_DIR)))) {
            debugObj = new Debug(debugName);
        }
        return debugObj;
    }
    
    /** Checks if message debugging is enabled.
     *
     * <p><b>NOTE:</b> It is recommended that <code>messageEnabled()</code>
     * be used instead of <code>debugEnabled()</code> as the former is more 
     * intuitive.</>
     *
     * @return <code>true</code> if message debugging is enabled
     *        <code>false</code> if message debugging is disabled
     *
     * @deprecated Use {@link #messageEnabled}
     */
    public boolean debugEnabled() {
        return (debugLevel > Debug.WARNING);
    }

    /** Checks if message debugging is enabled.
     *
     * <p><b>NOTE:</b> Debugging is an IO intensive operation and may hurt
     * application performance when abused. Particularly, note that Java 
     * evaluates arguments to <code>message()</code> even when 
     * debugging is turned off. It is recommended that 
     * <code>messageEnabled()</code> be called to check the debug state
     * before invoking any <code>message()</code> methods to avoid
     * unnecessary argument evaluation and maximize application performance.</p>
     *
     * @return <code>true</code> if message debugging is enabled
     *        <code>false</code> if message debugging is disabled
     */
    public boolean messageEnabled() {
        return (debugLevel > Debug.WARNING);
    }

    /** Checks if warning debugging is enabled.
     *
     * <p><b>NOTE:</b> Debugging is an IO intensive operation and may hurt
     * application performance when abused. Particularly, note that Java 
     * evaluates arguments to <code>warning()</code> even when 
     * warning debugging is turned off. It is recommended that 
     * <code>warningEnabled()</code> be called to check the debug state
     * before invoking any <code>warning()</code> methods to avoid
     * unnecessary argument evaluation and maximize application performance.</p>
     *
     * @return <code>true</code> if warning debugging is enabled
     *        <code>false</code> if warning debugging is disabled
     */
    public boolean warningEnabled() {
        return (debugLevel > Debug.ERROR);
    }

    /**
     * Returns one of the five possible values:
     * <ul>
     * <li><code>Debug.OFF</code>
     * <li><code>Debug.ERROR</code>
     * <li><code>Debug.WARNING</code>
     * <li><code>Debug.MESSAGE</code>
     * <pli<code>Debug.ON</code>
     * </ul>
     *
     * @return debug state.
     */
    public int getState() {
        return debugLevel;
    }
    
    /** Prints messages only when the debug state is either 
     * DEBUG.MESSAGE or Debug.ON.
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
     * @param msg message to be printed. A newline will be appended to the
     *        message before printing either to <code>System.out</code>
     *        or to the debug file. If <code>msg</code> is null, it is
     *        ignored.
     * @see Debug#message(String msg, Throwable t)
     */
    public void message(String msg) {
        if (debugLevel > Debug.WARNING) {
            message(msg, null);
        }
    }
    
    /** <p> Prints debug and exception messages only when the debug
     * state is either DEBUG.MESSAGE or Debug.ON. If the debug file is not 
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
     * @param msg message to be printed. A newline will be appended to the
     *        message before printing either to <code>System.out</code>
     *        or to the debug file. If <code>msg</code> is null, it is
     *        ignored.
     * @param t <code>Throwable</code>, on which <code>printStackTrace</code>
     *        will be invoked to print the stack trace. If <code>t</code> is
     *        null, it is ignored.
     * @see Debug#error(String msg, Throwable t)
     */
    public void message(String msg, Throwable t) {
        if (debugLevel > Debug.WARNING) {
            formatAndWrite(null, msg, t);
        }
    }

    /** Prints warning messages only when debug level is greater than 
     * DEBUG.ERROR.
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
     * @param msg message to be printed. A newline will be appended to the
     *        message before printing either to <code>System.out</code>
     *        or to the debug file. If <code>msg</code> is null, it is
     *        ignored.
     * @see Debug#warning(String msg, Throwable t)
     */
    public void warning(String msg) {
        if (debugLevel > Debug.ERROR) {
            formatAndWrite("WARNING: ", msg, null);
        }
    }
    
    /** Prints warning messages only when debug level is greater than
     * DEBUG.ERROR.
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
     * @param msg message to be printed. A newline will be appended to the
     *        message before printing either to <code>System.out</code>
     *        or to the debug file. If <code>msg</code> is null, it is
     *        ignored.
     * @param t <code>Throwable</code>, on which
     *        <code>printStackTrace()</code> will be invoked to print the
     *        stack trace. If <code>t</code> is null, it is ignored.
     */
    public void warning(String msg, Throwable t) {
        if (debugLevel > Debug.ERROR) {
            formatAndWrite("WARNING: ", msg, t);
        }
    }
    
    /**
     * Prints error messages only when debug level is greater than DEBUG.OFF.
     *
     * @param msg message to be printed. A newline will be appended to the
     *        message before printing either to <code>System.out</code>
     *        or to the debug file. If <code>msg</code> is null, it is
     *        ignored.
     * @see Debug#error(String msg, Throwable t)
     */
    public void error(String msg) {
        if (debugLevel > Debug.OFF) {
            formatAndWrite("ERROR: ", msg, null);
        }
    }
    
    /** Prints error messages only if debug state is greater than
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
     * @param msg message to be printed. A newline will be appended to the
     *        message before printing either to <code>System.out</code>
     *        or to the debug file. If <code>msg</code> is null, it is
     *        ignored.
     * @param t <code>Throwable</code>, on which <code>printStackTrace()</code>
     *        will be invoked to print the stack trace. If <code>t</code> is
     *        null, it is ignored.
     */
    public void error(String msg, Throwable t) {
        if (debugLevel > Debug.OFF) {
            formatAndWrite("ERROR: ", msg, t);
        }
    }

    private void formatAndWrite(String prefix, String msg, Throwable t) {
        if (debugLevel == Debug.ON) {
            if (msg != null) {
                if (prefix == null) {
                    System.out.println(msg);
                } else {
                    System.out.println(prefix + msg);
                }
            }
            if (t != null) {
                System.out.println(t.getMessage());
                t.printStackTrace(System.out);
            }
            return;
        }
            
        // The default capacity of StringBuffer in StringWriter is 16, but we
        // know for sure that the minimum header size is about 35. Hence, to
        // avoid reallocation allocate at least 160 chars.

        StringWriter swriter = new StringWriter(160);
        PrintWriter buf = new PrintWriter(swriter, true);
        synchronized (dateFormat) {
            buf.write(dateFormat.format(newDate()));
        }
        buf.write(": ");
        buf.write(Thread.currentThread().toString());
        buf.write("\n");
        if (prefix != null) {
            buf.write(prefix);
        }
        if (msg != null) {
            buf.write(msg);
        }
        if (t != null) {
            buf.write("\n");
            t.printStackTrace(buf);
        }
        buf.flush();
        
        write(swriter.toString());
    }

    /** Actually writes to the debug file. If it cannot write to the debug
     * file, it turn off debugging. The first time this method is invoked on 
     * a Debug object, that object's debug file is created/opened in the
     * directory specified by the 
     * <code>property com.iplanet.services.debug.directory</code> in the 
     * properties file, <code>DebugConfig.properties</code>.
     */
    private synchronized void write(String msg) {

        try {
            // debugging is enabled.
            // First, see if the debugFile is already open. If not, open it now.
            if (debugFile == null ||
                    (debugDirectory != null &&
                    !debugDirectory.equals(SystemPropertiesManager.get(IDPDiscoveryConstants.DEBUG_DIR)))) {

                initService();

                // open file in append mode
                FileOutputStream fos = new FileOutputStream(debugDirectory + File.separator + debugName,  true);
                debugFile = new PrintWriter(
                    new BufferedWriter( new OutputStreamWriter(fos, "UTF8") ),
                    true); // autoflush enabled

                debugFile.println(
                    "******************************************************");
            }

            debugFile.println(msg);
        } catch (IOException e) {
            System.err.println(msg);
                
            // turn off debugging because debugFile is not accessible
            debugLevel = Debug.OFF;
        }
    }

    /**
     * Sets the debug capabilities based on the values of the
     * <code>debugType</code> argument.
     *
     * @param debugType is any one of five possible values:
     * <ul>
     * <li><code>Debug.OFF</code>
     * <li><code>Debug.ERROR</code>
     * <li><code>Debug.WARNING</code>
     * <li><code>Debug.MESSAGE</code>
     * <li><code>Debug.ON</code>
     * <ul>
     */
    public void setDebug(int debugType) {
        switch (debugType) {
          case Debug.OFF:
          case Debug.ERROR:
          case Debug.WARNING:
          case Debug.MESSAGE:
          case Debug.ON:
              debugLevel = debugType;
              break;

          default:
              // ignore invalid debugType values
              break;
        }
    }

    /**
     * Enables or disables debugging based on the value of debug attribute,
     * <code>com.iplanet.services.debug.level</code>, in the
     * <code>DebugConfig.properties</code> file.
     * <code>DebugConfig.properties<code>
     * file should be accessible from CLASSPATH.
     * If the property is not defined, debug level is set to <code>error</code>.
     *
     * @deprecated Use {@link #getInstance}. {@link #getInstance} will
     *             automatically set the debug level based on the information in
     *             <code>DebugConfig.properties</code> file.
     */
    public void setDebug() {
        // The following initService is temporary. setDebug() is anyways
        // deprecated and will be removed in future.
        initService();
        setDebug(debugLevelStr);
    }

    /**
     * Sets the debug capabilities based on the values of the
     * <code>debugType</code> argument.
     *
     * @param debugType is any one of the following possible values:
     * <ul>
     * <li>off - debugging is disabled
     * <li>on - all debugging is enabled and written to <code>System.out</code>
     * <li>message - message debugging is enabled and written to the debug file
     * <li>warning - warning debugging is enabled and written to the debug file
     * <li>error - error debugging is enabled and written to the debug file
     * </ul>
     */
    public void setDebug(String debugType) {
        if (debugType == null) {
            return;
        } else if (debugType.equalsIgnoreCase("error")) {
            debugLevel = Debug.ERROR;
        } else if (debugType.equalsIgnoreCase("warning")) {
            debugLevel = Debug.WARNING;
        } else if (debugType.equalsIgnoreCase("message")) {
            debugLevel = Debug.MESSAGE;
        } else if (debugType.equalsIgnoreCase("on")) {
            debugLevel = Debug.ON;
        } else if (debugType.equalsIgnoreCase("off")) {
            debugLevel = Debug.OFF;
        } else if (debugType.equals("*")) {
            debugLevel = Debug.ON;
        } else {
            if (debugType.endsWith("*")) {
                debugType = debugType.substring(0,debugType.length()-1);
            }
            if (debugName.startsWith(debugType)){
                debugLevel = Debug.ON;
            }
        }
    }

    /** Destroys the debug object, closes the debug file and releases any system
     * resources. Note that the debug file will remain open until
     * <code>destroy()</code> is invoked. To conserve file resources, you should
     * invoke <code>destroy()</code> explicitly rather than wait for the garbage
     * collector to clean up.
     *
     * <p> If this object is accessed after <code>destroy()</code> has been
     * invoked, the results are undefined.</p>
     */
    public void destroy() {
        finalize();
    }
    
    /** Flushes and then closes the debug file. */
    protected void finalize() {
        synchronized (debugMap) {
            debugMap.remove(debugName);
        }
        
        synchronized (this) {
            if (debugFile == null) {
                return;
            }
            
            debugLevel = Debug.OFF;
            debugFile.flush();
            debugFile.close();
            debugFile = null;
        }
    }
}
