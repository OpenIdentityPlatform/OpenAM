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

import java.util.Vector;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

/**
 * A queue of response messsages from the server. Multiple requests
 * can be multiplexed on the same queue. For synchronous LDAPConnection
 * requests, there will be only one request per queue. For asynchronous
 * LDAPConnection requests, the user can add multiple request to the
 * same queue.
 * 
 * Superclass for LDAResponseListener and LDAPSearchListener
 *
 */
abstract class LDAPMessageQueue implements java.io.Serializable {

    static final long serialVersionUID = -7163312406176592278L;

    public static final int LDAP_RESPONSE_LISTENER = 0;
    public static final int LDAP_SEARCH_LISTENER = 1;
    
    /**
     * Request entry encapsulates request parameters
     */
    static class RequestEntry {
        int id;
        LDAPConnection connection;        
        LDAPConnThread connThread;        
        long timeToComplete;        
    
        RequestEntry(int id, LDAPConnection connection,
            LDAPConnThread connThread, int timeLimit) {
            this.id= id;
            this.connection = connection;
            this.connThread = connThread;            
            if (timeLimit > 0) {
                this.timeToComplete = System.currentTimeMillis() + timeLimit;
            } else {
                this.timeToComplete = 0;
            }
        }    
    }

    /**
     * Internal variables
     */
    protected LDAPException m_exception; /* For network errors */
    protected volatile boolean m_asynchOp;
    
    /**
     * Constructor
     * @param asynchOp a boolean flag  that is true if the object is used 
     * for asynchronous LDAP operations
     * @see com.sun.identity.shared.ldap.LDAPAsynchronousConnection
     */   
    //okok
    LDAPMessageQueue (boolean asynchOp) {
        m_asynchOp = asynchOp;
    }

    /**
     * Returns a flag whether the listener is used for asynchronous LDAP
     * operations
     * @return asynchronous operation flag.
     * @see com.sun.identity.shared.ldap.LDAPAsynchronousConnection
     */
    //okok
    public boolean isAsynchOp() {
        return m_asynchOp;
    }

    /**
     * Blocks until a response is available.
     * Used by LDAPConnection.sendRequest (synch ops) to test if the server
     * is really available after a request had been sent.
     * @exception LDAPException Network error exception
     * @exception LDAPInterruptedException The invoking thread was interrupted
     */
    protected abstract void waitFirstMessage(int msgId) throws LDAPException;
    
    /**
     * Queues the LDAP server's response.  This causes anyone waiting
     * in nextMessage() to unblock.
     * @param msg response message
     */
    protected abstract void addMessage(LDAPMessage msg);

    /**
     * Signals that a network exception occured while servicing the
     * request.  This exception will be throw to any thread waiting
     * in nextMessage()
     * @param connThread LDAPConnThread on which the exception occurred
     * @param e exception
     */
    public synchronized void setException (LDAPConnThread connThread,
        LDAPException e) {        
        m_exception = e;        
        removeAllRequests(connThread);        
    }

    /**
     * Returns the count of queued messages
     * @return message count.
     */
    public abstract int getMessageCount();

    /**
     * Resets the state of this object, so it can be recycled.
     * Used by LDAPConnection synchronous operations.
     * @see com.sun.identity.shared.ldap.LDAPConnection#getResponseListener
     * @see com.sun.identity.shared.ldap.LDAPConnection#getSearchListener
     */
    protected abstract void reset();
    
    /**
     * Registers a LDAP request
     * @param id LDAP request message ID
     * @param connection LDAP Connection for the message ID
     * @param connThread a physical connection to the server
     * @param timeLimit the maximum number of milliseconds to wait for
     * the request to complete 
    */
    protected abstract void addRequest(int id, LDAPConnection connection, LDAPConnThread connThread, int timeLimit);
    
    /**
     * Remove request with the specified ID
     * Called when a LDAP operation is abandoned (called from
     * LDAPConnThread), or terminated (called by nextMessage() when
     * LDAPResponse message is received) 
     * @return flag indicating whether the request was removed.
     */
    protected abstract boolean removeRequest(int id);

    /**
     * Remove all requests associated with the specified connThread
     * Called when a connThread has a network error
     * @return number of removed requests.
     */
    protected abstract int removeAllRequests(LDAPConnThread connThread);

    /**
     * String representation of the object
     */
    public abstract String toString();
    
    public abstract int getType();
}
