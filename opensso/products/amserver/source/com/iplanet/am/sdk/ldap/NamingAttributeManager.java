/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: NamingAttributeManager.java,v 1.3 2008/06/25 05:41:25 qcheng Exp $
 *
 */

package com.iplanet.am.sdk.ldap;

import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.common.MiscUtils;
import com.iplanet.ums.CreationTemplate;
import com.iplanet.ums.Guid;
import com.iplanet.ums.TemplateManager;
import com.iplanet.ums.UMSException;
import com.sun.identity.shared.debug.Debug;
import java.util.HashMap;
import java.util.Map;

class NamingAttributeManager {

    // Define default naming attribute name for different types of object
    private static final String DEFAULT_USER_NAMING_ATTR = "uid";

    private static final String DEFAULT_RESOURCE_NAMING_ATTR = "uid";

    private static final String DEFAULT_ROLE_NAMING_ATTR = "cn";

    private static final String DEFAULT_GROUP_NAMING_ATTR = "cn";

    private static final String DEFAULT_ORG_NAMING_ATTR = "o";

    private static final String DEFAULT_ORG_UNIT_NAMING_ATTR = "ou";

    private static final String DEFAULT_PEOPLE_CONTAINER_NAMING_ATTR = "ou";

    private static final String DEFAULT_GROUP_CONTAINER_NAMING_ATTR = "ou";

    private static final String DEFAULT_DYNAMIC_GROUP_NAMING_ATTR = "cn";

    private static final String DEFAULT_FILTERED_ROLE_NAMING_ATTR = "cn";

    private static final String DEFAULT_ASSIGNABLE_DYNAMIC_GROUP_NAMING_ATTR =
        "cn";

    // Creation Template Names
    private static final String USER_CREATION_TEMPLATE = "BasicUser";

    private static final String GROUP_CREATION_TEMPLATE = "BasicGroup";

    private static final String MANAGED_ROLE_CREATION_TEMPLATE = 
        "BasicManagedRole";

    private static final String RESOURCE_CREATION_TEMPLATE = 
        "BasicResource";

    private static final String FILTERED_ROLE_CREATION_TEMPLATE = 
        "BasicFilteredRole";

    private static final String ASSIGANABLE_DYNAMIC_GROUP_CREATION_TEMPLATE = 
        "BasicAssignableDynamicGroup";

    private static final String DYNAMIC_GROUP_CREATION_TEMPLATE = 
        "BasicDynamicGroup";

    private static final String ORGANIZATION_CREATION_TEMPLATE = 
        "BasicOrganization";

    private static final String PEOPLE_CONTAINTER_CREATION_TEMPLATE = 
        "BasicPeopleContainer";

    private static final String ORGANIZATIONAL_UNIT_CREATION_TEMPLATE = 
        "BasicOrganizationalUnit";

    private static final String GROUP_CONTAINER_CREATION_TEMPLATE = 
        "BasicGroupContainer";

    // TemplateManager handle. Keep handle to avoid getManager() calls which
    // are synchronized
    private static TemplateManager templateMgr = null;

    private static Debug debug = CommonUtils.getDebugInstance();

    private static Map namingAttrMap = new HashMap();

    static String getNamingAttribute(int objectType) {
        return getNamingAttribute(objectType, null);
    }

    /**
     * Gets the naming attribute after reading it from the corresponding
     * creation template. If not found, a default value will be used
     */
    static String getNamingAttribute(int objectType, String orgDN) {
        String key = (new Integer(objectType)).toString() + ":"
                + MiscUtils.formatToRFC(orgDN);
        String namingAttribute = (String) namingAttrMap.get(key);
        if (namingAttribute == null) {
            namingAttribute = getNamingAttributeFromTemplate(objectType, orgDN);
            if (namingAttribute != null) {
                namingAttrMap.put(key, namingAttribute);
            }
        }
        return namingAttribute;
    }

    /**
     * Gets the naming attribute after reading it from the corresponding
     * creation template. If not found, a default value will be used
     */
    private static String getNamingAttributeFromTemplate(int objectType,
            String orgDN) {
        try {
            // Intitalize TemplateManager if not already initialized
            if (templateMgr == null) {
                templateMgr = TemplateManager.getTemplateManager();
            }

            String templateName = getCreationTemplateName(objectType);
            if (templateName == null) {
                debug.warning("AMNamingAttrMgr.getNamingAttr(objectType, "
                        + "orgDN): ("  + objectType   + "," + orgDN
                        + ") Could not determine creation template name. " 
                        + "Returning <empty> value");
                return "";
            }

            Guid orgGuid = ((orgDN == null) ? null : new Guid(orgDN));
            CreationTemplate creationTemp = templateMgr.getCreationTemplate(
                    templateName, orgGuid, TemplateManager.SCOPE_ANCESTORS);
            // get search filter attribute
            String namingAttr = creationTemp.getNamingAttribute();
            if (namingAttr == null) {
                debug.error("AMNamingAttrManager.getNamingAttr()"
                        + " Naming attribute for Object Type:" + objectType
                        + " Org DN: " + orgDN + " is null");
            } else if (debug.messageEnabled()) {
                debug.message("AMNamingAttrManager.getNamingAttr(): Naming "
                        + "attribute for Object type= " + objectType + ": "
                        + namingAttr);
            }
            return namingAttr;
        } catch (UMSException ue) {
            // The right thing would be to propagate this exception back
            String defaultAttr = getDefaultNamingAttr(objectType);
            debug.warning("Unable to get the naming attribute for "
                    + objectType + " Using default " + defaultAttr);
            return defaultAttr;
        }
    }

    /**
     * Gets the default naming attribute corresponding to an object type
     */
    private static String getDefaultNamingAttr(int objectType) {
        switch (objectType) {
        case AMObject.USER:
            return DEFAULT_USER_NAMING_ATTR;
        case AMObject.ROLE:
            return DEFAULT_ROLE_NAMING_ATTR;
        case AMObject.FILTERED_ROLE:
            return DEFAULT_FILTERED_ROLE_NAMING_ATTR;
        case AMObject.GROUP:
            return DEFAULT_GROUP_NAMING_ATTR;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            return DEFAULT_ASSIGNABLE_DYNAMIC_GROUP_NAMING_ATTR;
        case AMObject.DYNAMIC_GROUP:
            return DEFAULT_DYNAMIC_GROUP_NAMING_ATTR;
        case AMObject.ORGANIZATION:
            return DEFAULT_ORG_NAMING_ATTR;
        case AMObject.PEOPLE_CONTAINER:
            return DEFAULT_PEOPLE_CONTAINER_NAMING_ATTR;
        case AMObject.ORGANIZATIONAL_UNIT:
            return DEFAULT_ORG_UNIT_NAMING_ATTR;
        case AMObject.GROUP_CONTAINER:
            return DEFAULT_GROUP_CONTAINER_NAMING_ATTR;
        case AMObject.RESOURCE:
            return DEFAULT_RESOURCE_NAMING_ATTR;
        default:
            debug.warning("AMNamingAttrMgr.getDefaultNamingAttr(): Unknown "
                    + "object type is passed. Returning <empty> value");
            return ""; // This should not occur
        }
    }

    /**
     * Get the name of the creation template to use for specified object type.
     */
    static String getCreationTemplateName(int objectType) {
        String templateName = (String) CommonUtils.creationtemplateMap
                .get(Integer.toString(objectType));
        if (templateName != null) {
            return templateName;
        }
        switch (objectType) {
        case AMObject.USER:
            return USER_CREATION_TEMPLATE;
        case AMObject.ROLE:
            return MANAGED_ROLE_CREATION_TEMPLATE;
        case AMObject.FILTERED_ROLE:
            return FILTERED_ROLE_CREATION_TEMPLATE;
        case AMObject.GROUP:
            return GROUP_CREATION_TEMPLATE;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            return ASSIGANABLE_DYNAMIC_GROUP_CREATION_TEMPLATE;
        case AMObject.DYNAMIC_GROUP:
            return DYNAMIC_GROUP_CREATION_TEMPLATE;
        case AMObject.ORGANIZATION:
            return ORGANIZATION_CREATION_TEMPLATE;
        case AMObject.PEOPLE_CONTAINER:
            return PEOPLE_CONTAINTER_CREATION_TEMPLATE;
        case AMObject.ORGANIZATIONAL_UNIT:
            return ORGANIZATIONAL_UNIT_CREATION_TEMPLATE;
        case AMObject.GROUP_CONTAINER:
            return GROUP_CONTAINER_CREATION_TEMPLATE;
        case AMObject.RESOURCE:
            return RESOURCE_CREATION_TEMPLATE;
        default:
            debug.warning("AMNamingAttrMgr.getCreationTemplateName(): "
                    + "Unknown object type is passed. Returning null value");
            return null;
        }
    }
}
