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
 * $Id: DCTreeServicesImpl.java,v 1.5 2009/01/28 05:34:48 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk.ldap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.ldap.util.DN;

import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.ldap.ModSet;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.Guid;
import com.iplanet.ums.PersistentObject;
import com.iplanet.ums.SearchControl;
import com.iplanet.ums.SearchResults;
import com.iplanet.ums.UMSException;
import com.iplanet.ums.UMSObject;
import com.iplanet.ums.dctree.DomainComponent;
import com.iplanet.ums.dctree.DomainComponentTree;
import com.iplanet.ums.dctree.InvalidDCRootException;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMSDKBundle;
import com.iplanet.am.sdk.common.DCTreeServicesHelper;
import com.iplanet.am.sdk.common.IDCTreeServices;
import com.sun.identity.shared.debug.Debug;

/**
 * This class <code>DCTree</code> contains the functionality to support a DC
 * Tree in the LDAP DIT. The methods of this class will be used by other classes
 * in com.iplanet.dpro.sdk package.
 * <p>
 * 
 * In order to determine if DC Tree support is required or not, the parameter
 * <code>com.iplanet.am.dctree</code> will be verified. A value of
 * <code>true</code> for this parameter, means DC Tree support is required
 * <p>
 * 
 * NOTE: An explicit check must be performed using DCTree.isRequired() method
 * before calling any other methods in this class.
 */
public class DCTreeServicesImpl extends DCTreeServicesHelper implements
        AMConstants, IDCTreeServices {

    private static Map domainMap = new HashMap();

    private static Map canonicalDomainMap = new HashMap();

    private static Debug debug = CommonUtils.debug;

    /**
     * Public default constructor
     * 
     */
    public DCTreeServicesImpl() {
        super();
    }

    /**
     * Method which creates a <Code>Domain Component Tree </Code> for the given
     * organization, if the <code>sunPreferredDomain</code> attribute is
     * present and has a fully qualified domain name as value.
     * 
     * @param token
     *            SSO Token
     * @param orgGuid
     *            identifiication of organization entry to be mapped from 
     *            <Code>dctree</Code> to organization DIT organization
     * @param attrSet
     *            the attributes to be set on creation of domain.
     * 
     * @exception AMException
     *                if unsuccessful in creating a dc tree for the organization
     *                or unsuccessful in setting the mapping between dc tree and
     *                the organization
     */
    protected void createDomain(SSOToken token, Guid orgGuid, AttrSet attrSet)
            throws AMException, SSOException {
        if (DCTREE_START_DN == null) {
            throw new AMException(AMSDKBundle.getString("355"), "355");
        }

        // Create a DC tree is value is specified for
        // sunPreferredDomain attribute
        String domainName = attrSet.getValue(IPLANET_DOMAIN_NAME_ATTR);
        // remove the attribute from the attribute set.
        attrSet.remove(IPLANET_DOMAIN_NAME_ATTR);
        if ((domainName != null) && (!domainName.equals(""))) {
            try {
                DomainComponentTree dcTree = new DomainComponentTree(token,
                        new Guid(DCTREE_START_DN));
                dcTree.addDomain(domainName);
                // Set the domain mapping
                dcTree.setDomainMapping(domainName, orgGuid);
                String status = attrSet.getValue(INET_DOMAIN_STATUS_ATTR);
                if (status != null) {
                    dcTree.setDomainStatus(domainName, status);
                }
                AttrSet[] attrSetArray = splitAttrSet(orgGuid.getDn(), attrSet);
                if (attrSetArray[1] != null) {
                    setDomainAttributes(token, orgGuid.getDn(), 
                            attrSetArray[1]);
                }
            } catch (InvalidDCRootException ie) {
                debug.error("DCTree.createDomain(): ", ie);
                throw new AMException(AMSDKBundle.getString("343"), "343");
            } catch (UMSException ue) {
                debug.error("DCTree.createDomain(): ", ue);
                throw new AMException(AMSDKBundle.getString("344"), "344");
            }
        }
    }

    /**
     * Method which creates a DC Tree for the given org, if the
     * <code>sunPreferredDomain</code> attribute is present and has a fully
     * qualified domain name as value.
     * 
     * @param token
     *            SSOToken
     * @param orgGuid
     *            identifiication of Organization entry to be mapped from dctree
     *            to organization DIT organization
     * @param domainName
     *            set the domain this organization belongs to.
     * @param attrSet
     *            the AttrSet of the organization
     * 
     * @exception AMException
     *                if unsuccessful in creating a dc tree for the organization
     *                or unsuccessful in setting the mapping between dc tree and
     *                the organization
     */
    protected void createDomain(SSOToken token, Guid orgGuid,
            String domainName, AttrSet attrSet) throws AMException {
        if (DCTREE_START_DN == null) {
            throw new AMException(AMSDKBundle.getString("355"), "355");
        }

        // Create a DC tree for specified domain.
        if ((domainName != null) && (!domainName.equals(""))) {
            try {
                DomainComponentTree dcTree = new DomainComponentTree(token,
                        new Guid(DCTREE_START_DN));
                dcTree.addDomain(domainName);
                // Set the domain mapping
                dcTree.setDomainMapping(domainName, orgGuid);
                String status = attrSet.getValue(INET_DOMAIN_STATUS_ATTR);
                if (status != null) {
                    dcTree.setDomainStatus(domainName, status);
                }
            } catch (InvalidDCRootException ie) {
                debug.error("DCTree.createDomain(): ", ie);
                throw new AMException(AMSDKBundle.getString("343"), "343");
            } catch (UMSException ue) {
                debug.error("DCTree.createDomain(): ", ue);
                throw new AMException(AMSDKBundle.getString("344"), "344");
            }
        }
    }

    /**
     * Method which removes the DC Tree corresponding to the Org
     * 
     * @param token
     *            SSOToken
     * @param orgDN
     *            String representing the DN correponding to the organization
     * 
     * @exception AMException
     *                if error occured in accessing the org corresponding to
     *                orgDN or during the removal of the dc tree corresponding
     *                to the orgDN
     */
    protected void removeDomain(SSOToken token, String orgDN)
            throws AMException {

        // String orgAttribute[] = {IPLANET_DOMAIN_NAME_ATTR};
        try {
            PersistentObject po = UMSObject.getObject(token, new Guid(orgDN));
            if (!(po instanceof com.iplanet.ums.Organization)) {
                if (debug.messageEnabled()) {
                    debug.message("DCTree.removeDomain-> " + orgDN
                            + " is not an organization");
                }
                return;
            }
            String domainName = getCanonicalDomain(token, orgDN);
            if (debug.messageEnabled()) {
                debug.message("DCTree.removeDomain-> "
                        + "Obtained canon domain " + domainName);
            }
            if ((domainName != null) && (domainName.length() > 0)) {
                DomainComponentTree dcTree = new DomainComponentTree(token,
                        new Guid(DCTREE_START_DN));
                if (debug.messageEnabled()) {
                    debug.message("DCTree.removeDomain: removing domain: "
                            + domainName);
                }
                dcTree.removeDomain(domainName);
            } else {
                if (debug.warningEnabled()) {
                    debug.warning("DCTree.removeDomain(): "
                            + " unable to get domain for " + orgDN);
                }
            }
        } catch (UMSException ue) {
            if (debug.warningEnabled()) {
                debug.warning("DCTree.removeDomain(): ", ue);
            }
        }
    }

    /**
     * Method which update attribute inetdomainstatus of the DC Tree
     * corresponding to the Org
     * 
     * @param token
     *            SSOToken
     * @param orgDN
     *            String representing the DN correponding to the organization
     * @param status
     *            inetdomainstatus value
     * 
     * @exception AMException
     *                if error occured in accessing the org corresponding to
     *                orgDN or during the attribute change of the dc tree
     *                corresponding to the orgDN
     */
    protected void updateDomainStatus(SSOToken token, String orgDN,
            String status) throws AMException {
        try {
            String domainName = getCanonicalDomain(token, orgDN);
            if ((domainName != null) && (domainName.length() > 0)) {
                DomainComponentTree dcTree = new DomainComponentTree(token,
                        new Guid(DCTREE_START_DN));
                dcTree.setDomainStatus(domainName, status);
            } else {
                debug.warning("DCTree.updateDomainStatus(): value for "
                        + IPLANET_DOMAIN_NAME_ATTR + " attribute "
                        + "null or empty");
            }
            // }
        } catch (UMSException ue) {
            debug.error("DCTree.removeDomain(): ", ue);
            throw new AMException(AMSDKBundle.getString("356"), "356");
        }
    }

    protected void setDomainAttributes(SSOToken token, String orgDN,
            AttrSet attrSet) throws AMException {
        String domainName = null;
        try {
            domainName = getCanonicalDomain(token, orgDN);
            DomainComponentTree dcTree = new DomainComponentTree(token,
                    new Guid(DCTREE_START_DN));
            if (domainName == null) {
                if (debug.messageEnabled()) {
                    debug.message("DCTree.setDomainAttrs: "
                            + "No domain found for org : " + orgDN);
                }
                return;
            }
            DomainComponent dcNode = dcTree.getDomainComponent(domainName);
            if (attrSet != null) {
                if (debug.messageEnabled()) {
                    debug.message("DCTree.setDomainAttrs: "
                            + " setting attributes on domain " + domainName
                            + ": " + attrSet.toString());
                }
                Attr ocAttr = attrSet.getAttribute("objectclass");
                if (ocAttr != null) {
                    Attr oldOCAttr = dcNode.getAttribute("objectclass");
                    if (oldOCAttr != null) {
                        ocAttr.addValues(oldOCAttr.getStringValues());
                    }
                    if (debug.messageEnabled()) {
                        debug.message("DCTree.setDomainAttrs-> "
                                + "objectclasses to be set "
                                + ocAttr.toString());
                    }
                    if (ocAttr.size() == 0)
                        dcNode.modify(ocAttr, ModSet.DELETE);
                    else
                        dcNode.modify(ocAttr, ModSet.REPLACE);
                    dcNode.save();
                    attrSet.remove("objectclass");
                }
                int size = attrSet.size();
                for (int i = 0; i < size; i++) {
                    Attr attr = attrSet.elementAt(i);
                    if (attr.size() == 0) {
                        // remove attribute
                        dcNode.modify(attr, ModSet.DELETE);
                    } else {
                        // replace attribute
                        dcNode.modify(attr, ModSet.REPLACE);
                    }
                }
                dcNode.save();
            }
        } catch (UMSException umse) {
            debug.error("DCTree.setDomainAttributes: " + " error setting "
                    + " attribute for domain " + domainName, umse);
        }
    }

    protected String getDCNodeDN(SSOToken token, String orgDN)
            throws AMException {
        try {
            String domainName = getCanonicalDomain(token, orgDN);
            if (domainName != null) {
                DomainComponentTree dcTree = new DomainComponentTree(token,
                        new Guid(DCTREE_START_DN));
                String dcNodeDN = dcTree.mapDomainToDN(domainName);
                return CommonUtils.formatToRFC(dcNodeDN);
            } else {
                return null;
            }
        } catch (InvalidDCRootException e) {
            debug.error("DCTree.getDCNodeDN(): Invalid DC root ", e);
            throw new AMException(AMSDKBundle.getString("343"), "343");
        } catch (UMSException e) {
            debug.error("DCTree.getDCNodeDN(): Unable to get dc node dn "
                    + "for: " + orgDN, e);
            throw new AMException(AMSDKBundle.getString("344"), "344");
        }
    }

    protected AttrSet getDomainAttributes(SSOToken token, String orgDN,
            String[] attrNames) throws AMException, SSOException {
        String domainName = null;
        try {
            AttrSet domAttrSet;
            domainName = getCanonicalDomain(token, orgDN);
            if (domainName == null) {
                debug.error("DCTree.getDomainAttributes-> "
                        + "Domain not found for:  " + orgDN);
                return null;
            }
            DomainComponentTree dcTree = new DomainComponentTree(token,
                    new Guid(DCTREE_START_DN));
            DomainComponent dcNode = dcTree.getDomainComponent(domainName);
            if (attrNames != null) {
                domAttrSet = dcNode.getAttributes(attrNames);
            } else {
                domAttrSet = dcNode.getAttributes(dcNode.getAttributeNames());
            }
            AttrSet[] attrArray = splitAttrSet(null, domAttrSet);
            return attrArray[1];
        } catch (UMSException umse) {
            debug.error("DCTree.getDomainAttributes: "
                    + " error getting attributes for domain " + domainName);
        }
        return null;
    }

    /**
     * Returns the organization DN matching the domain name
     * 
     * @param token
     *            SSOToken
     * @param domainName
     *            String representing domin name
     * @return
     *            the organization dn
     * @throws AMException
     */
    public String getOrganizationDN(SSOToken token, String domainName)
            throws AMException {
        try {
            DomainComponentTree dcTree = new DomainComponentTree(token,
                    new Guid(DCTREE_START_DN));
            Hashtable domainToOrgTable = dcTree.getChildDomainIDs();
            if (debug.messageEnabled()) {
                debug.message("DCTree:getOrgDN-> domain=" + domainName);
            }
            return ((String) domainToOrgTable.get(domainName));
        } catch (UMSException umse) {
            // Deepa: Is there a localized property for 1000?
            debug.error("DCTree:getOrganizationDN: " + "UMS Exception: ", umse);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }
    }

    protected AttrSet[] splitAttrSet(String orgDN, AttrSet attrSet)
            throws AMException, SSOException {
        AttrSet attrArray[] = new AttrSet[2];
        attrArray[0] = (attrSet != null) ? (AttrSet) attrSet.clone()
                : new AttrSet();
        attrArray[1] = new AttrSet();
        if (attrSet == null) {
            return (attrArray);
        }
        Set dcNodeAttrs = dcNodeAttributes();
        Iterator it = dcNodeAttrs.iterator();
        while (it.hasNext()) {
            String aName = (String) it.next();
            if (aName.indexOf("objectclass=") > -1) {
                Attr attr0 = attrSet.getAttribute("objectclass");
                Attr attr = (attr0 != null) ? (Attr) attr0.clone() : null;
                String oc = aName.substring("objectclass=".length());
                Attr dcAttr = new Attr("objectclass");
                if (attr != null && attr.contains(oc)) {
                    attr.removeValue(oc);
                    dcAttr.addValue(oc);
                    attrArray[0].replace(attr);
                    attrArray[1].add(dcAttr);
                }
            } else {
                Attr attr = attrSet.getAttribute(aName);
                if (attr != null) {
                    attrArray[1].add(attr);
                    attrArray[0].remove(aName);
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message("DCTreeServicesImpl.splitAttrSet: "
                    + "domain attrset = " + attrArray[1].toString());
            debug.message("DCTreeServicesImpl.splitAttrSet: "
                    + "non-domain attrset = " + attrArray[0].toString());
        }
        return attrArray;
    }

    /**
     * This is a public method to get canonical domain name for a given
     * organization.
     */

    protected String getCanonicalDomain(SSOToken token, String orgDN)
            throws AMException {
        String canonOrgDN = (new DN(orgDN)).toRFCString().toLowerCase();
        /*
         * if (AMCacheManager.isCachingEnabled()) { returnDomain = (String)
         * canonicalDomainMap.get(canonOrgDN); if (returnDomain != null) {
         * return returnDomain; } }
         */
        return updateCacheAndReturnDomain(token, canonOrgDN);
    }

    /**
     * This is a public method used by the notification event listener thread to
     * clean the domain map, when organization entry is changed.
     * 
     * @param canonOrgDN
     *            organization DN
     */
    public void cleanDomainMap(String canonOrgDN) {
        synchronized (canonicalDomainMap) {
            canonicalDomainMap.remove(canonOrgDN);
        }
        synchronized (domainMap) {
            domainMap.remove(canonOrgDN);
        }
    }

    /**
     * This is a private method to update cache
     */
    private String updateCacheAndReturnDomain(SSOToken token, String canonOrgDN)
            throws AMException {
        try {
            DomainComponentTree dcTree = new DomainComponentTree(token,
                    new Guid(DCTREE_START_DN));

            SearchControl scontrol = new SearchControl();
            scontrol.setSearchScope(SearchControl.SCOPE_SUB);
            PersistentObject po = UMSObject.getObject(token, new Guid(
                    DCTREE_START_DN));
            String searchFilter = "(inetDomainBaseDN=" + canonOrgDN + ")";
            if (debug.messageEnabled()) {
                debug.message("DCTree.updateCache-> " + "searchFilter= "
                        + searchFilter);
            }
            SearchResults results = po.search(searchFilter, null);

            int count = 0;
            String domainName = null;
            String canonDomain = null;
            while (results.hasMoreElements()) {
                DomainComponent dcNode = (DomainComponent) results.next();
                count++;
                domainName = dcTree.mapDCToDomainName(dcNode);
                if (debug.messageEnabled()) {
                    debug.message("DCTree:updateCache-> " + "domainName= "
                            + domainName);
                }
                Attr isCanonical = dcNode.getAttribute(INET_CANONICAL_DOMAIN);
                if (isCanonical != null) {
                    /*
                     * if (AMCacheManager.isCachingEnabled()) {
                     * synchronized(canonicalDomainMap) {
                     * canonicalDomainMap.put(canonOrgDN, domainName); } }
                     */
                    canonDomain = domainName;
                }
                /*
                 * if (AMCacheManager.isCachingEnabled()) {
                 * synchronized(domainMap) { domainMap.put(canonOrgDN,
                 * domainName); } }
                 */
            }
            results.abandon();
            if (count == 1) {
                canonDomain = domainName;
                /*
                 * if (AMCacheManager.isCachingEnabled()) {
                 * canonicalDomainMap.put(canonOrgDN, domainName); }
                 */
            }
            if (debug.messageEnabled()) {
                debug.message("DCTree.updateCache-> " + "returning domain= "
                        + canonDomain);
            }
            return canonDomain;

        } catch (UMSException umse) {
            debug.error("DCTree:updateCache: UMSException", umse);
            return null;

        }

    }

    public Set dcNodeAttributes() throws AMException, SSOException {

        if (!isInitalized()) {
            initialize();
        }

        Map attrMap = getAdminServiceGlobalSchema().getAttributeDefaults();
        Set values = (Set) attrMap.get(DCT_ATTRIBUTE_LIST_ATTR);
        if (values == null) {
            if (debug.messageEnabled()) {
                debug.message("DCTree.dcNodeAttributes = null");
            }
            return Collections.EMPTY_SET;
        } else {
            if (debug.messageEnabled()) {
                debug.message("DCTree.dcNodeAttributes = " + values.toString());
            }
            return values;
        }
    }

}
