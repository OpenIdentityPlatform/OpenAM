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
 * $Id: SAMLPropertyXMLBuilder.java,v 1.7 2009/10/15 00:02:02 asyhuang Exp $
 *
 */

package com.sun.identity.console.federation;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.property.PropertyTemplate;
import com.sun.identity.console.property.PropertyXMLBuilderBase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SAMLPropertyXMLBuilder
    implements PropertyTemplate {
    private static SAMLPropertyXMLBuilder instance =
        new SAMLPropertyXMLBuilder ();
    private static Map profiles = new HashMap (10);
    private static List profileList = new ArrayList (5);
    public static final String DESTINATION_ARTIFACT = "destinationArtifact";
    public static final String DESTINATION_POST = "destinationPost";
    public static final String SOURCE_ARTIFACT = "sourceArtifact";
    public static final String SOURCE_POST = "sourcePost";
    public static final String DESTINATION_SOAP = "destinationSoap";
    
    private boolean allAttributesReadonly;
    
    static {
        String[] attrs = {
            SAMLConstants.PARTNERNAME,
            SAMLConstants.SOURCEID,
            SAMLConstants.TARGET,
            SAMLConstants.SAMLURL,
            SAMLConstants.HOST_LIST,
            SAMLConstants.SITEATTRIBUTEMAPPER,
            SAMLConstants.NAMEIDENTIFIERMAPPER,            
            SAMLConstants.VERSION
        };
        String[] mand = {
            SAMLConstants.PARTNERNAME,
            SAMLConstants.SOURCEID,
            SAMLConstants.TARGET,
            SAMLConstants.SAMLURL,
            SAMLConstants.HOST_LIST
        };
        
        SAMLProperty samlProp = new SAMLProperty (
            DESTINATION_ARTIFACT, SAMLProperty.ROLE_DESTINATION,
            SAMLProperty.METHOD_ARTIFACT, attrs, mand);
        profiles.put (DESTINATION_ARTIFACT, samlProp);
        profileList.add (samlProp);
    }
    
    static {
        String[] attrs = {
            SAMLConstants.PARTNERNAME,
            SAMLConstants.SOURCEID,
            SAMLConstants.TARGET,
            SAMLConstants.POSTURL,
            SAMLConstants.SITEATTRIBUTEMAPPER,
            SAMLConstants.NAMEIDENTIFIERMAPPER,           
            SAMLConstants.VERSION
        };
        String[] mand = {
            SAMLConstants.PARTNERNAME,
            SAMLConstants.SOURCEID,
            SAMLConstants.TARGET,
            SAMLConstants.POSTURL
        };
        
        SAMLProperty samlProp = new SAMLProperty(
            DESTINATION_POST, SAMLProperty.ROLE_DESTINATION,
            SAMLProperty.METHOD_POST, attrs, mand);
        profiles.put(DESTINATION_POST, samlProp);
        profileList.add (samlProp);
    }
    
    static {
        String[] attrs = {
            SAMLConstants.PARTNERNAME,
            SAMLConstants.SOURCEID,
            SAMLConstants.SOAPUrl,           
            SAMLConstants.AUTHTYPE,
            SAMLConstants.AUTH_UID,
            SAMLConstants.AUTH_PASSWORD,
            SAMLConstants.AUTH_PASSWORD + SAMLPropertyTemplate.CONFIRM_SUFFIX,
            SAMLConstants.CERTALIAS,
            SAMLConstants.ACCOUNTMAPPER,
            SAMLConstants.VERSION
        };
        String[] mand = {
            SAMLConstants.PARTNERNAME,
            SAMLConstants.SOURCEID,
            SAMLConstants.SOAPUrl
        };
        
        SAMLProperty samlProp = new SAMLProperty (
            SOURCE_ARTIFACT, SAMLProperty.ROLE_SOURCE,
            SAMLProperty.METHOD_ARTIFACT, attrs, mand);
        profiles.put (SOURCE_ARTIFACT, samlProp);
        profileList.add (samlProp);
    }
    
    static {
        String[] attrs = {
            SAMLConstants.PARTNERNAME,
            SAMLConstants.SOURCEID,
            SAMLConstants.ISSUER,
            SAMLConstants.ACCOUNTMAPPER,
            SAMLConstants.CERTALIAS
        };
        String[] mand = {
            SAMLConstants.PARTNERNAME,
            SAMLConstants.SOURCEID,
            SAMLConstants.ISSUER
        };
        
        SAMLProperty samlProp = new SAMLProperty (
            SOURCE_POST, SAMLProperty.ROLE_SOURCE,
            SAMLProperty.METHOD_POST, attrs, mand);
        profiles.put (SOURCE_POST, samlProp);
        profileList.add (samlProp);
    }
    
    static {
        String[] attrs = {
            SAMLConstants.PARTNERNAME,
            SAMLConstants.SOURCEID,
            SAMLConstants.HOST_LIST,
            SAMLConstants.ATTRIBUTEMAPPER,            
            SAMLConstants.ACTIONMAPPER,
            SAMLConstants.CERTALIAS,
            SAMLConstants.ISSUER,
            SAMLConstants.VERSION
        };
        String[] mand = {
            SAMLConstants.PARTNERNAME,
            SAMLConstants.SOURCEID,
            SAMLConstants.HOST_LIST
        };
        
        SAMLProperty samlProp = new SAMLProperty (
            DESTINATION_SOAP, SAMLProperty.ROLE_DESTINATION,
            SAMLProperty.METHOD_SOAP, attrs, mand);
        profiles.put (DESTINATION_SOAP, samlProp);
        profileList.add (samlProp);
    }
    /**
     * get samlv1.x Instance.
     *
     * @return samlv1.x instance
     */
    public static SAMLPropertyXMLBuilder getInstance() {
        return instance;
    }
    
    /**
     * get samlv1.x Property.
     *
     * @param name
     * @return samlv1.x Property
     */
    public static SAMLProperty getSAMLProperty(String name) {
        return (SAMLProperty)profiles.get (name);
    }
    
    /**
     * get samlv1.x attribute names
     *
     * @param samlProperties a list of saml properties
     * @return Set a set of attributes
     */
    public Set getAttributeNames (List samlProperties) {
        Set attributes = new HashSet ();
        for (Iterator i = samlProperties.iterator (); i.hasNext (); ) {
            SAMLProperty p = (SAMLProperty)i.next ();
            attributes.addAll (p.getAttributeNames ());
        }
        return attributes;
    }
    
    /**
     * get samlv1.x Properties
     *
     * @param values a Map of attribute values
     * @return List a list of SAMLProperty
     */
    public List getSAMLProperties (Map values) {
        List list = new ArrayList (values.size()*2);
        Set attributeNames = AMAdminUtils.lowerCase (values.keySet ());
        // for upgrade case, the old value won't have PARTNERNAME
        // if we don't add PARTNERNAME to attributeNames, this function
        // will return empty List.
        attributeNames.add(SAMLConstants.PARTNERNAME.toLowerCase());

        for (Iterator i = profileList.iterator (); i.hasNext (); ) {
            SAMLProperty p = (SAMLProperty)i.next ();
            if (attributeNames.containsAll (
                AMAdminUtils.lowerCase (
                p.getMandatoryAttributeNames ()))) 
            {
                list.add (p);
            }
        }
        return list;
    }
    
    /**
     * get samlv1.x Properties
     *
     * @param samlProperties a Map of attribute values.
     * @return a set of SAMLProperty.
     */
    public Set getMandatoryAttributeNames(List samlProperties) {
        Set attributes = new HashSet ();
        for (Iterator i = samlProperties.iterator (); i.hasNext (); ) {
            SAMLProperty p = (SAMLProperty)i.next ();
            attributes.addAll (p.getMandatoryAttributeNames ());
        }
        return attributes;
    }
    
    /**
     * get samlv1.x trusted partner properties sheet, xml format.
     *
     * @param samlProperties a List of samlv1.x Properties
     * @param edit boolean for readonly or editable
     * @return String a string of saml trusted partner properties
     */
    public String getXML (List samlProperties, boolean edit) {
        StringBuffer buff = new StringBuffer ();
        List commonAttributes = new ArrayList ();
        List attributes = new ArrayList ();
        
        if ((samlProperties != null) && !samlProperties.isEmpty ()) {
            buff.append (PropertyXMLBuilderBase.getXMLDefinitionHeader())
            .append (START_TAG);
            getAttributes (samlProperties, commonAttributes, attributes);
            
            buff.append (SAMLPropertyTemplate.getSection (
                SAMLProperty.COMMON_SETTINGS));
            if (edit && !allAttributesReadonly) {
                buff.append (SAMLPropertyTemplate.EDIT_SETTING_XML);
            }
            for (Iterator i = commonAttributes.iterator (); i.hasNext (); ) {
                buff.append (SAMLPropertyTemplate.getAttribute (
                    (String)i.next (), allAttributesReadonly));
            }
            buff.append (SECTION_END_TAG);
            
            appendAttributes (buff, SAMLProperty.ROLE_DESTINATION,
                samlProperties, attributes);
            appendAttributes (buff, SAMLProperty.ROLE_SOURCE,
                samlProperties, attributes);
            buff.append (END_TAG);
        }
        return buff.toString ();
    }
    
    private void addAttributeForEachProfile (
        StringBuffer buff,
        SAMLProperty p,
        List attributes
        ) {
        List pAttributes = p.getAttributeNames ();
        boolean isEmpty = true;
        
        for (Iterator i = pAttributes.iterator (); i.hasNext (); ) {
            String attrName = (String)i.next ();
            
            if (attributes.contains (attrName)) {
                isEmpty = false;
                if (attrName.equals (SAMLConstants.ISSUER) &&
                    (p == profiles.get (DESTINATION_SOAP))
                    ) {
                    buff.append ((allAttributesReadonly) ?
                        SAMLPropertyTemplate.DESTINATION_SOAP_ISSUER_XML_READONLY :
                        SAMLPropertyTemplate.DESTINATION_SOAP_ISSUER_XML);
                } else {
                    buff.append (SAMLPropertyTemplate.getAttribute (attrName,
                        allAttributesReadonly));
                }
            }
        }
        
        if (isEmpty) {
            buff.append (SAMLPropertyTemplate.NO_ATTRIBUTE_CC);
        }
    }
    
    private void appendAttributes (
        StringBuffer buff,
        String role,
        List samlProperties,
        List attributes
    ) {
        List selected = getSAMLProperty(role, samlProperties);
        
        if (!selected.isEmpty ()) {
            buff.append (SAMLPropertyTemplate.getSection (role));
            
            for (Iterator i = selected.iterator (); i.hasNext (); ) {
                SAMLProperty p = (SAMLProperty)i.next ();
                buff.append (SAMLPropertyTemplate.getSection (
                    p.getBindMethod ()));
                addAttributeForEachProfile (buff, p, attributes);
                buff.append (SUBSECTION_END_TAG);
            }
            
            buff.append (SECTION_END_TAG);
        }
    }
    
    private List getSAMLProperty(String role, List samlProperties) {
        List selected = new ArrayList ();
        for (Iterator iter = samlProperties.iterator (); iter.hasNext (); ) {
            SAMLProperty p = (SAMLProperty)iter.next ();
            if (p.getRole ().equals (role)) {
                selected.add (p);
            }
        }
        return selected;
    }
    
    private void getAttributes (
        List samlProperties,
        List common,
        List allAttributes
        ) {
        for (Iterator iter = samlProperties.iterator (); iter.hasNext (); ) {
            SAMLProperty p = (SAMLProperty)iter.next ();
            
            if (allAttributes.isEmpty ()) {
                allAttributes.addAll (p.getAttributeNames ());
                allAttributes.remove (SAMLConstants.PARTNERNAME);
                common.add (SAMLConstants.PARTNERNAME);
                allAttributes.remove (SAMLConstants.SOURCEID);
                common.add (SAMLConstants.SOURCEID);
            } else {
                List pList = p.getAttributeNames ();
                getCommonAttributes (pList, allAttributes, common);
            }
        }
    }
    
    private void getCommonAttributes (List list1, List master, List common) {
        for (Iterator iter = list1.iterator (); iter.hasNext (); ) {
            Object e = iter.next ();
            if (!common.contains (e)) {
                if (master.contains (e)) {
                    common.add (e);
                    master.remove (e);
                } else {
                    master.add (e);
                }
            }
        }
    }
    
    /**
     * Set all all attribute values read only.
     *
     * @param flag true if all attribute values are read only.
     */
    public void setAllAttributeReadOnly (boolean flag) {
        allAttributesReadonly = flag;
    }
}
