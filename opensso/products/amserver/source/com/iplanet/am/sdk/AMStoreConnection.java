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
 * $Id: AMStoreConnection.java,v 1.13 2009/01/28 05:34:47 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk;

import com.iplanet.am.sdk.common.IDirectoryServices;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchemaManager;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;


/**
 * The <code>AMStoreConnection</code> class represents a connection to the Sun
 * Java System Access Manager data store. It provides methods to create, remove
 * and get different type of Sun Java System Access Manager SDK objects in the
 * data tore. <code>AMStoreConnection</code> controls and manages access to
 * the data store.
 * <p>
 * An instance of <code>AMStoreConnection</code> object should always be
 * obtained by anyone using the AM SDK since this object is the entry point to
 * all other AM SDK managed objects. The constructor takes the SSO token of the
 * user. Here is some sample code on how to get a user's attributes, using AM
 * SDK:
 * 
 * <PRE>
 * 
 * AMStoreConnection amsc = new AMStoreConnection(ssotoken); AMUser user =
 * amsc.getUser(ssotoken.getPrincipal()); Map attributes = user.getAttributes();
 * 
 * </PRE>
 * 
 * <p>
 * <code>AMStoreConnection</code> also has other helper methods which are very
 * useful. Some examples below:
 * 
 * <PRE>
 * 
 * int otype = amsc.getAMObjectType(fullDN);
 * 
 * </PRE>
 * 
 * <p>
 * <code>otype</code> returned is one of the managed <code>AMObject</code>
 * types, like <code>AMObject.USER</code>, <code>AMObject.ROLE</code>,
 * <code>AMObject.ORGANIZATION</code>. If the entry being checked in not of
 * the type managed by AM SDK, then an <code>AMException</code> is thrown.
 * 
 * <PRE>
 * 
 * boolean exists = amsc.isValidEntry(fullDN);
 * 
 * </PRE>
 * 
 * <p>
 * If there is a <code>fullDN</code> that you want to know if it exists or not
 * in the data store, then use the above method. The typical use of this method
 * is in the case when you know that you need to get a managed object from
 * <code>amsc</code>, but you want to verify that it exists before you create
 * the managed object instance:
 * 
 * <PRE>
 * 
 * if (amsc.isValidEntry(userDN)) { AMUser user = amsc.getUser(userDN); - More
 * code here - }
 * 
 * </PRE>
 * 
 * <p>
 * Helper method <code>getOrganizationDN()</code>: Use this method to perform
 * a subtree scoped search for organization,based on various attribute values.
 * 
 * <PRE>
 * 
 * String orgDN = amsc.getOrganizationDN("sun.com", null);
 * 
 * </PRE>
 * 
 * <p>
 * The above method will return the DN of a organization, which matches the
 * search criterias of having either domain name of <code>sun.com</code>,
 * Domain alias name of <code>sun.com</code> or it's naming attribute value is
 * <code>sun.com</code>. More examples of how to use this method are provided
 * in the Javadocs of the method below.
 * 
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public final class AMStoreConnection implements AMConstants {
    // ~ Static fields/initializers
    // ---------------------------------------------

    public static String rootSuffix;

    protected static String defaultOrg;

    protected static Map orgMapCache = new AMHashMap();

    protected static Debug debug = AMCommonUtils.debug;

    static {
    }

    // ~ Instance fields
    // --------------------------------------------------------

    private IDirectoryServices dsServices;

    private SSOToken token;

    private String locale = "en_US";

    // ~ Constructors
    // -----------------------------------------------------------

    /**
     * Gets the connection to the Sun Java System Access Manager data store if
     * the Session is valid.
     * 
     * @param ssoToken
     *            a valid SSO token object to authenticate before getting the
     *            connection
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public AMStoreConnection(SSOToken ssoToken) throws SSOException {
        // initialize whatever you want to here.
        SSOTokenManager.getInstance().validateToken(ssoToken);
        this.token = ssoToken;
        this.locale = AMCommonUtils.getUserLocale(ssoToken);
        dsServices = AMDirectoryAccessFactory.getDirectoryServices();
    }

    // ~ Methods
    // ----------------------------------------------------------------

    /**
     * Returns the root suffix for user management node.
     * 
     * @return root suffix for user management node.
     *
     */
    public static String getAMSdkBaseDN() {
        defaultOrg = rootSuffix = 
            com.sun.identity.common.DNUtils.normalizeDN(
                SMSEntry.getAMSdkBaseDN());
        if (debug.messageEnabled()) {
            debug.message("AMStoreConnection:getAMSdkBaseDN():rootsuffix " + 
                rootSuffix);
        }
        if (debug.messageEnabled()) {
            debug.message("default org: " + defaultOrg);
            debug.message("AMStoreConnection:getAMSdkBaseDN():default org " + 
                defaultOrg);
        }
        return defaultOrg;
    }

    /**
     * Returns the filtered role naming attribute.
     * 
     * @return filtered role naming attribute
     * @deprecated This method is deprecated. Use
     *             {@link #getNamingAttribute(int) 
     *             getNamingAttribute(int objectType)}
     */
    public static String getFilteredRoleNamingAttribute() {
        return AMNamingAttrManager.getNamingAttr(AMObject.FILTERED_ROLE);
    }

    /**
     * Returns the group container naming attribute.
     * 
     * @return group container naming attribute
     * @deprecated This method is deprecated. Use
     *             {@link #getNamingAttribute(int) 
     *             getNamingAttribute(int objectType)}
     */
    public static String getGroupContainerNamingAttribute() {
        return AMNamingAttrManager.getNamingAttr(AMObject.GROUP_CONTAINER);
    }

    /**
     * Returns the group naming attribute.
     * 
     * @return group naming attribute
     * @deprecated This method is deprecated. Use
     *             {@link #getNamingAttribute(int) 
     *             getNamingAttribute(int objectType)}
     */
    public static String getGroupNamingAttribute() {
        return AMNamingAttrManager.getNamingAttr(AMObject.GROUP);
    }

    /**
     * Returns the naming attribute of an object type.
     * 
     * @param objectType
     *            Object type can be one of the following:
     *            <ul>
     *            <li> {@link AMObject#USER AMObject.USER}
     *            <li> {@link AMObject#ROLE AMObject.ROLE}
     *            <li> {@link AMObject#FILTERED_ROLE AMObject.FILTERED_ROLE}
     *            <li> {@link AMObject#ORGANIZATION AMObject.ORGANIZATION}
     *            <li> {@link AMObject#ORGANIZATIONAL_UNIT
     *            AMObject.ORGANIZATIONAL_UNIT}
     *            <li> {@link AMObject#GROUP AMObject.GROUP}
     *            <li> {@link AMObject#DYNAMIC_GROUP AMObject.DYNAMIC_GROUP}
     *            <li> {@link AMObject#ASSIGNABLE_DYNAMIC_GROUP
     *            AMObject.ASSIGNABLE_DYNAMIC_GROUP}
     *            <li>
     *            {@link AMObject#PEOPLE_CONTAINER AMObject.PEOPLE_CONTAINER}
     *            <li> {@link AMObject#GROUP_CONTAINER AMObject.GROUP_CONTAINER}
     *            </ul>
     * @return the naming attribute corresponding to the <code>objectType</code>
     * @throws AMException
     *             if an error occurred in obtaining the naming attribute
     */
    public static String getNamingAttribute(int objectType) throws AMException {
        return AMNamingAttrManager.getNamingAttr(objectType);
    }

    /**
     * Returns the organization naming attribute.
     * 
     * @return organization naming attribute
     * @deprecated This method is deprecated. Use
     *             {@link #getNamingAttribute(int) 
     *             getNamingAttribute(int objectType)}
     */
    public static String getOrganizationNamingAttribute() {
        return AMNamingAttrManager.getNamingAttr(AMObject.ORGANIZATION);
    }

    /**
     * Returns the organizational unit naming attribute.
     * 
     * @return organizational unit naming attribute
     * @deprecated This method is deprecated. Use
     *             {@link #getNamingAttribute(int) 
     *             getNamingAttribute(int objectType)}
     */
    public static String getOrganizationalUnitNamingAttribute() {
        return AMNamingAttrManager.getNamingAttr(AMObject.ORGANIZATIONAL_UNIT);
    }

    /**
     * Returns the people container naming attribute.
     * 
     * @return people container naming attribute
     * @deprecated This method is deprecated. Use
     *             {@link #getNamingAttribute(int) 
     *             getNamingAttribute(int objectType)}
     */
    public static String getPeopleContainerNamingAttribute() {
        return AMNamingAttrManager.getNamingAttr(AMObject.PEOPLE_CONTAINER);
    }

    /**
     * Returns the role naming attribute.
     * 
     * @return role naming attribute
     * @deprecated This method is deprecated. Use
     *             {@link #getNamingAttribute(int)
     *             getNamingAttribute(int objectType)}
     */
    public static String getRoleNamingAttribute() {
        return AMNamingAttrManager.getNamingAttr(AMObject.ROLE);
    }

    /**
     * Returns the user naming attribute.
     * 
     * @return user naming attribute
     * @deprecated This method is deprecated. Use
     *             {@link #getNamingAttribute(int) 
     *             getNamingAttribute(int objectType)}
     */
    public static String getUserNamingAttribute() {
        return AMNamingAttrManager.getNamingAttr(AMObject.USER);
    }

    /**
     * Returns the type of the object given its DN.
     * 
     * @param dn
     *            DN of the object whose type is to be known.
     * @return the type of the object given its DN.
     * @throws AMException
     *             if the data store is unavailable or if the object type is
     *             unknown.
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public int getAMObjectType(String dn) throws AMException, SSOException {
        return dsServices.getObjectType(token, dn);
    }

    /**
     * Take a supported type, and returns the matching name of the supported
     * managed type. For example, if <code> AMObject.USER</code> is passed in,
     * it will return "user" (one of the basic supported types in AM SDK. But
     * this method (and configuration in the service <code>DAI</code>) can be
     * used to extend the basic supported types to include customer-specific
     * entities, like "agents", "printers" etc.
     * 
     * @param type
     *            Integer type (as returned by <code>getAMObjectType</code>)
     * @return identifier for the above type. Returns null if type is unknown.
     */
    public String getAMObjectName(int type) {
        return ((String) AMCommonUtils.supportedNames.get(Integer
                .toString(type)));
    }

    /**
     * Take a supported type, and returns the matching name of the supported
     * managed type. For example, if <code> AMObject.USER</code> is passed in,
     * it will return "user" (one of the basic supported types in AM SDK. But
     * this method (and configuration in the service <code>DAI</code>) can be
     * used to extend the basic supported types to include customer-specific
     * entities, like "agents", "printers" etc.
     * 
     * @param type
     *            Integer type (as returned by <code>getAMObjectType</code>)
     * @return identifier for the above type. Returns null if type is unknown.
     */
    public static String getObjectName(int type) {
        return ((String) AMCommonUtils.supportedNames.get(Integer
                .toString(type)));
    }

    /**
     * Returns the handle to the <code>AMAssignableDynamicGroup</code> object
     * represented by DN. However, the validity of the handle returned by this
     * method cannot be guaranteed, since the object is created in memory, and
     * not instantiated from the data store. Using the
     * <code>AMAssignableDynamicGroup</code> returned from this method may
     * result in exceptions thrown in the later part of the application, if the
     * DN is not valid or represents an entry that does not exist.
     * <p>
     * 
     * Validity of the DN can be verified is using <code>isValidEntry()</code>
     * method of the object returned.
     * 
     * @see #isValidEntry
     * 
     * @param assignableDynamicGroupDN
     *            assignable dynamic group DN
     * @return <code>AMAssignableDynamicGroup</code> object represented by DN.
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public AMAssignableDynamicGroup getAssignableDynamicGroup(
            String assignableDynamicGroupDN) throws SSOException {
        AMAssignableDynamicGroup assignableDynamicGroup = 
            new AMAssignableDynamicGroupImpl(this.token, 
                    assignableDynamicGroupDN);

        return assignableDynamicGroup;
    }

    /**
     * Returns the service attribute names for a given service name and schema
     * type.
     * 
     * @param serviceName
     *            the name of the service
     * @param schemaType
     *            the type of service schema
     * @return Set of service attribute names
     * @throws AMException
     *             if an error is encountered while retrieving information.
     * @deprecated use <code>com.sun.identity.sm.ServiceSchemaManager.
     * getServiceAttributeNames(com.sun.identity.sm.SchemaType)</code>
     */
    public Set getAttributeNames(String serviceName, AMSchema.Type schemaType)
            throws AMException {
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(serviceName,
                    token);
            SchemaType st = schemaType.getInternalSchemaType();

            return ssm.getServiceAttributeNames(st);
        } catch (SSOException se) {
            debug.message("AMStoreConnection.getAttributeNames(String, " 
                    + "AMSchema.Type)", se);
            throw new AMException(AMSDKBundle.getString("902", locale), "902");
        } catch (SMSException se) {
            debug.message("AMStoreConnection.getAttributeNames(String, " +
                    "AMSchema.Type)", se);
            throw new AMException(AMSDKBundle.getString("911", locale), "911");
        }
    }

    /**
     * Returns the handle to the <code>AMDynamicGroup</code> object
     * represented by DN. However, the validity of the handle returned by this
     * method cannot be guaranteed, since the object is created in memory, and
     * not instantiated from the data store. Using the
     * <code>AMDynamicGroup</code> returned from this method may result in
     * exceptions thrown in the later part of the application, if the DN is not
     * valid or represents an entry that does not exist.
     * <p>
     * 
     * Validity of the DN can be verified is using <code>isValidEntry()</code>
     * method of the object returned.
     * 
     * @see #isValidEntry
     * 
     * @param dynamicGroupDN
     *            group DN
     * @return <code>AMDynamicGroup</code> object represented by DN.
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public AMDynamicGroup getDynamicGroup(String dynamicGroupDN)
            throws SSOException {
        AMDynamicGroup dynamicGroup = new AMDynamicGroupImpl(this.token,
                dynamicGroupDN);

        return dynamicGroup;
    }

    /**
     * Returns the handle to the <code>AMFilteredRole</code> object
     * represented by DN. However, the validity of the handle returned by this
     * method cannot be guaranteed, since the object is created in memory, and
     * not instantiated from the data store. Using the
     * <code>AMFilteredRole</code> returned from this method may result in
     * exceptions thrown in the later part of the application, if the DN is not
     * valid or represents an entry that does not exist.
     * <p>
     * 
     * Validity of the DN can be verified is using <code>isValidEntry()</code>
     * method of the object returned.
     * 
     * @see #isValidEntry
     * 
     * @param roleDN
     *            role DN.
     * @return <code>AMFilteredRole</code> object represented by DN.
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public AMFilteredRole getFilteredRole(String roleDN) throws SSOException {
        AMFilteredRole role = new AMFilteredRoleImpl(this.token, roleDN);

        return role;
    }

    /**
     * Returns the handle to the <code>AMGroupContainer</code> object
     * represented by DN. However, the validity of the handle returned by this
     * method cannot be guaranteed, since the object is created in memory, and
     * not instantiated from the data store. Using the
     * <code>AMGroupContainer</code> returned from this method may result in
     * exceptions thrown in the later part of the application, if the DN is not
     * valid or represents an entry that does not exist.
     * <p>
     * 
     * Validity of the DN can be verified is using <code>isValidEntry()</code>
     * method of the object returned.
     * 
     * @see #isValidEntry
     * 
     * @param groupContainerDN
     *            group container DN.
     * @return <code>AMGroupContainer</code> object represented by DN.
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public AMGroupContainer getGroupContainer(String groupContainerDN)
            throws SSOException {
        AMGroupContainer groupContainer = new AMGroupContainerImpl(this.token,
                groupContainerDN);

        return groupContainer;
    }

    /**
     * Returns the I18N properties file name that contains the internationalized
     * messages.
     * 
     * @param serviceName
     *            the service name
     * @return String String representing i18N properties file name
     * @throws AMException
     *             if an error is encountered while retrieving information
     */
    public String getI18NPropertiesFileName(String serviceName)
            throws AMException {
        try {
            ServiceSchemaManager scm = new ServiceSchemaManager(serviceName,
                    token);

            return scm.getI18NFileName();
        } catch (SSOException so) {
            debug.error("AMStoreConnection.getI18NPropertiesFileName(): ", so);
            throw new AMException(AMSDKBundle.getString("902", locale), "902");
        } catch (SMSException se) {
            debug.error("AMStoreConnection.getServiceNames(): ", se);
            throw new AMException(AMSDKBundle.getString("909", locale), "909");
        }
    }

    /**
     * Returns the handle to the <code>AMOrganization</code> object
     * represented by DN. However, the validity of the handle returned by this
     * method cannot be guaranteed, since the object is created in memory, and
     * not instantiated from the data store. Using the
     * <code>AMOrganization</code> returned from this method may result in
     * exceptions thrown in the later part of the application, if the DN is not
     * valid or represents an entry that does not exist.
     * <p>
     * 
     * Validity of the DN can be verified is using <code>isValidEntry()</code>
     * method of the object returned.
     * 
     * @see #isValidEntry
     * 
     * @param orgDN
     *            organization DN
     * @return <code>AMOrganization</code> object represented by DN.
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public AMOrganization getOrganization(String orgDN) throws SSOException {
        AMOrganization organization = new AMOrganizationImpl(this.token, orgDN);

        return organization;
    }

    /**
     * Returns the DN of the organization, using the <code>domainname</code>
     * provided and the <code>searchTemplate</code> (if provided). If
     * <code>searchTemplate</code> is null, SDK uses the default
     * <code>searchTemplate</code> to perform the <code>orgDN</code> search.
     * If the DC tree global flag is enabled, the DC tree is used to obtain the
     * organization DN, otherwise an LDAP search is conducted using the
     * <code>searchfilter</code> in the <code>searchtemplate</code>. All
     * <code>%V</code> in the filter are replaced with <code>domainname</code>.
     * If the search returns more than one entries, then an Exception is thrown.
     * Otherwise the DN obtained is returned.
     * 
     * @param domainname
     *            Organization identifier passed. It can be a domain name
     *            (example: <code>sun.com</code>) or it could be a full DN or
     *            it could be null or <code>* "/"</code>. A full DN is
     *            verified to be an organization and returned as is. A "/" is
     *            assumed to be a request for the root DN and the root DN is
     *            returned. A "/" separated string is assumed to represent an
     *            existing organization DN in the DIT. For example:
     *            <code>/iplanet/sun</code> is converted to a DN
     *            <code>(o=iplanet,o=sun,&lt;base DN>)</code> and the validity
     *            of this DN is checked and returned. Any other string is
     *            assumed to be either a domain or an associated domain or the
     *            organization name. The search filter is created accordingly.
     * @param orgSearchTemplate
     *            template to use for the search.
     * @return The full organization DN
     * @throws AMException
     *             If there is a problem connecting or searching the data store.
     * @throws SSOException
     *             If the user has an invalid SSO token.
     */
    public String getOrganizationDN(String domainname, String orgSearchTemplate)
            throws AMException, SSOException {
        if (domainname == null) {
            return rootSuffix;
        }

        // If a DN is passed and is a valid organization DN, then
        // return it.
        if (DN.isDN(domainname) && isValidEntry(domainname)
                && (getAMObjectType(domainname) == AMObject.ORGANIZATION)) {
            return domainname;
        }

        if (!domainname.startsWith("http://") && (domainname.indexOf("/") > -1))
        {
            String orgdn = orgNameToDN(domainname);

            if (isValidEntry(orgdn)
                    && (getAMObjectType(orgdn) == AMObject.ORGANIZATION)) {
                return (orgdn);
            } else {
                Object[] args = { orgdn };
                String locale = AMCommonUtils.getUserLocale(token);
                throw new AMException(AMSDKBundle
                        .getString("467", args, locale), "467", args);
            }
        }

        try {
            String orgdn;

            if (AMDCTree.isRequired()) {
                orgdn = AMDCTree.getOrganizationDN(token, domainname);

                if (orgdn != null) {
                    return orgdn;
                }
            }
        } catch (AMException ae) {
            // do nothing. will try to search the organization
            // using search template
            debug.error("AMStoreConnection.getOrganizationDN-> "
                    + "In DC tree mode, unabe to find organization "
                    + " for domain: " + domainname);
        }

        String orgdn = (String) orgMapCache.get(domainname.toLowerCase());

        if (orgdn != null) {
            return (orgdn);
        } else {
            // use the searchfilter to obtain org DN
            // replace %V with domainname.
            String searchFilter = AMSearchFilterManager.getSearchFilter(
                    AMObject.ORGANIZATION, null, orgSearchTemplate, false);

            if ((orgSearchTemplate != null)
                    && (searchFilter.indexOf("%V") > -1)) {
                searchFilter = AMObjectImpl.constructFilter(AMNamingAttrManager
                        .getNamingAttr(AMObject.ORGANIZATION), searchFilter,
                        domainname);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("(|(&").append(searchFilter).append("(").append(
                        AMNamingAttrManager
                                .getNamingAttr(AMObject.ORGANIZATION)).append(
                        "=").append(domainname).append(")").append(")(&")
                        .append(searchFilter).append("(").append(
                                "sunPreferredDomain=").append(domainname)
                        .append(")").append(")(&").append(searchFilter).append(
                                "(").append("associatedDomain=").append(
                                domainname).append(")").append(")(&").append(
                                searchFilter).append("(").append(
                                "sunOrganizationAlias=").append(domainname)
                        .append(")").append("))");
                searchFilter = sb.toString();
            }

            if (debug.messageEnabled()) {
                debug.message("AMSC:getOrgDN-> " + "using searchfilter "
                        + searchFilter);
            }

            Set orgSet = dsServices.search(token, rootSuffix, searchFilter,
                    SCOPE_SUB);

            if ((orgSet == null) || (orgSet.size() > 1) || orgSet.isEmpty()) {
                // throw an exception
                Object[] args = { domainname };
                String locale = AMCommonUtils.getUserLocale(token);
                throw new AMException(AMSDKBundle
                        .getString("971", args, locale), "971", args);
            } else {
                Iterator it = orgSet.iterator();
                orgdn = (String) it.next();
                addToOrgMapCache(token, orgdn);

                return (orgdn);
            }
        }
    }

    /**
     * Returns the handle to the <code>AMOrganizationalUnit</code> object
     * represented by DN. However, the validity of the handle returned by this
     * method cannot be guaranteed, since the object is created in memory, and
     * not instantiated from the data store. Using the
     * <code>AMOrganizationialUnit</code> returned from this method may result
     * in exceptions thrown in the later part of the application, if the DN is
     * not valid or represents an entry that does not exist.
     * <p>
     * 
     * Validity of the DN can be verified is using <code>isValidEntry()</code>
     * method of the object returned.
     * 
     * @see #isValidEntry
     * 
     * @param orgUnitDN
     *            organizational unit DN
     * @return <code>AMOrganizationalUnit</code> object represented by DN.
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public AMOrganizationalUnit getOrganizationalUnit(String orgUnitDN)
            throws SSOException {
        AMOrganizationalUnit organizationalUnit = new AMOrganizationalUnitImpl(
                this.token, orgUnitDN);

        return organizationalUnit;
    }

    /**
     * Returns the handle to the <code>AMPeopleContainer</code> object
     * represented by DN. However, the validity of the handle returned by this
     * method cannot be guaranteed, since the object is created in memory, and
     * not instantiated from the data store. Using the
     * <code>AMPeopleContainer</code> returned from this method may result in
     * exceptions thrown in the later part of the application, if the DN is not
     * valid or represents an entry that does not exist.
     * <p>
     * 
     * Validity of the DN can be verified is using <code>isValidEntry()</code>
     * method of the object returned.
     * 
     * @see #isValidEntry
     * 
     * @param peopleContainerDN
     *            people container DN
     * @return <code>AMPeopleContainer</code> object represented by DN.
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public AMPeopleContainer getPeopleContainer(String peopleContainerDN)
            throws SSOException {
        AMPeopleContainer peopleContainer = new AMPeopleContainerImpl(
                this.token, peopleContainerDN);

        return peopleContainer;
    }

    /**
     * Returns the handle to the <code>AMTemplate</code> object represented by
     * DN. However, the validity of the handle returned by this method cannot be
     * guaranteed, since the object is created in memory, and not instantiated
     * from the data store. Using the <code>AMTemplate</code> returned from
     * this method may result in exceptions thrown in the later part of the
     * application, if the DN is not valid or represents an entry that does not
     * exist.
     * <p>
     * 
     * Validity of the DN can be verified is using <code>isValidEntry()</code>
     * method of the object returned.
     * 
     * @deprecated
     * @see #isValidEntry
     * 
     * @param templateDN
     *            a policy template DN.
     * @return <code>AMTemplate</code> object represented by DN.
     * @throws AMException
     *             if the DN does not represent a Policy template DN
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public AMTemplate getPolicyTemplate(String templateDN) throws AMException,
            SSOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the URL of the view bean for the service
     * 
     * @param serviceName
     *            the service name
     * @return String URL of the view bean for the service
     * @throws AMException
     *             if an error is encountered while retrieving information
     */
    public String getPropertiesViewBeanURL(String serviceName)
            throws AMException {
        try {
            ServiceSchemaManager scm = new ServiceSchemaManager(serviceName,
                    token);

            return scm.getPropertiesViewBeanURL();
        } catch (SSOException so) {
            debug.error("AMStoreConnection.getPropertiesViewBeanURL(): ", so);
            throw new AMException(AMSDKBundle.getString("902", locale), "902");
        } catch (SMSException se) {
            debug.error("AMStoreConnection.getServiceNames(): ", se);
            throw new AMException(AMSDKBundle.getString("910", locale), "910");
        }
    }

    /**
     * Returns the handle to the <code>AMResource</code> object represented by
     * DN. However, the validity of the handle returned by this method cannot be
     * guaranteed, since the object is created in memory, and not instantiated
     * from the data store. Using the <code>AMResource</code> returned from
     * this method may result in exceptions thrown in the later part of the
     * application, if the DN is not valid or represents an entry that does not
     * exist.
     * <p>
     * 
     * Validity of the DN can be verified is using <code>isValidEntry()</code>
     * method of the object returned.
     * 
     * @see #isValidEntry
     * 
     * @param resourceDN
     *            resource DN.
     * @return <code>AMResource</code> object represented by DN.
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public AMResource getResource(String resourceDN) throws SSOException {
        AMResource res = new AMResourceImpl(this.token, resourceDN);

        return res;
    }

    /**
     * Returns the handle to the <code>AMRole</code> object represented by DN.
     * However, the validity of the handle returned by this method cannot be
     * guaranteed, since the object is created in memory, and not instantiated
     * from the data store. Using the <code>AMRole</code> returned from this
     * method may result in exceptions thrown in the later part of the
     * application, if the DN is not valid or represents an entry that does not
     * exist.
     * <p>
     * 
     * Validity of the DN can be verified is using <code>isValidEntry()</code>
     * method of the object returned.
     * 
     * @see #isValidEntry
     * 
     * @param roleDN
     *            role DN
     * @return <code>AMRole</code> object represented by DN.
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public AMRole getRole(String roleDN) throws SSOException {
        AMRole role = new AMRoleImpl(this.token, roleDN);

        return role;
    }

    /**
     * Returns the <code>AMSchema</code> for the given service name and
     * service type.
     * 
     * @param serviceName
     *            the name of the service
     * @param schemaType
     *            the type of service schema that needs to be retrieved.
     * 
     * @return <code>AMSchema</code> corresponding to the given service name
     *         and schema type.
     * 
     * @throws AMException
     *             if an error is encountered in retrieving the
     *             <code>AMSchema</code>.
     * 
     * @deprecated This method has been deprecated. Please use
     *             <code>com.sun.identity.sm.ServiceSchemaManager.getSchema()
     *             </code>.
     */
    public AMSchema getSchema(String serviceName, AMSchema.Type schemaType)
            throws AMException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the schema types available for a particular service.
     * 
     * @param serviceName
     *            the name of the service whose schema types needs to be
     *            retrieved
     * @return Set of <code>AMSchema.Type</code> objects
     * @throws AMException
     *             if an error is encountered in retrieving the
     *             <code>schemaTypes</code>.
     * 
     * @deprecated This method has been deprecated. Please use
     *             <code>
     *             com.sun.identity.sm.ServiceSchemaManager.getSchemaTypes()
     *             </code>.
     */
    public Set getSchemaTypes(String serviceName) throws AMException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the service hierarchy for all registered services.
     * 
     * @return the service hierarchy for all registered services.
     * @throws AMException
     *             if an error is encountered in retrieving the service
     *             hierarchy. The return value is a Set of strings in slash
     *             format.
     */
    public Set getServiceHierarchy() throws AMException {
        try {
            Set retSet = new HashSet();
            ServiceManager sm = new ServiceManager(token);
            Set serviceNames = sm.getServiceNames();
            Iterator itr = serviceNames.iterator();

            while (itr.hasNext()) {
                String st = (String) itr.next();
                ServiceSchemaManager scm = new ServiceSchemaManager(st, token);
                String sh = scm.getServiceHierarchy();

                if ((sh != null) && (sh.length() != 0)) {
                    retSet.add(sh);
                }
            }

            return retSet;
        } catch (SSOException so) {
            debug.error("AMStoreConnection.getServiceNames(): ", so);
            throw new AMException(AMSDKBundle.getString("902", locale), "902");
        } catch (SMSException se) {
            debug.error("AMStoreConnection.getServiceNames(): ", se);
            throw new AMException(AMSDKBundle.getString("905", locale), "905");
        }
    }

    /**
     * Returns the set of name of services that have been loaded to the data
     * store.
     * 
     * @return set of name of services.
     * @throws AMException
     *             if an error is encountered in retrieving the names of the
     *             services
     */
    public Set getServiceNames() throws AMException {
        try {
            ServiceManager sm = new ServiceManager(token);

            return sm.getServiceNames();
        } catch (SSOException so) {
            debug.error("AMStoreConnection.getServiceNames(): ", so);
            throw new AMException(AMSDKBundle.getString("902", locale), "902");
        } catch (SMSException se) {
            debug.error("AMStoreConnection.getServiceNames(): ", se);
            throw new AMException(AMSDKBundle.getString("906", locale), "906");
        }
    }

    /**
     * Returns the handle to the <code>AMStaticGroup</code> object represented
     * by DN. However, the validity of the handle returned by this method cannot
     * be guaranteed, since the object is created in memory, and not
     * instantiated from the data store. Using the <code>AMStaticGroup</code>
     * returned from this method may result in exceptions thrown in the later
     * part of the application, if the DN is not valid or represents an entry
     * that does not exist.
     * <p>
     * 
     * Validity of the DN can be verified is using <code>isValidEntry()</code>
     * method of the object returned.
     * 
     * @see #isValidEntry
     * 
     * @param groupDN
     *            group DN
     * @return <code>AMStaticGroup</code> object represented by DN.
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public AMStaticGroup getStaticGroup(String groupDN) throws SSOException {
        AMStaticGroup group = new AMStaticGroupImpl(this.token, groupDN);
        return group;
    }

    /**
     * Returns the top level containers (Organizations, People Containers,
     * Roles, etc) for the particular user based on single sign on token as the
     * starting point in the tree.
     * 
     * @return set of <code>DBObjects</code> that are top level containers for
     *         the signed in user.
     * @throws AMException
     *             if an error occurred when retrieving the information from the
     *             data store.
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public Set getTopLevelContainers() throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(this.token);
        return dsServices.getTopLevelContainers(token);
    }

    /**
     * Returns the "real" or "physical" top level organizations as the starting
     * point in the tree.
     * 
     * @return Set Set of DN Strings for top level Organizations
     * @throws AMException
     *             if an error occurred when retrieving the information from the
     *             data store.
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public Set getTopLevelOrganizations() throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(this.token);

        return dsServices.search(this.token, rootSuffix, AMSearchFilterManager
                .getGlobalSearchFilter(AMObject.ORGANIZATION), SCOPE_ONE);
    }

    /**
     * Returns the handle to the <code>AMUser</code> object represented by DN.
     * However, the validity of the handle returned by this method cannot be
     * guaranteed, since the object is created in memory, and not instantiated
     * from the data store. Using the <code>AMUser</code> returned from this
     * method may result in exceptions thrown in the later part of the
     * application, if the DN is not valid or represents an entry that does not
     * exist.
     * <p>
     * 
     * Validity of the DN can be verified is using <code>isValidEntry()</code>
     * method of the object returned.
     * 
     * @see #isValidEntry
     * 
     * @param userDN
     *            user DN
     * @return <code>AMUser</code> object represented by DN
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public AMUser getUser(String userDN) throws SSOException {
        AMUser user = new AMUserImpl(this.token, userDN);
        return user;
    }

    /**
     * Returns the handle to the <code>AMEntity</code> object represented by
     * DN. However, the validity of the handle returned by this method cannot be
     * guaranteed, since the object is created in memory, and not instantiated
     * from the data store. Using the <code>AMEntity</code> returned from this
     * method may result in exceptions thrown in the later part of the
     * application, if the DN is not valid or represents an entry that does not
     * exist.
     * <p>
     * 
     * Validity of the DN can be verified is using <code>isValidEntry()</code>
     * method of the object returned.
     * 
     * @see #isValidEntry
     * 
     * @param eDN
     *            entity DN.
     * @return <code>AMEntity</code> object represented by DN.
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public AMEntity getEntity(String eDN) throws SSOException {
        AMEntity entity = null;
        try {
            entity = new AMEntityImpl(this.token, eDN, getAMObjectType(eDN));
        } catch (AMException ame) {
            // Return AMEntity without object type
            entity = new AMEntityImpl(this.token, eDN);
        }
        return entity;
    }

    /**
     * Checks if the entry exists in the directory or not. First a syntax check
     * is done on the DN string corresponding to the entry. If the DN syntax is
     * valid, a directory call will be made to check for the existence of the
     * entry.
     * <p>
     * 
     * <b>NOTE:</b> This method internally invokes a call to the directory to
     * verify the existence of the entry. There could be a performance overhead.
     * Hence, please use your discretion while using this method.
     * 
     * @param dn
     *            DN of the entry that needs to be validated.
     * 
     * @return false if the entry does not have a valid DN syntax or if the
     *         entry does not exists in the Directory. True otherwise.
     * 
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public boolean isValidEntry(String dn) throws SSOException {
        // First check if DN syntax is valid. Avoid making iDS call
        if (!com.sun.identity.shared.ldap.util.DN.isDN(dn)) { // May be a exception thrown

            return false; // would be better here.
        }

        SSOTokenManager.getInstance().validateToken(token);

        if (debug.messageEnabled()) {
            debug.message("AMStoreConnection.isValidEntry(): DN=" + dn);
        }

        return dsServices.doesEntryExists(token, dn);
    }

    /**
     * Bootstraps the Organization tree by creating the Top Organization tree.
     * 
     * @param orgName
     *            name of the top organization
     * @param avPairs
     *            Attribute-Value pairs for the top organization
     * @return Top Organization object.
     * @throws AMException
     *             if an error occurred during the process of creation.
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public AMOrganization createTopOrganization(String orgName, Map avPairs)
            throws AMException, SSOException {
        StringBuilder orgDNSB = new StringBuilder();
        orgDNSB
                .append(
                        AMNamingAttrManager
                                .getNamingAttr(AMObject.ORGANIZATION)).append(
                        "=").append(orgName).append(",").append(rootSuffix);

        AMOrganizationImpl orgImpl = new AMOrganizationImpl(this.token, orgDNSB
                .toString());
        orgImpl.setAttributes(avPairs);
        orgImpl.create();

        return orgImpl;
    }

    /**
     * This method takes an organization DN and purges all objects marked for
     * deletion. If the organization itself is marked for deletion, then a
     * recursive delete of everything under the organization is called, followed
     * by the organization deletion. This method works in the mode where
     * soft-delete option in Access Manager is turned on. The Pre/Post
     * <code>callbacks</code> for users are executed during this method.
     * 
     * @param domainName
     *            domain to be purged
     * @param graceperiod
     *            time in days which should have passed since the entry was last
     *            modified before it can be deleted from the system.
     * @throws AMException
     *             if an error occurred when retrieving the information from the
     *             data store.
     * @throws SSOException
     *             if single sign on token is invalid or expired.
     */
    public void purge(String domainName, int graceperiod) throws AMException,
            SSOException {
        String orgDN;
        Set orgSet;
        boolean deleted = false;
        if (AMDCTree.isRequired()) {
            orgDN = AMDCTree.getOrganizationDN(token, domainName);
            orgSet = new HashSet();
            orgSet.add(orgDN);
        } else {
            // Use special org search filter for searching for deleted
            // organizations.
            String filter = AMCompliance
                    .getDeletedObjectFilter(AMObject.ORGANIZATION);
            filter = AMObjectImpl.constructFilter(AMNamingAttrManager
                    .getNamingAttr(AMObject.ORGANIZATION), filter, domainName);

            if (debug.messageEnabled()) {
                debug.message("AMStoreConnection.purgeOrg: "
                        + "Using org filter= " + filter);
            }

            orgSet = dsServices.search(token, rootSuffix, filter, SCOPE_SUB);

            if ((orgSet == null) || orgSet.isEmpty()) {
                orgSet = getOrganizations(domainName, null);
                deleted = false;
            } else {
                deleted = true;
            }
        }

        if (orgSet == null || orgSet.isEmpty()) {
            return;
        }
        Iterator delIter = orgSet.iterator();
        while (delIter.hasNext()) {
            orgDN = (String) delIter.next();
            if (debug.messageEnabled()) {
                debug.message("AMStoreConnection.purge: " + "Organization= "
                        + orgDN);
            }

            AMOrganization org = getOrganization(orgDN);

            // Check to see if grace period has expired.
            if (deleted && graceperiod < daysSinceModified(token, orgDN)) {
                // Delete all objects using the hardDelete method.
                org.purge(true, -1);
            } else {
                // Search for objects marked as deleted and
                // try to purge them, if graceperiod as expired.
                String filter = AMCompliance.getDeletedObjectFilter(-1);

                if (debug.messageEnabled()) {
                    debug.message("AMStoreConnection.purge: "
                            + "Searching deleted objects. Filter: " + filter);
                }

                Set deletedObjs = dsServices.search(token, orgDN, filter,
                        SCOPE_SUB);

                if (deletedObjs == null) {
                    // No objecxts to delete
                    if (debug.messageEnabled()) {
                        debug.message("AMStoreConnection.purge: "
                                + "No objects to be deleted found for "
                                + orgDN);
                    }
                }

                Iterator iter = deletedObjs.iterator();
                List list = new ArrayList();

                // get number of RDNs in the entry itself
                int entryRDNs = (new DN(orgDN)).countRDNs();

                // to count maximum level of RDNs in the search return
                int maxRDNCount = entryRDNs;

                // go through all search results, add DN to the list, and
                // set the maximun RDN count, will be used to remove DNs
                while (iter.hasNext()) {
                    String thisDN = (String) iter.next();
                    DN dn = new DN(thisDN);
                    int count = dn.countRDNs();

                    if (count > maxRDNCount) {
                        maxRDNCount = count;
                    }

                    list.add(dn);
                }

                int len = list.size();
                for (int i = maxRDNCount; i >= entryRDNs; i--) {
                    for (int j = 0; j < len; j++) {
                        // depending on object type,
                        DN thisdn = (DN) list.get(j);

                        if (thisdn.countRDNs() == i) {
                            String thisDN = thisdn.toRFCString();
                            int objType = getAMObjectType(thisDN);
                            AMObject thisObj;

                            if (debug.messageEnabled()) {
                                debug.message("AMStoreConnection:purgeOrg: " 
                                        + "deleting child " + thisDN);
                            }
                            try { // catch PreCallBackException
                                switch (objType) {
                                case AMObject.USER:
                                    thisObj = getUser(thisDN);
                                    thisObj.purge(false, graceperiod);

                                    break;

                                case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                                    thisObj = getAssignableDynamicGroup(thisDN);
                                    thisObj.purge(false, graceperiod);

                                    break;

                                case AMObject.DYNAMIC_GROUP:
                                    thisObj = getDynamicGroup(thisDN);
                                    thisObj.purge(false, graceperiod);

                                    break;

                                case AMObject.STATIC_GROUP:
                                case AMObject.GROUP:
                                    thisObj = getStaticGroup(thisDN);
                                    thisObj.purge(false, graceperiod);

                                    break;

                                case AMObject.RESOURCE:
                                    thisObj = getResource(thisDN);
                                    thisObj.purge(false, -1);
                                    break;

                                case AMObject.ORGANIZATION:
                                    thisObj = getOrganization(thisDN);

                                    if (!(new DN(thisDN)).equals(new DN(orgDN)))
                                    {
                                        thisObj.purge(true, graceperiod);
                                    }

                                    break;

                                default:

                                    // should not show up in the searched
                                    // objects.
                                    // as none of the other objects are
                                    // supported
                                    // for being marked as soft-deleted/
                                    // purging.
                                    break;
                                } // switch
                            } catch (AMPreCallBackException amp) {
                                debug.error("AMStoreConnection.purge: "
                                        + "Aborting delete of: "
                                        + thisDN
                                        + " due to pre-callback exception",
                                        amp);
                            }
                        } // if
                    } // for
                } // for

            } // else
        } // delIter

        return;
    }

    /**
     * This method takes a user ID and a domain name, It uses default search
     * templates to search for the organization and uses the deleted objects
     * search filter for Users as defined in the Administration Service of
     * Access Manager. This filter is used to search for the deleted user under
     * the organization. If the user is marked for deletion and the grace period
     * is passed then the user is purged. The pre-delete call backs as listed in
     * the Administration service, are called before the user is deleted. If any
     * of the <code>callbacks</code> throw an exception the delete operation
     * is aborted.
     * 
     * @param uid
     *            user ID
     * @param domainName
     *            domain in which the user belongs.
     * @param graceperiod
     *            time in days which should have passed before this user can be
     *            deleted.
     * 
     * @throws AMException
     *             if there is an error in deleting the user, or if the user
     *             <code>callbacks</code> thrown an exception
     * @throws SSOException
     */
    public void purgeUser(String uid, String domainName, int graceperiod)
            throws AMException, SSOException {
        String orgDN = getOrganizationDN(domainName, null);
        String filter = AMCompliance.getDeletedObjectFilter(AMObject.USER);
        filter = AMObjectImpl.constructFilter(AMNamingAttrManager
                .getNamingAttr(AMObject.USER), filter, uid);

        if (debug.messageEnabled()) {
            debug.message("AMStoreConnection.purgeGroup: "
                    + "Using deleted user filter= " + filter);
        }

        Set uSet = dsServices.search(token, orgDN, filter, SCOPE_SUB);

        if ((uSet == null) || (uSet.size() > 1) || uSet.isEmpty()) {
            // throw an exception
            Object args[] = { uid };
            throw new AMException(AMSDKBundle.getString("971", args, locale),
                    "971", args);
        }

        String uDN = (String) uSet.iterator().next();
        AMUser user = getUser(uDN);
        user.purge(false, graceperiod);

        return;
    }

    /**
     * This method takes a resource ID and a domain name, It uses default search
     * templates to search for the organization and uses the deleted objects
     * search filter for Resources as defined in the Administration Service of
     * Access Manager. This filter is used to search for the deleted resource
     * under the organization. If the resource is marked for deletion and the
     * grace period is passed then the resource is purged. The pre-delete call
     * backs as listed in the Administration service, are called before the user
     * is deleted. If any of the <code>callbacks</code> throw an exception the
     * delete operation is aborted.
     * 
     * @param rid
     *            resource ID
     * @param domainName
     *            domain in which the user belongs.
     * @param graceperiod
     *            time in days which should have passed before this user can be
     *            deleted.
     * 
     * @throws AMException
     *             if there is an error in deleting the user, or if the user
     *             <code>callbacks</code> thrown an exception
     * @throws SSOException
     */
    public void purgeResource(String rid, String domainName, int graceperiod)
            throws AMException, SSOException {
        String orgDN = getOrganizationDN(domainName, null);
        String filter = AMCompliance.getDeletedObjectFilter(AMObject.RESOURCE);
        filter = AMObjectImpl.constructFilter(AMNamingAttrManager
                .getNamingAttr(AMObject.RESOURCE), filter, rid);

        if (debug.messageEnabled()) {
            debug.message("AMStoreConnection.purgeGroup: "
                    + "Using deleted user filter= " + filter);
        }

        Set uSet = dsServices.search(token, orgDN, filter, SCOPE_SUB);

        if ((uSet == null) || (uSet.size() > 1) || uSet.isEmpty()) {
            // throw an exception
            Object args[] = { rid };
            throw new AMException(AMSDKBundle.getString("971", args, locale),
                    "971", args);
        }

        String uDN = (String) uSet.iterator().next();
        AMResource resource = getResource(uDN);
        resource.purge(false, graceperiod);

        return;
    }

    /**
     * This method takes a group name and a domain name, It uses default search
     * templates to search for the organization and uses the deleted objects
     * search filter for Groups as defined in the Administration Service of
     * Access Manager. This filter is used to search for the deleted user under
     * the organization. If the group is marked for deletion and the grace
     * period is passed then the group is purged. The pre-delete call backs as
     * listed in the Administration service, are called before the group is
     * deleted. If any of the <code>callbacks</code> throw an exception the
     * delete operation is aborted.
     * 
     * @param gid
     *            group name
     * @param domainName
     *            domain in which the group belongs.
     * @param graceperiod
     *            time in days which should have passed before this user can be
     *            deleted. If a -1 is passed, group is deleted right away
     *            without check on <code>graceperiod</code>.
     * 
     * @throws AMException
     *             if there is an error in deleting the group, or if the
     *             <code>callbacks</code> thrown an exception
     * @throws SSOException
     */
    public void purgeGroup(String gid, String domainName, int graceperiod)
            throws AMException, SSOException {
        String orgDN = getOrganizationDN(domainName, null);
        String filter = AMCompliance.getDeletedObjectFilter(AMObject.GROUP);
        filter = AMObjectImpl.constructFilter(AMNamingAttrManager
                .getNamingAttr(AMObject.GROUP), filter, gid);

        if (debug.messageEnabled()) {
            debug.message("AMStoreConnection.purgeGroup: "
                    + "Using deleted group filter= " + filter);
        }

        Set gSet = dsServices.search(token, orgDN, filter, SCOPE_SUB);

        if ((gSet == null) || (gSet.size() > 1) || gSet.isEmpty()) {
            // throw an exception
            Object args[] = { gid };
            throw new AMException(AMSDKBundle.getString("971", args, locale),
                    "971", args);
        }

        String uDN = (String) gSet.iterator().next();
        AMGroup g = null;
        int type = getAMObjectType(uDN);
        switch (type) {
        case AMObject.GROUP:
        case AMObject.STATIC_GROUP:
            g = new AMStaticGroupImpl(token, uDN);
            break;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            g = new AMAssignableDynamicGroupImpl(token, uDN);
            break;
        case AMObject.DYNAMIC_GROUP:
            g = new AMDynamicGroupImpl(token, uDN);
            break;
        default:
        }
        if (g != null) {
            g.purge(false, graceperiod);
        }
        return;
    }

    /**
     * Returns a set of <code>com.iplanet.am.sdk.AMEntityType</code> objects,
     * which is the set of objects which are supported by the
     * <code>com.iplanet.am.sdk.AMEntity</code> APIs.
     * 
     * @return Set of <code>AMEntityType</code> objects.
     */
    public Set getEntityTypes() {
        return AMCommonUtils.getSupportedEntityTypes();
    }

    protected String getBaseDN(ServiceConfig sc) {
        if (sc != null) {
            Map attrMap = sc.getAttributes();
            Set vals = (Set) attrMap.get("baseDN");

            if ((vals == null) || vals.isEmpty()) {
                return null;
            } else {
                Iterator it = vals.iterator();

                return ((String) it.next());
            }
        } else {
            return null;
        }
    }

    protected boolean isRFC2247(ServiceConfig sc) {
        // ServiceConfig sc = getSearchTemplateConfig(orgTemplate);
        if (sc != null) {
            Map attrMap = sc.getAttributes();
            Set vals = (Set) attrMap.get("rfc2247flag");

            if ((vals == null) || (vals.isEmpty())) {
                return (false);
            } else {
                Iterator it = vals.iterator();

                return (((String) it.next()).equalsIgnoreCase("true") ? true
                        : false);
            }
        } else {
            return (false);
        }
    }

    /**
     * Protected method to update the <code>orgMapCache</code>
     * 
     */
    protected static void addToOrgMapCache(SSOToken stoken, String dn)
            throws AMException, SSOException {
        if ((dn == null) || !DN.isDN(dn)) {
            return;
        }

        // String rfcDN = (new DN(dn)).toRFCString().toLowerCase();
        String rfcDN = dn;
        Set attrNames = new HashSet();
        attrNames.add("objectclass");
        attrNames.add("sunpreferreddomain");
        attrNames.add("associateddomain");
        attrNames.add("sunorganizationalias");

        Map attributes = AMDirectoryAccessFactory.getDirectoryServices()
                .getAttributes(stoken, dn, attrNames, AMObject.ORGANIZATION);

        // Add to cache
        String rdn = LDAPDN.explodeDN(dn, true)[0];
        Set prefDomain = (Set) attributes.get("sunpreferreddomain");
        Set associatedDomain = (Set) attributes.get("associateddomain");
        Set orgAlias = (Set) attributes.get("sunorganizationalias");

        synchronized (orgMapCache) {
            orgMapCache.put(rdn.toLowerCase(), rfcDN);

            if ((prefDomain != null) && (prefDomain.size() == 1)) {
                String preferredDomain = (String) prefDomain.iterator().next();

                // AMHashMap so no need to lowercase
                orgMapCache.put(preferredDomain, rfcDN);
            }

            if ((associatedDomain != null) && !associatedDomain.isEmpty()) {
                Iterator itr = associatedDomain.iterator();

                while (itr.hasNext()) {
                    String value = (String) itr.next();
                    orgMapCache.put(value, rfcDN);
                }
            }

            if ((orgAlias != null) && !orgAlias.isEmpty()) {
                Iterator itr = orgAlias.iterator();

                while (itr.hasNext()) {
                    String value = (String) itr.next();
                    orgMapCache.put(value, rfcDN);
                }
            }
        }
    }

    /**
     * Protected method to obtain the number of days since this DN was last
     * modified.
     */
    protected static int daysSinceModified(SSOToken stoken, String entryDN)
            throws AMException, SSOException {
        NumberFormat nf = NumberFormat.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddhhmmss");
        ParsePosition pp = new ParsePosition(0);

        Set attrNames = new HashSet(1);

        // Why are we adding objectclass when it is not being used?
        // If a specific reason, then we need to change the method call.
        // Same question applicable to other places where we add into orgmap
        // cache
        // attrNames.add("objectclass");
        attrNames.add("modifytimestamp");

        Map attributes = AMDirectoryAccessFactory.getDirectoryServices()
                .getAttributes(stoken, entryDN, attrNames,
                        AMObject.UNDETERMINED_OBJECT_TYPE);
        Set values = (Set) attributes.get("modifytimestamp");

        if ((values == null) || values.isEmpty()) {
            return -1;
        }

        String value = (String) values.iterator().next();

        if ((value == null) || value.length() == 0) {
            return -1;
        }

        Number n;

        try {
            n = nf.parse(value);
        } catch (ParseException pe) {
            if (debug.warningEnabled()) {
                debug.warning("AMStoreConnection.daysSinceModified: "
                        + "unable to parse date: " + value
                        + " :Returning default= -1", pe);
            }

            return (-1);
        }

        Date modDate = df.parse(n.toString(), pp);
        Date nowDate = new Date();

        // getTime() fn returns number of milliseconds
        // since January 1, 1970, 00:00:00 GMT
        long modTimeMSecs = modDate.getTime();
        long nowTimeMSecs = nowDate.getTime();

        long elapsedTimeMSecs = nowTimeMSecs - modTimeMSecs;
        int elapsedDays = (int) (elapsedTimeMSecs / (1000 * 60 * 60 * 24));

        if (debug.messageEnabled()) {
            debug.message("AMStoreConnection.daysSinceModified() for dn: "
                    + entryDN + ", days: " + elapsedDays + " days");
        }

        return (elapsedDays);
    }

    /**
     * Protected method to update <code>orgMapCache</code>.
     */
    protected static void updateCache(String dn, int eventType) {
        if ((dn == null) || !DN.isDN(dn)) {
            return;
        }

        String rfcDN = AMCommonUtils.formatToRFC(dn);
        switch (eventType) {
        case AMEvent.OBJECT_ADDED:
            // nothing to do
            return;

        case AMEvent.OBJECT_RENAMED:
            synchronized (orgMapCache) {
                orgMapCache.clear();
            }
            return;

        case AMEvent.OBJECT_REMOVED:
        case AMEvent.OBJECT_CHANGED:
            // Go through the entire cache and check and delete
            // any entries with values matching this DN
            synchronized (orgMapCache) {
                Iterator keys = orgMapCache.keySet().iterator();

                // String removeKey = null;
                Set removeKeys = new HashSet();

                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String val = (String) orgMapCache.get(key);

                    if (val.equalsIgnoreCase(rfcDN)) {
                        removeKeys.add(key);
                    }
                }

                if (removeKeys != null) {
                    keys = removeKeys.iterator();

                    while (keys.hasNext()) {
                        String removeKey = (String) keys.next();
                        orgMapCache.remove(removeKey);
                    }
                }
                // orgMapCache.clear();
            }
        }
    }

    private Set getOrganizations(String domainname, String orgSearchTemplate)
            throws AMException, SSOException {
        if (domainname == null) {
            return Collections.EMPTY_SET;
        }

        // use the searchfilter to obtain organization DN
        // replace %V with domainname.
        String searchFilter = AMSearchFilterManager.getSearchFilter(
                AMObject.ORGANIZATION, null, orgSearchTemplate, false);

        if ((orgSearchTemplate != null) && (searchFilter.indexOf("%V") > -1)) {
            searchFilter = AMObjectImpl.constructFilter(AMNamingAttrManager
                    .getNamingAttr(AMObject.ORGANIZATION), searchFilter,
                    domainname);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("(|(&(").append(
                    AMNamingAttrManager.getNamingAttr(AMObject.ORGANIZATION))
                    .append("=").append(domainname).append(")").append(
                            searchFilter).append(")(&(").append(
                            "sunPreferredDomain=").append(domainname).append(
                            ")").append(searchFilter).append(")(&(").append(
                            "associatedDomain=").append(domainname).append(")")
                    .append(searchFilter).append(")(&(").append(
                            "sunOrganizationAlias=").append(domainname).append(
                            ")").append(searchFilter).append("))");
            searchFilter = sb.toString();
        }

        if (debug.messageEnabled()) {
            debug.message("AMSC:getOrgDN-> " + "using searchfilter "
                    + searchFilter);
        }

        Set orgSet = dsServices.search(token, rootSuffix, searchFilter,
                SCOPE_SUB);
        return orgSet;
    }

    /**
     * Converts organization name which is "/" separated to DN.
     */
    private static String orgNameToDN(String orgName) {
        // Check if it is null or empty
        if ((orgName == null) || (orgName.length() == 0)) {
            return (rootSuffix);
        }

        // Check if it is org name
        if (DN.isDN(orgName)) {
            return (orgName);
        }

        // Construct the DN
        StringBuilder buf = new StringBuilder();
        ArrayList arr = new ArrayList();
        StringTokenizer strtok = new StringTokenizer(orgName, "/");

        while (strtok.hasMoreElements()) {
            arr.add(strtok.nextToken());
        }

        int size = arr.size();

        for (int i = 0; i < size; i++) {
            String theOrg = (String) arr.get(i);
            buf
                    .append(AMNamingAttrManager
                            .getNamingAttr(AMObject.ORGANIZATION));
            buf.append('=').append(theOrg).append(',');
        }

        if (rootSuffix.length() > 0) {
            buf.append(rootSuffix);
        } else {
            buf.deleteCharAt(buf.length() - 1);
        }

        return (buf.toString());
    }
}
