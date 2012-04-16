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
 * $Id: FSRequestCleanUpRunnable.java,v 1.2 2008/06/25 05:46:55 qcheng Exp $
 *
 */

package com.sun.identity.federation.services;

import com.sun.identity.common.PeriodicGroupRunnable;
import com.sun.identity.federation.common.FSUtils;
import java.util.Map;
import java.util.Set;

/**
 * This is a helper class used by FSSessionManager to clean up expired
 * assertionIDs from the map.
 */

public class FSRequestCleanUpRunnable extends PeriodicGroupRunnable {
    
    private Map idRequestMap = null;
    private Map idDestMap = null;
    
    /**
     * Constructor.
     * @param idAuthnRequestMap request ID (String) and FSAuthnRequest map
     * @param idDestnMap request ID (String) and FSProviderDescriptor map
     * @param threadCleanupInterval thread cleanup interval 
     * @param timeoutPeriod timeout time
     */
    
    public FSRequestCleanUpRunnable(Map idAuthnRequestMap, Map idDestnMap,
        long threadCleanupInterval, long timeoutPeriod) {
        super(null, threadCleanupInterval, timeoutPeriod, true);
        idRequestMap = idAuthnRequestMap;
        idDestMap =  idDestnMap;
    }
    
    /*
     * Run the cleanup task.
     */
    
    public void run() {
        FSUtils.debug.message("FSRequestCleanUpRunnable:run thread wakeup");
        synchronized (thisTurn) {
            idRequestMap.keySet().removeAll(thisTurn);
            idDestMap.keySet().removeAll(thisTurn);
            thisTurn.clear();
        }
        // swap the containers, same as in PeriodicRunnable
        synchronized (nextTurn[containerNeeded - 1]) {
            Set tempSet = thisTurn;
            for (int i = 0; i < containerNeeded + 1; i++) {
                if (i == 0) {
                    thisTurn = nextTurn[0];
                } else {
                    if (i == containerNeeded) {
                        nextTurn[containerNeeded - 1] = tempSet;
                    } else {
                        nextTurn[i - 1] = nextTurn[i];
                    }
                }
            }
        }
    }
    
}
