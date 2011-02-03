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
 * $Id: DomainComponent.java,v 1.4 2008/06/25 05:41:47 qcheng Exp $
 *
 */

package com.iplanet.ums.dctree;

import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.ModSet;
import com.iplanet.services.util.I18n;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.Guid;
import com.iplanet.ums.IUMSConstants;
import com.iplanet.ums.PersistentObject;
import com.iplanet.ums.UMSException;
import com.iplanet.ums.UMSObject;

/**
 * Represents a domain component in the domain componet tree. Each Domain
 * Component object represent a node in the dctree and each dc may be associated
 * with an organization in the customer DIT (the convergence tree as noted in
 * the nortel specication).
 * 
 * @see DomainComponentTree
 * @supported.api
 */
public class DomainComponent extends PersistentObject {

    static final String[] dcObjectClasses = { "top", "domain", "inetDomain" };

    static final String DEFAULT_NAMING_ATTR = "dc";

    static final String TAG_ORG_LINK = "inetDomainBaseDN";

    static final String TAG_DOMAIN_STATUS = "inetDomainStatus";

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    /**
     * Default constructor
     */
    public DomainComponent() {
    }

    /**
     * Given a name for domain component, construct
     * the dc object in memory
     * 
     * @param dcName
     *            Domain Componet name
     * @supported.api
     */
    public DomainComponent(SSOToken token, String dcName) throws UMSException {
        setAttribute(new Attr("objectclass", dcObjectClasses));
        setAttribute(new Attr("dc", dcName));
        try {
            SSOTokenManager.getInstance().validateToken(token);
        } catch (SSOException se) {
            throw new UMSException(i18n.getString(IUMSConstants.INVALID_TOKEN),
                    se);
        }
        m_token = token;
    }

    /**
     * Sets the mapping of a dc entry in the dctree to an
     * organization or OrganizationalUnit entry in the convergence DIT, the
     * directory tree hosting all the actual entries
     * 
     * @param org
     *            Organization entry to be associated with the dc entry in the
     *            dctree
     * @supported.api
     */
    public void setAssociatedOrganization(PersistentObject org)
            throws UMSException {
        setAssociatedOrganization(org.getGuid());
    }

    /**
     * Sets the mapping of a dc entry in the dctree to an
     * organization or organizational unit entry in the convergence DIT, the
     * directory tree hosting all the actual entries.
     * 
     * @param orgGuid Identifier for organization.
     * @throws UMSException if mapping of dc entry cannot be set.
     * @supported.api
     */
    public void setAssociatedOrganization(Guid orgGuid) throws UMSException {
        if (orgGuid == null || orgGuid.getDn().length() == 0) {
            throw new IllegalArgumentException();
        }

        modify(new Attr(TAG_ORG_LINK, orgGuid.getDn()), ModSet.REPLACE);

        // Only save if it is already a persistent object
        //
        if (isPersistent()) {
            save();
        }
    }

    /**
     * Get the domain mapping from this dc entry to the
     * organization entry in the customer DIT. Return the organization object
     * associated with thic dc entry
     * 
     * @return PersistentObject representing the organization entry associated
     *         with dc entry
     * @supported.api
     */
    public PersistentObject getOrganization() throws UMSException {

        Guid orgGuid = getAssociatedOrganizationGuid();
        return UMSObject.getObject(getSSOToken(), orgGuid);
    }

    /**
     * Get the domain mapping from this dc entry. Domain
     * mapping is in terms of DN
     * 
     * @return identifier for the domain mapping associated with this dc entry
     * @supported.api
     */
    public Guid getAssociatedOrganizationGuid() throws UMSException {

        Attr attr = getAttribute(TAG_ORG_LINK);

        if (attr == null) {
            return null;
        }

        return (new Guid(attr.getValue()));
    }

    /**
     * Get the domain status in the dc entry
     * 
     * @return Domain status in the dc entry
     * @supported.api
     */
    public String getDomainStatus() throws UMSException {
        Attr attr = getAttribute(TAG_DOMAIN_STATUS);

        if (attr == null) {
            return null;
        }

        return attr.getValue();
    }

    /**
     * Set the domain status in the dc entry
     * 
     * @param status
     *            Domain status to be set
     * @supported.api
     */
    public void setDomainStatus(String status) throws UMSException {
        modify(new Attr(TAG_DOMAIN_STATUS, status), ModSet.REPLACE);

        // Only save if it is already a persistent object
        //
        if (isPersistent()) {
            save();
        }
    }

    /**
     * Get the naming attribute for the dc entry
     * @supported.api
     */
    public String getNamingAttribute() {
        return (DEFAULT_NAMING_ATTR);
    }

    /**
     * get the SSO Token
     * 
     * @return SSOToken authenticated SSO token
     */
    SSOToken getSSOToken() {
        return m_token;
    }

    /**
     * set the SSO Token
     */
    void setSSOToken(SSOToken token) throws UMSException {
        try {
            SSOTokenManager.getInstance().validateToken(token);
        } catch (SSOException se) {
            throw new UMSException(i18n.getString(IUMSConstants.INVALID_TOKEN),
                    se);
        }
        m_token = token;
    }

    private SSOToken m_token = null;

}
