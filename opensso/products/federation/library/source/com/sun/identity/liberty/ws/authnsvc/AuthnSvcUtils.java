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
 * $Id: AuthnSvcUtils.java,v 1.5 2008/12/05 00:18:02 exu Exp $
 *
 */


package com.sun.identity.liberty.ws.authnsvc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.encode.Base64;

import com.sun.identity.shared.Constants;
import com.sun.identity.liberty.ws.authnsvc.protocol.SASLResponse;
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
import com.sun.identity.liberty.ws.security.SecurityAssertion;
import com.sun.identity.liberty.ws.soapbinding.Message;

/**
 * The class <code>AuthnSvcUtils</code> provides some utils for Authentication
 * service related stuff.
 */
public class AuthnSvcUtils {

    /**
     * <code>ResourceBundle</code> object for this service.
     */
    public static ResourceBundle bundle =
                        Locale.getInstallResourceBundle("libAuthnSvc");

    /**
     * <code>Debug</code> object for this service.
     */
    public static Debug debug = Debug.getInstance("libIDWSF");

    /**
     * Returns localized string from resource bundle.
     *
     * @param key a key to a resource bundle.
     * @return a localized string
     */
    public static String getString(String key) {
        return bundle.getString(key);
    }

    /**
     * Decodes the value of a Data Element.
     * @param dataE a Data element
     * @return a byte array of decoded value
     */
    public static byte[] decodeDataElement(Element dataE) {
        if (dataE == null) {
            return null;
        }

        String value = XMLUtils.getElementValue(dataE);
        if (value == null) {
            return null;
        }

        return Base64.decode(value);
    }

    /**
     * Sets resource offering and credentials to the SASL response based on
     * provided sso token.
     * @param saslResp a SASL response
     * @param message a SOAP message containing a SASL request
     * @param userDN Distinguished Name of the User.
     * @return <code>true</code> if it sets correctly
     */
    public static boolean setResourceOfferingAndCredentials(
        SASLResponse saslResp, Message message, String userDN)
    {

        try {
            DiscoEntryElement discoEntry = (DiscoEntryElement)
                      DiscoServiceManager.getBootstrappingDiscoEntry();
            ResourceOfferingType offering = discoEntry.getResourceOffering();
            if (!DiscoServiceManager.useImpliedResource()) {
                ServiceInstanceType serviceInstance =
                                                offering.getServiceInstance();
                String providerID = serviceInstance.getProviderID();
                ResourceIDMapper idMapper =
                    DiscoServiceManager.getResourceIDMapper(providerID);
                if (idMapper == null) {
                    idMapper = DiscoServiceManager.getDefaultResourceIDMapper();
                }
                ObjectFactory fac =
                    new com.sun.identity.liberty.ws.disco.jaxb.ObjectFactory();
                ResourceIDType resourceID = fac.createResourceIDType();
                String resourceIDValue = idMapper.getResourceID(providerID,
                                                                userDN);

                if (AuthnSvcUtils.debug.messageEnabled()) {
                    AuthnSvcUtils.debug.message(
                        "AuthnSvcUtils.setResourceOfferingAndCredentials" +
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
            Map map = DiscoUtils.checkPolicyAndHandleDirectives(userDN,
                message, discoEntryList, null, null, null, message.getToken());
            List offerings = (List) map.get(DiscoUtils.OFFERINGS);
            if (offerings.isEmpty()) {
                if (AuthnSvcUtils.debug.messageEnabled()) {
                    AuthnSvcUtils.debug.message(
                        "AuthnSvcUtils.setResourceOfferingAndCredentials" +
                        "no ResourceOffering");
                }
                return false;
            }

            ResourceOffering ro = (ResourceOffering)offerings.get(0);

            saslResp.setResourceOffering(ro);
            List assertions = (List) map.get(DiscoUtils.CREDENTIALS);
            if ((assertions != null) && (!assertions.isEmpty())) {
                Iterator iter = assertions.iterator();
                List credentials = new ArrayList();
                while (iter.hasNext()) {
                    SecurityAssertion assertion =
                                (SecurityAssertion)iter.next();
                    Document doc = XMLUtils.toDOMDocument(
                                         assertion.toString(true, true),
                                          AuthnSvcUtils.debug);
                    credentials.add(doc.getDocumentElement());
                }
                saslResp.setCredentials(credentials);
            }
            return true;
        } catch (Exception ex) {
            debug.error("AuthnSvcUtils.setResourceOfferingAndCredentials:",ex);
            return false;
        }
    }
}
