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
 * $Id: AdminReq.java,v 1.2 2008/06/25 05:52:23 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMEntity;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;

abstract class AdminReq {
    static final String SCHEMA_TYPE_ORGANIZATION = "organization";
    static final String SCHEMA_TYPE_DYNAMIC = "dynamic";
    static final String SCHEMA_TYPE_POLICY = "policy";

    static final String GROUP_TYPE_STATIC = "static";
    static final String GROUP_TYPE_DYNAMIC = "dynamic";
    static final String GROUP_TYPE_ASSIGNABLE_DYNAMIC = "assignableDynamic";
    static final String GROUP_FILTER_INFO = "filterinfo";

    static final String ROLE_TYPE_STATIC = "static";
    static final String ROLE_TYPE_FILTERED = "filtered";
    static final String ROLE_FILTER_INFO = "filterinfo";

    static final String AUTH_SERVICE = "iPlanetAMAuthService";
    static final String AUTH_USER_CONTAINER_ATTRIBUTE =
        "iplanet-am-auth-user-container";

    static final String ENTITY_NAME = "entity-name-description";
    static final String ENTITIES_DESC = "entities-description";

    protected static Debug debug = Debug.getInstance("amAdmin");
    protected static PrintWriter writer = new PrintWriter(System.out, true);
    protected static ResourceBundle bundle = AdminResourceBundle.getResources();
    protected String targetDN = null;

    /**
     * Constructs a new empty AdminReq.
     */
    AdminReq() {
        super();
    }        
    
    /**
     * Constructs a new AdminReq. 
     * @param  targetDN. 
     */        
    AdminReq(String targetDN) {
        super();
          this.targetDN = targetDN;
    }

    /**
     * sets the value for targetDN which can be Org DN, PeopleContainer DN,
     * Role DN, Group DN.
     *
     * @param  targetDn the DN of a request. 
     */
    void setTargetDN(String targetDN) {
          this.targetDN = targetDN;
    }

    /**
     * gets the value targetDN which can be Org DN, PeopleContainer DN, Role
     * DN, Group DN.
     *
     * @return targetDN.
     */
    String getTargetDN() {
        return targetDN;
    }        

    ServiceSchemaManager getServiceSchemaManager(SSOToken ssoToken,
        String serviceName)
        throws AdminException
    {
        try {
            return new ServiceSchemaManager(serviceName, ssoToken);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        } catch (SMSException smse) {
            throw new AdminException(smse);
        }
    }

    void printAMObjectDN(Set setAMObjects) {
        for (Iterator iter = setAMObjects.iterator(); iter.hasNext(); ) {
            AMObject obj = (AMObject)iter.next();
            writer.println(obj.getDN());
        }
    }

    void doLog(Set setObjects, String logMessageKey) {
        String[] args = new String[1];
        for (Iterator iter = setObjects.iterator(); iter.hasNext(); ) {
            AMObject obj = (AMObject)iter.next();
            args[0] = obj.getDN();
            AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                Level.INFO, logMessageKey, args);
        }
    }

    void doLogForEntity(
        Set setObjects, 
        String logMessageKey, 
        String localizedName) 
    {
        String[] args = new String[2];
        args[0] = localizedName;
        for (Iterator iter = setObjects.iterator(); iter.hasNext(); ) {
            AMEntity obj = (AMEntity)iter.next();
            args[1] = obj.getDN();
            AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                Level.INFO, logMessageKey, args);
        }
    }

    void doLog(AMObject obj, String logMessageKey) {
        String[] args = {obj.getDN()};
        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
            Level.INFO, logMessageKey, args);
    }

    void doLog(AMEntity entity, String logMessageKey, String entityName) {
        String[] args = new String[2];
        args[0] = entityName;
        args[1] = entity.getDN();
        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
            Level.INFO, logMessageKey, args);
    }

    void doLogStringSet(Set set, String logMessageKey) {
        String[] args = new String[1];

        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            args[0] = (String)iter.next();
            AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                Level.INFO, logMessageKey, args);
        }
    }

    void doLogStringSetForEntity(
        Set set, 
        String logMessageKey, 
        String entityName) 
    {
        String[] args = new String[2];
        args[0] = entityName;

        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            args[1] = (String)iter.next();
            AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                Level.INFO, logMessageKey, args);
        }
    }


    void doLogStringSet(Set set, AMObject obj, String logMessageKey) {
        String[] args = new String[2];
        args[1] = obj.getDN();

        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            args[0] = (String)iter.next();
            AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                Level.INFO, logMessageKey, args);
        }
    }

    void doLog(String param, AMObject obj, String logMessageKey) {
        String[] args = {param, obj.getDN()};
        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
            Level.INFO, logMessageKey, args);
    }

    void doLog(String[] param, String logMessageKey) {
        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
            Level.INFO, logMessageKey, param);
    }

}
