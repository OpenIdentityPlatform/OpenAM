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
 * $Id: SMDiscoveryServiceModelImpl.java,v 1.2 2008/06/25 05:49:47 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMServiceProfileModelImpl;
import com.sun.identity.console.property.PropertyXMLBuilder;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - NEED NOT LOG - */

public class SMDiscoveryServiceModelImpl
    extends AMServiceProfileModelImpl
    implements SMDiscoveryServiceModel
{
    public static final String SERVICE_NAME =
	"sunIdentityServerDiscoveryService";
    private Map cachedValues;

    /**
     * Creates a simple model using default resource bundle.
     *
     * @param req HTTP Servlet Request
     * @param map of user information
     */
    public SMDiscoveryServiceModelImpl(
	HttpServletRequest req,
	Map map
    ) throws AMConsoleException {
	super(req, SERVICE_NAME, map);
    }

    /**
     * Creates a simple model using default resource bundle.
     *
     * @param req HTTP Servlet Request
     * @param serviceName Name of Service.
     * @param map of user information
     */
    public SMDiscoveryServiceModelImpl(
	HttpServletRequest req,
	String serviceName,
	Map map
    ) throws AMConsoleException {
	super(req, serviceName, map);
    }

    /**
     * Returns attributes values.
     *
     * @return attributes values.
     */
    public Map getAttributeValues() {
	if (cachedValues == null) {
	    cachedValues = super.getAttributeValues();
	}
	return cachedValues;
    }

    /**
     * Returns resource offering entry stored in the model map for a given
     * type.
     *
     * @param dynamic value to indicate if it is a dynamic or not.
     * @return resource offering entry stored in the model map for a given
     * type.
     */
    public Set getDiscoEntry(boolean dynamic) {
        Set set = null;
        if (dynamic) {
	    // TO FIX
        } else {
	    ServiceSchemaManager mgr = getServiceSchemaManager();
	    try {
		ServiceSchema schema = mgr.getSchema(
		    SchemaType.GLOBAL);
		AttributeSchema as = schema.getAttributeSchema(
		    AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF);
		set = as.getDefaultValues();
	    } catch (SMSException e) {
		debug.error("SMDiscoveryServiceModelImpl.getDiscoEntry", e);
	    }
        }
        return set;
    }

    /**
     * Returns provider resource ID mapper attribute value.
     *
     * @return provider resource ID mapper attribute value.
     */
    public Set getProviderResourceIdMapper() {
	Set set = null;
	ServiceSchemaManager mgr = getServiceSchemaManager();
	try {
	    ServiceSchema schema = mgr.getSchema(SchemaType.GLOBAL);
	    AttributeSchema as = schema.getAttributeSchema(
		AMAdminConstants.DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER);
	    set = as.getDefaultValues();
	} catch (SMSException e) {
	    debug.error(
		"SMDiscoveryServiceModelImpl.getProviderResourceIdMapper", e);
	}
	return set;
    }

    /**
     * Returns the XML for property sheet view component.
     *
     * @param realmName Name of Realm.
     * @param viewbeanClassName Class Name of View Bean.
     * @param serviceName Name of Service.
     * @return the XML for property sheet view component.
     * @throws AMConsoleException if XML cannot be created.
     */
    public String getPropertySheetXML(
	String realmName,
	String viewbeanClassName,
	String serviceName
    ) throws AMConsoleException {
        String xml = super.getPropertySheetXML(
        realmName, viewbeanClassName, serviceName);

        xml = PropertyXMLBuilder.removeSubSection(xml, 
            AMAdminConstants.DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER,
            TBL_PROVIDER_RESOURCE_ID_MAPPER);

        xml = PropertyXMLBuilder.appendXMLProperty(xml, TBL_BOOTSTRAP_RES_OFF);
        return xml;
    }

    protected void addMoreAttributeSchemasForModification(Set attributeSchemas){
	super.addMoreAttributeSchemasForModification(attributeSchemas);
	ServiceSchemaManager mgr = getServiceSchemaManager();

	try {
	    ServiceSchema global = mgr.getSchema(SchemaType.GLOBAL);
	    attributeSchemas.add(global.getAttributeSchema(
		AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF));
	} catch (SMSException e) {
	    debug.error(
	    "SMDiscoveryServiceModelImpl.addMoreAttributeSchemasForModification"
	    , e);
	}
    }

    private static String TBL_PROVIDER_RESOURCE_ID_MAPPER =
        "<property span=\"true\"><cc name=\"sunIdentityServerDiscoProviderResourceIDMapper\" tagclass=\"com.sun.web.ui.taglib.table.CCActionTableTag\" > <attribute name=\"title\" value=\"discovery.service.table.providerResourceIdMapper.name\" /><attribute name=\"empty\" value=\"discovery.service.table.providerResourceIdMapper.noentries\" /><attribute name=\"showPaginationControls\" value=\"false\" /><attribute name=\"selectionType\" value=\"multiple\" /><attribute name=\"selectionJavascript\" value=\"toggleTblButtonState('SMDiscoveryService', 'SMDiscoveryService.sunIdentityServerDiscoProviderResourceIDMapper', 'providerResourceIdMapperCount', 'SMDiscoveryService.tblProviderResourceIdMapperButtonDelete', this)\" /><attribute name=\"showAdvancedSortingIcon\" value=\"false\" /><attribute name=\"showLowerActions\" value=\"false\" /><attribute name=\"showPaginationControls\" value=\"false\" /><attribute name=\"showPaginationIcon\" value=\"false\" /><attribute name=\"showSelectionIcons\" value=\"true\" /><attribute name=\"showSelectionSortIcon\" value=\"false\" /><attribute name=\"showSortingRow\" value=\"false\" /></cc></property>";

    private static String TBL_BOOTSTRAP_RES_OFF =
        "<property span=\"true\"><cc name=\"sunIdentityServerBootstrappingDiscoEntry\" tagclass=\"com.sun.web.ui.taglib.table.CCActionTableTag\" > <attribute name=\"title\" value=\"discovery.service.table.bootstrapResourceOffering.name\" /><attribute name=\"empty\" value=\"discovery.service.table.bootstrapResourceOffering.noentries\" /><attribute name=\"showPaginationControls\" value=\"false\" /><attribute name=\"selectionType\" value=\"multiple\" /><attribute name=\"selectionJavascript\" value=\"toggleTblButtonState('SMDiscoveryService', 'SMDiscoveryService.sunIdentityServerBootstrappingDiscoEntry', 'bootstrapResOffCount', 'SMDiscoveryService.tblBootstrapResOffButtonDelete', this)\" /><attribute name=\"showAdvancedSortingIcon\" value=\"false\" /><attribute name=\"showLowerActions\" value=\"false\" /><attribute name=\"showPaginationControls\" value=\"false\" /><attribute name=\"showPaginationIcon\" value=\"false\" /><attribute name=\"showSelectionIcons\" value=\"true\" /><attribute name=\"showSelectionSortIcon\" value=\"false\" /><attribute name=\"showSortingRow\" value=\"true\" /></cc></property>";

}
