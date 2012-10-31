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
 * $Id: SCConfigModelImpl.java,v 1.2 2008/06/25 05:43:18 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - NEED NOT LOG - */

/**
 * This class provides relevant information to service configuration related
 * view beans.
 */ 
public class SCConfigModelImpl extends AMModelBase
    implements SCConfigModel
{
    private static String[] sectionNames = {
        SEC_AUTH, SEC_CONSOLE, SEC_GLOBAL, SEC_SYSTEM
    };

    private static ResourceBundle rbServiceTable =
        ResourceBundle.getBundle("amServiceTable");

    /*
     * Map of section name to a list of service names.
     */
    private Map mapSectionNameToServiceNames =
        new HashMap(sectionNames.length *2);

    /*
     * Map of service name to its localized name.
     */
    private Map mapServiceNameToLocalizedName = new HashMap();

    /**
     * Creates a service data model implementation object.
     *
     * @param req The <code>HttpServletRequest</code> object.
     * @param map of user information.
     */
    public SCConfigModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
        getServiceNames();
        getL10NServiceNames();
        sortServiceNames();
    }

    /**
     * Returns properties view bean URL of a service.
     *
     * @param serviceName Name of service.
     * @return properties view bean URL of a service. Returns null if
     *         this URL is not defined in the schema.
     */
    public String getServicePropertiesViewBeanURL(String serviceName) {
        if (AMAdminConstants.POLICY_SERVICE.equals(serviceName)) {
            return "../service/SCPolicy";
        } else {
            return super.getServicePropertiesViewBeanURL(serviceName);
        }
    }

    public List getServiceNames(String sectionName) {
        return (List)mapSectionNameToServiceNames.get(sectionName);
    }

    public String getLocalizedServiceName(String serviceName) {
        return (String)mapServiceNameToLocalizedName.get(serviceName);
    }

    private void getServiceNames() {
        try {
            ServiceManager sm = new ServiceManager(getUserSSOToken());
            Set serviceNames = sm.getServiceNames();

            Set authServices = 
                AMAuthenticationManager.getAuthenticationServiceNames();

            if (serviceNames != null) {
                for (Iterator i = serviceNames.iterator(); i.hasNext(); ) {
                    String svcName = (String)i.next();
                    String sectionName = null;

                    try {
                        sectionName = rbServiceTable.getString(svcName);
                    } catch (MissingResourceException e) {
                        /*
                        * we want all authentication services to be displayed
                        * in the authentication section. The rest of the 
                        * unknown services can be put into the global section.
                        */
                        if (authServices.contains(svcName)) {
                            sectionName = SEC_AUTH;
                        } else {
                            sectionName = SEC_GLOBAL;
                        }
                    }

                    // hide section name with "."
                    if (!sectionName.equals(".")) {
                        List list = (List)mapSectionNameToServiceNames.get(
                            sectionName);
                        if (list == null) {
                            list = new ArrayList(20);
                            mapSectionNameToServiceNames.put(sectionName, list);
                        }
                        list.add(svcName);
                    }
                }
            }
        } catch (SSOException ssoe) {
            debug.error("SCConfigModelImpl.getServiceNames", ssoe);
        } catch (SMSException smse) {
            debug.error("SCConfigModelImpl.getServiceNames", smse);
        }
    }

    private void getL10NServiceNames() {
        for (Iterator i = mapSectionNameToServiceNames.keySet().iterator();
            i.hasNext();
        ) {
            String sectionName = (String)i.next();
            List list = (List)mapSectionNameToServiceNames.get(sectionName);

            for (Iterator iter = list.iterator(); iter.hasNext(); ) {
                String svcName = (String)iter.next();
                String i18nName = super.getLocalizedServiceName(svcName, null);

                if (i18nName != null) {
                    mapServiceNameToLocalizedName.put(svcName, i18nName);
                } else {
                    iter.remove();
                }
            }
        }
    }

    private void sortServiceNames() {
        for (Iterator i = mapSectionNameToServiceNames.keySet().iterator();
            i.hasNext();
        ) {
            String sectionName = (String)i.next();
            List list = (List)mapSectionNameToServiceNames.get(sectionName);
            Map map = new HashMap(list.size() *2);

            for (Iterator iter = list.iterator(); iter.hasNext(); ) {
                String svcName = (String)iter.next();
                map.put(getLocalizedServiceName(svcName), svcName);
            }

            List sorted = AMFormatUtils.sortKeyInMap(map, getUserLocale());
            List sortedList = new ArrayList(list.size());
            for (Iterator iter = sorted.iterator(); iter.hasNext(); ) {
                String localizedName = (String)iter.next();
                sortedList.add(map.get(localizedName));
            }
            mapSectionNameToServiceNames.put(sectionName, sortedList);
        }
    }

    public boolean hasConfigAttributes(String serviceName) {

        Set o = AMAdminUtils.getDisplayableAttributeNames(
            serviceName, SchemaType.ORGANIZATION);
        Set d = AMAdminUtils.getDisplayableAttributeNames(
            serviceName, SchemaType.DYNAMIC);
        Set g = AMAdminUtils.getDisplayableAttributeNames(
            serviceName, SchemaType.GLOBAL);

        return (!o.isEmpty() || !d.isEmpty() || !g.isEmpty());
    }
}
