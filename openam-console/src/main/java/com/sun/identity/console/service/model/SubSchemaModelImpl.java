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
 * $Id: SubSchemaModelImpl.java,v 1.2 2008/06/25 05:43:19 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMServiceProfileModelImpl;
import com.sun.identity.console.base.model.SubConfigMeta;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.sm.SMSException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */
/* Logging is done in base.model.SubConfigMeta class */

public class SubSchemaModelImpl
    extends AMServiceProfileModelImpl
    implements SubSchemaModel
{
    private SubConfigMeta subConfigMeta;

    public SubSchemaModelImpl(
        HttpServletRequest req,
        String serviceName,
        Map map
    ) throws AMConsoleException {
        super(req, serviceName, map);
        subConfigMeta = new SubConfigMeta(serviceName, this);
    }

    /**
     * Returns true if this service has global sub schema.
     *
     * @return true if this service has global sub schema.
     */
    public boolean hasGlobalSubSchema() {
        return subConfigMeta.hasGlobalSubSchema();
    }

    /**
     * Returns list of sub configuration objects.
     *
     * @return list of sub configuration objects.
     * @see com.sun.identity.console.base.model.SMSubConfig
     */
    public List getSubConfigurations() {
        return subConfigMeta.getSubConfigurations();
    }

    /**
     * Deletes sub configurations.
     *
     * @param names Names of sub configuration which are to be deleted.
     * @throws AMConsoleException if sub configuration cannot be deleted.
     */
    public void deleteSubConfigurations(Set names)
        throws AMConsoleException {
        subConfigMeta.setParentId("/");
        subConfigMeta.deleteSubConfigurations(names);
    }

    /**
     * Returns XML for property sheet XML.
     *
     * @param parentId Sub config parent ID.
     * @param viewBeanName Name of View Bean.
     * @param viewBeanClassName Class Name of View Bean.
     * @return XML for property sheet XML.
     */
    public String getPropertySheetXML(
        String parentId,
        String viewBeanName,
        String viewBeanClassName
    ) throws AMConsoleException {
        DelegationConfig dConfig = DelegationConfig.getInstance();
        boolean canModify = dConfig.hasPermission("/", serviceName,
            AMAdminConstants.PERMISSION_MODIFY, this, viewBeanClassName);
        if (!canModify) {
            xmlBuilder.setAllAttributeReadOnly(true);
        }

        subConfigMeta.setParentId(parentId);
        xmlBuilder.setSupportSubConfig(subConfigMeta.hasGlobalSubSchema());
        xmlBuilder.setViewBeanName(viewBeanName);

        try {
            return xmlBuilder.getXML();
        } catch (SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Returns a map of sub schema name to its localized name. We should
     * be able to create sub configuration with these names.
     *
     * @return Map of sub schema name to its localized name.
     */
    public Map getCreateableSubSchemaNames() {
        return subConfigMeta.getCreateableSubSchemaNames();
    }
}
