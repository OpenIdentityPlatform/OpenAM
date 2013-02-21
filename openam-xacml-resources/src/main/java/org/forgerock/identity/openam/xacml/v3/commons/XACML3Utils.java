/**
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.identity.openam.xacml.v3.commons;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;

import org.forgerock.identity.openam.xacml.v3.model.XACML3Constants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.*;

/**
 * XACML
 * Various Utils to self contain and reduce dependency.
 * Some of these methods have been copied from SAML2 code
 *
 * @author jeff.schenk@forgerock.com
 * @see SAML2Utils
 */
public class XACML3Utils implements XACML3Constants {
    /**
     * Define our Static resource Bundle for our debugger.
     */
    private static Debug debug = Debug.getInstance("amXACML");

    //  XACML Resource bundle
    public static final String BUNDLE_NAME = "amXACML";
    // The resource bundle for XACML 3.0 implementation.
    public static ResourceBundle bundle = Locale.getInstallResourceBundle(BUNDLE_NAME);

    /**
     * Simple Helper Method to read in Resources as a Stream
     *
     * @param resourceName
     * @return String containing the Resource Contents or null if issue.
     */
    public static String getResourceContents(final String resourceName) {
        InputStream inputStream = null;
        try {
            if (resourceName != null) {
                inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
                return (inputStream != null) ? new Scanner(inputStream).useDelimiter("\\A").next() : null;
            }
        } catch (Exception e) {
            // TODO
        }
        // Catch All.
        return null;
    }

    /**
     * Simple Helper Method to read in Resources as a Stream
     *
     * @param resourceName
     * @return InputStream containing the Resource Contents or null if issue.
     */
    public static InputStream getResourceContentStream(final String resourceName) {
        try {
            if (resourceName != null) {
                return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
            }
        } catch (Exception e) {
            // TODO
        }
        // Catch All.
        return null;
    }


    /**
     * Returns metaAlias embedded in uri.
     *
     * @param uri The uri string.
     * @return the metaAlias embedded in uri or null if not found.
     */
    public static String getMetaAliasByUri(String uri) {
        if (uri == null) {
            return null;
        }

        int index = uri.indexOf(NAME_META_ALIAS_IN_URI);
        if (index == -1 || index + 9 == uri.length()) {
            return null;
        }

        return uri.substring(index + 9);
    }

    /**
     * Returns the realm by parsing the metaAlias. MetaAlias format is
     * <pre>
     * &lt;realm>/&lt;any string without '/'> for non-root realm or
     * /&lt;any string without '/'> for root realm.
     * </pre>
     *
     * @param metaAlias The metaAlias.
     * @return the realm associated with the metaAlias.
     */
    public static String getRealmByMetaAlias(String metaAlias) {
        if (metaAlias == null) {
            return null;
        }

        int index = metaAlias.lastIndexOf("/");
        if (index == -1 || index == 0) {
            return "/";
        }

        return metaAlias.substring(0, index);
    }

    /**
     * Returns entity ID associated with the metaAlias.
     *
     * @param metaAlias The metaAlias.
     * @return entity ID associated with the metaAlias or null if not found.
     * @throws SAML2Exception if unable to retrieve the entity ids.
     */
    public static String getEntityByMetaAlias(String metaAlias)
            throws SAML2Exception {
        if (SAML2Utils.getSAML2MetaManager() == null) {
            return null;
        }
        return SAML2Utils.getSAML2MetaManager().getEntityByMetaAlias(metaAlias);
    }

    /**
     * Returns first Element with given local name in samlp name space inside
     * SOAP message.
     *
     * @param messageBody XML Element.
     * @param localName   local name of the Element to be returned.
     * @return first Element matching the local name.
     * @throws com.sun.identity.saml2.common.SAML2Exception
     *          if the Element could not be found or there is
     *          SOAP Fault present.
     */
    public static Element getSamlpElement(
            Element messageBody, String localName) throws SAML2Exception {
        if (messageBody == null) {
            return null;
        }
        NodeList nlBody = messageBody.getChildNodes();
        if (nlBody == null) {
            debug.error("XACML3Utils.getSamlpElement: empty body");
            throw new SAML2Exception(bundle.getString("missingBody"));
        }
        int blength = nlBody.getLength();
        if (blength <= 0) {
            debug.error("XACML3Utils.getSamlpElement: empty body");
            throw new SAML2Exception(bundle.getString("missingBody"));
        }
        Element retElem = null;
        Node node = null;
        for (int i = 0; i < blength; i++) {
            node = (Node) nlBody.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String nlName = node.getLocalName();
            if (nlName == null) {
                nlName = node.getNodeName();
            }
            if (debug.messageEnabled()) {
                debug.message("SAML2Utils.getSamlpElement: node=" +
                        nlName + ", nsURI=" + node.getNamespaceURI());
            }
            if ((nlName != null) && (nlName.equals("Fault"))) {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                        "soapFaultInSOAPResponse"));
            } else if ((nlName != null) && (nlName.equals(localName) &&
                    SAML2Constants.PROTOCOL_NAMESPACE.equals(
                            node.getNamespaceURI()))) {
                retElem = (Element) node;
                break;
            }
        }
        if (retElem == null) {
            throw new SAML2Exception(bundle.getString("elementNotFound") +
                    localName);
        }
        return retElem;
    }


}
