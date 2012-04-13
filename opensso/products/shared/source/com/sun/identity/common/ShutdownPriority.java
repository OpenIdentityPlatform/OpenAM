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
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: ShutdownPriority.java,v 1.2 2008/06/25 05:52:52 qcheng Exp $
 *
 */

package com.sun.identity.common;

import java.util.ArrayList;
import java.util.List;

/**
 * This class defines the shutdown priorities that are consumed by
 * <code>com.sun.identity.common.ShutdownManager</code>.
 */

public class ShutdownPriority {
    
    /**
     * HIGHEST is the priority pre-defined in the system. Components which are 
     * registered with this priority will be shutdown first.
     */
    
    public static final ShutdownPriority HIGHEST = new ShutdownPriority(3);
    
    /**
     * DEFAULT is the priority pre-defined in the system. Components which are 
     * registered with this priority will be shutdown after the componets with
     * HIGHEST priority.
     */
    
    public static final ShutdownPriority DEFAULT = new ShutdownPriority(2);
    
    /**
     * LOWEST is the priority pre-defined in the system. Components which are 
     * registered with this priority will be shutdown after the componets with
     * HIGHEST or DEFAULT priority.
     */
    
    public static final ShutdownPriority LOWEST = new ShutdownPriority(1);
    
    private static List priorities = new ArrayList(3);
    static {
        priorities.add(HIGHEST);
        priorities.add(DEFAULT);
        priorities.add(LOWEST);
    }
    
    private int priority;
    
    private ShutdownPriority(int priority) {
        this.priority = priority;
    }
    
    /**
     * Returns the priority.
     *
     * @return the priority.
     */
    
    public int getIntValue() {
        return priority;
    }
    
    /**
     * Returns list of all the priorities (ordered from the highest to the
     * lowest priority) defined in the system.
     *
     * @return list of all the priorities defined in the system.
     */
    
    public static List getPriorities() {
        return priorities;
    }
} 
