/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SiteStatusCheckThreadImpl.java,v 1.7 2009/10/16 16:43:08 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2011-2013 ForgeRock AS
 * Portions Copyrighted 2013 Nomura Research Institute, Ltd
 */
package com.iplanet.services.naming;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.WebtopNaming.SiteStatusCheck;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.shared.Constants;

/**
 * The class implements <code>SiteStatusCheck</code> interface that provides
 * method that will be used by SiteMonitor to check each site is alive.
 */
public class SiteStatusCheckThreadImpl implements SiteStatusCheck {
    protected static Debug debug = Debug.getInstance("amNaming");
    private static int timeout = Long.valueOf(SystemProperties.
            get(Constants.MONITORING_TIMEOUT, "10000")).intValue();
    private static String hcPath = SystemProperties.
            get(Constants.URLCHECKER_TARGET_URL, null);
    private static int urlCheckerInvalidateInterval = 
    	    Long.valueOf(SystemProperties.
            get(Constants.URLCHECKER_INVALIDATE_INTERVAL, "70000")).intValue();
    private static int urlCheckerSleep = Long.valueOf(SystemProperties.
            get(Constants.URLCHECKER_SLEEP_INTERVAL, "30000")).intValue();
    private static int urlCheckerRetryInterval = Long.valueOf(SystemProperties.
            get(Constants.URLCHECKER_RETRY_INTERVAL, "500")).intValue();
    private static int urlCheckerRetryLimit = Long.valueOf(SystemProperties.
            get(Constants.URLCHECKER_RETRY_LIMIT, "3")).intValue();

    private HashMap urlCheckers = null;
    
    static {
        if (hcPath == null) {
            String deployuri = SystemProperties.get
                (Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR, "/openam");
            hcPath = deployuri + "/namingservice";
        }
        if (!hcPath.startsWith("/")) {
            hcPath = "/" + hcPath;
        }
    }

    /**
     * Constructs a SiteStatusCheckThreadImpl object based on the configured
     * parameter com.sun.identity.sitemonitor.SiteStatusCheck.class.
     */
    public SiteStatusCheckThreadImpl() {
        super();
        urlCheckers = new HashMap();
    }
    
    private String getThreadName(URL u) {
        return "Site-Monitor " +  u.toExternalForm();
    }
    
    private URLChecker getURLChecker(URL url) {
        URLChecker checker = (URLChecker)urlCheckers.get(getThreadName(url));
        if (checker == null) {
            synchronized(urlCheckers) {
                checker = (URLChecker)urlCheckers.get(getThreadName(url));
                if (checker != null) {
                     return checker;
                }
                checker = new URLChecker(url);
                urlCheckers.put(getThreadName(url), checker);
                checker.check();
            }
            SystemTimer.getTimer().schedule(checker, new Date(((
                System.currentTimeMillis() + urlCheckerSleep) / 1000) * 1000));
            synchronized(checker) {
                try {
                    checker.wait(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return (URLChecker)urlCheckers.get(getThreadName(url));
    }
    
    /**
     * The method that will be used by SiteMonitor to check each site is alive.
     * @param url that needs to be checked alive.
     */
    public boolean doCheckSiteStatus(URL url)
    {
        if (debug.messageEnabled()) {
            debug.message("SiteStatusCheckThreadImpl.doCheckSiteStatus: check "
                + url);
        }
        URLChecker checker = getURLChecker(url);
        if (checker != null && (checker.getStatus()
            == URLStatus.STATUS_UNKNOWN)) {
            synchronized(checker) {
                checker.cancel();
                checker.notify();
            }
            synchronized(urlCheckers) {
                urlCheckers.remove(getThreadName(url));
            }
            debug.error("SiteStatusCheckThreadImpl.doCheckSiteStatus() " 
                    + "Killing thread " + getThreadName(url));
            return false;
        } else if ((checker != null) &&  
            (checker.getStatus() == URLStatus.STATUS_AVAILABLE)) {
            return true;
        } else {
            return false;
        }
    }

    class URLStatus {
        public static final int STATUS_UNKNOWN = 0;
        public static final int STATUS_AVAILABLE = 1;
        public static final int STATUS_UNAVAILABLE = -1;
        private int status = STATUS_UNAVAILABLE;
        private Date lastStatusUpdatedTime = null;

        public URLStatus() {
            super();
            setStatus(STATUS_UNKNOWN);
            lastStatusUpdatedTime=Calendar.getInstance().getTime();
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
            if(getStatus() != STATUS_UNKNOWN) {
                lastStatusUpdatedTime=Calendar.getInstance().getTime();
            }
        }

        public Date getLastStatusUpdatedTime() {
            return lastStatusUpdatedTime;
        }
    }

    class URLChecker extends GeneralTaskRunnable {
        private URL url = null;
        private URLStatus urlStatus = null;
        
        URLChecker(URL url) {
            this.url = getHealthCheckURL(url);
            setUrlStatus(new URLStatus());
        }

        public void check() {
            int cnt = 0;
            boolean statusNotSet = true;
            while (cnt < urlCheckerRetryLimit && statusNotSet) {
                cnt++;
                try {
                    boolean sockStatus = checkSocketConnection(url);
                    if (!sockStatus) {
                        getUrlStatus().setStatus(URLStatus.STATUS_UNAVAILABLE);
                        Thread.sleep(urlCheckerRetryInterval);
                    } else {
                        Date t0 = null;
                        if (debug.messageEnabled()) {
                            t0 = Calendar.getInstance().getTime();
                        }

                        HttpURLConnection huc = 
                                HttpURLConnectionManager.getConnection(url);
                        huc.setDoInput(true);
                        huc.setRequestMethod("GET");
                        int responseCode = huc.getResponseCode();
                        
                        if (debug.messageEnabled()) {
                            Date t1 = Calendar.getInstance().getTime();
                            long t = t1.getTime() - t0.getTime();
                            debug.message("URLChecker.check() : " +
                                "Http connection took " + t + " ms");
                        }
                        
                        if (responseCode == 200) {
                            if (debug.messageEnabled()) {
                                debug.message("URLChecker.check() : " +
                                    " setting status to " +
                                	"AVAILABLE for " + url.toExternalForm());
                            }
                            statusNotSet = false;
                            getUrlStatus().setStatus(
                                URLStatus.STATUS_AVAILABLE);
                        } else {
                            if (debug.messageEnabled()) {
                                debug.message("URLChecker.check() : " +
                                    "setting status to " + 
                            	    "** UNAVAILABLE ** for " + 
                            	url.toExternalForm());
                            }
                            if (cnt == urlCheckerRetryLimit) {
                                getUrlStatus().setStatus(
                                        URLStatus.STATUS_UNAVAILABLE);
                            } else {
                                Thread.sleep(urlCheckerRetryInterval);
                            }
                        }
                        huc.disconnect();
                    }
                } catch (Exception e) {
                    debug.error("URLChecker.check() :  setting status to " +
                        "** UNAVAILABLE ** for " + url.toExternalForm(), e);
                    getUrlStatus().setStatus(URLStatus.STATUS_UNAVAILABLE);
                }
            }
        }

        public void run() {
            if (debug.messageEnabled()) {
                debug.message("URLChecker.run() : monitoring URL " +
                    url.toExternalForm());
            }
            check();
        }

        public long getRunPeriod() {
            return urlCheckerSleep;
        }

        public boolean isEmpty() {
            return true;
        }

        public boolean addElement(Object obj) {
            return false;
        }

        public boolean removeElement(Object obj) {
            return false;
        }

        private URLStatus getUrlStatus() {
            return urlStatus;
        }

        private void setUrlStatus(URLStatus urlStatus) {
            this.urlStatus = urlStatus;
        }

        public int getStatus() {
            if ((Calendar.getInstance().getTimeInMillis() - getUrlStatus()
                    .getLastStatusUpdatedTime().getTime()) > 
                    urlCheckerInvalidateInterval) {
                if (debug.messageEnabled()) {
                    debug.message("URLChecker.getStatus() : " + 
                    "Last status update was @ " + getUrlStatus()
                    .getLastStatusUpdatedTime());
                }
                return URLStatus.STATUS_UNKNOWN;
            } else {
                return getUrlStatus().getStatus();
            }
        }

        boolean checkSocketConnection(URL url) {
            boolean flag = false;
            try {
                InetSocketAddress inetsocketaddress = 
                	new InetSocketAddress(url.getHost(), url.getPort());
                Socket socket = new Socket();
                socket.connect(inetsocketaddress, timeout);
                socket.close();
                flag = true;
            } catch (IOException ioexception) {
                debug.error("URLChecker.checkSocketConnection() : " + 
               	    "Socket connection to " + url.toString() + " Failed : " 
              	    + ioexception.toString());
            }

            if (debug.messageEnabled()) {
                debug.message("URLChecker.checkSocketConnection() returning " 
                    + flag + " for " + url.toString());
            }
            
            return flag;
        }

        private URL getHealthCheckURL(URL u) {
            URL url = null;
            int port = u.getPort();
            String protocol = u.getProtocol();
            if (port == -1) {
            	port = protocol.equalsIgnoreCase("http") ? 80 : 443;
            }
            StringBuilder buff = new StringBuilder(protocol);
            buff.append("://").append(u.getHost()).append(":").append(port)
                    .append(hcPath);
            try {
                url= new URL(buff.toString());
            } catch (MalformedURLException e) {
                debug.error("URLChecker.getHealthCheckURL() : Incorrect URL : "
                    + e.toString());
            }
            
            return url;
        }
    }
    
}
