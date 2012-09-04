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
 * $Id: URLFailoverHelper.java,v 1.5 2008/06/25 05:51:42 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.agents.common;



import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.AgentServerErrorException;
import com.sun.identity.agents.arch.Module;
import com.sun.identity.agents.arch.SurrogateBase;
import com.sun.identity.agents.filter.AmFilterRequestContext;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;


/**
 * The class manages URL failover
 */
public class URLFailoverHelper extends SurrogateBase 
        implements IURLFailoverHelper 
{
    
    public URLFailoverHelper(Module module) {
        super(module);
    }

    public void initialize(
                boolean probeEnabled, 
                boolean isPrioritized, 
                long timeout,
                String[] urlList,
                Map<String, Set<String>> conditionalUrls) throws AgentException {
        if(urlList.length == 1) {
            if(isLogWarningEnabled()) {
                logWarning("URLFailoverHelper: Only one URL is specified, "
                           + "failover will be disabled");
            }

            markDisabled();
        }
        setPrioritized(isPrioritized);
        setProbeEnabled(probeEnabled);
        setTimeout(timeout);
        setURLList(urlList);
        setConditionalUrlList(conditionalUrls);
    }

    public String getAvailableURL(AmFilterRequestContext ctx) throws AgentException {
        return getAvailableURL(ctx.getHttpServletRequest());
    }

    public String getAvailableURL(HttpServletRequest request) throws AgentException {
        if ((_urlList == null) || (_urlList.length == 0)) {
            return null;
        }
        String domain = request.getServerName();
        Set<String> urls = conditionalUrls.get(domain);
        if (urls != null) {
            for (String url : urls) {
                if (isAvailable(url)) {
                    if(isLogMessageEnabled()) {
                        logMessage("URLFailoverHelper: conditional URL " + url
                                    + " is available");
                    }
                    return url;
                }
            }
        } else {
            if (isLogMessageEnabled()) {
                logMessage("URLFailoverHelper: No conditional URL found for "
                        + "domain: " + domain + " Falling back to non-conditional URLs.");
            }
        }
        String result = null;
        if(isEnabled()) {
            String url = getCurrentURL();
            if(isAvailable(url)) {
                result = url;
            } else {
                if(isLogWarningEnabled()) {
                    logWarning("URLFailoverHelper: Detected the failure of "
                               + url + ", initiating failover sequence");
                }
                int currentIndex = getCurrentIndex();
                int newIndex = currentIndex;
                boolean done = false;
                while( !done) {
                    String newURL = getURL(newIndex);
                    if(isAvailable(newURL)) {
                        if(isLogMessageEnabled()) {
                            logMessage("URLFailoverHelper: url " + newURL
                                       + " is available");
                        }
                        if(newIndex != currentIndex) {
                            updateIndex(newIndex, currentIndex);

                            result = newURL;
                        }

                        done = true;
                    } else {
                        newIndex = (newIndex + 1) % (getMaxIndex() + 1);

                        if(newIndex == currentIndex) {
                            logError(
                               "URLFailoverHelper: No URL is available at" 
                                    + " this time");

                            throw new AgentServerErrorException(
                                "No URL is available at this time");
                        }
                    }
                }
            }
        } else {
            result = getCurrentURL();

            if( !this.isAvailable(result)) {
                logError(
                    "URLFailoverHelper: No URL is available at this time");

                throw new AgentServerErrorException(
                    "No URL is available at this time");
            }
        }

        if(isLogMessageEnabled()) {
            logMessage("URLFailoverHelper: getAvailableURL() => " + result);
        }

        return result;
    }

    /**
     * Method updateIndex
     *
     *
     * @param newIndex
     * @param oldIndex
     *
     */
    private void updateIndex(int newIndex, int oldIndex) {

        boolean indexUpdated = false;
        if (!isPrioritized()) {
            if((newIndex != oldIndex) 
                    && (newIndex >= 0) && (newIndex <= getMaxIndex())) 
            {
                    synchronized(this) {
                        if(oldIndex == getCurrentIndex()) {
                            setCurrentIndex(newIndex);

                            indexUpdated = true;
                        }
                    }
                }
            if(isLogWarningEnabled()) {
                String oldURL = getURL(oldIndex);
                String newURL = getURL(newIndex);

                if(indexUpdated) {
                    logWarning("URLFailoverHelper: URL updated from " + oldURL
                               + " to " + newURL);
                }
            }
        }
    }

    private boolean isAvailable(String url) {

        boolean result = true;
       
        if (isProbeEnabled()) {
       
            ServerMonitor monitor = new ServerMonitor(url);
            result = monitor.isAvailable();
        }
        return result;
    }
    
    /**
     * Method setCurrentIndex
     *
     *
     * @param index
     *
     */
    private void setCurrentIndex(int index) {
        _index = index;
    }

    /**
     * Method getURL
     *
     *
     * @param index
     *
     * @return
     *
     */
    private String getURL(int index) {
        return _urlList[index];
    }

    /**
     * Method getCurrentURL
     *
     *
     * @return
     *
     */
    private String getCurrentURL() {
        return _urlList[getCurrentIndex()];
    }

    /**
     * Method getCurrentIndex
     *
     *
     * @return
     *
     */
    private int getCurrentIndex() {
        return _index;
    }

    /**
     * Method setURLList
     *
     *
     * @param urlList
     *
     */
    private void setURLList(String[] urlList) {
        _urlList = urlList;
    }

    private void setConditionalUrlList(Map<String, Set<String>> conditionalUrls) {
        this.conditionalUrls = conditionalUrls;
    }

    /**
     * Method getMaxIndex
     *
     *
     * @return
     *
     */
    private int getMaxIndex() {
        return _urlList.length - 1;
    }
    
    private void setPrioritized(boolean isPrioritized) {
        _isPrioritized = isPrioritized;
    }
    
    private boolean isPrioritized() {
        return _isPrioritized;
    }

    private void setProbeEnabled(boolean probeEnabled) {
        _probeEnabled = probeEnabled;
    }
    
    private boolean isProbeEnabled() {
        return _probeEnabled;
    }
    
    private void setTimeout(long timeout) {
        _timeout = timeout;
    }
    
    private long getTimeout() {
        return _timeout;
    }
    

    /**
     * Method isEnabled
     *
     *
     * @return
     *
     */
    private boolean isEnabled() {
        return !_disabled;
    }

    /**
     * Method markDisabled
     *
     *
     */
    private void markDisabled() {
        _disabled = true;
    }

    /*
     * This is a Runnable class used to proble login url's availablity.
     * It simulates adding a timeout value for HttpURLConnection.connect().
     * Since only JDK1.5 adds URLConnection.setConnectTimeout(), we can not 
     * use setConnectTimeout now until Agent can use JDK1.5.
     */
    class ServerMonitor implements Runnable {
        private boolean isAvailable = false;
        private String url = null;

        public ServerMonitor (String url) {
            this.url = url;
        }

        public boolean isAvailable () {

            try { 
                long timeout = getTimeout();
                Thread thread = new Thread(this);
                thread.start();
                thread.join(timeout);

                if (thread.isAlive()) {
                    thread.interrupt();
                    return false;
                }

            } catch (InterruptedException ex) {
                if(isLogWarningEnabled()) {
                   logWarning("URLFailoverHelper: the url " + url
                               + " is not available", ex);
                }
                return false;
            }

            return this.isAvailable;
        }

        public void run() {
            HttpURLConnection connection = null;

            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.connect();
                isAvailable  = true;

            } catch (IOException ex) {
                if(isLogWarningEnabled()) {
                    logWarning("URLFailoverHelper: the url " + url
                               + " is not available", ex);
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                    if (isLogMessageEnabled()) {
                        logMessage(
                           "URLFailoverHelper: disconnected the "
                            + "connection for availability check");
                    }
                }
            }
        }
    }    
    
    private String[] _urlList;
    private Map<String, Set<String>> conditionalUrls;
    private int      _index    = 0;
    private boolean  _disabled = false;
    private boolean _isPrioritized = false;
    private boolean _probeEnabled = false;
    private long _timeout = 2000;
}
