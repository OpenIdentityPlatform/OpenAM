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
 * $Id: GetBackendDataStore.java,v 1.4 2008/06/25 05:44:04 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;

/**
 * Class that implements to get backend datastore based on the vendor
 * information in the schema of the respective ldapv3 based database.
 * Code is to be added in future for other directory server support
 * like openldap.
 */
public class GetBackendDataStore {
    private static Debug debug = Debug.getInstance("amSMS");

    private GetBackendDataStore() {
    }
    
    public static String getDataStore(SSOToken token) {
        String dataStore = SMSEntry.DATASTORE_FLAT_FILE;
        
        if (!isFlatFile()) {
            dataStore = isSunDS(token) ? SMSEntry.DATASTORE_SUN_DIR :
                SMSEntry.DATASTORE_ACTIVE_DIR;
        }
        if (debug.messageEnabled()) {
            debug.message("GetBackendDataStore.getDataStore: datastore=" +
                dataStore);
        }
        return dataStore;
    }
    
    
    private static boolean isSunDS(SSOToken token) {
        return true;
        // The rest of the code is commentd for now - revisit when if
        // ActiveDirectory support is planned.

        //String srchBaseDN = "cn=7-bit check,cn=plugins,cn=config";
        //String filter = "nsslapd-pluginVendor=Sun Microsystems, Inc.";

        //try {
            //Set results = SMSEntry.search(token, srchBaseDN, filter);
            //return (results != null);
        //} catch (SMSException smse) {
            ////ignore
        //}
        //return false;
    }
    
    private static boolean isFlatFile() {
        String plugin = SystemProperties.get(SMSEntry.SMS_OBJECT_PROPERTY);
        return (plugin != null) &&
            (plugin.equals(SMSEntry.FLATFILE_SMS_CLASS_NAME));
    }
    
}
