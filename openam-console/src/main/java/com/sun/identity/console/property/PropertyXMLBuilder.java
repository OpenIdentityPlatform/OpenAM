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
 * $Id: PropertyXMLBuilder.java,v 1.4 2008/10/02 16:31:29 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.console.property;

import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class PropertyXMLBuilder extends PropertyXMLBuilderBase {
    private Set<SchemaType> schemaTypes;
    private final static String FILENAME_SUFFIX = ".section.properties";
    private Map<String, List<String>> sectionOrder = null;
    private Map<SchemaType, List<String>> sectionForType = null;

    /**
     * Returns a XML for displaying attribute in property sheet.
     *
     * @param serviceName Name of the service.
     * @param schemaTypes Set of schema types to display.
     * @param model Model for getting localized string and user locale.
     * @throws SMSException if attribute schema cannot obtained.
     * @throws SSOException if single sign on token is invalid.
     */
    public PropertyXMLBuilder(
        String serviceName,
        Set schemaTypes,
        AMModel model
    ) throws SMSException, SSOException
    {
        this.model = model;
        this.schemaTypes = schemaTypes;
        this.serviceName = serviceName;
        svcSchemaManager = new ServiceSchemaManager(
            serviceName, model.getUserSSOToken());
        getServiceResourceBundle();
        if (getSectionOrder()) {
            getSectionsForType();
        }
        
        if (serviceBundle != null) {
            mapTypeToAttributeSchema = getAttributeSchemas(serviceName);
        } 
    }

    /**
     * Returns a XML for displaying attribute in property sheet.
     *
     * @param serviceSchema Service schemas.
     * @param model Model for getting localized string and user locale.
     * @throws SMSException if attribute schema cannot obtained.
     * @throws SSOException if single sign on token is invalid.
     */
    public PropertyXMLBuilder(ServiceSchema serviceSchema, AMModel model)
        throws SMSException, SSOException
    {
        this.model = model;
        this.serviceName = serviceSchema.getServiceName();
        getServiceResourceBundle(serviceSchema);

        if (serviceBundle != null) {
            mapTypeToAttributeSchema = new HashMap();
            mapTypeToAttributeSchema.put(
                serviceSchema.getServiceType(),
                serviceSchema.getAttributeSchemas());
        }
    }
    
    public PropertyXMLBuilder(
        String serviceName,
        AMModel model,
        Set attributeSchemas
    ) throws SMSException, SSOException {
        this(serviceName, model, attributeSchemas, null); 
    }

    /**
     * Constructs a XML builder.
     *
     * @param serviceName Name of the service.
     * @param model Model for getting localized string and user locale.
     * @param attributeSchemas List of attributeSchema.
     * @throws SMSException if attribute schema cannot obtained.
     * @throws SSOException if single sign on token is invalid.
     */
    public PropertyXMLBuilder(
        String serviceName,
        AMModel model,
        Set attributeSchemas,
        SchemaType schemaType
    ) throws SMSException, SSOException {
        this.model = model;
        this.serviceName = serviceName;
        svcSchemaManager = new ServiceSchemaManager(
            serviceName, model.getUserSSOToken());
        
        if (schemaType != null) {
            if (getSectionOrder()) {
                schemaTypes = new HashSet<SchemaType>();
                schemaTypes.add(schemaType);
                getSectionsForType();
            }
        }
        
        getServiceResourceBundle();
        if (serviceBundle != null) {
            mapTypeToAttributeSchema = new HashMap(attributeSchemas.size() *2);
            mapTypeToAttributeSchema.put(NULL_TYPE, attributeSchemas);
        }
    }

    /**
     * Returns a XML for displaying attribute in property sheet.
     *
     * @param serviceSchema Service schemas.
     * @param model Model for getting localized string and user locale.
     * @throws SMSException if attribute schema cannot obtained.
     * @throws SSOException if single sign on token is invalid.
     */
    public PropertyXMLBuilder(
        ServiceSchema serviceSchema, 
        AMModel model,
        Set attributeSchemas
    ) throws SMSException, SSOException {
        this.model = model;
        this.serviceName = serviceSchema.getServiceName();
        getServiceResourceBundle(serviceSchema);

        if (serviceBundle != null) {
            mapTypeToAttributeSchema = new HashMap(attributeSchemas.size()*2);
            mapTypeToAttributeSchema.put(NULL_TYPE, attributeSchemas);
        }
    }

    /**
     * Returns a XML for displaying attribute in property sheet.
     *
     * @throws SMSException if attribute schema cannot obtained.
     * @throws SSOException if single sign on token is invalid.
     * @throws AMConsoleException if there are no attribute to display.
     * @return XML for displaying attribute in property sheet.
     */
    public String getXML()
        throws SMSException, SSOException, AMConsoleException {
        return getXML(Collections.EMPTY_SET, true);
    }
    
    /**
     * Returns an XML string for displaying attributes in a property sheet for 
     * a specific realm. This will build a page whose values may be specific 
     * to the specified realm.
     *
     * @param realmName name of the realm to obtain values.
     * @throws SMSException if attribute schema cannot obtained.
     * @throws SSOException if single sign on token is invalid.
     * @throws AMConsoleException if there are no attribute to display.
     * @return XML for displaying attribute in property sheet.
     */
    public String getXML(String realmName)
        throws SMSException, SSOException, AMConsoleException {
        
        currentRealm = realmName;
        return getXML(Collections.EMPTY_SET, true);
    }
    
    /**
     * Returns a XML for displaying attribute in property sheet.
     *
     * @param bPropertySheetElementNode true to include
     *        <code>&lt;propertysheet&gt;</code> tag.
     * @throws SMSException if attribute schema cannot obtained.
     * @throws SSOException if single sign on token is invalid.
     * @throws AMConsoleException if there are no attribute to display.
     * @return XML for displaying attribute in property sheet.
     */
    public String getXML(boolean bPropertySheetElementNode)
        throws SMSException, SSOException, AMConsoleException {
        return getXML(Collections.EMPTY_SET, bPropertySheetElementNode);
    }

    /**
     * Returns a XML for displaying attribute in property sheet.
     *
     * @param readonly Set of readonly attributes.
     * @throws SMSException if attribute schema cannot obtained.
     * @throws SSOException if single sign on token is invalid.
     * @throws AMConsoleException if there are no attribute to display.
     * @return XML for displaying attribute in property sheet.
     */
    public String getXML(Set readonly)
        throws SMSException, SSOException, AMConsoleException {
        return getXML(readonly, true);
    }

    /**
     * Returns a XML for displaying attribute in property sheet.
     *
     * @param readonly Set of readonly attributes.
     * @param bPropertySheetElementNode true to include
     *        <code>&lt;propertysheet&gt;</code> tag.
     * @throws SMSException if attribute schema cannot obtained.
     * @throws SSOException if single sign on token is invalid.
     * @throws AMConsoleException if there are no attribute to display.
     * @return XML for displaying attribute in property sheet.
     */
    public String getXML(Set readonly, boolean bPropertySheetElementNode)
        throws SMSException, SSOException, AMConsoleException {
        StringBuffer xml = new StringBuffer(1000);
        onBeforeBuildingXML(mapTypeToAttributeSchema);

        if (bPropertySheetElementNode) {
            xml.append(getXMLDefinitionHeader()).append(START_TAG);
        }

        if (supportSubConfig) {
            String configTable = SUB_CONFIG_TABLE_XML;
            configTable = tagSwap(
                configTable, SUB_CONFIG_TABLE_VIEW_BEAN, viewBeanName);
            xml.append(configTable);
        }

        if (!supportSubConfig && hasNoAttributes()) {
            throw new AMConsoleException(model.getLocalizedString(
                "propertysheet.no.attributes.message"));
        }

        Set attributeSchema = (Set) mapTypeToAttributeSchema.get(NULL_TYPE);
        if ((attributeSchema != null) && !attributeSchema.isEmpty()) {
            String display = "blank.header";
            SchemaType type = null;

            if ((schemaTypes != null) && (schemaTypes.size() == 1)) {
                Iterator<SchemaType> it = schemaTypes.iterator();
                type = it.next();
            }
            
            if (getSectionsForType(type) == null) {
                buildSchemaTypeXML(display, attributeSchema, xml, model,
                    serviceBundle, readonly);
            } else {
                String label = "lbl" + display.replace('.', '_');
                Object[] params = { label, display};
                xml.append(MessageFormat.format(SECTION_START_TAG, params));

                for (String section : sectionForType.get(type)) {
                    String sectionName = 
                            model.getLocalizedString("section.label." + serviceName + "." + type.getType() + "." + section);
                    buildSchemaTypeXML(sectionName, attributeSchema, xml,
                        model, serviceBundle, readonly, sectionOrder.get(section));
                }

                xml.append(SECTION_END_TAG);                
            }
        }

        for (Iterator iter = orderDisplaySchemaType.iterator(); iter.hasNext();) {
            SchemaType type = (SchemaType)iter.next();
            attributeSchema = (Set)mapTypeToAttributeSchema.get(type);

            if ((attributeSchema != null) && !attributeSchema.isEmpty()) {
                String display = model.getLocalizedString(
                    (String)mapSchemaTypeToName.get(type));
                if (getSectionsForType(type) == null) {
                    buildSchemaTypeXML(display, attributeSchema, xml,
                        model, serviceBundle, readonly);
                } else {
                    String label = "lbl" + display.replace('.', '_');
                    Object[] params = { label, display};
                    xml.append(MessageFormat.format(SECTION_START_TAG, params));
        
                    for (String section : sectionForType.get(type)) {
                        String sectionName = 
                                model.getLocalizedString("section.label." + serviceName + "." + type.getType() + "." + section);
                        buildSchemaTypeXML(sectionName, attributeSchema, xml,
                            model, serviceBundle, readonly, sectionOrder.get(section));
                    }
                    
                    xml.append(SECTION_END_TAG);
                }
            }
        }

        if (bPropertySheetElementNode) {
            xml.append(END_TAG);
        }

        return xml.toString();
    }

    private boolean hasNoAttributes() {
        boolean no = true;
        if ((mapTypeToAttributeSchema != null) &&
            (!mapTypeToAttributeSchema.isEmpty())) 
        {
            for (Iterator i = mapTypeToAttributeSchema.keySet().iterator();
                i.hasNext() && no; ) 
            {
                Set set = (Set)mapTypeToAttributeSchema.get(i.next());
                no = (set == null) || set.isEmpty();
            }
        }
        return no;
    }

    private Map getAttributeSchemas(String serviceName) 
        throws SMSException, SSOException {
        Map map = new HashMap();
        if (schemaTypes == null) {
            schemaTypes = svcSchemaManager.getSchemaTypes();
        } 

        for (Iterator iter = schemaTypes.iterator(); iter.hasNext(); ) {
            SchemaType type = (SchemaType)iter.next();
            Object sectionTitle = mapSchemaTypeToName.get(type);

            if (sectionTitle != null) {
                Set set = getAttributeSchemas(type);
                if ((set != null) && !set.isEmpty()) {
                    map.put(type, set);
                }
            }
        }

        return map;
    }

    private Set getAttributeSchemas(SchemaType type)
        throws SMSException {
        Set results = null;
        ServiceSchema schema = svcSchemaManager.getSchema(type);

        if (schema!= null) {
            Set attributes = schema.getAttributeSchemas();

            if ((attributes != null) && !attributes.isEmpty()) {
                results = new HashSet(attributes.size() *2);
                for (Iterator iter = attributes.iterator(); iter.hasNext(); ) {
                    AttributeSchema as = (AttributeSchema)iter.next();
                    String i18nKey = as.getI18NKey();

                    if ((i18nKey != null) && (i18nKey.trim().length() > 0)) {
                        results.add(as);
                    }
                }
            }
        }

        return results;
    }

    /** 
     * Extends this method to selective drop attribute schema that you do not
     * want to display.
     *
     * @param map Map of schema type (com.sun.identity.sm.SchemaType) to a set
     *        of attribute schema (com.sun.identity.sm.AttributeSchema)
     */
    protected void onBeforeBuildingXML(Map<SchemaType, Set<AttributeSchema>> map) {
        if (serviceName.equals("iPlanetAMSessionService")) {
            //remove deprecated property from the console
            Set<AttributeSchema> attrs = map.get(SchemaType.GLOBAL);
            if (attrs != null) {
                Iterator<AttributeSchema> it = attrs.iterator();
                while (it.hasNext()) {
                    if (it.next().getName().equals("iplanet-am-session-constraint-resulting-behavior")) {
                        it.remove();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Discards attributes from the builder.
     * 
     * @param discard Set of attribute names to be discarded.
     */
    public void discardAttribute(Set discard) {
        for (Iterator i = mapTypeToAttributeSchema.keySet().iterator();
            i.hasNext();
        ) {
            SchemaType type = (SchemaType)i.next();
            Set attributeSchema = (Set)mapTypeToAttributeSchema.get(type);

            if ((attributeSchema != null) && !attributeSchema.isEmpty()) {
                for (Iterator j = attributeSchema.iterator(); j.hasNext(); ) {
                    AttributeSchema as = (AttributeSchema)j.next();
                    if (discard.contains(as.getName())) {
                        j.remove();
                    }
                }
            }
        }
    }
    
    /**
     * Loads the section order (if available) for this service.
     * 
     * @return true if found, false other wise
     */
    private boolean getSectionOrder() {
        InputStream is = 
                getClass().getClassLoader().getResourceAsStream(serviceName + FILENAME_SUFFIX);
        
        if (is == null) {
            if (debug.messageEnabled()) {
                debug.message("getSectionOrder: no section for service" + serviceName);
            } 
            
            return false;
        }
        
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        
        try {
            String line = reader.readLine();
            while (line != null) {
                if (!line.startsWith("#") && (line.trim().length() > 0)) {
                    int idx = line.indexOf('=');
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx+1).trim();
                    
                    List<String> list = map.get(key);
                    if (list == null) {
                        list = new ArrayList<String>();
                        map.put(key, list);
                    }
                    list.add(value);
                }
                line = reader.readLine();
            }
        } catch (IOException ioe) {
            if (debug.messageEnabled()) {
                debug.message("PropertyXMLBuilder:getSectionOrder", ioe);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                    // do nothing
                }
            }
        }
        
        sectionOrder = map;
        
        if (debug.messageEnabled()) {
            debug.message("getSectionOrder: " + sectionOrder);
        }
        
        return true;
    }
    
    private void getSectionsForType() {
        sectionForType = new HashMap<SchemaType, List<String>>();
        
        for (SchemaType schemaType : schemaTypes) {
            String name = "sections." + serviceName + "." + schemaType.getType();
            String section = model.getLocalizedString(name);
            
            if (section.equals(name)) {
                continue;
            }
            
            StringTokenizer st = new StringTokenizer(section, " ");
            List<String> sections = new ArrayList<String>();
            
            while (st.hasMoreTokens()) {
                sections.add(st.nextToken());
            }
            
            sectionForType.put(schemaType, sections);
        }        
        
        if (debug.messageEnabled()) {
            debug.message("getSectionsForType: " + sectionForType);
        }
    }
    
    private List<String> getSectionsForType(SchemaType type) {
        if (sectionForType == null) {
            return null;
        }
        
        List<String> result = sectionForType.get(type);
        
        if (result == null) {
            return null;
        }
        
        return result;
    }
}
