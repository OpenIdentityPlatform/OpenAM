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
 * $Id: DiscoUtils.java,v 1.5 2008/06/25 05:47:12 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.disco.common;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.BitSet;

import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.assertion.Statement;
import com.sun.identity.liberty.ws.disco.DiscoveryException;
import com.sun.identity.liberty.ws.disco.EncryptedResourceID;
import com.sun.identity.liberty.ws.disco.ResourceOffering;
import com.sun.identity.liberty.ws.disco.ResourceID;
import com.sun.identity.liberty.ws.disco.Description;
import com.sun.identity.liberty.ws.disco.jaxb.*;
import com.sun.identity.liberty.ws.disco.jaxb11.*;
import com.sun.identity.liberty.ws.disco.plugins.NameIdentifierMapper;
import com.sun.identity.liberty.ws.disco.plugins.jaxb.*;
import com.sun.identity.liberty.ws.interfaces.Authorizer;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.liberty.ws.security.*;
import com.sun.identity.liberty.ws.soapbinding.Message;
import com.sun.identity.liberty.ws.soapbinding.ProviderHeader;
import com.sun.identity.liberty.ws.soapbinding.Utils;
import com.sun.identity.liberty.ws.util.ProviderManager;
import com.sun.identity.liberty.ws.util.ProviderUtil;
import com.sun.identity.federation.message.common.EncryptedNameIdentifier;
import com.sun.identity.federation.message.common.IDPProvidedNameIdentifier;

/**
 * Provides utility methods to discovery service.
 */
public class DiscoUtils extends DiscoSDKUtils {

    /**
     * Constructor
     * iPlanet-PRIVATE-DEFAULT-CONSTRUCTOR
     */
    private DiscoUtils() {
    }

    /**
     * Checks policy and returns resource offerings and credentials.
     * @param userDN user DN
     * @param message soap request <code>Message</code> received.
     * @param results <code>Collection</code> of <code>InsertEntryType</code>
     *  objects.
     * @param authorizer <code>Authorizer</code> object.
     * @param invoSession <code>SessionContext</code>
     * @param wscID web service client ID.
     * @param token token of this soap session.
     * @return Map of following key value pairs:
     *  <pre>
     *  Key: <code>OFFERINGS</code>
     *  Value: List of <code>ResourceOffering</code>s
     *  Key: <code>CREDENTIALS</code>
     *  Value: List of credentials (<code>Assertion</code>s)
     *  </pre>
     */
    public static Map checkPolicyAndHandleDirectives(
                                String userDN, Message message,
                                Collection results, Authorizer authorizer,
                                SessionContext invoSession, String wscID,
                                Object token)
    {
        DiscoUtils.debug.message("DiscoService.checkPolicyAndHandleDirectives");

        List offerings = new LinkedList();
        List credentials = new LinkedList();
        Map env = null;
        Iterator k = results.iterator();
        while (k.hasNext()) {
            InsertEntryType entry = (InsertEntryType) k.next();
            if (authorizer != null) {
                if (env == null) {
                    env = new HashMap();
                    env.put(Authorizer.USER_ID, userDN);
                    env.put(Authorizer.AUTH_TYPE,
                        message.getAuthenticationMechanism());
                    env.put(Authorizer.MESSAGE, message);
                }
                if (!authorizer.isAuthorized(message.getToken(),
                        DiscoConstants.ACTION_LOOKUP,
                        entry.getResourceOffering(),
                        env))
                {
                    DiscoUtils.debug.error("DiscoveryService.checkPolicyAndHan"
                        +"dleDirectives: WSC is not authorized to do lookup");
                    continue;
                }
            }
            ResourceOffering current = null;
            try {
                current = new ResourceOffering(
                Utils.convertJAXBToElement(entry.getResourceOffering(), false));
            } catch (Exception ex) {
                DiscoUtils.debug.error("DiscoveryService.checkPolicyAndHandle"
                    +"Directives:exception when constructing ResourceOffering:",
                    ex);
                continue;
            }
            List directives = entry.getAny();
            if ((directives == null) || directives.isEmpty()) {
                DiscoUtils.debug.message("DiscoService: no directives.");
                offerings.add(current);
            } else {
                DiscoUtils.debug.message("DiscoService: has directives.");
                handleDirectives(current, directives, userDN, message,
                        invoSession, wscID, token, offerings, credentials);
            }
        }

        Map returnMap = new HashMap();
        returnMap.put(OFFERINGS, offerings);
        returnMap.put(CREDENTIALS, credentials);
        return returnMap;
    }

    private static String ALL = "all";
    private static int AUTHN = 0;
    private static int AUTHO = 1;
    private static int SESSION = 2;
    private static int BEARER = 3;
    private static int LOGOUT = 4;
    private static int SIZE = 5;
    private static BitSet EMPTY_BITSET = new BitSet(SIZE);
    private static void handleDirectives(ResourceOffering current,
                                        List directives,
                                        String userDN,
                                        Message message,
                                        SessionContext invoSession,
                                        String wscID,
                                        Object token,
                                        List offerings,
                                        List credentials)
    {
        Map descIDDirectiveMap = new HashMap();
        BitSet all = new BitSet(SIZE);
        if (invoSession != null) {
            if (DiscoServiceManager.needSessionContextStatement()) {
                all.set(SESSION);
            }
        }
        Iterator iter0 = directives.iterator();
        while(iter0.hasNext()) {
            Object directive = iter0.next();
            List descIDRefs =
                ((DirectiveType) directive).getDescriptionIDRefs();
            if (directive instanceof EncryptResourceIDElement) {
                debug.message("DiscoService: has encrypt D");
                current = doEncryption(current);
            } else if
                (directive instanceof AuthenticateRequesterElement)
            {
                setMap(descIDRefs, AUTHN, descIDDirectiveMap, all);
            } else if
                (directive instanceof AuthorizeRequesterElement)
            {
                setMap(descIDRefs, AUTHO, descIDDirectiveMap, all);
            } else if
                (directive instanceof AuthenticateSessionContextElement)
            {
                setMap(descIDRefs, SESSION, descIDDirectiveMap, all);
            } else if
                (directive instanceof GenerateBearerTokenElement)
            {
                setMap(descIDRefs, BEARER, descIDDirectiveMap, all);
            } else {
                if (debug.messageEnabled()) {
                    debug.message("DiscoUtils.handleDirective: directive not "
                        + "supported.");
                }
                continue;
            }
        }

        Map directiveCredIDMap = new HashMap();
        Map descIDCredIDMap = new HashMap();
        Iterator iter2 = descIDDirectiveMap.keySet().iterator();
        while (iter2.hasNext()) {
            String descID = (String) iter2.next();
            BitSet dirs = (BitSet) descIDDirectiveMap.get(descID);
            dirs.or(all);
            if (directiveCredIDMap.containsKey(dirs)) {
                descIDCredIDMap.put(descID, 
                        (String) directiveCredIDMap.get(dirs));
            } else {
                String ref = generateCredential(dirs, current, message, userDN,
                                credentials, invoSession, wscID, token);
                if (ref != null) {
                    directiveCredIDMap.put(dirs, ref);
                    descIDCredIDMap.put(descID, ref);
                }
            }
        }
        
        // loop though each description to add credIDRefs
        Iterator descIter =
                current.getServiceInstance().getDescription().iterator();
        List credIDs = null;
        while (descIter.hasNext()) {
            credIDs = new ArrayList();
            Description desc = (Description) descIter.next();
            String id = desc.getId();
            if ((id != null) && (id.length() != 0) &&
                (descIDCredIDMap.containsKey(id)))
            {
                if (debug.messageEnabled()) {
                    debug.message("DiscoUtils.handleDirective: containsKey:"
                                + id);
                }
                credIDs.add((String) descIDCredIDMap.get(id));
            } else {
                debug.message("DiscoUtils.handleDirective:  not containsKey");
                String allCred = (String) descIDCredIDMap.get("all");
                if (allCred == null) {
                    if (directiveCredIDMap.containsKey(all)) {
                        allCred = (String) directiveCredIDMap.get(all);
                        descIDCredIDMap.put("all", allCred);
                        credIDs.add(allCred);
                    } else {
                        if (!all.equals(EMPTY_BITSET)) {
                            allCred = generateCredential(all, current, message,
                                userDN, credentials, invoSession, wscID,token);
                            if (allCred != null) {
                                descIDCredIDMap.put("all", allCred);
                                credIDs.add(allCred);
                            }
                        }
                    }
                } else {
                    credIDs.add(allCred);
                }
            }
            if (!credIDs.isEmpty()) {
                desc.setCredentialRef(credIDs);
            }
        }

        // everything is done, add current to offerings
        offerings.add(current);
        
    }

    private static ResourceOffering doEncryption(ResourceOffering current) {
        ResourceID ri = current.getResourceID();
        if (ri == null) {
            return current;
        }
        try {
            EncryptedResourceID eri =
                        EncryptedResourceID.getEncryptedResourceID(
                                ri,
                                current.getServiceInstance().getProviderID());
            current.setResourceID(null);
            current.setEncryptedResourceID(eri);
        } catch (Exception e) {
            debug.error("DiscoUtils.doEncryption: exception:", e);
        }
        return current;
    }

    private static void setMap(List descIDRefs,
                                int type,
                                Map descIDDirectiveMap,
                                BitSet all)
    {
        if ((descIDRefs == null) || (descIDRefs.size() ==0)) {
            all.set(type);
        } else {
            Iterator iter1 = descIDRefs.iterator();
            while (iter1.hasNext()) {
                String descID = ((DescriptionType) iter1.next()).getId();
                BitSet cur = (BitSet) descIDDirectiveMap.get(descID);
                if (cur == null) {
                    cur = new BitSet(SIZE);
                }
                cur.set(type);
                descIDDirectiveMap.put(descID, cur);
            }
        }
    }

    private static SessionContext getSessionContext(SecurityAssertion assertion)
    {
        if (assertion == null) {
            return null;
        }
        Iterator iter = assertion.getStatement().iterator();
        SessionContext context = null;
        while (iter.hasNext()) {
            Statement st = (Statement) iter.next();
            int type = st.getStatementType();
            if (type == ResourceAccessStatement.RESOURCEACCESS_STATEMENT) {
                context = ((ResourceAccessStatement) st).getSessionContext();
                if (context != null) {
                    return context;
                }
            } else if (type == SessionContextStatement.SESSIONCONTEXT_STATEMENT)
            {
                context = ((SessionContextStatement) st).getSessionContext();
                if (context != null) {
                    return context;
                }
            }
        }
        return null;
    }

    private static String generateCredential(BitSet dirs,
                                                ResourceOffering current,
                                                Message message,
                                                String userDN,
                                                List credentials,
                                                SessionContext invoSession,
                                                String wscID,
                                                Object token)
    {
        SecurityAssertion assertion = null;
        try {
            SecurityTokenManager secuMgr = new SecurityTokenManager(token);

            NameIdentifier senderIdentity = null;
            String providerID = wscID;
            if ((providerID == null) || (providerID.length() == 0)) {
                ProviderHeader ph = message.getProviderHeader();
                if (ph != null) {
                    providerID = ph.getProviderID();
                }
            }

            SessionContext invocatorSession = invoSession;
            if (invocatorSession == null) {
                invocatorSession = getSessionContext(message.getAssertion());
            }

            String tproviderID = current.getServiceInstance().getProviderID();
            if (invocatorSession != null) {
                try {
                    ProviderManager pm = ProviderUtil.getProviderManager();
                    SessionSubject sub = invocatorSession.getSessionSubject();
                    NameIdentifier ni = sub.getNameIdentifier();
                    if ((ni.getFormat() != null) && (ni.getFormat().equals(
                        "urn:liberty:iff:nameid:encrypted")))
                    {
                        ni =EncryptedNameIdentifier.getDecryptedNameIdentifier(
                            ni, pm.getDecryptionKey(
                            DiscoServiceManager.getDiscoProviderID()));
                    }
 
                    NameIdentifier newNi = null;
                    NameIdentifierMapper niMapper = 
                        DiscoServiceManager.getNameIdentifierMapper();
                    if (niMapper != null) {
                        String discoEntityID = 
                            DiscoServiceManager.getDiscoProviderID();
                        newNi = niMapper.getNameIdentifier(
                            tproviderID, discoEntityID, ni, userDN);
                    }
                    if ((newNi != null) && !newNi.equals(ni)) {
                         sub.setNameIdentifier(newNi);
                         // modify IDPProvidedNameIdentifier, this should be
                         // a EncryptedIDPProvidedNameIdentifier, but not 
                         // defined by specification.
                         // Or set this to null once we make it optional
                         // in SessionSubject class implementation 
                         IDPProvidedNameIdentifier idpNi =
                             sub.getIDPProvidedNameIdentifier(); 
                         if (idpNi != null) {
                             IDPProvidedNameIdentifier newIdpNi =
                                 new IDPProvidedNameIdentifier(
                                     newNi.getName(),
                                     newNi.getNameQualifier(),
                                     newNi.getFormat());
                             sub.setIDPProvidedNameIdentifier(newIdpNi); 
                         }
		    } else if (pm.isNameIDEncryptionEnabled(tproviderID)){
                        sub.setNameIdentifier(
                            EncryptedNameIdentifier.getEncryptedNameIdentifier(
                            ni, tproviderID,
                            pm.getEncryptionKey(tproviderID),
                            pm.getEncryptionKeyAlgorithm(tproviderID),
                            pm.getEncryptionKeyStrength(tproviderID)));
                    } else {
                        sub.setNameIdentifier(ni);
                    }
                    invocatorSession.setSessionSubject(sub);
                } catch (Exception ex) {
                    debug.error("DiscoUtils.handleDirective: En/Decryption"
                        + " Exception:", ex);
                    return null;
                }
            }

            Object resourceID = current.getEncryptedResourceID();
            if (resourceID == null) {
                resourceID = current.getResourceID();
                if (resourceID == null) {
                    resourceID = (String) DiscoConstants.IMPLIED_RESOURCE;
                } else {
                    resourceID = ((ResourceID) resourceID).getResourceID();
                }
            }
            if (dirs.get(BEARER)) {
                if (dirs.get(AUTHN) || dirs.get(AUTHO) || dirs.get(SESSION)) {
                    if ((providerID != null) && (providerID.length() != 0)) {
                        senderIdentity = new NameIdentifier(
                                    providerID,
                                    null,
                                    DiscoConstants.PROVIDER_ID_FORMAT);
                    } else {
                        senderIdentity = new NameIdentifier(userDN);
                    }
                    if (resourceID instanceof String) {
                        assertion = secuMgr.getSAMLBearerToken(
                                        senderIdentity,
                                        invocatorSession,
                                        (String) resourceID,
                                        dirs.get(AUTHN),
                                        dirs.get(AUTHO),
                                        tproviderID);
                    } else {
                        assertion = secuMgr.getSAMLBearerToken(
                                        senderIdentity,
                                        invocatorSession,
                                        (EncryptedResourceID) resourceID,
                                        dirs.get(AUTHN),
                                        dirs.get(AUTHO),
                                        tproviderID);
                    }
                }
            } else {
                if ((providerID != null) && (providerID.length() != 0)) {
                    senderIdentity = new NameIdentifier(
                                providerID,
                                null,
                                DiscoConstants.PROVIDER_ID_FORMAT);
                } else {
                    senderIdentity = new NameIdentifier(userDN);
                }
                if (providerID != null) {
                    secuMgr.setCertAlias(ProviderUtil.getProviderManager()
                        .getSigningKeyAlias(providerID));
                } else {
                    X509Certificate wscCert = message.getPeerCertificate();
                    if (wscCert == null) {
                        wscCert = message.getMessageCertificate();
                        if (wscCert == null) {
                            if (debug.messageEnabled()) {
                                debug.message("DiscoUtils.generateCredential:"
                                    + "client cert is null. Cannot generate "
                                    + "credential.");
                            }
                            return null;
                        }
                    }
                    secuMgr.setCertificate(wscCert);
                }

                if (resourceID instanceof String) {
                    assertion = secuMgr.getSAMLAuthorizationToken(
                                        senderIdentity,
                                        invocatorSession,
                                        (String) resourceID,
                                        dirs.get(AUTHN),
                                        dirs.get(AUTHO),
                                        tproviderID);
                } else {
                    assertion = secuMgr.getSAMLAuthorizationToken(
                                        senderIdentity,
                                        invocatorSession,
                                        (EncryptedResourceID) resourceID,
                                        dirs.get(AUTHN),
                                        dirs.get(AUTHO),
                                        tproviderID);
                }
            }
        } catch (Exception ex) {
            debug.error("DiscoUtils.generateCredential:"
                            + "cannot generate credential: ", ex);
        }
        if (assertion == null) {
            debug.error("DiscoUtils.generateCredential: "
                            + "cannot generate credential.");
            
            return null;
        } else {
            credentials.add(assertion);
            return assertion.getAssertionID();
        }
    }

}
