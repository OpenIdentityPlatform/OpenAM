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
 * $Id: AMEntityImpl.java,v 1.5 2008/06/25 05:41:20 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

/*
 * @deprecated  As of Sun Java System Access Manager 7.1.
*/
public class AMEntityImpl extends AMObjectImpl implements AMEntity {

    protected AMEntityImpl(SSOToken ssotoken, String dn) {
        super(ssotoken, dn, AMObject.UNDETERMINED_OBJECT_TYPE);
    }

    protected AMEntityImpl(SSOToken ssotoken, String dn, int type) {
        super(ssotoken, dn, type);
    }

    public void delete(boolean recursive) throws AMException, SSOException {
        int type = dsServices.getObjectType(token, entryDN);
        profileType = type;
        super.delete(recursive);
    }

    public Map getAttributes() throws AMException, SSOException {
        int type = dsServices.getObjectType(token, entryDN);
        profileType = type;
        return super.getAttributes();
    }

    public String getDN() {
        return super.getDN();
    }

    public String getOrganizationDN() throws AMException, SSOException {
        return super.getOrganizationDN();
    }

    public String getParentDN() {
        return super.getParentDN();
    }

    public boolean isExists() throws SSOException {
        return super.isExists();
    }

    public void purge(boolean recursive, int graceperiod) throws AMException,
            SSOException {
        int type = dsServices.getObjectType(token, entryDN);
        profileType = type;
        super.purge(recursive, graceperiod);

    }

    public void removeAttributes(Set attributes) throws AMException,
            SSOException {
        super.removeAttributes(attributes);

    }

    public void setAttributes(Map attributes) throws AMException, SSOException {
        super.setAttributes(attributes);
    }

    public void delete() throws AMException, SSOException {
        delete(false);

    }

    public void store() throws AMException, SSOException {
        int type = dsServices.getObjectType(token, entryDN);
        profileType = type;
        super.store();
    }

    /* 
     * This method is used to create the entity (so far only in memory)
     * in the LDAP data store. A string identifying the type of entry being
     * created, has to be passed. The types supported are the ones defined
     * in the configuration of DAI service. Some examples are: "user", "agent".
     */
    public void create(String stype) throws AMException, SSOException {
        String type = (String) AMCommonUtils.supportedTypes.get(stype
                .toLowerCase());
        if (type != null) {
            profileType = Integer.parseInt(type);
            super.create();
        } else {
            throw new AMException(AMSDKBundle.getString("156", super.locale),
                    "156");
        }
    }


    public void activate() throws AMException, SSOException {
        int type = dsServices.getObjectType(token, entryDN);
        String stype = Integer.toString(type);
        String stAttrName = (String) AMCommonUtils.statusAttributeMap
                .get(stype);
        if (stAttrName != null) {
            profileType = type;
            setStringAttribute(stAttrName, "active");
            store();
        }
    }

    public void deactivate() throws AMException, SSOException {

        int type = dsServices.getObjectType(token, entryDN);
        String stype = Integer.toString(type);
        String stAttrName = (String) AMCommonUtils.statusAttributeMap
                .get(stype);
        if (stAttrName != null) {
            profileType = type;
            setStringAttribute(stAttrName, "inactive");
            store();
        }

    }

    public boolean isActivated() throws AMException, SSOException {
        int type = dsServices.getObjectType(token, entryDN);
        String stype = Integer.toString(type);
        String stAttrName = (String) AMCommonUtils.statusAttributeMap
                .get(stype);
        String stAttrValue = null;
        if (stAttrName != null) {
            profileType = type;
            stAttrValue = getStringAttribute(stAttrName);
        }
        if (stAttrValue == null || stAttrValue.length() == 0
                || stAttrValue.equalsIgnoreCase("active")) {
            return (true);
        } else {
            return (false);
        }
    }

    public Map getAttributes(Set attributeNames) throws AMException,
            SSOException {
        int type = dsServices.getObjectType(token, entryDN);
        profileType = type;
        return super.getAttributes(attributeNames);
    }

    protected Set getRoleDNs() throws AMException, SSOException {
        Set nsroleDNSet = new HashSet();
        nsroleDNSet.add("nsroledn");
        Map nsrolesMap = super.getAttributesFromDataStore(nsroleDNSet);
        Set answer = (Set) nsrolesMap.get("nsroledn");
        return ((answer == null) ? java.util.Collections.EMPTY_SET : answer);
    }

}
