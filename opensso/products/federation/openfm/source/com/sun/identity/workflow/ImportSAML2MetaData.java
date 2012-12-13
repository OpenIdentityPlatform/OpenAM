/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ImportSAML2MetaData.java,v 1.5 2008/07/08 01:12:01 exu Exp $
 *
 */

/**
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.workflow;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.meta.SAML2MetaConstants;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaSecurityUtils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Import SAML2 Metadata.
 */
public class ImportSAML2MetaData {

    private ImportSAML2MetaData() {
    }

    /**
     * Imports meta and extended metadata.
     *
     * @param realm Realm of the entity.
     * @param metadata Meta data.
     * @param extended extended data.
     * @return realm and entity ID.
     */
    public static String[] importData(
        String realm,
        String metadata,
        String extended
    ) throws  WorkflowException {
        String entityID = null;
        
        try {
            SAML2MetaManager metaManager = new SAML2MetaManager();
            EntityConfigElement configElt = null;

            if (extended != null) {
                Object obj = SAML2MetaUtils.convertStringToJAXB(extended);
                configElt = (obj instanceof EntityConfigElement) ?
                    (EntityConfigElement)obj : null;
                if (configElt != null && configElt.isHosted()) {
                    List config =
                    configElt.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
                    if (!config.isEmpty()) {
                        BaseConfigType bConfig = (BaseConfigType)
                            config.iterator().next();
                        realm = SAML2MetaUtils.getRealmByMetaAlias(
                            bConfig.getMetaAlias());
                    }
                }
            }

            EntityDescriptorElement descriptor = null;
            if (metadata != null) {
                descriptor = getEntityDescriptorElement(metadata); 
                if (descriptor != null) {
                    entityID = descriptor.getEntityID();
                }
            } 
            metaManager.createEntity(realm, descriptor, configElt);
        } catch (SAML2MetaException e) {
            throw new WorkflowException(e.getMessage());
        } catch (JAXBException e) {
            throw new WorkflowException(e.getMessage());
        }

        String[] results = {realm, entityID};
        return results;
    }

    static EntityDescriptorElement getEntityDescriptorElement(String metadata)
        throws SAML2MetaException, JAXBException, WorkflowException {
        Debug debug = Debug.getInstance("workflow");
        Document doc = XMLUtils.toDOMDocument(metadata, debug);

        if (doc == null) {
            throw new WorkflowException(
                "import-entity-exception-invalid-descriptor", null);
        }

        Element docElem = doc.getDocumentElement();
        
        if ((!SAML2MetaConstants.ENTITY_DESCRIPTOR.equals(
            docElem.getLocalName())) ||
            (!SAML2MetaConstants.NS_METADATA.equals(
                docElem.getNamespaceURI()))
        ) {
            throw new WorkflowException(
                "import-entity-exception-invalid-descriptor", null);
        }
        SAML2MetaSecurityUtils.verifySignature(doc);
        workaroundAbstractRoleDescriptor(doc);
        Object obj = SAML2MetaUtils.convertNodeToJAXB(doc);
        obj = workaroundJAXBBug(obj);

        return (obj instanceof EntityDescriptorElement) ?
            (EntityDescriptorElement)obj : null;
    }
    
    private static void workaroundAbstractRoleDescriptor(Document doc) {
        Debug debug = Debug.getInstance("workflow");
        NodeList nl = doc.getDocumentElement().getElementsByTagNameNS(
            SAML2MetaConstants.NS_METADATA,SAML2MetaConstants.ROLE_DESCRIPTOR);
        int length = nl.getLength();
        if (length == 0) {
            return;
        }

        for(int i = 0; i < length; i++) {
            Element child = (Element)nl.item(i);
            String type = child.getAttributeNS(SAML2Constants.NS_XSI, "type");
            if (type != null) {
                if ((type.equals(
                    SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR_TYPE)) ||
                    (type.endsWith(":" +
                    SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR_TYPE))) {

                    String newTag = type.substring(0, type.length() - 4);

                    String xmlstr = XMLUtils.print(child);
                    int index = xmlstr.indexOf(
                        SAML2MetaConstants.ROLE_DESCRIPTOR);
                    xmlstr = "<" + newTag + xmlstr.substring(index +
                        SAML2MetaConstants.ROLE_DESCRIPTOR.length());
                    if (!xmlstr.endsWith("/>")) {
                        index = xmlstr.lastIndexOf("</");
                        xmlstr = xmlstr.substring(0, index) + "</" + newTag +
                            ">";
                    }

                    Document tmpDoc = XMLUtils.toDOMDocument(xmlstr, debug);
                    Node newChild =
                        doc.importNode(tmpDoc.getDocumentElement(), true);
                    child.getParentNode().replaceChild(newChild, child);
                }
            }
        }
    }

    private static Object workaroundJAXBBug(Object obj) throws JAXBException {
        String metadata = SAML2MetaUtils.convertJAXBToString(obj);
        String replaced = metadata.replaceAll("<(.*:)?Extensions/>", "");
        if (metadata.equalsIgnoreCase(replaced)) {
            return obj;
        } else {
            return SAML2MetaUtils.convertStringToJAXB(replaced);
        }
    }

}

