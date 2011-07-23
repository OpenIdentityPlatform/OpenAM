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
import com.sun.identity.shared.ldap.client.opers.*;
import com.sun.identity.shared.ldap.ber.stream.*;
import com.sun.identity.shared.ldap.util.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import com.sun.identity.shared.debug.Debug;

/**
 * Multiple LDAPConnection clones can share a single physical connection,
 * which is maintained by a thread.
 *
 *   +----------------+
 *   | LDAPConnection | --------+
 *   +----------------+         |
 *                              |
 *   +----------------+         |        +----------------+
 *   | LDAPConnection | --------+------- | LDAPConnThread |
 *   +----------------+         |        +----------------+
 *                              |
 *   +----------------+         |
 *   | LDAPConnection | --------+
 *   +----------------+
 *
 * All LDAPConnections send requests and get responses from
 * LDAPConnThread (a thread).
 */

class LDAPConnThread implements Runnable {

    /**
     * Constants
     */
    private final static int MAXMSGID = Integer.MAX_VALUE;
    private final static int BACKLOG_CHKCNT = 50;

    /**
     * Internal variables
     */
    transient private static int m_highMsgId;
    transient private InputStream m_serverInput, m_origServerInput;
    transient private OutputStream m_serverOutput, m_origServerOutput;
    transient private Hashtable m_requests;
    transient private Hashtable m_messages = null;
    transient private Set m_registered;       
    transient private LDAPCache m_cache = null;
    transient private Thread m_thread = null;
    transient private Object m_sendRequestLock = new Object();
    transient private LDAPConnSetupMgr m_connMgr = null;
    transient private Object m_traceOutput = null;
    transient private int m_backlogCheckCounter = BACKLOG_CHKCNT;
    transient private volatile boolean m_bound;

    /**
     * Connection IDs for ldap trace messages
     */
    transient private static int m_nextId;
    transient private int m_id;
    

    // Time Stemp format Hour(0-23):Minute:Second.Milliseconds used for trace msgs
    static SimpleDateFormat m_timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        
    /**
     * Constructs a connection thread that maintains connection to the
     * LDAP server.
     * @param connMgr the connection setup manager
     * @param cache cache object or null
     * @param traceOutput trace object or null
     */
    public LDAPConnThread(LDAPConnSetupMgr connMgr, LDAPCache cache,
        Object traceOutput) {
        m_requests = new Hashtable ();
        m_registered = Collections.synchronizedSet(new HashSet());
        m_connMgr = connMgr;
        setCache( cache );
        setTraceOutput(traceOutput);
    }

    synchronized void connect(LDAPConnection ldc) throws LDAPException{

        if (m_thread != null) {
            return;
        }

        try {
            Socket socket = m_connMgr.openConnection();
            m_serverInput  = new BufferedInputStream (
                socket.getInputStream());
            m_serverOutput = new BufferedOutputStream(
                socket.getOutputStream());
            register(ldc);
        } catch (IOException e) {
            throw new LDAPException ( "failed to connect to server " +
                  m_connMgr.getHost(), LDAPException.CONNECT_ERROR );
        }

        m_id = m_nextId++;        
        String url = m_connMgr.getLDAPUrl().getServerUrl();

        if (m_traceOutput != null) {
            StringBuffer sb = new StringBuffer(" Connected to ");
            sb.append(url);
            logTraceMessage(sb);
        }

        String threadID = "LDAPConnThread-" + m_id + " " + url;
        m_thread = new Thread(this, threadID);
        m_thread.setDaemon(true);
        m_thread.start();
    }

    public synchronized String toString() {
        if (m_thread != null) {
            return m_thread.getName();
        }
        else {
            return "LDAPConnThread-" + m_id + " <disconnected>";
        }
    }

    /**
     * Layer a SSL socket over the current non-SSL one
     */
    protected void layerSocket(LDAPTLSSocketFactory factory) throws Exception {
        synchronized (m_sendRequestLock) {
            try {
                Socket socket = m_connMgr.layerSocket(factory);                                
                setInputStream(socket.getInputStream());
                setOutputStream(socket.getOutputStream());
            }
            catch (Exception e) {
                m_serverInput  = m_origServerInput;
                m_serverOutput = m_origServerOutput;
                throw e;
            }
        }
    }

    protected void setBound(boolean bound) {
        m_bound = bound;
    }

    protected boolean isBound() {
        return (m_thread != null) && m_bound;
    }

    InputStream getInputStream() {
        return m_serverInput;
    }

    void setInputStream( InputStream is ) {
        m_serverInput = is;
    }

    OutputStream getOutputStream() {
        return m_serverOutput;
    }

    void setOutputStream( OutputStream os ) {
        m_serverOutput = os;
    }

    int getRequestCount() {
        return m_requests.size();
    }

    void setTraceOutput(Object traceOutput) {
        synchronized (m_sendRequestLock) {
            if (traceOutput == null) {
               m_traceOutput = null;
            }
            else if (traceOutput instanceof OutputStream) {
                m_traceOutput = new PrintWriter((OutputStream)traceOutput);
            }
            else if (traceOutput instanceof LDAPTraceWriter) {
                m_traceOutput = traceOutput;
            }
        }            
    }
    
    void logTraceMessage(StringBuffer msg) {

        String timeStamp = m_timeFormat.format(new Date());
        StringBuffer sb = new StringBuffer(timeStamp);
        sb.append(" ldc="); 
        sb.append(m_id);

        synchronized( m_sendRequestLock ) {
            if (m_traceOutput instanceof PrintWriter) {
                PrintWriter traceOutput = (PrintWriter)m_traceOutput;
                traceOutput.print(sb); // header
                traceOutput.println(msg);
                traceOutput.flush();
            }
            else if (m_traceOutput instanceof LDAPTraceWriter) {
                sb.append(msg);
                ((LDAPTraceWriter)m_traceOutput).write(sb.toString());
            }
        }
    }
    
    /**
     * Set the cache to use for searches.
     * @param cache The cache to use for searches; <CODE>null</CODE> for no cache
     */
    synchronized void setCache( LDAPCache cache ) {
        m_cache = cache;
        m_messages = (m_cache != null) ? new Hashtable() : null;
    }

    /**
     * Allocates a new LDAP message ID.  These are arbitrary numbers used to
     * correlate client requests with server responses.
     * @return new unique msgId
     */
    private static synchronized int allocateId () {
            m_highMsgId = (m_highMsgId + 1) % MAXMSGID;
            return m_highMsgId;
    }


    /**
     * Sends LDAP request via this connection thread.
     * @param request request to send
     * @param toNotify response listener to invoke when the response
     *          is ready
     */
    protected int sendRequest (LDAPConnection conn, JDAPProtocolOp request,
        LDAPMessageQueue toNotify, LDAPConstraints cons)
         throws LDAPException {
        
        if (m_thread == null) {
            throw new LDAPException ( "Not connected to a server",
                                       LDAPException.SERVER_DOWN );
        }

        int msgId = allocateId();                
        LDAPMessage msg = 
            new LDAPMessage(msgId, request, cons.getServerControls());        
        if ( toNotify != null ) {
            /* Only worry about toNotify if we expect a response... */
            m_requests.put (new Integer (msgId), toNotify);

            /* Notify the backlog checker that there may be another outstanding
               request */
            resultRetrieved(); 

            toNotify.addRequest(msgId, conn, this, cons.getTimeLimit());
        }
        if (!sendRequest(msg, /*ignoreErrors=*/false)) {
            throw new LDAPException("Server or network error",
                                     LDAPException.SERVER_DOWN);
        }
        return msgId;
    }


    protected int sendRequest (LDAPConnection conn, LDAPRequest request,
        LDAPMessageQueue toNotify, LDAPConstraints cons)
         throws LDAPException {

        if (m_thread == null) {
            throw new LDAPException ( "Not connected to a server",
                                       LDAPException.SERVER_DOWN );
        }

        int msgId = allocateId();
        if ( toNotify != null ) {
            /* Only worry about toNotify if we expect a response... */
            m_requests.put (new Integer (msgId), toNotify);

            /* Notify the backlog checker that there may be another outstanding
               request */
            resultRetrieved();

            toNotify.addRequest(msgId, conn, this, cons.getTimeLimit());
        }
        if (!sendRequest(msgId, request, cons.getServerControls(),
            /*ignoreErrors=*/false)) {
            throw new LDAPException("Server or network error",
                                     LDAPException.SERVER_DOWN);
        }
        return msgId;
    }

    private boolean sendRequest (LDAPMessage msg, boolean ignoreErrors) {
        synchronized( m_sendRequestLock ) {
            try {
                if (m_traceOutput != null) {
                    logTraceMessage(msg.toTraceString());
                }                   
                msg.write (m_serverOutput);
                m_serverOutput.flush();
                return true;
            }
            catch (IOException e) {
                if (!ignoreErrors) {
                    networkError(e);
                }
            }
            catch (NullPointerException e) {
                // m_serverOutput is null bacause of network error
                if (!ignoreErrors) {
                    if (m_thread != null) {
                        throw e;
                    }
                }
            }
            return false;
        }
    }

    private boolean sendRequest (int msgId, LDAPRequest request,
        LDAPControl controls[], boolean ignoreErrors) {
        synchronized( m_sendRequestLock ) {
            try {
                if (m_traceOutput != null) {
                    //logTraceMessage(msg.toTraceString());
                }
                int totalLength = 0;
                byte[] controlsLength = null;
                if (controls != null) {
                    // calculate total controls length
                    for (int i = 0; i < controls.length; i++) {
                        totalLength += controls[i].getBytesSize();
                    }                
                    // parse length for controls
                    controlsLength = LDAPRequestParser.getLengthBytes(
                        totalLength);
                    // controls length + contorls length length + tag
                    totalLength += (controlsLength.length + 1);
                }
                // controls + content length
                totalLength += request.getBytesSize();
                byte[] msgIdBytes = LDAPRequestParser.getIntBytes(msgId);
                byte[] msgIdLength = LDAPRequestParser.getLengthBytes(
                    msgIdBytes.length);
                // controls + content length + msg id length
                totalLength += (msgIdBytes.length + msgIdLength.length + 1);
                byte[] totalMessageLength = LDAPRequestParser.getLengthBytes(
                    totalLength);
                // write sequence tag
                m_serverOutput.write(BERElement.SEQUENCE);
                // write total message length
                m_serverOutput.write(totalMessageLength);
                // write msg id tag
                m_serverOutput.write(BERElement.INTEGER);
                // write msg id length
                m_serverOutput.write(msgIdLength);
                // write msg int
                m_serverOutput.write(msgIdBytes);                      
                // write content
                LinkedList requestContents = request.getBytesLinkedList();
                for (Iterator iter = requestContents.iterator();
                    iter.hasNext();) {
                    m_serverOutput.write((byte[]) iter.next());
                }
                if (controls != null) {
                    // write controls tag
                    m_serverOutput.write(BERElement.CONTEXT |
                        BERElement.CONSTRUCTED | 0);
                    // write controls length
                    m_serverOutput.write(controlsLength);
                    // write controls content
                    for (int i = 0; i < controls.length; i++) {
                        LinkedList controlBytes =
                            controls[i].getBytesLinkedList();
                        for (Iterator iter = controlBytes.iterator();
                            iter.hasNext();) {
                            m_serverOutput.write((byte[]) iter.next());
                        }
                    }
                }
                m_serverOutput.flush();
                return true;
            }
            catch (IOException e) {
                if (!ignoreErrors) {
                    networkError(e);
                }
            }
            catch (NullPointerException e) {
                // m_serverOutput is null bacause of network error
                if (!ignoreErrors) {
                    if (m_thread != null) {
                        throw e;
                    }
                }
            }
            return false;
        }
    }

    private int sendUnbindRequest(LDAPControl[] ctrls) {
        int msgId = allocateId();
        LDAPMessage msg = 
            new LDAPMessage(msgId, new JDAPUnbindRequest(), ctrls);
        sendRequest(msg, /*ignoreErrors=*/true);
        return msgId;
    }
    
    private int sendAbandonRequest(int id, LDAPControl[] ctrls) {
        int msgId = allocateId();
        LDAPMessage msg = 
            //new LDAPMessage(allocateId(), new JDAPAbandonRequest(id), ctrls);
            new LDAPMessage(msgId, new JDAPAbandonRequest(id), ctrls);
        sendRequest(msg, /*ignoreErrors=*/true);
        return msgId;
    }

    /**
     * Register with this connection thread.
     * @param conn LDAP connection
     */
    public synchronized void register(LDAPConnection conn) {
        m_registered.add(conn);
    }

    int getClientCount() {
        return m_registered.size();
    }

    boolean isConnected() {
        return m_thread != null;
    }

    /**
     * De-Register with this connection thread. If all the connection
     * is deregistered. Then, this thread should be killed.
     * @param conn LDAP connection
     */
    /**
     * De-Register with this connection thread. If all the connection
     * is deregistered. Then, this thread should be killed.
     * @param conn LDAP connection
     */
    synchronized void deregister(LDAPConnection conn) {

        if (m_thread == null) {
            return;
        }
        
        m_registered.remove(conn);
        if (m_registered.size() == 0) {
            
            // No more request processing
            Thread t = m_thread;               
            m_thread = null;

            try {
                // Notify the server
                sendUnbindRequest(conn.getConstraints().getServerControls());
                
                // interrupt the thread
                try {
                    t.interrupt();
                    wait(500);
                }
                catch (InterruptedException ignore) {}
                 
            } catch (Exception e) {
                LDAPConnection.printDebug(e.toString());
            }
            finally {
                cleanUp(null);
            }
        }
    }


    /**
     * Clean up after the thread shutdown.
     * The list of registered clients m_registered is left in the current state
     * to enable the clients to recover the connection.
     */
    private void cleanUp(LDAPException ex) {

        resultRetrieved();

        try {
            m_serverOutput.close ();
        } catch (Exception e) {
        } finally {
            m_serverOutput = null;
        }

        try {
            m_serverInput.close ();
        } catch (Exception e) {
        } finally {
            m_serverInput = null;
        }

        if (m_origServerInput != null) {
            try {
                m_origServerInput.close ();
            } catch (Exception e) {
            } finally {
                m_origServerInput = null;
            }
        }

        if (m_origServerOutput != null) {
            try {
                m_origServerOutput.close ();
            } catch (Exception e) {
            } finally {
                m_origServerOutput = null;
            }
        }

        // Notify the Connection Manager 
        if (ex != null) {
            // the connection is lost
            m_connMgr.invalidateConnection();
        }
        else {
            // the connection is closed
            m_connMgr.closeConnection();
        }

        // Set the status for all outstanding requests
        synchronized (m_requests) {
            for (Iterator iter = m_requests.values().iterator();
                iter.hasNext();) {
                LDAPMessageQueue listener = (LDAPMessageQueue) iter.next();
                try {                 
                    if (ex != null) {
                        listener.setException(this, ex);
                    }
                    else {
                        listener.removeAllRequests(this);
                    }
                }
                catch (Exception ignore) {}
                iter.remove();
            }
        }

        if (m_messages != null) {
            m_messages.clear();
        }

        m_bound = false;
    }

    /**
     * Sleep if there is a backlog of search results
     */
    private void checkBacklog() throws InterruptedException{

        while (true) {

            synchronized (m_requests) {
            
                if (m_requests.size() == 0) {
                    return;
                }
            
                for (Iterator iter = m_requests.values().iterator();
                    iter.hasNext();) {
                    LDAPMessageQueue l = (LDAPMessageQueue) iter.next();
                    // If there are clients waiting for a regular response
                    // message, skip backlog checking
                    if ( !(l.getType() ==
                        LDAPMessageQueue.LDAP_SEARCH_LISTENER)) {
                        return;
                    }

                    LDAPSearchListener sl = (LDAPSearchListener)l;
                
                    // should never happen, but just in case
                    if (sl.getSearchConstraints() == null) {
                        return;
                    }

                    int slMaxBacklog =
                        sl.getSearchConstraints().getMaxBacklog();
                    int slBatchSize  = sl.getSearchConstraints().getBatchSize();
                
                    // Disabled backlog check ?
                    if (slMaxBacklog == 0) {
                        return;
                    }
                
                    // Synch op with zero batch size ?
                    if (!sl.isAsynchOp() && slBatchSize == 0) {
                        return;
                    }
                
                    // Max backlog not reached for at least one listener ?
                    // (if multiple requests are in progress)
                    if (sl.getMessageCount() < slMaxBacklog) {
                        return;
                    }
                }
            }
            synchronized(this) {
                wait(3000);
            }            
        }
    }



    /**
     * This is called when a search result has been retrieved from the incoming
     * queue. We use the notification to unblock the listener thread, if it
     * is waiting for the backlog to lighten.
     */
    synchronized void resultRetrieved() {
        notifyAll();
    }

    /**
     * Reads from the LDAP server input stream for incoming LDAP messages.
     */
    public void run() {

        LDAPMessage msg = null;
        int[] bytesProcessed = new int[1];
        int[] nread = new int[1];        

        while (Thread.currentThread() == m_thread) {
            try  {

                // Check every BACKLOG_CHKCNT messages if the backlog is not too high
                if (--m_backlogCheckCounter <= 0) {
                    m_backlogCheckCounter = BACKLOG_CHKCNT;
                    checkBacklog();
                }

                nread[0] = 0;
                msg = LDAPMessage.getLDAPMessage(m_serverInput, nread,
                    bytesProcessed);
                if (m_traceOutput != null) {
                    logTraceMessage(msg.toTraceString());
                }                    

                // passed in the ber element size to approximate the size of the cache
                // entry, thereby avoiding serialization of the entry stored in the
                // cache
                processResponse (msg, nread[0]);
            } catch (Exception e)  {
                if (Thread.currentThread() == m_thread) {
                    networkError(e);
                }
                else {
                    resultRetrieved();
                }
            }
        }
    }

    /**
     * When a response arrives from the LDAP server, it is processed by
     * this routine.  It will pass the message on to the listening object
     * associated with the LDAP msgId.
     * @param msg New message from LDAP server
     */
    private void processResponse (LDAPMessage msg, int size) {       
        Integer messageID = new Integer (msg.getMessageID());        
        LDAPMessageQueue l = (LDAPMessageQueue)m_requests.get (messageID);        
        if (l == null) {            
            return; /* nobody is waiting for this response (!) */
        }

        if (m_cache != null && (l.getType() ==
            LDAPMessageQueue.LDAP_SEARCH_LISTENER)) {            
            cacheSearchResult((LDAPSearchListener)l, msg, size);
        }   
        l.addMessage (msg);

        if ((msg.getMessageType() == LDAPMessage.LDAP_RESPONSE_MESSAGE) ||
            (msg.getMessageType() ==
            LDAPMessage.LDAP_EXTENDED_RESPONSE_MESSAGE)) { 
            m_requests.remove (messageID);            
            if (m_requests.size() == 0) {                
                m_backlogCheckCounter = BACKLOG_CHKCNT;
            }

            // Change IO streams if startTLS extended op completed
            if (msg.getMessageType() ==
                LDAPMessage.LDAP_EXTENDED_RESPONSE_MESSAGE) {
                LDAPExtendedResponse extrsp = (LDAPExtendedResponse) msg;                
                String extid = extrsp.getID();                
                if (extrsp.getResultCode() == 0 && extid != null &&
                    extid.equals(LDAPConnection.OID_startTLS)) {                    
                    changeIOStreams();
                }
            }
        }
    }

    private void changeIOStreams() {

        // Note: For the startTLS, the new streams are layered over the
        // existing ones so current IO streams as well as the socket MUST
        // not be closed.
        m_origServerInput  = m_serverInput;
        m_origServerOutput = m_serverOutput;
        m_serverInput  = null;
        m_serverOutput = null;
        
        while (m_serverInput == null ||  m_serverOutput == null) {
            try {
                if (Thread.currentThread() != m_thread) {
                    return; // user disconnected
                }
                Thread.sleep(200);
            } catch (InterruptedException ignore) {}
        }
    }

    
    /**
     * Collect search results to be added to the LDAPCache. Search results are
     * packaged in a vector and temporary stored into a hashtable m_messages
     * using the message id as the key. The vector first element (at index 0)
     * is a Long integer representing the total size of all LDAPEntries entries.
     * It is followed by the actual LDAPEntries.
     * If the total size of entries exceeds the LDAPCache max size, or a referral
     * has been received, caching of search results is disabled and the entry is 
     * not added to the LDAPCache. A disabled search request is denoted by setting
     * the entry size to -1.
     */
    private synchronized void cacheSearchResult (LDAPSearchListener l, LDAPMessage msg, int size) {
        Integer messageID = new Integer (msg.getMessageID());
        Long key = l.getKey();
        Vector v = null;

        if ((m_cache == null) || (key == null)) {
            return;
        }
        
        if (msg.getMessageType() == LDAPMessage.LDAP_SEARCH_RESULT_MESSAGE) {

            // get the vector containing the LDAPMessages for the specified messageID
            v = (Vector)m_messages.get(messageID);
            if (v == null) {
                m_messages.put(messageID, v = new Vector());
                v.addElement(new Long(0));
            }

            // Return if the entry size is -1, i.e. the caching is disabled
            if (((Long)v.firstElement()).longValue() == -1L) {
                return;
            }
            
            // add the size of the current LDAPMessage to the lump sum
            // assume the size of LDAPMessage is more or less the same as the size
            // of LDAPEntry. Eventually LDAPEntry object gets stored in the cache
            // instead of LDAPMessage object.
            long entrySize = ((Long)v.firstElement()).longValue() + size;

            // If the entrySize exceeds the cache size, discard the collected
            // entries and disble collecting of entries for this search request
            // by setting the entry size to -1.
            if (entrySize > m_cache.getSize()) {
                v.removeAllElements();
                v.addElement(new Long(-1L));
                return;
            }                
                
            // update the lump sum located in the first element of the vector
            v.setElementAt(new Long(entrySize), 0);

            // convert LDAPMessage object into LDAPEntry which is stored to the
            // end of the Vector
            v.addElement(((LDAPSearchResult)msg).getEntry());

        } else if (msg.getMessageType() ==
            LDAPMessage.LDAP_SEARCH_RESULT_REFERENCE_MESSAGE) {

            // If a search reference is received disable caching of
            // this search request 
            v = (Vector)m_messages.get(messageID);
            if (v == null) {
                m_messages.put(messageID, v = new Vector());
            }
            else {
                v.removeAllElements();
            }
            v.addElement(new Long(-1L));

        } else if (msg.getMessageType() == LDAPMessage.LDAP_RESPONSE_MESSAGE) {

            // The search request has completed. Store the cache entry
            // in the LDAPCache if the operation has succeded and caching
            // is not disabled due to the entry size or referrals
            
            boolean fail = ((LDAPResponse)msg).getResultCode() > 0;
            v = (Vector)m_messages.remove(messageID);
            
            if (!fail)  {
                // If v is null, meaning there are no search results from the
                // server
                if (v == null) {
                    v = new Vector();
                    v.addElement(new Long(0));
                }

                // add the new entry if the entry size is not -1 (caching diabled)
                if (((Long)v.firstElement()).longValue() != -1L) {
                    m_cache.addEntry(key, v);
                }
            }
        }
    }

    /**
     * Stop dispatching responses for a particular message ID and send 
     * the abandon request.
     * @param id Message ID for which to discard responses.
     */
    void abandon (int id, LDAPControl[] ctrls) {
        
        if (m_thread == null) {
            return;
        }
        
        LDAPMessageQueue l = (LDAPMessageQueue)m_requests.remove(new Integer(id));
        // Clean up cache if enabled
        if (m_messages != null) {
            m_messages.remove(new Integer(id));
        }
        if (l != null) {
            l.removeRequest(id);
        }
        resultRetrieved(); // If LDAPConnThread is blocked in checkBacklog()
        
        sendAbandonRequest(id, ctrls);
    }

    /**
     * Change listener for a message ID. Required when LDAPMessageQueue.merge()
     * is invoked.
     * @param id Message ID for which to chanage the listener.
     * @return Previous listener.
     */
    LDAPMessageQueue changeListener (int id, LDAPMessageQueue toNotify) {

        if (m_thread == null) {
            toNotify.setException(this, new LDAPException("Server or network error",
                                                           LDAPException.SERVER_DOWN));
            return null;
        }
        return (LDAPMessageQueue) m_requests.put (new Integer (id), toNotify);
    }

    /**
     * Handles network errors.  Basically shuts down the whole connection.
     * @param e The exception which was caught while trying to read from
     * input stream.
     */
    private synchronized void networkError (Exception e) {

        if (m_thread == null) {
            return;
        }
        m_thread = null; // No more request processing
        cleanUp(new LDAPException("Server or network error",
                                  LDAPException.SERVER_DOWN));
    }
}
