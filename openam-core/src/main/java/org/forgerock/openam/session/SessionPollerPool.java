/*
* The contents of this file are subject to the terms of the Common Development and
* Distribution License (the License). You may not use this file except in compliance with the
* License.
*
* You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
* specific language governing permission and limitations under the License.
*
* When distributing Covered Software, include this CDDL Header Notice in each file and include
* the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
* Header, with the fields enclosed by brackets [] replaced by your own identifying
* information: "Portions copyright [year] [name of copyright owner]".
*
* Copyright 2015 ForgeRock AS.
*/
package org.forgerock.openam.session;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.am.util.ThreadPool;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;

import javax.inject.Singleton;

import static org.forgerock.openam.session.SessionConstants.*;

/**
 * Singleton holding the threadpool running the amSessionPoller.
 */
@Singleton
public class SessionPollerPool {

    private ThreadPool threadPool = null; //configured on first-use by #initPollerPool()

    private boolean cacheBasedPolling =
            SystemProperties.getAsBoolean("com.iplanet.am.session.client.polling.cacheBased", false);

    private static Debug debug = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    private static SessionPollerPool instance;
    /**
     * ClientSDK: Static initialisation required for non-Guice usage.
     *
     * @return A singleton SessionPollerPool instance.
     */
    public static synchronized SessionPollerPool getInstance() {
        if (instance == null) {
            instance = new SessionPollerPool();
        }
        return instance;
    }

    // Hidden default constructor to enforce singleton.
    private SessionPollerPool() {
    }

    /**
     * Indicates whether to enable or disable the session cleanup thread.
     */
    private boolean sessionCleanupEnabled =
            SystemProperties.getAsBoolean("com.iplanet.am.session.client.cleanup.enable", true);

    /**
     * Indicates whether session to use polling or notifications to clear the
     * client cache.
     */
    private boolean pollingEnabled = false;

    /**
     * Indicates whether the polling thread pool has been successfully prepared for use.
     */
    private boolean pollerPoolInitialized = false;

    /**
     * Checks if Polling is enabled and initializes the threadpool if it's not already configured.
     *
     * @return <code> true if polling is enabled , <code>false<code> otherwise.
     */
    public boolean isPollingEnabled(){
        // This is only a transitional solution before the complete
        // implementation for making the session properties
        // hot-swappable is in place
        if (!SystemProperties.isServerMode()) {
            pollingEnabled = SystemProperties.getAsBoolean(ENABLE_POLLING_PROPERTY, false);
        }

        if (debug.messageEnabled()) {
            debug.message("Session.isPollingEnabled is " + pollingEnabled);
        }

        if (!pollerPoolInitialized){
            initPollerPool();
        }

        return pollingEnabled;
    }

    /**
     * Configures the threadpool, and registers the threadpool with the shutdown manager so that it correctly
     * shuts down when the server is brought down.
     */
    private synchronized void initPollerPool() {
        if (!pollerPoolInitialized) {
            if (pollingEnabled) {
                int poolSize = SystemProperties.getAsInt(Constants.POLLING_THREADPOOL_SIZE, DEFAULT_POOL_SIZE);
                int threshold = SystemProperties.getAsInt(Constants.POLLING_THREADPOOL_THRESHOLD, DEFAULT_THRESHOLD);
                final ShutdownManager shutdownMan = com.sun.identity.common.ShutdownManager.getInstance();
                threadPool = new ThreadPool("amSessionPoller", poolSize, threshold, true, debug);
                shutdownMan.addShutdownListener(
                        new ShutdownListener() {
                            public void shutdown() {
                                threadPool.shutdown();
                                threadPool = null;
                                pollerPoolInitialized = false;
                            }
                        }
                );
                pollerPoolInitialized = true;
            } else {
                if (debug.messageEnabled()) {
                    debug.message("Session Cache cleanup is set to " + sessionCleanupEnabled);
                }
            }
        }
    }

    public ThreadPool getThreadPool() {
        return threadPool;
    }

    public boolean getCacheBasedPolling() {
        return cacheBasedPolling;
    }

    public boolean isSessionCleanupEnabled() {
        return sessionCleanupEnabled;
    }

}
