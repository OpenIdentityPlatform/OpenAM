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
 * $Id: RemoteHandler.java,v 1.18 2009/12/13 22:58:06 hvijay Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log.handlers;

import com.iplanet.am.util.ThreadPoolException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Handler;
import java.util.logging.Level;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.services.comm.client.PLLClient;
import com.iplanet.services.comm.share.Request;
import com.iplanet.services.comm.share.RequestSet;
import com.iplanet.services.comm.share.Response;
import com.iplanet.services.naming.URLNotFoundException;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.log.AMLogException;
import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManager;
import com.sun.identity.log.LogManagerUtil;
import com.sun.identity.log.ILogRecord;
import com.sun.identity.log.spi.Debug;

/**
 * The class which publishes the log message to a remote log service. Also
 * does buffering of LogRecords upto the number specified in the log
 * configuration. This buffer is emptied when the number of log records in the
 * buffer reaches the specified number or when the specified time
 * interval is exceeded.
 */
public class RemoteHandler extends Handler {
    private static LogManager manager;
    static {
        manager = (LogManager)LogManagerUtil.getLogManager();
    }
    
    private int recCount = 0;
    private String bufferSize;
    private int recCountLimit;
    // Map with loggedBySID as key & reqSet as value.
    // It's to make remote loggging work properly when buffering is enabled 
    // and log request is directed to different OpenSSO servers.
    private Map reqSetMap;
    private TimeBufferingTask bufferTask;
    private boolean timeBufferingEnabled = false;
    private String logName;
    private URL logServURL;
    private LoggingThread thread = LoggingThread.getInstance();
    
    private void configure() {
        
        bufferSize = manager.getProperty(LogConstants.BUFFER_SIZE);
        if (bufferSize != null && bufferSize.length() > 0) {
            try {
                recCountLimit = Integer.parseInt(bufferSize);
            } catch (NumberFormatException e) {
                recCountLimit = 1;
            }
        } else {
            recCountLimit = 1;
        }

        String status = manager.getProperty(LogConstants.TIME_BUFFERING_STATUS);

        if ( status != null && status.equalsIgnoreCase("ON"))
        {
            timeBufferingEnabled = true;
        }

        setLevel(Level.ALL);
        setFilter(null);

        String urlString =
            manager.getProperty(LogConstants.LOGGING_SERVICE_URL);
        try {
            logServURL = new URL(urlString);
        } catch (MalformedURLException mue) {
            if (Debug.warningEnabled()) {
                Debug.warning("RemoteHandler.getLogHostURL(): '" +
                    urlString + "' is malformed. " + mue.getMessage());
            }
        }
    }
    
    /**
     * Constructor in which the configuration of the handler is done.
     * @param dummyParam dummy parameter. 
     * To make instantation generic for all handlers.
     */
    public RemoteHandler(String dummyParam) {
        reqSetMap = new HashMap();
        configure();
        if (timeBufferingEnabled) {
            startTimeBufferingThread();
        }
        //Redundant instantiation of FlushTask for early class loading
        //and hence performance improvement.
        new FlushTask(reqSetMap);
    }
    
    /**
     * This method sends the LogRecord to the remote logging service.
     * @param logRecord The LogRecord to be published to the remote 
     *        logging service.
     */
    public synchronized void publish(java.util.logging.LogRecord logRecord) {
        logName = logRecord.getLoggerName();
        String xml = getFormatter().format(logRecord);
        if (xml == null || xml.length() <= 0 ) {
            if (Debug.warningEnabled()) {
                Debug.warning(logName + 
                    ":RemoteHandler.publish : formatted xml is null");
            }
            return;
        }
        Request request = new Request(xml);

        if (logRecord instanceof ILogRecord) {
            Map logInfoMap = ((ILogRecord) logRecord).getLogInfoMap();
            String loggedBySid = (String) logInfoMap.get(
                LogConstants.LOGGED_BY_SID);
            if (loggedBySid != null) {
                RequestSet reqSet = (RequestSet) reqSetMap.get(loggedBySid);
                if (reqSet == null) {
                    reqSet = new RequestSet("Logging");
                }
                reqSet.addRequest(request);
                reqSetMap.put(loggedBySid, reqSet);
            }
        }
        
        this.recCount++;
        if (this.recCount >= recCountLimit) {
            if (Debug.messageEnabled()) {
                Debug.message(logName + ":RemoteHandler.publish(): got " 
                    + recCount + " records, flushing all");
            }
            nonBlockingFlush();
        }
    }
    
    /**
     * Flushes any buffered output by calling flush(), and then close 
     * the handler and free all associated resources with this handler.
     */
    public void close() {
        flush();
        stopBufferTimer();
    }

    /**
     * Flush any buffered output.
     */
    public synchronized void flush() {
        if (recCount <= 0) {
            if (Debug.messageEnabled()) {
                Debug.message("RemoteHandler.flush(): no records " +
                                "in buffer to send");
            }
            return;
        }
        Vector responses = new Vector();
        if (Debug.messageEnabled()) {
            Debug.message("RemoteHandler.flush(): sending buffered records");
        }

        String thisAMException = null;
        try {
            Iterator sidIter = reqSetMap.keySet().iterator();
            while (sidIter.hasNext()) {
                String currentLoggedBySID = (String)sidIter.next();
                URL logHostURL = getLogHostURL(currentLoggedBySID);
                if (logHostURL == null) {
                    Debug.error("RemoteHandler.flush(): logHostURL is null");
                    this.recCount = 0;
                    reqSetMap = new HashMap();
                    return;
                }
                RequestSet reqSet = 
                    (RequestSet)reqSetMap.get(currentLoggedBySID);
                responses = PLLClient.send(logHostURL, reqSet);
                Iterator respIter = responses.iterator();
                while (respIter.hasNext()) {
                    Response resp = (Response)respIter.next();
                    String respContent = resp.getContent();
                    if (!respContent.equals("OK")) {
                        Debug.error("RemoteHandler.flush(): " + respContent 
                            + " on remote machine");
                        if (thisAMException == null) {
                            thisAMException = "RemoteHandler.flush(): " +
                            respContent + " on remote machine";
                        }
                    }
                }
            }
        } catch (Exception e) {
            Debug.error("RemoteHandler.flush(): " , e);
        }
        this.recCount = 0;
        reqSetMap = new HashMap();
        if (thisAMException != null) {
            throw new AMLogException(thisAMException);
        }
    }
   
    /**
     * Copy the existing request set map and pass it on to ThreadPool as part
     * of a FlushTask. Initiatize a new map as the new request set map for
     * future remote logging calls.
     */
    public synchronized void nonBlockingFlush() {
        if (recCount <= 0) {
            if (Debug.messageEnabled()) {
                Debug.message("RemoteHandler.nonBlockingFlush(): no records " +
                        "in buffer to send");
            }
            return;
        }

        FlushTask task = new FlushTask(reqSetMap);
        try {
            thread.run(task);
        } catch (ThreadPoolException ex) {
            //Use current thread to complete the task if ThreadPool can not
            //execute it.
            if (Debug.messageEnabled()) {
                Debug.message("RemoteHandler.nonBlockingFlush(): ThreadPoolException" +
                        ". Performing blocking flush.");
            }
            task.run();
        }
        this.recCount = 0;
        reqSetMap = new HashMap();
    }

 
    private URL getLogHostURL(String loggedBySID) {
        SessionID sid = new SessionID(loggedBySID);
        
        String sessionProtocol = sid.getSessionServerProtocol();
        String sessionHost = sid.getSessionServer();
        String sessionPort = sid.getSessionServerPort();
        String sessionURI = sid.getSessionServerURI();

        //
        //  if remote logging service and protocol, host, and port
        //  are null, get them from the logging service url in the
        //  AMConfig.properties file.
        //
        if ((!manager.isLocal) &&
            ((sessionProtocol == null) || (sessionProtocol.length() <= 0) ||
             (sessionHost == null) || (sessionHost.length() <= 0)))
        {
            if (Debug.messageEnabled()) {
                Debug.message("RemoteHandler.getLogHostURL(): remote serv = " +
                    logServURL);
            }
            return (logServURL);
        }
        
        if (Debug.messageEnabled()) {
            Debug.message("RemoteHandler.getLogHostURL(): " + 
                " sessionProtocol: " + sessionProtocol + 
                " sessionHost: " + sessionHost + 
                " sessionPort: " + sessionPort +
                " sessionURI: " + sessionURI);
        }
        URL loggingURL = null;
        try {
            loggingURL =  WebtopNaming.getServiceURL(
                LogConstants.LOGGING_SERVICE, 
                sessionProtocol, sessionHost, sessionPort, sessionURI);
            
            if (Debug.messageEnabled()) {
                Debug.message(
                    "RemoteHandler.getLogHostURL(): WebtopNaming logging"
                    + "service URL: " + loggingURL);
            }
        } catch (URLNotFoundException unfe) {
            Debug.error(
                "RemoteHandler.getLogHostURL(): URLNotFoundException: ", unfe);
            return null;
        }
        return loggingURL;
    }
    
    /**
     * This inner class is instantiated by the nonBlockingFlush() method to
     * create task for flushing out (asynchronously) the current buffer of
     * log record requests.
     */
    private class FlushTask implements Runnable {

        private Map<String, RequestSet> logReqsMap = null;

        FlushTask(Map<String, RequestSet> reqSetMap) {
            this.logReqsMap = reqSetMap;
        }
        
        public void run() {
            Vector responses = new Vector();
            if (Debug.messageEnabled()) {
                Debug.message("RemoteHandler.FlushTask.run(): " +
                        "sending buffered records");
            }

            String thisAMException = null;
            try {
                for(String currentLoggedBySID : logReqsMap.keySet()){
                    URL logHostURL = getLogHostURL(currentLoggedBySID);
                    if (logHostURL == null) {
                        Debug.error("RemoteHandler.FlushTask.run(): " +
                                "logHostURL is null");
                        return;
                    }
                    RequestSet reqSet =
                            (RequestSet) logReqsMap.get(currentLoggedBySID);
                    responses = PLLClient.send(logHostURL, reqSet);
                    Iterator respIter = responses.iterator();
                    while (respIter.hasNext()) {
                        Response resp = (Response) respIter.next();
                        String respContent = resp.getContent();
                        if (!respContent.equals("OK")) {
                            Debug.error("RemoteHandler.FlushTask.run(): " + 
                                    respContent + " on remote machine");
                            if (thisAMException == null) {
                                thisAMException =
                                        "RemoteHandler.FlushTask.run(): " +
                                        respContent + " on remote machine";
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Debug.error("RemoteHandler.FlushTask.run(): ", e);
            }

            if(thisAMException != null){
                throw new AMLogException(thisAMException);
            }
        }    
    }


    private class TimeBufferingTask extends GeneralTaskRunnable {
        
        private long runPeriod;
        
        public TimeBufferingTask(long runPeriod) {
            this.runPeriod = runPeriod;
        }
        
        /**
         * The method which implements the GeneralTaskRunnable.
         */
        public void run() {
            if (Debug.messageEnabled()) {
                Debug.message("RemoteHandler:TimeBufferingTask.run() called");
            }
            flush();
        }
        
        /**
         *  Methods that need to be implemented from GeneralTaskRunnable.
         */
        
        public boolean isEmpty() {
            return true;
        }
        
        public boolean addElement(Object obj) {
            return false;
        }
        
        public boolean removeElement(Object obj) {
            return false;
        }
        
        public long getRunPeriod() {
            return runPeriod;
        }
        
    }
    
    private void startTimeBufferingThread() {
        String period = manager.getProperty(LogConstants.BUFFER_TIME);
        long interval;
        if((period != null) || (period.length() != 0)) {
            interval = Long.parseLong(period);
        } else {
            interval = LogConstants.BUFFER_TIME_DEFAULT;
        }
        interval *= 1000;
        if(bufferTask == null){
            bufferTask = new TimeBufferingTask(interval);
            try {
                SystemTimer.getTimer().schedule(bufferTask, new Date(((
                    System.currentTimeMillis() + interval) / 1000) * 1000));
            } catch (IllegalArgumentException e) {
                Debug.error (logName + ":RemoteHandler:BuffTimeArg: " +
                    e.getMessage());
            } catch (IllegalStateException e) {
                if (Debug.messageEnabled()) {
                    Debug.message (logName + ":RemoteHandler:BuffTimeState: "
                        + e.getMessage());
                }
            }
            if (Debug.messageEnabled()) {
                Debug.message("RemoteHandler: Time Buffering Thread Started");
            }
        }
    }

    private void stopBufferTimer() {
        if(bufferTask != null) {
            bufferTask.cancel();
            bufferTask = null;
            if (Debug.messageEnabled()) {
                Debug.message("RemoteHandler: Buffer Timer Stopped");
            }
        }
    }
}
