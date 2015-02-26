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
 * $Id: PeriodicGroupMap.java,v 1.3 2009/08/18 21:16:39 ww203982 Exp $
 *
 */

package com.sun.identity.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * PeriodicGroupMap is a general Map and a scheduleable unit. Elements pairs
 * will be grouped by using the time they enter the map. PeriodicGroupMap can
 * be scheduled to Timer or TimerPool. For every run period,
 * ScheduleableGroupAction will act on the elements which are timeout in the
 * map.
 * 
 */

public class PeriodicGroupMap extends PeriodicGroupRunnable implements Map {
    
    
    protected Map map;
    
    /**
     * Constructor of PeriodicGroupMap.
     *
     * @param target The ScheduleableGroupAction to run when there is time
     * @param runPeriod Run period in ms
     * @param timeoutPeriod timeout period in ms
     * @param removeElementAfterAction A boolean to indicate whether to remove
     *        the elements after action
     */
    
    public PeriodicGroupMap(ScheduleableGroupAction target, long runPeriod,
        long timeoutPeriod, boolean removeElementAfterAction) {
        this(target, runPeriod, timeoutPeriod, removeElementAfterAction, null);
    }
    
    /**
     * Constructor of PeriodicGroupMap.
     *
     * @param target The ScheduleableGroupAction to run when there is time
     * @param runPeriod Run period in ms
     * @param timeoutPeriod timeout period in ms
     * @param removeElementAfterAction A boolean to indicate whether to remove
     *        the elements after action
     * @param map The synchronized map to use
     */
    
    public PeriodicGroupMap(ScheduleableGroupAction target, long runPeriod,
        long timeoutPeriod, boolean removeElementAfterAction, Map map) throws
        IllegalArgumentException {
        super(target,runPeriod, timeoutPeriod, removeElementAfterAction);
        if (map != null) {
            this.map = map;
        } else {
            this.map = Collections.synchronizedMap(new HashMap());
        }
        this.removeElementAfterAction = removeElementAfterAction;
        
    }
    
    /**
     * Removes all the elements.
     */
    
    public void clear() {
        synchronized (map) {
            synchronized (nextTurn[containerNeeded - 1]) {
                for (int i = (containerNeeded - 1); i >= 0 ; i--) {
                    nextTurn[i].clear();
                }
                synchronized (thisTurn) {
                    thisTurn.clear();
                }
            }
            map.clear();
        }
    }
    
    /**
     * Checks if the map contains the key.
     *
     * @param key The key of the element
     * @return boolean to indicate whether the map contains the key
     */
    
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }
    
    /**
     * Checks if the map contains the value.
     *
     * @param value The value of the element
     * @return boolean to indicate whether the map contains the value
     */
    
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }
    
    /**
     * Gets the element assoicated with the key.
     *
     * @param key The key of the element
     * @return Object assoicated with the key
     */
    
    public Object get(Object key) {
        return map.get(key);
    }
    
    /**
     * Puts the key and element to the map.
     *
     * @param key The key to identify the element
     * @param value The value of the element
     * @return The element the key previously map to
     */
    
    public Object put(Object key, Object value) {
        Object oldValue = null;
        synchronized (map) {
            oldValue = map.put(key, value);
            if (oldValue != null) {
                removeElement(key);
            }
            addElement(key);
        }
        return oldValue;
    }
    
    /**
     * Removes the element associated with the key.
     *
     * @param key The key to identify the element
     * @return The element the key map to
     */
    
    public Object remove(Object key) {
        synchronized (map) {
            if (removeElement(key)) {
                return map.remove(key);
            }
        }
        return null;
    }
    
    /**
     * Puts all the elements in the map to this map.
     *
     * @param m The map to be added to this map.
     */
    
    public void putAll(Map m) {
        Set keys = m.keySet();
        for (Iterator iter = keys.iterator(); iter.hasNext();) {
            Object key = iter.next();
            put(key, m.get(key));
        }
    }
    
    /**
     * Returns the size of this map.
     */
    
    public int size() {
        return map.size();
    }
    
    
    /**
     * Returns the Set of keys of this map.
     *
     * @return Set of keys of this map
     */
    
    public Set keySet() {
        return map.keySet();
    }
    
    /**
     * Returns the Collection of values of this map.
     *
     * @return Collection of values of this map
     */
    
    public Collection values() {
        return map.values();
    }
    
    /**
     * Returns a Set view of entry in this map.
     *
     * @return Set view of entry in this map.
     */
    
    public Set entrySet() {
        return map.entrySet();
    }
    
    /**
     * Compares whether this map is equal to Object o.
     *
     * @param o The object to be compared to
     * @return boolean to indicate whether they are equal
     */
    
    public boolean equals(Object o) {
        return map.equals(o);
    }
    
    /**
     * Returns hash code of the map.
     *
     * @return hash code of this map
     */
    
    public int hashCode() {
        return map.hashCode();
    }

    /**
     * Indicates whether this PeriodicRunnable is empty.
     *
     * @return A boolean to indicate whether this PeriodicRunnable is empty
     */
    
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
      * Implements for TaskRunnable. Run the function of ScheduleableGroupAction
      * on the objects in thisTurn 1 by 1, and interchange thisTurn and nextTurn.
      */

    public void run() {
        synchronized (map) {
            synchronized (thisTurn) {
                if (!thisTurn.isEmpty()) {
                    for (Iterator iter = thisTurn.iterator();
                        iter.hasNext();) {
                        Object obj = iter.next();
                        doGroupAction(obj);
                        iter.remove();
                    }
                }
            }
        }
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
