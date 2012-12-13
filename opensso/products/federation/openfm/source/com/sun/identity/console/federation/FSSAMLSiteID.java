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
 * $Id: FSSAMLSiteID.java,v 1.3 2008/06/25 05:49:35 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.sun.identity.console.base.AMPipeDelimitAttrTokenizer;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.shared.datastruct.OrderedSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class FSSAMLSiteID {
    OrderedSet sites = new OrderedSet();
    
    public FSSAMLSiteID(Set siteIds) {
        if ((siteIds != null) && !siteIds.isEmpty()) {
            AMPipeDelimitAttrTokenizer tokenizer =
                AMPipeDelimitAttrTokenizer.getInstance();
            
            for (Iterator i = siteIds.iterator(); i.hasNext(); ) {
                sites.add(new SiteID((String)i.next()));
            }
        }
    }
    
    public void addSiteID(Map mapData)
    throws AMConsoleException {
        SiteID siteId = new SiteID(mapData);
        if (sites.contains(siteId)) {
            throw new AMConsoleException(
                "saml.profile.siteid.already.exists.siteId");
        }
        sites.add(siteId);
    }
    
    public void replaceSiteID(int idx, Map mapData)
    throws AMConsoleException {
        SiteID siteId = new SiteID(mapData);
        int count = 0;
        
        for (Iterator i = sites.iterator(); i.hasNext(); ) {
            SiteID id = (SiteID)i.next();
            if ((count != idx) && id.equals(siteId)) {
                throw new AMConsoleException(
                    "saml.profile.siteid.already.exists.siteId");
            }
            count++;
        }
        sites.set(idx, siteId);
    }
    
    public Set getValues() {
        Set values = new OrderedSet();
        for (Iterator i = sites.iterator(); i.hasNext(); ) {
            SiteID siteId = (SiteID)i.next();
            values.add(siteId.toString());
        }
        return values;
    }
    
    class SiteID {
        String instanceId;
        String issuerName;
        String siteid;
        
        SiteID(String strData) {
            AMPipeDelimitAttrTokenizer tokenizer =
                AMPipeDelimitAttrTokenizer.getInstance();
            initialize(tokenizer.tokenizes(strData));
        }
        
        SiteID(Map map) {
            initialize(map);
        }
        
        public boolean equals(Object other) {
            boolean same = false;
            if (getClass().isInstance(other)) {
                same = toString().equals(other.toString());
            }
            return same;
        }
        
        public int hashCode() {
            return toString().hashCode();
        }
        
        private void initialize(Map map) {
            for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
                String key = (String)i.next();
                if (key.equalsIgnoreCase(SAMLConstants.INSTANCEID)) {
                    instanceId = (String)map.get(key);
                } else if (key.equalsIgnoreCase(SAMLConstants.SITEID)) {
                    siteid = (String)map.get(key);
                } else if (key.equalsIgnoreCase(SAMLConstants.ISSUERNAME)) {
                    issuerName = (String)map.get(key);
                }
            }
        }
        
        public String toString() {
            StringBuffer buff = new StringBuffer(1000);
            boolean hasValue = false;
            
            if (instanceId != null) {
                hasValue = true;
                buff.append(SAMLConstants.INSTANCEID)
                .append("=")
                .append(instanceId);
            }
            
            if (issuerName != null) {
                if (hasValue) {
                    buff.append("|");
                } else {
                    hasValue = true;
                }
                buff.append(SAMLConstants.ISSUERNAME)
                .append("=")
                .append(issuerName);
            }
            
            if (siteid != null) {
                if (hasValue) {
                    buff.append("|");
                } else {
                    hasValue = true;
                }
                buff.append(SAMLConstants.SITEID)
                .append("=")
                .append(siteid);
            }
            return buff.toString();
        }
    }
}
