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
 * $Id: DMModelBase.java,v 1.4 2009/01/28 05:34:57 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.dm.model;

/*import com.iplanet.am.sdk.AMAssignableDynamicGroup;
 *import com.iplanet.am.sdk.AMDynamicGroup;
 *import com.iplanet.am.sdk.AMFilteredRole;
 *import com.iplanet.am.sdk.AMPeopleContainer;
 * import com.iplanet.am.sdk.AMStaticGroup;
 * import com.iplanet.services.util.Crypt;
 * import java.security.PrivilegedAction;
 *
 * */
import com.iplanet.am.sdk.AMEntity;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMGroup;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMRole;
import com.iplanet.am.sdk.AMSearchControl;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMTemplate;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.admin.AdminInterfaceUtils;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMResBundleCacher;
import com.sun.identity.console.base.model.AMSystemConfig;
import com.sun.identity.console.property.PropertyTemplate;
import com.sun.identity.console.property.PropertyXMLBuilder;
import com.sun.identity.console.property.PropertyXMLBuilderBase;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.RequiredValueValidator;
import java.security.AccessController;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.MissingResourceException;
import javax.servlet.http.HttpServletRequest;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.RDN;

/* - LOG COMPLETE - */

/**
 * This model is used by the all the User Management Navigation viewbeans to
 * get data for the show menu combo box and the title bar
 */
public class DMModelBase
    extends AMModelBase
    implements DMModel
{
    private static RequiredValueValidator reqValidator =
	new RequiredValueValidator();

    protected String errorMessage = null;
    protected String searchErrorMsg = null;
    protected Set values = null;
    protected List searchReturnAttrs = null;
    private Set searchResults = null;
    protected Map resultsMap = null;
    protected Map mandatoryAttributes = new HashMap();

    /** store connection object for current user */
    private AMStoreConnection dpStoreConn = null;

    private int groupConfiguration = AMObject.STATIC_GROUP;

    public int errorCode = AMSearchResults.SUCCESS;
    
    private String filterAttributeXML = null;
    private Set filterAttributes = null;
    private Set filterAttributeNames = null;

    protected int locationType = -1;
    private boolean userMgmtEnabled;
    private boolean peopleContainerDisplay;
    private boolean groupContainerDisplay;
    private boolean orgUnitDisplay;
    private boolean adminGroupsEnabled;

    private String DEFAULT_NAME_COLUMN = "table.dm.name.column.name";
    private String DEFAULT_SEARCH_PATTERN = "*";
    private String EMPTY_DISPLAY_STRING = "-";

    /**
     * Creates a navigation model implementation object
     *
     * @param req  HTTP Servlet Request
     * @param map of user information
     */
    public DMModelBase(HttpServletRequest req, Map map) {
	super(req, map);
    }

    protected void initialize(HttpServletRequest req, String rbName) {
        super.initialize(req, rbName);
        readConsoleProfile();
    }

    private boolean isAttributeSet(ServiceSchema schema, String attrName) {
        String value = getDefaultAttrValue(schema, attrName);
        return (value != null) && (value.equals("true"));
    }

    private void readConsoleProfile() {
        try {
            ServiceSchemaManager svcSchemaMgr =
                getServiceSchemaManager(ADMIN_CONSOLE_SERVICE);
            ServiceSchema schema = svcSchemaMgr.getSchema(SchemaType.GLOBAL);

            peopleContainerDisplay = isAttributeSet(schema,
                CONSOLE_PC_DISPLAY_ATTR);
            groupContainerDisplay = isAttributeSet(schema,
                CONSOLE_GC_DISPLAY_ATTR);
            adminGroupsEnabled = isAttributeSet(schema,
                CONSOLE_ADMIN_GROUPS_DISPLAY_ATTR);
            userMgmtEnabled = isAttributeSet(schema, CONSOLE_UM_ENABLED_ATTR);

            if (!AMSystemConfig.iPlanetCompliantDIT) {
                orgUnitDisplay = isAttributeSet(schema,
                    CONSOLE_OU_DISPLAY_ATTR);
                getGroupConfigurationType(schema);
            }
        } catch (SMSException smse) {
            debug.error("DMModelBase.readConsoleProfile", smse);
        } catch (SSOException ssoe) {
            debug.error("DMModelBase.readConsoleProfile", ssoe);
        }
    }

    private void getGroupConfigurationType(ServiceSchema schema) {
        String value = getDefaultAttrValue(schema, CONSOLE_GROUP_TYPE_ATTR);
        if (value.equalsIgnoreCase("dynamic")) {
            groupConfiguration = AMObject.ASSIGNABLE_DYNAMIC_GROUP;
        } else {
            groupConfiguration = AMObject.STATIC_GROUP;
        }
    }

    private String getDefaultAttrValue(ServiceSchema schema, String attrName) {
        String value = null;
        AttributeSchema as = schema.getAttributeSchema(attrName);
        Set defaultValue = as.getDefaultValues();
        if ((defaultValue != null) && !defaultValue.isEmpty()) {
            value = (String)defaultValue.iterator().next();
        }
        return value;
    }

    /**
     * Returns group configuration.
     *
     * @return group configuration.
     */
    protected int getGroupConfiguration() {
        return groupConfiguration;
    }

    /**
     * Returns error message that were generated in the model.
     *
     * @return error message List
     */
    public String getErrorMessage() {
	return errorMessage;
    }
    
    /**
     * Sets the error message in the error message Set of the
     * model. This will be displayed by viewbean.
     *
     * @param msgStr Error message as a String
     */    
    public void setErrorMessage(String msgStr) {
	errorMessage = msgStr;
    }
    
    /**
     * Returns the attribute list stored in the model.
     *
     * @return the attribute list stored in the model.
     */
    public Set getAttrList() {
        return values;
    }

    /**
     * Sets attribute list in model.
     *
     * @param set of data to be stored in model.
     */
    public void setAttrList(Set set) {
        values = set;
    }

    /**
     * Sets the search results from the <code>AMSearchResults</code> object
     * and stores the results, attribute map, and error code locally. 
     *
     * @param AMSearchResults search results object
     * @return set of search results.
     */
    Set setSearchResults(AMSearchResults results) {
	searchResults = Collections.EMPTY_SET;

	if (results != null) {
	    searchResults = results.getSearchResults();
            resultsMap = results.getResultAttributes();
            if (searchErrorMsg != null) {
                errorCode = results.getErrorCode();
            }
	}
	return searchResults;
    }

    Set getSearchResults() {
        return searchResults;
    }
    
    /**
     * Returns localized name of attribute name.
     *
     * @param name attribute name.
     * @param schemaName schema name.
     * @return localized name of attribute name.
     */
    public String getAttributeLocalizedName(String name, String schemaName) {
        String localizedStr = name;
        try {
            ServiceSchemaManager mgr = getServiceSchemaManager(
                ENTRY_SPECIFIC_SERVICE);

            String key = getSubSchemaI18NKey(
                mgr, SchemaType.GLOBAL, schemaName, name);
            if (key == null) {
                key = name;
            }
            localizedStr =  getL10NAttributeName(mgr, key);
        } catch (SMSException se) {
            debug.error ("DMModelBase.getAttributeLocalizedName", se);
        } catch (SSOException se) {
            debug.error ("DMModelBase.getAttributeLocalizedName", se);
        }
        return localizedStr;
    }

    /**
     * Returns the i18n key of an attribute.
     *
     * @param svcSchemaMgr service schema manager
     * @param type schema type
     * @param attribute name
     * @return i18n key of an attribute
     * @throws SMSException if operation fails
     */
    private String getSubSchemaI18NKey(
        ServiceSchemaManager svcSchemaMgr,
        SchemaType type,
        String name,
        String attribute)
        throws SMSException
    {
        String i18nKey = null;
        ServiceSchema schema = svcSchemaMgr.getSchema(type);
        if (schema != null) {
            ServiceSchema subSchema = schema.getSubSchema(name);
            if (subSchema != null) {
                AttributeSchema as = subSchema.getAttributeSchema(attribute);
                if (as != null) {
                    i18nKey = as.getI18NKey();
                }
            }
        }
        return i18nKey;
    }

    /**
     * Returns the localized string of an attribute from the i18n key. The i18n
     * key will be looked up in the properties file for the specified service
     * schema manager and the display string corresponding to the key will be
     * returned. If key does not exist in the properties file the key will be
     * returned.
     *
     * @param mgr service schema manager.
     * @param key i18n key of the attribute.
     * @return localized string of an attribute in a service.
     */
    protected String getL10NAttributeName(
        ServiceSchemaManager mgr,
        String key
    ) {
        String i18nName = key;
        try {
            String name = mgr.getI18NFileName();
            if (name != null) {
                ResourceBundle rb = AMResBundleCacher.getBundle(name, locale);
                i18nName = Locale.getString(rb, key, debug);
            }
        } catch (MissingResourceException mre) {
            debug.warning("DMModelBase.getL10NAttributeName", mre);
        }
        return i18nName;
    }

    /**
     * Returns object's attribute names.
     *
     * @param schemaName name of schema.
     * @param objectType object to search for.
     * @return a set of object's attribute names.
     */
    protected Set getObjectAttributeNames(String schemaName, int objectType) {
        
        ServiceSchemaManager mgr = null;
        
        try {
            getServiceSchemaManager(ENTRY_SPECIFIC_SERVICE);
        } catch (SSOException s) {
            if (debug.warningEnabled()) {
                debug.warning("DMModel.getObjectAttributeNames",s);
            }
            return Collections.EMPTY_SET;
        } catch (SMSException se) {
            if (debug.warningEnabled()) {
                debug.warning("DMModel.getObjectAttributeNames",se);
            }
        }

        Set attrSchemaSet = getAttributesToDisplay(
            mgr, SchemaType.GLOBAL, schemaName);

        Set set = Collections.EMPTY_SET;
        if (attrSchemaSet != null && !attrSchemaSet.isEmpty()) {
            Iterator iter = attrSchemaSet.iterator();
            set = new HashSet(attrSchemaSet.size()+1);
            while (iter.hasNext()) {
                AttributeSchema attrSchema = (AttributeSchema)iter.next();
                String name = attrSchema.getName();
                set.add(name);
            }
        } else {
            set = new HashSet(1);
        }
        // add the default attribute as valid attribute.
        set.add( AdminInterfaceUtils.getNamingAttribute(
                objectType, debug));
        return set;
    }

    /**
     * Sets the attributes to return in the search contol object.
     * 
     * @param searchControl <code>AMSearchControl</code>.
     * @param returnAttributes a <code>String</code> containing the attributes 
     *     to return in the search results map.
     */
    protected void setSearchControlAttributes(
        AMSearchControl searchControl,
        String returnAttributes
    ) {
        StringTokenizer tok = new StringTokenizer(returnAttributes);
        List searchAttrs = new ArrayList(tok.countTokens()*2);
        while (tok.hasMoreTokens()) {
            searchAttrs.add(tok.nextToken());
        } 

        String[] sortKeys = new String[1];
        sortKeys[0] = (String)searchAttrs.get(0);
        searchControl.setSortKeys(sortKeys);
        searchControl.setReturnAttributes(new HashSet(searchAttrs));
    }

    /**
     * Sets the attributes to return in the search contol object.
     * 
     * @param orgDN organization DN.
     * @param schemaName name of schema.
     * @param objectType object type.
     * @param searchControl <code>AMSearchControl</code>.
     * @param type navigation view type.
     */
    protected void setSearchControlAttributes(
        String orgDN,
        String schemaName,
        int objectType,
        AMSearchControl searchControl,
        String type
    ) {
        setLocationDN(orgDN);
        if (searchReturnAttrs == null) {
            String returnAttr = getSearchReturnValue();

            if (returnAttr == null) {             
                searchReturnAttrs = new ArrayList(1);
                searchReturnAttrs.add(AdminInterfaceUtils.getNamingAttribute(
                    objectType, debug));
            } else {
                searchReturnAttrs = getValidatedAttributes(
                    returnAttr, schemaName, objectType, type);
            }
        }

        String sortKey = (String)searchReturnAttrs.get(0);
        String[] sortKeys = new String[1];
        sortKeys[0] = sortKey;
        searchControl.setSortKeys(sortKeys);
        searchControl.setReturnAttributes(new HashSet(searchReturnAttrs));
    }

    /**
     * Returns the value of the Search Return Attribue found in
     * the administration service.
     *
     * @param schemaName name of schema.
     * @param objectType object to search for.
     * @param type navigation view type.
     * @return the value of the return attribute in the administration
     *    service.
     */
    public List getSearchReturnAttributes(
        String schemaName, 
        int objectType, 
        String type
    ) {
        if (searchReturnAttrs == null) {
            String returnAttr = getSearchReturnValue();
            if (returnAttr == null) {
                searchReturnAttrs = new ArrayList(1);
                searchReturnAttrs.add(AdminInterfaceUtils.getNamingAttribute(
                    objectType, debug));
            } else {
                searchReturnAttrs = getValidatedAttributes(
                    returnAttr, schemaName, objectType, type);
            }
        }
        return searchReturnAttrs;
    }

    /**
     * Returns the validated attribute names from a return search string.
     *
     * @param returnAttr string to search attributes for.
     * @param schemaName name of schema.
     * @param objectType object type.
     * @param type navigation view type.
     * @return the validated attribute names from a return search string.
     */
    protected List getValidatedAttributes(
        String returnAttr, 
        String schemaName, 
        int objectType,
        String type
    ) {
        List searchAttrs = Collections.EMPTY_LIST;
        if (returnAttr != null && returnAttr.length() > 0) {
            List list = getObjectDisplayList(returnAttr, type);
            if (list != null && !list.isEmpty()) {
                searchAttrs = new ArrayList(list.size());
                Set objAttrs = getObjectAttributeNames(schemaName, objectType);

                if (objAttrs != null && !objAttrs.isEmpty()) {
                    for (Iterator iter = list.iterator(); iter.hasNext();) {
                        String str = (String)iter.next();
                        if (objAttrs.contains(str) && 
                            !searchAttrs.contains(str)) 
                        {
                            searchAttrs.add(str);
                        }
                    }
                }
            }
        }
        if (searchAttrs == null || searchAttrs.isEmpty()) {
            searchAttrs = new ArrayList(1);
            searchAttrs.add(AdminInterfaceUtils.getNamingAttribute(
                objectType, debug));
        }
        return searchAttrs;
    }

    /**
     * Returns the value of an attribute for a given object and attribute name.
     *
     * @param objectDN DN of the user.
     * @param attrName attribute name.
     * @return the value of an attribute for a given role and attribute name.
     */
    public String getAttributeValue(String objectDN, String attrName) {
        String value = "";
        if (resultsMap != null && !resultsMap.isEmpty()) {
            Map lvalues = (Map)resultsMap.get(objectDN);
            if (lvalues != null && !lvalues.isEmpty()) {
                Set attrValues = (Set)lvalues.get(attrName);
                if (attrValues != null && !attrValues.isEmpty()) {
                    value = getMultiValue(attrValues);
                }
            }
        }
        return value;
    }


    /**
     * Returns the last attribute name from the search results.
     *
     * @param schemaName name of schema.
     * @param objectType object to search for.
     * @param type navigation view type.
     * @return the last name attribute from the search results.
     */
    public String getAttributeName(
        String schemaName, 
        int objectType, 
        String type) 
    {
        getSearchReturnAttributes(schemaName, objectType, type);

        if (searchReturnAttrs.isEmpty()) {
            return AdminInterfaceUtils.getNamingAttribute(
                objectType, debug);
        }
        int size = searchReturnAttrs.size();
        return (String)searchReturnAttrs.get(size-1);
    }

    /**
     * Returns the search attribute list stored in the model.
     *
     * @return the search attribute list stored in the model.
     */
    public List getAttrSearchList() {
        return searchReturnAttrs;
    }

    /**
     * Sets attribute search list.
     *
     * @param list  attribute search list
     */
    public void setAttrSearchList(List list) {
       searchReturnAttrs = list;
    }

    /**                                        
     * Returns the user map list stored in the model.
     *
     * @return the user map list stored in the model.
     */
    public Map getAttrMap() {
        return resultsMap;
    }

    /**
     * Sets attribute map for users.
     *
     * @param map data for users.
     */
    public void setAttrMap(Map map) {
       resultsMap = map;
    }

    /**
     * Returns values for attribute and service for organization template.
     * Values from default will be returned if template does not exist.
     *   
     * @param attrName name of attribute.
     * @param serviceName name of service.
     * @param dn current location DN.
     * @return attribute value for <code>attrName</code>
     */  
    protected Set getAttrValues(String attrName, String serviceName, String dn)
    {
        return getAttrValues(
            attrName, serviceName, dn, AMTemplate.ORGANIZATION_TEMPLATE);
    }

    /**
     * Returns values for attribute and service for a given template type.
     * Values from default will be returned if template does not exist.
     *   
     * @param attrName name of attribute.
     * @param serviceName name of service.
     * @param dn current location DN.
     * @param templateType type of template
     * @return attribute value for <code>attrName</code>
     */
    protected Set getAttrValues(
        String attrName,
        String serviceName,
        String dn,
        int templateType
    ) {
        Set values = Collections.EMPTY_SET;
 
        try {
            String orgDN = getOrganizationDN(dn);
            AMTemplate template = getServiceTemplate(
                orgDN, serviceName, templateType);
                  
            if ((template != null) && template.isExists()) {
                values = template.getAttribute(attrName);
            } else {
                SchemaType schemaType = SchemaType.ORGANIZATION;
                  
                if (templateType == AMTemplate.POLICY_TEMPLATE) {
                    schemaType = SchemaType.POLICY;
                } else if (templateType == AMTemplate.DYNAMIC_TEMPLATE) {
                    schemaType = SchemaType.DYNAMIC;
                }
 
                ServiceSchemaManager mgr = getServiceSchemaManager(serviceName);
		ServiceSchema schema = mgr.getSchema(schemaType);
 
                if (schema != null) {
                    AttributeSchema attrSchema =
                        schema.getAttributeSchema(attrName);
                    if (attrSchema != null) {
                        values = attrSchema.getDefaultValues();
                    }
                }
            }
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("DMModelBase.getAttrValues: attrName=" +
                    attrName + ", serviceName=" + serviceName, ssoe);
            }
        } catch (AMException ame) {
            debug.error("DMModelBase.getAttrValues: attrName=" + attrName +
                ", serviceName=" + serviceName, ame);
        } catch (SMSException smse ){
            debug.error("DMModelBase.getAttrValues: attrName=" + attrName +
                ", serviceName=" + serviceName, smse);
        }
 
        return values;
    }

    /**
     * Returns organization distinguished name of an <code>AMObject</code>
     * or <code>AMEntity</code>.
     *   
     * @param dn distinguished name of <code>AMObject</code> or
     * <code>AMEntity</code>.
     * @return organization distinguished name.
     * @throws AMException if organization distinguished name cannot be
     * obtained.
     * @throws SSOException if single sign on token has expired or invalid.
     */  
    private String getOrganizationDN(String dn)
        throws AMException, SSOException
    {
        String orgDN = null;
        AMObject obj = getAMObject(dn);

        if (obj != null) {
            orgDN = obj.getOrganizationDN();
        } else {
            AMEntity entity = getAMStoreConnection().getEntity(dn);

            if (entity != null) {
                orgDN = entity.getOrganizationDN();
            }
        }

        return (orgDN != null) ? orgDN : "";
    }

    /**
     * Returnsthe service template for a organization DN
     *   
     * @param orgDN organization DN.
     * @param service service name.
     * @param templateType type of template.
     * @return service template
     */  
    protected AMTemplate getServiceTemplate(
        String orgDN,
        String service,
        int templateType
    ) {
        AMTemplate template = null;
	boolean bOrganization = false;
        try {
            int type = getObjectType(orgDN);
	    String[] params = {orgDN, service, Integer.toString(templateType)};

            switch (type) {
            case AMObject.ORGANIZATION:
		bOrganization = true;
		logEvent(
		    "ATTEMPT_DIR_MGR_GET_SERVICE_TEMPLATE_UNDER_ORGANIZATION",
		    params);
                AMOrganization org = (AMOrganization)getAMObject(orgDN);
 
                if (org != null) {
                    if ((templateType != AMTemplate.ORGANIZATION_TEMPLATE) ||
                        org.orgTemplateExists(service)
                    ) {
                        template = org.getTemplate(service, templateType);
                    }
                }
		logEvent(
		    "SUCCEED_DIR_MGR_GET_SERVICE_TEMPLATE_UNDER_ORGANIZATION",
		    params);
                break;
            case AMObject.ORGANIZATIONAL_UNIT:
		logEvent(
		    "ATTEMPT_DIR_MGR_GET_SERVICE_TEMPLATE_UNDER_CONTAINER",
		    params);
                AMOrganizationalUnit orgUnit = (AMOrganizationalUnit)
                    getAMObject(orgDN);
 
                if (orgUnit != null) {
                    if ((templateType != AMTemplate.ORGANIZATION_TEMPLATE) ||
                        orgUnit.orgTemplateExists(service)
                    ) {
                        template = orgUnit.getTemplate(service, templateType);
                    }
                }      
		logEvent(
		    "SUCCEED_DIR_MGR_GET_SERVICE_TEMPLATE_UNDER_CONTAINER",
		    params);
                break;
            }
 
            if ((template != null) && !template.isExists()) {
                template = null;
            }
        } catch (AMException e) {
	    String msgId = (bOrganization) ?
		"AM_EXCEPTION_DIR_MGR_GET_SERVICE_TEMPLATE_UNDER_ORGANIZATION" :
		"AM_EXCEPTION_DIR_MGR_GET_SERVICE_TEMPLATE_UNDER_CONTAINER";
	    String[] paramsEx = {orgDN, service, 
		Integer.toString(templateType), getErrorString(e)};
	    logEvent(msgId, paramsEx);
            debug.error(
                "couldn't get " + service + " template for " + orgDN, e);
        } catch (SSOException e) {
	    String msgId = (bOrganization) ?
		"SSO_EXCEPTION_DIR_MGR_GET_SERVICE_TEMPLATE_UNDER_ORGANIZATION":
		"SSO_EXCEPTION_DIR_MGR_GET_SERVICE_TEMPLATE_UNDER_CONTAINER";
	    String[] paramsEx = {orgDN, service, 
		Integer.toString(templateType), getErrorString(e)};
	    logEvent(msgId, paramsEx);
            debug.error(
                "couldn't get " + service + " template for " + orgDN, e);
        }
 
        return template;
    }

    /**
     * Returns service template under current location
     *   
     * @param serviceName name of service
     * @param schemaType type of schema
     * @return service template
     * @throws AMException
     * @throws SSOException
     */  
    protected AMTemplate getServiceTemplate(
        String serviceName,
        SchemaType schemaType)
        throws AMException, SSOException
    {
        AMTemplate template =  null;

        switch (locationType) {
        case AMObject.ORGANIZATION:
            template = getServiceTemplateInOrg(serviceName, locationDN,
                schemaType);
            break;
        case AMObject.ORGANIZATIONAL_UNIT:
            template = getServiceTemplateInOrgUnit(serviceName, locationDN,
                schemaType);
            break;
        case AMObject.ROLE:
        case AMObject.FILTERED_ROLE:
        case AMObject.MANAGED_ROLE:
            AMRole role = getAMStoreConnection().getRole(locationDN);
            template = role.getTemplate(serviceName,
                AMTemplate.DYNAMIC_TEMPLATE);
            break;
        }
 
        return template;
    }

    private AMTemplate getServiceTemplateInOrg(
        String serviceName,
        String orgDN,
        SchemaType schemaType) 
        throws AMException, SSOException
    {
        AMTemplate template = null;
        AMOrganization org = getAMStoreConnection().getOrganization(orgDN);
 
        if (schemaType.equals(SchemaType.ORGANIZATION)) {
            if (org.orgTemplateExists(serviceName)) {
                template = org.getTemplate(serviceName,
                    AMTemplate.ORGANIZATION_TEMPLATE);
            }
        } else {
            template = org.getTemplate(serviceName,
                AMTemplate.DYNAMIC_TEMPLATE);
        }
 
        return template;
    }
 
    private AMTemplate getServiceTemplateInOrgUnit(
        String serviceName,
        String orgUnitDN,
        SchemaType schemaType)
        throws AMException, SSOException
    {
        AMTemplate template = null;
        AMOrganizationalUnit orgUnit =
            getAMStoreConnection().getOrganizationalUnit(orgUnitDN);
 
        if (schemaType.equals(SchemaType.ORGANIZATION)) {
            if (orgUnit.orgTemplateExists(serviceName)) {
                template = orgUnit.getTemplate(serviceName,
                    AMTemplate.ORGANIZATION_TEMPLATE);
            }
        } else {
            template = orgUnit.getTemplate(
                serviceName, AMTemplate.DYNAMIC_TEMPLATE);
        }
        return template;
    }    


    protected String getMultiValue(Set attrValues) {
        // Handle multi-valued attributes
        StringBuilder sb = new StringBuilder(25);
        if (attrValues != null && !attrValues.isEmpty()) {
            Iterator iter = attrValues.iterator();
            while (iter.hasNext()) {
                 if (sb.length() != 0) {
                     sb.append(getLocalizedString("multiValuedAttrSeparator")).append(" ");
                 }
                 sb.append((String)iter.next());
            }
        }
        return sb.toString();
    }

    /**
     * Sets the search control limits (maximum number of items and timeout)
     * The values of these limits come from the administration
     * service where the user logged in. If the service is not registered 
     * in the authenticating org, the value comes from the global config.
     *
     * @param searchControl search control object.
     */
    protected void setSearchControlLimits(AMSearchControl searchControl) {
	searchControl.setMaxResults(getSearchResultLimit());
        searchControl.setTimeOut(getSearchTimeOutLimit());
    }

    protected AMTemplate getOrgTemplate(AMObject obj) {
        AMTemplate amTemplate = null;
        if (obj instanceof AMOrganization) {
            amTemplate = getOrgTemplate((AMOrganization)obj);
        } else if (obj instanceof AMOrganizationalUnit) {
            amTemplate = getOrgTemplate((AMOrganizationalUnit)obj);
        } else if (debug.warningEnabled()) {
	    debug.warning("DMModelBase.getOrgTemplate, invalid object type "
		+ obj.getDN());
        }
        return amTemplate;
    }

    protected AMTemplate getOrgTemplate(String orgDN) {
        AMTemplate template = null;    

        try {
            int type = getObjectType(orgDN);
            if (type == AMObject.ORGANIZATION) {
                template = getOrgTemplate(
                    getAMStoreConnection().getOrganization(orgDN));
            } else if (type == AMObject.ORGANIZATIONAL_UNIT) {
                template = getOrgTemplate(
                    getAMStoreConnection().getOrganizationalUnit(orgDN));
            }
        } catch (SSOException ssoe) {
            debug.warning("DMModelBase.getOrgTemplate", ssoe);
        }
        return template;
    }

    protected AMTemplate getOrgTemplate(AMOrganization org) {
        AMTemplate template = null;

        if (org != null) {
            try {
                if (org.orgTemplateExists(ADMIN_CONSOLE_SERVICE)) {
                    template = org.getTemplate(ADMIN_CONSOLE_SERVICE,
                        AMTemplate.ORGANIZATION_TEMPLATE); }
            } catch (AMException ame) {
                debug.warning("DMModel.getOrgTemplate", ame);
            } catch (SSOException ssoe) {
                debug.warning("DMModel.getOrgTemplate", ssoe);
            }
        }

        return template;
    }

    protected AMTemplate getOrgTemplate(AMOrganizationalUnit ou) {
        AMTemplate template = null;

        if (ou != null) {
            try {
                if (ou.orgTemplateExists(ADMIN_CONSOLE_SERVICE)) {
                    template = ou.getTemplate(ADMIN_CONSOLE_SERVICE,
                        AMTemplate.ORGANIZATION_TEMPLATE);
                }
            } catch (AMException ame) {
                debug.warning("DMModelBase.getOrgTemplate", ame);
            } catch (SSOException ssoe) {
                debug.warning("DMModelBase.getOrgTemplate", ssoe);
            }
        }

        return template;
    }

    /**
     * Returns a list of attribute names that should be displayed for the
     * specified type of object. In the Administration service there is an
     * attribute which specifies the attributes to return on a search. We 
     * have overloaded this attribute to actually specify which attributes
     * should be displayed in the console navigation pages. For example,
     * the default attributes listed in this attribute are for user entries.
     * To specifiy attributes displayed in the Role page, you would add the 
     * following ROLES=iplanet-role-description
     *
     * @param returnAttr string from which to build the list.
     * @param type object view type.
     * @return list of the search return attribute.
     */
    protected List getObjectDisplayList(String returnAttr, String type) {
        StringTokenizer st = new StringTokenizer(returnAttr, "|");
        List list = Collections.EMPTY_LIST;
        while (st.hasMoreTokens()) {
            String str = st.nextToken();
            int index = str.indexOf("=");
            if (index != -1) {
                String name = str.substring(0,index).trim();
                if (name.equals(type)) {
                    str = str.substring(index+1, str.length());
                    list = getReturnAttrList(str);
                    break;
                }
            } else if (index == -1 && type.equals(USERS)) {
                list = getReturnAttrList(str);
                break;
            }
        }
        return list;
    }

    /**
     * Returns true if administration group is enabled
     *
     * @return true if administration group is enabled
     */
    public boolean isAdminGroupsEnabled() {
        return adminGroupsEnabled;
    }

    private List getReturnAttrList(String str) {
        List list = Collections.EMPTY_LIST;
        StringTokenizer subSt = new StringTokenizer(str);
        int n = subSt.countTokens();
        if (n > 0) {
           list = new ArrayList(n);
        }
        while (subSt.hasMoreTokens()) {
            String value = subSt.nextToken().trim();
            if (value.length() > 0) {
                list.add(value);
            }
        }
        return list;
    }    
    
    /**
     * Attempts to remove the specified set of <code>AMObjects</code> 
     * like Organizations, Users, Roles, etc...
     *
     * @param dnSet a Set containing the DNs of the <code>AMObjects</code> that
     *        should be deleted
     * @return map of DNs and error messages of objects that could not be
     * deleted 
     * @throws AMConsoleException if deletion is blocked by listener
     */    
    public Map deleteObject(Set dnSet)
	throws AMConsoleException
    {
	if (dnSet == null || dnSet.isEmpty()) {
	    throw new AMConsoleException(
                getLocalizedString("no.entries.selected"));
	}

	int size = dnSet.size();
	Map failedObjectMap = new HashMap(size);
	Set deleted = new HashSet(size);

	for (Iterator iter = dnSet.iterator(); iter.hasNext(); ) {
	    /*
	    * try and retrieve the object from the store. if it can't be
	    * found, we can assume that it has already been removed by 
	    * another user and skip the object.
	    */
            AMObject dpObj = null;
            String deleteObj = (String)iter.next();
            if (deleteObj.equals(getStartDSDN())) {
                debug.warning("DModelBase.deleteUMObject - skipping entry "+
                    "tried to remove the users starting location");
                continue;
            }
            try {		
                dpObj = getAMObject(deleteObj);
                if (dpObj == null) {
                    if (debug.warningEnabled()) {
                        debug.warning("skipping " + deleteObj + 
			    ", no object found for it.");
		    }
                    continue;
                }
            } catch (AMException ame) {
                if (debug.messageEnabled()) {
                    debug.message( 
                        "skipping " + deleteObj + ", no object found for it.");
                }
                continue;
            } catch (SSOException ssoe) {
                debug.error("token exception", ssoe);
                continue;
            }
            
	    /*
	    * found the object, now try and delete it. extra check by calling
	    * isExists() here. If there is an error from sdk during delete
	    * store the dn for use in the error message.
	    */
            try {
                if (dpObj.isExists())  {
		    String[] param = {deleteObj};
		    logEvent("ATTEMPT_DIR_MGR_DELETE_DIR_OBJECT", param);
                    dpObj.delete(true);
		    logEvent("SUCCEED_DIR_MGR_DELETE_DIR_OBJECT", param);
                    deleted.add(deleteObj);
                } else {
                    if (debug.warningEnabled()) {
                        debug.warning("skipping "+ deleteObj +
                        ", doesn't exist in directory");
                    }
                }		
            } catch (AMException dpe) {
                errorMessage = getErrorString(dpe);
		String[] paramsEx = {deleteObj, errorMessage};
		logEvent("AM_EXCEPTION_DIR_MGR_DELETE_DIR_OBJECT", paramsEx);
                failedObjectMap.put(deleteObj, errorMessage);
                if (debug.warningEnabled()) {
                    debug.warning("Failed to delete AMObject: " + deleteObj);
                    debug.warning("DModelBase.deleteOrgs:", dpe);
                }
            } catch (SSOException ssoe) {
		String[] paramsEx = {deleteObj, getErrorString(ssoe)};
		logEvent("SSO_EXCEPTION_DIR_MGR_DELETE_DIR_OBJECT", paramsEx);
                if (debug.warningEnabled()) {
                    debug.warning("Failed to delete AMObject: " + deleteObj);
                    debug.warning("DModelBase.deleteOrgs", ssoe);
                }
            }
        }

	return failedObjectMap;
    }  

    private void addEntry(String type, List entries) {
        Map tab = new HashMap(10);
        tab.put("label", "dm." + type + ".label");
        tab.put("status", "dm." + type + ".status");
        tab.put("tooltip", "dm." + type + ".tooltip");
        tab.put("url", "../dm/" + type);
        tab.put("viewbean", "com.sun.identity.console.dm." + type + "ViewBean");
	tab.put("permissions", "sunAMRealmService");
        entries.add(tab);
    }

    public List getTabMenu() {
        List entries = new ArrayList(15);

        Map tab = new HashMap(10);
        
        if (showOrganizations()) {
            addEntry("Organization", entries);
        }

        if (showOrgUnits()) {
            addEntry("Container", entries);
        }

        if (showGroupContainers()) {
            addEntry("GroupContainer", entries);
        }

        if (showGroups()) {
            addEntry("Group", entries);
        }

        if (showPeopleContainers()) {
            addEntry("PeopleContainer", entries);
        }

        if (userMgmtEnabled) {
            addEntry("User", entries);
        }

        if (showRoles()) {
            addEntry("Role", entries);
        }
        return entries;
    }

    /**
     * Compares the old attribute value with the new value. If the values are
     * different, the new value is stored in a map with the attribute name as
     * the key. The methods <code>AMObject::setAttribute</code> and
     * <code>AMObject::store</code> are called to update the profile if the map
     * contains any changed values.
     *
     * @param amObj altering <code>AMObject</code>.
     * @param data map containing the data.
     * @throws AMConsoleException
     * @throws SSOException.
     */
    public void writeProfile(AMObject amObj, Map data)
	throws AMConsoleException, SSOException
    {
            writeProfile(amObj, data, false);
    }

    /**
     * Compares the old attribute value with the new value. If the values are
     * different, the new value is stored in a map with the attribute name as
     * the key. The methods <code>AMObject::setAttribute</code> and
     * <code>AMObject::store</code> are called to update the profile if the map
     * contains any changed values.
     *
     * @param amObj altering <code>AMObject</code>.
     * @param data map containing the data.
     * @param overwrite true overwrites the value without comparing the
     *        old value and the new value.
     * @throws AMConsoleException
     * @throws SSOException.
     */
    public void writeProfile(AMObject amObj, Map data, boolean overwrite)
        throws AMConsoleException, SSOException
    {
        List errors = new ArrayList(data.size());
    
        /*
        * construct a set of attribute names to retrieve from the user 
        * entry. doing this because password and encrypted passwords 
        * have modified attribute names. We need to restore them to the
        * real attribute name.
        */
        Set keys = new HashSet();
        for (Iterator i=data.keySet().iterator(); i.hasNext();) {
            String key = (String)i.next();
            if (key.startsWith(ENCRYPTED)) {
                key = key.substring(ENCRYPTED.length());
            } else if (key.startsWith(PASSWORD)) {
                key = key.substring(PASSWORD.length());
            }
            keys.add(key);
        }
    
        /*
        * retrieve all the values being set in one call instead of calling
        * getAttribute for each attribute
        */
        Map storedValues = null;
        try {
            storedValues = amObj.getAttributes(keys);
        } catch (AMException ame) {
            if (debug.warningEnabled()) {
                debug.warning("DMModelBase.writeProfile, ", ame);
            }
        }
        if (storedValues == null) {
            storedValues = Collections.EMPTY_MAP;
        }
    
        /*
        * loop through the attributes and see if the values have changed.
        * only write the attributes which were modified.
        */
        for (Iterator i = data.keySet().iterator(); i.hasNext();) {
            String key = (String)i.next();
            Set newValue = (Set)data.get(key);
    
            boolean encrypted = false;
            boolean plainpassword = false;
            if (key.startsWith(ENCRYPTED)) {
                encrypted = true;
                key = key.substring(ENCRYPTED.length());
            } else if (key.startsWith(PASSWORD)) {
                plainpassword = true;
                key = key.substring(PASSWORD.length());
            }
    
            Set oldValue = (Set)storedValues.get(key);
            // Cannot set an attribute to an empty set if it doesn't exist.
            if (oldValue.isEmpty() && newValue.isEmpty()) {
                continue;
            }
            if (overwrite || !newValue.equals(oldValue)) {
                String error = writeAttrValues(
                    amObj, key, newValue, oldValue, encrypted, plainpassword);
    
                if ((error != null) && (error.length() > 0)) {
                    errors.add(error);
                }
            }
        }
    
        if (errors.size() > 0) {
            throw new AMConsoleException(errors);
        }
    }

    /**
     * Writes attribute. Since we do not know attribute is written or not
     * we have handle exception accordingly.
     *
     * @param amObj access management object
     * @param name of attribute
     * @param values of attribute
     * @param buffer string buffer to store error message
     * @return error message, if any
     * @throws SSOException
     */
    private String writeAttrValues(
        AMObject amObj, 
        String name, 
        Set values,
        Set oldValues,
        boolean encrypted,
        boolean plainpassword
    ) throws SSOException {
        String error = null;

        if (encrypted) {
	    values = encryptString(values);
        }

        try {
	    String[] param = {amObj.getDN()};
	    logEvent("ATTEMPT_DIR_MGR_MODIFY_DIR_OBJECT", param);

            Map mapAttrVal = new HashMap(1);
            mapAttrVal.put(name, values);
            amObj.setAttributes(mapAttrVal);
            amObj.store();

	    logEvent("SUCCEED_DIR_MGR_MODIFY_DIR_OBJECT", param);
        } catch (AMException ame) {
	    String[] paramsEx = {amObj.getDN(), getErrorString(ame)};
	    logEvent("AM_EXCEPTION_DIR_MGR_MODIFY_DIR_OBJECT", paramsEx);
            if (debug.warningEnabled()) {
                debug.warning("error setting attribute " + name, ame);
            }
            error = name + "-" + getErrorString(ame);
        }

        return error;
    }

    protected void getPropertyXML(
        String serviceName,
        String subSchemaName,
        SchemaType type,
        StringBuffer buff
    ) {
        try {                                                      
            ServiceSchema sub = getSubSchema(serviceName, type, subSchemaName);
	    if (sub != null) {
                PropertyXMLBuilder xmlBuilder =
                    new PropertyXMLBuilder(sub, this);
                buff.append(xmlBuilder.getXML(false));
		setMandatoryAttributes(xmlBuilder.getAttributeSchemas());
	    }
        } catch (SSOException e) {
            debug.error("DMModeBase.getPropertyXML-SSO", e);
        } catch (SMSException e) {
            debug.error("DMModeBase.getPropertyXML-SMS", e);
        } catch (AMConsoleException e) {
            debug.error("DMModeBase.getPropertyXML-AMConsole", e);
        }
    }

    protected ServiceSchema getServiceSchema(
        String serviceName, 
	SchemaType type
    ) throws SMSException, SSOException 
    {
        ServiceSchemaManager manager = getServiceSchemaManager(serviceName);
        ServiceSchema ss = manager.getSchema(type);
        return ss;
    }

    protected ServiceSchema getSubSchema(
        String serviceName, 
        SchemaType type, 
        String subschema
    ) throws SMSException, SSOException 
    {
        ServiceSchema service = getServiceSchema(serviceName, type);
        ServiceSchema tmp = service.getSubSchema(subschema);
        return tmp;
    }

    /**
     * Removes the specified services from this object.
     *
     * @param location name of current organization.
     * @param services of services to remove from the organization.
     */
    public void removeServices(String location, Set services) 
        throws AMConsoleException
    {
        if (services == null || services.isEmpty()) {
            throw new AMConsoleException("empty.service.list");
        }
	if (location == null || location.length() == 0) {
            throw new AMConsoleException("system.error");
	}

        Set currentServices = null;
        AMOrganization org = null;
        AMOrganizationalUnit orgUnit = null;
        int locationType = getObjectType(location);
        try {
            if (locationType == AMObject.ORGANIZATION) {
                org = getAMStoreConnection().getOrganization(location);
                currentServices = orgUnit.getRegisteredServiceNames();
            } else {  
                orgUnit = getAMStoreConnection().getOrganizationalUnit(
                    location);
                currentServices = orgUnit.getRegisteredServiceNames();
            }
        } catch (SSOException ssoe) {
            debug.warning("DMModelBase.removeServices", ssoe);
        } catch (AMException ame) {
            debug.warning("DMModelBase.removeServices", ame);
        }

        for (Iterator i = services.iterator(); i.hasNext();) {
	    boolean bOrganization = false;
            String name = (String)i.next();

            try {
                if (!currentServices.contains(name)) {
                    if (debug.messageEnabled()) {
                        debug.message("DMModelBase.removeServies");
                        debug.message("skipping " + name +
                            ", not a registerd service.");
                    }
                    continue;
                }

		bOrganization = (locationType == AMObject.ORGANIZATION);
		String[] params = {location, name};

                if (bOrganization) {
		    logEvent("ATTEMPT_DIR_MGR_REMOVE_SERIVCE_FROM_ORGANIZATION",
			params);
                    org.unregisterService(name);
		    logEvent("SUCCEED_DIR_MGR_REMOVE_SERIVCE_FROM_ORGANIZATION",
			params);
                } else {
		    logEvent("ATTEMPT_DIR_MGR_REMOVE_SERIVCE_FROM_CONTAINER",
			params);
                    orgUnit.unregisterService(name);
		    logEvent("SUCCEED_DIR_MGR_REMOVE_SERIVCE_FROM_CONTAINER",
			params);
                }
            } catch (SSOException ssoe) {
		String msgId = (bOrganization) ?
		    "SSO_EXCEPTION_DIR_MGR_REMOVE_SERIVCE_FROM_ORGANIZATION" :
		    "SSO_EXCEPTION_DIR_MGR_REMOVE_SERIVCE_FROM_CONTAINER";
		String[] paramsEx = {location, name, getErrorString(ssoe)};
		logEvent(msgId, paramsEx);
                debug.warning("DMModelBase.removeServices", ssoe);
            } catch (AMException ame) {
		String msgId = (bOrganization) ?
		    "AM_EXCEPTION_DIR_MGR_REMOVE_SERIVCE_FROM_ORGANIZATION" :
		    "AM_EXCEPTION_DIR_MGR_REMOVE_SERIVCE_FROM_CONTAINER";
		String[] paramsEx = {location, name, getErrorString(ame)};
		logEvent(msgId, paramsEx);
                debug.warning("DMModelBase.removeServices", ame);
            }
        }
    }

    protected Set getSchemaAttributes(String serviceName, SchemaType type)
        throws SSOException, SMSException
     {
        Set attributes = null;
        try {
            ServiceSchemaManager manager =
                getServiceSchemaManager(serviceName);
            ServiceSchema sub = manager.getSchema(type);
            attributes = sub.getAttributeSchemas();
        } catch (SSOException e) {
            debug.error("DMModelBase.getSchemaAttributes", e);
        } catch (SMSException e) {
            debug.error("DMModelBase.getSchemaAttributes", e);
        }
        return (attributes != null) ? attributes : Collections.EMPTY_SET;
    }

    /**
     * Create the start of the filter subsection. This includes the 
     * Match dropdown tag to select either All, or Any of the attributes
     * in the filter.
     */
    private String getFilterSectionXML() {
        StringBuilder xml = new StringBuilder(1000);

        xml.append("<property>")
           .append("<label name=\"LogicalOperatorLabel\" ")
           .append("defaultValue=\"search.user.logical.operator\" ") 
           .append("labelFor=\"").append(ATTR_NAME_LOGICAL_OPERATOR).append("\" />")
           .append("<cc name=\"").append(ATTR_NAME_LOGICAL_OPERATOR).append("\" tagclass=")
           .append("\"com.sun.web.ui.taglib.html.CCDropDownMenuTag\">")
           .append("<option label=\"").append(getLogicalOrOpLabel()).append("\" value=\"")
           .append(getLogicalOrOpValue()).append("\" /><option label=\"")
           .append(getLogicalAndOpLabel()).append("\" value=\"").append(getLogicalAndOpValue())
           .append("\" /></cc><cc name=\"matchLabel\" tagclass=\"")
           .append("com.sun.web.ui.taglib.html.CCStaticTextFieldTag\">")
           .append("<attribute name=\"defaultValue\" ")
           .append("value=\"search.user.logical.operator.label\" /></cc>")
           .append("</property>");

        return xml.toString();
    }

    /**
     * Creates a filter section which can be inserted into an existing
     * property sheet. The attributes used are those from the user schema, 
     * whichever are marked as 'filter' type attributes. The filter 
     * attributes should not be required which means we need to scan the 
     * generated xml for the required indicator and remove it (the 
     * requied indicator). 
     */
    private String processFilterSection(StringBuffer xml) {       
        String SECTION_NAME = "<section name";
        String REQUIRED_PROPERTY = "<property required=\"true\">";

        /*
         * remove the <section name="xxx"> portion by replacing it
         * with the filterSectionXML. 
         */
        int start = xml.toString().indexOf(SECTION_NAME);
        int end = xml.toString().indexOf('>', start);
        xml.replace(start, end + 1, getFilterSectionXML());

        // locate the </section> ending tag and remove it
        start = xml.toString().lastIndexOf(PropertyTemplate.SECTION_END_TAG);
        int length = PropertyTemplate.SECTION_END_TAG.length();
        xml.delete(start, start + length);

        /*
         * replace <property required="true"> with <property> so that 
         * the fields are not marked as required in the filter.
         */
        length = REQUIRED_PROPERTY.length();
        int idx = xml.toString().indexOf(REQUIRED_PROPERTY);
        while (idx != -1) {
            xml.replace(
                idx, idx+length, PropertyTemplate.PROPERTY_START_TAG);
            idx = xml.toString().indexOf(REQUIRED_PROPERTY);
        }

        return xml.toString(); 
    }

    /**
     * Returns the property sheet string of XML that will generate the 
     * filter UI section.
     *
     * @return filter section XML string.
     */
    protected String getFilterAttributesXML() {
        if (filterAttributeXML == null) {
            StringBuffer buff = new StringBuffer(500);
            try {
                PropertyXMLBuilder xmlBuilder =  new PropertyXMLBuilder(
                    AMAdminConstants.USER_SERVICE, this, getFilterAttributes());
                buff.append(xmlBuilder.getXML(false));
                filterAttributeXML = processFilterSection(buff);
            } catch (SSOException e) {
                debug.error("DMModelBase.getFilterAttributesXML", e);
            } catch (SMSException e) {
                debug.error("DMModelBase.getFilterAttributesXML", e);
            } catch (AMConsoleException e) {
                debug.error("DMModelBase.getFilterAttributesXML", e);
            }
        }
        return filterAttributeXML;
    }   
    
    /**
     * Returns a set of attribute schemas from the user service, which are
     * defined as 'filter' attributes.
     *
     * @return set of user attributes.
     */
    protected Set getFilterAttributes() {
	if (filterAttributes == null) {
            filterAttributes = new HashSet();
            try {
                filterAttributes = getSchemaAttributes
                    (AMAdminConstants.USER_SERVICE, SchemaType.USER);
                String[] show = {"filter"};
                PropertyXMLBuilder.filterAttributes(filterAttributes, show);
            } catch (SSOException e) {
                debug.error("DMModelBase.getFilterAttributes", e);
            } catch (SMSException e) {
                debug.error("DMModelBase.getFilterAttributes", e);
    	    }
        }
        return filterAttributes;
    }

    /**
     * Returns a set of attribute names from the user service, which are
     * defined as 'filter' attributes.
     *
     * @return set of user attributes.
     */
    protected Set getFilterAttributeNames() {
        if (filterAttributeNames == null) {
            filterAttributeNames = new HashSet();
            for (Iterator i= getFilterAttributes().iterator(); i.hasNext();) {
                AttributeSchema as = (AttributeSchema)i.next();
                filterAttributeNames.add(as.getName());
            }
	    filterAttributeNames.add(ATTR_NAME_LOGICAL_OPERATOR);
        }
        return filterAttributeNames;
    }

    protected String generateFilter(Map attributes) {
        StringBuilder filter = new StringBuilder(512);

        for (Iterator iter = attributes.keySet().iterator(); iter.hasNext(); ) {
            AttributeSchema as = (AttributeSchema)iter.next();
            String key = as.getName();
            Set value = (Set)attributes.remove(key);
            if (value == null || value.isEmpty()) {
                if (key.equals(USER_SERVICE_UID)) {
                    filter.append("(").append(key).append("=*)");
                }
                continue;
            }
            Iterator valIter = value.iterator();
            String val = (String)valIter.next();
            if (key.equalsIgnoreCase(USER_SERVICE_ACTIVE_STATUS)) {
                if (val.equalsIgnoreCase(STRING_ACTIVE)) {
                    filter.append("(|(")
                           .append(USER_SERVICE_ACTIVE_STATUS)
                           .append("=active)(!(")
                           .append(USER_SERVICE_ACTIVE_STATUS)
                           .append("=*)))");
                } else {
                    filter.append("(")
                           .append(USER_SERVICE_ACTIVE_STATUS)
                           .append("=")
                           .append(val)
                           .append(")");
                }
            } else {
                if (val.length() > 0) {
                    filter.append("(")
                        .append(key)
                        .append("=")
                        .append(val)
                        .append(")");
                } else if (key.equals(USER_SERVICE_UID)) {
                    filter.append("(" + USER_SERVICE_UID + "=*)");
                }
    
            }
        }
            
        return filter.toString();
    }

    protected String getParentDN(String dn)
        throws AMException, SSOException 
    {
        AMObject obj = getAMObject(dn);
        return obj.getOrganizationDN();
    }

    protected boolean hasDisplayedAttributes(String service, SchemaType type) {
        ServiceSchema schema = null;

        try {
            schema = getServiceSchema(service, type);
        } catch (SMSException e) {
            debug.warning("DMModelBase.hasDisplayedAttributes", e);
        } catch (SSOException e) {
            debug.warning("DMModelBase.hasDisplayedAttributes", e);
        }

        boolean display = false;
        if (schema != null) {
            Set as = schema.getAttributeSchemas();
            if (as != null && !as.isEmpty()) {
                for (Iterator i = as.iterator(); i.hasNext() && !display; ) {
                    if (isDisplayed((AttributeSchema)i.next())) {
                        display = true;
                    }
                }
            }
        }
        return display;
    }

    /**
     * Returns Universal Id of a given Distinguished Name.
     *
     * @param dn Distinguished Name.
     */
    public String getUniversalId(String dn) {
	String universalId = "";

	try {
	    AMObject obj = getAMObject(dn);
	    String orgDN = obj.getOrganizationDN();
	    AMIdentity amid = IdUtils.getIdentity(getUserSSOToken(), dn, orgDN);
	    universalId = IdUtils.getUniversalId(amid);
	} catch (AMException e) {
	    debug.error("DMModelBase.getUniversalId", e);
	} catch (SSOException e) {
	    debug.error("DMModelBase.getUniversalId", e);
	} catch (IdRepoException e) {
	    debug.error("DMModelBase.getUniversalId", e);
	}

	return universalId;
    }

    /**
     * Returns label for logical OR operator
     *
     * @return label for logical OR operator
     */
    private String getLogicalOrOpLabel() {
        return getLocalizedString("logicalOR.label");
    }

    /**
     * Returns value for logical OR operator
     *
     * @return value for logical OR operator
     */
    private String getLogicalOrOpValue() {
        return STRING_LOGICAL_OR;
    }

    /**
     * Returns label for logical operator text
     *
     * @return label for logical operator text
     */
    private String getLogicalAndOpValue() {
        return STRING_LOGICAL_AND;
    }

    /**
     * Returns label for logical AND operator
     *
     * @return label for logical AND operator
     */
    private String getLogicalAndOpLabel() {
        return getLocalizedString("logicalAND.label");
    }

    /**
     * getXML() will create an xml string which contains the starting
     * and ending section tags. We need to remove this so we can
     * insert this xml into another section.
     */
    protected String removeSectionTags(String xml) {
        if (xml.length() > 0) {
            // remove the starting section tag so we are only left with the
            // attributes and the closing section tag
            int idx = xml.indexOf(">");
            xml = xml.substring(idx+1);

            // now remove ending section tag
            idx = xml.lastIndexOf(PropertyTemplate.SECTION_END_TAG);
            if (idx != -1) {
                xml = xml.substring(0,idx);
            }
        }
        return xml;
    }

    protected AMGroup getAMGroup(String dn) {
        AMGroup group = null;
        int locType = getObjectType(dn);
        switch (locType) {
        case AMObject.DYNAMIC_GROUP:
            group = getDynamicGroup(dn);
            break;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            group = getAssignableDynamicGroup(dn);
            break;
        case AMObject.GROUP:
        case AMObject.STATIC_GROUP:
            group = getStaticGroup(dn);
            break;
        }

        return group;
    }

    protected AMGroup getDynamicGroup(String dn) {
        AMGroup group = null;

        try {
            group = getAMStoreConnection().getDynamicGroup(dn);
        } catch (SSOException ssoe) {
            debug.warning("DMModel.getDynamicGroup", ssoe);
        }

        return group;
    }

    protected AMGroup getAssignableDynamicGroup(String dn) {
        AMGroup group = null;

        try {
            group = getAMStoreConnection().getAssignableDynamicGroup(dn);
        } catch (SSOException ssoe) {
            debug.warning("DMModel.getAssignableDynamicGroup",
                ssoe);
        }

        return group;
    }

    protected AMGroup getStaticGroup(String dn) {
        AMGroup group = null;

        try {
            group = getAMStoreConnection().getStaticGroup(dn);
        } catch (SSOException ssoe) {
            debug.warning("DMModel.getStaticGroup", ssoe);
        }

        return group;
    }

    protected void setMandatoryAttributes(Set attributeSchemas) {
	for (Iterator i = attributeSchemas.iterator(); i.hasNext(); ) {
	    AttributeSchema as = (AttributeSchema)i.next();
	    String any = as.getAny();
	    if (PropertyXMLBuilderBase.hasAnyAttribute(as.getAny(),
		PropertyTemplate.ANY_REQUIRED)) {
		mandatoryAttributes.put(as.getName(), as);
	    }
	}
    }

    protected void validateRequiredAttributes(Map data)
	throws AMConsoleException
    {
	for (Iterator i = data.keySet().iterator(); i.hasNext(); ) {
	    String attrName = (String)i.next();
	    AttributeSchema as = (AttributeSchema)mandatoryAttributes.get(
		attrName);
	    if (as != null) {
		if (!reqValidator.validate((Set)data.get(attrName))) {
		    String serviceName = as.getServiceSchema().getServiceName();
		    String expMsg = getLocalizedString("entity-values-missing");
		    ResourceBundle rb = getServiceResourceBundle(serviceName);
		    String[] arg = {
			Locale.getString(rb, as.getI18NKey(), debug)};
		    throw new AMConsoleException(MessageFormat.format(
			expMsg, (Object[])arg));
		}
	    }
	}
    }

    /**
     * Returns the value of the Search Return Attribute found
     * in the Administration service. This attribute controls what attributes
     * get displayed in the views displaying the user entries. The value 
     * returned comes from the Administration service where the user 
     * authenticated. If the service is not defined at the login organization,
     * the value is retrieved from the Global Configuration settings.
     *
     * If the value in the attribute is not a valid user attribute, the 
     * user naming attribute will be returned.
     */
    protected String getSearchReturnValue() {
        String searchAttr = null;
        try {
            AMOrganization orgObject = 
                (AMOrganization)getAMObject(getStartDSDN());

            searchAttr = getSearchReturnValue(orgObject);
        } catch (SSOException ssoe) {
            debug.warning(
                "DMModelBase.getSearchReturnValue", ssoe);
        } catch (AMException dpe) {
            debug.warning(
                "DMModelBase.getSearchReturnValue", dpe);
        }

        if (searchAttr == null || searchAttr.length() == 0) {
            searchAttr = AdminInterfaceUtils.getNamingAttribute(
                AMObject.USER, debug);
        }

        return searchAttr;
    }

    /**
     * Returns the value set in the Search Return Attribute. First attempt
     * is in the organization. If the service is not registered then the 
     * value is retrieved from the Global Configuration.
     */
    private String getSearchReturnValue(AMObject obj) {
        String searchAttr = null;
    
        try {
            AMTemplate amTemplate = getOrgTemplate(obj);
            if (amTemplate != null) {
                searchAttr = getStringAttribute(amTemplate,
                    CONSOLE_USER_SEARCH_RETURN_KEY);
            } else {
                ServiceSchemaManager mgr = getServiceSchemaManager(
                    ADMIN_CONSOLE_SERVICE);
    
                if (mgr != null) {
                    searchAttr = getStringAttribute(
                       mgr,
                       SchemaType.ORGANIZATION,
                       CONSOLE_USER_SEARCH_RETURN_KEY);
                }
            }
        } catch (SMSException smse) {
            debug.warning(
                "DMModelBase.getUserSearchReturnAttribute", smse);
        } catch (SSOException ssoe) {
            debug.warning(
                "DMModelBase.getUserSearchReturnAttribute", ssoe);
        } catch (AMException dpe) {
            debug.warning(
                "DMModelBase.getUserSearchReturnAttribute", dpe);
        }
         
        return searchAttr;           
    }

    /**
     * Validates the attribute names in the <code>value</code> are actually
     * attributes from the <code>User</code> service. Names that are not
     * valid are discarded. A new <code>String</code> will be constructed 
     * which contains the valid attribute names. If no valid names are found
     * the user naming attribute will be returned as a default.
     *
     * @param value containing user attribute names.
     * @return validated user attribute names.
     */
    protected String getValidUserAttributes(String value) {
        StringBuilder userAttributes = new StringBuilder(16);
        if ((value != null) && (value.length() > 0)) {
            StringTokenizer tokenizer = new StringTokenizer(value);
            try {
                ServiceSchemaManager mgr =
                    getServiceSchemaManager(AMAdminConstants.USER_SERVICE);
                ServiceSchema schema = mgr.getSchema(SchemaType.USER);
                Set attributeNames = schema.getAttributeSchemaNames();
    
                while (tokenizer.hasMoreTokens()) {
                    String tmp = (String)tokenizer.nextToken();
                    if (attributeNames.contains(tmp)) {
                        userAttributes.append(" ").append(tmp);
                    }
                }
            } catch (SSOException s) {
                if (debug.warningEnabled()) {
                    debug.warning("DMModel.getValidUserAttributes",s);
                }
            } catch (SMSException se) {
                if (debug.warningEnabled()) {
                    debug.warning("DMModel.getValidUserAttributes",se);
                }
            }
        }
    
        return (userAttributes.length() > 0) ? 
            userAttributes.toString() : 
            AdminInterfaceUtils.getNamingAttribute(AMObject.USER, debug);
    }

    /**
     * Returns true if the current user can create an organization at
     * the specified location.
     * 
     * @param location where the organization will be created.
     * @return true if the organization can be created.
     */
    public boolean createOrganization(String location) {
        int type = getObjectType(location);
        return ((type == AMObject.ORGANIZATION) ||
            (type == AMObject.ORGANIZATIONAL_UNIT));
    }  

    /**
     * Returns true if the current user can create an organization unit at
     * the specified location.
     * 
     * @param location where the organization unit will be created.
     * @return true if the organization unit can be created.
     */
    public boolean createOrganizationUnit(String location) {
        int type = getObjectType(location);
        return ((type == AMObject.ORGANIZATION) ||
            (type == AMObject.ORGANIZATIONAL_UNIT));
    }  

    /**
     * Returns true if the current user can create a group container at
     * the specified location.
     * 
     * @param location where group container will be created.
     * @return true if the group container can be created.
     */
    public boolean createGroupContainer(String location) {
        int type = getObjectType(location);
        return ((type == AMObject.ORGANIZATION) ||
            (type == AMObject.ORGANIZATIONAL_UNIT) ||
            (type == AMObject.GROUP_CONTAINER));
    }  

    /**
     * Returns true if the current user can create a group at
     * the specified location.
     * 
     * @param location where the group will be created.
     * @return true if the group can be created.
     */
    public boolean createGroup(String location) {
        boolean create = true;
        int type = getObjectType(location);

        if ((groupContainerDisplay &&
            (type != AMObject.GROUP_CONTAINER) &&
            (type != AMObject.GROUP) &&
            (type != AMObject.DYNAMIC_GROUP) &&
            (type != AMObject.STATIC_GROUP) &&
            (type != AMObject.ASSIGNABLE_DYNAMIC_GROUP)) ||

            (!groupContainerDisplay &&
            (type != AMObject.ORGANIZATION) &&
            (type != AMObject.ORGANIZATIONAL_UNIT) &&
            (type != AMObject.GROUP) &&
            (type != AMObject.DYNAMIC_GROUP) &&
            (type != AMObject.STATIC_GROUP) &&
            (type != AMObject.ASSIGNABLE_DYNAMIC_GROUP)))
        {
            create = false;
        }
        return create;
    }  

    /**
     * Returns true if the current user can create a role at
     * the specified location.
     * 
     * @param location where the role will be created.
     * @return true if the role can be created.
     */
    public boolean createRole(String location) {
        int type = getObjectType(location);
        return ((type == AMObject.ORGANIZATION) ||
            (type == AMObject.ORGANIZATIONAL_UNIT));
    }  

    /**
     * Returns true if the current user can create a people container at
     * the specified location.
     * 
     * @param location where the people container will be created.
     * @return true if the people container can be created.
     */
    public boolean createPeopleContainer(String location) {
        int type = getObjectType(location);
        return ((type == AMObject.ORGANIZATION) ||
            (type == AMObject.ORGANIZATIONAL_UNIT) ||
            (type == AMObject.PEOPLE_CONTAINER));
    }  

    /**
     * Returns true if the current user can create a user at
     * the specified location.
     * 
     * @param location where the user will be created.
     * @return true if the user can be created.
     */
    public boolean createUser(String location) {
        boolean create = true;
        int type = getObjectType(location);

        if ((peopleContainerDisplay &&
            (type != AMObject.PEOPLE_CONTAINER)) ||
            (!peopleContainerDisplay &&
             (type != AMObject.ORGANIZATION) &&
             (type != AMObject.ORGANIZATIONAL_UNIT)) )
        {
            create = false;
        }
        return create;
    }  

    /**
     * Returns the type of object for a given DN.
     *
     * @param name DN of an object.
     * @return type Type of object.
     */
    public int getObjectType(String name) {
        int objectType = AMObject.UNKNOWN_OBJECT_TYPE;

        if (name == null || name.length() == 0 ) {
            debug.warning("DMModelBase.Invalid object type");
        } else {
            AMStoreConnection storeConn = getAdminStoreConnection();

            try {
                if (DN.isDN(name) || name.indexOf("/") != -1) {
                    objectType = storeConn.getAMObjectType(name);
                } else {
                    // Could be a service
                    if ((locationDN != null) && DN.isDN(locationDN)) {
                        Set services = getServiceList(locationDN);
                        if (services.contains(name)) {
                            objectType = AMObject.SERVICE;
                        }
                    }
                }
            } catch (AMException e) {
                debug.warning("DMModelBase.getObjectType", e);
            } catch (SSOException e) {
                debug.warning("DMModelBase.getObjectType", e);
            }
        }

        return objectType;
    }

    /**
     * Returns a list of the services that have been configured for the
     * organization which contains the given DN.
     */
    protected Set getServiceList(String dn)
        throws AMException, SSOException
    {
        Set list = Collections.EMPTY_SET;
        int type = getObjectType(dn);
        AMStoreConnection sc = getAMStoreConnection();

        if (type == AMObject.ORGANIZATION) {
            list = sc.getOrganization(dn).getRegisteredServiceNames();
        } else if (type == AMObject.ORGANIZATIONAL_UNIT) {
            list = sc.getOrganizationalUnit(dn).getRegisteredServiceNames();
        } else {
            list = getServiceList(AMAdminUtils.getParent(dn));
        }

        return list;
    }

    /**
     * Returns an instance of store connection.
     *
     * @return store connection.
     */
    protected AMStoreConnection getAMStoreConnection() {
        try {
            if (dpStoreConn == null) {
                dpStoreConn = new AMStoreConnection(getUserSSOToken());
            }
        } catch (SSOException ssoe) {
            debug.error("DMModelBase.getAMStoreConnection", ssoe);
        }
        return dpStoreConn;
    }

    /**
     * Returns <code>true</code> if group containers are displayed.
     *
     * @return <code>true</code> to show group containers
     */
    public boolean showGroupContainers() {
        int type = getObjectType(getStartDSDN());
        boolean display = (type == AMObject.ORGANIZATION) ||
            (type == AMObject.ORGANIZATIONAL_UNIT) ||
            (type == AMObject.GROUP_CONTAINER);
        return (groupContainerDisplay && display);
    }

    /**
     * Returns <code>true</code> if people containers are displayed.
     *
     * @return <code>true</code> to show people containers.
     */
    public boolean showPeopleContainers() {
        int type = getObjectType(getStartDSDN());
        boolean display = (type == AMObject.ORGANIZATION) ||
            (type == AMObject.ORGANIZATIONAL_UNIT) ||
            (type == AMObject.PEOPLE_CONTAINER);
        return (display && peopleContainerDisplay);
    }

    /**
     * Returns <code>true</code> if organizational units are displayed.
     *
     * @return <code>true</code> to show organizational units.
     */
    public boolean showOrgUnits() {
        int type = getObjectType(getStartDSDN());
        boolean display = (type == AMObject.ORGANIZATION) ||
            (type == AMObject.ORGANIZATIONAL_UNIT);
        return (display && orgUnitDisplay);
    }

    /**
     * Returns <code>true</code> if organizations are displayed.
     *
     * @return <code>true</code> to show organizations.
     */
    public boolean showOrganizations() {
        int type = getObjectType(getStartDSDN());
        return (type == AMObject.ORGANIZATION) ||
            (type == AMObject.ORGANIZATIONAL_UNIT);
    }

    /**
     * Returns <code>true</code> if groups are displayed.
     *
     * @return <code>true</code> to show groups.
     */
    public boolean showGroups() {
        int type = getObjectType(getStartDSDN());
        return (type == AMObject.ORGANIZATION) ||
            (type == AMObject.ORGANIZATIONAL_UNIT) ||
            (type == AMObject.GROUP) ||
            (type == AMObject.GROUP_CONTAINER);
    }

    /**
     * Returns <code>true</code> if roles are displayed.
     *
     * @return <code>true</code> to show roles.
     */
    public boolean showRoles() {
        int type = getObjectType(getStartDSDN());
        return (type == AMObject.ORGANIZATION) ||
            (type == AMObject.ORGANIZATIONAL_UNIT) ||
            (type == AMObject.ROLE) ||
            (type == AMObject.MANAGED_ROLE) ||
            (type == AMObject.FILTERED_ROLE);
    }

    /**
     * Returns the <code>AMObject</code> of a given DN and a
     * store connection.
     *
     * @param dn Distinguished Name of the object.
     * @return <code>AMObject</code>
     * @throws AMException if AM SDK layer fails.
     * @throws SSOException if user's single sign on token is invalid.
     */
    public AMObject getAMObject(String dn)
        throws AMException, SSOException
    {
        AMStoreConnection sc = getAdminStoreConnection();
        AMObject dpObj = null;
        int objectType = getObjectType(dn);

        switch (objectType) {
        case AMObject.ORGANIZATION:
            dpObj = sc.getOrganization(dn);
            break;
        case AMObject.ORGANIZATIONAL_UNIT:
            dpObj = sc.getOrganizationalUnit(dn);
            break;
        case AMObject.GROUP:
        case AMObject.STATIC_GROUP:
            dpObj = sc.getStaticGroup(dn);
            break;
        case AMObject.DYNAMIC_GROUP:
            dpObj = sc.getDynamicGroup(dn);
            break;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            dpObj = sc.getAssignableDynamicGroup(dn);
            break;
        case AMObject.ROLE:
            dpObj = sc.getRole(dn);
            break;
        case AMObject.FILTERED_ROLE:
            dpObj = sc.getFilteredRole(dn);
            break;

        case AMObject.USER:
            dpObj = sc.getUser(dn);
            break;
        case AMObject.PEOPLE_CONTAINER:
            dpObj = sc.getPeopleContainer(dn);
            break;
        case AMObject.GROUP_CONTAINER:
            dpObj = sc.getGroupContainer(dn);
            break;
        default:
            if (debug.warningEnabled()) {
                debug.warning("DMModelBase.getAMObject: " +
                    "Cannot create AMObject for:" + dn);
            }
        }
        return dpObj;
    }

    protected AMStoreConnection getAdminStoreConnection() {
        AMStoreConnection storeConn = null;
        SSOToken adminSSOToken = (SSOToken)
            AccessController.doPrivileged(AdminTokenAction.getInstance());
        if (adminSSOToken != null) {
            try {
                storeConn = new AMStoreConnection(adminSSOToken);
            } catch (SSOException ssoe) {
                debug.warning("DMModelBase.getAdminStoreConnection", ssoe);
            }
        }
        return storeConn;
    }

    /**
     * Converts a dn to a readable format which can be displayed by a client.
     * Each RDN will be separated by the '>' character.
     *
     * @param dn Distinguished Name to be converted.
     * @return the formated string.
     */
    public String getDisplayPath(String dn) {
        StringBuilder path = new StringBuilder(64);
        List nodes = pathToDisplayString(dn);
        int size = nodes.size();

        for (int x = 0; x < size; x++) {
            String tmp  = (String)nodes.get(x);
                                                                                
            path.append(AMFormatUtils.DNToName(this, tmp));
            if (x < size-1) {
                path.append(" > ");
            }
        }
        return path.toString();
    }


    /**
     * Returns a list of parentage path strings from <code>dn</code> to user's
     * start DN.
     * For example <code>o=org1,o=org2,dc=sun,dc=com</code>, this method returns     * list with entries = <code>[sun,org2,org1]</code>.
     *
     * @param dn location DN
     * @return parentage path from a location to root
     */
    public List pathToDisplayString(String dn) {
        int locType = getObjectType(dn);
        boolean isRole =
            (locType == AMObject.ROLE) || (locType == AMObject.FILTERED_ROLE);
        return pathToDisplayString(dn, getStartDN(), isRole);
    }

    /**
     * Returns a list of parentage path strings from <code>dn</code> to
     * <code>startDN</code>.
     *
     * @param dn location DN.
     * @param startDN start DN of parentage path.
     * @param isRole true if <code>dn</code> is a Role DN.
     * @return a list of parentage path strings.
     */
    public List pathToDisplayString(
        String dn,
        String startDN,
        boolean isRole
    ) {
        List pp = null;
        DN startDNObj = new DN(startDN);
        DN dnObj = new DN(dn);

        if (dnObj.equals(startDNObj)) {
            pp = new ArrayList(1);
                                                                                
            //if (dn.equalsIgnoreCase(AMSystemConfig.rootSuffix)) {
            if (dn.equalsIgnoreCase(getStartDSDN())) {
                pp.add(getStartDSDN());
            } else {
                pp.add(DNToName(startDN, isRole));
            }
        } else {
            pp = pathToDisplayString(LDAPDN.explodeDN(dn, false), dn, isRole);
        }
        return pp;
    }

    private List pathToDisplayString(
        String[] RDNs,
        String locDN,
        boolean isRole
    ) {
        int size = RDNs.length;
        List pp = new ArrayList(size);
        DN currentDN = null;
        DN startDNObj = new DN(getStartDSDN());

        for (int i = size -1; i >= 0; --i) {
            String rootSuffix = getStartDSDN();
            if (currentDN == null) {
                currentDN = new DN(RDNs[i]);
            } else {
                currentDN.addRDN(new RDN(RDNs[i]));
            }

            if (startDNObj.equals(currentDN)) {
                String dn = currentDN.toString();
                                                                                
                if (rootSuffix.equalsIgnoreCase(dn)) {
                    // do this so that the right case will be displayed
                    pp.add(rootSuffix);
                } else {
                    pp.add(dn);
                }

                for (int j = i -1; j > 0; --j) {
                    currentDN.addRDN(new RDN(RDNs[j]));
                    dn = currentDN.toString();
                    pp.add(dn);
                }

                break;
            }
        }

        pp.add(DNToName(locDN, isRole));
        return pp;
    }

    /**
     * Returns the String value of an attribute.
     *
     * @param template Template object.
     * @param attribute Attribute name.
     * @return String value of an attribute.
     * @throws AMException if operation fails.
     * @throws SSOException if user's single sign on token is invalid.
     */
    public String getStringAttribute(AMTemplate template, String attribute)
        throws AMException, SSOException {
        Set tmp = getAttribute(template, attribute);

        return ((tmp != null) && (!tmp.isEmpty())) ? 
            (String)(tmp.iterator().next()) : "";
    }

    /**
     * Returns the string value of an attribute.
     *   
     * @param svcSchemaMgr service schema manager
     * @param type schema type
     * @param attribute name
     * @return string value of an attribute
     * @throws SMSException if operation fails
     */
    public static String getStringAttribute(
        ServiceSchemaManager svcSchemaMgr,
        SchemaType type,
        String attribute
    ) throws SMSException {
        Set tmp = AMAdminUtils.getAttribute(svcSchemaMgr, type, attribute);

        return ((tmp != null) && (!tmp.isEmpty())) ?
            (String)(tmp.iterator().next()) : "";
    }
       
    /**
     * Returns the value of an attribute from a template.
     *
     * @param template Template object.
     * @param attribute Attribute name.
     * @return values of attribute.
     * @throws AMException if operations fails
     * @throws SSOException if user's single sign on token is invalid.
     */
    public Set getAttribute(AMTemplate template, String attribute)
        throws AMException, SSOException {
        Set value = Collections.EMPTY_SET;
        if (template != null) {
            value = template.getAttribute(attribute);
        }
        return (value == null) ? Collections.EMPTY_SET : value;
    }

    /**
     * Used by the IdRepo interfaces to convert a realm based rolename to
     * a dispalyable format.
     * For example:
     *    cn=Static-1_ou=Groups_dc=sun_dc=com
     * would be dispalyed as
     *    Static-1 Administrator
     *
     * This will be used by the Privileges tab and Entity Subject tab views.
     *
     * @param roleName role name value being converted.
     * @return a string which can be displayed in the console.
     */
    public String getRoleDisplayName(String roleName) {
        String suffix = getLocalizedString("admin_suffix.name");

        String grpNamingAttr = AdminInterfaceUtils.getNamingAttribute(
            AMObject.GROUP, debug) + "=";
        String pcNamingAttr = AdminInterfaceUtils.getNamingAttribute(
            AMObject.PEOPLE_CONTAINER, debug) + "=";

        if (roleName.startsWith(grpNamingAttr)) {
            /*
            * Sub groups will contain multiple occurences of '_cn'
            * in the dn so we need to first check if this string
            * exists in the new ret string. If '_cn=' is not
            * present, then return the portion leading up to '_ou'.
            */
            int start = 3;
            int end = roleName.indexOf("_" + grpNamingAttr);
            if (end == -1) {
                end = roleName.lastIndexOf("_" +
                    AdminInterfaceUtils.getNamingAttribute(
                        AMObject.GROUP_CONTAINER, debug));
                if (end == -1) {
                    end = roleName.length();
                }
            }
            roleName = roleName.substring(start, end) + " " + suffix;
        } else if (roleName.startsWith(pcNamingAttr)) {
            int start = 3;
            roleName = roleName.substring(start);
            int end = roleName.indexOf("_");
            if (end == -1) {
                end = roleName.length();
            }
            roleName = roleName.substring(0, end) + " " + suffix;
        }

        return roleName;
    }

    /**
     * Returns relative distinguished name.
     *
     * @param dn Distinguished name.
     * @param isRoleDN <code>true</code> if is role DN.
     * @return name of relative distinguished name.
     */
    public String DNToName(String dn, boolean isRoleDN) {
        String ret = dn;
        if (DN.isDN(dn)) {
            String [] comps = LDAPDN.explodeDN(dn, true);
            ret = comps[0];
            if (isRoleDN) {
                // temp workaround until localization of role names is completed
                //
                // when displaying the role name for one of the group admin
                // strip off the cn= and the remainder of the name after the
                // first occurance of "_" which is not part of actual role name.                // Help Desk Admin's are named 'help_desk_cn=' so for
                // this case we must strip off the naming portion, then
                // everything after the first '_'.
                String HD_NAME = "help_desk_" +
                    AdminInterfaceUtils.getNamingAttribute(
                        AMObject.GROUP, debug) + "=";

                String suffix = getLocalizedString("admin_suffix.name");

                if (ret.startsWith(HD_NAME)) {
                    ret = ret.substring(HD_NAME.length());
                    suffix = getLocalizedString("help_desk_admin_suffix.name");
                    int end = ret.indexOf("_");
                    if (end == -1) {
                        end = ret.length();
                    }
                    ret = ret.substring(0, end) + " " + suffix;
                } else {
                    String grpNamingAttr =
                        AdminInterfaceUtils.getNamingAttribute(
                            AMObject.GROUP, debug);
                    String pcNamingAttr =
                        AdminInterfaceUtils.getNamingAttribute(
                            AMObject.PEOPLE_CONTAINER, debug);

                    if (ret.startsWith(grpNamingAttr + "=")) {
                        /*
                        * Sub groups will contain multiple occurences of '_cn'
                        * in the dn so we need to first check if this string
                        * exists in the new ret string. If '_cn=' is not
                        * present, then return the portion leading up to '_ou'.
                        */
                        int start = 3;
                        int end = ret.indexOf("_" + grpNamingAttr + "=");
                        if (end == -1) {
                            end = ret.lastIndexOf("_" +
                                AdminInterfaceUtils.getNamingAttribute(
                                    AMObject.GROUP_CONTAINER, debug));
                            if (end == -1) {
                                end = ret.length();
                            }
                        }
                        ret = ret.substring(start, end) + " " + suffix;
                    } else if (ret.startsWith(pcNamingAttr + "=")) {
                        int start = 3;
                        ret = ret.substring(start);
                        int end = ret.indexOf("_");
                        if (end == -1) {
                            end = ret.length();
                        }
                        ret = ret.substring(0, end) + " " + suffix;
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Returns search results warning message. <code>AMSearchResult</code>
     * returns an error code whenever size or time limit is reached.
     * This method interprets the error code and return the appropriate
     * warning message.  Empty string is returned if no limits are reached.
     *
     * @param results Access Management Search Result object.
     * @return search results warning message.
     */
    public String getSearchResultWarningMessage(AMSearchResults results) {
        String message = null;

        if (results != null) {
            int lerrorCode = results.getErrorCode();

            if (lerrorCode == AMSearchResults.SIZE_LIMIT_EXCEEDED) {
                message = getLocalizedString("sizeLimitExceeded.message");
            } else if (lerrorCode == AMSearchResults.TIME_LIMIT_EXCEEDED) {
                message = getLocalizedString("timeLimitExceeded.message");
            }
        }
        return (message != null) ? message : "";
    }

    /**
     * Returns the value to display for the Name column in tables which 
     * display user entries. The value returned will be the first entry 
     * found in the Search Return Attribute from the Administration
     * service. This will help users identify what value is being displayed
     * in this column.
     */
    public String getNameColumnLabel() {
        String name = null;

        try {
            String attribute = getUserDisplayAttribute();
            ServiceSchemaManager mgr =
                getServiceSchemaManager(AMAdminConstants.USER_SERVICE);
            ServiceSchema schema = mgr.getSchema(SchemaType.USER);
            AttributeSchema as = schema.getAttributeSchema(attribute);
            if (as != null) {
                String i18nKey = as.getI18NKey();
                name = getL10NAttributeName(mgr, i18nKey);
            }

        } catch (SSOException s) {
            if (debug.warningEnabled()) {
                debug.warning("DMModel.getNameColumnLabel", s);
            }
        } catch (SMSException se) {
            if (debug.warningEnabled()) {
                debug.warning("DMModel.getNameColumnLabel", se);
            }
        }
        return (name != null) ? name : getLocalizedString(DEFAULT_NAME_COLUMN);
    }

    /**
     * When a search is performed a set of attributes from each entry
     * is also returned. This set of attributes is mapped to the dn of the
     * entry returned. This call can be used to retrieve the first attribute
     * from the set of attributes returned. Used for display purpose in the
     * user, role, and group member listing views. Note, since the group 
     * member pages may contain other groups, we need to flag if the entry
     * is a user or not.
     *
     * @param dn the entry being displayed.
     * @return value of the attribute to display.
     */
    public String getUserDisplayValue(String dn) {
        String displayValue = null;
        try {                                  
            // get the first attribute to display
            String displayAttribute = getUserDisplayAttribute();
            if (displayAttribute == null) {
                displayAttribute = AdminInterfaceUtils.getNamingAttribute(
                    AMObject.USER, debug);
            }

            /*
            * Retrieve the entry from the search results, then 
            * pull the attribute from the userEntry map. If the 
            * search results have not been set, get the object,
            * then retrieve the atttribute value from there.
            */
            Set values = null;
            if ((resultsMap != null) && (!resultsMap.isEmpty()) ) {
                Map dnEntry = (Map)resultsMap.get(dn);
                values = (Set)dnEntry.get(displayAttribute);
            } else {
                AMObject dnEntry = getAMObject(dn);
                values = dnEntry.getAttribute(displayAttribute);
            }

            if ((values != null) && (!values.isEmpty())) {
                displayValue = (String)values.iterator().next();
            } else { 
                displayValue = EMPTY_DISPLAY_STRING;
            }
        } catch (AMException ae) {
            if (debug.warningEnabled()) {
                debug.warning("DMModelBase.getUserDisplayValue " +
                    dn + " is not a user entry. Returning DN");
            }
        } catch (SSOException s) {
            debug.warning("DMModelBase.getUserDisplayValue", s);
        }
        return (displayValue != null) ? displayValue : DNToName(dn, false);
    }

    /**
     * Returns the filter used to locate a set of users.
     * If the pattern to search on is * all users should be returned. 
     * The only way to guarantee this is to search using the naming attribute. 
     * The attribute defined in the User Search Key may be an attribute
     * which doesn't require a value. In that case, searches against
     * that attribute will not be returned in the result set (if that
     * attribute happens to be empty).
     *
     * @param pattern to match against user entries.
     * 
     * @return filter for matching users.
     */
    String createUserSearchFilter(String pattern) throws SSOException {
        String searchAttribute = null;
        if (pattern.equals(DEFAULT_SEARCH_PATTERN)) {
            searchAttribute = AdminInterfaceUtils.getNamingAttribute(
                AMObject.USER, debug);
        } else {
            searchAttribute = getUserSearchAttribute();
            if (searchAttribute == null) {
                searchAttribute = AdminInterfaceUtils.getNamingAttribute(
                    AMObject.USER, debug);
            }
        }
        StringBuilder searchFilter = new StringBuilder(20);
        searchFilter.append("(")
            .append(searchAttribute).append("=").append(pattern)
            .append(")");
        return searchFilter.toString();
    }
}
