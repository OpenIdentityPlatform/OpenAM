/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FQDNHelper.java,v 1.2 2008/06/25 05:51:39 qcheng Exp $
 *
 */

package com.sun.identity.agents.common;



import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.agents.arch.Module;
import com.sun.identity.agents.arch.SurrogateBase;


/**
 * This class does FQDN mapping
 *
 */
public class FQDNHelper extends SurrogateBase implements IFQDNHelper {

    public FQDNHelper(Module module) {
        super(module);
    }

    public String getValidFQDNResource(String serverName) {
        String result = null;
        String canonicalServerName = serverName.toLowerCase();
        if (!canonicalServerName.equals(getDefaultFQDN())) {
            if (!getValidFQDNSet().contains(canonicalServerName)) {
                String mappedValue =
                    (String) getFQDNMap().get(canonicalServerName);
                if (mappedValue != null) {
                    result = mappedValue;
                } else {
                    result = getDefaultFQDN();
                }
            }            
        }

        if (isLogMessageEnabled()) {
            logMessage("FQDNHelper: Incoming Server Name: [" + serverName
                           + "] Result: " + result);
        }
        return result;
    }

    public void initialize(String defaultFQDN, Map fqdnMap) {
        HashMap canonicalFQDNMap = new HashMap();
        Set validResourceSet = new HashSet();
        Set invalidResourceSet = new HashSet();

        setDefaultFQDN(defaultFQDN.toLowerCase());
        
        validResourceSet.add(getDefaultFQDN());
        if (fqdnMap.size() > 0) {
            Iterator it = fqdnMap.keySet().iterator();

            while (it.hasNext()) {
                String nextKey = (String) it.next();
                String nextValue = (String) fqdnMap.get(nextKey);
                String invalidResource = nextKey.toLowerCase();
                String validResource = nextValue.toLowerCase();

                invalidResourceSet.add(invalidResource);
                validResourceSet.add(validResource);
                canonicalFQDNMap.put(invalidResource, validResource);
            }
        }

        setFQDNMap(canonicalFQDNMap);
        setValidFQDNSet(validResourceSet);
        setInvalidFQDNSet(invalidResourceSet);
        if (isLogMessageEnabled()) {
            logMessage("FQDNHelper: default FQDN => " + getDefaultFQDN());
            logMessage("FQDNHelper: valid FQDN Set => " + getValidFQDNSet());
            logMessage("FQDNHelper: invalid FQDN Set => "
                       + getInvalidFQDNSet());
            logMessage("FQDNHelper: FQDN Map => " + getFQDNMap());
        }
    }

    private Map getFQDNMap() {
        return _fqdnMap;
    }

    private void setFQDNMap(HashMap map) {
        _fqdnMap = map;
    }

    private Set getValidFQDNSet() {
        return _validFQDNSet;
    }

    private void setValidFQDNSet(Set set) {
        _validFQDNSet = set;
    }

    private Set getInvalidFQDNSet() {
        return _invalidFQDNSet;
    }

    private void setInvalidFQDNSet(Set set) {
        _invalidFQDNSet = set;
    }

    private String getDefaultFQDN() {
        return _defaultFQDN;
    }

    private void setDefaultFQDN(String defaultFQDN) {
        _defaultFQDN = defaultFQDN;
    }

    private String  _defaultFQDN;
    private HashMap _fqdnMap;
    private Set     _validFQDNSet;
    private Set     _invalidFQDNSet;
}
