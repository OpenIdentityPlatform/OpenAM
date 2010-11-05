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
 * $Id: FedletConfigurationImpl.java,v 1.5 2010/01/26 21:31:59 madan_ranganath Exp $
 *
 */

package com.sun.identity.plugin.configuration.impl;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaConstants;
import com.sun.identity.saml2.meta.SAML2MetaSecurityUtils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>FedletConfigurationImpl</code> class is the implementation for 
 * Fedlet to retrieve metadata/COT configuration from flat files. 
 */
public class FedletConfigurationImpl implements ConfigurationInstance {

    // Name of attribute in COT file to contains the COT name
    private static final String COT_NAME = "cot-name";
    // Suffix for extended metadata file name.
    private static final String EXTENDED_XML_SUFFIX = "-extended.xml";
    // Suffix for COT file name.
    private static final String COT_FILE_SUFFIX = ".cot";
    // fedlet home directory which contains metadata/COT/configuration files
    private static String fedletHomeDir; 
    // property name to point to the fedlet home
    // if not defined, default to "$user_home/fedlet"
    private static final String FEDLET_HOME_DIR = 
        "com.sun.identity.fedlet.home"; 
    private String componentName = null;
    private static final String RESOURCE_BUNDLE = "fmConfigurationService";
    static Debug debug = Debug.getInstance("libPlugins");;
    // Map to store COT information
    private static Map cotMap = new HashMap();
    // Map to store metadata information
    private static Map entityMap = new HashMap();

    /**
     * Initializer.
     * @param componentName Name of the components, e.g. SAML1, SAML2, ID-FF
     * @param session FM Session object.
     * @exception ConfigurationException if could not initialize the instance.
     */
    public void init(String componentName, Object session) 
        throws ConfigurationException {

        if (debug.messageEnabled()) {
            debug.message("FedletConfigurationImpl.init: component=" +
                componentName);
        }
        this.componentName = componentName;       
        fedletHomeDir = System.getProperty(FEDLET_HOME_DIR);
        if ((fedletHomeDir == null) || (fedletHomeDir.trim().length() == 0)) {
            fedletHomeDir = System.getProperty("user.home") +
                File.separator + "fedlet";
        }
        if (debug.messageEnabled()) {
            debug.message("FedletConfigurationImpl.init: fedlet home=" + 
                fedletHomeDir);
        }
        // initialize SAML2 metadata and COT from fedlet home directory
        initializeMetadataAndCOT();
        if (debug.messageEnabled()) {
            debug.message("FedletConfImpl entityMap: =" + entityMap.keySet());
            debug.message("FedletConfImpl cotMap: =" + cotMap.keySet());
        } 
    }

    /**
     * Returns Configurations.
     * @param realm the name of organization at which the configuration resides.
     * @param configName configuration instance name. e.g. "/sp".
     *     The configName could be null or empty string, which means the default
     *     configuration for this components. 
     * @return Map of key/value pairs, key is the attribute name, value is
     *     a Set of attribute values or null if service configuration doesn't
     *     doesn't exist.
     * @exception ConfigurationException if an error occurred while getting
     *     service configuration.
     */
    public Map getConfiguration(String realm, String configName)
        throws ConfigurationException {

        if (debug.messageEnabled()) {
            debug.message("FedletConfigurationImpl.getConfiguration: " +
                "componentName = " + componentName + ", realm = " + realm +
                ", configName = " + configName);
        }

        // only need to support SAML2/LIBCOT for now
        if ("SAML2".equals(componentName)) {
            return (Map) entityMap.get(configName);
        } else if ("LIBCOT".equals(componentName)) {
            return (Map) cotMap.get(configName);
        } else {
            return null;
        }
    }

    /**
     * Initializes SAMLv2 metadata and COT from flat files under Fedlet
     * home directory.
     * The metadata information will be stored in a Map, key is the entity ID,
     * value is a Map whose key is the standard/extended attribute name, 
     * value is a String containing the standard/extended metadata XML.
     * Standard metadata is stored in a file named <fileName>.xml
     * Extended metadata is stored in a file named <fileName>-extended.xml
     * 
     * The COT information will be stored in a Map, key is the COT name, 
     * value is a Map whose key is the attribute  name, value is a Set of 
     * values for the attribute.
     * COT is stored in a file named <filename>.cot
     *
     */
    private void initializeMetadataAndCOT() {
        try {
            // read all SAML2 metadata/COT files from fedlet home directory
            File homeDir = new File(fedletHomeDir); 
            String[] files = homeDir.list();
            if ((files == null) || (files.length == 0)) {
                return;
            } 
            for (int i = 0; i < files.length; i++) {
                String fileName = files[i];  
                if (debug.messageEnabled()) {
                    debug.message("FedletConfigImpl.initMetaCOT: " + fileName);
                }
                if (fileName.endsWith(EXTENDED_XML_SUFFIX)) {
                    // processing metadata entry
                    handleSAML2Metadata(fileName.substring(0, 
                        fileName.length() - EXTENDED_XML_SUFFIX.length()));
                } else if (fileName.endsWith(COT_FILE_SUFFIX)) {
                    handleCOT(fileName.substring(0,
                        fileName.length() - COT_FILE_SUFFIX.length()));
                } else {
                    continue;
                }
            }
        } catch (NullPointerException npe) {
            debug.error("FedletConfigurationImpl.processSAML2Metadata()", npe);
        } catch (SecurityException se) {
            debug.error("FedletConfigurationImpl.processSAML2Metadata()", se);
        }
    }

    /**
     * Gets SAML2 metadata from flat files and stores in entityMap.
     */
    private void handleSAML2Metadata(String fileName) {
        // get standard metadata
        String metaFile = fedletHomeDir + File.separator + fileName + ".xml";
        if (debug.messageEnabled()) {
            debug.message("FedletConfigurationImpl.handleSAML2Metadata: " +
                "metaFile=" + metaFile);
        }
        String metaXML = openFile(metaFile);
        if (metaXML == null) {
            return;
        }

        metaXML = workaroundAbstractRoleDescriptor(metaXML);
        String entityId = getEntityID(metaXML);
        if (entityId == null) {
            return;
        }
        Map map = new HashMap();
        Set set = new HashSet();
        set.add(metaXML);
        map.put("sun-fm-saml2-metadata", set);
        // get extended metadata files
        String extFile = fedletHomeDir + File.separator + fileName
            + EXTENDED_XML_SUFFIX; 
        String extXML = openFile(extFile); 
        if (extXML == null) {
            return;
        }
        set = new HashSet();
        set.add(extXML);
        map.put("sun-fm-saml2-entityconfig", set);
        // add to entity Map
        entityMap.put(entityId, map);
        if (debug.messageEnabled()) {
            debug.message("FedletConfigurationImpl.handleSAML2Metadata: " +
                "done processing entity " + entityId);
        }
    }

    private String workaroundAbstractRoleDescriptor(String metaXML) {
        Document doc = XMLUtils.toDOMDocument(metaXML, debug);
        if (doc != null) {
            NodeList nl = doc.getDocumentElement().getElementsByTagNameNS(
                                         SAML2MetaConstants.NS_METADATA,
                                         SAML2MetaConstants.ROLE_DESCRIPTOR);
            int length = nl.getLength();
            for (int i = 0; i < length; i++) {
                Element child = (Element)nl.item(i);
                String type = child.getAttributeNS(SAML2Constants.NS_XSI,
                                                   "type");
                if ((type != null) && (type.equals(
                    SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR_TYPE)) ||
                    (type.endsWith(":" +
                    SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR_TYPE))) {
                     metaXML = metaXML.replaceAll(
                                              SAML2Constants.XSI_DECLARE_STR,
                                              "");
                    metaXML = metaXML.replaceAll(
                           "xsi:type=\"query:AttributeQueryDescriptorType\"",
                           "");
                    metaXML = metaXML.replaceAll("<" +
                               SAML2MetaConstants.ROLE_DESCRIPTOR,
                               "<" + SAML2MetaSecurityUtils.PREFIX_MD_QUERY
                               + ":" +
                               SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR);
                    metaXML = metaXML.replaceAll("</" +
                               SAML2MetaConstants.ROLE_DESCRIPTOR,
                               "</" +
                               SAML2MetaSecurityUtils.PREFIX_MD_QUERY
                               + ":" +
                               SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR);
                }
            }
        }
        return metaXML;
    }
    
    private String getEntityID(String metaXML) {
        try {
            Object obj = SAML2MetaUtils.convertStringToJAXB(metaXML);
            if (obj instanceof EntityDescriptorElement) {
                return ((EntityDescriptorElement) obj).getEntityID();
            }
        } catch (JAXBException jaxbe) {
            debug.error("FedletConfigImpl.getEntityID: " + metaXML, jaxbe);
        }
        return null;
    }

    /**
     * Gets COT information from flat file and stores in cotMap.
     * The COT is stored in a flat file named "<fileName>.cot" which contains
     * list of properties, format like this :
     *     <attribute_name>=<value1>,<value2>,<value3>,...
     * for example:
     *     cot-name=sample
     *     sun-fm-cot-status=Active
     *     sun-fm-trusted-providers=idp,sp
     * Note : Value which contains "%" and "," need to be escaped to 
     *        "%25" and "%2c" before saving to the file.
     */
    private void handleCOT(String fileName) {
        String cotFile = fedletHomeDir + File.separator + fileName
            + COT_FILE_SUFFIX;
        if (debug.messageEnabled()) {
            debug.message("FedletConfigurationImpl.handleCOT: " +
                "cotFile=" + cotFile);
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(cotFile);
            Properties props = new Properties();
            props.load(fis); 
            // convert each value string to a Set.
            Map attrMap = new HashMap();
            if (props != null) {
                Enumeration keys = props.propertyNames();
                while (keys.hasMoreElements()) {
                    String key = (String)keys.nextElement();
                    String vals = props.getProperty(key);
                    if ((vals != null) && (vals.length() > 0)) {
                        attrMap.put(key, toValSet(key, vals));
                    }
                }
            }
            Set cotName = (Set) attrMap.get(COT_NAME);
            if (cotName == null) {
                debug.error("FedletConfigImpl.handleCOT: null COT name in "
                    + cotFile);
            } else {
                cotMap.put((String) cotName.iterator().next(), attrMap);
                if (debug.messageEnabled()) {
                    debug.message("FedletConfigurationImpl.handleCOT: " +
                        "done processing cot " + cotName);
                }
            }
        } catch (FileNotFoundException fnf) {
            debug.error("FedletConfigurationImpl.handleCOT: " + cotFile 
                + " for component " + componentName, fnf);
        } catch (IOException ioe) {
            debug.error("FedletConfigurationImpl.getConfiguration:"  + cotFile 
                + " for component " + componentName, ioe);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ioe) {
                }
            } 
        }
    }

    /**
     * Converts a string of values from the attributes properties file 
     * to a Set, decoding special characters in each value.
     */
    protected Set toValSet(String attrName, String vals) {
        Set valset = new HashSet();
        char[] valchars = vals.toCharArray();
        int i, j;

        for (i = 0, j = 0; j < valchars.length; j++) {
            char c = valchars[j];
            if (c == ',') {
                if (i == j) {
                    i = j +1;
                } else { // separator found
                    String val = new String(valchars, i, j-i).trim();
                    if (val.length() > 0) {
                        val = decodeVal(val);
                    }
                    valset.add(val);
                    i = j +1;
                }
            }
        }
        if (j == valchars.length && i < j) {
            String val = new String(valchars, i, j-i).trim();
            if (val.length() > 0) {
                val = decodeVal(val);
            }
            valset.add(val);
        }
        return valset;
    }


    /** 
     * Decodes a value, %2C to comma and %25 to percent. 
     */
    protected String decodeVal(String v) {
        char[] chars = v.toCharArray();
        StringBuffer sb = new StringBuffer(chars.length);
        int i = 0, lastIdx = 0;
        for (i = 0; i < chars.length; i++) {
            if (chars[i] == '%' && i+2 < chars.length && chars[i+1] == '2') {
                if (lastIdx != i) {
                    sb.append(chars, lastIdx, i-lastIdx);
                }
                if (chars[i+2] == 'C') {
                    sb.append(',');
                }
                else if (chars[i+2] == '5') {
                    sb.append('%');
                }
                else {
                    sb.append(chars, i, 3);
                }
                i += 2;
                lastIdx = i+1;
            }
        }
        if (lastIdx != i) {
            sb.append(chars, lastIdx, i-lastIdx);
        }
        return sb.toString();
    }

    /**
     * Returns the content of a file as String.
     * Returns null if error occurs.
     */
    private String openFile(String file) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file)); 
            StringBuffer sb = new StringBuffer(5000);
            String temp;
            while ((temp = br.readLine()) != null) {
                sb.append(temp);
            }
            return sb.toString();
        } catch (FileNotFoundException fnf) {
            debug.error("FedletConfigurationImpl.getConfiguration: " + file
                + " for component " + componentName, fnf);
            return null;
        } catch (IOException ioe) {
            debug.error("FedletConfigurationImpl.getConfiguration:"  + file
                + " for component " + componentName, ioe);
            return null;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    /**
     * Sets Configurations.
     * @param realm the name of organization at which the configuration resides.
     * @param configName configuration instance name. e.g. "/sp"
     *     The configName could be null or empty string, which means the default
     *     configuration for this components.
     * @param avPairs Map of key/value pairs to be set in the service
     *     configuration, key is the attribute name, value is
     *     a Set of attribute values. 
     * @exception ConfigurationException if could not set service configuration
     *     or service configuration doesn't exist.
     */
    public void setConfiguration(String realm,
        String configName, Map avPairs)
        throws ConfigurationException {

        if (debug.messageEnabled()) {
            debug.message("FedletConfigurationImpl.setConfiguration: " +
                "componentName = " + componentName + ", realm = " + realm +
                ", configName = " + configName + ", avPairs = " + avPairs);
        }

        String[] data = { componentName, realm };
        throw new ConfigurationException(RESOURCE_BUNDLE, 
            "failedSetConfig", data);
    }

    /**
     * Creates Configurations.
     * @param realm the name of organization at which the configuration resides.
     * @param configName service configuration name. e.g. "/sp"
     *     The configName could be null or empty string, which means the
     *     default configuration for this components.
     * @param avPairs Map of key/value pairs to be set in the service
     *     configuration, key is the attribute name, value is
     *     a Set of attribute values. 
     * @exception ConfigurationException if could not create service 
     *     configuration.
     */
    public void createConfiguration(String realm, String configName,
        Map avPairs)
        throws ConfigurationException {

        if (debug.messageEnabled()) {
            debug.message("FedletConfigurationImpl.createConfiguration: " +
                "componentName = " + componentName + ", realm = " + realm +
                ", configName = " + configName + ", avPairs = " + avPairs);
        }

        String[] data = { componentName, realm };
        throw new ConfigurationException(RESOURCE_BUNDLE,
            "failedCreateConfig", data);
    }

    /**
     * Deletes Configuration.
     * @param realm the name of organization at which the configuration resides.
     * @param configName service configuration name. e.g. "/sp"
     *     The configName could be null or empty string, which means the default
     *     configuration for this components.
     * @param attributes A set of attributes to be deleted from the Service
     *     configuration. If the value is null or empty, deletes all service 
     *     configuration.
     * @exception ConfigurationException if could not delete service 
     *     configuration.
     */
    public void deleteConfiguration(String realm, 
        String configName, Set attributes)
        throws ConfigurationException {

        if (debug.messageEnabled()) {
            debug.message("FedletConfigurationImpl.deleteConfiguration: " +
                "componentName = " + componentName + ", realm = " + realm +
                ", configName = " + configName + ", attributes = " +
                attributes);
        }

        String[] data = { componentName, realm };
        throw new ConfigurationException(RESOURCE_BUNDLE, 
            "failedDeleteConfig", data);
    }

    /**
     * Returns all service config name for this components.
     * @param realm the name of organization at which the configuration resides.
     * @return Set of service configuration names. Return null if there 
     *     is no service configuration for this component, return empty set
     *     if there is only default configuration instance.
     * @exception ConfigurationException if could not get all service 
     *     configuration names.
     */
    public Set getAllConfigurationNames(String realm) 
        throws ConfigurationException {

        if (debug.messageEnabled()) {
            debug.message("FedletConfigurationImpl.getAllConfigurationNames"+
                ": realm = " + realm + ", componentName = " + componentName);
        }
        if ("SAML2".equals(componentName)) {
            return entityMap.keySet();
        } else if ("LIBCOT".equals(componentName)) {
            return cotMap.keySet();
        } else {
            return Collections.EMPTY_SET;
        }
    }

    /**
     * Registers for changes to the component's configuration. The object will
     * be called when configuration for this component is changed.
     * @return the registered id for this listener instance.
     * @exception ConfigurationException if could not register the listener.
     */
    public String addListener(ConfigurationListener listener)
        throws ConfigurationException {
        return "NO_OP";
    }

    /**
     * Unregisters the listener from the component for the given
     * listener ID. The ID was issued when the listener was registered.
     * @param listenerID the returned id when the listener was registered.
     * @exception ConfigurationException if could not register the listener.
     */
    public void removeListener(String listenerID)
        throws ConfigurationException {
    }
}
