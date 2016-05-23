/**
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
 * $Id: WSFederationMetaUtils.java,v 1.5 2009/10/28 23:58:59 exu Exp $
 *
 * Portions Copyrighted 2012-2016 ForgeRock AS.
 */
package com.sun.identity.wsfederation.meta;

import com.sun.identity.wsfederation.jaxb.entityconfig.AttributeType;
import com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType;
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

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.forgerock.openam.utils.StringUtils;
import org.w3c.dom.Node;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.jaxb.entityconfig.AttributeElement;
import com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement;

import org.xml.sax.InputSource;

/**
 * The <code>WSFederationMetaUtils</code> provides metadata related utility
 * methods.
 */
public final class WSFederationMetaUtils {
    public static Debug debug = 
	Debug.getInstance(WSFederationConstants.AM_WSFEDERATION);

    /**
     * Resource bundle for the WS-Federation implementation.
     */ 
    public static ResourceBundle bundle = Locale.
	getInstallResourceBundle(WSFederationConstants.BUNDLE_NAME);

    
    // Need to explicitly list xmldsig, otherwise JAXB doesn't see it, since
    // dsig elements are buried in 'any' elements. Grrr...
    private static final String JAXB_PACKAGES =
        "com.sun.identity.wsfederation.jaxb.xmlsig:" +
        "com.sun.identity.wsfederation.jaxb.wsu:" +
        "com.sun.identity.wsfederation.jaxb.wsse:" +
        "com.sun.identity.wsfederation.jaxb.wsaddr:" +
        "com.sun.identity.wsfederation.jaxb.wspolicy:" +
        "com.sun.identity.wsfederation.jaxb.wsspolicy:" +
        "com.sun.identity.wsfederation.jaxb.entityconfig:" +
        "com.sun.identity.wsfederation.jaxb.wsfederation";

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
            debug.error("WSFederationMetaUtils.static:", jaxbe);
            throw new ExceptionInInitializerError(jaxbe);
        }
    }

    /*
     * Private constructor ensure that no instance is ever created
     */
    private WSFederationMetaUtils() {
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
    public static Map<String,List<String>> getAttributes(BaseConfigType config) 
    {
        Map<String,List<String>> attrMap = new HashMap<String,List<String>>();
        List list = config.getAttribute();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            AttributeType avp = (AttributeType)iter.next();
            attrMap.put(avp.getName(), avp.getValue());
        }

        return attrMap;
    }

    /**
     * Returns all attribute values associated with they key provided.
     *
     * @param config The configuration object.
     * @param key The attribute key.
     * @return All attribute values associated with the key provided.
     */
    public static List<String> getAttributes(BaseConfigType config, String key) {
        return getAttributes(config).get(key);
    }

    /**
     * Sets attribute value pairs in <code>BaseConfigType</code>. NOTE - 
     * existing AVPs are discarded! The key is 
     * @param config the <code>BaseConfigType</code> object
     * @param map mapping from attribute names to <code>List</code>s of 
     * attribute values;
     */
    public static void setAttributes(BaseConfigType config, 
        Map<String,List<String>> map) 
        throws JAXBException
    {
        JAXBContext jc = WSFederationMetaUtils.getMetaJAXBContext();
        com.sun.identity.wsfederation.jaxb.entityconfig.ObjectFactory 
            objFactory = 
            new com.sun.identity.wsfederation.jaxb.entityconfig.ObjectFactory();

        List attributeList = config.getAttribute();

        attributeList.clear();

        // add the new content
        for (String key : map.keySet())
        {
            AttributeElement
                avp = objFactory.createAttributeElement();
            avp.setName(key);
            avp.getValue().addAll(map.get(key));
            
            attributeList.add(avp);
        }
    }
    
    /**
     * Gets a single attribute value from <code>BaseConfigType</code>
     * @param config the <code>BaseConfigType</code> object
     * @param key attribute key.
     * @return the attribute value
     */
    public static String getAttribute(BaseConfigType config, String key)
    {
        List list = config.getAttribute();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            AttributeType avp = (AttributeType)iter.next();
            if ( avp.getName().equals(key) ) {
                return (String)avp.getValue().get(0);
            }
        }

        return null;
    }

    /**
     * Gets a single attribute value from <code>BaseConfigType</code>.
     *
     * @param config The <code>BaseConfigType</code> object.
     * @param key Attribute key.
     * @param defaultValue The defaultValue to return if the attribute was not set.
     * @return The attribute value.
     */
    public static String getAttribute(BaseConfigType config, String key, String defaultValue) {
        String value = getAttribute(config, key);
        return StringUtils.isNotEmpty(value) ? value : defaultValue;
    }

    /**
     * Gets the int value stored in the configuration under the provided key, or the default value if it was not defined
     * or if it was malformed.
     *
     * @param config The configuration object to investigate.
     * @param key The configuration key.
     * @param defaultValue The default value to return if the config is missing or malformed.
     * @return The int value associated with the requested configuration key.
     */
    public static int getIntAttribute(BaseConfigType config, String key, int defaultValue) {
        final String attribute = getAttribute(config, key);
        if (attribute != null) {
            try {
                int value = Integer.parseInt(attribute);
                if (debug.messageEnabled()) {
                    debug.message("Retrieved {} attribute from config: {}", key, value);
                }
                return value;
            } catch (NumberFormatException nfe) {
                debug.error("Failed to get {} attribute from IDP SSO config", key, nfe);
            }
        }

        return defaultValue;
    }

    /**
     * Returns the realm by parsing the metaAlias. MetaAlias format is
     * <pre>
     * &lt;realm&gt;/&lt;any string without '/'&gt; for non-root realm or
     * /&lt;any string without '/'&gt; for root realm.
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

        int index = uri.indexOf(WSFederationConstants.NAME_META_ALIAS_IN_URI);
        if (index == -1 || index + 9 == uri.length()) {
            return null;
        }

        return uri.substring(index + 9);
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
     * Returns the endpoint baseURL as stored in the configuration, or if absent, generates the base URL based on the
     * incoming HTTP request.
     *
     * @param idpConfig The configuration object.
     * @param request The HTTP request corresponding to the current WS-Fed action.
     * @return The Base URL of the OpenAM deployment.
     */
    public static String getEndpointBaseUrl(IDPSSOConfigElement idpConfig, HttpServletRequest request) {
        String endpointBaseUrl = getAttribute(idpConfig, WSFederationConstants.ENDPOINT_BASE_URL);
        if (StringUtils.isEmpty(endpointBaseUrl)) {
            endpointBaseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                    + request.getContextPath();
        }
        return endpointBaseUrl;
    }
}
