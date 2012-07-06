/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: BackwardCompSupport.java,v 1.1 2009/09/05 01:30:45 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.common;

import com.iplanet.am.sdk.AMObject;
import com.sun.identity.common.admin.AdminInterfaceUtils;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.ServiceManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class BackwardCompSupport {
    private static BackwardCompSupport instance;
    private static final String DEBUG_NAME = "common";

    private Map mapIdTypeToServiceName = new HashMap();
    private Map mapIdTypeToSchemaType = new HashMap();
    private Map mapIdTypeToSubSchemaName = new HashMap();

    private String namingAttribute = null;

    private BackwardCompSupport() {
        mapIdTypeToServiceName.put(IdType.USER.getName(),
            "iPlanetAMUserService");
        mapIdTypeToSchemaType.put(IdType.USER.getName(), "user");

        mapIdTypeToServiceName.put(IdType.AGENT.getName(),
            "iPlanetAMAgentService");
        mapIdTypeToSchemaType.put(IdType.AGENT.getName(), "user");

        mapIdTypeToServiceName.put(IdType.GROUP.getName(),
            "iPlanetAMEntrySpecificService");
        mapIdTypeToSubSchemaName.put(IdType.GROUP.getName(), "Group");

        mapIdTypeToServiceName.put(IdType.ROLE.getName(),
            "iPlanetAMEntrySpecificService");
        mapIdTypeToSubSchemaName.put(IdType.ROLE.getName(), "Role");

        mapIdTypeToServiceName.put(IdType.FILTEREDROLE.getName(),
            "iPlanetAMEntrySpecificService");
        mapIdTypeToSubSchemaName.put(IdType.FILTEREDROLE.getName(),
            "FilteredRole");

        if (ServiceManager.isAMSDKEnabled()) {
            namingAttribute = AdminInterfaceUtils.getNamingAttribute(
                AMObject.USER, Debug.getInstance(DEBUG_NAME));
        } else {
            // Since naming attribute cannot be obtained for IdRepo
            // hardcode to "uid"
            namingAttribute = "uid";
        }
    }

    public static BackwardCompSupport getInstance() {
        if (instance == null) {
             instance = new BackwardCompSupport();
        }
        return instance;
    }

    public String getServiceName(String idType) {
        return (String)mapIdTypeToServiceName.get(idType);
    }

    public String getSchemaType(String idType) {
        return (String)mapIdTypeToSchemaType.get(idType);
    }

    public String getSubSchemaName(String idType) {
        return (String)mapIdTypeToSubSchemaName.get(idType);
    }

    public void beforeDisplay(String idType, Set attributeSchemas) {
        if (idType != null) {
            if (idType.equalsIgnoreCase(IdType.USER.getName())) {
                beforeDisplayUser(attributeSchemas);
            }
        }
    }

    private void beforeDisplayUser(Set attributeSchemas) {
        for (Iterator iter = attributeSchemas.iterator(); iter.hasNext(); ) {
            AttributeSchema as = (AttributeSchema)iter.next();
            String name = as.getName();
            if (name.equals(namingAttribute) || name.equals("ChangePassword")) {
                iter.remove();
            }
        }
    }

    public void beforeCreate(
        String idType,
        String entityName,
        Map values
    ) {
        if (idType.equalsIgnoreCase(IdType.USER.getName())) {
            beforeCreateUser(idType, entityName, values);
        }
    }

    public void beforeModify(
        String idType,
        String entityName,
        Map values
    ) {
        if (idType.equalsIgnoreCase(IdType.USER.getName())) {
            beforeModifyUser(idType, entityName, values);
        }
    }

    private void beforeCreateUser(
        String idType,
        String entityName,
        Map values
    ) {
        Set set = new HashSet(2);
        set.add(entityName);
        values.put(namingAttribute, set);
    }

    private void beforeModifyUser(
        String idType,
        String entityName,
        Map values
    ) {
        Set set = new HashSet(2);
        set.add(entityName);
        values.put(namingAttribute, set);
    }
}
