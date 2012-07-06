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
 * $Id: IDynamicMembership.java,v 1.4 2008/06/25 05:41:45 qcheng Exp $
 *
 */

package com.iplanet.ums;

/**
 * Represents a container interface common to groups and roles. It extends
 * IMembership by adding support for adding and removing members.
 * 
 * @see com.iplanet.ums.IMembership
 * @supported.api
 */
public interface IDynamicMembership extends IMembership {

    /**
     * Sets the search filter used to evaluate this dynamic group.
     * 
     * @param filter
     *            search filter for evaluating members of the group
     *
     * @supported.api
     */
    public void setSearchFilter(String filter);

    /**
     * Gets the search filter used to evaluate this dynamic group.
     * 
     * @return search filter for evaluating members of the group
     *
     * @supported.api
     */
    public String getSearchFilter();

    /**
     * Sets the search base used to evaluate this dynamic group.
     * 
     * @param baseGuid Search base for evaluating members of the group.
     *
     * @supported.api
     */
    public void setSearchBase(Guid baseGuid);

    /**
     * Gets the search base used to evaluate this dynamic group.
     * 
     * @return search base for evaluating members of the group
     *
     * @supported.api
     */
    public Guid getSearchBase();

    /**
     * Sets the search scope used to evaluate this dynamic group.
     * 
     * @param scope Search scope for evaluating members of the group. Use one of
     *        the <code>LDAPv2</code> scopes: <code>SCOPE_BASE</code>,
     *        <code>SCOPE_ONE</code>, or <code>SCOPE_SUB</code>.
     *
     * @supported.api
     */
    public void setSearchScope(int scope);

    /**
     * Gets the search scope used to evaluate this dynamic group.
     * 
     * @return search scope for evaluating members of the group
     *
     * @supported.api
     */
    public int getSearchScope();
}
