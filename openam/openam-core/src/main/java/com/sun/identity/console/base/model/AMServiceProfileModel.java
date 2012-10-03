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
 * $Id: AMServiceProfileModel.java,v 1.2 2008/06/25 05:42:50 qcheng Exp $
 *
 */

package com.sun.identity.console.base.model;

import java.util.Map;
import java.util.Set;

/* - NEED NOT LOG - */

public interface AMServiceProfileModel
    extends AMModel
{
    /**
     * Returns page title.
     *
     * @return page title.
     */
    public String getPageTitle();

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
    ) throws AMConsoleException;

    /**
     * Returns attributes values.
     *
     * @return attributes values.
     */
    public Map getAttributeValues();

    /**
     * Returns attribute values.
     *
     * @param name Name of attribute.
     * @return attribute values.
     */
    public Set getAttributeValues(String name);

    /**
     * Set attribute values.
     *
     * @param map Map of attribute name to Set of attribute values.
     * @throws AMConsoleException if values cannot be set.
     */
    public void setAttributeValues(Map map)
        throws AMConsoleException;

    /**
     * Returns properties view bean URL for an attribute schema.
     *
     * @param name Name of attribute schema.
     * @return properties view bean URL for an attribute schema.
     */
    public String getPropertiesViewBean(String name);
}
