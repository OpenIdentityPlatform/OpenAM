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
 * $Id: SPSessionListener.java,v 1.6 2009/09/23 22:28:32 bigfatrat Exp $
 *
 */


package com.sun.identity.saml2.profile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;

import com.sun.identity.plugin.monitoring.FedMonAgent;
import com.sun.identity.plugin.monitoring.FedMonSAML2Svc;
import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.plugin.session.SessionListener;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.saml2.common.NameIDInfoKey;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.shared.debug.Debug;


/**
 * The class <code>SPSessionListener</code> implements
 * SessionListener interface and is used for maintaining the 
 * SP session cache.
 */

public class SPSessionListener implements SessionListener {

    private static SAML2MetaManager sm = null;
    private static Debug debug = SAML2Utils.debug;
    private static FedMonAgent agent;
    private static FedMonSAML2Svc saml2Svc;
    static {
        try {
            sm = new SAML2MetaManager();
        } catch (SAML2MetaException sme) {
            debug.error("Error retreiving metadata",sme);
        }
        agent = MonitorManager.getAgent();
        saml2Svc = MonitorManager.getSAML2Svc();
    }
    
    private String infoKeyString = null;
    private String sessionID = null;

    /**
     *  Constructor of <code>SPSessionListener</code>.
     */
    public SPSessionListener(String infoKeyString, String sessionID) {
        this.infoKeyString = infoKeyString;
        this.sessionID = sessionID;
    }

    /**
     *  Callback for SessionListener.
     *  It is used for cleaning up the SP session cache.
     *  
     *  @param session The session object
     */
    public void sessionInvalidated(Object session)
    {
        String classMethod = "SPSessionListener.sessionInvalidated: ";
        HashMap paramsMap = new HashMap();
        NameIDInfoKey nameIdInfoKey = null;
        
        if (session == null || infoKeyString == null ||
            sessionID == null) {
            return;
        }
        SessionProvider sessionProvider = null;
        SPFedSession fedSession = null;

        try {
            sessionProvider = SessionManager.getProvider();
        } catch (SessionException se) {
            return;
        }
        if (!sessionID.equals(sessionProvider.getSessionID(session)))
        {
            return;
        }
        List fedSessionList = (List)
            SPCache.fedSessionListsByNameIDInfoKey.get(infoKeyString);
        if (fedSessionList == null) {
            return;
        }

        try {
            Iterator iter = fedSessionList.iterator();
            while (iter.hasNext()) {
                fedSession = (SPFedSession) iter.next();
                if (fedSession.spTokenID.equals(sessionID)) {

                    paramsMap.put(SAML2Constants.ROLE, SAML2Constants.SP_ROLE);

                    String metaAlias = fedSession.metaAlias;

                    nameIdInfoKey = NameIDInfoKey.parse(infoKeyString);

                    String spEntityID = sm.getEntityByMetaAlias(metaAlias);

                    String realm =
                            SAML2Utils.getRealm(
                                SAML2MetaUtils.getRealmByMetaAlias(metaAlias));

                    BaseConfigType spConfig =
                                        sm.getSPSSOConfig(realm, spEntityID);
                    if (spConfig != null) {
                        List spSessionSyncList =
                            (List) SAML2MetaUtils.getAttributes(spConfig).
                                get(SAML2Constants.SP_SESSION_SYNC_ENABLED);

                        if (spEntityID != null &&
                            spSessionSyncList != null &&
                            (spSessionSyncList.size() != 0)) {
                         
                             boolean spSessionSyncEnabled =
                                ((String)spSessionSyncList.get(0)).
                                      equals(SAML2Constants.TRUE)? true : false;
                             // Initiate SP SLO on SP Idle/Max
                             // session timeout only when Session Sync flag
                             // is enabled
                             if (spSessionSyncEnabled) {
                                 if (SAML2Utils.debug.messageEnabled()) {
                                     SAML2Utils.debug.message(
                                         classMethod +
                                         "SP Session Synchronization flag " +
                                         "is enabled, initiating SLO to IDP");
                                 }
                                 initiateSPSingleLogout(metaAlias,
                                                        realm,
                                                        SAML2Constants.SOAP,
                                                        nameIdInfoKey,
                                                        fedSession,
                                                        paramsMap);
                             }
                        }
                    } else {
                        if (SAML2Utils.debug.messageEnabled()) {
                            SAML2Utils.debug.message(
                                         classMethod +
                                         "Unable to retrieve the SP config" +
                                         " data, spConfig is null");
                        }
                    }
                }
            }
        } catch (SAML2MetaException sme) {
                SAML2Utils.debug.error(
                    "SPSessionListener.sessionInvalidated:", sme);
        } catch (SAML2Exception se) {
                SAML2Utils.debug.error(
                    "SPSessionListener.sessionInvalidated:", se);
        } catch (SessionException s) {
                SAML2Utils.debug.error(
                           "IDPSessionListener.sessionInvalidated:", s);
        }
        
        synchronized (fedSessionList) {
            Iterator iter = fedSessionList.iterator();
            while (iter.hasNext()) {
                fedSession = (SPFedSession) iter.next();
                if (fedSession.spTokenID.equals(sessionID)) {
                    iter.remove();
                    if ((agent != null) &&
                        agent.isRunning() &&
                        (saml2Svc != null))
                    {
                        saml2Svc.setFedSessionCount(
		            (long)SPCache.fedSessionListsByNameIDInfoKey.
				size());
                    }
                }
            }
            if (fedSessionList.isEmpty()) {
                SPCache.fedSessionListsByNameIDInfoKey.remove(infoKeyString);
            }
        }
    }


    /**
     * Initiates SP Single logout <code>initiateIDPSingleLogout</code> to
     * the IDP
     *
     * @param metaAlias SP meta alias
     * @param realm Realm
     * @param binding Binding used
     * @param nameIdInfoKey the nameIdInfoKey
     * @param fedSession SP Federated session
     * @param paramsMap  parameters map
     *
     * <code>initiateIDPSingleLogout</code>.
     * @return
     * @throws SAML2MetaException if error processing
     *          <code>initiateIDPSingleLogout</code>.
     * @throws SAML2Exception if error processing
     *          <code>initiateIDPSingleLogout</code>.
     * @throws SessionException if error processing
     *          <code>initiateIDPSingleLogout</code>.
     */
    static private void initiateSPSingleLogout(String metaAlias,
                                        String realm,
                                        String binding,
                                        NameIDInfoKey nameIdInfoKey,
                                        SPFedSession fedSession,
                                        Map paramsMap)
                                        throws SAML2MetaException,
                                        SAML2Exception, SessionException {   

        // get IDPSSODescriptor
        IDPSSODescriptorElement idpsso =
            sm.getIDPSSODescriptor(realm,nameIdInfoKey.getRemoteEntityID());

        if (idpsso == null) {
            String[] data = {nameIdInfoKey.getRemoteEntityID()};
            LogUtil.error(Level.INFO,LogUtil.IDP_METADATA_ERROR,data, null);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("metaDataError"));
        }

        List slosList = idpsso.getSingleLogoutService();
        if (slosList == null) {
            String[] data = {nameIdInfoKey.getRemoteEntityID()};
            LogUtil.error(Level.INFO,LogUtil.SLO_NOT_FOUND,data, null);
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("sloServiceListNotfound"));
        }

        IDPSSOConfigElement idpConfig = sm.getIDPSSOConfig(realm,
                                             nameIdInfoKey.getRemoteEntityID());
                
        LogoutUtil.doLogout(metaAlias,
                            nameIdInfoKey.getRemoteEntityID(),
                            slosList, null, binding,
                            null,
                            fedSession.idpSessionIndex,
                            fedSession.info.getNameID(),
                            null, null, paramsMap, idpConfig);

    }
}
