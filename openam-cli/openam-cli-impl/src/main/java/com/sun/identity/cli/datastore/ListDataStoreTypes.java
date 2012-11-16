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
 * $Id: ListDataStoreTypes.java,v 1.4 2009/09/05 01:30:45 veiming Exp $
 *
 */

package com.sun.identity.cli.datastore;

import com.iplanet.sso.SSOException;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.FormatUtils;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.log.Level;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * List the supported data store types.
 */
public class ListDataStoreTypes extends AuthenticatedCommand {
    /**
     * Lists the supported data store types.
     *
     * @param rc Request Context.
     * @throws CLIException if request cannot be processed.
     */
    public void handleRequest(RequestContext rc)
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();

        String[] params = {};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_LIST_DATASTORE_TYPES", params);
        try {
            Map labelToName = getIDRepoTypesMap();
            if ((labelToName != null) && !labelToName.isEmpty()) {
                getOutputWriter().printlnMessage(
                    getResourceString(
                        "datastore-list-datastore-types-succeeded"));
                getOutputWriter().printlnMessage(
                    FormatUtils.formatMap(
                    getResourceString("datastore-list-datastore-types-desc"),
                    getResourceString("datastore-list-datastore-types-type"),
                    labelToName));
            } else {
                getOutputWriter().printlnMessage(
                    getResourceString(
                        "datastore-list-datastore-types-no-entries"));
            }
            
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_LIST_DATASTORE_TYPES", params);
        } catch (SMSException e) {
            debugError("ListDataStores.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_LIST_DATASTORE_TYPES", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            debugError("ListDataStores.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_LIST_DATASTORE_TYPES", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }    

    private Map getIDRepoTypesMap()
        throws SMSException, SSOException {
            ServiceSchemaManager schemaMgr = new ServiceSchemaManager(
                IdConstants.REPO_SERVICE, getAdminSSOToken());
            ResourceBundle rb = ResourceBundle.getBundle(
                schemaMgr.getI18NFileName(), getCommandManager().getLocale());
            ServiceSchema orgSchema = schemaMgr.getOrganizationSchema();
            Set names = orgSchema.getSubSchemaNames();
            Map map = new HashMap(names.size() *2);

            for (Iterator iter = names.iterator(); iter.hasNext(); ) {
                String name = (String)iter.next();
                ServiceSchema ss = orgSchema.getSubSchema(name);
                String i18nKey = ss.getI18NKey();

                if ((i18nKey != null) && (i18nKey.trim().length() > 0)) {
                    map.put(rb.getString(i18nKey), name);
                }
            }

            return map;
    }
}
