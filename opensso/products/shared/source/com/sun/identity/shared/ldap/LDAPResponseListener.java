/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * The contents of this file are subject to the Netscape Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/NPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is Netscape
 * Communications Corporation.  Portions created by Netscape are
 * Copyright (C) 1999 Netscape Communications Corporation. All
 * Rights Reserved.
 *
 * Contributor(s): 
 */
package com.sun.identity.shared.ldap;


/**
 * Represents the message queue associated with a particular LDAP
 * operation or operations.
 * 
 */
public class LDAPResponseListener extends LDAPMessageQueue {

    static final long serialVersionUID = 901897097111294329L;

    private RequestEntry request;
    private LDAPResponse response;
    private boolean notified;
    private boolean hasListener;
    
    /**
     * Constructor
     * @param asynchOp a boolean flag that is true if the object is used for 
     * asynchronous LDAP operations
     * @see com.sun.identity.shared.ldap.LDAPAsynchronousConnection
     */
    LDAPResponseListener(boolean asynchOp) {
        super(asynchOp);
        this.hasListener = false;
        this.notified = false;
    }
    
    protected void addRequest(int id, LDAPConnection connection,
        LDAPConnThread connThread, int timeLimit) {
        synchronized (this) {
            request = new RequestEntry(id, connection, connThread, timeLimit);
            response = null;
        }
    }
    
    protected void addMessage(LDAPMessage msg) {
        synchronized (this) {
            if ((request != null) && (msg.getMessageID() == request.id)) {
                response = (LDAPResponse) msg;
                if (isAsynchOp() && msg.getType() == msg.BIND_RESPONSE) {
                    if (response.getResultCode() == 0) {
                        request.connection.setBound(true);                        
                    }                
                }                        
                if (hasListener) {
                    notified = true;
                    notify();
                }
            }
        }        
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer("LDAPResponseListener:");
        sb.append(" requestIDs={");        
        synchronized (this) {
            sb.append(request == null ? "" : String.valueOf(request.id));
            sb.append("} messageCount=");
            sb.append(String.valueOf((response == null ? 0 : 1)));
        }                
        return sb.toString();
    }
    
    protected boolean removeRequest(int id) {
        synchronized (this) {
            if ((request != null) && (request.id == id)) {
                request = null;
                response = null;
                if (hasListener) {
                    notified = true;
                    notify();
                }
                return true;
            }
        }
        return false;
    }
    
    protected int removeAllRequests(LDAPConnThread connThread) {
        synchronized (this) {
            if ((request != null) && (request.connThread == connThread)) {
                request = null;
                response = null;
                if (hasListener) {
                    notified = true;
                    notify();
                }
                return 1;
            }
        }
        return 0;
    }
    
    protected void reset() {
        synchronized (this) {
            m_exception = null;
            request = null;
            response = null;            
            if (hasListener) {
                notified = true;
                notify();
            }
        }
    }
    
    public int getMessageCount() {
        synchronized (this) {
            if (response != null) {
                return 1;
            } else {
                return 0 ;
            }
        }
    }
    
    protected void waitFirstMessage (int msgId) throws LDAPException {
        synchronized (this) {
            if (!hasListener) {
                hasListener = true;
                while ((request != null) && (request.id == msgId) &&
                    (m_exception == null) && (response == null)) {
                    waitForMessage();
                }        
                hasListener = false;
                // Network exception occurred ?
                if (m_exception != null) {
                    LDAPException ex = m_exception;
                    m_exception = null;
                    throw ex;
                }
            } else {
                //?
                throw new LDAPException();
            }
        }
    }
    
    private void waitForMessage () throws LDAPException {
        try {
            if (request.timeToComplete > 0) {
                long timeToWait = request.timeToComplete -
                    System.currentTimeMillis();
                if (timeToWait > 0) {
                    wait(timeToWait);
                    if (!notified) { 
                        request = null;
                        throw new LDAPException(
                            "Time to complete operation exceeded",
                            LDAPException.LDAP_TIMEOUT);
                    } else {
                        notified = false;
                    }
                } else {
                    request = null;
                    throw new LDAPException(
                        "Time to complete operation exceeded",
                        LDAPException.LDAP_TIMEOUT);
                }
            } else {
                wait();
            }
        } catch (InterruptedException e) {
            throw new LDAPInterruptedException("Interrupted LDAP operation");
        }
    }    
    
    public int getType() {
        return LDAPMessageQueue.LDAP_RESPONSE_LISTENER;
    }
    
    /**
     * Blocks until a response is available, or until all operations
     * associated with the object have completed or been canceled, and
     * returns the response.
     *
     * @return a response for an LDAP operation or null if there are no
     * more outstanding requests.
     * @exception LDAPException Network error exception
     * @exception LDAPInterruptedException The invoking thread was interrupted
     */
    public LDAPResponse getResponse() throws LDAPException {
        LDAPResponse tempResponse = null;
        synchronized (this) {
            if (!hasListener) {
                hasListener = true;       
                while ((request != null) && (m_exception == null) &&
                    (response == null)) {                                
                    waitForMessage();                    
                }                            
                hasListener = false;
                if (m_exception != null) {
                    LDAPException ex = m_exception;
                    m_exception = null;
                    throw ex;
                }
                if (request == null) {
                    return null;
                }
                tempResponse = response;
                response = null;
                request = null;                
            } else {
                //?
                throw new LDAPException();
            }
        }
        return tempResponse;
    }
        
}
