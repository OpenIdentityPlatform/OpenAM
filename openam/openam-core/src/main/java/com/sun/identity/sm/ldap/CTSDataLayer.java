/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock US Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information:
 *
 * "Portions copyright [year] [name of copyright owner]".
 *
 */
package com.sun.identity.sm.ldap;


import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPException;

/**
 * Protected Static Helper Accessor class to Access the protected SMDataLayer.
 * Allows us to securely hack around the accessing a package protected resource.
 *
 * @author jeff.schenk@forgerock.com
 */
public class CTSDataLayer {

    /**
     * Debug Logging
     */
    private static Debug DEBUG = SessionService.sessionDebug;
    /**
     * Singleton Instance.
     */
    private static CTSDataLayer instance = new CTSDataLayer();
    /**
     * Reference to Internally Shared SM Data Layer.
     */
    private static SMDataLayer sharedSMDataLayer = SMDataLayer.getInstance();
    /**
     * Do not allow this class to be instantiated.
     */
    private CTSDataLayer() {
    }

    /**
     * Allow restricted Access by specific Class Caller without using
     * Java Security.
     *
     * @return CTSDataLayer - Wrapper Accessor Class for Shared SM Data Layer Instance.
     */
    public static CTSDataLayer getSharedSMDataLayerAccessor() {
        return instance;
    }

    /**
     * Obtain a Connection from the Shared Pool
     *
     * @return LDAPConnection - Obtained from Pool
     */
    public LDAPConnection getConnection() {
        return sharedSMDataLayer.getConnection();
    }

    /**
     * Release an obtained Connection back to the pool.
     *
     * @param ldapConnection
     */
    public void releaseConnection( LDAPConnection ldapConnection) {
        sharedSMDataLayer.releaseConnection(ldapConnection);
    }

    /**
     * Release an obtained Connection back to the pool, with a
     * specified return code for clean-up.
     *
     * @param ldapConnection
     * @param ldapErrorCode
     */
    public void releaseConnection(LDAPConnection ldapConnection, int ldapErrorCode ) {
        sharedSMDataLayer.releaseConnection(ldapConnection, ldapErrorCode);
    }

    /**
     * Release an obtained Connection back to the pool, with a
     * specified Last LDAP Stack Exception for clean-up.
     *
     * @param ldapConnection
     * @param lastLDAPException - Last up Stream LDAP Exception, can be null if no issues arose
     *                          with the connection or operations.
     */
    public void releaseConnection(LDAPConnection ldapConnection, LDAPException lastLDAPException ) {
        if (lastLDAPException == null) {
            sharedSMDataLayer.releaseConnection(ldapConnection);
        } else {
            if (lastLDAPException.getLDAPResultCode() == LDAPException.CONNECT_ERROR) {
                // We had a connect error using the connection provided, attempt
                // to recovery the connection if possible, before returning to
                // the Connection pool.
                // TODO -- Add additional logic to properly fix a broken connection.
            }
            sharedSMDataLayer.releaseConnection(ldapConnection, lastLDAPException.getLDAPResultCode());
        }
    }

}
