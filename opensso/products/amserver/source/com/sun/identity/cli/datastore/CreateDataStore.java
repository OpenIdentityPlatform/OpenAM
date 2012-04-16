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
 * $Id: CreateDataStore.java,v 1.4 2009/02/11 17:21:31 veiming Exp $
 *
 */

package com.sun.identity.cli.datastore;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AttributeValues;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.log.Level;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * List the names of data store under a realm.
 */
public class CreateDataStore extends DataStoreBase {
    
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
        String type = getStringOptionValue(DatastoreOptions.DATASTORE_TYPE);
        
        String datafile = getStringOptionValue(IArgument.DATA_FILE);
        List listValues = rc.getOption(IArgument.ATTRIBUTE_VALUES);
        
        if ((datafile == null) && (listValues == null)) {
            throw new CLIException(
                getResourceString("datastore-create-datastore-missing-data"),
                ExitCodes.INCORRECT_OPTION, rc.getSubCommand().getName());
        }

        validateRealm(realm);
        
        Map attributeValues = AttributeValues.parse(getCommandManager(),
            datafile, listValues);
        
        String[] params = {realm, name, type};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_CREATE_DATASTORE", params);
        try {
            ServiceConfigManager svcCfgMgr = new ServiceConfigManager(
                IdConstants.REPO_SERVICE, adminSSOToken);
            ServiceConfig cfg = svcCfgMgr.getOrganizationConfig(realm, null);
            if (cfg == null) {
                OrganizationConfigManager orgCfgMgr = new 
                    OrganizationConfigManager(adminSSOToken, realm);
                Map attrValues = getDefaultAttributeValues(adminSSOToken);
                cfg = orgCfgMgr.addServiceConfig(
                    IdConstants.REPO_SERVICE, attrValues);
            }
            
            cfg.addSubConfig(name, type, 0, attributeValues);
            getOutputWriter().printlnMessage(
                getResourceString("datastore-create-datastore-succeeded"));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_CREATE_DATASTORE", params);
        } catch (SMSException e) {
            debugError("CreateDataStore.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_CREATE_DATASTORE", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            debugError("CreateDataStore.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_CREATE_DATASTORE", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

    }
    
    private Map getDefaultAttributeValues(SSOToken adminSSOToken)
        throws SMSException, SSOException
    {
        ServiceSchemaManager schemaMgr = new ServiceSchemaManager(
            IdConstants.REPO_SERVICE, adminSSOToken);
        ServiceSchema orgSchema = schemaMgr.getOrganizationSchema();
        Set attrs = orgSchema.getAttributeSchemas();
        Map values = new HashMap(attrs.size() *2);
        
        for (Iterator iter = attrs.iterator(); iter.hasNext(); ) {
            AttributeSchema as = (AttributeSchema)iter.next();
            values.put(as.getName(), as.getDefaultValues());
        }
        
        return values;
    }
}
