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
 * $Id: RMRealmModel.java,v 1.3 2008/06/25 05:43:12 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.realm.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import java.util.Collection;
import java.util.Map;

/* - NEED NOT LOG - */

public interface RMRealmModel
    extends AMModel
{
    String TF_NAME = "tfName";
    String TF_PARENT = "tfParent";

    /**
     * Returns sub realm creation property XML.
     *
     * @throws AMConsoleException if there are no attributes to display.
     * @return sub realm creation property XML.
     */
    String getCreateRealmPropertyXML()
        throws AMConsoleException;

    /**
     * Returns realm profile property XML.
     *
     * @param realmName Name of Realm.
     * @param viewbeanClassName Class name of View Bean
     * @throws AMConsoleException if there are no attributes to display.
     * @return realm profile property XML.
     */
    String getRealmProfilePropertyXML(
        String realmName,
        String viewbeanClassName
    ) throws AMConsoleException;

    /**
     * Creates sub realm.
     *
     * @param parentRealm Parent realm name.
     * @param name Name of new sub realm.
     * @param attrValues Map of attribute name to a set of attribute values.
     * @throws AMConsoleException if sub realm cannot be created.
     */
    void createSubRealm(String parentRealm, String name, Map attrValues)
        throws AMConsoleException;

    /**
     * Deletes sub realms
     *
     * @param parentRealm Parent realm name.
     * @param names List of realm names to be deleted.
     * @throws AMConsoleException if sub realms cannot be deleted.
     */
    void deleteSubRealms(String parentRealm, Collection names)
        throws AMConsoleException;

    /**
     * Returns Map of default realm attribute values.
     *
     * @return attribute values.
     */
    Map getDefaultValues();


    /**
     * Returns Map of attribute name to empty set of values.
     *
     * @return attribute values.
     */
    Map getDataMap();

    /**
     * Returns attribute values. Map of attribute name to set of values.
     *
     * @param name Name of Realm.
     * @return attribute values.
     */
    Map getAttributeValues(String name)
        throws AMConsoleException;

    /**
     * Set attribute values.
     *
     * @param name Name of Realm.
     * @param attributeValues Map of attribute values to set of values.
     * @throws AMConsoleException if attribute values cannot be updated.
     */
    void setAttributeValues(String name, Map attributeValues)
        throws AMConsoleException;
}
