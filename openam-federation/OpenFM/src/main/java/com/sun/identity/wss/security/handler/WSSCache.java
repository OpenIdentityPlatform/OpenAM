/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * "Portions Copyrighted [year] [name of copyght owner]"
 *
 * $Id: WSSCache.java,v 1.2 2009/04/21 17:41:25 mallas Exp $
 *
 */

package com.sun.identity.wss.security.handler;

import java.util.Date;
import java.util.Map;

import com.sun.identity.common.PeriodicCleanUpMap;
import com.sun.identity.wss.security.WSSConstants;
import com.sun.identity.wss.security.WSSUtils;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.common.TaskRunnable;
import com.sun.identity.common.TimerPool;

/**
 * This class <code>WSSCache</code> is a cache holder for the WSS
 * tokens.
 */
public class WSSCache {
    
    
    static int cacheTimeoutInterval = 300; //sec
    static int cacheCleanupInterval = 60; //sec
    static PeriodicCleanUpMap messageIDMap = null;
    static PeriodicCleanUpMap nonceCache = null;
    
    static {
        String intervalStr = SystemConfigurationUtil.getProperty(
                     WSSConstants.CACHE_CLEANUP_INTERVAL);
        String tmpStr = SystemConfigurationUtil.getProperty(
                WSSConstants.CACHE_TIMEOUT_INTERVAL);
        try {
            if (intervalStr != null && intervalStr.length() != 0) {
                cacheCleanupInterval = Integer.parseInt(intervalStr);
            }
            if (tmpStr != null && tmpStr.length() != 0) {
                cacheTimeoutInterval = Integer.parseInt(tmpStr);
            }
        } catch (NumberFormatException e) {
            if (WSSUtils.debug.messageEnabled()) {
                WSSUtils.debug.message("WSSCache static: "
                    + "invalid cleanup interval. Using default.");
            }
        }
        
        nonceCache = new PeriodicCleanUpMap(
                cacheCleanupInterval * 1000, cacheTimeoutInterval * 1000);
    
        messageIDMap = new PeriodicCleanUpMap(
                cacheCleanupInterval * 1000, cacheTimeoutInterval * 1000);
        
        SystemTimerPool.getTimerPool().schedule(nonceCache, 
                new Date(System.currentTimeMillis() + cacheCleanupInterval));
    
        SystemTimerPool.getTimerPool().schedule(messageIDMap,  
                new Date(System.currentTimeMillis() + cacheCleanupInterval));
    }

}
