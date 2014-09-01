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
 * $Id: DomainComponentTree.java,v 1.5 2009/01/28 05:34:51 ww203982 Exp $
 *
 */

package com.iplanet.ums.dctree;

import java.util.Hashtable;
import java.util.StringTokenizer;

import com.sun.identity.shared.ldap.LDAPDN;

import com.iplanet.services.util.I18n;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.EntryNotFoundException;
import com.iplanet.ums.Guid;
import com.iplanet.ums.IUMSConstants;
import com.iplanet.ums.PersistentObject;
import com.iplanet.ums.SearchResults;
import com.iplanet.ums.UMSException;
import com.iplanet.ums.UMSObject;
import com.iplanet.ums.User;

/**
 * Represents the domain component index tree (dctree). A domain component tree
 * is used to represent virtual domains as used in DNS. DCTree composes of a
 * hiearchical tree of domain components (dc) and each dc node may/maynot
 * associate with a organizational DIT (convergence tree as noted in nortel
 * spec). Sample of a dctree that starts at dcroot of "o=internet" will look
 * like this
 * <p>
 * 
 * <pre>
 *               o=internet
 *                   |
 *              ------------------------------
 *             |             |                |
 *           dc=com        dc=net           dc=edu
 *              |
 *         ---------
 *         |        |
 *       dc=sun    dc=iplanet
 *         |            |
 *       dc=eng       dc=red
 * </pre>
 * 
 * DomainComponentTree allows the user to create a dc tree capturing virtual
 * domain names in a network (hosted or enterprise) environment with each low
 * level dc node being mapped to an organizational DIT.
 * 
 * @see DomainComponent
 * @supported.api
 */
public class DomainComponentTree {

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    /**
     * Default constructor
     */
    public DomainComponentTree() {
    }

    /**
     * Constructs a <code>DomainComponentTree</code> with an authenticated
     * prinicipal and an identification of the root of the dc index tree.
     * 
     * @param token Single sign on token of authenticated principal with
     *        priviledge for accessing the domain component index tree (dctree).
     * @param dcRoot Identification of root, a DN, of the dc tree such as
     *        <code>o=internet</code>.
     * @throws InvalidDCRootException if invalid root specification.
     * @throws UMSException if other read error occurred.
     * @supported.api
     */
    public DomainComponentTree(SSOToken token, Guid dcRoot)
            throws InvalidDCRootException, UMSException {
        if (token == null)
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.NULL_TOKEN));
        try {
            SSOTokenManager.getInstance().validateToken(token);
        } catch (SSOException se) {
            throw new UMSException(i18n.getString(IUMSConstants.INVALID_TOKEN),
                    se);
        }

        setSSOToken(token);

        try {
            setDCRoot(dcRoot);
        } catch (EntryNotFoundException e) {
            throw new InvalidDCRootException(dcRoot.getDn(), e.getRootCause());
        }

    }

    /**
     * Sets the authenticated principal used for access to directory server
     * 
     * @param ssotoken
     *            SSO token for authenticated user
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

    /**
     * Gets the authenticated Principal used to contruct the dctree instance
     * 
     * @return authenticated principal that is associated with dctree
     *         construction
     * 
     * java.security.Principal getPrincipal() { if (token) return
     * token.getPrincipal(); else return null; }
     */

    /**
     * Gets the SSOToken used to contruct the dctree instance
     * 
     * @return SSOToken that is associated with dctree construction
     */
    SSOToken getSSOToken() {
        return m_token;
    }

    /**
     * Sets the root of the domain component tree (dc
     * tree). Needs an established authenticated principal before setting
     * dcroot.
     * 
     * @param root
     *            Identification of the root of the tree such as o=internet
     * @supported.api
     */
    public void setDCRoot(Guid root) throws UMSException {
        SSOToken token = getSSOToken();
        try {
            SSOTokenManager.getInstance().validateToken(token);
        } catch (SSOException se) {
            throw new UMSException(i18n.getString(IUMSConstants.INVALID_TOKEN),
                    se);
        }

        if (token != null) {
            m_dcRoot = UMSObject.getObject(token, root);
        }
    }

    /**
     * Gets the root of the domain component tree (dc
     * tree)
     * 
     * @return PersistentObject representing the dctree root in the dctree DIT
     * @supported.api
     */
    public PersistentObject getDCRoot() {
        return m_dcRoot;
    }

    /**
     * Add a virtual domain into the domain component
     * tree.
     * 
     * @param domain
     *            Fully qualified domain name
     * @return Domain Componet entry just added to the dctree
     * @throws InvalidDCRootException
     *             if dcroot is not defined
     * @throws UMSException
     *             for write problem in adding domain to dctree
     * @supported.api
     */
    public DomainComponent addDomain(String domain) throws UMSException {
        if (domain == null || domain.length() == 0) {
            throw new IllegalArgumentException();
        }

        if (m_dcRoot == null) {
            throw new InvalidDCRootException();
        }

        StringTokenizer st = new StringTokenizer(domain, ".");

        int nDoms = st.countTokens();
        String[] doms = new String[nDoms];

        int i = 0;
        while (st.hasMoreElements()) {
            doms[i++] = st.nextToken();
        }

        PersistentObject parent = UMSObject.getObject(getSSOToken(), m_dcRoot
                .getGuid());

        // Going from right to left on the virtual domain name
        // to go through all the domain components (dc). Make sure that
        // all dc entries are created in the dctree
        // e.g. adding a domain for eng.sun.com with dcRoot being o=internet
        // will yield
        // <pre>
        // o=internet (assmumed to exist)
        // dc=com,o=internet (created)
        // dc=sun,dc=com,o=ineternet (created)
        // dc=eng,dc=sun,dc=com,o=internet (created)
        // </pre>
        // in the domain component tree
        //
        DomainComponent dc = null;
        for (i = 0; i < nDoms; i++) {

            SearchResults results = parent.getChildren("dc="
                    + doms[nDoms - i - 1], null);

            try {
                dc = (DomainComponent) results.assertOneEntry();
            } catch (EntryNotFoundException e) {
                dc = new DomainComponent(getSSOToken(), doms[nDoms - i - 1]);
                parent.addChild(dc);
            }

            parent = UMSObject.getObject(getSSOToken(), dc.getGuid());
        }

        return dc;
    }

    /**
     * Remove a virtual domain in the dctree
     * 
     * @param domain
     *            Virtual domain name to be removed
     * @throws UMSException
     *             upon failure to remove the corresponding dc entry in the
     *             dctree
     * 
     * @supported.api
     */
    public void removeDomain(String domain) throws UMSException {
        if (m_dcRoot == null)
            return;

        DomainComponent dc = getDomainComponent(domain);
        m_dcRoot.removeChild(dc);
    }

    /**
     * Set the domain mapping so that the dc entry maps to
     * an organization in the the organization DIT hosting user data (the
     * convergence tree in Nortel spec)
     * 
     * @param domain
     *            Fully qualified domain name
     * @param org
     *            Organization entry to be mapped from dctree to organization
     *            DIT (the convergence tree in nortel spec)
     * @throws DomainNotFoundException
     *             if domain id not defined
     * @throws UMSException
     *             upon write failure
     * @supported.api
     */
    public void setDomainMapping(String domain, PersistentObject org)
            throws UMSException {
        setDomainMapping(domain, org.getGuid());
    }

    /**
     * Set the domain mapping so that the dc entry maps to
     * an organization in the convergence tree.
     * 
     * @param domain Virtual domain name.
     * @param orgGuid Identifiication of Organization entry to be mapped from
     *        dctree to organization DIT (the convergence tree in nortel spec).
     * @throws UMSException if write failed.
     * @supported.api
     */
    public void setDomainMapping(String domain, Guid orgGuid)
            throws UMSException {
        DomainComponent dc = getDomainComponent(domain);
        dc.setAssociatedOrganization(orgGuid);
    }

    /**
     * Sets the domain status for a given virtual domain
     * 
     * @param domain
     *            Virtual domain name
     * @param status
     *            Domain status to be set
     * @throws DomainNotFoundException
     *             if domain is not found in dctree
     * @throws UMSException
     *             upon write failure
     * 
     * @supported.api
     */
    public void setDomainStatus(String domain, String status)
            throws DomainNotFoundException, UMSException {
        DomainComponent dc = getDomainComponent(domain);
        dc.setDomainStatus(status);
    }

    /**
     * Gets the domain status of a given virtual domain
     * 
     * @param domain
     *            Virtual domain name
     * @return Domain status for the given domain
     * @throws DomainNotFoundException
     *             if domain not found in dctree
     * @throws UMSException
     *             upon read failure
     * @supported.api
     */
    public String getDomainStatus(String domain)
            throws DomainNotFoundException, UMSException {
        DomainComponent dc = getDomainComponent(domain);
        return dc.getDomainStatus();
    }

    /**
     * Given a fully qualified domain name, maps it to the
     * corresponding DN in the DCtree
     * 
     * @param domain
     *            Fully qualified domain name
     * @return String representation of the Distinguished Name in the DC Tree
     * @supported.api
     */
    public String mapDomainToDN(String domain) {
        StringTokenizer st = new StringTokenizer(domain, ".");
        String dn = new String();

        while (st.hasMoreElements()) {
            dn = dn + "dc=" + st.nextToken() + ",";
        }

        dn = dn + getDCRoot().getDN();
        return dn;
    }

    /**
     * Given a virtual domain name such as
     * "javasoft.sun.com", returns the domain component entry in the dc index
     * tree. This entry lives under dc index tree and one can use the dc entry
     * to get to the organization assoicated with the dc tree
     * 
     * @param domain
     *            Virtual domain name such as "javasoft.sun.com"
     * @return Domain componet entry representing the virtual domain in the
     *         domain component tree
     * @throws DomainNotFoundException
     *             if given domain is not found in the dctree
     * @throws UMSException
     *             upon read error
     * @supported.api
     */
    public DomainComponent getDomainComponent(String domain)
            throws DomainNotFoundException, UMSException {

        String dn = mapDomainToDN(domain);

        try {
            DomainComponent dc = (DomainComponent) UMSObject.getObject(
                    getSSOToken(), new Guid(dn));
            dc.setSSOToken(getSSOToken());
            return dc;
        } catch (EntryNotFoundException e) {
            throw new DomainNotFoundException(domain, e.getRootCause());
        }

    }

    /**
     * Given a virtual domain name such as
     * "javasoft.sun.com", return the organization, organizationalunit or any
     * DIT entry that is assoicated from the domain compoent tree (dctree) to
     * the customer oranization DIT (the convergence tree as outlined in nortel
     * spec)
     * 
     * @param domain
     *            Fully qualified virtual domain name
     * @return Entry referred in the dc tree.
     * @throws DomainNotFoundException
     *             if domain is not found
     * @throws UMSException
     *             for reading problem in instantiating the mapped organization
     * @supported.api
     */
    public PersistentObject getOrganization(String domain)
            throws DomainNotFoundException, UMSException {
        DomainComponent dc = getDomainComponent(domain);
        return dc.getOrganization();
    }

    /**
     * Given a uid for a user, lookup the user under a
     * specified virtual domain name. For example,
     * 
     * <pre>
     * DomainComponentTree dctree = new DomainComponentTree(ctx, 
     *                                      &quot;red.iplanet.com&quot;);
     * 
     * User user = dctree.getUser(&quot;hman&quot;, 
     *                                      &quot;red.iplanet.com&quot;);
     * </pre>
     * 
     * @param uid
     *            User id for the entry to be searched
     * @param domain
     *            Fully qualified domain name such as "red.iplanet.com"
     * @return User object found
     * @throws DomainNotFoundException
     *             if domain is not found
     * @throws UMSException
     *             upon failure in instantiating the user object
     * @supported.api
     */
    public User getUser(String uid, String domain)
            throws DomainNotFoundException, UMSException {
        return getUser("uid", uid, domain);
    }

    /**
     * Given identification of a user with a naming
     * attribute and value, lookup the user under a virtual domain specified.
     * For example,
     * 
     * <pre>
     * DomainComponentTree dctree = new DomainComponentTree(ctx,
     *                                           &quot;red.iplanet.com&quot;);
     * 
     * User user = dctree.getUser(&quot;cn&quot;, &quot;Hin Man&quot;, 
     *                                             &quot;red.iplanet.com&quot;);
     * </pre>
     * 
     * @param namingAttribute
     *            Naming attribute for the user object such as "uid" or "mail".
     *            The naming attribute has to provide a unique identifier for
     *            the user.
     * @param value
     *            attribute value for the naming attribute
     * @param domain
     *            Fully qualified domain name such as "red.iplanet.com"
     * @return User object if found
     * @throws DomainNotFoundException
     *             if domain is not found
     * @throws UMSException
     *             upon failure in instantiating the user object
     * @supported.api
     */
    public User getUser(String namingAttribute, String value, String domain)
            throws DomainNotFoundException, UMSException {
        PersistentObject orgEntry = getOrganization(domain);

        SearchResults result = orgEntry.search(namingAttribute + "=" + value,
                null);

        return (User) result.assertOneEntry();
    }

    /**
     * Given a domain componet in a dctree, maps it to a
     * virtual domain name
     * 
     * @param dc
     *            A domain component that lives in the dctree
     * @return Fully qualified domain name
     * @supported.api
     */
    public String mapDCToDomainName(DomainComponent dc) {

        if (m_dcRoot == null)
            return null;

        String rootDN = LDAPDN.normalize(m_dcRoot.getDN());
        String dcDN = LDAPDN.normalize(dc.getDN());

        // Skip the dcRoot part of the DN for the given domain component.
        // Find the position of the rootDN in dcDN
        //
        int end = dcDN.indexOf("," + rootDN);

        // TODO: Kind of kludgy, need to revisit
        //
        dcDN = dcDN.substring(0, end);

        String[] doms = LDAPDN.explodeDN(dcDN, true);
        String domainName = doms[0];

        // Compose the fully qualified domain name with the "." character
        //
        for (int i = 1; i < doms.length; i++) {
            domainName = domainName + "." + doms[i];
        }

        return domainName;
    }

    /**
     * Get all virtual domains present in the dctree.
     * Construct a hashtable of the found domain names and their associated
     * organization in the customer organizational DIT (the convergence tree)
     * <p>
     * 
     * This function can be used as a cache function for the complete DCTree.
     * The returning hastable provides all the virtual domain name as keys that
     * maps to organization mapping linked in the domain component dc nodes
     * <p>
     * 
     * @return Hashtable of domain names and associated organizations. Each
     *         domain name is associated with one organization but muliple
     *         domain names can map to the same organization in the customer
     *         DIT.
     * @throws UMSException
     *             upon failure in searching all mapped domains
     * @supported.api
     */
    public Hashtable getChildDomainIDs() throws UMSException {

        if (m_dcRoot == null) {
            return null;
        }

        // Search in DCTree that have a link to a mapped organization
        //
        SearchResults results = m_dcRoot.search(
                "(&(objectclass=inetDomain)(inetDomainBaseDN=*))", null);

        Hashtable domains = new Hashtable();

        // For each domain found, recapture the domain name from the DC hiearchy
        // such as
        // dc=red,dc=iplanet,dc=com -> red.iplanet.com
        // dc=eng,dc=sun,dc=com -> eng.sun.com
        // and put in the hashtable the corresponding organization mapped from
        // dctree to customer organization DIT (the convergence tree in nortel
        // spec)
        //
        while (results.hasMoreElements()) {
            DomainComponent dc = (DomainComponent) results.next();
            String domainName = mapDCToDomainName(dc);
            domains.put(domainName, dc.getAssociatedOrganizationGuid().getDn());
        }

        return domains;
    }

    private PersistentObject m_dcRoot = null;

    private SSOToken m_token = null;
}
