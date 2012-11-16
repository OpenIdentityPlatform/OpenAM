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
 * $Id: ShowDataStore.java,v 1.4 2009/02/11 17:21:32 veiming Exp $
 *
 */

package com.sun.identity.cli.datastore;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CLIUtil;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.FormatUtils;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.log.Level;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import java.util.Map;
import java.util.Set;

/**
 * List the names of data store under a realm.
 */
public class ShowDataStore extends DataStoreBase {
    
    /**
     * Handles request.
     *
     * @param rc Request Context.
     * @throws CLIException if request cannot be processed.
     */
    public void handleRequest(RequestContext rc)
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        SSOToken adminSSOToken = getAdminSSOToken();

        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String name = getStringOptionValue(DatastoreOptions.DATASTORE_NAME);

        validateRealm(realm);
        
        String[] params = {realm, name};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_SHOW_DATASTORE", params);
        try {
            ServiceConfigManager svcCfgMgr = new ServiceConfigManager(
                IdConstants.REPO_SERVICE, adminSSOToken);
            ServiceConfig cfg = svcCfgMgr.getOrganizationConfig(realm, null);
            if (cfg != null) {
                ServiceConfig ss = cfg.getSubConfig(name);

                if (ss != null) {
                    Set passwords = CLIUtil.getPasswordFields(
                        IdConstants.REPO_SERVICE, SchemaType.ORGANIZATION,
                        ss.getSchemaID());

                    Map attributesValues = ss.getAttributes();
                    getOutputWriter().printlnMessage(
                        FormatUtils.printAttributeValues("{0}={1}",
                            attributesValues, passwords));
                } else {
                    getOutputWriter().printlnMessage(getResourceString(
                        "datastore-show-datastore-not-found"));
                }
            } else{
                getOutputWriter().printlnMessage(getResourceString(
                    "datastore-show-datastore-not-found"));
            }
            
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_SHOW_DATASTORE", params);
        } catch (SMSException e) {
            String[] p = {realm, name, e.getMessage()};
            debugError("ShowDataStore.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SHOW_DATASTORE", p);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] p = {realm, name, e.getMessage()};
            debugError("ShowDataStore.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SHOW_DATASTORE", p);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
