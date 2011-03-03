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
 * $Id: FSDiscoveryBootStrap.java,v 1.4 2008/12/05 00:18:00 exu Exp $
 *
 */


package com.sun.identity.federation.services;

import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.message.FSSubject;
import com.sun.identity.federation.message.common.AuthnContext;
import com.sun.identity.federation.message.common.EncryptedNameIdentifier;
import com.sun.identity.liberty.ws.disco.ResourceOffering;
import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.common.DiscoServiceManager;
import com.sun.identity.liberty.ws.disco.common.DiscoUtils;
import com.sun.identity.liberty.ws.disco.jaxb.*;
import com.sun.identity.liberty.ws.disco.plugins.jaxb.DiscoEntryElement;
import com.sun.identity.liberty.ws.interfaces.ResourceIDMapper;
import com.sun.identity.liberty.ws.security.*;
import com.sun.identity.saml.assertion.*;
import com.sun.identity.saml.common.*;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Document;


/**
 * The class <code>FSDiscoBootStrap</code> helps in generating the discovery
 * boot strap statement i.e. Discovery Resource Offering as part of the SAML
 * assertion that is generated during the Single Sign-On. This class checks
 * if there are any credentials that need to be generated for accesing 
 * discovery service and do the needful.
 */
public class FSDiscoveryBootStrap {

    private AttributeStatement _bootStrapStatement = null;
    private List _assertions = null;
    private Object _ssoToken = null;
    private boolean _hasCredentials = false;

    /**
     * Constructor.
     * @param ssoToken session of the user.
     *  TODO: Currently we use the session of the user, but ideally this
     *  be of the WSC that require credentials.
     * @param authnContext Authentication context that the user is signed-on.
     * @param sub Federated Subject.
     * @param userID User's ID for which the discovery resource offering is
     *  being obtained.
     * @param wscID the wsc's entity ID
     * @param realm the realm in which the provider resides
     * @exception FSException if there is any failure. 
     */
    public FSDiscoveryBootStrap(
        Object ssoToken,
        AuthnContext authnContext,
        FSSubject sub,
        String userID,
        String wscID,
        String realm)
        throws FSException
    {
        if (sub == null || userID == null) {
            FSUtils.debug.error("FSDiscoBootStrap: null values.");
            throw new FSException("nullInputParameter", null);
        }
        this._ssoToken = ssoToken;
        try {
            List attributeList = new ArrayList();
            List resourceOfferings = new ArrayList();
            Document offering = getResourceOffering(
                sub, authnContext, userID, wscID, realm);
            resourceOfferings.add(offering.getDocumentElement());
            Attribute attribute =
                new Attribute(IFSConstants.DISCO_RESOURCE_OFFERING_NAME,
                        DiscoConstants.DISCO_NS,
                        resourceOfferings);
            attributeList.add(attribute);
            _bootStrapStatement = new AttributeStatement(sub, attributeList);
        } catch (Exception ex) {
            FSUtils.debug.error("FSDiscoBootStrap: Constructor" +
                "while creating discovery bootstrap statement", ex);
            throw new FSException(ex);
        }

    }

    /**
     * Gets the discovery bootstrap resource offering for the user.
     * @return Document Discovery Resource Offering in an attribute statement
     * @exception FSException if there's any failure.
     */
    private Document getResourceOffering(
        FSSubject libSubject,
        AuthnContext authnContext,
        String userID,
        String wscID,
        String realm)
        throws FSException
    {

        FSUtils.debug.message("FSDiscoveryBootStrap.getResourceOffering:Init");
        StringBuffer sb = new StringBuffer(300);
        sb.append("<").append(SAMLConstants.ASSERTION_PREFIX)
            .append("AttributeValue").append(SAMLConstants.assertionDeclareStr)
            .append(">").append(SAMLConstants.NL);

        DiscoEntryElement discoEntry =
            DiscoServiceManager.getBootstrappingDiscoEntry();
        if (discoEntry == null) {
            throw new FSException("nullDiscoveryOffering", null);
        }

        try {
            ResourceOfferingType offering = discoEntry.getResourceOffering();
            ServiceInstanceType serviceInstance = offering.getServiceInstance();
            String providerID = serviceInstance.getProviderID();
            if (!DiscoServiceManager.useImpliedResource()) {
                ResourceIDMapper idMapper =
                    DiscoServiceManager.getResourceIDMapper(providerID);
                if (idMapper == null) {
                    idMapper = DiscoServiceManager.getDefaultResourceIDMapper();
                }
                ObjectFactory fac =
                    new com.sun.identity.liberty.ws.disco.jaxb.ObjectFactory();
                ResourceIDType resourceID = fac.createResourceIDType();
                String resourceIDValue = idMapper.getResourceID(
                    providerID, userID);

                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSDiscoveryBootStrap.getResource" +
                        "Offering: ResourceID Value:" + resourceIDValue);
                }
                resourceID.setValue(resourceIDValue);
                offering.setResourceID(resourceID);
            } else {
                ObjectFactory fac =
                    new com.sun.identity.liberty.ws.disco.jaxb.ObjectFactory();
                ResourceIDType resourceID = fac.createResourceIDType();
                resourceID.setValue(DiscoConstants.IMPLIED_RESOURCE);
                offering.setResourceID(resourceID);
            }

            List discoEntryList = new ArrayList();
            discoEntryList.add(discoEntry);
            SessionSubject sessionSubject = null;
            if (DiscoServiceManager.encryptNIinSessionContext()) {
                sessionSubject = new SessionSubject(
                    EncryptedNameIdentifier.getEncryptedNameIdentifier(
                        libSubject.getNameIdentifier(),
                        realm, 
                        providerID),
                    libSubject.getSubjectConfirmation(),
                    libSubject.getIDPProvidedNameIdentifier());
            } else {
                sessionSubject = new SessionSubject(
                    libSubject.getNameIdentifier(),
                    libSubject.getSubjectConfirmation(),
                    libSubject.getIDPProvidedNameIdentifier());
            }

            SessionContext invocatorSession = new SessionContext(
                sessionSubject, authnContext, providerID);

            Map map = DiscoUtils.checkPolicyAndHandleDirectives(
                userID, null, discoEntryList, null,
                invocatorSession, wscID, _ssoToken);
            List offerings = (List) map.get(DiscoUtils.OFFERINGS);
            if (offerings.isEmpty()) {
                FSUtils.debug.message(
                    "FSDiscoBootStrap.getResourceOffering:no ResourceOffering");
                throw new FSException("nullDiscoveryOffering", null);
            }
            ResourceOffering resourceOffering =
                (ResourceOffering) offerings.get(0);
            _assertions = (List) map.get(DiscoUtils.CREDENTIALS);
            if ((_assertions != null) && (_assertions.size() != 0)) {
                _hasCredentials = true;
            }

            sb.append(resourceOffering.toString());
            sb.append("</").append(SAMLConstants.ASSERTION_PREFIX)
                .append("AttributeValue>");
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSDiscoveryBootStap.getResourceOffering:Resource Offering:"
                    + sb.toString());
            }
            return XMLUtils.toDOMDocument(sb.toString(), null);
       } catch (Exception ex) {
            FSUtils.debug.error("FSDiscoveryBootStrap.getResourceOffering:" +
                "Exception while creating resource offering.", ex);
            throw new FSException(ex);
       }

    }

    /**
     * Checks if the credentials are generated.
     * @return <code>true</code> if the credentials are generated;
     *  <code>false</code> otherwise.
     */
    public boolean hasCredentials() {
        return _hasCredentials;
    }

    /**
     * Returns the credentials for discovery boot strap resource offering.
     * @return <code>Advice</code> object that contains credentials
     */
    public Advice getCredentials() {
        if ((_assertions != null) && (_assertions.size() != 0)) {
            List assertionList = new ArrayList();
            assertionList.addAll(_assertions);
            return new Advice(null, assertionList, null);
        } 
        return null;
    }

    /**
     * Returns the bootstrap attribute statement.
     * @return AttributeStatement ResourceOffering AttributeStatement.
     */
    public AttributeStatement getBootStrapStatement() {
        return _bootStrapStatement;
    }

}
