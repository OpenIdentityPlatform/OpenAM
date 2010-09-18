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
 * $Id: MAPDeviceProfileModelImpl.java,v 1.2 2008/06/25 05:43:18 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.iplanet.services.cdm.Client;
import com.iplanet.services.cdm.clientschema.AMClientCapData;
import com.iplanet.services.cdm.clientschema.AMClientCapException;
import com.iplanet.sso.SSOException;
import com.sun.identity.console.property.PropertyXMLBuilder;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class MAPDeviceProfileModelImpl
    extends MAPModelBase
    implements MAPDeviceProfileModel
{
    private Set createDeviceReqAttrs = null;
    private Map createDeviceDefaultValues = null;

    public MAPDeviceProfileModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
    }

    /**
     * Returns an array of classifications for device attributes.
     *
     * @param clientType Client Type
     * @return an array of classifications for device attributes.
     */
    public String[] getAttributeClassification(String clientType) {
        AMClientCapData ccd = getClientCapDataInstance(clientType);
        return (ccd != null) ? ccd.getClassifications() : null;
    }

    /**
     * Returns localized labels for device attribute classification.
     *
     * @param classifications Array of classifications for device attributes.
     * @return localized labels for device attribute classification.
     */
    public Map getLocalizedClassificationLabels(String[] classifications) {
        Map map = new HashMap(classifications.length *2);
        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                mapServiceName, getUserSSOToken());
            ServiceSchema schema = mgr.getSchema(SchemaType.GLOBAL);

            for (int i = 0; i < classifications.length; i++) {
                String name = classifications[i];
                AttributeSchema as = schema.getAttributeSchema(name);
                String i18nKey = as.getI18NKey();

                if ((i18nKey != null) && (i18nKey.trim().length() > 0)) {
                    map.put(name, getL10NStrFromSvcResourceBundle(i18nKey));
                } else {
                    map.put(name, name);
                }
            }

        } catch (SSOException e) {
            debug.warning(
                "MAPDeviceProfileModelImpl.getLocalizedClassificationLabels",e);
        } catch (SMSException e) {
            debug.warning(
                "MAPDeviceProfileModelImpl.getLocalizedClassificationLabels",e);
        }

        return map;
    }

    /**
     * Returns the property XML for profile view.
     *
     * @param clientType Client Type.
     * @param classification Device attribute classification.
     * @throws AMConsoleException if there are no attribute to display.
     * @return the property XML for profile view.
     */
    public String getProfilePropertyXML(
        String clientType,
        String classification
    ) throws AMConsoleException {
        String xml = "";
        Set attributeNames = getAttributeNames(clientType, classification);
        Set attributeSchemas = getAttributeSchemas(attributeNames);

        try {
            PropertyXMLBuilder builder = new PropertyXMLBuilder(
                mapServiceName, this, attributeSchemas);
            xml = builder.getXML(getReadOnlyAttributeNames(
                clientType, attributeNames));
        } catch (SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        }

        return xml; 
    }

    /**
     * Returns readonly attribute names.
     *
     * @param clientType Client Type.
     * @param attributeNames Attribute Names
     * @return readonly attribute names.
     */
    public Set getReadOnlyAttributeNames(String clientType, Set attributeNames){
        Set readonly = new HashSet(attributeNames.size() *2);
        Map temp = getClientCapDataIntInstance().getProperties(clientType);
        boolean readOnlyProfile = (temp != null) && !isUserAgentSet(clientType);

        if (readOnlyProfile) {
            readonly.addAll(attributeNames);
        } else {
            for (Iterator iter = attributeNames.iterator(); iter.hasNext(); ) {
                String name = (String)iter.next();
                if (name.equals(ATTRIBUTE_NAME_CLIENT_TYPE) ||
                    name.equals(ATTRIBUTE_NAME_USER_AGENT)
                ) {
                    readonly.add(name);
                }
            }
        }

        return readonly;
    }

    private Set getAttributeNames(String clientType, String classification) {
        Set names = null;
        AMClientCapData ccd = getClientCapDataInstance(clientType);
        if (ccd != null) {
            names = ccd.getPropertyNames(classification);
        }
        return (names == null) ? Collections.EMPTY_SET : names;
    }

    private Set getAttributeSchemas(Set names) {
        Set results = new HashSet(names.size() *2);

        for (Iterator iter = names.iterator(); iter.hasNext(); ) {
            String name = (String)iter.next();
            AttributeSchema as = getDeviceAttributeSchema(name);
            if (as != null) {
                results.add(as);
            }
        }

        return results;
    }

    /**
     * Returns attribute values of a device.
     *
     * @param clientType Client Type.
     * @param classification Profile attribute classification.
     * @return attribute values of a device.
     */
    public Map getAttributeValues(String clientType, String classification) {
        String[] params = {clientType, classification};
        logEvent("ATTEMPT_CLIENT_DETECTION_GET_CLIENT_PROFILE", params);
        Set attributeNames = getAttributeNames(clientType, classification);
        Map attributeValues = new HashMap(attributeNames.size() *2);

        if (classification.equals(ADDITIONAL_PROPERTIES_CLASSIFICAIION)) {
            Set values = getAdditionalPropertyValues(clientType);

            if ((values == null) || values.isEmpty()) {
                values = new HashSet(1);
                values.add("");
            }

            String attrName = (String)attributeNames.iterator().next();
            attributeValues = new HashMap(2);
            attributeValues.put(attrName, values);
        } else {
            Client client = clientTypesManager.getClientInstance(clientType);
            if (client != null) {
                for (Iterator i = attributeNames.iterator(); i.hasNext(); ) {
                    String name = (String)i.next();
                    Set values = client.getProperties(name);
                    if ((values == null) || values.isEmpty()) {
                        values = Collections.EMPTY_SET;
                        AttributeSchema as = getDeviceAttributeSchema(name);

                        if (as.getSyntax().equals(
                            AttributeSchema.Syntax.BOOLEAN)
                        ) {
                            values = new HashSet(2);
                            values.add(AMAdminConstants.STRING_FALSE);
                        }
                    }
                    attributeValues.put(name, values);
                }
            }
        }
        logEvent("SUCCEED_CLIENT_DETECTION_GET_CLIENT_PROFILE", params);

        return (attributeValues != null)
            ? attributeValues : Collections.EMPTY_MAP;
    }

    /**
     * Modifies device profile.
     *
     * @param clientType Client Type.
     * @param attributeValues Map of attribute name to set of attribute values.
     * @throws AMConsoleException if updates fails.
     */
    public void modifyProfile(String clientType, Map attributeValues)
        throws AMConsoleException {
        String[] param = {clientType};
        logEvent("ATTEMPT_CLIENT_DETECTION_MODIFY_CLIENT_PROFILE", param);

        Set readonly = getReadOnlyAttributeNames(
            clientType, attributeValues.keySet());
        Map values = new HashMap(attributeValues.size());

        for (Iterator i = attributeValues.keySet().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            if (!readonly.contains(name)) {
                values.put(name, attributeValues.get(name));
            }
        }

        if (!values.isEmpty()) {
            Set set = new HashSet(2);
            set.add(clientType);
            values.put(ATTRIBUTE_NAME_CLIENT_TYPE, set);

            try {
                clientTypesManager.modifyClientExternal(
                    getUserSSOToken(), values);
                logEvent("SUCCEED_CLIENT_DETECTION_MODIFY_CLIENT_PROFILE",
                    param);
            } catch (AMClientCapException e) {
                String strError= getErrorString(e);
                String[] paramsEx = {clientType, strError};
                logEvent("CLIENT_SDK_EXCEPTION_CLIENT_DETECTION_CREATE_CLIENT",
                    paramsEx);
                debug.warning("MAPDeviceProfileModelImpl.modifyProfile", e);
                throw new AMConsoleException(strError);
            }
        }
    }
}
