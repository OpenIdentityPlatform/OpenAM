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
 * $Id: AuthThreadManager.java,v 1.4 2008/06/25 05:42:04 qcheng Exp $
 *
 */



package com.sun.identity.authentication.service;

import java.util.Enumeration;
import java.util.Hashtable;

import com.sun.identity.shared.debug.Debug;

/**
 * AuthThreadManager sleeps for n milliseconds as set in 
 * iplanet-am-auth-sleep-interval wakes up and checks for each thread 
 * in the timeout Hash for timeout.
 * If thread has timed out adds thread to timedOutHash and removed from
 * timeoutHash. 
 */
public class AuthThreadManager extends Thread   {

    long timeout = 60;
    static Debug debug = null;
    static int i=0;
    Hashtable timeoutHash = new Hashtable();
    Hashtable timedOutHash = new Hashtable();
    long lastCallbackSent;
    long defaultSleepTime = 300000;

    /**
     * Creates <code>AuthThreadManager</code> object.
     */
    public AuthThreadManager () {
        debug = Debug.getInstance("amThreadManager");
        defaultSleepTime = AuthD.getAuth().getDefaultSleepTime();
        if (debug.messageEnabled()) {
            debug.message("Default sleep time : " + defaultSleepTime);
        }
    }

    /**
     * thread sleeps for n milliseconds as set in iplanet-am-auth-sleep-interval
     * wakes up and checks for each thread in the timeout Hash for timeout.
     * if thread has timed out adds thread to timedOutHash and removed from
     * timeoutHash 
     */
    public void run() {
        while (true) {
            try {
                sleep(defaultSleepTime);
                if (debug.messageEnabled()) {
                    debug.message("Thread Waking up");
                    debug.message("timeoutHash :" + timeoutHash);
                }

                if ((timeoutHash != null) && (!timeoutHash.isEmpty())) {
                    Enumeration timeoutElem = timeoutHash.keys();
                    while (timeoutElem.hasMoreElements()) {
                        Object key = timeoutElem.nextElement();
                        Thread thread = (Thread) key;
                        Hashtable s = (Hashtable) timeoutHash.get(key);
                        long timeout = ((Long)s.get("PageTimeout")).longValue();
                        long lastCallbackSent = 
                            ((Long) s.get("LastCallbackSent")).longValue();

                        if (isLoginTimeout(lastCallbackSent , timeout)) {
                            if (debug.messageEnabled()) {
                                debug.message("Interrupting thread" + thread);
                            }
                            thread.interrupt();
                            timeoutHash.remove(key);
                            timedOutHash.put(thread, Boolean.TRUE);
                        }
                    }
                }
            } catch (Exception e) {
                debug.message("Error run : " , e);
            }
        }
    }

    /**
     * Checks login state for time out
     * @param lastCallbackSent time for last callback was sent.
     * @param timeout configured timeout value.
     * @return <code>true</code> if the thread is timed out. 
     */
    public boolean isLoginTimeout(long lastCallbackSent, long timeout) {
        long now = System.currentTimeMillis();
        long timeoutVal = lastCallbackSent + (timeout -3) * 1000;
        return (timeoutVal < now);
    }

    /**
     * Stores the thread as key and the time out value &
     * last callback sent in a Hashtable 
     * @param currentThread will be stored
     * @param pageTimeOut configured timeout value
     * @param lastCallbackSent time for last callback was sent
     */
    public void setHash(
        Thread currentThread,
        long pageTimeOut,
        long lastCallbackSent) {
        if (debug.messageEnabled()) {
            debug.message("Setting hash... : "  + currentThread);
        }
        if (timeoutHash.contains(currentThread)) {
            return;
        }
        Hashtable param = new Hashtable();
        param.put("PageTimeout" , new Long(pageTimeOut));
        param.put("LastCallbackSent",new Long(lastCallbackSent));

        timeoutHash.put(currentThread,param);    

        if (debug.messageEnabled()){
            debug.message("timeOutHash is : " + timeoutHash);
        }
    }

    /**
     * Checks if thread has timed out
     * @param thread will be checked
     * @return <code>true</code> if the is timed out
     */
    public boolean isTimedOut(Thread thread) {
        if (debug.messageEnabled()) {
            debug.message("Timed ut hash has : " + timedOutHash);
        }
        if  ((timedOutHash ==  null) || (timedOutHash.isEmpty())) {
            return false;
        }
        try {
            Boolean timedOut = (Boolean) timedOutHash.get(thread);
            return timedOut.booleanValue();
        } catch (Exception e ) {
            return false;
        }
    }
    
    /**
     * Removes thread from <code>Hashtable</code> specified by hashName
     * @param thread will be removed from the hash
     * @param hashName has associated thread
     */
    public void removeFromHash(Thread thread,String hashName) {
        if (debug.messageEnabled()) {
            debug.message("Request to remove thread " + 
                thread + "from hash : " + hashName);
        }
        if (hashName.equals("timeoutHash")) {
            removeHash(timeoutHash,thread);
            if (debug.messageEnabled()){
                debug.message("timeOutHash is : " + timeoutHash);
            }
        }

        if (hashName.equals("timedOutHash")) {
            removeHash(timedOutHash,thread);
            if (debug.messageEnabled()){
                debug.message("timedOutHash is : " + timedOutHash);
            }
        } 
    }

    void removeHash(Hashtable hash,Thread thread) {
        if ((hash == null) || hash.isEmpty()) {
            return;
        }
        try {
            hash.remove(thread);
        } catch (Exception e) {
        }
    }
}
