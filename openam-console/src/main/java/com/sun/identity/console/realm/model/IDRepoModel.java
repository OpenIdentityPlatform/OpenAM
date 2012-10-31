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
 * $Id: IDRepoModel.java,v 1.3 2009/11/19 23:46:00 asyhuang Exp $
 *
 */

package com.sun.identity.console.realm.model;

import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;

/* - NEED NOT LOG - */

public interface IDRepoModel
    extends AMModel
{
    String TF_NAME = "tfName";

    /**
     * Returns a set of ID Repository names.
     *
     * @param realmName Name of Realm.
     * @return a set of ID Repository names.
     * @throws AMConsoleException if there are errors getting these names.
     */
    Set getIDRepoNames(String realmName)
        throws AMConsoleException;

    /**
     * Returns map of ID Repo type name to its localized string.
     *
     * @return map of ID Repo type name to its localized string.
     */
    Map getIDRepoTypesMap()
        throws AMConsoleException;

    /**
     * Returns an option list of ID Repo type name to its localized string.
     *
     * @return an option list of ID Repo type name to its localized string.
     */
    OptionList getIDRepoTypes()
        throws AMConsoleException;

    /**
     * Returns property sheet XML for ID Repo Profile.
     *
     * @param realmName Name of Realm.
     * @param viewBeanClassName Class Name of View Bean.
     * @param type Type of ID Repo.
     * @param bCreate <code>true</code> for creation operation.
     * @return property sheet XML for ID Repo Profile.
     */
    String getPropertyXMLString(
        String realmName,
        String viewBeanClassName,
        String type,
        boolean bCreate
    ) throws AMConsoleException;

    /**
     * Returns default attribute values of ID Repo.
     *
     * @param type Type of ID Repo.
     * @return default attribute values of ID Repo.
     */
    Map getDefaultAttributeValues(String type);

    /**
     * Returns attribute values of ID Repo.
     *
     * @param realmName Name of Realm.
     * @param name Name of ID Repo.
     * @return attribute values of ID Repo.
     * @throws AMConsoleException if attribute values cannot be obtained.
     */
    Map getAttributeValues(String realmName, String name)
        throws AMConsoleException;

    /**
     * Creates ID Repo object.
     *
     * @param realmName Name of Realm.
     * @param idRepoName Name of ID Repo.
     * @param idRepoType Type of ID Repo.
     * @param values Map of attribute name to set of values.
     * @throws AMConsoleException if object cannot be created.
     */
    void createIDRepo(
        String realmName,
        String idRepoName,
        String idRepoType,
        Map values
    ) throws AMConsoleException;

    /**
     * Deletes ID Repo objects.
     *
     * @param realmName Name of Realm.
     * @param names Set of ID Repo names to be deleted.
     * @throws AMConsoleException if object cannot be deleted.
     */
    void deleteIDRepos(String realmName, Set names)
        throws AMConsoleException;

    /**
     * Edits ID Repo object.
     *
     * @param realmName Name of Realm.
     * @param idRepoName Name of ID Repo.
     * @param values Map of attribute name to set of values.
     * @throws AMConsoleException if object cannot be created.
     */
    void editIDRepo(String realmName, String idRepoName, Map values)
        throws AMConsoleException;

    /**
     * Returns ID Repo Type of an object.
     *
     * @param realmName Name of Realm.
     * @param idRepoName Name of ID Repo.
     * @return ID Repo Type of an object.
     * @throws AMConsoleException if type cannot be determined.
     */
    String getIDRepoType(String realmName, String idRepoName)
        throws AMConsoleException;

    void loadIdRepoSchema(String idRepoName, String realmName, ServletContext servletCtx)
        throws AMConsoleException;

}
