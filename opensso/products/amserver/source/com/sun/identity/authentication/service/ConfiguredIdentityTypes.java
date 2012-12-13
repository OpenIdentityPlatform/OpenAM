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
 * $Id: ConfiguredIdentityTypes.java,v 1.6 2008/06/25 05:42:04 qcheng Exp $
 *
 */


package com.sun.identity.authentication.service;

import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.ChoiceValues;
import com.sun.identity.sm.SMSEntry;

/**
 * The class determines the configured Identity Types for Identity Repository.
 * This list is computed per realm.
 */
public class ConfiguredIdentityTypes extends ChoiceValues {
    
    AMIdentityRepository amIdRepo = null;
    
    /**
     * Creates <code>ConfiguredIdentityTypes</code> object.
     * Default constructor that will be used by the SMS
     * to create an instance of this class
     */
    public ConfiguredIdentityTypes() {
        // do nothing
    }
    
    /**
     * Returns the map of choice values for top organization.
     * @return the map of choice values for top organization.
     */
    public Map getChoiceValues() {
        return getChoiceValues(Collections.EMPTY_MAP);
    }
    
    /**
     * Returns the map of choice values for given environment params.
     * @param envParams to get the map of choice values
     * @return the map of choice values for given environment params.
     */
    public Map getChoiceValues(Map envParams) {
        Map answer = new HashMap();
        String orgDN = null;
        String installTime = 
            SystemProperties.get(Constants.SYS_PROPERTY_INSTALL_TIME);
        if ((installTime != null) && (installTime.equals("true"))) {
            answer.put((IdType.USER).getName(),(IdType.USER).getName());
            answer.put((IdType.AGENT).getName(),(IdType.AGENT).getName());
            return (answer);
        }    
        if (envParams != null) {
            orgDN = (String)envParams.get(Constants.ORGANIZATION_NAME);
        }
        if (orgDN == null || orgDN.length() == 0) {
            orgDN = SMSEntry.getRootSuffix();
        }
        if ((choiceValues != null) && (!choiceValues.isEmpty())) {
            answer = (Map) choiceValues.get(orgDN);
            if ((answer != null) && (!answer.isEmpty())) {
                return (answer);
            }
        }
        SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
        AdminTokenAction.getInstance());
        Set idTypes = Collections.EMPTY_SET;
        try {
            amIdRepo = new AMIdentityRepository(adminToken,orgDN);
            idTypes = amIdRepo.getSupportedIdTypes();
        } catch (IdRepoException e) {
            // Due to this exception, idTypes will be NULL
        } catch (SSOException sso) {
            // Due to this exception, idTypes will be NULL
        }
        if ((idTypes != null) && !idTypes.isEmpty()) {
            Iterator idTypeIterator = idTypes.iterator();
            String strIdType = null;
            if (answer == null) {
                answer = new HashMap(idTypes.size() * 2);
            }
            while (idTypeIterator.hasNext()) {
                strIdType = ((IdType) idTypeIterator.next()).getName();
                answer.put(strIdType,strIdType);
            }
        } else {
            answer.put((IdType.USER).getName(),(IdType.USER).getName());
            answer.put((IdType.AGENT).getName(),(IdType.AGENT).getName());
        }
        choiceValues.put(orgDN,answer);
        //return the choice values map
        return (answer);
    }
    
    // Cache of choice values
    private static Hashtable choiceValues = new Hashtable();
}
