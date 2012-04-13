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
 * $Id: LogRecWrite.java,v 1.6 2009/06/19 02:33:29 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.log.service;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

import com.iplanet.dpro.parser.ParseOutput;
import com.iplanet.services.comm.share.Response;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.Logger;
import com.sun.identity.log.s1is.LogSSOTokenDetails;
import com.sun.identity.log.spi.Debug;
import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.monitoring.SsoServerLoggingHdlrEntryImpl;
import com.sun.identity.monitoring.SsoServerLoggingSvcImpl;

/**
 * This class implements <code>ParseOutput</code> interface and <code>
 * LogOperation</code> interface. It is parsing request and process the request.
 * log record. This class is registered with the SAX parser.
 */
public class LogRecWrite implements LogOperation, ParseOutput {
    
    String _logname;
    String _loggedBySid;
    Vector _records = new Vector();
    
    /**
     * Return result of the request processing in <code>Response</code>
     * @return result of the request processing in <code>Response</code>
     */
    public Response execute() {
        Response res = new Response("OK");
        SsoServerLoggingSvcImpl slsi = null;
        SsoServerLoggingHdlrEntryImpl slei = null;
        if (MonitoringUtil.isRunning()) {
            slsi = Agent.getLoggingSvcMBean();
            slei = slsi.getHandler(SsoServerLoggingSvcImpl.REMOTE_HANDLER_NAME);
        }
        
        Logger logger = (Logger)Logger.getLogger(_logname);
        if (Debug.messageEnabled()) {
            Debug.message("LogRecWrite: exec: logname = " + _logname);
        }
        
        Level level = 
            Level.parse(((com.sun.identity.log.service.LogRecord)_records.
        elementAt(0)).level);
        String msg = ((com.sun.identity.log.service.LogRecord)_records.
        elementAt(0)).msg;
        Map logInfoMap = ((com.sun.identity.log.service.LogRecord)_records.
        elementAt(0)).logInfoMap;
        Object [] parameters = 
            ((com.sun.identity.log.service.LogRecord)_records.
        elementAt(0)).parameters;
        
        try {
            msg = new String(com.sun.identity.shared.encode.Base64.decode(msg));
        } catch(RuntimeException ex){
            // if message is not base64 encoded just ignore & 
            // write msg as it is.
            if (Debug.messageEnabled()) {
                Debug.message("LogRecWrite: message is not base64 encoded");
            }
        }
        
        LogRecord rec = new LogRecord(level, msg);
        
        if (logInfoMap != null) {
            String loginIDSid = 
                (String)logInfoMap.get(LogConstants.LOGIN_ID_SID);
            if (loginIDSid != null && loginIDSid.length() > 0) {
                SSOToken loginIDToken = null;
                try {
                    SSOTokenManager ssom = SSOTokenManager.getInstance();
                    loginIDToken = ssom.createSSOToken(loginIDSid);
                } catch (SSOException e) {
                    if (Debug.warningEnabled()) {
                        Debug.warning("LogService::process(): SSOException", e);
                    }
                    rec.setLogInfoMap(logInfoMap);
                }
                if (loginIDToken != null){
                    // here fill up logInfo into the newlr
                    rec = LogSSOTokenDetails.logSSOTokenInfo(rec, loginIDToken);

                    // now take one be one values from logInfoMap and overwrite 
                    // any populated value from sso token.
                    Set keySet = logInfoMap.keySet();
                    Iterator i = keySet.iterator();
                    String key = null;
                    String value = null;
                    while (i.hasNext()) {
                        key = (String)i.next();
                        value = (String)logInfoMap.get(key);
                        if(value != null && value.length() > 0) {
                            if (key.equalsIgnoreCase(LogConstants.DATA)) {
                                try {
                                    value = new String(
                                   com.sun.identity.shared.encode.Base64.decode(
                                        value));
                                } catch(RuntimeException ex){
                                    // if message is not base64 encoded just 
                                    // ignore & write msg as it is.
                                    if (Debug.messageEnabled()) {
                                        Debug.message(
                                            "LogRecWrite: data is not "
                                            + "base64 encoded");
                                    }
                                }
                            }
                            rec.addLogInfo(key, value);
                        }
                    }
                }
            } else {
                rec.setLogInfoMap(logInfoMap);
            }
        }
        rec.setParameters(parameters);
        
        SSOToken loggedByToken = null;
        try {
            SSOTokenManager ssom = SSOTokenManager.getInstance();
            loggedByToken = ssom.createSSOToken(_loggedBySid);
        } catch (SSOException ssoe) {
            Debug.error("LogRecWrite: exec:SSOException: ", ssoe);
        }
        if (MonitoringUtil.isRunning()) {
            slei.incHandlerRequestCount(1);
        }
        logger.log(rec, loggedByToken);
        // Log file record write okay and return OK
        if (MonitoringUtil.isRunning()) {
            slei.incHandlerSuccessCount(1);
        }
        return res;
    }
    
    /**
     * The method that implements the ParseOutput interface. This is called
     * by the SAX parser.
     * @param name name of request
     * @param elems vaector has parsing elements
     * @param atts parsing attributes
     * @param pcdata given data to be parsed.
     */
    public void process(String name, Vector elems, Hashtable atts,
    String pcdata) { 
        
        _logname = ((Log) elems.elementAt(0))._logname;
        _loggedBySid = ((Log) elems.elementAt(0))._loggedBySid;
        
        for (int i = 1; i < elems.size(); i++) {
            com.sun.identity.log.service.LogRecord lr = 
                (com.sun.identity.log.service.LogRecord)elems.elementAt(i);
            _records.addElement(lr);
        }
    }
} //end of LogRecWrite
