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
 * $Id: Stats.java,v 1.5 2008/08/08 00:40:59 ww203982 Exp $
 *
 */

package com.sun.identity.shared.stats;

import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.common.TimerPool;
import com.sun.identity.shared.Constants;
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
import java.util.ResourceBundle;
import java.util.Vector;

// NOTE: Since JVM specs guarantee atomic access/updates to int variables
// (actually all variables except double and long), the design consciously
// avoids synchronized methods, particularly for message(). This is done to
// reduce the performance overhead of synchronized message() when statistics
// is disabled. This does not have serious side-effects other than an occasional
// invocation of message() missing concurrent update of 'statsState'.

/*******************************************************************************
 * <p>
 * Allows a uniform interface to statistics information in a uniform format.
 * <code>Stats</code> supports different states of filing stats information:
 * <code>OFF</code>, <code>FILE</code> and <code>CONSOLE</code>. <BR>
 * <li> <code>OFF</code> statistics is turned off.
 * <li> <code>FILE</code> statistics information is written to a file
 * <li> <code>CONSOLE</code> statistics information is written on console
 * <p>
 * Stats service uses the property file, <code>AMConfig.properties</code>, to
 * set the default stats level and the output directory where the stats files
 * will be placed. The properties file is located (using
 * {@link java.util.ResourceBundle} semantics) from one of the directories in
 * the CLASSPATH.
 * <p>
 * The following keys are used to configure the Stats service. Possible values
 * for the key 'state' are: off | off | file | console The key 'directory'
 * specifies the output directory where the stats files will be created.
 * 
 * <blockquote>
 * 
 * <pre>
 *  com.iplanet.services.stats.state
 *  com.iplanet.services.stats.directory
 * </pre>
 * 
 * </blockquote>
 * 
 * If there is an error reading or loading the properties, all the information
 * is redirected to <code>System.out</code>
 * 
 * If these properties are changed, the server must be restarted for the changes
 * to take effect.
 * 
 * <p>
 * <b>NOTE:</b> Printing Statistics is an IO intensive operation and may hurt
 * application performance when abused. Particularly, note that Java evaluates
 * the arguments to <code>message()</code> and <code>warning()</code> even
 * when statistics is turned off. It is recommended that the stats state be
 * checked before invoking any <code>message()</code> or
 * <code>warning()</code> methods to avoid unnecessary argument evaluation and
 * to maximize application performance.
 * </p>
 * @supported.all.api
 */
public class Stats implements ShutdownListener {
    /** flags the disabled stats state. */
    public static final int OFF = 0;

    /**
     * Flags the state where all the statistic information is printed to a file
     */
    public static final int FILE = 1;

    /**
     * Flags the state where printing to a file is disabled. All printing is
     * done on System.out.
     */
    public static final int CONSOLE = 2;

    /**
     * statsMap is a container of all active Stats objects. Log file name is the
     * key and Stats is the value of this map.
     */
    private static Map statsMap = new HashMap();

    /** serviceInitialized indicates if the service is already initialized. */
    private static boolean serviceInitialized = false;

    private static DateFormat dateFormat;

    /**
     * The default stats level for the entire service and the level that is used
     * when a Stats object is first created and before its level is modified.
     * Don't initialize the following two variables in a static
     * initializer/block because other components may initialize Stats in their
     * static initializers (as opposed to constructors or methods). The order of
     * execution of static blocks is not guaranteed by JVM. So if we set the
     * following two static variables to some default values here, then it will
     * interfere with the execution of {@link #initService}.
     */
    private static String defaultStatsLevel;

    private static String outputDirectory;

    private final String statsName;

    private PrintWriter statsFile = null;

    private int statsState;

    private static StatsRunner statsListeners = new StatsRunner();

    /**
     * Initializes the Stats service so that Stats objects can be created. At
     * startup (when the first Stats object is ever created in a JVM), this
     * method reads <code>AMConfig.properties</code> file (using
     * {@link java.util.ResourceBundle} semantics) from one of the directories
     * in the <code>CLASSPATH</code>, and loads the properties. It creates
     * the stats directory. If all the directories in output dir don't have
     * adequate permissions then the creation of the stats directory will fail
     * and all the stats files will be located in the "current working
     * directory" of the process running stats code. If there is an error
     * reading or loading the properties, it will set the stats service to
     * redirect all stats information to <code>System.out</code>
     */
    private static void initService() {
        /*
         * We will use the double-checked locking pattern. Rarely entered block.
         * Push synchronization inside it. This is the first check.
         */
        if (!serviceInitialized) {
            /*
             * Only 1 thread at a time gets past the next point. Rarely executed
             * synchronization statement and hence synchronization penalty is
             * not paid every time this method is called.
             */
            synchronized (Stats.class) {
                /*
                 * If a second thread was waiting to get here, it will now find
                 * that the instance has already been initialized, and it will
                 * not re-initialize the instance variable. This is the (second)
                 * double-check.
                 */
                if (!serviceInitialized) {
                    dateFormat = new SimpleDateFormat(
                            "MM/dd/yyyy hh:mm:ss:SSS a zzz");
                    try {
                        defaultStatsLevel = SystemPropertiesManager.get(
                            Constants.SERVICES_STATS_STATE);
                        outputDirectory = SystemPropertiesManager.get(
                            Constants.SERVICES_STATS_DIRECTORY);
                        ResourceBundle bundle = 
                            com.sun.identity.shared.locale.Locale
                                .getInstallResourceBundle("amUtilMsgs");
                        if (outputDirectory != null) {
                            File createDir = new File(outputDirectory);
                            if (!createDir.exists()) {
                                if (!createDir.mkdirs()) {
                                    System.err.println(bundle.getString(
                                           "com.iplanet.services.stats.nodir"));
                                }
                            }

                        }
                    } catch (MissingResourceException e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace();

                        // If there is any error in getting the level or
                        // outputDirectory, defaultStatsLevel will be set to
                        // ON so that output will go to
                        // System.out

                        defaultStatsLevel = "console";
                        outputDirectory = null;
                    } catch (SecurityException se) {
                        System.err.println(se.getMessage());
                    }
                    
                    SystemTimer.getTimer().schedule(statsListeners, new Date(((
                        System.currentTimeMillis() +
                        statsListeners.getRunPeriod()) / 1000) * 1000));

                    serviceInitialized = true;
                }
            }
        }
    }

    /**
     * This constructor takes as an argument the name of the stats file. The
     * stats file is neither created nor opened until the first time
     * <code>message()</code>, <code>warning()</code> or
     * <code>error()</code> is invoked and the stats state is neither
     * <code>OFF</code> nor <code>ON</code>.
     * <p>
     * <b>NOTE:</b>The recommended and preferred method to create Stats objects
     * is <code>getInstance(String)</code>. This constructor may be
     * deprecated in future.
     * </p>
     * 
     * @param statsFileName
     *            name of the stats file to create or use
     */
    private Stats(String statsName) {
        // Initialize the stats service the first time a Stats object is
        // created.

        initService();

        // Now initialize this instance itself

        this.statsName = statsName;
        setStats(defaultStatsLevel);

        synchronized (statsMap) {
            // explicitly ignore any duplicate instances.
            statsMap.put(statsName, this);
        }
        ShutdownManager shutdownMan = ShutdownManager.getInstance();
        if (shutdownMan.acquireValidLock()) {
            try {
                shutdownMan.addShutdownListener(this);
            } finally {
                shutdownMan.releaseLockAndNotify();
            }
        }
    }

    /**
     * Returns an existing instance of Stats for the specified stats file or a
     * new one if no such instance already exists. If a Stats object has to be
     * created, its level is set to the level defined in the
     * <code>AMConfig.properties</code> file. The level can be changed later
     * by using {@link #setStats(int)} or {@link #setStats(String)}
     * 
     * @param statsName
     *            name of statistic instance.
     * @return an existing instance of Stats for the specified stats file.
     */
    public static synchronized Stats getInstance(String statsName) {
        Stats statsObj = (Stats) statsMap.get(statsName);
        if (statsObj == null) {
            statsObj = new Stats(statsName);
        }
        return statsObj;
    }

    /**
     * Checks if statistics is enabled.
     * 
     * <p>
     * <b>NOTE:</b> It is recommended that <code>isEnabled()</code> be used
     * instead of <code>isEnabled()</code> as the former is more intuitive.
     * 
     * @return <code>true</code> if statistics is enabled <code>false</code>
     *         if statistics is disabled
     * 
     */
    public boolean isEnabled() {
        return (statsState > Stats.OFF);
    }

    /**
     * Returns one of the 3 possible values.
     * <ul>
     * <li><code>Stats.OFF</code>
     * <li><code>Stats.FILE</code>
     * <li><code>Stats.CONSOLE</code>
     * </ul>
     * 
     * @return state of Stats.
     */
    public int getState() {
        return statsState;
    }

    /**
     * Prints messages only when the stats state is either
     * <code>Stats.FILE</code> or <code>Stats.CONSOLE</code>.
     * 
     * <p>
     * <b>NOTE:</b> Printing Statistics is an IO intensive operation and may
     * hurt application performance when abused. Particularly, note that Java
     * evaluates arguments to <code>message()</code> even when statistics is
     * turned off. So when the argument to this method involves the String
     * concatenation operator '+' or any other method invocation,
     * <code>isEnabled</code> <b>MUST</b> be used. It is recommended that the
     * stats state be checked by invoking <code>isEnabled()</code> before
     * invoking any <code>message()</code> methods to avoid unnecessary
     * argument evaluation and maximize application performance.
     * </p>
     * 
     * @param msg
     *            message to be recorded.
     */
    public void record(String msg) {
        if (statsState > Stats.OFF) {
            formatAndWrite(null, msg);
        }
    }

    private void formatAndWrite(String prefix, String msg) {
        if (statsState == Stats.CONSOLE) {
            if (msg != null) {
                if (prefix == null) {
                    System.out.println(msg);
                } else {
                    System.out.println(prefix + msg);
                }
            }
            return;
        }

        // The default capacity of StringBuffer in StringWriter is 16, but we
        // know for sure that the minimum header size is about 35. Hence, to
        // avoid reallocation allocate at least 160 chars.

        String serverInstance = System.getProperty("server.name");
        StringWriter swriter = new StringWriter(160);
        PrintWriter buf = new PrintWriter(swriter, true);
        synchronized (dateFormat) {
            buf.write(dateFormat.format(new Date()));
        }
        if ((serverInstance != null) && (serverInstance != "")) {
            buf.write(": ");
            buf.write("Server Instance: " + serverInstance);
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
        buf.flush();

        write(swriter.toString());
    }

    /**
     * Actually writes to the stats file. If it cannot write to the stats file,
     * it turn off statistics. The first time this method is invoked on a Stats
     * object, that object's stats file is created/opened in the directory
     * specified by the
     * <code>property com.iplanet.services.stats.directory</code> in the
     * properties file, <code>AMConfig.properties</code>.
     */
    private synchronized void write(String msg) {
        try {
            // statistics is enabled.
            // First, see if the statsFile is already open. If not, open it now.

            if (statsFile == null) {
                // open file in append mode
                FileOutputStream fos = new FileOutputStream(outputDirectory
                        + File.separator + statsName, true);
                statsFile = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(fos, "UTF-8")), true); 

                statsFile.println("*********************************" +
                        "*********************");
            }

            statsFile.println(msg);
        } catch (IOException e) {
            System.err.println(msg);

            // turn off statistics because statsFile is not accessible
            statsState = Stats.OFF;
        }
    }

    /**
     * Sets the stats capabilities based on the values of the
     * <code>statsType</code> argument.
     * 
     * @param statsType
     *            is any one of five possible values:
     *            <p>
     *            <code>Stats.OFF</code>
     *            <p>
     *            <p>
     *            <code>Stats.FILE</code>
     *            <p>
     *            <p>
     *            <code>Stats.CONSOLE</code>
     *            <p>
     */
    public void setStats(int statsType) {
        switch (statsType) {
        case Stats.OFF:
        case Stats.FILE:
        case Stats.CONSOLE:
            statsState = statsType;
            break;

        default:
            // ignore invalid statsType values
            break;
        }
    }

    /**
     * Sets the <code>stats</code> capabilities based on the values of the
     * <code>statsType</code> argument.
     * 
     * @param statsType
     *            is any one of the following possible values:
     *            <p>
     *            off - statistics is disabled
     *            </p>
     *            <p>
     *            file - statistics are written to the stats file
     *            <code>System.out</code>
     *            </p>
     *            <p>
     *            console - statistics are written to the stats to the console
     */
    public void setStats(String statsType) {
        if (statsType == null) {
            return;
        } else if (statsType.equalsIgnoreCase("console")) {
            statsState = Stats.CONSOLE;
        } else if (statsType.equalsIgnoreCase("file")) {
            statsState = Stats.FILE;
        } else if (statsType.equalsIgnoreCase("off")) {
            statsState = Stats.OFF;
        } else if (statsType.equals("*")) {
            statsState = Stats.CONSOLE;
        } else {
            if (statsType.endsWith("*")) {
                statsType = statsType.substring(0, statsType.length() - 1);
            }
            if (statsName.startsWith(statsType)) {
                statsState = Stats.CONSOLE;
            }
        }
    }

    /**
     * Destroys the stats object, closes the stats file and releases any system
     * resources. Note that the stats file will remain open until
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
        finalize();
    }
    
    public void shutdown() {
        finalize();
    }

    /** Flushes and then closes the stats file. */
    protected void finalize() {
        synchronized (statsMap) {
            statsMap.remove(statsName);
        }

        synchronized (this) {
            if (statsFile == null) {
                return;
            }

            statsState = Stats.OFF;
            statsFile.flush();
            statsFile.close();
            statsFile = null;
        }
    }

    public void addStatsListener(StatsListener listener) {
        statsListeners.addElement(listener);
    }
}
