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
 * $Id: MAPCreateDeviceModelImpl.java,v 1.2 2008/06/25 05:43:18 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.iplanet.services.cdm.clientschema.AMClientCapData;
import com.iplanet.services.cdm.clientschema.AMClientCapException;
import com.iplanet.sso.SSOException;
import com.sun.identity.console.property.PropertyXMLBuilder;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class MAPCreateDeviceModelImpl
    extends MAPModelBase
    implements MAPCreateDeviceModel
{
    private Set createDeviceReqAttrs = null;
    private Map createDeviceDefaultValues = null;

    public MAPCreateDeviceModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
    }

    /**
     * Returns the XML for create device property sheet.
     *
     * @param clientType Name of Client Type.
     * @param style Name of Style.
     * @throws AMConsoleException if there are no attributes to display.
     * @return XML for create device property sheet.
     */
    public String getCreateDevicePropertyXML(String clientType, String style)
        throws AMConsoleException
    {
        String xml = "";
        createDeviceReqAttrs = getReqAttributeSchemas();

        if ((createDeviceReqAttrs != null) && !createDeviceReqAttrs.isEmpty()) {
            getCreateDeviceDefaultValues(
                createDeviceReqAttrs, clientType, style);
            String serviceName = getClientCapDataIntInstance().getServiceName();

            try {
                PropertyXMLBuilder builder = new PropertyXMLBuilder(
                    serviceName, this, createDeviceReqAttrs);
                xml = builder.getXML();
            } catch (SMSException e) {
                debug.warning(
                    "MAPCreateDeviceModelImpl.getCreateDevicePropertyXML", e);
            } catch (SSOException e) {
                debug.warning(
                    "MAPCreateDeviceModelImpl.getCreateDevicePropertyXML", e);
            }
        }

        return xml;
    }

    /**
     * Returns a set of attriute names for device creation.
     *
     * @return a set of attriute names for device creation.
     */
    public Set getCreateDeviceAttributeNames() {
        Set names = new HashSet(createDeviceReqAttrs.size() *2);

        for (Iterator iter = createDeviceReqAttrs.iterator(); iter.hasNext(); ){
            AttributeSchema as = (AttributeSchema)iter.next();
            names.add(as.getName());
        }
        return names;
    }

    /**
     * Returns a map of attribute name to its default values.
     *
     * @return a map of attribute name to its default values.
     */
    public Map getCreateDeviceDefaultValues() {
        return createDeviceDefaultValues;
    }

    private void getCreateDeviceDefaultValues(
        Set attributeSchemas,
        String clientType,
        String style
    ) {
        createDeviceDefaultValues = new HashMap(attributeSchemas.size());

        for (Iterator iter = attributeSchemas.iterator(); iter.hasNext(); ) {
            AttributeSchema as = (AttributeSchema)iter.next();
            String asName = as.getName();

            if (asName.equals(ATTRIBUTE_NAME_PARENT_TYPE)) {
                Set set = new HashSet(1);
                set.add(style);
                createDeviceDefaultValues.put(asName, set);
            } else if (asName.equals(ATTRIBUTE_NAME_CLIENT_TYPE)) {
                Set set = new HashSet(1);
                set.add(canonicalize(clientType));
                createDeviceDefaultValues.put(asName, set);
            } else if (asName.equals(ATTRIBUTE_NAME_USER_AGENT)) {
                Set set = new HashSet(1);
                set.add(clientType);
                createDeviceDefaultValues.put(asName, set);
            } else {
                createDeviceDefaultValues.put(as.getName(),
                    as.getDefaultValues());
            }
        }
    }

    private Set getReqAttributeSchemas() {
        Set required = null;
        AMClientCapData ccd = getClientCapDataExtInstance();

        if (ccd != null) {
            Set names = AMClientCapData.getSchemaElements();

            if ((names != null) && !names.isEmpty()) {
                required = getAttributeSchemas(names);

                for (Iterator iter = required.iterator(); iter.hasNext(); ) {
                    AttributeSchema as = (AttributeSchema)iter.next();
                    String any = as.getAny();
                    if ((any == null) || 
                        (any.indexOf(AMAdminConstants.REQUIRED_ATTRIBUTE) == -1)
                    ) {
                        iter.remove();
                    }
                }
            }
        }

        return (required == null) ? Collections.EMPTY_SET : required;
    }

    private Set getAttributeSchemas(Set names) {
        Set attributeSchemas = new HashSet(names.size());
        for (Iterator iter = names.iterator(); iter.hasNext(); ) {
            AttributeSchema as = getAttributeSchema((String)iter.next());
            if (as != null) {
                attributeSchemas.add(as);
            }
        }
        return attributeSchemas;
    }

    private AttributeSchema getAttributeSchema(String name) {
        AttributeSchema as = null;
        AMClientCapData internalInstance = getClientCapDataIntInstance();
        AMClientCapData externalInstance = getClientCapDataExtInstance();

        if ((internalInstance != null) && (externalInstance != null)) {
            as = internalInstance.getAttributeSchema(name);

            if (as == null) {
                as = externalInstance.getAttributeSchema(name);
            }

            if (as != null) {
                String i18nKey = as.getI18NKey();
                if ((i18nKey == null) || (i18nKey.length() == 0)) {
                    as = null;
                }
            }
        }

        return as;
    }

    /**
     * Create new device.
     *
     * @param attrValues Attribute Values for the new device.
     * @throws AMConsoleException if device cannot be created.
     */
    public void createDevice(Map attrValues)
        throws AMConsoleException {
        String clientType = (String)AMAdminUtils.getValue(
            (Set)attrValues.get(ATTRIBUTE_NAME_CLIENT_TYPE));
        String[] param = {clientType};
        logEvent("ATTEMPT_CLIENT_DETECTION_CREATE_CLIENT", param);

        try {
            validateClientType(clientType);
            clientTypesManager.addClientExternal(getUserSSOToken(), attrValues);
            logEvent("SUCCEED_CLIENT_DETECTION_CREATE_CLIENT", param);
        } catch (AMConsoleException e) {
            String[] paramsEx = {clientType, e.getMessage()};
            logEvent("INVALID_CLIENT_TYPE_CLIENT_DETECTION_CREATE_CLIENT",
                paramsEx);
            throw e;
        } catch (AMClientCapException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {clientType, strError};
            logEvent("CLIENT_SDK_EXCEPTION_CLIENT_DETECTION_CREATE_CLIENT",
                paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    private void validateClientType(String clientType)
        throws AMConsoleException {
        // Check if client exists in internal library before adding the client
        if (getClientCapDataIntInstance().isClientPresent(clientType)) {
            String[] param = {clientType};
            String msg = MessageFormat.format(
                getLocalizedString("mapClientExist.message"), (Object[])param);
            throw new AMConsoleException(msg);
        }

        if (!isClientTypeValid(clientType)) {
            throw new AMConsoleException(
                getLocalizedString("mapInvalidClientType.message"));
        }
    }

    /**
     * Clones a device.
     *
     * @param origClientType Original Client Type.
     * @param clientType Client Type.
     * @param deviceName Device Name.
     * @throws AMConsoleException if device cannot be clone
     */
    public void cloneDevice(
        String origClientType,
        String clientType,
        String deviceName
    ) throws AMConsoleException {
        Map map = getCloningProperties(origClientType);

        if (map == null) {
            throw new AMConsoleException(
                getLocalizedString("mapCloneFailed.message"));
        }
        String[] param = {clientType};
        logEvent("ATTEMPT_CLIENT_DETECTION_CREATE_CLIENT", param);

        try {
            validateClientType(clientType);

            Set set = new HashSet(1);
            set.add(clientType);
            map.put(ATTRIBUTE_NAME_CLIENT_TYPE, set);
            set = new HashSet(1);
            set.add(deviceName);
            map.put(ATTRIBUTE_NAME_USER_AGENT, set);

            clientTypesManager.addClientExternal(getUserSSOToken(), map);
            logEvent("SUCCEED_CLIENT_DETECTION_CREATE_CLIENT", param);
        } catch (AMConsoleException e) {
            String[] paramsEx = {clientType, e.getMessage()};
            logEvent("INVALID_CLIENT_TYPE_CLIENT_DETECTION_CREATE_CLIENT",
                paramsEx);
            throw e;
        } catch (AMClientCapException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {clientType, strError};
            logEvent("CLIENT_SDK_EXCEPTION_CLIENT_DETECTION_CREATE_CLIENT",
                paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    private Map getCloningProperties(String clientType) {
        AMClientCapData ccd = getClientCapDataIntInstance();
        Map iMap = ccd.getProperties(clientType);
                                                                                
        AMClientCapData eCD = getClientCapDataExtInstance();
        Map eMap = eCD.getProperties(clientType);

        Map merged = null;
        if (iMap != null) {
            merged = iMap;
            if (eMap != null) {
                merged.putAll(eMap);
            }
        } else if (eMap != null) {
            merged = eMap;
        }

        return merged;
    }
}
