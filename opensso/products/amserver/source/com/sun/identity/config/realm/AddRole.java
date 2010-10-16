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
 * $Id: AddRole.java,v 1.3 2008/06/25 05:42:38 qcheng Exp $
 *
 */
package com.sun.identity.config.realm;

import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.config.pojos.RealmRole;


/**
 * @author Jeffrey Bermudez
 */
public class AddRole extends AjaxPage {

    public void onPost() {
        String roleName = toString("roleName");
        boolean readWriteLogFiles = toString("read_write_log_files") != null;
        boolean readLogFiles = toString("read_log_files") != null;
        boolean writeLogFiles = toString("write_log_files") != null;
        boolean readWritePolicyPropertiesOnly = toString("read_write_policy_properties_only") != null;
        boolean readWriteAccessAllRealmsAndPolicyProperties = toString("read_write_access_all_realms_and_policy_properties") != null;

        RealmRole realmRole = new RealmRole();
        realmRole.setName(roleName);
        realmRole.setPrivilege(RealmRole.READ_WRITE_ALL_LOG_FILES, readWriteLogFiles);
        realmRole.setPrivilege(RealmRole.READ_ALL_LOG_FILES, readLogFiles);
        realmRole.setPrivilege(RealmRole.WRITE_ALL_LOG_FILES, writeLogFiles);
        realmRole.setPrivilege(RealmRole.READ_WRITE_POLICY_PROPERTIES_ONLY, readWritePolicyPropertiesOnly);
        realmRole.setPrivilege(RealmRole.READ_WRITE_ALL_REALMS_AND_POLICY_PROPERTIES, readWriteAccessAllRealmsAndPolicyProperties);

        saveRole(realmRole);
    }

    protected void saveRole(RealmRole role) {
        getConfigurator().createRole(role);
    }

}
