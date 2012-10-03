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
 * $Id: DebugPropertiesObserver.java,v 1.4 2008/08/13 16:00:54 rajeevangal Exp $
 *
 */

package com.sun.identity.common;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.common.configuration.ConfigurationListener;
import com.sun.identity.common.configuration.ConfigurationObserver;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import java.util.Collection;
import java.util.Iterator;

/**
 * This class observes changes in debug configuration properties.
 */
public class DebugPropertiesObserver implements ConfigurationListener {
    private static DebugPropertiesObserver instance;
    private static String currentState;
    private static String currentMergeFlag = "off";
    
    static {
        instance = new DebugPropertiesObserver();
        ConfigurationObserver.getInstance().addListener(instance);
        currentState = SystemProperties.get(Constants.SERVICES_DEBUG_LEVEL);
        currentMergeFlag = SystemProperties.get(Constants.SERVICES_DEBUG_MERGEALL);
        if (currentMergeFlag == null) {
            currentMergeFlag = "off";
        }
    }
    
    private DebugPropertiesObserver() {
    }
    
    /**
     * Returns an instance of <code>DebugPropertiesObserver</code> object.
     *
     * @return an instance of <code>DebugPropertiesObserver</code> object.
     */
    public static DebugPropertiesObserver getInstance() {
        return instance;
    }
    
    /**
     * This method will be call if configuration changed.
     */    
    public void notifyChanges() {
        String state = SystemProperties.get(Constants.SERVICES_DEBUG_LEVEL);
        if (!currentState.equals(state)) {
            Collection debugInstances = Debug.getInstances();
            for (Iterator i = debugInstances.iterator(); i.hasNext(); ) {
                Debug d = (Debug)i.next();
                d.setDebug(state);
            }
            currentState = state;
        }
        String mergeflag = SystemProperties.get(Constants.SERVICES_DEBUG_MERGEALL);
        if (!currentMergeFlag.equals(mergeflag)) {
            currentMergeFlag = mergeflag;
            Collection debugInstances = Debug.getInstances();
            for (Iterator i = debugInstances.iterator(); i.hasNext(); ) {
                Debug d = (Debug)i.next();
                d.resetDebug(mergeflag);
            }
        }
    }
    
}
