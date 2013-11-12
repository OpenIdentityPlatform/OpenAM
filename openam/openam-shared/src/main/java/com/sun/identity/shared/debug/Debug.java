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
 * $Id: Debug.java,v 1.6 2009/08/19 05:41:17 veiming Exp $
 *
 * Portions Copyrighted 2013 Forgerock AS.
 *
 */

package com.sun.identity.shared.debug;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.impl.DebugProviderImpl;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;


// NOTE: Since JVM specs guarantee atomic access/updates to int variables
// (actually all variables except double and long), the design consciously
// avoids synchronized methods, particularly for message(). This is done to
// reduce the performance overhead of synchronized message() when debugging
// is disabled. This does not have serious side-effects other than an occasional
// invocation of message() missing concurrent update of 'debugLevel'.

/*******************************************************************************
 * <p>
 * Allows a uniform interface to file debug and exception information in a
 * uniform format. <code>Debug</code> supports different levels/states of
 * filing debug information (in the ascending order): <code>OFF</code>,
 * <code>ERROR</code>, <code>WARNING</code>, <code>MESSAGE</code> and
 * <code>ON</code>. A given debug level/state is enabled if the debug
 * state/level is set to at least that state/level. For example, if the debug
 * state is <code>ERROR</code>, only errors will be filed. If the debug state
 * is <code>WARNING</code>, only errors and warnings will be filed. If the
 * debug state is <code>MESSAGE</code>, everything will be filed.
 * <code>MESSAGE</code> and <code>ON</code> are of the same levels; the
 * difference between them being <code>MESSAGE</code> writes to a file,
 * whereas <code>ON</code> writes to System.out.
 * </p>
 * <p>
 * Debug service uses the property file, <code>AMConfig.properties</code>, to
 * set the default debug level and the output directory where the debug files
 * will be placed. The properties file is located (using
 * {@link java.util.ResourceBundle} semantics) from one of the directories in
 * the CLASSPATH.
 * </p>
 * <p>
 * The following keys are used to configure the Debug service. Possible values
 * for the key 'com.iplanet.services.debug.level' are: off | error | warning |
 * message. The key 'com.iplanet.services.debug.directory' specifies the output
 * directory where the debug files will be created. Optionally, the key
 * 'com.sun.identity.util.debug.provider' may be used to plugin a non-default
 * implementation of the debug service where necessary.
 * </p>
 * 
 * <blockquote>
 * 
 * <pre>
 *  com.iplanet.services.debug.level
 *  com.iplanet.services.debug.directory
 *  com.sun.identity.util.debug.provider
 * </pre>
 * 
 * </blockquote>
 * 
 * If there is an error reading or loading the properties, Debug service will
 * redirect all debug information to <code>System.out</code>
 * 
 * If these properties are changed, the server must be restarted for the changes
 * to take effect.
 * 
 * <p>
 * <b>NOTE:</b> Debugging is an IO intensive operation and may hurt application
 * performance when abused. Particularly, note that Java evaluates the arguments
 * to <code>message()</code> and <code>warning()</code> even when debugging
 * is turned off. It is recommended that the debug state be checked before
 * invoking any <code>message()</code> or <code>warning()</code> methods to
 * avoid unnecessary argument evaluation and to maximize application
 * performance.
 * </p>
 * @supported.all.api
 */
public class Debug {

    /* Static fields and methods */

    /** flags the disabled debug state. */
    public static final int OFF = 0;

    /**
     * flags the state where error debugging is enabled. When debugging is set
     * to less than <code>ERROR</code>, error debugging is also disabled.
     */
    public static final int ERROR = 1;

    /**
     * flags the state where warning debugging is enabled, but message debugging
     * is disabled. When debugging is set to less than <code>WARNING</code>,
     * warning debugging is also disabled.
     */
    public static final int WARNING = 2;

    /** This state enables debugging of messages, warnings and errors. */
    public static final int MESSAGE = 3;

    /**
     * flags the enabled debug state for warnings, errors and messages. Printing
     * to a file is disabled. All printing is done on System.out.
     */
    public static final int ON = 4;

    /** flags the disabled debug state. */
    public static final String STR_OFF = "off";

    /**
     * flags the state where error debugging is enabled. When debugging is set
     * to less than <code>ERROR</code>, error debugging is also disabled.
     */
    public static final String STR_ERROR = "error";

    /**
     * flags the state where warning debugging is enabled, but message debugging
     * is disabled. When debugging is set to less than <code>WARNING</code>,
     * warning debugging is also disabled.
     */
    public static final String STR_WARNING = "warning";

    /** This state enables debugging of messages, warnings and errors. */
    public static final String STR_MESSAGE = "message";

    /**
     * flags the enables debug state for warnings, errors and messages. Printing
     * to a file is disabled. All printing is done on System.out.
     */
    public static final String STR_ON = "on";

    /**
     * debugMap is a container of all active Debug objects. Log file name is the
     * key and Debug is the value of this map.
     */
    private static Map debugMap = new HashMap();

    /**
     * serviceInitialized indicates if the service is already initialized.
     */
    private static boolean serviceInitialized = false;

    /**
     * the provider instance that will be used for Debug service.
     */
    private static IDebugProvider debugProvider;

    /**
     * Constant string used as property key to look up the debug provider class
     * name.
     */
    private static final String CONFIG_DEBUG_PROVIDER = 
        "com.sun.identity.util.debug.provider";

    /**
     * Gets an existing instance of Debug for the specified debug name or a new
     * one if no such instance already exists. If a Debug object has to be
     * created, its level is set to the level defined in the
     * <code>AMConfig.properties</code> file. The level can be changed later
     * by using {@link #setDebug(int)} or {@link #setDebug(String)} methods.
     * 
     * @param debugName
     *            name of the debug instances to be created
     * @return a Debug instance corresponding to the specified debug name.
     */
    public static synchronized Debug getInstance(String debugName) {
        Debug debug = (Debug)getDebugMap().get(debugName);
        if (debug == null) {
            debug = new Debug(getDebugProvider().getInstance(debugName));
            getDebugMap().put(debugName, debug);
        }
        return debug;
    }

    /**
     * Returns a collection of all Debug instances that exist in the system at
     * the current instance. This is a live collection that will be updated as
     * and when new Debug instances are created. Note that if an iterator is
     * used, it could potentially cause a
     * <code>ConcurrentModificationException</code> if during the process of
     * iteration, the collection is modified by the system.
     * 
     * @return a collection of all Debug instances in the system.
     */
    public static Collection getInstances() {
        return getDebugMap().values();
    }

    /**
     * Gets the <code>Map</code> of all Debug instances being used in the
     * system currently.
     * 
     * @return the <code>Map</code> of all Debug instances
     */
    private static Map getDebugMap() {
        return debugMap;
    }

    /**
     * Sets the provider instance to be used by Debug service.
     * 
     * @param provider
     *            the <code>IDebugProvider</code> instance that is used by the
     *            Debug service.
     */
    private static void setDebugProvider(IDebugProvider provider) {
        debugProvider = provider;
    }

    /**
     * Gets the configured debug provider being used by the Debug service.
     * 
     * @return the configured debug provider.
     */
    static IDebugProvider getDebugProvider() {
        return debugProvider;
    }

    /**
     * Initializes the Debug Service by locating the SPI implementations and
     * instantiating the appropriate classes.
     */
    private static synchronized void initialize() {
        if (!serviceInitialized) {
            String providerName = SystemPropertiesManager.get(
                CONFIG_DEBUG_PROVIDER);
            IDebugProvider provider = null;
            boolean providerLoadFailed = false;
            if (providerName != null && providerName.trim().length() > 0) {
                try {
                    provider = (IDebugProvider) Class.forName(providerName)
                            .newInstance();
                } catch (ClassNotFoundException cnex) {
                    providerLoadFailed = true;
                } catch (InstantiationException iex) {
                    providerLoadFailed = true;
                } catch (IllegalAccessException iaex) {
                    providerLoadFailed = true;
                } catch (ClassCastException ccex) {
                    providerLoadFailed = true;
                }
            }
            if (provider == null) {
                if (providerLoadFailed) {
                    ResourceBundle bundle =com.sun.identity.shared.locale.Locale
                            .getInstallResourceBundle("amUtilMsgs");
                    System.err.println(bundle.getString(
                            "com.iplanet.services.debug.invalidprovider"));
                }
                provider = new DebugProviderImpl();
            }
            setDebugProvider(provider);
            serviceInitialized = true;
        }
    }

    /* Instance fields and methods */

    /**
     * The instance of the actual debug service class as obtained from the
     * configured provider.
     */
    private IDebug debugServiceInstance;

    /**
     * Convinience method to query the name being used for this Debug instance.
     * The return value of this method is a string exactly equal to the name
     * that was used while creating this instance.
     * 
     * @return the name of this Debug instance
     */
    public String getName() {
        return getDebugServiceInstance().getName();
    }

    /**
     * Checks if message debugging is enabled.
     * 
     * <p>
     * <b>NOTE:</b> Debugging is an IO intensive operation and may hurt
     * application performance when abused. Particularly, note that Java
     * evaluates arguments to <code>message()</code> even when debugging is
     * turned off. It is recommended that <code>messageEnabled()</code> be
     * called to check the debug state before invoking any
     * <code>message()</code> methods to avoid unnecessary argument evaluation
     * and maximize application performance.
     * </p>
     * 
     * @return <code>true</code> if message debugging is enabled
     *         <code>false</code> if message debugging is disabled
     */
    public boolean messageEnabled() {
        return getDebugServiceInstance().messageEnabled();
    }

    /**
     * Checks if warning debugging is enabled.
     * 
     * <p>
     * <b>NOTE:</b> Debugging is an IO intensive operation and may hurt
     * application performance when abused. Particularly, note that Java
     * evaluates arguments to <code>warning()</code> even when warning
     * debugging is turned off. It is recommended that
     * <code>warningEnabled()</code> be called to check the debug state before
     * invoking any <code>warning()</code> methods to avoid unnecessary
     * argument evaluation and maximize application performance.
     * </p>
     * 
     * @return <code>true</code> if warning debugging is enabled
     *         <code>false</code> if warning debugging is disabled
     */
    public boolean warningEnabled() {
        return getDebugServiceInstance().warningEnabled();
    }

    /**
     * Checks if error debugging is enabled.
     * 
     * <p>
     * <b>NOTE:</b> Debugging is an IO intensive operation and may hurt
     * application performance when abused. Particularly, note that Java
     * evaluates arguments to <code>error()</code> even when error debugging
     * is turned off. It is recommended that <code>errorEnabled()</code> be
     * called to check the debug state before invoking any <code>error()</code>
     * methods to avoid unnecessary argument evaluation and maximize application
     * performance.
     * </p>
     * 
     * @return <code>true</code> if error debugging is enabled
     *         <code>false</code> if error debugging is disabled
     */
    public boolean errorEnabled() {
        return getDebugServiceInstance().errorEnabled();
    }

    /**
     * Returns one of the five possible values:
     * <ul>
     * <li>Debug.OFF
     * <li>Debug.ERROR
     * <li>Debug.WARNING
     * <li>Debug.MESSAGE
     * <li>Debug.ON
     * </ul>
     * 
     * @return the debug level
     */
    public int getState() {
        return getDebugServiceInstance().getState();
    }

    /**
     * Prints messages only when the debug state is either Debug.MESSAGE or
     * Debug.ON.
     * 
     * <p>
     * <b>NOTE:</b> Debugging is an IO intensive operation and may hurt
     * application performance when abused. Particularly, note that Java
     * evaluates arguments to <code>message()</code> even when debugging is
     * turned off. So when the argument to this method involves the String
     * concatenation operator '+' or any other method invocation,
     * <code>messageEnabled</code> <b>MUST</b> be used. It is recommended
     * that the debug state be checked by invoking <code>messageEnabled()</code>
     * before invoking any <code>message()</code> methods to avoid unnecessary
     * argument evaluation and maximize application performance.
     * </p>
     * 
     * @param msg
     *            debug message.
     * @see Debug#message(String, Throwable)
     */
    public void message(String msg) {
        getDebugServiceInstance().message(msg, null);
    }

    /**
     * <p>
     * Prints debug and exception messages only when the debug state is either
     * Debug.MESSAGE or Debug.ON. If the debug file is not accessible and
     * debugging is enabled, the message along with a time stamp and thread info
     * will be printed on <code>System.out</code>.
     * </p>
     * 
     * <p>
     * This method creates the debug file if does not exist; otherwise it starts
     * appending to the existing debug file. When invoked for the first time on
     * this object, the method writes a line delimiter of '*'s.
     * </p>
     * 
     * <p>
     * Note that the debug file will remain open until <code>destroy()</code>
     * is invoked. To conserve file resources, you should invoke
     * <code>destroy()</code> explicitly rather than wait for the garbage
     * collector to clean up.
     * </p>
     * 
     * <p>
     * <b>NOTE:</b> Debugging is an IO intensive operation and may hurt
     * application performance when abused. Particularly, note that Java
     * evaluates arguments to <code>message()</code> even when debugging is
     * turned off. It is recommended that the debug state be checked by invoking
     * <code>messageEnabled()</code> before invoking any
     * <code>message()</code> methods to avoid unnecessary argument evaluation
     * and to maximize application performance.
     * </p>
     * 
     * @param msg
     *            message to be printed. A newline will be appended to the
     *            message before printing either to <code>System.out</code> or
     *            to the debug file. If <code>msg</code> is null, it is
     *            ignored.
     * @param t
     *            <code>Throwable</code>, on which
     *            <code>printStackTrace</code> will be invoked to print the
     *            stack trace. If <code>t</code> is null, it is ignored.
     * @see Debug#error(String, Throwable)
     */
    public void message(String msg, Throwable t) {
        getDebugServiceInstance().message(msg, t);
    }

    /**
     * Prints warning messages only when debug level is greater than
     * Debug.ERROR.
     * 
     * <p>
     * <b>NOTE:</b> Debugging is an IO intensive operation and may hurt
     * application performance when abused. Particularly, note that Java
     * evaluates arguments to <code>warning()</code> even when debugging is
     * turned off. So when the argument to this method involves the String
     * concatenation operator '+' or any other method invocation,
     * <code>warningEnabled</code> <b>MUST</b> be used. It is recommended
     * that the debug state be checked by invoking <code>warningEnabled()</code>
     * before invoking any <code>warning()</code> methods to avoid unnecessary
     * argument evaluation and to maximize application performance.
     * </p>
     * 
     * @param msg
     *            message to be printed. A newline will be appended to the
     *            message before printing either to <code>System.out</code> or
     *            to the debug file. If <code>msg</code> is null, it is
     *            ignored.
     * 
     * @see Debug#warning(String, Throwable)
     */
    public void warning(String msg) {
        getDebugServiceInstance().warning(msg, null);
    }

    /**
     * Prints warning messages only when debug level is greater than
     * Debug.ERROR.
     * 
     * <p>
     * <b>NOTE:</b> Debugging is an IO intensive operation and may hurt
     * application performance when abused. Particularly, note that Java
     * evaluates arguments to <code>warning()</code> even when debugging is
     * turned off. It is recommended that the debug state be checked by invoking
     * <code>warningEnabled()</code> before invoking any
     * <code>warning()</code> methods to avoid unnecessary argument evaluation
     * and to maximize application performance.
     * </p>
     * 
     * <p>
     * If the debug file is not accessible and debugging is enabled, the message
     * along with a time stamp and thread info will be printed on
     * <code>System.out</code>.
     * </p>
     * 
     * <p>
     * This method creates the debug file if does not exist; otherwise it starts
     * appending to the existing debug file. When invoked for the first time on
     * this object, the method writes a line delimiter of '*'s.
     * </p>
     * 
     * <p>
     * Note that the debug file will remain open until <code>destroy()</code>
     * is invoked. To conserve file resources, you should invoke
     * <code>destroy()</code> explicitly rather than wait for the garbage
     * collector to clean up.
     * </p>
     * 
     * @param msg
     *            message to be printed. A newline will be appended to the
     *            message before printing either to <code>System.out</code> or
     *            to the debug file. If <code>msg</code> is null, it is
     *            ignored.
     * 
     * @param t
     *            <code>Throwable</code>, on which
     *            <code>printStackTrace()</code> will be invoked to print the
     *            stack trace. If <code>t</code> is null, it is ignored.
     */
    public void warning(String msg, Throwable t) {
        getDebugServiceInstance().warning(msg, t);
    }

    /**
     * Prints error messages only when debug level is greater than DEBUG.OFF.
     * 
     * @param msg
     *            message to be printed. A newline will be appended to the
     *            message before printing either to <code>System.out</code> or
     *            to the debug file. If <code>msg</code> is null, it is
     *            ignored.
     * 
     * @see Debug#error(String, Throwable)
     */
    public void error(String msg) {
        getDebugServiceInstance().error(msg, null);
    }

    /**
     * Prints error messages only if debug state is greater than Debug.OFF. If
     * the debug file is not accessible and debugging is enabled, the message
     * along with a time stamp and thread info will be printed on
     * <code>System.out</code>.
     * </p>
     * 
     * <p>
     * This method creates the debug file if does not exist; otherwise it starts
     * appending to the existing debug file. When invoked for the first time on
     * this object, the method writes a line delimiter of '*'s.
     * </p>
     * 
     * <p>
     * Note that the debug file will remain open until <code>destroy()</code>
     * is invoked. To conserve file resources, you should invoke
     * <code>destroy()</code> explicitly rather than wait for the garbage
     * collector to clean up.
     * </p>
     * 
     * @param msg
     *            message to be printed. A newline will be appended to the
     *            message before printing either to <code>System.out</code> or
     *            to the debug file. If <code>msg</code> is null, it is
     *            ignored.
     * 
     * @param t
     *            <code>Throwable</code>, on which
     *            <code>printStackTrace()</code> will be invoked to print the
     *            stack trace. If <code>t</code> is null, it is ignored.
     */
    public void error(String msg, Throwable t) {
        getDebugServiceInstance().error(msg, t);
    }

    /**
     * Sets the debug capabilities based on the values of the
     * <code>debugType</code> argument.
     * 
     * @param debugType
     *            is any one of five possible values:
     *            <ul>
     *            <li><code>Debug.OFF</code>
     *            <li><code>Debug.ERROR</code>
     *            <li><code>Debug.WARNING</code>
     *            <li><code>Debug.MESSAGE</code>
     *            <li><code>Debug.ON</code>
     *            </ul>
     */
    public void setDebug(int debugType) {
        getDebugServiceInstance().setDebug(debugType);
    }

    /**
     * Allows runtime modification of the backend used by this instance. 
     * by resetting the debug instance to reinitialize itself.
     * @param mf merge flag - on for creating a single debug file.
     */
    public void resetDebug(String mf) {
        getDebugServiceInstance().resetDebug(mf);
    }
    /**
     * Sets the debug capabilities based on the values of the
     * <code>debugType</code> argument.
     * 
     * @param debugType
     *            is any one of the following possible values:
     *            <ul>
     *            <li><code>Debug.STR_OFF</code>
     *            <li><code>Debug.STR_ERROR</code>
     *            <li><code>Debug.STR_WARNING</code>
     *            <li><code>Debug.STR_MESSAGE</code>
     *            <li><code>Debug.STR_ON</code>
     *            </ul>
     */
    public void setDebug(String debugType) {
        getDebugServiceInstance().setDebug(debugType);
    }

    /**
     * Destroys the debug object, closes the debug file and releases any system
     * resources. Note that the debug file will remain open until
     * <code>destroy()</code> is invoked. To conserve file resources, you
     * should invoke <code>destroy()</code> explicitly rather than wait for
     * the garbage collector to clean up.
     * 
     * <p>
     * If this object is accessed after <code>destroy()</code> has been
     * invoked, the results are undefined.
     * </p>
     */
    public void destroy() {
        // No handling required
    }

    /**
     * Setter for setting the actual debug service class which is obtained from
     * the configured provider.
     */
    private void setDebugServiceInstance(IDebug debugServiceInstance) {
        this.debugServiceInstance = debugServiceInstance;
    }

    /**
     * Returns the actual debug service class.
     * 
     * @return The underlying debug service class.
     */
    private IDebug getDebugServiceInstance() {
        return this.debugServiceInstance;
    }

    /**
     * The sole constructor of the Debug instances. This constructor is declared
     * private to ensure the use of the factory method provided in this class
     * called {@link #getInstance(String)}.
     */
    private Debug(IDebug debugServiceInstance) {
        setDebugServiceInstance(debugServiceInstance);
    }

    /* Static Initializer */
    static {
        initialize();
    }
}
