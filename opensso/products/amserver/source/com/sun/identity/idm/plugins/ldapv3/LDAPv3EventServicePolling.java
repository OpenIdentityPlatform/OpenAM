/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LDAPv3EventServicePolling.java,v 1.7 2009/12/22 19:11:55 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.idm.plugins.ldapv3;

import java.util.Date;
import java.util.Map;

import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPMessage;
import com.sun.identity.shared.ldap.LDAPResponse;
import com.sun.identity.shared.ldap.LDAPSearchResult;
import com.sun.identity.shared.ldap.LDAPSearchResultReference;

import com.sun.identity.common.PeriodicGroupMap;
import com.sun.identity.common.ScheduleableGroupAction;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.common.TaskRunnable;
import com.sun.identity.idm.IdRepoException;

/**
 * This class extends the LDAPv3EventService class and provides the
 * functionality to operate in a mode where it can be interrupted by the
 * LDAPv3TimeOut thread, typically when time outs are required. The time outs
 * are needed when the SDK is running behind a Load Balancer/Firewall as these
 * tend to drop idle connections on a periodic basis.
 * 
 * <p>
 * Whether or not the Event Service should operate in timeout mode is determined
 * by <code>com.sun.am.event.connection.idle.timeout</code>. An instance of
 * this class is instantiated when operating in the LB/Firewall time out mode by
 * making a <code>LDAPv3EventService.getInstance()</code> call.
 * </p>
 * 
 * <p>
 * The run method functionality has been designed to recover itself successfully
 * on the occurance of an Interrupt at any point in its run() cycle.
 * </p>
 * 
 * <p>
 * This thread will interrupt the LDAPv3TimeOut thread in 2 cases:
 * <ol>
 * <li>If interrupted by the LDAPv3TimeOut thread, this thread will reset the
 * connections that have timed out, then set new time out value for the
 * LDAPv3TimeOut thread and then notify the LDAPv3TimeOut thread by means of an
 * interrupt.</li>
 * <li>If a fatal exception (such as Server stop/down) is detected by this
 * thread. This thread will try to reset all its persistent searches and when
 * successful, it notifies the LDAPv3TimeOut thread by means of an interrupt.
 * </li>
 * </ol>
 * 
 * <p>
 * Both these threads (LDAPv3EventServicePolling & LDAPv3TimeOut) synchronize by
 * means of a monitor object (_monitor) shared between the 2 threads.
 * </p>
 */
public class LDAPv3EventServicePolling extends LDAPv3EventService {

    private final int IS_MESSAGE_PROCESSED = 0;
    
    private Map map;

    public synchronized void removeListener(String psIdKey) {
        // need to stop this process and the timeout monitor if no more request.
        if (debugger.messageEnabled()) {
            debugger.message("LDAPv3EventServicePolling.removeListener "
                + " psIdKey=" +  psIdKey);
        }
        super.removeListener(psIdKey);
    }

    protected LDAPv3EventServicePolling(Map pluginConfig, String serverNames)
            throws LDAPException {
        super(pluginConfig, serverNames);
        map = _requestList;
        _requestList = new PeriodicGroupMap(new ScheduleableGroupAction() {                        
                public void doGroupAction(Object obj) {
                Request req = (Request) map.remove(obj);
                // it should not be null, just for safety.
                if (req != null) {
                    try {
                        if (req.getStopStatus() == false) {
                            removeListener(req);
                            addListener(req.getRequester(), req.getListener(),
                                req.getBaseDn(), req.getScope(),
                                req.getFilter(), req.getOperations(),
                                req.getPluginConfig(), req.getOwner(),
                                req.getServerNames(), req.getPsIdKey());
                        }
                        
                        
                    } catch (IdRepoException le) {
                        // Something wrong with establishing connection. All
                        // searches need to be restared. Also reset timeout
                        // value back to original value
                        if (debugger.messageEnabled()) {
                            debugger.message(
                                "LDAPv3EventServicePolling.resetAllSearches(): "
                                + "IdRepException occurred while "
                                + "re-establishing listeners.  randomID=" +
                                randomID, le);
                        }
                        // this error is return by addListener only if the
                        // filter is not valid.
                        int errorCode = 87;
                        processExceptionErrorCodes(le, errorCode, false);
                    } catch (LDAPException e) {
                        // Probably psearch could not be established
                        debugger.error(
                            "LDAPv3EventServicePolling.resetAllSearches(): "
                            + "LDAPException occurred, while trying to " +
                            "re-establish persistent searches. randomID=" +
                            randomID, e);
                        
                        int errorCode = e.getLDAPResultCode();
                        processExceptionErrorCodes(e, errorCode, false);
                    }
                }
            }
        }, _idleTimeOutMills, _idleTimeOutMills, true, map);
        SystemTimer.getTimer().schedule((TaskRunnable) _requestList, new Date(((
            System.currentTimeMillis() + _idleTimeOutMills) / 1000) * 1000));
        if (debugger.messageEnabled()) {
            debugger.message("LDAPv3EventServicePolling.constructor()"
                    + " exit. randomID=" + randomID);
        }
    }

    protected String getName() {
        return "LDAPv3EventServicePolling";
    }

    /**
     * Method which process the Response received from the DS.
     * 
     * @param message -
     *            the LDAPMessage received as response
     * @return true if the reset was successful. False Otherwise.
     */    
    protected boolean processResponse(LDAPMessage message) {
        if ((message == null) && (!_requestList.isEmpty())) {
            // Some problem with the message queue. We should
            // try to reset it.
            debugger.warning("EventService.processResponse() - Received a "
                    + "NULL Response. Attempting to re-start persistent "
                    + "searches");
            resetErrorSearches(false);
            return true;
        }
        
        if (debugger.messageEnabled()) {
            debugger.message("LDAPv3EventService.processResponse() - received "
                    + "DS message  => " + message.toString() + " randomID="
                    + randomID);
        }

        // To determine if the monitor thread needs to be stopped.
        boolean successState = true;

        Request request = getRequestEntry(message.getMessageID());

        // If no listeners, abandon this message id
        if (request == null) {
            // We do not have anything stored about this message id.
            // So, just log a message and do nothing.
            if (debugger.messageEnabled()) {
                debugger.message(
                        "LDAPv3EventService.processResponse() - Received " +
                        "ldap message with unknown id = " + 
                        message.getMessageID() + " randomID=" + randomID);
            }
        } else if (message.getMessageType() ==
            LDAPMessage.LDAP_SEARCH_RESULT_MESSAGE) {
            // then must be a LDAPSearchResult carrying change control
            processSearchResultMessage((LDAPSearchResult) message, request);
            TaskRunnable taskList = (TaskRunnable) _requestList;
            taskList.removeElement(request.getRequestID());
            taskList.addElement(request.getRequestID());
            request.setLastUpdatedTime(System.currentTimeMillis());
        } else if (message.getMessageType() ==
            LDAPMessage.LDAP_RESPONSE_MESSAGE) {
            // Check for error message ...
            LDAPResponse rsp = (LDAPResponse) message;
            successState = processResponseMessage(rsp, request);
        } else if (message.getMessageType() ==
            LDAPMessage.LDAP_SEARCH_RESULT_REFERENCE_MESSAGE) { // Referral
            processSearchResultRef(
                    (LDAPSearchResultReference) message, request);
        }
        return successState;
    }

    private boolean processExceptionErrorCodes(Exception ex, int errorCode,
            boolean interrupt) {
        boolean successState = true;
        
        if (ex instanceof LDAPException) {
            LDAPException lex = (LDAPException) ex;
            String msg = lex.getLDAPErrorMessage();
            
            if ((errorCode == LDAPException.OTHER) &&
                (msg != null) && msg.equals("Invalid response")) {
                // We should not try to resetError and retry
                processNetworkError(ex);
            } else {
                if (_retryErrorCodes.contains("" + Integer.toString(errorCode))) {
                    // Call Only the parent method, because at this point we
                    // want to interrupt only if required.
                    resetErrorSearches(true);
                } else { // Some other network error
                    processNetworkError(ex);
                }
            }
        } else {
            if (_retryErrorCodes.contains("" + Integer.toString(errorCode))) {
                // Call Only the parent method, because at this point we
                // want to interrupt only if required.
                resetErrorSearches(true);
            } else { // Some other network error
                processNetworkError(ex);
            }
        }
        
        return successState;
    }

    protected Thread getServiceThread() {
        return _monitorThread;
    }
}

                        
