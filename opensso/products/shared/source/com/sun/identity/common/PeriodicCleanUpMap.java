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
 * $Id: PeriodicCleanUpMap.java,v 1.3 2009/08/18 21:16:39 ww203982 Exp $
 *
 */

package com.sun.identity.common;

import java.util.Map;
import java.util.Set;

/**
 * PeriodicCleanUpMap is a general Map and a scheduleable unit. Elements pairs
 * will be grouped by using the time they enter the map. PeriodicCleanUpMap can
 * be scheduled to Timer or TimerPool. For every run period,
 * The map will remove the elements which are timeout.
 */

public class PeriodicCleanUpMap extends PeriodicGroupMap {
    
    /**
     * Constructor of PeriodicCleanUpMap.
     *
     * @param runPeriod Run period in ms
     * @param timeoutPeriod timeout period in ms
     */
    
    public PeriodicCleanUpMap(long runPeriod, long timeoutPeriod) {
        this(runPeriod, timeoutPeriod, null);
    }
    
    /**
     * Constructor of PeriodicCleanUpMap.
     *
     * @param runPeriod Run period in ms
     * @param timeoutPeriod timeout period in ms
     * @param map The synchronized map to use
     */
    
    public PeriodicCleanUpMap(long runPeriod, long timeoutPeriod, Map map)
        throws IllegalArgumentException {
        super(null, runPeriod, timeoutPeriod, true, map);
    }
    
    /**
     * Remove the timed out elements in the map and swap the containers.
     */
    
    public void run() {
        synchronized (map) {
            synchronized (thisTurn) {
                map.keySet().removeAll(thisTurn);
                thisTurn.clear();
            }
        }
        // containers swapping
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
