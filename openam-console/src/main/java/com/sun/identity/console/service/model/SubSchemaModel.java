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
 * $Id: SubSchemaModel.java,v 1.2 2008/06/25 05:43:19 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMServiceProfileModel;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* - NEED NOT LOG - */

public interface SubSchemaModel
    extends AMServiceProfileModel
{
    /**
     * Returns true if this service has global sub schema.
     *
     * @return true if this service has global sub schema.
     */
    boolean hasGlobalSubSchema();

    /**
     * Returns list of sub configuration objects.
     *
     * @return list of sub configuration objects.
     * @see com.sun.identity.console.base.model.SMSubConfig
     */
    List getSubConfigurations();

    /**
     * Returns XML for property sheet XML.
     *
     * @param parentId Sub config parent ID.
     * @param viewBeanName Name of View Bean.
     * @param viewBeanClassName Class Name of View Bean.
     * @return XML for property sheet XML.
     */
    String getPropertySheetXML(
        String parentId,
        String viewBeanName,
        String viewBeanClassName
    ) throws AMConsoleException;

    /**
     * Deletes sub configurations.
     *
     * @param names Names of sub configuration which are to be deleted.
     * @throws AMConsoleException if sub configuration cannot be deleted.
     */
    void deleteSubConfigurations(Set names)
        throws AMConsoleException;

    /**
     * Returns a map of sub schema name to its localized name. We should
     * be able to create sub configuration with these names.
     *
     * @return Map of sub schema name to its localized name.
     */
    Map getCreateableSubSchemaNames();
}
