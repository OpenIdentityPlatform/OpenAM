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
 * $Id: POSTCleanUpRunnable.java,v 1.2 2008/06/25 05:47:38 qcheng Exp $
 *
 */

package com.sun.identity.saml.servlet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.saml.common.SAMLUtils;

/**
 * This is a helper class used by SAMLPOSTProfileServlet to clean up expired
 * assertionIDs from the map.
 */

public class POSTCleanUpRunnable extends GeneralTaskRunnable {
    private Map idTimeMap = null;
    private Set keys;
    private long runPeriod;
    
    
    /**
     * Constructor.
     * @param runPeriod The period for the clean up to run.
     * @param map the <code>Map</code> to be cleaned up.
     */
    public POSTCleanUpRunnable(long runPeriod, Map map) {
        this.runPeriod = runPeriod;
        this.idTimeMap = map;
        keys = new HashSet();
    }
    
    public boolean addElement(Object obj) {
        synchronized (keys) {
            return keys.add(obj);
        }
    }
    
    public boolean removeElement(Object obj) {
        synchronized (keys) {
            return keys.remove(obj);
        }
    }
    
    public boolean isEmpty() {
        return false;
    }
    
    public long getRunPeriod() {
        return runPeriod;
    }
    
    public void run() {
        String aIDString;
        long now = System.currentTimeMillis();
        synchronized (keys) {
            for (Iterator iter = keys.iterator(); iter.hasNext();) {
                aIDString = (String) iter.next();
                if (((Long)idTimeMap.get(aIDString)).longValue() < now) {
                    if (SAMLUtils.debug.messageEnabled()) {
                        SAMLUtils.debug.message("POSTCleanUpRunnable: deleting "
                            + aIDString);
                    }
                    iter.remove();
                    idTimeMap.remove(aIDString);
                }
            }
        }
    }
    
}
