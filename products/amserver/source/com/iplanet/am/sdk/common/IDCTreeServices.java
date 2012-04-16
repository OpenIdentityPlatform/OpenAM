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
 * $Id: IDCTreeServices.java,v 1.2 2008/06/25 05:41:24 qcheng Exp $
 *
 */

package com.iplanet.am.sdk.common;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

import com.iplanet.am.sdk.AMException;

public interface IDCTreeServices {

    /**
     * Method to determine if DC Tree support is required or not.
     * 
     * @return true if DC Tree support required, false otherwise
     */
    public boolean isRequired() throws AMException, SSOException;

    /**
     * Return the Organization DN for a given domain.
     * <p>
     * For example, if the domain is <code>sun.com </code>, then the value of
     * the attribute <code> inetBaseDomainDN </code> in the DC tree node
     * representing <code> sun.com </code> is returned.
     * 
     * @param token
     *            User's single sign on token
     * @param domainName
     *            Name of the domain
     * @return <code> DN </code> of the entry.
     * @throws AMException
     *             if an error is encountered while trying to determine the
     *             Organization DN.
     */
    public String getOrganizationDN(SSOToken token, String domainName)
            throws AMException;
}
