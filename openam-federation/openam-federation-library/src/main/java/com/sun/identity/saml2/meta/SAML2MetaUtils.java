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
 * $Id: SAML2MetaUtils.java,v 1.9 2009/09/21 17:28:12 exu Exp $
 *
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */
package com.sun.identity.saml2.meta;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.entityconfig.AttributeType;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.metadata.*;
import com.sun.identity.saml2.jaxb.metadataextquery.AttributeQueryDescriptorElement;
import java.util.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;
// import com.sun.identity.saml2.jaxb.metadataattr.ObjectFactory;

/**
 * The <code>SAML2MetaUtils</code> provides metadata related util methods.
 */
public final class SAML2MetaUtils {
    protected static final String RESOURCE_BUNDLE_NAME = "libSAML2Meta";
    protected static ResourceBundle resourceBundle =
                         Locale.getInstallResourceBundle(RESOURCE_BUNDLE_NAME);
    public static Debug debug = Debug.getInstance("libSAML2");
    private static final String JAXB_PACKAGES =
        "com.sun.identity.saml2.jaxb.xmlenc:" +
        "com.sun.identity.saml2.jaxb.xmlsig:" +
        "com.sun.identity.saml2.jaxb.assertion:" +
        "com.sun.identity.saml2.jaxb.metadata:" +
	"com.sun.identity.saml2.jaxb.metadataattr:" +
        "com.sun.identity.saml2.jaxb.entityconfig:" +
        "com.sun.identity.saml2.jaxb.schema";
    private static final String JAXB_PACKAGE_LIST_PROP =
        "com.sun.identity.liberty.ws.jaxb.packageList";
    private static JAXBContext jaxbContext = null;
    private static final String PROP_JAXB_FORMATTED_OUTPUT =
                                        "jaxb.formatted.output";
    private static final String PROP_NAMESPACE_PREFIX_MAPPER =
                                    "com.sun.xml.bind.namespacePrefixMapper";

    private static NamespacePrefixMapperImpl nsPrefixMapper =
                                            new NamespacePrefixMapperImpl();
    static String jaxbPackages = null;

    static {
        try {
            String tmpJaxbPkgs = SystemPropertiesManager.get(
                JAXB_PACKAGE_LIST_PROP);
            if (tmpJaxbPkgs != null && tmpJaxbPkgs.length() > 0) {
                jaxbPackages = JAXB_PACKAGES + ":" + tmpJaxbPkgs;
            } else {
                jaxbPackages = JAXB_PACKAGES;
            }
            if (debug.messageEnabled()) {
                debug.message("SAML2MetaUtils.static: " +
                    "jaxbPackages = " + jaxbPackages);
            }
            jaxbContext = JAXBContext.newInstance(jaxbPackages);
        } catch (JAXBException jaxbe) {
            debug.error("SAML2MetaUtils.static:", jaxbe);
        }
    }

    private SAML2MetaUtils() {
    }

    /**
     * Returns <code>JAXB</code> context for the metadata service.
     * @return <code>JAXB</code> context object.
     */
    public static JAXBContext getMetaJAXBContext() {

        return jaxbContext;
    }

    /**
     * Converts a <code>String</code> object to a JAXB object.
     * @param str a <code>String</code> object
     * @return a JAXB object converted from the <code>String</code> object.
     * @exception JAXBException if an error occurs while converting
     *                          <code>String</code> object
     */
    public static Object convertStringToJAXB(String str)
        throws JAXBException {

       Unmarshaller u = jaxbContext.createUnmarshaller();
       return u.unmarshal(XMLUtils.createSAXSource(new InputSource(new StringReader(str))));
    }

    /**
     * Reads from the <code>InputStream</code> and converts to a JAXB object.
     * @param is a <code>InputStream</code> object
     * @return a JAXB object converted from the <code>InputStream</code> object.
     * @exception JAXBException if an error occurs while converting
     *                          <code>InputStream</code> object
     */
    public static Object convertInputStreamToJAXB(InputStream is)
        throws JAXBException {

       Unmarshaller u = jaxbContext.createUnmarshaller();
       return u.unmarshal(XMLUtils.createSAXSource(new InputSource(is)));
    }

    /**
     * Converts a <code>Node</code> object to a JAXB object.
     * @param node a <code>Node</code> object
     * @return a JAXB object converted from the <code>Node</code> object.
     * @exception JAXBException if an error occurs while converting
     *                          <code>Node</code> object
     */
    public static Object convertNodeToJAXB(Node node)
        throws JAXBException {

       Unmarshaller u = jaxbContext.createUnmarshaller();
       //no need to get SAXSource, since the node is already created by using
       //a secure XML parser
       return u.unmarshal(node);
    }

    /**
     * Converts a JAXB object to a <code>String</code> object.
     * @param jaxbObj a JAXB object
     * @return a <code>String</code> representing the JAXB object.
     * @exception JAXBException if an error occurs while converting JAXB object
     */
    public static String convertJAXBToString(Object jaxbObj)
        throws JAXBException {

        StringWriter sw = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(PROP_JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(PROP_NAMESPACE_PREFIX_MAPPER, nsPrefixMapper);
        marshaller.marshal(jaxbObj, sw);
        return sw.toString();
    }

    /**
     * Converts a JAXB object and writes to an <code>OutputStream</code> object.
     * @param jaxbObj a JAXB object
     * @param os an <code>OutputStream</code> object
     * @exception JAXBException if an error occurs while converting JAXB object
     */
    public static void convertJAXBToOutputStream(Object jaxbObj,
                                                 OutputStream os)
        throws JAXBException {

        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(PROP_JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(PROP_NAMESPACE_PREFIX_MAPPER, nsPrefixMapper);
        marshaller.marshal(jaxbObj, os);
    }

    /**
     * Converts a JAXB object to a <code>String</code> object and creates a
     * <code>Map</code>. The key is 'attrName' and the value is a
     * <code>Set</code> contains the <code>String</code> object.
     * @param attrName attribute name
     * @param jaxbObj a JAXB object
     * @return a <code>Map</code>. The key is 'attrName' and the value is a
     *         <code>Set</code> contains the <code>String</code> object
     *         converted from the JAXB object.
     * @exception JAXBException if an error occurs while converting JAXB object
     */
    protected static Map convertJAXBToAttrMap(String attrName, Object jaxbObj)
        throws JAXBException {

        String xmlString = convertJAXBToString(jaxbObj);
        Map attrs = new HashMap();
        Set values = new HashSet();
        values.add(xmlString);
        attrs.put(attrName, values);

        return attrs;
    }

    /**
     * Gets attribute value pairs from <code>BaseConfigType</code> and
     * put in a <code>Map</code>. The key is attribute name and the value is
     * a <code>List</code> of attribute values;
     * @param config the <code>BaseConfigType</code> object
     * @return a attrbute value <code>Map</code>
     */
    public static Map<String, List<String>> getAttributes(BaseConfigType config) {
        Map<String, List<String>> attrMap = new HashMap<>();
        List<AttributeType> list = config.getAttribute();
        for (AttributeType avp : list) {
            attrMap.put(avp.getName(), avp.getValue());
        }

        return attrMap;
    }

    /**
     * Returns the realm by parsing the metaAlias. MetaAlias format is
     * <pre>
     * &lt;realm>/&lt;any string without '/'> for non-root realm or
     * /&lt;any string without '/'> for root realm.
     * </pre>
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
     * Returns metaAlias embedded in uri.
     * @param uri The uri string.
     * @return the metaAlias embedded in uri or null if not found.
     */
    public static String getMetaAliasByUri(String uri) {
        if (uri == null) {
            return null;
        }

        final int index = uri.indexOf(SAML2MetaManager.NAME_META_ALIAS_IN_URI);
        final int marker = index + SAML2MetaManager.NAME_META_ALIAS_IN_URI.length();
        if (index == -1 || marker == uri.length()) {
            return null;
        }

        return uri.substring(marker);
    }

    /**
     * Returns first policy decision point descriptor in an entity descriptor.
     *
     * @param eDescriptor The entity descriptor.
     * @return policy decision point descriptor or null if it is not found.
     */
    public static XACMLPDPDescriptorElement getPolicyDecisionPointDescriptor(
        EntityDescriptorElement eDescriptor)
    {
        XACMLPDPDescriptorElement descriptor = null;

        if (eDescriptor != null) {
            List list =
            eDescriptor.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();

            for (Iterator i = list.iterator();
                i.hasNext() && (descriptor == null);
            ) {
                Object obj = i.next();
                if (obj instanceof XACMLPDPDescriptorElement) {
                    descriptor = (XACMLPDPDescriptorElement)obj;
                }
            }
        }

        return descriptor;
    }


    /**
     * Returns first policy enforcement point descriptor in an entity
     * descriptor.
     *
     * @param eDescriptor The entity descriptor.
     * @return policy enforcement point descriptor or null if it is not found.
     */
    public static XACMLAuthzDecisionQueryDescriptorElement
        getPolicyEnforcementPointDescriptor(
        EntityDescriptorElement eDescriptor)
    {
        XACMLAuthzDecisionQueryDescriptorElement descriptor = null;

        if (eDescriptor != null) {
            List list =
            eDescriptor.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();

            for (Iterator i = list.iterator();
                i.hasNext() && (descriptor == null);
            ) {
                Object obj = i.next();
                if (obj instanceof XACMLAuthzDecisionQueryDescriptorElement) {
                    descriptor = (XACMLAuthzDecisionQueryDescriptorElement)obj;
                }
            }
        }

        return descriptor;
    }

    /**
     * Returns first service provider's SSO descriptor in an entity
     * descriptor.
     * @param eDescriptor The entity descriptor.
     * @return <code>SPSSODescriptorElement</code> for the entity or null if
     *         not found.
     */
    public static SPSSODescriptorElement getSPSSODescriptor(
        EntityDescriptorElement eDescriptor)
    {
        if (eDescriptor == null) {
            return null;
        }

        List list =
            eDescriptor.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            // TODO: may need to cache to avoid using instanceof
            if (obj instanceof SPSSODescriptorElement) {
                return (SPSSODescriptorElement)obj;
            }
        }

        return null;
    }

    /**
     * Returns first identity provider's SSO descriptor in an entity
     * descriptor.
     * @param eDescriptor The entity descriptor.
     * @return <code>IDPSSODescriptorElement</code> for the entity or null if
     *         not found.
     */
    public static IDPSSODescriptorElement getIDPSSODescriptor(
        EntityDescriptorElement eDescriptor)
    {
        if (eDescriptor == null) {
            return null;
        }

        List list =
            eDescriptor.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof IDPSSODescriptorElement) {
                return (IDPSSODescriptorElement)obj;
            }
        }

        return null;
    }

    /**
     * Returns attribute authority descriptor in an entity descriptor.
     *
     * @param eDescriptor The entity descriptor.
     * @return an <code>AttributeAuthorityDescriptorElement</code> object for
     *     the entity or null if not found.
     */
    public static AttributeAuthorityDescriptorElement
        getAttributeAuthorityDescriptor(EntityDescriptorElement eDescriptor)
    {
        if (eDescriptor == null) {
            return null;
        }

        List list =
            eDescriptor.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();

        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof AttributeAuthorityDescriptorElement) {
                return (AttributeAuthorityDescriptorElement)obj;
            }
        }

        return null;
    }

    /**
     * Returns attribute query descriptor in an entity descriptor.
     *
     * @param eDescriptor The entity descriptor.
     * @return an <code>AttributeQueryDescriptorElement</code> object for
     *     the entity or null if not found.
     */
    public static AttributeQueryDescriptorElement
        getAttributeQueryDescriptor(EntityDescriptorElement eDescriptor)
    {
        if (eDescriptor == null) {
            return null;
        }

        List list =
            eDescriptor.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();

        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof AttributeQueryDescriptorElement) {
                return (AttributeQueryDescriptorElement)obj;
            }
        }

        return null;
    }

    /**
     * Returns authentication authority descriptor in an entity descriptor.
     *
     * @param eDescriptor The entity descriptor.
     * @return an <code>AuthnAuthorityDescriptorElement</code> object for
     *     the entity or null if not found.
     */
    public static AuthnAuthorityDescriptorElement
        getAuthnAuthorityDescriptor(EntityDescriptorElement eDescriptor)
    {
        if (eDescriptor == null) {
            return null;
        }

        List list =
            eDescriptor.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();

        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof AuthnAuthorityDescriptorElement) {
                return (AuthnAuthorityDescriptorElement)obj;
            }
        }

        return null;
    }

    /**
     * Get the first value of set by given key searching in the given map.
     * return null if <code>attrMap</code> is null or <code>key</code>
     * is null.
     *
     * @param attrMap Map of which set is to be added.
     * @param key Key of the entry to be added.
     * @return the first value of a matching set by the given key.
     */
    public static String getFirstEntry(Map attrMap, String key) {
        String retValue = null;

        if ((attrMap != null) && !attrMap.isEmpty()) {
            Set valueSet = (Set)attrMap.get(key);

            if ((valueSet != null) && !valueSet.isEmpty()) {
                retValue = (String)valueSet.iterator().next();
            }
        }

        return retValue;
    }

     /**
     * Adds a set of a given value to a map. Set will not be added if
     * <code>attrMap</code> is null or <code>value</code> is null or
     * <code>key</code> is null.
     *
     * @param attrMap Map of which set is to be added.
     * @param key Key of the entry to be added.
     * @param value Value to be added to the Set.
     */
    public static void fillEntriesInSet(Map attrMap, String key, String value) {
        if ((key != null) && (value != null) && (attrMap != null)) {
            Set valueSet = new HashSet();
            valueSet.add(value);
            attrMap.put(key, valueSet);
        }
    }

    /**
     * Returns first service provider's SSO configuration in an entity.
     * @param eConfig <code>EntityConfigElement</code> of the entity to
     * be retrieved.
     * @return <code>SPSSOConfigElement</code> for the entity or null if not
     *         found.
     * @throws SAML2MetaException if unable to retrieve the first service
     *                            provider's SSO configuration.
     */
    public static SPSSOConfigElement getSPSSOConfig(EntityConfigElement eConfig)
        throws SAML2MetaException {

        if (eConfig == null) {
            return null;
        }

        List list =
            eConfig.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof SPSSOConfigElement) {
                return (SPSSOConfigElement)obj;
            }
        }

        return null;
    }

    /**
     * Returns first identity provider's SSO configuration in an entity
     * @param eConfig <code>EntityConfigElement</code> of the entity to
     * be retrieved.
     * @return <code>IDPSSOConfigElement</code> for the entity or null if not
     *         found.
     * @throws SAML2MetaException if unable to retrieve the first identity
     *                            provider's SSO configuration.
     */
    public static IDPSSOConfigElement getIDPSSOConfig(
        EntityConfigElement eConfig) throws SAML2MetaException {
        if (eConfig == null) {
            return null;
        }

        List list =
            eConfig.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof IDPSSOConfigElement) {
                return (IDPSSOConfigElement)obj;
            }
        }

        return null;
    }

    public static String exportStandardMeta(String realm, String entityID,
	boolean sign)
	throws SAML2MetaException {

	try {
	    SAML2MetaManager metaManager = new SAML2MetaManager();
	    EntityDescriptorElement descriptor =
		metaManager.getEntityDescriptor(realm, entityID);

	    String xmlstr = null;
	    if (descriptor == null) {
		return null;
	    }

	    if (sign) {
		Document doc = SAML2MetaSecurityUtils.sign(realm, descriptor);
		if (doc != null) {
                    xmlstr = XMLUtils.print(doc);
		}
            }
            if (xmlstr == null) {
		xmlstr = convertJAXBToString(descriptor);
		xmlstr = SAML2MetaSecurityUtils.formatBase64BinaryElement(
                    xmlstr);
            }
            xmlstr = workaroundAbstractRoleDescriptor(xmlstr);
            return xmlstr;
	} catch (JAXBException e) {
            throw new SAML2MetaException(e.getMessage());
	}
    }

    /**
     *
     * @param metadata A string representing an EntityDescriptorElement XML document
     * @return EntityDescriptorElement an EntityDescriptorElement from the passed metadata
     * @throws SAML2MetaException If there was a problem with the parsed metadata
     * @throws JAXBException If there was a problem parsing the metadata
     */
    public static EntityDescriptorElement getEntityDescriptorElement(String metadata)
        throws SAML2MetaException, JAXBException {

        Document doc = XMLUtils.toDOMDocument(metadata, debug);

        if (doc == null) {
            throw new SAML2MetaException("Null document");
        }

        Element docElem = doc.getDocumentElement();

        if ((!SAML2MetaConstants.ENTITY_DESCRIPTOR.equals(docElem.getLocalName())) ||
            (!SAML2MetaConstants.NS_METADATA.equals(docElem.getNamespaceURI()))) {
            throw new SAML2MetaException("Invalid  descriptor");
        }

        Object element = preProcessSAML2Document(doc);

        return (element instanceof EntityDescriptorElement) ?
            (EntityDescriptorElement)element : null;
    }

    /**
     * For the given XML metadata document representing either a SAML2 EntityDescriptorElement or EntitiesDescriptorElement,
     * return a list of entityId's for all the Entities created. Carries out a signature validation of the document as
     * part of the import process.
     * @param metaManager An instance of the SAML2MetaManager, used to do the actual create.
     * @param realm The realm to create the Entities in
     * @param doc The XML document that represents either an EntityDescriptorElement or EntitiesDescriptorElement
     * @return A list of all entityId's imported or an empty list if no Entities were imported.
     * @throws SAML2MetaException for any issues as a result of trying to create the Entities.
     * @throws JAXBException for any issues converting the document into a JAXB document.
     */
    public static List<String> importSAML2Document(SAML2MetaManager metaManager,
            String realm, Document doc) throws SAML2MetaException, JAXBException {

        List<String> result = new ArrayList<String>(1);

        Object element = preProcessSAML2Document(doc);

        if (element instanceof EntityDescriptorElement) {
            String entityId = importSAML2Entity(metaManager, realm,
                    (EntityDescriptorElement)element);
            if (entityId != null) {
                result.add(entityId);
            }
        } else if (element instanceof EntitiesDescriptorElement) {
            result = importSAML2Entites(metaManager, realm,
                    (EntitiesDescriptorElement)element);
        }

        if (debug.messageEnabled()) {
            debug.message("SAML2MetaUtils.importSAML2Document: " +
                "Created " + result + " entities");
        }

        return result;
    }

    private static Object preProcessSAML2Document(Document doc) throws SAML2MetaException, JAXBException {

        SAML2MetaSecurityUtils.verifySignature(doc);
        workaroundAbstractRoleDescriptor(doc);

        Object obj = convertNodeToJAXB(doc);

        // Remove any Extensions elements as these are currently not supported.
        obj = workaroundJAXBBug(obj);

        return obj;
    }

    private static List<String> importSAML2Entites(SAML2MetaManager metaManager, String realm,
            EntitiesDescriptorElement descriptor) throws SAML2MetaException {

        List<String> result = new ArrayList<String>();

        List descriptors = descriptor.getEntityDescriptorOrEntitiesDescriptor();
        if (descriptors != null && !descriptors.isEmpty()) {
            Iterator entities = descriptors.iterator();
            while (entities.hasNext()) {
                Object o = entities.next();
                if (o instanceof EntityDescriptorElement) {
                    String entityId = importSAML2Entity(metaManager, realm,
                            (EntityDescriptorElement) o);
                    if (entityId != null) {
                        result.add(entityId);
                    }
                }
            }
        }

        return result;
    }

    private static String importSAML2Entity(SAML2MetaManager metaManager, String realm,
            EntityDescriptorElement descriptor) throws SAML2MetaException {

        String result = null;

        List roles = descriptor.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();
        Iterator it = roles.iterator();
        while (it.hasNext()) {
            RoleDescriptorType role = (RoleDescriptorType)it.next();
            List protocols = role.getProtocolSupportEnumeration();
            if (!protocols.contains(SAML2Constants.PROTOCOL_NAMESPACE)) {
                if (debug.messageEnabled()) {
                    debug.message("SAML2MetaUtils.importSAML2Entity: "
                        + "Removing non-SAML2 role from entity "
                        + descriptor.getEntityID());
                }
                it.remove();
            }
        }

        if (roles.size() > 0) {
            metaManager.createEntityDescriptor(realm, descriptor);
            result = descriptor.getEntityID();
        }

        return result;
    }
    
    private static Object workaroundJAXBBug(Object obj) throws JAXBException {

        String metadata = convertJAXBToString(obj);
        String replaced = metadata.replaceAll("<(.*:)?Extensions/>", "");
        if (metadata.equalsIgnoreCase(replaced)) {
            return obj;
        } else {
            return convertStringToJAXB(replaced);
        }
    }

   private static void workaroundAbstractRoleDescriptor(Document doc) {

        NodeList nl = doc.getDocumentElement().getElementsByTagNameNS(
            SAML2MetaConstants.NS_METADATA,SAML2MetaConstants.ROLE_DESCRIPTOR);
        int length = nl.getLength();
        if (length == 0) {
            return;
        }

        for (int i = 0; i < length; i++) {
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

    private static String workaroundAbstractRoleDescriptor(String xmlstr) {
	int index =
	    xmlstr.indexOf(":" +SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR);
	if (index == -1) {
            return xmlstr;
	}

        int index2 = xmlstr.lastIndexOf("<", index);
	if (index2 == -1) {
            return xmlstr;
	}

        String prefix = xmlstr.substring(index2 + 1, index);
	String type =  prefix + ":" +
            SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR_TYPE;

	xmlstr = xmlstr.replaceAll("<" + prefix + ":" +
            SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR,
            "<" + SAML2MetaConstants.ROLE_DESCRIPTOR + " " +
            SAML2Constants.XSI_DECLARE_STR + " xsi:type=\"" + type + "\"");
	xmlstr = xmlstr.replaceAll("</" + prefix + ":" +
           SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR,
            "</" + SAML2MetaConstants.ROLE_DESCRIPTOR);
	return xmlstr;
    }
}
