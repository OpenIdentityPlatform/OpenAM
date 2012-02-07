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
 * $Id: LibertyManagerClient.java,v 1.5 2008/08/19 19:11:17 veiming Exp $
 *
 */
package com.sun.liberty.jaxrpc;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import org.w3c.dom.Document;

import com.sun.identity.liberty.ws.security.SecurityAssertion;
import com.sun.identity.liberty.ws.disco.ResourceOffering;
import com.sun.identity.liberty.ws.disco.DiscoveryException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.shared.xml.XMLUtils;


/**
 * This <code>final</code> class is used to retrieve the Liberty protocol
 * objects such as Discovery Service Boot Strap information after 
 * successful Liberty ID-FF(Identity Federation) Single Sign-on. This 
 * information will be used by the remote OpenSSO clients such as 
 * web service client providers for bootatrapping into Liberty ID-WSF
 * (Identity web services framework).
 */  
public final class LibertyManagerClient {

    private static final String SERVICE_NAME = "LibertyManagerIF";
    private static SOAPClient client = new SOAPClient(SERVICE_NAME);
    public static final String DISCO_RO = "_DiscoveryResourceOffering";
    public static final String DISCO_CRED = "_DiscoveryCredential";
    public static Map bootStrapCache = 
                Collections.synchronizedMap(new HashMap());

    /**
     * Constructs the LibertyManager Client
     */
    public LibertyManagerClient() {}

    /**
     * Returns the discovery service bootstrap resource offering. 
     * @param token Single Sign On Token.
     * @param hostProviderID Hosted <code>ProviderID</code>.
     * @return <code>ResourceOffering</code> Discovery Service bootstrap
     *  resource offering.
     * @exception FSException if any failure.
     */
    public ResourceOffering getDiscoveryResourceOffering(
        Object token, 
        String hostProviderID
    ) throws FSException
    {
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            String tokenID = sessionProvider.getSessionID(token);
            String cacheKey = tokenID + DISCO_RO;
            ResourceOffering ro = 
                    (ResourceOffering)bootStrapCache.get(cacheKey);
            if (ro != null) {
                return ro;
            }

            String[] objs = { tokenID, hostProviderID }; 
            String resourceOffering = (String)client.send(
                "getDiscoveryResourceOffering", objs, null, null);

            if ((resourceOffering == null) || (resourceOffering.length() == 0))
            {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("LibertyManagerClient.getDiscovery" +
                        "ResourceOffering: ResouceOffering is null or empty");
                }
                return null;
            }

            Document doc = XMLUtils.toDOMDocument(resourceOffering,
                FSUtils.debug);
            ro =  new ResourceOffering(doc.getDocumentElement());
            sessionProvider.addListener(
                token, new LibertyClientSSOTokenListener());
            bootStrapCache.put(cacheKey, ro);
            return ro;

        } catch (SessionException se) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("LibertyManagerClient.getDiscovery" +
                    "ResourceOffering: InvalidSessionToken", se); 
            }
            throw new FSException(
                FSUtils.bundle.getString("invalidSSOToken")); 

        } catch (DiscoveryException de) {
            FSUtils.debug.error("LibertyManagerClient.getDiscovery" +
                "ResourceOffering: Invalid ResourceOffering", de); 
            throw new FSException(
                FSUtils.bundle.getString("invalidResourceOffering")); 

        } catch (Exception ex) {
            FSUtils.debug.error("LibertyManagerClient.getDiscovery" +
                "ResourceOffering: SOAPClient Exception", ex); 
            throw new FSException(
                FSUtils.bundle.getString("soapException")); 
        }
    }

    /**
     * Returns the discovery service credential.
     * @param token Single Sign On Token.
     * @param hostProviderID Hosted <code>ProviderID</code>.
     * @return <code>SecurityAssertion</code> Discovery Service Bootstrap
     *         Credential.
     * @exception FSException if any failure.
     */ 
    public SecurityAssertion getDiscoveryServiceCredential(
        Object token, 
        String hostProviderID
    ) throws FSException
    {
        try {
            String tokenID = SessionManager.getProvider().getSessionID(token);
            String cacheKey = tokenID + DISCO_CRED;
            SecurityAssertion cred = 
                    (SecurityAssertion)bootStrapCache.get(cacheKey);
            if (cred != null) {
                return cred;
            }

            String[] objs = { tokenID, hostProviderID };
            String credential = (String)client.send(
                "getDiscoveryServiceCredential", objs, null, null);
 
            if ((credential == null) || (credential.length() == 0)) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("LibertyManagerClient.getDiscovery" +
                        "ServiceCredential: Credential is null or empty"); 
                }
                return null;
            }
            
            Document doc = XMLUtils.toDOMDocument(credential, FSUtils.debug);
            cred = new SecurityAssertion(doc.getDocumentElement()); 
            bootStrapCache.put(cacheKey, cred); 
            return cred;
        } catch (SessionException se) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("LibertyManagerClient.getDiscovery" +
                    "ServiceCredential: InvalidSessionToken", se); 
            }
            throw new FSException(
                FSUtils.bundle.getString("invalidSSOToken")); 

        } catch (DiscoveryException de) {
            FSUtils.debug.error("LibertyManagerClient.getDiscovery" +
                "ServiceCredential: InvalidAssertion", de); 
            throw new FSException(
                FSUtils.bundle.getString("invalidCredential"));

        } catch (Exception ex) {
            FSUtils.debug.error("LibertyManagerClient.getDiscovery" +
                "ResourceOffering: SOAPClient Exception", ex); 
            throw new FSException(
                FSUtils.bundle.getString("soapException")); 

        }
    }

}
