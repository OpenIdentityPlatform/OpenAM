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
 * $Id: SMProfileModel.java,v 1.2 2008/06/25 05:43:21 qcheng Exp $
 *
 */

package com.sun.identity.console.session.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import java.util.List;
import java.util.Map;

/* - NEED NOT LOG - */

/**
 * <code>SMProfileModel</code> defines a set of methods that are required by
 * session profile view bean.
 */
public interface SMProfileModel
    extends AMModel 
{
    /**
     * Sets server name.
     *
     * @param serverName sever name.
     */
    public void setProfileServerName(String serverName);

    /**
     * Invalidates list of session.
     *
     * @param list of session <code>ID</code>s.
     * @param pattern Search pattern.
     * @return a list of session <code>ID</code>s that cannot be validated.
     */
    public List invalidateSessions(List list, String pattern)
        throws AMConsoleException;


    /**
     * Returns true if current user session state is valid.
     *
     * @return true is the session is valid.
     */
    public boolean isSessionValid();

    /**
     * Returns session cache.
     *
     * @param pattern Pattern for search.
     * @return session cache.
     * @throws AMConsoleException if unable to get the session cache.
     */
    public SMSessionCache getSessionCache(String pattern)
        throws AMConsoleException;

    /**
     * Sets session cache.
     *
     * @param cache Session cache.
     */
    public void setSessionCache(SMSessionCache cache);

    /**
     * Returns map of server names.
     *
     * @return map of server names.
     */
    public Map getServerNames();

    /**
     * Returns attribute values. Map of attribute name to set of values.
     *
     * @param name Name of Attribute.
     * @return attribute values.
     */
    Map getAttributeValues(String name)
            throws AMConsoleException;

    /**
     * Set attribute values.
     *
     * @param name Name of Attribute.
     * @param attributeValues Map of attribute values to set of values.
     * @throws AMConsoleException if attribute values cannot be updated.
     */
    void setAttributeValues(String name, Map attributeValues)
            throws AMConsoleException;

    /**
     * Returns session profile property XML.
     *
     * @param profileName Name of Profile to be Obtained.
     * @param viewbeanClassName Class name of View Bean
     * @throws AMConsoleException if there are no attributes to display.
     * @return String of session profile property XML.
     */
     String getSessionProfilePropertyXML(String profileName, String viewbeanClassName)
             throws AMConsoleException;

}
