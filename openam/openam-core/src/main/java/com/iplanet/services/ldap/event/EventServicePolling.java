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
 * $Id: EventServicePolling.java,v 1.6 2009/01/28 05:34:50 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.ldap.event;

import java.util.Date;
import java.util.Map;

import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPMessage;
import com.sun.identity.shared.ldap.LDAPResponse;
import com.sun.identity.shared.ldap.LDAPSearchResult;
import com.sun.identity.shared.ldap.LDAPSearchResultReference;
import com.iplanet.services.ldap.LDAPServiceException;
import com.sun.identity.common.PeriodicGroupMap;
import com.sun.identity.common.ScheduleableGroupAction;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.common.TaskRunnable;

/**
 * This class extends the EventService class and provides the functionality to
 * operate in a mode where it can be interrupted by the TimeOut thread,
 * typically when time outs are required. The time outs are needed when the SDK
 * is running behind a Load Balancer/Firewall as these tend to drop idle
 * connections on a periodic basis.
 * 
 * <p>
 * Whether or not the Event Service should operate in timeout mode is determined
 * by <code>com.sun.am.event.connection.idle.timeout</code>. An instance of
 * this class is instantiated when operating in the LB/Firewall time out mode by
 * making a <code>EventService.getInstance()</code> call.
 * </p>
 * 
 * <p>
 * The run method functionality has been designed to recover itself successfully
 * on the occurance of an Interrupt at any point in its run() cycle.
 * </p>
 * 
 * <p>
 * This thread will interrupt the TimeOut thread in 2 cases:
 * <ol>
 * <li>If interrupted by the TimeOut thread, this thread will reset the
 * connections that have timed out, then set new time out value for the TimeOut
 * thread and then notify the TimeOut thread by means of an interrupt.</li>
 * <li>If a fatal exception (such as Server stop/down) is detected by this
 * thread. This thread will try to reset all its persistent searches and when
 * successful, it notifies the TimeOut thread by means of an interrupt.</li>
 * </ol>
 * 
 */
public class EventServicePolling extends EventService {

    private Map map;

    protected EventServicePolling() throws EventException {
        super();
        map = _requestList;
        _requestList = new PeriodicGroupMap(new ScheduleableGroupAction() {
            public void doGroupAction(Object obj) {
                Request req = (Request) map.remove(obj);
                // it should not be null, just for safety.
                if (req != null) {
                    try {
                        addListener(req.getRequester(), req.getListener(),
                            req.getBaseDn(), req.getScope(), req.getFilter(),
                            req.getOperations());
                        removeListener(req);
                    } catch (LDAPServiceException le) {
                        // Something wrong with establishing connection. All searches need
                        // to be restared. Also reset timeout value back to original value
                        if (debugger.messageEnabled()) {
                            debugger.message("EventServicePolling: "
                                + " LDAPServiceException occurred while re-establishing"
                                + "listeners. ", le);
                        }
                        int errorCode = le.getLDAPExceptionErrorCode();
                        processExceptionErrorCodes(le, errorCode);
                    } catch (LDAPException e) {// Probably psearch could not be established
                        if (debugger.messageEnabled()) {
                            debugger.message("EventServicePolling.resetAllSearches(): "
                                + "LDAPException occurred, while trying to re-establish "
                                + "persistent searches.", e);
                        }
                        int errorCode = e.getLDAPResultCode();
                        processExceptionErrorCodes(e, errorCode);
                    }
                }
            }
        }, _idleTimeOutMills, _idleTimeOutMills, true, map);
        SystemTimer.getTimer().schedule((TaskRunnable) _requestList, new Date(((
            System.currentTimeMillis() + _idleTimeOutMills) / 1000) * 1000));
    }

    protected static String getName() {
        return "EventServicePolling";
    }

    private boolean processExceptionErrorCodes(Exception ex, int errorCode) {
            
        boolean successState = true;
        if (_retryErrorCodes.contains(Integer.toString(errorCode))) {
            // Call Only the parent method, because at this point we
            // want to interrupt only if required.
            resetErrorSearches(true);
        } else { // Some other error
            processNetworkError(ex);
        }
        return successState;
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
            debugger.message("EventService.processResponse() - received "
                    + "DS message  => " + message.toString());
        }

        // To determine if the monitor thread needs to be stopped.
        boolean successState = true;

        Request request = getRequestEntry(message.getMessageID());

        // If no listeners, abandon this message id
        if (request == null) {
            // We do not have anything stored about this message id.
            // So, just log a message and do nothing.
            if (debugger.messageEnabled()) {
                debugger.message("EventService.processResponse() - Received "
                        + "ldap message with unknown id = "
                        + message.getMessageID());
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

    protected Thread getServiceThread() {
        return _monitorThread;
    }
}
