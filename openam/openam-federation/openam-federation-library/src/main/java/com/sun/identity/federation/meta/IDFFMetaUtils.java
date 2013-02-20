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
 * $Id: IDFFMetaUtils.java,v 1.5 2008/11/10 22:56:57 veiming Exp $
 *
 */

/**
 *
 * @author bina
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.federation.meta;

import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.AttributeType;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement;
import com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.StringReader;
import java.io.StringWriter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * This class contains utility methods to process
 * the IDFF Meta data.
 */
public class IDFFMetaUtils {
    
    /**
     * IDFF Meta Data Configuration Name
     */
    public static final String IDFF_META_SERVICE = "ID-FF_META";
    /**
     * IDFF Meta Debug
     */
    public static Debug debug = Debug.getInstance("libIDFF");
    
    /**
     * Bundle Name
     */
    protected static String IDFF_BUNDLE_NAME = "libIDFFMeta";
    private static final String JAXB_PACKAGES =
            "com.sun.identity.liberty.ws.meta.jaxb:" +
            "com.sun.identity.federation.jaxb.entityconfig:" ;
    
    private static JAXBContext jaxbContext = null;
    private static final String PROP_JAXB_FORMATTED_OUTPUT =
            "jaxb.formatted.output";
    private static final String PROP_NAMESPACE_PREFIX_MAPPER =
            "com.sun.xml.bind.namespacePrefixMapper";
    
    private static NamespacePrefixMapperImpl nsPrefixMapper =
            new NamespacePrefixMapperImpl();
    
    static {
        try {
            jaxbContext = JAXBContext.newInstance(JAXB_PACKAGES);
        } catch (JAXBException jaxbe) {
            debug.error("IDFFMetaUtils.static:", jaxbe);
            jaxbe.printStackTrace();
        }
    }
    
    /**
     * Default Constructor
     */
    protected IDFFMetaUtils() {
    }
    
    /**
     * Converts a JAXB object to a <code>String</code> object.
     * @param jaxbObj a JAXB object
     * @return a <code>String</code> representing the JAXB object.
     * @throws JAXBException if an error occurs while converting JAXB object
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
     * Converts a JAXB object to a <code>String</code> object and creates a
     * <code>Map</code>. The key is 'attrName' and the value is a
     * <code>Set</code> which contains the <code>String</code> object.
     *
     * @param attrName attribute name
     * @param jaxbObj a JAXB object
     * @return a <code>Map</code>. The key is 'attrName' and the value is a
     *         <code>Set</code> contains the <code>String</code> object
     *         converted from the JAXB object.
     * @throws JAXBException if an error occurs while converting JAXB object
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
     * Converts a <code>Node</code> object to a JAXB object.
     *
     * @param node a <code>Node</code> object
     * @return a JAXB object converted from the <code>Node</code> object
     * @throws JAXBException if an error occurs while converting
     *         <code>Node</code> object
     */
    public static Object convertNodeToJAXB(Node node)
    throws JAXBException {
        Unmarshaller u = jaxbContext.createUnmarshaller();
        //no need to get SAXSource, since the node is already created by using
        //a secure XML parser
        return u.unmarshal(node);
    }
    
    /**
     * Converts a <code>String</code> object to a JAXB object.
     *
     * @param str a <code>String</code> object
     * @return a JAXB object converted from the <code>String</code> object.
     * @throws JAXBException if an error occurs while converting
     *         <code>String</code> object
     */
    public static Object convertStringToJAXB(String str)
    throws JAXBException {
        Unmarshaller u = jaxbContext.createUnmarshaller();
        return u.unmarshal(XMLUtils.createSAXSource(new InputSource(new StringReader(str))));
    }
    
    /**
     * Returns the SPDescriptor in the Entity Descriptor.
     * If there is more then one SPDescriptor then the first
     * one in the list is returned.
     *
     * @param entityDescriptor the EntityDescriptor element.
     * @return the <code>SPDescriptorType</code> object.
     */
    public static SPDescriptorType getSPDescriptor(
            EntityDescriptorElement entityDescriptor) {
        SPDescriptorType spDescriptor = null;
        if (entityDescriptor != null) {
            List spList = entityDescriptor.getSPDescriptor();
            if (spList != null && !spList.isEmpty()) {
                Iterator spIterator = spList.iterator();
                while (spIterator.hasNext()) {
                    Object eObj = spIterator.next();
                    if (eObj instanceof SPDescriptorType) {
                        spDescriptor = (SPDescriptorType) eObj;
                        break;
                    }
                }
            }
        }
        return spDescriptor;
    }
    /**
     * Returns the <code>IDPDescriptor</code> in the Entity Descriptor.
     * If there multiple descriptors then the first retreived
     * descriptor is returned.
     *
     * @param entityDescriptor the EntityDescriptor element.
     * @return the <code>IDPDescriptorType</code> object.
     */
    public static IDPDescriptorType getIDPDescriptor(
            EntityDescriptorElement entityDescriptor) {
        IDPDescriptorType idpDescriptor = null;
        if (entityDescriptor != null) {
            List idpList = entityDescriptor.getIDPDescriptor();
            if (idpList != null && !idpList.isEmpty()) {
                Iterator idpIterator = idpList.iterator();
                while (idpIterator.hasNext()) {
                    Object eObj = idpIterator.next();
                    if (eObj instanceof IDPDescriptorType) {
                        idpDescriptor = (IDPDescriptorType) eObj;
                        break;
                    }
                }
            }
        }
        return idpDescriptor;
    }
    
    /**
     * Returns the Service Provider Entity Configuration.
     * If there are multiple Configurations then the first
     * configuration retreived is returned.
     *
     * @param entityConfig the <code>EntityConfigElement</code> object.
     * @return the <code>SPDescriptorEntityConfigElement</code> object.
     */
    public static SPDescriptorConfigElement getSPDescriptorConfig(
            EntityConfigElement entityConfig) {
        SPDescriptorConfigElement spEntityConfig = null;
        if (entityConfig != null) {
            List spCfgList = entityConfig.getSPDescriptorConfig();
            if (spCfgList != null && !spCfgList.isEmpty()) {
                Iterator spCfgIterator = spCfgList.iterator();
                while (spCfgIterator.hasNext()) {
                    Object eObj = spCfgIterator.next();
                    if (eObj instanceof SPDescriptorConfigElement) {
                        spEntityConfig = (SPDescriptorConfigElement) eObj;
                        break;
                    }
                }
            }
        }
        return spEntityConfig;
    }
    
    /**
     * Returns the Identity Provider Entity Configuration.
     * If there are multiple Configuraitons then the
     * first configuration retreived is returned.
     *
     * @param entityConfig the <code>EntityConfigElement</code> object.
     * @return the <code>IDPDescriptorEntityConfigElement</code> object.
     */
    public static IDPDescriptorConfigElement getIDPDescriptorConfig(
            EntityConfigElement entityConfig) {
        IDPDescriptorConfigElement idpEntityConfig = null;
        if (entityConfig != null) {
            List idpCfgList = entityConfig.getIDPDescriptorConfig();
            if (idpCfgList != null && !idpCfgList.isEmpty()) {
                Iterator idpCfgIterator = idpCfgList.iterator();
                while (idpCfgIterator.hasNext()) {
                    Object eObj = idpCfgIterator.next();
                    if (eObj instanceof IDPDescriptorConfigElement) {
                        idpEntityConfig = (IDPDescriptorConfigElement) eObj;
                        break;
                    }
                }
            }
        }
        return idpEntityConfig;
    }

    /**
     * Gets attribute value pairs from <code>BaseConfigType</code> and
     * put in a <code>Map</code>. The key is attribute name and the value is
     * a <code>List</code> of attribute values.
     * @param config the <code>BaseConfigType</code> object
     * @return an attrbute value <code>Map</code>
     */
    public static Map getAttributes(BaseConfigType config) {
        Map attrMap = new HashMap();
        List list = config.getAttribute();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            AttributeType avp = (AttributeType)iter.next();
            attrMap.put(avp.getName(), avp.getValue());
        }

        return attrMap;
    }

    /**
     * Gets the first value of list by given key searching in the given map.
     *
     * @param attrMap Map of which list is to be retrieved.
     * @param key Key of the entry to be retrieved.
     * @return the first value of a matching list by the given key. Returns
     *  <code>null</code> if <code>attrMap</code> is null or <code>key</code>
     *  is <code>null</code>.
     */

    public static String getFirstAttributeValue(
        Map attrMap, String key)
    {
        String retValue = null;

        if ((attrMap != null) && !attrMap.isEmpty()) {
            List valueList = (List)attrMap.get(key);

            if ((valueList != null) && !valueList.isEmpty()) {
                retValue = (String)valueList.iterator().next();
            }
        }

        return retValue;
    }

    public static boolean isAutoFedEnabled(Map attributes) {
        boolean returnVal = false;
        String autoFedEnabledStr = getFirstAttributeValue(
            attributes, IFSConstants.ENABLE_AUTO_FEDERATION);
        if (autoFedEnabledStr != null) {
            returnVal = autoFedEnabledStr.equalsIgnoreCase("true");
        }
        return returnVal;
    }

    public static String getFirstAttributeValueFromIDPConfig(
        IDFFMetaManager metaManager, String realm,
        String idpEntityID, String attrName)
    {
        if (metaManager == null || idpEntityID == null || attrName == null) {
            return null;
        }
        String returnVal = null;
        try {
            IDPDescriptorConfigElement idpConfig =
                metaManager.getIDPDescriptorConfig(realm, idpEntityID);
            if (idpConfig != null) {
                Map attributes = getAttributes(idpConfig);
                returnVal = getFirstAttributeValue(attributes, attrName);
            }
        } catch (IDFFMetaException e) {
            returnVal = null;
        }
        return returnVal;
    }

    public static boolean getBooleanAttributeValueFromConfig(
        BaseConfigType config, String attrName)
    {
        String valStr = getFirstAttributeValueFromConfig(config, attrName);
        if (valStr != null && valStr.equalsIgnoreCase("true")) {
            return true;
        } else {
            return false;
        }
    }

    public static String getFirstAttributeValueFromConfig(
        BaseConfigType config, String attrName)
    {
        if (config == null || attrName == null) {
            return null;
        }
        Map attributes = getAttributes(config);
        return getFirstAttributeValue(attributes, attrName);
    }

    public static List getAttributeValueFromConfig(
        BaseConfigType config, String attrName)
    {
        if (config == null || attrName == null) {
            return null;
        }
        Map attributes = getAttributes(config);
        if (attributes != null) {
            return (List) attributes.get(attrName);
        } else {
            return null;
        }
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

        int index = uri.indexOf(IDFFMetaManager.NAME_META_ALIAS_IN_URI);
        if (index == -1 || index + 9 == uri.length()) {
            return null;
        }

        return uri.substring(index + 9);
    }

    /**
     * Obtains provider's meta alias.
     * @param realm the realm in which the provider resides
     * @param providerID provider's entity ID
     * @param providerRole provider's role
     * @param session user session object
     * @return service provider's meta alias; or <code>null</code> if an error
     *     occurs.
     */
    public static String getMetaAlias(
        String realm, String providerID, String providerRole, Object session)
    {
        try {
            IDFFMetaManager metaManager = new IDFFMetaManager(session);
            if (metaManager != null) {
                BaseConfigType extendedConfig = getExtendedConfig(
                    realm, providerID, providerRole, metaManager);
                if (extendedConfig != null) {
                    return extendedConfig.getMetaAlias();
                }
            }
        } catch (IDFFMetaException e) {
            if (debug.messageEnabled()) {
                debug.message("IDFFMetaUtils.getMetaAlias:", e);
            }
        }
        return null;
    }

    /**
     * Obtains provider's extended meta.
     * @param realm the realm in which the provider resides
     * @param providerId provider's entity ID
     * @param providerRole provider's role
     * @param metaManager <code>IDFFMetaManager</code> instance.
     * @return provider's extended meta; or <code>null</code> if an error
     *     occurs.
     */
    public static BaseConfigType getExtendedConfig(
        String realm, String providerId, String providerRole,
        IDFFMetaManager metaManager)
    {
        BaseConfigType providerConfig = null;
        if (metaManager != null && providerRole != null) {
            try {
                if (providerRole.equalsIgnoreCase(IFSConstants.IDP)) {
                    providerConfig = metaManager.getIDPDescriptorConfig(
                        realm, providerId);
                } else if (providerRole.equalsIgnoreCase(IFSConstants.SP)) {
                    providerConfig = metaManager.getSPDescriptorConfig(
                        realm, providerId);
                }
            } catch (IDFFMetaException ie) {
                debug.error(
                    "IDFFMetaUtils.getExtendedConfig: couldn't get meta:",ie);
            }
        }
        return providerConfig;
    }
}
