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
 * $Id: SMSObjectDB.java,v 1.4 2008/06/25 05:44:05 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm;

import com.iplanet.services.ldap.*;

import com.iplanet.ums.IUMSConstants;




/**
 * Abstract class that needs to be implemented to get root suffix 
 * configuration data from a datastore.
 */
public abstract class SMSObjectDB extends SMSObject {

    static String amsdkbaseDN;
    static String baseDN;

    /**
     * Returns the AMSDK BaseDN for the UM objects.
     * This is the root suffix.
     */
    public String getAMSdkBaseDN() {
        try {
            // Use puser id just to get the baseDN from serverconfig.xml
            // from "default" server group. This is for user management 
            // baseDN.
            ServerInstance serverInstanceForUM = null;
            DSConfigMgr mgr = DSConfigMgr.getDSConfigMgr();
            if (mgr != null) {
                serverInstanceForUM =
                    mgr.getServerInstance(LDAPUser.Type.AUTH_PROXY);
            }
            if (serverInstanceForUM != null) {
                amsdkbaseDN = serverInstanceForUM.getBaseDN();
            }
            if ((mgr == null) || 
                (serverInstanceForUM == null)) {
                debug().error("SMSObjectDB: Unable to initialize LDAP");
                throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.CONFIG_MGR_ERROR, null));
            }
            if (debug().messageEnabled()) {
                debug().message("SMSObjectDB: amsdkbasedn: "+amsdkbaseDN);
            }

        } catch (Exception e) {
            debug().error("SMSObjectDB: Unable to get amsdkbasedn:", e);
        }
        return (amsdkbaseDN);
    }

    /**
     * Returns the BaseDN for the SM objects.
     * This is the root suffix.
     */
    public String getRootSuffix() {
        try {
            // Use puser id just to get the baseDN from serverconfig.xml
            // from "sms" server group. This is for SM base DN.
            ServerInstance serverInstanceForSM = null;
            DSConfigMgr mgr = DSConfigMgr.getDSConfigMgr();
            if (mgr != null) {
                serverInstanceForSM = mgr.getServerInstance(
                    SMSEntry.SMS_SERVER_GROUP,LDAPUser.Type.AUTH_PROXY);
            }
            if (serverInstanceForSM != null) {
                baseDN = serverInstanceForSM.getBaseDN();
            }
            if ((mgr == null) || 
                (serverInstanceForSM == null)) {
                baseDN = getAMSdkBaseDN();
                if (debug().warningEnabled()) {
                    debug().warning("SMSObjectDB: SMS servergroup not "+
                    "available. Returning the default baseDN: "+baseDN);
                }
            }
            if (debug().messageEnabled()) {
                debug().message("SMSObjectDB: basedn: "+baseDN);
            }

        } catch (Exception e) {
            baseDN = getAMSdkBaseDN();
            if (debug().warningEnabled()) {
                debug().warning("SMSObjectDB: SMS servergroup not "+
                    "available. Returning the default baseDN: "+baseDN);
            }
        }
        return (baseDN);
    }
}
