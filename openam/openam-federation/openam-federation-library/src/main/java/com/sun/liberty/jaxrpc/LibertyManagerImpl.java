/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LibertyManagerImpl.java,v 1.4 2009/11/12 23:25:34 exu Exp $
 *
 */

package com.sun.liberty.jaxrpc;

import java.rmi.RemoteException;

import com.sun.identity.liberty.ws.disco.ResourceOffering;
import com.sun.identity.liberty.ws.disco.DiscoveryException;
import com.sun.identity.liberty.ws.security.SecurityAssertion;
import com.sun.identity.federation.services.FSSession;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;

import java.util.List;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

/**
 * This class <code>LibertyManagerImpl</code> implements the remotable
 * <code>LibertyManagerIF</code> so that the JAX-RPC server endpoint can
 * invoke while processing for the remote client requests.
 */
public class LibertyManagerImpl implements LibertyManagerIF {

    /**
     * Returns the discovery service bootstrap resource offering. 
     * @param tokenID Single Sign On Token ID.
     * @param hostProviderID Hosted <code>ProviderID</code>.
     * @return <code>String</code> Discovery Service Resource Offering.
     * @exception RemoteException if any failure.
     */
    public String getDiscoveryResourceOffering(
        String tokenID,
        String hostProviderID
    ) throws RemoteException {

        try {
            Object token = SessionManager.getProvider().getSession(tokenID);
    
            FSSession session = FSSessionManager.getInstance(
                IDFFMetaUtils.getMetaAlias(
                    IFSConstants.ROOT_REALM, hostProviderID, 
                    IFSConstants.SP, null)).
                        getSession(token);

            if (session == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("LibertyManagerImpl.getDiscovery:" +
                       "ResourceOffering: no FSSession found");
                }
                return null;
            }

            NodeList bootStrapRO = session.getBootStrapResourceOfferings();
            if (bootStrapRO == null || bootStrapRO.getLength() == 0) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("LibertyManagerImpl.getDiscovery:" +
                    "ResourceOffering: bootstrap resource offering is null");
                }
                return null;
            }

            ResourceOffering offering = 
                  new ResourceOffering((Element)bootStrapRO.item(0));
            return offering.toString();
        } catch (SessionException se) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "LibertyManagerImpl.getDiscoveryResource" +
                    "Offering: SessionException", se);
            }
            throw new RemoteException (
                FSUtils.bundle.getString("invalidSSOToken"));
        } catch (DiscoveryException de) {
            FSUtils.debug.error("LibertyManagerImpl.getDiscoveryResource" +
                "Offering: Resource Offering parsing error", de);
            throw new RemoteException (
                FSUtils.bundle.getString("invalidResourceOffering"));
        }
    }

    /**
     * Returns the discovery service credential.
     * @param tokenID Single Sign On Token ID.
     * @param hostProviderID Hosted <code>ProviderID</code>.
     * @return <code>String</code> Credential to access the discovery service.
     *         <code>null</code> if the credential does not present.
     * @exception RemoteException if any failure.
     */ 
    public String getDiscoveryServiceCredential(
        String tokenID,
        String hostProviderID
    ) throws RemoteException {

        try {
            Object token = SessionManager.getProvider().getSession(tokenID);

            FSSession session = FSSessionManager.getInstance(
                IDFFMetaUtils.getMetaAlias(
                    IFSConstants.ROOT_REALM, hostProviderID, 
                    IFSConstants.SP, null)).
                        getSession(token);

            if (session == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                       "LibertyManagerImpl.getDiscoveryServiceCredential:" +
                       "ResourceOffering: no FSSession found");
                }
                return null;
            }
            List creds = session.getBootStrapCredential();
            if (creds == null || creds.size() == 0) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("LibertyManagerImpl.getDiscovery:" +
                        "ServiceCredential: bootstrap credential is null");
                }
                return null;
            }

            return ((SecurityAssertion)creds.get(0)).toString();
        } catch (SessionException se) {
           if (FSUtils.debug.messageEnabled()) {
               FSUtils.debug.message("LibertyManagerImpl.getDiscoveryService" +
                   "Credential: SessionException", se);
           }
           throw new RemoteException (
               FSUtils.bundle.getString("invalidSSOToken"));
        }
    }

}
