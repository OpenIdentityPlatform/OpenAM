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
 * $Id: SubConfigPropertyXMLBuilder.java,v 1.3 2008/10/02 16:31:29 veiming Exp $
 *
 */

package com.sun.identity.console.property;

import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SubConfigPropertyXMLBuilder
    extends PropertyXMLBuilderBase
{
    private ServiceSchema serviceSchema;

    /**
     * Returns a XML for displaying attribute in property sheet.
     *
     * @param serviceName Name of the service.
     * @param serviceSchema Service Schema.
     * @param model Model for getting localized string and user locale.
     * @throws SMSException if attribute schema cannot obtained.
     * @throws SSOException if single sign on token is invalid.
     */
    public SubConfigPropertyXMLBuilder(
        String serviceName,
        ServiceSchema serviceSchema,
        AMModel model
    ) throws SMSException, SSOException
    {
        this.model = model;
        this.serviceSchema = serviceSchema;
        this.serviceName = serviceName;
        svcSchemaManager = new ServiceSchemaManager(
            serviceName, model.getUserSSOToken());
        getServiceResourceBundle();
        if (serviceBundle != null) {
            mapTypeToAttributeSchema = getSubConfigAttributeSchemas();
        }
    }

    /**
     * Returns a XML for displaying attribute in property sheet.
     *
     * @param childSchemaName Sub Schema Name of Child configuration.
     * @throws SMSException if attribute schema cannot obtained.
     * @throws SSOException if single sign on token is invalid.
     * @return XML for displaying attribute in property sheet.
     */
    private Map getSubConfigAttributeSchemas()
        throws SMSException, SSOException {
        Map map = new HashMap();
        /*
         * 2005-03-01 Dennis
         * Supporting global sub configuration only.
         */
        Object sectionTitle = mapSchemaTypeToName.get(SchemaType.GLOBAL);
        if (sectionTitle != null) {
            Set set = getSubConfigAttributeSchema();
            if ((set != null) && !set.isEmpty()) {
                map.put(SchemaType.GLOBAL, set);
            }
        }
        return map;
    }

    private Set getSubConfigAttributeSchema() {
        Set results = null;
        Set attributes = serviceSchema.getAttributeSchemas();

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

        return (results != null) ? results : Collections.EMPTY_SET;
    }

    /**
     * Returns a XML for displaying attribute in property sheet.
     *
     * @throws SMSException if attribute schema cannot obtained.
     * @throws SSOException if single sign on token is invalid.
     * @return XML for displaying attribute in property sheet.
     */
    public String getXML()
        throws SMSException, SSOException {
        StringBuffer xml = new StringBuffer(1000);
        xml.append(getXMLDefinitionHeader())
           .append(START_TAG);

        if (supportSubConfig) {
            String configTable = SUB_CONFIG_TABLE_XML;
            configTable = tagSwap(
                configTable, SUB_CONFIG_TABLE_VIEW_BEAN, viewBeanName);
            xml.append(configTable);
        }

        Set attributeSchema = (Set)mapTypeToAttributeSchema.get(
            SchemaType.GLOBAL);
        if ((attributeSchema != null) && !attributeSchema.isEmpty()) {
            String display = model.getLocalizedString(
                (String)mapSchemaTypeToName.get(SchemaType.GLOBAL));
            buildSchemaTypeXML(display, attributeSchema, xml, model,
                serviceBundle, Collections.EMPTY_SET);
        }

        xml.append(END_TAG);
        return xml.toString();
    }
}
