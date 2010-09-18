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

import java.util.*;
import com.sun.identity.shared.ldap.client.*;

/**
 * Manages search results, references and responses returned on one or 
 * more search requests
 *
 */
public class LDAPSearchListener extends LDAPMessageQueue {

    static final long serialVersionUID = -7163312406176592277L;               
    
    class ExtendedRequestEntry extends RequestEntry {
        
        boolean gotFirstMessage;
        boolean completed;
        
        ExtendedRequestEntry(int id, LDAPConnection connection,
            LDAPConnThread connThread, int timeLimit) {
            super(id, connection, connThread, timeLimit);
            gotFirstMessage = false;
            completed = false;
        }
    }
    
    // this instance variable is only for cache purpose
    private Long m_key = null;
    private LDAPSearchConstraints m_constraints;

    protected boolean notified;    
    protected boolean hasListener;    
    protected /*LDAPMessage*/ Vector m_messageQueue;
    protected /*RequestEntry*/ Hashtable m_requestList;
    private ExtendedRequestEntry currentWaitRequest;
    
    /**
     * Constructs a LDAP search listener.
     * @param asynchOp a boolean flag indicating whether the object is used 
     * for asynchronous LDAP operations
     * @param cons LDAP search constraints
     * @see com.sun.identity.shared.ldap.LDAPAsynchronousConnection
     */    
    LDAPSearchListener ( boolean asynchOp, LDAPSearchConstraints cons ) {
        super ( asynchOp );
        m_messageQueue = new Vector();
        if (asynchOp) {
            m_requestList = new Hashtable();
        }
        currentWaitRequest = null;
        this.notified = false;        
        this.hasListener = false;
        m_constraints = cons;
    }

    protected void addRequest(int id, LDAPConnection connection,
        LDAPConnThread connThread, int timeLimit) {                
        ExtendedRequestEntry request = new ExtendedRequestEntry(id,
            connection, connThread, timeLimit);        
        synchronized (this) {
            if (m_asynchOp) {
                m_requestList.put(new Integer(id), request);
                if ((currentWaitRequest == null) || (request.timeToComplete < 
                    currentWaitRequest.timeToComplete)) {
                    currentWaitRequest = request;
                    if (hasListener) {                    
                        notified = true;
                        notify();
                    }                
                }
            } else {                
                reset();               
                currentWaitRequest = request;
            }
        }
    }
    
    protected void addMessage(LDAPMessage msg) {                        
        synchronized (this) {            
            if (m_asynchOp) {
                synchronized (m_requestList) {
                    ExtendedRequestEntry request = null;        
                    if ((request = (ExtendedRequestEntry) m_requestList.get(
                        new Integer(msg.getMessageID()))) != null) {                    
                        request.gotFirstMessage = true;
                        if (msg.getMessageType() ==
                            LDAPMessage.LDAP_RESPONSE_MESSAGE) {
                            request.completed = true;
                        }
                        m_messageQueue.add(msg);
                        if (hasListener) {
                            notified = true;
                            notify();
                        }                
                    }                             
                }
            } else {                                                                
                if ((currentWaitRequest != null) && 
                    (currentWaitRequest.id == msg.getMessageID())) {
                    currentWaitRequest.gotFirstMessage = true;
                    if (msg.getMessageType() ==
                        LDAPMessage.LDAP_RESPONSE_MESSAGE) {
                            currentWaitRequest.completed = true;
                    }
                    m_messageQueue.add(msg);
                    if (hasListener) {
                        notified = true;
                        notify();
                    }           
                }
            }
        }
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer("LDAPSearchListener:");
        sb.append(" requestIDs={");        
        if (m_asynchOp) {
            synchronized (m_requestList) {            
                for (Iterator iter = m_requestList.keySet().iterator();
                    iter.hasNext();) {
                    sb.append(iter.next());
                    if (iter.hasNext()) {
                        sb.append(",");
                    }
                }                
            }                                    
        } else {
            synchronized (this) {
                if (currentWaitRequest != null) {
                    sb.append(currentWaitRequest.id);
                }
            }
        }
        sb.append("} messageCount="+m_messageQueue.size());
        return sb.toString();
    }
    
    protected boolean removeRequest(int msgId) {                        
        ExtendedRequestEntry entry;
        synchronized (this) {            
            if (m_asynchOp) {
                Integer id = new Integer(msgId);
                if ((entry = (ExtendedRequestEntry) m_requestList.remove(id))
                    != null) {                             
                    for (Iterator iter = m_messageQueue.iterator();
                        iter.hasNext();) {
                        LDAPMessage tempMsg = (LDAPMessage) iter.next();
                        if (tempMsg.getMessageID() == msgId) {
                            iter.remove();
                        }
                    }
                    if (currentWaitRequest == entry) {
                        currentWaitRequest = null;                        
                        if (hasListener) {
                            notified = true;
                            notify();
                        }
                    }                
                    return true;
                }
            } else {        
                if ((currentWaitRequest != null) && (currentWaitRequest.id ==
                    msgId)){
                    reset();
                    return true;
                }                                                                                                                                            
            }
        }
        return false;
    }
    
    protected int removeAllRequests(LDAPConnThread connThread) {
        int removeCount=0;                      
        boolean needToNotify = false;
        synchronized (this) {
            if (m_asynchOp) {
                synchronized (m_requestList) {
                    for (Iterator iter = m_requestList.values().iterator();
                        iter.hasNext();) {
                        ExtendedRequestEntry entry = (ExtendedRequestEntry)
                            iter.next();
                        if (connThread == entry.connThread) {                        
                            iter.remove();
                            removeCount++;
                            if (currentWaitRequest == entry) {
                                currentWaitRequest = null;
                                needToNotify = true;
                            }
                            for (Iterator jter = m_messageQueue.iterator();
                                jter.hasNext();) {
                                LDAPMessage tempMsg = (LDAPMessage) jter.next();
                                if (tempMsg.getMessageID() == entry.id) {
                                    jter.remove();
                                }                            
                            }                        
                        }                                    
                    }            
                }
                if (needToNotify) {                
                   if (hasListener) {
                        notified = true;
                        notify();
                    }
                }
            } else {
                if ((currentWaitRequest != null) && 
                    (currentWaitRequest.connThread == connThread)) {
                    removeCount = m_messageQueue.size();
                    reset();                    
                }
            }                                    
        }                    
        return removeCount;
    }
    
    public int getMessageCount() {
        synchronized (this) {
            return m_messageQueue.size();
        }
    }
    
    protected void waitFirstMessage (int msgId) throws LDAPException {                
        synchronized (this) {
            if (!hasListener) {
                hasListener = true;
                if (m_asynchOp) {
                    ExtendedRequestEntry request;
                    Integer id = new Integer(msgId);
                    while (((request = (ExtendedRequestEntry) m_requestList.get(
                        id)) != null) && (!request.gotFirstMessage) &&
                        (m_exception == null)) {
                        waitForMessage(request);
                    }        
                } else {
                    while ((currentWaitRequest != null) &&
                        (currentWaitRequest.id == msgId) &&
                        (!currentWaitRequest.gotFirstMessage) &&
                            (m_exception == null)) {
                        waitForMessage(currentWaitRequest);
                    }
                }
                hasListener = false;
                // Network exception occurred ?
                if (m_exception != null) {
                    LDAPException ex = m_exception;
                    m_exception = null;
                    throw ex;
                }
            } else {
                throw new LDAPException(
                        "Time to complete operation exceeded",
                        LDAPException.LDAP_TIMEOUT);
            }
        }                                
    }               
    
    private void waitForMessage(ExtendedRequestEntry entry) throws
        LDAPException {
        try {
            if (entry.timeToComplete > 0) {
                long timeToWait = entry.timeToComplete -
                    System.currentTimeMillis();
                if (timeToWait > 0) {
                    wait(timeToWait);
                    if (!notified) { 
                        removeRequest(entry.id);                        
                        throw new LDAPException(
                            "Time to complete operation exceeded",
                            LDAPException.LDAP_TIMEOUT);
                    } else {
                        notified = false;
                    }
                } else {
                    removeRequest(entry.id);                                            
                    throw new LDAPException(
                        "Time to complete operation exceeded",
                        LDAPException.LDAP_TIMEOUT);
                }
            } else {
                wait();
                notified = false;
            }
        } catch (InterruptedException e) {
            throw new LDAPInterruptedException("Interrupted LDAP operation");
        }
    }
    
    /**
     * Block until all results are in. Used for synchronous search with 
     * batch size of zero.
     * @return search response message.
     * @exception Network exception error
     */
    protected Vector completeSearchOperation(int msgId) throws
        LDAPException {
        Integer msgInt = new Integer(msgId);        
        synchronized (this) {
            if (!m_asynchOp) {
                if (!hasListener) {
                    hasListener = true;                
                    while ((currentWaitRequest != null) && (m_exception == null)
                        && (!currentWaitRequest.completed)) {
                        waitForMessage(currentWaitRequest);
                    }                    
                    // Network exception occurred ?
                    if (m_exception != null) {
                        LDAPException ex = m_exception;
                        m_exception = null;
                        hasListener = false;
                        throw ex;
                    }   
                    if (currentWaitRequest == null) {
                        hasListener = false;
                        throw new LDAPException("Invalid response",
                            LDAPException.OTHER);
                    }
                    Vector tempMessages = m_messageQueue;
                    LDAPMessage tempResponse = (LDAPMessage)
                        tempMessages.elementAt(tempMessages.size() - 1);
                    removeRequest(tempResponse.getMessageID());
                    hasListener = false;
                    return tempMessages;                    
                } else {
                    throw new LDAPException(
                        "Time to complete operation exceeded",
                        LDAPException.LDAP_TIMEOUT);
                }
            } else {
                throw new LDAPException(
                        "Time to complete operation exceeded",
                        LDAPException.LDAP_TIMEOUT);
            }
        }        
    }
    
    public LDAPMessage getResponse() throws LDAPException {
        LDAPMessage tempResponse = null;
        synchronized (this) {
            if (!hasListener) {
                hasListener = true;       
                if (m_asynchOp) {
                    while ((!m_requestList.isEmpty()) && (m_exception == null)
                        && m_messageQueue.isEmpty()) {
                        if (currentWaitRequest == null) {                        
                            synchronized (m_requestList) {
                                for (Iterator iter =
                                    m_requestList.values().iterator();
                                    iter.hasNext();) {
                                    ExtendedRequestEntry entry =
                                        (ExtendedRequestEntry) iter.next();
                                    if ((currentWaitRequest == null) ||
                                        (entry.timeToComplete <
                                        currentWaitRequest.timeToComplete)) {
                                        currentWaitRequest = entry;
                                    }
                                }
                            }
                        }                    
                        waitForMessage(currentWaitRequest);
                    }
                } else {
                    while ((currentWaitRequest != null) && (m_exception == null)
                        && m_messageQueue.isEmpty()) {
                        waitForMessage(currentWaitRequest);
                    }
                }
                hasListener = false;
                if (m_exception != null) {
                    LDAPException ex = m_exception;
                    m_exception = null;
                    throw ex;
                }
                if (m_asynchOp) {
                    if (m_requestList.isEmpty()) {
                        throw new LDAPException("Invalid response",
                            LDAPException.OTHER);
                    }  
                } else {
                    if (currentWaitRequest == null) {
                        throw new LDAPException("Invalid response",
                            LDAPException.OTHER);
                    }
                }
                tempResponse = (LDAPMessage) m_messageQueue.remove(0);
                if (tempResponse.getMessageType() ==
                    LDAPMessage.LDAP_RESPONSE_MESSAGE) {                    
                    removeRequest(tempResponse.getMessageID());                    
                }                
            } else {
                throw new LDAPException(
                        "Time to complete operation exceeded",
                        LDAPException.LDAP_TIMEOUT);
            }
        }
        return tempResponse;
    }
    
    public int getType() {
        return LDAPMessageQueue.LDAP_SEARCH_LISTENER;
    }


    /**
     * Returns message IDs for all outstanding requests
     * @return message ID array.
     */
    public int[] getMessageIDs() {
        int[] ids = null;        
        if (m_asynchOp) {
            synchronized (m_requestList) {
                ids = new int[m_requestList.size()];
                int i = 0;
                for (Iterator iter = m_requestList.keySet().iterator();
                    iter.hasNext();) {
                    ids[i] = ((Integer) iter.next()).intValue();
                    i++;
                }            
            }
        } else {
            synchronized (this) {
                if (currentWaitRequest != null) {
                    ids = new int[1];
                    ids[0] = currentWaitRequest.id;
                }
            }
            
        }        
        return ids;
    }
    
    public void merge(LDAPSearchListener mq2) throws LDAPException {
        
        // Yield just in case the LDAPConnThread is in the process of
        // dispatching a message
        if ((isAsynchOp()) && (mq2.isAsynchOp())) {
            synchronized (LDAPSearchListener.class) {
                synchronized(this) {            
                    synchronized (mq2) {                    
                        if (mq2.m_exception != null) {
                            m_exception = mq2.m_exception;
                        }                    
                        for (int i=0; i < mq2.m_messageQueue.size(); i++) {
                            m_messageQueue.addElement(
                                mq2.m_messageQueue.elementAt(i));
                        }
                        synchronized (mq2.m_requestList) {
                            for (Iterator iter =
                                mq2.m_requestList.values().iterator();
                                iter.hasNext();) {
                                RequestEntry entry = (RequestEntry) iter.next();
                                m_requestList.put(new Integer(entry.id), entry);
                                entry.connThread.changeListener(entry.id, this);
                            }
                        }                                        
                        mq2.reset();                    
                    }
                    if (hasListener) {
                        notified = true;
                        notify();
                    }
                }
            }
        } else {
            throw new LDAPException();
        }
    }
    
    /**
     * Return the search constraints used to create this object.
     * @return the search constraints used to create this object.
     */
    LDAPSearchConstraints getSearchConstraints() {
        return m_constraints;
    }

    /**
     * Set new search constraints object.
     * @param cons LDAP search constraints
     */
     void setSearchConstraints(LDAPSearchConstraints cons) {
        m_constraints = cons;
    }

    /**
     * Resets the state of this object, so it can be recycled.
     * Used by LDAPConnection synchronous operations.
     */
     protected void reset() {
         synchronized (this) {
            m_exception = null;
            if (m_asynchOp) {
                m_requestList.clear();
            }
            m_messageQueue.clear();
            currentWaitRequest = null;
            if (hasListener) {
                notified = true;
                notify();
            }
        }
     }
    
    /**
     * Set the key of the cache entry. The listener needs to know this value
     * when the results get processed in the queue. After the results have been
     * saved in the vector, then the key and a vector of results are put in
     * the cache.
     * @param key the key of the cache entry
     */
    void setKey(Long key) {
        m_key = key;
    }

    /**
     * Get the key of the cache entry.
     * @return the key of the cache entry.
     */
    Long getKey() {
        return m_key;
    }
}
