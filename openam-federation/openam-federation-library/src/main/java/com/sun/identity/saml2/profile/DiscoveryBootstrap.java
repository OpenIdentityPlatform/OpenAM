/*
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
 * $Id: DiscoveryBootstrap.java,v 1.4 2008/12/05 00:18:31 exu Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */
package com.sun.identity.saml2.profile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.message.common.AuthnContext;
import com.sun.identity.federation.message.common.EncryptedNameIdentifier;
import com.sun.identity.federation.message.common.IDPProvidedNameIdentifier;
import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.common.DiscoServiceManager;
import com.sun.identity.liberty.ws.disco.common.DiscoUtils;
import com.sun.identity.liberty.ws.disco.jaxb.ObjectFactory;
import com.sun.identity.liberty.ws.disco.jaxb.ResourceIDType;
import com.sun.identity.liberty.ws.disco.jaxb.ResourceOfferingType;
import com.sun.identity.liberty.ws.disco.jaxb.ServiceInstanceType;
import com.sun.identity.liberty.ws.disco.plugins.jaxb.DiscoEntryElement;
import com.sun.identity.liberty.ws.disco.ResourceOffering;
import com.sun.identity.liberty.ws.interfaces.ResourceIDMapper;
import com.sun.identity.liberty.ws.security.SessionContext;
import com.sun.identity.liberty.ws.security.SessionSubject;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml2.assertion.Advice;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.AttributeStatement;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.SubjectConfirmation;
import com.sun.identity.saml2.assertion.SubjectConfirmationData;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.key.EncInfo;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.xml.XMLUtils;


/**
 * The class <code>DiscoBootstrap</code> helps in generating the discovery
 * boot strap statement i.e. Discovery Resource Offering as part of the SAML2
 * assertion that is generated during the Single Sign-On. This class checks
 * if there are any credentials that need to be generated for accesing 
 * discovery service and do the needful.
 */

public class DiscoveryBootstrap {

    private AttributeStatement bootstrapStatement = null;
    private List assertions = null;
    private Object session = null;

    /**
     * Constructor. 
     * @param session session of the user.
     * @param sub SAML2 Subject.
     * @param authnContextClassRef Authentication context class ref
     *     that the user is signed-on.
     * @param wscID wsc entity ID.
     * @param realm the realm name.
     * @exception SAML2Exception if there is any failure. 
     */
    public DiscoveryBootstrap(Object session, Subject sub,
        String authnContextClassRef, String wscID, String realm)
        throws SAML2Exception {

        this.session = session;
        try {
            List attributeList = new ArrayList();
            List resourceOfferings = new ArrayList();
            String offering = getResourceOffering(authnContextClassRef,
                sub, wscID, realm);
            resourceOfferings.add(offering);
            Attribute attribute =
                AssertionFactory.getInstance().createAttribute();
            attribute.setName(
                SAML2Constants.DISCOVERY_BOOTSTRAP_ATTRIBUTE_NAME);
            attribute.setNameFormat(
                SAML2Constants.DISCOVERY_BOOTSTRAP_ATTRIBUTE_NAME_FORMAT);
            attribute.setAttributeValueString(resourceOfferings);
            attributeList.add(attribute);

            bootstrapStatement =
                AssertionFactory.getInstance().createAttributeStatement();
            bootstrapStatement.setAttribute(attributeList);
        } catch (Exception ex) {
            SAML2Utils.debug.error("DiscoveryBootstrap.DiscoveryBootstrap: " +
                "while creating discovery bootstrap statement", ex);
            throw new SAML2Exception(ex);
        }

    }

    /**
     * Gets the discovery bootstrap resource offering for the user.
     * @return Discovery Resource Offering String
     * @exception  SAML2Exception if there's any failure.
     */
    private String getResourceOffering(String authnContextClassRef,
        Subject subject, String wscID, String realm) throws SAML2Exception {

        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                "DiscoveryBootstrap.getResourceOffering:Init");
        }

        DiscoEntryElement discoEntry =
            DiscoServiceManager.getBootstrappingDiscoEntry();
        if (discoEntry == null) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("missingUnivID"));
        }

        String[] values = null;
        try {
            values = SessionManager.getProvider().getProperty(session,
                Constants.UNIVERSAL_IDENTIFIER);
        } catch (SessionException se) {
            throw new SAML2Exception(se);
        }

        if ((values == null) || (values.length == 0)) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("missingDiscoOffering"));
        }

         String univID = values[0];

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

                ObjectFactory fac = new ObjectFactory();
                ResourceIDType resourceID = fac.createResourceIDType();
                String resourceIDValue = idMapper.getResourceID(providerID,
                    univID);

                if(SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(
                        "DiscoveryBootstrap.getResourceOffering: " +
                        "ResourceID Value:" + resourceIDValue);
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
                IDPSSODescriptorElement idpSSODesc = SAML2Utils
                    .getSAML2MetaManager().getIDPSSODescriptor(realm,
                    providerID);
                EncInfo encInfo = KeyUtil.getEncInfo(idpSSODesc, wscID,
                    SAML2Constants.IDP_ROLE);

                NameIdentifier ni =
                    EncryptedNameIdentifier.getEncryptedNameIdentifier(
                    convertSPNameID(subject.getNameID()), providerID,
                    encInfo.getWrappingKey(),
                    encInfo.getDataEncAlgorithm(),
                    encInfo.getDataEncStrength());
                sessionSubject = new SessionSubject(
                  ni,
                  convertSC(subject.getSubjectConfirmation()),
                  convertIDPNameID(subject.getNameID()));
            } else {
                sessionSubject = new SessionSubject(
                    convertSPNameID(subject.getNameID()),
                    convertSC(subject.getSubjectConfirmation()),
                    convertIDPNameID(subject.getNameID()));
            }

            AuthnContext authnContext = new AuthnContext(authnContextClassRef,
                null);
            authnContext.setMinorVersion(
                IFSConstants.FF_12_PROTOCOL_MINOR_VERSION);
            SessionContext invocatorSession = new SessionContext(
                sessionSubject, authnContext, providerID);

            Map map = DiscoUtils.checkPolicyAndHandleDirectives(univID, null,
                discoEntryList, null, invocatorSession, wscID, session);
            List offerings = (List) map.get(DiscoUtils.OFFERINGS);
            if (offerings.isEmpty()) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(
                        "DiscoveryBootstrap.getResourceOffering:" +
                        "no ResourceOffering");
                }
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("missingDiscoOffering"));
            }
            ResourceOffering resourceOffering =
                (ResourceOffering) offerings.get(0);
            assertions = (List) map.get(DiscoUtils.CREDENTIALS);

            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "DiscoveryBootstrap.getResourceOffering: "+
                    "Resource Offering:" + resourceOffering);
            }
            return resourceOffering.toString();
        } catch (Exception ex) {
            SAML2Utils.debug.error("DiscoveryBootstrap.getResourceOffering:" +
            "Exception while creating resource offering.", ex);
            throw new SAML2Exception(ex);
        }

    }

    /**
     * Gets the bootstrap attribute statement
     * @return AttributeStatement ResourceOffering AttributeStatement.
     */
    public AttributeStatement getBootstrapStatement() {
        return bootstrapStatement;
    }

    /**
     * Gets the credential for discovery boot strap resource offering
     * @return Advice Credential advice
     */
    public Advice getCredentials() throws SAML2Exception {
        Advice advice = null;

        if ((assertions != null) && (assertions.size() != 0)) {
            List assertionStrs = new ArrayList();
            for (Iterator iter = assertions.iterator(); iter.hasNext();) {
                Assertion assertion = (Assertion)iter.next();
                assertionStrs.add(assertion.toString(true, true));
            }
            advice = AssertionFactory.getInstance().createAdvice();
            advice.setAdditionalInfo(assertionStrs);
        }

        return advice;
    }

    private static NameIdentifier convertSPNameID(NameID nameId)
        throws SAMLException {

        return new NameIdentifier(nameId.getValue(),
            nameId.getSPNameQualifier(),  nameId.getFormat());
    }

    private static IDPProvidedNameIdentifier convertIDPNameID(NameID nameId)
        throws SAMLException {

        return new IDPProvidedNameIdentifier(nameId.getValue(),
            nameId.getNameQualifier(),  nameId.getFormat());
    }

    private static com.sun.identity.saml.assertion.SubjectConfirmation
        convertSC(List subjectConfirmations) throws SAMLException {
        if ((subjectConfirmations == null) || subjectConfirmations.isEmpty()) {
            return null;
        }

        SubjectConfirmation subjectConfirmation =
            (SubjectConfirmation)subjectConfirmations.get(0);

        com.sun.identity.saml.assertion.SubjectConfirmation samlSC =
            new com.sun.identity.saml.assertion.SubjectConfirmation(
            subjectConfirmation.getMethod());

        SubjectConfirmationData scData =
            subjectConfirmation.getSubjectConfirmationData();
        if (scData != null) {
            List content = scData.getContent();
            if ((content != null) && (!content.isEmpty())) {
                samlSC.setSubjectConfirmationData((String)content.get(0));
            }
        }

        return samlSC;
    }
}
