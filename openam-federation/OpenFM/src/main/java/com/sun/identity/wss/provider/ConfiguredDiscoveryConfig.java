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
 * $Id: ConfiguredDiscoveryConfig.java,v 1.4 2008/12/13 07:16:09 veiming Exp $
 *
 */


package com.sun.identity.wss.provider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

import com.sun.identity.sm.ChoiceValues;

/**
 * The class determines the configured Discovery agent configurations.
 * 
 */
public class ConfiguredDiscoveryConfig extends ChoiceValues {
    /**
     * Creates <code>ConfiguredDiscoveryConfig</code> object.
     * Default constructor that will be used by the SMS
     * to create an instance of this class
     */
    public ConfiguredDiscoveryConfig() {
        // do nothing
    }
    
    /**
     * Returns the choice values and their corresponding localization keys.
     *
     * @return the choice values and their corresponding localization keys.
     */
    public Map getChoiceValues() {
        return getChoiceValues(Collections.EMPTY_MAP);
    }

    /**
     * Returns the choice values from configured environment params.
     * @param envParams map for configured parameters
     * @return the choice values from configured environment params.
     */
    public Map getChoiceValues(Map envParams) {

        List discoveryConfigList = ProviderUtils.getAllDiscoveryConfig();
        Map answer = new HashMap();

        if (discoveryConfigList != null && !discoveryConfigList.isEmpty()) {
            for (Iterator it = discoveryConfigList.iterator(); it.hasNext(); ) {
                DiscoveryConfig discoConfig = (DiscoveryConfig) it.next();
                answer.put(discoConfig.getName(), discoConfig.getName());
            }
        }

        answer.put("[Empty]", "label.Empty");

        //return the choice values map
        return (answer);
    }
    
}
