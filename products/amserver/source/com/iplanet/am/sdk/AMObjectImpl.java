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
 * $Id: AMObjectImpl.java,v 1.14 2009/11/20 23:52:51 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.sun.identity.shared.ldap.LDAPUrl;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.RDN;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.SearchControl;

import com.sun.identity.common.DNUtils;
import com.sun.identity.common.admin.DisplayOptionsUtils;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

import com.iplanet.am.sdk.common.IDirectoryServices;
import com.sun.identity.shared.debug.Debug;

/**
 * This class implements the AMObject interface.
 * <p>
 * 
 * Each instance of AMObjectImpl (essentially a instance of its subclass which
 * inherits all the features if this class) has a Set of private listeners
 * <code>listeners</code> which holds the list of all registered
 * <code>AMEventListener</code> instances. Apart from this class has a static
 * <code>
 * objImplListeners</code> table which holds the list of all *Impl
 * instances which are interested in receiving notifications for entry
 * changed/deleted/renamed events in LDAP. The first time a AMEventListener
 * instance is added to particular *Impl instance, invoking the method
 * *ImplObj.addEventListener (dpEventListener) it is checked to see if *Impl
 * instance has already been registered itself to the
 * <code>objImplListeners</code> table. If not already registered then it is
 * added to the <code>
 * objImplListeners</code> table. The verification is done
 * by the means of boolean variable <code>isRegistered</code> which exists for
 * each instance. So when ever a event notification is received, then
 * <code>objImplListener</code> is looked into to figure out the interested
 * *Impl instances and then notifications are sent to each of their private Set
 * of listeners.
 * 
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
class AMObjectImpl implements AMObject {
    // ~ Static fields/initializers
    // ---------------------------------------------

    // Private Constants
    private static final String POLICY_ADMIN_ROLE_NAME = 
        "Organization Policy Admin Role";

    private static final String POLICY_ADMIN_ROLE_PERMISSION = 
        "Organization Policy Admin";

    private static final String ROLE_DISPLAY_ATTR = 
        "iplanet-am-role-display-options";

    /**
     * <code>objImplListeners</code> holds the list of all registered
     * "*Impl's" that are interested in receiving notifications. The DN is the
     * key and value is a Set of *Impl instances interested in receiving
     * notifications for that DN.
     */
    private static Map objImplListeners = new HashMap();

    /**
     * Hash table used to keep track of elements that need to be removed from
     * objImplListeners table when a SSOToken is no longer valid. The key is
     * SSOTokenId & the value is a Set of DN's.
     */
    protected static Hashtable profileNameTable = new Hashtable();

    protected static Debug debug = AMCommonUtils.debug;

    // ~ Instance fields
    // --------------------------------------------------------
    protected IDirectoryServices dsServices;

    protected SSOToken token;

    protected String entryDN;

    protected String rfcDN = null;

    protected String locale = "en_US";

    protected int profileType;

    // Don't initialize until needed
    private AMHashMap byteValueModMap;

    private AMHashMap stringValueModMap;

    /**
     * A private Set <code>listeners</code> holds the list of all registered
     * listeners. thread saftety, 'listeners' should be enclosed in a
     * synchronized block.
     */
    private Set listeners = new HashSet();

    private String organizationDN = null;

    /**
     * This varible is to make sure that the AMObjectImpl instance is not added
     * more than once to profileNameTable and the objImplListeners
     */
    private boolean isRegistered = false;

    // ~ Constructors
    // -----------------------------------------------------------

    AMObjectImpl(SSOToken ssoToken, String dn, int type) {
        entryDN = dn;
        rfcDN = DNUtils.normalizeDN(entryDN);
        token = ssoToken;
        profileType = type;
        dsServices = AMDirectoryAccessFactory.getDirectoryServices();
        stringValueModMap = new AMHashMap(false);
        byteValueModMap = new AMHashMap(true);
        locale = AMCommonUtils.getUserLocale(token);
    }

    // ~ Methods
    // ----------------------------------------------------------------

    /**
     * Gets all service names that are assigned to the user/group/org.
     * 
     * @return The Set of service names that are assigned to the user.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set getAssignedServices() throws AMException, SSOException {
        // TODO: UnsupportedOperationException should move to the sub classes
        // No check here!
        if (!((profileType == AMObject.ORGANIZATION)
                || (profileType == AMObject.USER)
                || (profileType == AMObject.STATIC_GROUP)
                || (profileType == AMObject.DYNAMIC_GROUP)
                || (profileType == AMObject.ORGANIZATIONAL_UNIT)
                || (profileType == AMObject.ASSIGNABLE_DYNAMIC_GROUP) 
                || (profileType == AMObject.GROUP))) {
            throw new UnsupportedOperationException();
        }

        Set result = new HashSet(1);
        Set objectClasses = getAttribute("objectclass");
        String parentOrgDN = getOrganizationDN(); // better to check for org
        AMOrganization parentOrg = new AMOrganizationImpl(token, parentOrgDN);

        Set serviceNames = parentOrg.getRegisteredServiceNames();
        Iterator iter = serviceNames.iterator();

        while (iter.hasNext()) {
            String serviceName = (String) iter.next();
            Set tmpService = new HashSet(1);
            tmpService.add(serviceName);

            Set serviceOCs = AMServiceUtils.getServiceObjectClasses(token,
                    tmpService);

            if (!serviceOCs.isEmpty()) {
                boolean serviceAssigned = true;
                Iterator iter2 = serviceOCs.iterator();

                while (iter2.hasNext()) {
                    String oc = (String) iter2.next();

                    // Do we have to check if all the service object classes are
                    // present? Why can't we do the opposite? exit if 1 present
                    if (!AMCommonUtils.isObjectClassPresent(objectClasses, oc)) 
                    {
                        serviceAssigned = false;
                        break;
                    }
                }

                if (serviceAssigned) {
                    result.add(serviceName);
                }
            }
        }

        return result;
    }

    public void setAttribute(String attributeName, Set attributeValue)
            throws AMException, SSOException {
        Set copyValue = AMCommonUtils.getSetCopy(attributeValue);
        stringValueModMap.put(attributeName, copyValue);
    }

    public Set getAttribute(String attributeName) throws AMException,
            SSOException {
        SSOTokenManager.getInstance().validateToken(token);

        Set attrName = new HashSet(1);
        attrName.add(attributeName);

        Map attributes = dsServices.getAttributes(token, entryDN, attrName,
                profileType);
        Set values = (Set) attributes.get(attributeName);

        return ((values != null) ? values : new HashSet());
    }

    public void setAttributeByteArray(String attrName, byte[][] byteValues)
            throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(token);
        byteValueModMap.put(attrName, byteValues);
    }

    public byte[][] getAttributeByteArray(String attributeName)
            throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(token);

        Set attrName = new HashSet(1);
        attrName.add(attributeName);

        Map attributes = dsServices.getAttributesByteValues(token, entryDN,
                attrName, profileType);
        byte[][] values = (byte[][]) attributes.get(attributeName);

        return values; // Could be null, but thats what we return
    }

    public void setAttributes(Map attributes) throws AMException, SSOException {
        stringValueModMap.copy(attributes);
    }

    public Map getAttributes() throws AMException, SSOException {
        return getAttributes(null);
    }

    public Map getAttributes(Set attributeNames) throws AMException,
            SSOException {
        SSOTokenManager.getInstance().validateToken(token);

        Map attributes = dsServices.getAttributes(token, entryDN,
                attributeNames, profileType);

        /* preserve the attribute map's key with the attribute name passed in */
        attributes = replaceMapKey(attributes, attributeNames);
        return attributes;
    }

    public Map getAttributesFromDataStore() throws AMException, SSOException {
        return getAttributesFromDataStore(null);
    }

    public Map getAttributesFromDataStore(Set attributeNames)
            throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(token);

        Map attributes = dsServices.getAttributesFromDS(token, entryDN,
                attributeNames, profileType);

        /* preserve the attribute map's key with the attribute name passed in */
        attributes = replaceMapKey(attributes, attributeNames);
        return attributes;
    }

    public void setAttributesByteArray(Map attributes) throws SSOException,
            AMException {
        SSOTokenManager.getInstance().validateToken(token);
        byteValueModMap.copy(attributes);
    }

    public Map getAttributesByteArray() throws AMException, SSOException {
        return getAttributesByteArray(null);
    }

    public Map getAttributesByteArray(Set attributeNames) throws AMException,
            SSOException {
        SSOTokenManager.getInstance().validateToken(token);

        Map attributes = dsServices.getAttributesByteValues(token, entryDN,
                attributeNames, profileType);

        /* preserve the attribute map's key with the attribute name passed in */
        attributes = replaceMapKey(attributes, attributeNames);
        return attributes;
    }

    public void setBooleanAttribute(String attributeName, boolean value)
            throws AMException, SSOException {
        if (value) {
            setStringAttribute(attributeName, "true");
        } else {
            setStringAttribute(attributeName, "false");
        }
    }

    public boolean getBooleanAttribute(String attributeName)
            throws AMException, SSOException {
        Set attributeValue = getAttribute(attributeName);

        if (attributeValue.size() == 1) {
            String str = (String) (attributeValue.iterator().next());

            if (str.equalsIgnoreCase("true")) {
                return true;
            }

            if (str.equalsIgnoreCase("false")) {
                return false;
            }

            throw new AMException(AMSDKBundle.getString("154", locale), "154");
        }

        if (attributeValue.isEmpty()) {
            throw new AMException(AMSDKBundle.getString("155", locale), "155");
        }

        throw new AMException(AMSDKBundle.getString("154", locale), "154");
    }

    public String getDN() {
        return entryDN;
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
     * @return false if the entry does not have a valid DN syntax or if the
     *         entry does not exists in the Directory. True otherwise.
     * 
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public boolean isExists() throws SSOException {
        // First check if DN syntax is valid. Avoid making iDS call
        if (rfcDN == null) { // May be a exception thrown

            return false; // would be better here.
        }

        SSOTokenManager.getInstance().validateToken(token);

        if (debug.messageEnabled()) {
            debug.message("AMObjectImpl.isExists(): DN=" + entryDN);
        }

        return dsServices.doesEntryExists(token, entryDN);
    }

    public void setIntegerAttribute(String attributeName, int value)
            throws AMException, SSOException {
        setStringAttribute(attributeName, "" + value);
    }

    public int getIntegerAttribute(String attributeName) throws AMException,
            SSOException {
        Set attributeValue = getAttribute(attributeName);

        if (attributeValue.size() == 1) {
            try {
                String str = ((String) (attributeValue.iterator().next()));

                return Integer.parseInt(str);
            } catch (NumberFormatException nfex) {
                throw new AMException(AMSDKBundle.getString("152", locale),
                        "152");
            }
        }

        if (attributeValue.isEmpty()) {
            throw new AMException(AMSDKBundle.getString("153", locale), "153");
        }

        throw new AMException(AMSDKBundle.getString("152", locale), "152");
    }

    /**
     * Gets the object's organization.
     * 
     * @return The object's organization DN.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store or the object
     *             doesn't have organzation DN.
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public String getOrganizationDN() throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(token);

        if (organizationDN == null) {
            String startDN = entryDN;
            if (profileType == USER) {
                startDN = getParentDN();
            }
            organizationDN = dsServices.getOrganizationDN(token, startDN);
        }

        return organizationDN;
    }

    public String getParentDN() {
        if (entryDN.equalsIgnoreCase(AMStoreConnection.getAMSdkBaseDN())) {
            return null;
        } else {
            return new DN(entryDN).getParent().toString();
        }
    }

    public Map getPolicy(String serviceName)
            throws UnsupportedOperationException, AMException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMObjectImpl.getPolicy(" + serviceName + "): DN="
                    + entryDN);
        }

        try {
            Set serviceAttributeNames = AMServiceUtils
                    .getServiceAttributeNames(token, serviceName,
                            SchemaType.POLICY);
            Map map = getAttributes(serviceAttributeNames);

            return map;
        } catch (SMSException smsex) {
            if (debug.messageEnabled()) {
                debug.message("AMObjectImpl.getPolicy(" + serviceName + ")",
                        smsex);
            }

            throw new AMException(AMSDKBundle.getString("498", locale), "498");
        }
    }

    public AMTemplate getPolicyTemplate(String serviceName)
            throws UnsupportedOperationException, AMException, SSOException {
        return getTemplate(serviceName, AMTemplate.POLICY_TEMPLATE);
    }

    public Map getServiceAttributes(String serviceName) throws AMException,
            SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMObjectImpl.getServiceAttributes(" + serviceName
                    + "): DN=" + entryDN);
        }

        try {
            Set serviceAttributeNames = AMServiceUtils
                    .getServiceAttributeNames(token, serviceName,
                            SchemaType.DYNAMIC);

            Set set = AMServiceUtils.getServiceAttributeNames(token,
                    serviceName, SchemaType.GLOBAL);

            if (!set.isEmpty()) {
                if (serviceAttributeNames.isEmpty()) {
                    serviceAttributeNames = set;
                } else {
                    serviceAttributeNames.addAll(set);
                }
            }

            set = AMServiceUtils.getServiceAttributeNames(token, serviceName,
                    SchemaType.USER);

            if (!set.isEmpty()) {
                if (serviceAttributeNames.isEmpty()) {
                    serviceAttributeNames = set;
                } else {
                    serviceAttributeNames.addAll(set);
                }
            }

            return getAttributes(serviceAttributeNames);
        } catch (SMSException smsex) {
            if (debug.messageEnabled()) {
                debug.message("AMObjectImpl.getServiceAttributes("
                        + serviceName + ")", smsex);
            }

            throw new AMException(AMSDKBundle.getString("915", locale), "915");
        }
    }

    public void setServiceStatus(String sname, String status)
            throws AMException, SSOException {
        String stAttributeName = null;

        if (!((profileType == AMObject.ORGANIZATION)
                || (profileType == AMObject.USER)
                || (profileType == AMObject.STATIC_GROUP)
                || (profileType == AMObject.DYNAMIC_GROUP)
                || (profileType == AMObject.ORGANIZATIONAL_UNIT)
                || (profileType == AMObject.ASSIGNABLE_DYNAMIC_GROUP) 
                || (profileType == AMObject.GROUP))) {
            throw new UnsupportedOperationException();
        }

        // validate that this service is assigned to this entity.
        Set assignedServices = getAssignedServices();

        if (!assignedServices.contains(sname)) {
            throw new AMException(AMSDKBundle.getString("126", locale), "126");
        }

        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(sname, token);
            ServiceSchema ss = null;

            if (profileType == AMObject.USER) {
                ss = ssm.getSchema(SchemaType.USER);
            } else if ((profileType == AMObject.ORGANIZATION)
                    || (profileType == AMObject.ORGANIZATIONAL_UNIT)) {
                ss = ssm.getSchema(SchemaType.DOMAIN);
            } else if ((profileType == AMObject.STATIC_GROUP)
                    || (profileType == AMObject.DYNAMIC_GROUP)
                    || (profileType == AMObject.ASSIGNABLE_DYNAMIC_GROUP)
                    || (profileType == AMObject.GROUP)) {
                ss = ssm.getSchema(SchemaType.GROUP);
            }

            stAttributeName = ss.getStatusAttribute();
        } catch (SMSException se) {
            // throw new AMException
            debug.error("AMObjectImpl.setServiceStatus: " + "SMSException: ",
                    se);
            throw new AMException(AMSDKBundle.getString("908", locale), "908");
        }

        Set attrVal = new HashSet();
        attrVal.add(status);

        if (stAttributeName == null) {
            throw new AMException(AMSDKBundle.getString("705", locale), "705");
        }

        // TODO validate service attribute value.
        setAttribute(stAttributeName, attrVal);
        store();
    }

    public String getServiceStatus(String serviceName) throws AMException,
            SSOException {
        if (!((profileType == AMObject.ORGANIZATION)
                || (profileType == AMObject.USER)
                || (profileType == AMObject.STATIC_GROUP)
                || (profileType == AMObject.DYNAMIC_GROUP)
                || (profileType == AMObject.ORGANIZATIONAL_UNIT)
                || (profileType == AMObject.ASSIGNABLE_DYNAMIC_GROUP) 
                || (profileType == AMObject.GROUP))) {
            throw new UnsupportedOperationException();
        }

        String stAttributeName = null;

        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(serviceName,
                    token);
            ServiceSchema ss = null;

            if (profileType == AMObject.USER) {
                ss = ssm.getSchema(SchemaType.USER);
            } else if ((profileType == AMObject.ORGANIZATION)
                    || (profileType == AMObject.ORGANIZATIONAL_UNIT)) {
                ss = ssm.getSchema(SchemaType.DOMAIN);
            } else if ((profileType == AMObject.STATIC_GROUP)
                    || (profileType == AMObject.DYNAMIC_GROUP)
                    || (profileType == AMObject.ASSIGNABLE_DYNAMIC_GROUP)
                    || (profileType == AMObject.GROUP)) {
                ss = ssm.getSchema(SchemaType.GROUP);
            }

            if (ss != null) {
                stAttributeName = ss.getStatusAttribute();
            }
        } catch (SMSException se) {
            // throw new AMException
            debug.error("AMObjectImpl.getServiceStatus: " + "SMSException: ",
                    se);
            throw new AMException(AMSDKBundle.getString("908", locale), "908");
        }

        if (stAttributeName != null) {
            Set res = getAttribute(stAttributeName);

            if ((res == null) || res.isEmpty()) {
                return null;
            } else {
                Iterator it = res.iterator();

                return ((String) it.next());
            }
        }

        return (null);
    }

    /**
     * Sets string type attribute value.
     * 
     * @param attributeName
     *            Attribute name
     * @param value
     *            value to be set for the attributeName
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public void setStringAttribute(String attributeName, String value)
            throws AMException, SSOException {
        Set attrValue = new HashSet(1);
        attrValue.add(value);
        stringValueModMap.put(attributeName, attrValue);
    }

    public String getStringAttribute(String attributeName) throws AMException,
            SSOException {
        SSOTokenManager.getInstance().validateToken(token);

        Set attrName = new HashSet(1);
        attrName.add(attributeName);

        Map attributes = dsServices.getAttributes(token, entryDN, attrName,
                profileType);
        Set values = (Set) attributes.get(attributeName);

        if ((values != null) && (values.size() == 1)) {
            return (String) values.iterator().next();
        } else if ((values == null) || values.isEmpty()) {
            return "";
        } else {
            throw new AMException(AMSDKBundle.getString("150", locale), "150");
        }
    }

    // TODO: The right way to do these checks is to override the getTemplate()
    // methods in AMOrgTemplate, AMTemplate etc.
    public AMTemplate getTemplate(String serviceName, int templateType)
            throws UnsupportedOperationException, AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(token);

        if ((profileType != ORGANIZATION) && (profileType != ROLE)
                && (profileType != ORGANIZATIONAL_UNIT)
                && (profileType != FILTERED_ROLE)) {
            throw new UnsupportedOperationException();
        }

        // Organization template
        if (templateType == AMTemplate.ORGANIZATION_TEMPLATE) {
            if ((profileType != ORGANIZATIONAL_UNIT)
                    && (profileType != ORGANIZATION)) {
                throw new UnsupportedOperationException();
            }

            ServiceConfig sc = AMServiceUtils.getOrgConfig(token, rfcDN,
                    serviceName);

            if (sc == null) {
                Object[] args = { serviceName };
                throw new AMException(AMSDKBundle
                        .getString("480", args, locale), "480", args);
            }

            return new AMOrgTemplateImpl(token, sc.getDN(), serviceName, sc,
                    rfcDN);
        }

        if (debug.messageEnabled()) {
            debug.message("AMObjectImpl.getTemplate(" + serviceName + ", "
                    + templateType + "): DN=" + entryDN);
        }

        String templateDN = dsServices.getAMTemplateDN(token, rfcDN,
                profileType, serviceName, templateType);

        return new AMTemplateImpl(token, templateDN, serviceName, templateType);
    }

    /**
     * Register a AMEventListener that needs to be invoked when a relevant event
     * occurs. If the listener was already registered, then it is registered
     * only once; no duplicate registration is allowed.
     * <p>
     * {@link Object#equals Object.equals()} method on the listener object is
     * used to determine duplicates. <BR>
     * NOTE: This method does not check if the listener implementation object
     * exists in the directory, since it is brought from directory itself.
     * 
     * @param listener
     *            listener object that will be called upon when an event occurs.
     * 
     * @throws SSOException
     *             if errors were encountered in adding a new SSOTokenListener
     *             instance
     */
    public void addEventListener(AMEventListener listener) throws SSOException {
        SSOTokenManager.getInstance().validateToken(token);

        // Check if this AMObjectImpl has been added to the objImplListeners
        // Map.
        // if not added previously, then add one.
        if (!isRegistered) {
            // Make an entry for this SSOToken and dn in Profile Name table
            if (debug.messageEnabled()) {
                debug.message("AMObjectImpl.addEventListener(..): "
                        + "registering this instance to obj*Impl table");
            }

            try {
                addToProfileNameTable(token, entryDN);
            } catch (SSOException se) {
                debug.message("AMObjectImpl.addEventListener(): "
                        + se.toString());
                throw se;
            }

            synchronized (objImplListeners) {
                Set destObjs = (Set) objImplListeners.get(
		    entryDN.toLowerCase());

                if (destObjs == null) {
                    destObjs = new HashSet();
                    objImplListeners.put(entryDN.toLowerCase(), destObjs);
                }

                destObjs.add(this);

                // Since, this AMObjectImpl is registered, set isRegistered:true
                this.isRegistered = true;
            }
        }

        // Add the listener to this AMObjectImpl's list of registered listener
        // that need to be notifed.
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    // TODO: deprecated remove next release

    /**
     * Assigns the given policies to this object.
     * 
     * @param serviceName
     *            serviceName
     * @param policyDNs
     *            Set of policy DN string
     * 
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public void assignPolicies(String serviceName, Set policyDNs)
            throws AMException, SSOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Assigns a set of services and the attributes for a service to the user.
     * 
     * @param serviceNamesAndAttr
     *            Set of service names and the attributes for a service.
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     * @see com.iplanet.am.sdk.AMUserImpl#assignServices(java.util.Set)
     */
    public void assignServices(Map serviceNamesAndAttr) throws AMException,
            SSOException {
        assignServices(serviceNamesAndAttr, true);
    }

    /**
     * Assigns a set of services and the attributes for a service to the user.
     * 
     * @param serviceNamesAndAttr
     *            Set of service names and the attributes for a service.
     * @param store
     *            A boolean value. If the boolean value is 'true', 1) Checks if
     *            there is already an assigned service. 2) Checks if any of the
     *            assigned services are registered with the parent organization.
     *            3) Combines the old Object Classes and the new Object classes
     *            and assigns them for a service. If the boolean value is
     *            'false', 1) Assigns services without any check for existence
     *            of already assigned service. 2) Assigns only the new object
     *            classes.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     * @see com.iplanet.am.sdk.AMUserImpl#assignServices( java.util.Set
     *      serviceNames)
     */
    public void assignServices(Map serviceNamesAndAttr, boolean store)
            throws AMException, SSOException {
        if (!((profileType == AMObject.ORGANIZATION)
                || (profileType == AMObject.USER)
                || (profileType == AMObject.STATIC_GROUP)
                || (profileType == AMObject.DYNAMIC_GROUP)
                || (profileType == AMObject.ORGANIZATIONAL_UNIT)
                || (profileType == AMObject.ASSIGNABLE_DYNAMIC_GROUP) 
                || (profileType == AMObject.GROUP))) {
            throw new UnsupportedOperationException();
        }

        if ((serviceNamesAndAttr == null) || serviceNamesAndAttr.isEmpty()) {
            return;
        }

        Set newOCs = new HashSet();
        Set canAssign = new HashSet();

        if (store) {
            Set assignedServices = getAssignedServices();
            Set toAssign = serviceNamesAndAttr.keySet();
            Iterator it = toAssign.iterator();

            while (it.hasNext()) {
                // If already assigned service, then do nothing,
                // else add the servicename to services to be
                // assigned.
                String thisService = (String) it.next();

                if (!assignedServices.contains(thisService)) {
                    canAssign.add(thisService);
                } else {
                    if (debug.warningEnabled()) {
                        debug.warning("AMObjectImpl.assignService()-> "
                                + thisService + " is already assigned to "
                                + entryDN);
                    }
                }
            }

            /*
             * Check if any of the assigned services are registered with the
             * parent organization. If not then throw an exception. We cannot
             * assign a service which is not registered with the parent
             * organization.
             */
            Set registered = null;

            if (profileType == ORGANIZATION) {
                registered = dsServices
                        .getRegisteredServiceNames(null, entryDN);
            } else {
                registered = dsServices.getRegisteredServiceNames(null,
                        getOrganizationDN());
            }

            it = canAssign.iterator();
            while (it.hasNext()) {
                if (!registered.contains((String) it.next())) {
                    throw new AMException(AMSDKBundle.getString("126", locale),
                            "126");
                }
            }
        } else {
            canAssign = serviceNamesAndAttr.keySet();
        }
        newOCs = AMServiceUtils.getServiceObjectClasses(token, canAssign);

        if (store) {
            Set oldOCs = getAttribute("objectclass");
            newOCs = AMCommonUtils.combineOCs(newOCs, oldOCs);
        }
        setAttribute("objectclass", newOCs);

        Iterator it = canAssign.iterator();

        while (it.hasNext()) {
            String thisService = (String) it.next();
            Map attrMap = (Map) serviceNamesAndAttr.get(thisService);

            if ((attrMap == null) || attrMap.isEmpty()) {
                attrMap = new HashMap();
            }

            /*
             * Check if the service has the schema type specified for the
             * respective profile type. If not throw an exception. The object
             * class is assigned above even if the schema type is not specified.
             * The reason behind this is to support the "COS" type attributes.
             */

            try {
                ServiceSchemaManager ssm = new ServiceSchemaManager(
                        thisService, token);
                ServiceSchema ss = null;
                Object args[] = { thisService };

                if (profileType == AMObject.USER) {
                    ss = ssm.getSchema(SchemaType.USER);
                    if (ss == null) {
                        ss = ssm.getSchema(SchemaType.DYNAMIC);
                    }
                } else if ((profileType == AMObject.ORGANIZATION)
                        || (profileType == AMObject.ORGANIZATIONAL_UNIT)) {
                    ss = ssm.getSchema(SchemaType.DOMAIN);
                } else if ((profileType == AMObject.STATIC_GROUP)
                        || (profileType == AMObject.DYNAMIC_GROUP)
                        || (profileType == AMObject.ASSIGNABLE_DYNAMIC_GROUP)
                        || (profileType == AMObject.GROUP)) {
                    ss = ssm.getSchema(SchemaType.GROUP);
                }

                if (ss == null) {
                    debug.warning(AMSDKBundle.getString("1001"));
                    throw new AMException(AMSDKBundle.getString("1001", args,
                            locale), "1001", args);
                }

                if (ss.getServiceType() != SchemaType.DYNAMIC) {
                    attrMap = ss.validateAndInheritDefaults(attrMap, true);
                }

                /*
                 * Below we iterate through the attribute map to remove any
                 * attribute that do not have values (empty set) This is because
                 * the default behaviour when doing "setAttributes" with
                 * attributes containing no values is to "delete" that attribute
                 * from the entry. this is not the behaviour we want so the
                 * below check is a precaution to avoid that behaviour.
                 */

                attrMap = AMCommonUtils.removeEmptyValues(attrMap);
            } catch (SMSException smse) {
                debug.error("AMObjectImpl:assignService-> "
                        + "unable to validate attributes for " + thisService,
                        smse);
                throw new AMException(AMSDKBundle.getString("908", locale),
                        "908");
            }

            // TODO validate the attributes here...
            setAttributes(attrMap);
        }

        if (store) {
            store();
        }
    }

    public void create() throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(token);

        if (debug.messageEnabled()) {
            debug.message("AMObjectImpl.create(): DN=" + entryDN);
        }

        if (rfcDN == null) {
            throw new AMInvalidDNException(
                    AMSDKBundle.getString("157", locale), "157");
        }

        DN dn = new DN(entryDN);
        String parentDN = dn.getParent().toString();
        String name = ((RDN) dn.getRDNs().get(0)).getValues()[0];

        // validateAttributeUniqueness(true);
        if (profileType == USER) {
            AMUserImpl admin = new AMUserImpl(token, token.getPrincipal()
                    .getName());
            Set roleDNs = admin.getRoleDNs();

            if (roleDNs.size() > 0) {
                stringValueModMap.put("iplanet-am-modifiable-by", roleDNs);
            }
            stringValueModMap = integrateLocale();
        }

        dsServices.createEntry(token, name, profileType, parentDN,
                stringValueModMap);

        if ((profileType == ORGANIZATION)
                || (profileType == ORGANIZATIONAL_UNIT)) {
            String peopleDN = AMNamingAttrManager
                    .getNamingAttr(PEOPLE_CONTAINER)
                    + "=People," + entryDN;

            try {
                AMPeopleContainerImpl people = new AMPeopleContainerImpl(token,
                        peopleDN);
                people.createAdminRole();
            } catch (Exception ex) {
                if (debug.messageEnabled()) {
                    debug.message("AMObject.create: "
                            + "Unable to create admin role for " + peopleDN
                            + ex);
                }
            }
        }

        if ((profileType == ORGANIZATION)
                || (profileType == ORGANIZATIONAL_UNIT)) {
            String adminRoleName;
            String helpRoleName;
            String adminRolePermission;
            String helpRolePermission;

            if (profileType == ORGANIZATION) {
                adminRoleName = "Organization Admin Role";
                helpRoleName = "Organization Help Desk Admin Role";
                adminRolePermission = "Organization Admin";
                helpRolePermission = "Organization Help Desk Admin";
            } else {
                adminRoleName = "Container Admin Role";
                helpRoleName = "Container Help Desk Admin Role";
                adminRolePermission = "Container Admin";
                helpRolePermission = "Container Help Desk Admin";
            }

            String adminRoleDN = AMNamingAttrManager.getNamingAttr(ROLE) + "="
                    + adminRoleName + "," + entryDN;
            AMRole adminRole = new AMRoleImpl(token, adminRoleDN);

            if (adminRole.isExists()) {
                try {
                    setRoleAciDescAciList(adminRole, adminRolePermission);
                    setAciForRole(adminRole);
                } catch (Exception ex) {
                    if (debug.messageEnabled()) {
                        debug.message("AMObject.create: "
                                + "Unable to set aci or org admin role. ", ex);
                    }
                }

                try {
                    adminRole.setStringAttribute(
                            ROLE_MANAGED_CONTAINER_DN_ATTRIBUTE, entryDN);
                    adminRole.store();
                } catch (Exception ex) {
                    if (debug.warningEnabled()) {
                        debug.warning("AMObject.create: Unable to set " 
                                + "managed dn for org admin role.", ex);
                    }
                }
            }

            String helpRoleDN = AMNamingAttrManager.getNamingAttr(ROLE) + "="
                    + helpRoleName + "," + entryDN;

            AMRole helpRole = new AMRoleImpl(token, helpRoleDN);

            if (helpRole.isExists()) {
                try {
                    setRoleAciDescAciList(helpRole, helpRolePermission);
                    setAciForRole(helpRole);
                } catch (Exception ex) {
                    if (debug.messageEnabled()) {
                        debug.message("AMObject.create: Unable"
                                + " to set aci or org help desk admin role. ",
                                ex);
                    }
                }

                try {
                    helpRole.setStringAttribute(
                            ROLE_MANAGED_CONTAINER_DN_ATTRIBUTE, entryDN);
                    helpRole.store();
                } catch (Exception ex) {
                    if (debug.warningEnabled()) {
                        debug.warning("AMObject.create: Unable to set " 
                                + "managed dn for org help role.", ex);
                    }
                }
            }

            String policyAdminRoleDN = AMNamingAttrManager.getNamingAttr(ROLE)
                    + "=" + POLICY_ADMIN_ROLE_NAME + "," + entryDN;
            AMRole policyAdminRole = new AMRoleImpl(token, policyAdminRoleDN);

            if (policyAdminRole.isExists()) {
                try {
                    setRoleAciDescAciList(policyAdminRole,
                            POLICY_ADMIN_ROLE_PERMISSION);
                    setAciForRole(policyAdminRole);
                } catch (Exception ex) {
                    if (debug.messageEnabled()) {
                        debug.message("AMObject.create: Unable to set aci " 
                                + "or org policy admin role. ", ex);
                    }
                }

                try {
                    policyAdminRole.setStringAttribute(
                            ROLE_MANAGED_CONTAINER_DN_ATTRIBUTE, entryDN);
                    policyAdminRole.store();
                } catch (Exception ex) {
                    if (debug.warningEnabled()) {
                        debug.warning("AMObject.create: Unable to set " 
                                + "managed dn for org policy admin role.",  ex);
                    }
                }
            }
        } else if ((profileType == GROUP) || (profileType == DYNAMIC_GROUP)
                || (profileType == ASSIGNABLE_DYNAMIC_GROUP)
                || (profileType == PEOPLE_CONTAINER)) {
            try {
                if (!AMDCTree.isRequired()) {
                    createAdminRole();
                }
            } catch (Exception ex) {
                if (debug.messageEnabled()) {
                    debug.message("AMObject.create: Unable to create admin" +
                            " role for " + entryDN + ex);
                }
            }
        } else if ((profileType == ROLE) || (profileType == FILTERED_ROLE)) {
            setAciForRole((AMRole) this);
        }

        if ((profileType == GROUP) || (profileType == DYNAMIC_GROUP)
                || (profileType == ASSIGNABLE_DYNAMIC_GROUP)) {
            try {
                if (!AMDCTree.isRequired()) {
                    setAciBasedOnGroupPclist();
                }
            } catch (Exception ex) {
                if (debug.messageEnabled()) {
                    debug.message("AMObject.create: "
                            + "Unable to set aci based on "
                            + "group pclist for " + entryDN + ex);
                }
            }
        }

        stringValueModMap.clear();

        if (!byteValueModMap.isEmpty()) {
            byteValueModMap.clear();
        }
    }

    // TODO: deprecated remove next release
    public AMTemplate createPolicyTemplate(String serviceName, Map attributes)
            throws UnsupportedOperationException, AMException, SSOException {
        return createPolicyTemplate(serviceName, attributes,
                AMTemplate.UNDEFINED_PRIORITY);
    }

    public AMTemplate createPolicyTemplate(String serviceName, Map attributes,
            int priority) throws UnsupportedOperationException, AMException,
            SSOException {
        return createTemplate(AMTemplate.POLICY_TEMPLATE, serviceName,
                attributes, priority);
    }

    public AMTemplate createTemplate(int templateType, String serviceName,
            Map attributes) throws UnsupportedOperationException, AMException,
            SSOException {
        return createTemplate(templateType, serviceName, attributes,
                AMTemplate.UNDEFINED_PRIORITY);
    }

    public AMTemplate createTemplate(int templateType, String serviceName,
            Map attributes, int priority) throws UnsupportedOperationException,
            AMException, SSOException {
        return createTemplate(templateType, serviceName, attributes, priority,
                null);
    }

    // TODO: TBD these createTemplate API's should be moved to Org, OrgUnit,
    // Role etc. Also, The right way to do these checks is to override the
    // getTemplate() methods in AMOrgTemplate, AMTemplate etc.
    public AMTemplate createTemplate(int templateType, String serviceName,
            Map attributes, int priority, Set policyDNs)
            throws UnsupportedOperationException, AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(token);

        if ((profileType != ORGANIZATION) && (profileType != ROLE)
                && (profileType != ORGANIZATIONAL_UNIT)
                && (profileType != FILTERED_ROLE)) {
            throw new UnsupportedOperationException();
        }

        // If template type is an Org template
        if (templateType == AMTemplate.ORGANIZATION_TEMPLATE) {
            if ((profileType != ORGANIZATIONAL_UNIT)
                    && (profileType != ORGANIZATION)) {
                throw new UnsupportedOperationException();
            }

            ServiceConfig sc = AMServiceUtils.createOrgConfig(token, entryDN,
                    serviceName, attributes);

            return new AMOrgTemplateImpl(token, sc.getDN(), serviceName, sc,
                    entryDN);
        }

        if (debug.messageEnabled()) {
            debug.message("AMObjectImpl.createTemplate(" + templateType + ", "
                    + serviceName + ", Map, " + priority + "): DN=" + entryDN);
        }

        // If the template type is DYNAMIC
        if (attributes == null) {
            try {
                attributes = AMServiceUtils.getServiceConfig(token,
                        serviceName, SchemaType.DYNAMIC);
            } catch (SMSException smsex) {
                if (debug.messageEnabled()) {
                    debug.message("AMObjectImpl.createTemplate(" + templateType
                            + ", " + serviceName + ", Map, " + priority + ")",
                            smsex);
                }

                throw new AMException(AMSDKBundle.getString("451", locale),
                        "451");
            }
        }

        ServiceSchemaManager ssm = null;
        ServiceSchema ss = null;

        try {
            ssm = new ServiceSchemaManager(serviceName, token);
            ss = ssm.getSchema(SchemaType.DYNAMIC);
        } catch (SMSException sme) {
            debug.error("AMObjectImpl.createTemplate()", sme);
            throw new AMException(AMSDKBundle.getString("484", locale), "484");
        }

        if (ss == null) {
            throw new AMException(AMSDKBundle.getString("484", locale), "484");
        }

        attributes = AMCrypt.encryptPasswords(attributes, ss);

        if (debug.messageEnabled()) {
            debug.message("AMObjectImpl.createTemplate(): attributes "
                    + "encrypted: " + attributes);
        }
        // Fix for comms integration (locale integration)
        attributes = integrateLocaleForTemplateCreation(attributes);
        // Only Dynamic template needs to be created
        String templateDN = dsServices.createAMTemplate(token, rfcDN,
                profileType, serviceName, attributes, priority);

        return new AMTemplateImpl(token, templateDN, serviceName, templateType);
    }

    /**
     * Deletes object. This method takes a boolean parameter, if its value is
     * true, will remove all sub entries and the object itself, otherwise, will
     * try to remove the object only. Two notes on recursive delete. First, be
     * aware of the PERFORMANCE hit when large amount of child objects present.
     * Second, it won't follow referral.
     * 
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public void delete(boolean recursive) throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(token);

        if (AMCompliance.isComplianceUserDeletionEnabled()) {
            switch (profileType) {
            case ORGANIZATION:
            case ORGANIZATIONAL_UNIT:
            case USER:
            case ASSIGNABLE_DYNAMIC_GROUP:
            case DYNAMIC_GROUP:
            case STATIC_GROUP:
            case GROUP:
            case RESOURCE:
                // %%% TODO Notification
                AMCompliance.verifyAndDeleteObject(token, rfcDN);
                return;

            case ROLE:
            case FILTERED_ROLE:
                purge(recursive, -1);

                return;
            case PEOPLE_CONTAINER:
                // Entities can be created under people container.
                // If that's used extensively, the search filter should
                // include all managed objects under this node.

                String pcFilter = getSearchFilter(AMObject.PEOPLE_CONTAINER);
                String userFilter = getSearchFilter(AMObject.USER);
                StringBuilder sb = new StringBuilder();
                sb.append("(|").append(pcFilter).append(userFilter);
                sb.append(")");
                String filter = sb.toString();
                Set pcEntries = new HashSet();
                try {
                    pcEntries = search(AMConstants.SCOPE_ONE, filter);
                } catch (AMException ame) {
                    String ldapErr = ame.getLDAPErrorCode();
                    int ldapError = Integer.parseInt(ldapErr);
                    if (ldapErr != null && (ldapError == 4 || ldapError == 11)) 
                    {
                        String locale = AMCommonUtils.getUserLocale(token);
                        throw new AMException(AMSDKBundle.getString("977",
                                locale), "977");
                    } else {
                        if (debug.messageEnabled()) {
                            debug.message("AMObjectImpl.delete people "
                                    + "container " + ame);
                        }
                        throw ame;
                    }
                }
                if (pcEntries != null && !pcEntries.isEmpty()) {
                    throw new AMException(AMSDKBundle.getString("977", locale),
                            "977");
                } else {
                    purge(recursive, -1);
                    return;
                }
            case GROUP_CONTAINER:
                String gcFilter = getSearchFilter(AMObject.GROUP_CONTAINER);
                String groupFilter = getSearchFilter(AMObject.GROUP);
                StringBuilder sbf = new StringBuilder();
                sbf.append("(|").append(gcFilter).append(groupFilter);
                sbf.append(")");
                String flt = sbf.toString();
                Set gcEntries = new HashSet();
                try {
                    gcEntries = search(AMConstants.SCOPE_ONE, flt);
                } catch (AMException ame) {
                    String ldapErr = ame.getLDAPErrorCode();
                    int ldapError = Integer.parseInt(ldapErr);
                    if (ldapErr != null && (ldapError == 4 || ldapError == 11)) 
                    {
                        throw new AMException(AMSDKBundle.getString("977",
                                locale), "977");
                    } else {
                        if (debug.messageEnabled()) {
                            debug.message("AMObjectImpl.delete group container "
                                    + ame);
                        }
                        throw ame;
                    }
                }
                if (gcEntries != null && !gcEntries.isEmpty()) {
                    throw new AMException(AMSDKBundle.getString("977", locale),
                            "977");
                } else {
                    purge(recursive, -1);
                    return;
                }
            default:
                /*
                 * If none of the above, then this case is for Printers, other
                 * devices, Agents etc., This is for the dynamic
                 * objects-AMEntities, configured through DAI service.
                 */
                purge(recursive, -1);
                return;
            }
        } else {
            purge(recursive, -1);
        }
    }

    /**
     * Removes and destroys the object.
     * 
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public void delete() throws AMException, SSOException {
        delete(false);
    }

    public void modifyService(String sname, Map attrMap) throws AMException,
            SSOException {
        if (!((profileType == AMObject.ORGANIZATION)
                || (profileType == AMObject.USER)
                || (profileType == AMObject.STATIC_GROUP)
                || (profileType == AMObject.DYNAMIC_GROUP)
                || (profileType == AMObject.ORGANIZATIONAL_UNIT)
                || (profileType == AMObject.ASSIGNABLE_DYNAMIC_GROUP) 
                || (profileType == AMObject.GROUP))) {
            throw new UnsupportedOperationException();
        }

        if ((attrMap == null) || attrMap.isEmpty() || (sname == null)) {
            return;
        }

        Set assignedServices = getAssignedServices();

        if (!assignedServices.contains(sname)) {
            throw new AMException(AMSDKBundle.getString("126", locale), "126");
        }

        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(sname, token);
            ServiceSchema ss = null;

            if (profileType == AMObject.USER) {
                ss = ssm.getSchema(SchemaType.USER);
            } else if ((profileType == AMObject.ORGANIZATION)
                    || (profileType == AMObject.ORGANIZATIONAL_UNIT)) {
                ss = ssm.getSchema(SchemaType.DOMAIN);
            } else if ((profileType == AMObject.STATIC_GROUP)
                    || (profileType == AMObject.DYNAMIC_GROUP)
                    || (profileType == AMObject.ASSIGNABLE_DYNAMIC_GROUP)
                    || (profileType == AMObject.GROUP)) {
                ss = ssm.getSchema(SchemaType.GROUP);
            }

            ss.validateAttributes(attrMap, getOrganizationDN());
        } catch (SMSException smse) {
            debug.error("AMObjectImpl:modifyService-> "
                    + "unable to validate attributes for " + sname, smse);
            Object args[] = { sname };
            throw new AMException(AMSDKBundle.getString("976", args, locale),
                    "976", args);
        }

        // TODO validate the attributes here...
        setAttributes(attrMap);
        store();
    }

    /**
     * Method to hard Delete an object.
     * 
     */
    public void purge(boolean recursive, int graceperiod) throws AMException,
            SSOException {
        if (debug.messageEnabled()) {
            debug
                    .message("AMObjectImpl.delete(): DN=" + entryDN
                            + " recursive=" + recursive + "graceperiod= "
                            + graceperiod);
        }

        if (entryDN.equals(AMStoreConnection.defaultOrg)) {
            throw new AMException(AMSDKBundle.getString("160", locale), "160");
        }

        if ((graceperiod > -1)
                && (graceperiod > AMStoreConnection.daysSinceModified(token,
                        entryDN))) {
            // Return with a logged message. Cannot purge till grace period
            // has expired.
            if (debug.messageEnabled()) {
                debug.message("AMObjectImpl.purge-> " + entryDN
                        + "will not be purged. Grace period= " + graceperiod
                        + " has not expired");
            }

            throw new AMException(AMSDKBundle.getString("974", locale), "974");
        }

        if ((profileType == GROUP) || (profileType == DYNAMIC_GROUP)
                || (profileType == ASSIGNABLE_DYNAMIC_GROUP)
                || (profileType == PEOPLE_CONTAINER)) {
            try {
                removeAdminRoleAci(recursive);
            } catch (Exception e) {
                if (debug.messageEnabled()) {
                    debug.message("AMObjectImpl.delete: Unable to remove "
                            + "admin role aci." + e);
                }
            }

            try {
                // remove the group admin role
                dsServices.removeAdminRole(token, entryDN, recursive);
            } catch (Exception e) {
                // probably because admin role does not exist, ignore
                if (debug.messageEnabled()) {
                    debug.message("AMObjectImpl.delete: " + e.getMessage());
                }
            }
        }

        Set aciList = null;
        Set templateDNs = null;

        if ((profileType == ROLE) || (profileType == FILTERED_ROLE)) {
            aciList = findRemovableAciList(getAttribute(
                    "iplanet-am-role-aci-list"));

            String filter = "(&(objectclass=costemplate)(cn=\"" + entryDN
                    + "\"))";
            templateDNs = dsServices.search(token, getOrganizationDN(), filter,
                    AMConstants.SCOPE_SUB);
        }

        dsServices.removeEntry(token, entryDN, profileType, recursive, false);

        if (aciList != null) {
            removeAci(aciList);
        }

        if ((templateDNs != null) && !templateDNs.isEmpty()) {
            Iterator iter = templateDNs.iterator();

            while (iter.hasNext()) {
                String templateDN = (String) iter.next();
                dsServices.removeEntry(token, templateDN, AMObject.TEMPLATE,
                        recursive, false);
            }
        }

        stringValueModMap.clear();

        if (!byteValueModMap.isEmpty()) {
            byteValueModMap.clear();
        }
    }

    public void removeAttributes(Set attrNames) throws AMException,
            SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMObjectImpl.removeAttributes(Set): DN=" + entryDN
                    + "\n" + attrNames);
        }

        Iterator itr = attrNames.iterator();
        Map attributes = new HashMap(attrNames.size());
        Map altAttributes = new HashMap();
        while (itr.hasNext()) {
            String name = (String) itr.next();
            attributes.put(name, AMConstants.REMOVE_ATTRIBUTE);
            if (AMCommonUtils.integrateLocale
                    && name.equalsIgnoreCase("preferredLanguage")) {
                altAttributes.put("preferredLocale",
                        AMConstants.REMOVE_ATTRIBUTE);
            }
            if (AMCommonUtils.integrateLocale
                    && name.equalsIgnoreCase("preferredLocale")) {
                altAttributes.put("preferredLanguage",
                        AMConstants.REMOVE_ATTRIBUTE);
            }
        }

        // Removing attributes we don't care if they are string valued or byte
        // valued
        dsServices.setAttributes(token, entryDN, profileType, attributes, null,
                false);
        // Fix for comms backward compatibility. If this operation causes
        // exceptions (due to the attribute not being there etc.) we ignore
        // them.
        if (!altAttributes.isEmpty()) {
            try {
                dsServices.setAttributes(token, entryDN, profileType,
                        altAttributes, null, false);
            } catch (Exception e) {
                // ignore exceptions
            }
        }

        // Remove the attribute names from the local copy of these maps
        stringValueModMap.removeKeys(attrNames);

        if (!byteValueModMap.isEmpty()) {
            byteValueModMap.removeKeys(attrNames);
        }
    }

    /**
     * UnRegister a previously registered event listener. If the
     * <code>listener</code> was not registered, the method simply returns
     * without doing anything.
     * <p>
     * 
     * @param listener
     *            listener object that will be removed or unregistered.
     */
    public void removeEventListener(AMEventListener listener) {
        // Remove the listener from the AMObjectImpl's private listener list
        boolean removed = false;

        synchronized (listeners) {
            removed = listeners.remove(listener);
        }

        // Remove this AMObjectImpl from the objImplListeners table if
        // it does not have any private listeners
        if (removed && listeners.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message("AMObjectImpl.removeEventListener(..): "
                        + "private listener table empty for this instance");
            }

            synchronized (objImplListeners) {
                Set destObjs = (Set) objImplListeners.get(
		    entryDN.toLowerCase());

                if (destObjs != null) {
                    destObjs.remove(this);

                    if (destObjs.isEmpty()) {
                        objImplListeners.remove(entryDN);
                    }
                }

                // Since, this AMObjectImpl does'nt have any private listeners
                // set isRegistered:false
                this.isRegistered = false;
            }

            // Remove the (SSOToken,dn) for this AMObjectImpl from the
            // Profile Name table.
            removeFromProfileNameTable(token, entryDN);
        }
    }

    public Set search(int level, String filter) throws AMException,
            SSOException {
        SSOTokenManager.getInstance().validateToken(token);

        if (debug.messageEnabled()) {
            debug.message("AMObjectImpl.search(" + level + ", " + filter
                    + "): DN=" + entryDN);
        }

        return dsServices.search(token, entryDN, filter, level);
    }

    public Set searchObjects(String namingAttr, String objectClassFilter,
            String wildcard, Map avPairs, int level) throws AMException,
            SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMObjectImpl.searchObjects(" + namingAttr + ", "
                    + objectClassFilter + ", " + wildcard + ", Map): DN="
                    + entryDN + ", level " + level + "\n"
                    + mapToString(avPairs));
        }

        StringBuilder filterSB = new StringBuilder();

        filterSB.append("(&").append(
                constructFilter(namingAttr, objectClassFilter, wildcard));

        if ((avPairs != null) && (avPairs.size() > 0)) {
            filterSB.append(constructFilter(avPairs));
        }

        filterSB.append(")");

        if (debug.messageEnabled()) {
            debug.message("    filter: " + filterSB.toString());
        }

        return search(level, filterSB.toString());
    }

    public void store() throws AMException, SSOException {
        store(false);
    }

    public void store(boolean isAdd) throws AMException, SSOException {
        try {
            SSOTokenManager.getInstance().validateToken(token);

            if (debug.messageEnabled()) {
                if (stringValueModMap.containsKey("userpassword")) {
                    Map noPasswdMap = stringValueModMap.getCopy();
                    noPasswdMap.remove("userpassword");
                    Set set = new HashSet(2);
                    set.add("********");
                    noPasswdMap.put("userpassword", set);
                    debug.message("AMObjectImpl.store(): DN=" + entryDN + "\n"
                            + AMCommonUtils.mapSetToString(noPasswdMap));
                } else {
                    debug.message("AMObjectImpl.store(): DN=" + entryDN + "\n"
                            + AMCommonUtils.mapSetToString(stringValueModMap));
                }
            }

            // If name space is enabled, verify that the attributes
            // being set are allowed by name space constrictions.
            // validateAttributeUniqueness(false);

            // if ORGANIZATION then you might need to add the
            // sunISManagedOrganization OC
            if (profileType == ORGANIZATION
                    && stringValueModMap.containsKey("sunOrganizationAlias")) {
                Set currentOC = getAttribute("objectclass");
                Set ocSet = (Set) stringValueModMap.get("objectclass");
                if (ocSet == null || ocSet == Collections.EMPTY_SET) {
                    ocSet = currentOC;
                } else {
                    ocSet = AMCommonUtils.combineOCs(ocSet, currentOC);
                }

                // object class are case insensitive.
                boolean hasIt = false;
                Iterator itr = ocSet.iterator();
                while (itr.hasNext()) {
                    String value = (String) itr.next();
                    if (value.equalsIgnoreCase("sunISManagedOrganization")) {
                        hasIt = true;
                        break;
                    }
                }
                if (!hasIt) {
                    ocSet.add("sunISManagedOrganization");
                }

                stringValueModMap.put("objectclass", ocSet);
            }
            // Fix for comms integration of user locale.
            stringValueModMap = integrateLocale();
            Set oldAciList = null;

            if (stringValueModMap.containsKey("iplanet-am-role-aci-list")) {
                try {
                    oldAciList = findRemovableAciList(getAttribute(
                            "iplanet-am-role-aci-list"));
                } catch (Exception ex) {
                    if (debug.messageEnabled()) {
                        debug.message("AMObjectImpl.store: Failed "
                                + "to get old iplanet-am-role-aci-list");
                    }
                }
            }

            Set oldUM = null;

            if ((profileType == GROUP)
                    && stringValueModMap.containsKey(UNIQUE_MEMBER_ATTRIBUTE)) {
                try {
                    oldUM = getAttribute(UNIQUE_MEMBER_ATTRIBUTE);
                } catch (Exception ex) {
                    if (debug.messageEnabled()) {
                        debug.message("AMObjectImpl.store: Failed "
                                + "to get old uniquemember");
                    }
                }
            }

            dsServices.setAttributes(token, entryDN, profileType,
                    stringValueModMap, byteValueModMap, isAdd);

            if (stringValueModMap.containsKey("iplanet-am-role-aci-list")) {
                try {
                    removeAci(oldAciList);
                } catch (Exception ex) {
                    if (debug.messageEnabled()) {
                        debug.message("AMObjectImpl.store: Failed "
                                + "to remove old acis");
                    }
                }

                try {
                    setAciForRole(new AMRoleImpl(token, entryDN));
                } catch (Exception ex) {
                    if (debug.messageEnabled()) {
                        debug.message("AMObjectImpl.store: Failed "
                                + "to add new acis");
                    }
                }
            }

            if ((profileType == GROUP)
                    && stringValueModMap.containsKey(UNIQUE_MEMBER_ATTRIBUTE)) {
                if (oldUM != null) {
                    dsServices
                            .updateUserAttribute(token, oldUM, entryDN, false);
                }

                Set set = (Set) stringValueModMap.get(UNIQUE_MEMBER_ATTRIBUTE);
                dsServices.updateUserAttribute(token, set, entryDN, true);
            }
        } finally {
            stringValueModMap.clear();
            byteValueModMap.clear();
        }
    }

    // TODO: deprecated remove next release

    /**
     * Unassigns the given policies from this object.
     * 
     * @param serviceName
     *            serviceName
     * @param policyDNs
     *            Set of policy DN string
     * 
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public void unassignPolicies(String serviceName, Set policyDNs)
            throws AMException, SSOException {
        unassignPolicies(serviceName, policyDNs, true);
    }

    /**
     * Unassigns services from the user.
     * 
     * @param serviceNames
     *            Set of service names
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void unassignServices(Set serviceNames) throws AMException,
            SSOException {
        if ((serviceNames == null) || serviceNames.isEmpty()) {
            return;
        }

        // TODO: UnsupportedOperationException should move to the sub classes
        // No check here!
        if (!((profileType == AMObject.ORGANIZATION)
                || (profileType == AMObject.USER)
                || (profileType == AMObject.STATIC_GROUP)
                || (profileType == AMObject.DYNAMIC_GROUP)
                || (profileType == AMObject.ORGANIZATIONAL_UNIT)
                || (profileType == AMObject.ASSIGNABLE_DYNAMIC_GROUP) 
                || (profileType == AMObject.GROUP))) {
            throw new UnsupportedOperationException();
        }

        // Verify if you are trying to remove an unassigned service
        Set assignedServices = getAssignedServices();
        Iterator iter = serviceNames.iterator();

        while (iter.hasNext()) {
            String serviceName = (String) iter.next();

            if (!assignedServices.contains(serviceName)) {
                debug.error(AMSDKBundle.getString("126", locale));
                throw new AMException(AMSDKBundle.getString("126", locale),
                        "126");
            }
        }

        // Get the object classes that need to be remove from Service Schema
        Set removeOCs = AMServiceUtils.getServiceObjectClasses(token,
                serviceNames);
        Set objectClasses = getAttribute("objectclass");
        removeOCs = AMCommonUtils.updateAndGetRemovableOCs(objectClasses,
                removeOCs);

        // Get the attributes that need to be removed
        Set removeAttrs = new HashSet();

        // SchemaManager sm = SchemaManager.getSchemaManager(token);
        Iterator iter1 = removeOCs.iterator();

        while (iter1.hasNext()) {
            String oc = (String) iter1.next();

            // TODO: Modify SchemaManager.getAttributes() to return
            // lowercase attribute names.
            Set attrs = dsServices.getAttributesForSchema(oc);
            Iterator iter2 = attrs.iterator();

            while (iter2.hasNext()) {
                String attrName = (String) iter2.next();
                removeAttrs.add(attrName.toLowerCase());
            }
        }

        // Will be AMHashMap, So the attr names will be in lower case
        Map avPair = getAttributes();
        Iterator itr = avPair.keySet().iterator();

        while (itr.hasNext()) {
            String attrName = (String) itr.next();

            if (removeAttrs.contains(attrName)) {
                try {
                    // remove attribute one at a time, so if the first
                    // one fails, it will keep continue to remove
                    // other attributes.
                    Set tmpSet = new HashSet();
                    tmpSet.add(attrName);
                    removeAttributes(tmpSet);
                } catch (Exception ex) {
                    if (debug.messageEnabled()) {
                        debug.message("AMUserImpl.unassignServices()"
                                + "Error occured while removing attribute: "
                                + attrName);
                    }
                }
            }
        }

        // Now update the object class attribute
        setAttribute("objectclass", objectClasses);
        store();
    }

    protected static String constructFilter(String objectClassFilter) {
        int index = objectClassFilter.indexOf("%U");

        if (index == -1) {
            return objectClassFilter;
        }

        StringBuilder filterSB = new StringBuilder();
        filterSB.append(objectClassFilter.substring(0, index)).append("*");

        int index2 = objectClassFilter.indexOf("%V");

        if (index2 == -1) {
            filterSB.append(objectClassFilter.substring(index + 2));
        } else {
            filterSB.append(objectClassFilter.substring(index + 2, index2))
                    .append("*")
                    .append(objectClassFilter.substring(index2 + 2));
        }

        return filterSB.toString();
    }

    protected static String constructFilter(String namingAttr,
            String objectClassFilter, String wildcard) {
        StringBuffer filterSB = new StringBuffer();
        int index = objectClassFilter.indexOf("%U");
        int vIndex = objectClassFilter.indexOf("%V");

        if ((index == -1) && (vIndex == -1)) {
            filterSB.append("(&(").append(namingAttr).append("=").append(
                    wildcard).append(")").append(objectClassFilter).append(")");
            objectClassFilter = filterSB.toString();

            return (objectClassFilter);
        } else {
            while (index != -1) {
                filterSB.append(objectClassFilter.substring(0, index)).append(
                        wildcard)
                        .append(objectClassFilter.substring(index + 2));
                objectClassFilter = filterSB.toString();
                filterSB = new StringBuffer();
                index = objectClassFilter.indexOf("%U");
            }

            // int index2 = objectClassFilter.indexOf("%V");
            while (vIndex != -1) {
                filterSB.append(objectClassFilter.substring(0, vIndex)).append(
                        wildcard).append(
                        objectClassFilter.substring(vIndex + 2));
                objectClassFilter = filterSB.toString();
                filterSB = new StringBuffer();
                vIndex = objectClassFilter.indexOf("%V");
            }
        }

        return objectClassFilter;
    }

    protected static String constructFilter(Map avPairs) {
        StringBuilder filterSB = new StringBuilder();
        filterSB.append("(&");

        Iterator iter = avPairs.keySet().iterator();

        while (iter.hasNext()) {
            String attributeName = (String) (iter.next());
            Iterator iter2 = ((Set) (avPairs.get(attributeName))).iterator();

            while (iter2.hasNext()) {
                String attributeValue = (String) iter2.next();
                filterSB.append("(").append(attributeName).append("=").append(
                        attributeValue).append(")");
            }
        }

        filterSB.append(")");

        return filterSB.toString();
    }

    protected String getSearchFilter(int objectType) {
        return getSearchFilter(objectType, null);
    }

    protected String getSearchFilter(int objectType, String searchTempName) {
        try {
            return AMSearchFilterManager.getSearchFilter(objectType,
                    getOrganizationDN(), searchTempName, false);
        } catch (Exception ex) {
            return AMSearchFilterManager.getGlobalSearchFilter(objectType,
                    searchTempName);
        }
    }

    /**
     * Notifies ACI Change. This method will be called by the
     * <code>AMIdRepoListener to send
     * notifications to all interested AMObjectImp's whenever an ACI
     * change occurs.
     *
     * @param dn name of the object changed
     * @param eventType type of modification
     */
    protected static void notifyACIChangeEvent(String dn, int eventType) {
        // NOTE: COS related events do not come here..
        if (debug.messageEnabled()) {
            debug.message("In AMObjectImpl.notifyACIChangeEvent(..): " + dn);
        }

        synchronized (objImplListeners) {
            if (objImplListeners.isEmpty()) {
                return;
            }

            // Create a new AMEvent type object
            AMEvent dpEvent = new AMEvent(new AMEvent(dn), eventType);

            // Based on event type, send notifications to required
            // registered listeners
            switch (eventType) {
            case AMEvent.OBJECT_CHANGED:
            case AMEvent.OBJECT_RENAMED:
                if (debug.messageEnabled()) {
                    debug.message("In AMObjectImpl.notifyACIChangeEvent(..): "
                            + "ACI Entry renamed/changed event");
                }
                notifyAffectedDNs(dn, dpEvent);
                break;

            case AMEvent.OBJECT_REMOVED: // Just an entry with aci removed
                if (debug.messageEnabled()) {
                    debug.message("In AMObjectImpl.notifyACIChangeEvent(..): "
                            + "ACI Entry removed event");
                }
                Set objImplSet = (Set) objImplListeners.get(dn.toLowerCase());
                if (objImplSet == null) {
                    return;
                }

                Iterator itr = objImplSet.iterator();
                while (itr.hasNext()) {
                    AMObjectImpl dpObjImpl = (AMObjectImpl) itr.next();
                    dpObjImpl.sendEvents(dpEvent);
                }
                break;

            default:
                ; // This should not occur. Ignore if they occur
            }
        }
        // End synchronized
    }

    /**
     * This method will be called EntryEventListener to send notifications to
     * all interested AMObjectImp's whenever an Entry Event occurs.
     * <p>
     * 
     * @param dn
     *            the object that is modified
     * @param eventType
     *            type of modification
     * @param cosType -
     *            true if it is a cosrelated event; false otherwise
     */
    protected static void notifyEntryEvent(String dn, int eventType,
            boolean cosType) {
        synchronized (objImplListeners) {
            if (objImplListeners.isEmpty()) {
                return;
            }

            // Create a new AMEvent type object
            AMEvent dpEvent = new AMEvent(new AMEvent(dn), eventType);

            // Based on event type, send notifications to required
            // registered listeners
            switch (eventType) {
            case AMEvent.OBJECT_ADDED:
                if (cosType) { // Need to notify affected DNs if true
                    if (debug.messageEnabled()) {
                        debug.message("AMObjectImpl.notifyEntryEvent(..): "
                                + "change/remove cos event!" + dn);
                    }
                    notifyAffectedDNs(dn, dpEvent);
                }
                break;

            case AMEvent.OBJECT_CHANGED:
            case AMEvent.OBJECT_REMOVED:
                if (cosType) { // Need to notify affected DNs if true
                    if (debug.messageEnabled()) {
                        debug.message("AMObjectImpl.notifyEntryEvent(..): "
                                + "change/remove cos event!" + dn);
                    }
                    notifyAffectedDNs(dn, dpEvent);
                } else { // Just a entry change/remove; subtree not affected
                    if (debug.messageEnabled()) {
                        debug.message("AMObjectImpl.notifyEntryEvent(..): "
                                + "change/remove entry event!" + dn);
                    }
                    Set objImplSet = (Set) objImplListeners.get(
			dn.toLowerCase());
                    if (objImplSet == null) {
                        return;
                    }

                    // clone to provide ConcurrentModificationException
                    Iterator itr = ((HashSet)((HashSet) objImplSet).clone()).iterator(); 
                    while (itr.hasNext()) {
                        AMObjectImpl dpObjImpl = (AMObjectImpl) itr.next();
                        dpObjImpl.sendEvents(dpEvent);
                    }
                }
                break;

            case AMEvent.OBJECT_RENAMED:
                // Notify all affected with this dn, since it is a rename
                if (debug.messageEnabled()) {
                    debug.message("AMObjectImpl.notifyEntryEvent(..): "
                            + "rename entry event!" + dn);
                }
                notifyAffectedDNs(dn, dpEvent);
                break;

            default:
                ; // This should not occur.
            }
        }
        // End synchronized
    }

    protected static void sendExpiryEvent(String sourceDN, int sourceType) {
        if (debug.messageEnabled()) {
            debug.message("AMObjectImpl.sendExpiryEvent(..) - for:" + sourceDN);
        }

        Set objectImplSet = (Set) objImplListeners.get(sourceDN.toLowerCase());
        if (objectImplSet != null) {
            synchronized (objectImplSet) { // Lock, so that no more objects
                // get added/removed here
                Iterator itr = objectImplSet.iterator();
                // Note: This is a hack, we can't create a DSEvent object, so we
                // just pass the sourceDN here.
                AMEvent amEvent = new AMEvent(sourceDN, AMEvent.OBJECT_EXPIRED,
                        sourceDN, sourceType);
                while (itr.hasNext()) {
                    AMObjectImpl amObjectImpl = (AMObjectImpl) itr.next();
                    amObjectImpl.sendEvents(amEvent);
                }
            }
        }
    }

    /**
     * This method removes the entry corresponding to SSOTokenID supplied.
     * 
     * @return Set of DN's for the given SSOTokenID or null if not present
     *         <p>
     * 
     * @param ssoToken -
     *            a SSOToken
     * 
     */
    protected static Set removeFromProfileNameTable(SSOToken ssoToken) {
        Hashtable pTable = profileNameTable;

        if ((pTable == null) || (pTable.isEmpty())) {
            return null;
        }

        if (debug.messageEnabled()) {
            debug.message("In ProfileService."
                    + "removeFromProfilefNameTable(SSOTokenID)..");
        }

        Set dnList = null;

        synchronized (pTable) {
            String principal;

            // Check if the entry exists corresponding to this session
            try {
                principal = ssoToken.getPrincipal().getName();
            } catch (SSOException ssoe) {
                debug.error("AMObjectImpl.removeFromProfileNameTable(): "
                        + "Could not update PFN table");

                return null;
            }

            dnList = (Set) pTable.remove(principal);
        }

        // Note dnList could be null if there was no key with ssoTokenID
        return dnList;
    }

    /**
     * Method that removes all the entries that correspond ("dn",ssoTokenId)
     * supplied. This is done for all the DN's in the set of DN's supplied.
     * 
     * @param dnSet -
     *            a set of DNs
     * @param ssoTokenId -
     *            the SSO token Id
     */
    protected static void removeObjImplListeners(Set dnSet,
            SSOTokenID ssoTokenId) {
        if (debug.messageEnabled()) {
            debug.message("In AMObjectImpl.removeObjImplListeners(..): ");
        }

        synchronized (objImplListeners) {
            Iterator dnItr = dnSet.iterator();

            while (dnItr.hasNext()) { // Iterate through the dn set.

                String dn = (String) dnItr.next();
                Set objSet = (Set) objImplListeners.get(dn.toLowerCase());

                if (objSet == null) {
                    continue;
                }

                Iterator objItr = objSet.iterator();

                while (objItr.hasNext()) {
                    AMObjectImpl dpObjectImpl = (AMObjectImpl) objItr.next();
                    SSOToken dpObjSSOToken = dpObjectImpl.getSSOToken();

                    if (ssoTokenId.equals(dpObjSSOToken.getTokenID())) {
                        objSet.remove(dpObjectImpl);
                    }
                }

                if (objSet.isEmpty()) {
                    objImplListeners.remove(dn);
                }
            }
        }
        // End Synchronized
    }

    /**
     * Substitutes the macros in the set of DN:ACI.
     * 
     * @param aciSet
     *            Set of DN:ACI
     * @param roleDN
     *            Role DN to replace macro ROLENAME
     * @param orgDN
     *            Organization DN to replace macro ORGANIZATION
     * @param groupDN
     *            Group DN to replace macro GROUPNAME
     * @param pcDN
     *            People container DN to replace PCNAME
     */
    protected Set replaceAciListMacros(Set aciSet, String roleDN, String orgDN,
            String groupDN, String pcDN) {
        Set resultSet = new HashSet();

        Iterator iter = aciSet.iterator();

        while (iter.hasNext()) {
            resultSet.add(replaceAciMacros((String) iter.next(), roleDN, orgDN,
                    groupDN, pcDN));
        }

        return resultSet;
    }

    protected String replaceAciMacro(String aci, String macro, String str) {
        if (str == null) {
            return aci;
        }

        StringBuilder sb = new StringBuilder();

        while (true) {
            int index = aci.indexOf(macro);

            if (index == -1) {
                sb.append(aci);

                break;
            }

            sb.append(aci.substring(0, index)).append(str);
            aci = aci.substring(index + macro.length());
        }

        return sb.toString();
    }

    protected String replaceAciMacros(String aci, String roleDN, String orgDN,
            String groupDN, String pcDN) {
        String result;

        result = replaceAciMacro(aci, "ROLENAME", roleDN);
        result = replaceAciMacro(result, "ORGANIZATION", orgDN);
        result = replaceAciMacro(result, "GROUPNAME", groupDN);
        result = replaceAciMacro(result, "PCNAME", pcDN);

        String filter = null;
        String adgFilter = "(memberof=*" + entryDN + ")";
        String sgFilter = "(iplanet-am-static-group-dn=*" + entryDN + ")";

        if (profileType == DYNAMIC_GROUP) {
            Set attr = (Set) stringValueModMap.get("memberurl");

            if ((attr != null) && attr.iterator().hasNext()) {
                String memberurl = (String) attr.iterator().next();

                try {
                    LDAPUrl ldapurl = new LDAPUrl(memberurl);
                    filter = "(|" + adgFilter + sgFilter + ldapurl.getFilter()
                            + ")";
                } catch (java.net.MalformedURLException ex) {
                    if (debug.messageEnabled()) {
                        debug.message("AMObject.create: "
                                + "Invalid member url " + memberurl);
                    }
                }
            }

            if (filter == null) {
                filter = "(|" + adgFilter + sgFilter + ")";
            }
        } else if ((profileType == ASSIGNABLE_DYNAMIC_GROUP)
                || (profileType == GROUP)) {
            filter = "(|" + adgFilter + sgFilter + ")";
        }

        if (filter != null) {
            result = replaceAciMacro(result, "FILTER", filter);
        }

        return result;
    }

    protected AMSearchResults searchObjects(String namingAttr,
            String objectClassFilter, String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(token);

        StringBuilder filterSB = new StringBuilder();
        filterSB.append("(&").append(
                constructFilter(namingAttr, objectClassFilter, wildcard));

        if ((avPairs != null) && !avPairs.isEmpty()) {
            filterSB.append(constructFilter(avPairs));
        }

        filterSB.append(")");

        if (debug.messageEnabled()) {
            debug.message("AMObjectImpl.searchObjects(" + namingAttr + ", "
                    + objectClassFilter + ", " + wildcard + ", Map): DN="
                    + entryDN + ", level " + searchControl.getSearchScope()
                    + "\n" + mapToString(avPairs));
            debug.message("AMObjectImpl.searchObjects(): filter: "
                    + filterSB.toString());
        }

        SearchControl sc = searchControl.getSearchControl();
        String[] returnAttrs = searchControl.getReturnAttributes();

        return dsServices.search(token, entryDN, filterSB.toString(), sc,
                returnAttrs);
    }

    protected AMSearchResults searchObjects(String namingAttr,
            String objectClassFilter, String wildcard,
            AMSearchControl searchControl, String avfilter) throws AMException,
            SSOException {
        SSOTokenManager.getInstance().validateToken(token);

        StringBuilder filterSB = new StringBuilder();

        filterSB.append("(&").append(
                constructFilter(namingAttr, objectClassFilter, wildcard));

        if (avfilter != null) {
            filterSB.append(avfilter);
        }

        filterSB.append(")");

        if (debug.messageEnabled()) {
            debug.message("AMObjectImpl.searchObjects(" + namingAttr + ", "
                    + objectClassFilter + ", " + wildcard + ", " + avfilter
                    + "): DN=" + entryDN + ", level "
                    + searchControl.getSearchScope());
            debug.message("AMObjectImpl.searchObjects(): filter: "
                    + filterSB.toString());
        }

        SearchControl sc = searchControl.getSearchControl();
        String[] returnAttrs = searchControl.getReturnAttributes();

        return dsServices.search(token, entryDN, filterSB.toString(), sc,
                returnAttrs);
    }

    protected AMSearchResults searchObjects(String objectClassFilter,
            AMSearchControl searchControl, String avfilter) throws AMException,
            SSOException {
        SSOTokenManager.getInstance().validateToken(token);

        StringBuilder filterSB = new StringBuilder();
        filterSB.append("(&").append(constructFilter(objectClassFilter));

        if (avfilter != null) {
            filterSB.append(avfilter);
        }

        filterSB.append(")");

        if (debug.messageEnabled()) {
            debug.message("AMObjectImpl.searchObjects(" + objectClassFilter
                    + ", " + avfilter + "): DN=" + entryDN + ", level "
                    + searchControl.getSearchScope());
            debug.message("AMObjectImpl.searchObjects(): filter: "
                    + filterSB.toString());
        }

        SearchControl sc = searchControl.getSearchControl();
        String[] returnAttrs = searchControl.getReturnAttributes();

        return dsServices.search(token, entryDN, filterSB.toString(), sc,
                returnAttrs);
    }

    protected Set searchObjects(int[] objectTypes, String wildcard,
            Map avPairs, int level) throws AMException, SSOException {
        StringBuilder filterSB = new StringBuilder();
        filterSB.append("(&");

        filterSB.append("(|");

        for (int i = 0; i < objectTypes.length; i++) {
            String namingAttr = AMNamingAttrManager
                    .getNamingAttr(objectTypes[i]);
            String objectClassFilter = getSearchFilter(objectTypes[i]);
            filterSB.append(constructFilter(namingAttr, objectClassFilter,
                    wildcard));
        }

        filterSB.append(")");

        if ((avPairs != null) && !avPairs.isEmpty()) {
            filterSB.append(constructFilter(avPairs));
        }

        filterSB.append(")");

        if (debug.messageEnabled()) {
            debug.message("    filter: " + filterSB.toString());
        }

        return search(level, filterSB.toString());
    }

    protected AMSearchResults searchObjects(int[] objectTypes, String wildcard,
            Map avPairs, AMSearchControl searchControl) throws AMException,
            SSOException {
        SSOTokenManager.getInstance().validateToken(token);

        StringBuilder filterSB = new StringBuilder();
        filterSB.append("(&");

        filterSB.append("(|");

        for (int i = 0; i < objectTypes.length; i++) {
            String namingAttr = AMNamingAttrManager
                    .getNamingAttr(objectTypes[i]);
            String objectClassFilter = getSearchFilter(objectTypes[i]);
            filterSB.append(constructFilter(namingAttr, objectClassFilter,
                    wildcard));
        }

        filterSB.append(")");

        if ((avPairs != null) && !avPairs.isEmpty()) {
            filterSB.append(constructFilter(avPairs));
        }

        filterSB.append(")");

        if (debug.messageEnabled()) {
            debug.message("AMObjectImpl.searchObjects(): filter: "
                    + filterSB.toString());
        }

        SearchControl sc = searchControl.getSearchControl();
        String[] returnAttrs = searchControl.getReturnAttributes();

        return dsServices.search(token, entryDN, filterSB.toString(), sc,
                returnAttrs);
    }

    /**
     * Sets aci based on the "iplanet-am-admin-console-group-pclist" and
     * "iplanet-am-admin-console-group-default-pc" attributes
     * 
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    void setAciBasedOnGroupPclist() throws AMException, SSOException {
        String orgDN = getOrganizationDN();
        AMOrganizationImpl org = new AMOrganizationImpl(token, orgDN);

        Set pcSet = new HashSet();
        DN thisDN = new DN(entryDN);

        try {
            AMTemplate template = org.getTemplate(ADMINISTRATION_SERVICE,
                    AMTemplate.ORGANIZATION_TEMPLATE);
            Set groupPclist = template
                    .getAttribute("iplanet-am-admin-console-group-pclist");

            if (debug.messageEnabled()) {
                debug.message("AMObject.setAciBasedOnGroupPclist: "
                        + "iplanet-am-admin-console-group-pclist "
                        + setToString(groupPclist));
            }

            Iterator iter = groupPclist.iterator();

            while (iter.hasNext()) {
                String groupPc = (String) iter.next();

                int index = groupPc.indexOf("|");

                if (index == -1) {
                    continue;
                }

                DN groupDN = new DN(groupPc.substring(0, index));

                if (groupDN.equals(thisDN)) {
                    pcSet.add(groupPc.substring(index + 1));
                }
            }

            if (pcSet.isEmpty()) {
                String defaultPc = template.getStringAttribute(
                        "iplanet-am-admin-console-group-default-pc");

                if (defaultPc != null) {
                    if (defaultPc.length() > 0) {
                        pcSet.add(defaultPc);
                    }
                }
            }
        } catch (AMException ex) {
            if (debug.messageEnabled()) {
                debug.message("AMObject.setAciBasedOnGroupPclist: "
                        + "Unable to get template for "
                        + ADMINISTRATION_SERVICE);
            }
        }

        if (pcSet.isEmpty()) {
            pcSet.add(AMNamingAttrManager.getNamingAttr(PEOPLE_CONTAINER)
                    + "=People," + orgDN);
        }

        String roleDN = AMNamingAttrManager.getNamingAttr(ROLE) + "="
                + thisDN.toString().replace(',', '_') + "," + orgDN;
        AMRoleImpl gRole = new AMRoleImpl(token, roleDN);

        if (!gRole.isExists()) {
            // Role does not exists, do not set the acis
            return;
        }

        Set aciSet = new HashSet(); // org.getAttribute("aci");
        Set aciListSet = new HashSet();
        Iterator iter = pcSet.iterator();

        while (iter.hasNext()) {
            String pc = (String) iter.next();
            String thisAci = "(target=\"ldap:///" + pc + "\")"
                    + "(targetattr=\"nsroledn\")"
                    + "(targattrfilters=\"add=nsroledn:(!(nsroledn=*)),"
                    + "del=nsroledn:(!(nsroledn=*))\")"
                    + "(version 3.0; acl \"Group admin's right to add user "
                    + "to people container\"; allow (add) roledn = \""
                    + "ldap:///" + roleDN + "\";)";
            aciSet.add(thisAci);
            aciListSet.add(orgDN + ":aci:" + thisAci);
        }

        org.setAttribute("aci", aciSet);
        org.store(true);
        gRole.setAttribute("iplanet-am-role-aci-list", aciListSet);
        gRole.store(true);
    }

    /**
     * Gets set of DN:ACI in attribute "iplanet-am-role-aci-list" in the role
     * and sets aci accordingly.
     * 
     * @param role
     *            Role
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    void setAciForRole(AMRole role) throws AMException, SSOException {
        Set aciSet = new TreeSet(role.getAttribute("iplanet-am-role-aci-list"));
        Iterator iter = aciSet.iterator();
        DN targetDN = null;
        Set acis = new HashSet();
        Set newAcis = new HashSet();
        boolean needUpdate = false;
        boolean denied = false;
        AMObjectImpl targetObj = null;

        while (iter.hasNext()) {
            String aci = (String) iter.next();

            int index = aci.indexOf(":aci:");

            if (index != -1) {
                DN tmpDN = new DN(aci.substring(0, index));
                String newAci = aci.substring(index + 5).trim();

                if (targetDN == null) {
                    targetDN = tmpDN;

                    try {
                        targetObj = new AMObjectImpl(token,
                                targetDN.toString(), UNKNOWN_OBJECT_TYPE);
                        acis = targetObj.getAttribute("aci");

                        if (!acis.contains(newAci)) {
                            needUpdate = true;
                            newAcis.add(newAci);
                        }
                    } catch (Exception ex) {
                        if (debug.messageEnabled()) {
                            debug.message("AMObject.setAciForRole :"
                                    + targetDN.toString()
                                    + " read access denied." + ex);
                        }

                        denied = true;
                    }
                } else if (tmpDN.equals(targetDN)) {
                    if (!(denied || acis.contains(newAci))) {
                        needUpdate = true;
                        newAcis.add(newAci);
                    }
                } else {
                    if ((!denied) && needUpdate) {
                        try {
                            targetObj.setAttribute("aci", newAcis);
                            targetObj.store(true);
                        } catch (Exception ex) {
                            if (debug.messageEnabled()) {
                                debug.message("AMObject.setAciForRole :"
                                        + targetDN.toString()
                                        + " write access denied." + ex);
                            }
                        }
                    }

                    needUpdate = false;
                    denied = false;
                    targetDN = tmpDN;

                    try {
                        targetObj = new AMObjectImpl(token,
                                targetDN.toString(), UNKNOWN_OBJECT_TYPE);
                        acis = targetObj.getAttribute("aci");

                        if (!acis.contains(newAci)) {
                            needUpdate = true;
                            newAcis.add(newAci);
                        }
                    } catch (Exception ex) {
                        if (debug.messageEnabled()) {
                            debug.message("AMObject.setAciForRole :"
                                    + targetDN.toString()
                                    + " read access denied." + ex);
                        }

                        denied = true;
                    }
                }
            }
        }

        if (needUpdate) {
            targetObj.setAttribute("aci", newAcis);
            targetObj.store(true);
        }
    }

    /**
     * Gets the aci description and DN:ACI of the role type that matches the
     * permission
     * 
     * @param permission
     *            Permission in the role type
     * @param aciDesc
     *            StringBuffer to store aci description
     * @return Set of DN:ACI
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    Set getDefaultAcis(String permission, StringBuffer aciDesc)
            throws AMException, SSOException {
        Map map;

        try {
            map = AMServiceUtils.getServiceConfig(token,
                    ADMINISTRATION_SERVICE, SchemaType.GLOBAL);
        } catch (SMSException smsex) {
            debug.error(smsex.toString());
            throw new AMException(AMSDKBundle.getString("158", locale), "158");
        }

        Set defaultAcis = (Set) map
                .get("iplanet-am-admin-console-dynamic-aci-list");
        Iterator iter = defaultAcis.iterator();
        String aci = null;

        while (iter.hasNext()) {
            String defaultAci = (String) iter.next();

            if (defaultAci.startsWith(permission + "|")) {
                aci = defaultAci;

                break;
            }
        }

        if (aci == null) {
            throw new AMException(AMSDKBundle.getString("158", locale), "158");
        }

        StringTokenizer stz = new StringTokenizer(aci, "|");

        if (stz.countTokens() < 3) {
            throw new AMException(AMSDKBundle.getString("159", locale), "159");
        }

        permission = stz.nextToken();
        aciDesc.append(stz.nextToken());

        int index = aci.indexOf('|', permission.length() + 1);
        String acis = aci.substring(index + 1);
        stz = new StringTokenizer(acis, "##");

        Set aciSet = new HashSet();

        while (stz.hasMoreTokens()) {
            aciSet.add(stz.nextToken());
        }

        return aciSet;
    }

    // TODO: deprecated remove next release

    /**
     * Set template attributes according to policy DNs.
     * 
     * @param serviceName
     *            serviceName
     * @param policyDNs
     *            Set of policy DN string
     * 
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    void setPolicyTemplate(AMTemplate template, Set policyDNs)
            throws AMException, SSOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets attributes "iplanet-am-role-aci-description" and
     * "iplanet-am-role-aci-list" for role based on the role type that matches
     * the permission
     * 
     * @param role
     *            Role that sets attributes
     * @param permission
     *            Permission in the role type
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    void setRoleAciDescAciList(AMRole role, String permission)
            throws AMException, SSOException {
        StringBuffer aciDescSB = new StringBuffer();
        Set aciSet = getDefaultAcis(permission, aciDescSB);
        Set displayOptions = getDefaultDisplayOptions(permission);

        Map attributeMap = new HashMap();
        Set valueSet = new HashSet();
        valueSet.add(aciDescSB.toString());
        attributeMap.put("iplanet-am-role-aci-description", valueSet);
        attributeMap.put("iplanet-am-role-aci-list", replaceAciListMacros(
                aciSet, role.getDN(), entryDN, null, null));

        if ((displayOptions != null) && !displayOptions.isEmpty()) {
            attributeMap.put(ROLE_DISPLAY_ATTR, displayOptions);
        } else if (debug.messageEnabled()) {
            debug.message("AMObjectImpl.setRoleAciDescAciList-> "
                    + "Display Options for permision = " + permission
                    + "  are not defined");
        }

        role.setAttributes(attributeMap);
        role.store();
    }

    private Set getDefaultDisplayOptions(String permission) throws AMException,
            SSOException {
        Set displayOptions = null;

        try {
            displayOptions = DisplayOptionsUtils.getDefaultDisplayOptions(
                    token, permission);
        } catch (SMSException smse) {
            debug.error("AMObjectImpl.getDefaultDisplayOptions", smse);
            throw new AMException(AMSDKBundle.getString("158", locale), "158");
        }

        return displayOptions;
    }

    void createAdminRole() throws SSOException, AMException {
        if (debug.messageEnabled()) {
            debug.message("AMObject.createAdminRole : dn=" + entryDN);
        }

        DN ldapDN = new DN(entryDN);
        String orgDN = dsServices.getOrganizationDN(token, ldapDN.getParent()
                .toString());

        String permission;
        String roleDN;

        if (profileType == PEOPLE_CONTAINER) {
            permission = "People Container Admin";
            roleDN = AMNamingAttrManager.getNamingAttr(ROLE) + "="
                    + ldapDN.toString().replace(',', '_') + "," + orgDN;
            createAdminRole(permission, orgDN, roleDN);
        } else {
            permission = "Group Admin";
            roleDN = AMNamingAttrManager.getNamingAttr(ROLE) + "="
                    + ldapDN.toString().replace(',', '_') + "," + orgDN;
            createAdminRole(permission, orgDN, roleDN);
        }
    }

    void createAdminRole(String permission, String orgDN, String roleDN)
            throws SSOException, AMException {
        StringBuffer aciDescSB = new StringBuffer();
        Set aciSet = getDefaultAcis(permission, aciDescSB);
        Set displayOptions = getDefaultDisplayOptions(permission);

        Map attributeMap = new HashMap();
        Set valueSet = new HashSet();
        valueSet.add("" + AMRole.GENERAL_ADMIN_ROLE);
        attributeMap.put("iplanet-am-role-type", valueSet);

        valueSet = new HashSet();
        valueSet.add(permission);
        attributeMap.put("iplanet-am-role-description", valueSet);

        valueSet = new HashSet();
        valueSet.add(aciDescSB.toString());
        attributeMap.put("iplanet-am-role-aci-description", valueSet);

        if (profileType == PEOPLE_CONTAINER) {
            attributeMap.put("iplanet-am-role-aci-list", replaceAciListMacros(
                    aciSet, roleDN, orgDN, null, entryDN));
        } else {
            attributeMap.put("iplanet-am-role-aci-list", replaceAciListMacros(
                    aciSet, roleDN, orgDN, entryDN, null));
        }

        if ((displayOptions != null) && !displayOptions.isEmpty()) {
            attributeMap.put(ROLE_DISPLAY_ATTR, displayOptions);
        } else if (debug.messageEnabled()) {
            debug.message("AMObjectImpl.createAdminRole-> "
                    + "Display Options for permision = " + permission
                    + " are not defined");
        }

        valueSet = new HashSet();
        valueSet.add(entryDN);
        attributeMap.put(ROLE_MANAGED_CONTAINER_DN_ATTRIBUTE, valueSet);

        AMRoleImpl dpRole = new AMRoleImpl(token, roleDN);
        dpRole.setAttributes(attributeMap);
        dpRole.create();
    }

    /**
     * Gets the DN:ACI that is not shared by more that 1 role.
     * 
     * @param aciList
     *            Set of DN:ACI
     * @return Set of DN:ACI that are in attribute "iplanet-am-role-aci-list" in
     *         less than 2 roles so that we can remove it
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    Set findRemovableAciList(Set aciList) throws AMException, SSOException {
        Set resultSet = new HashSet();

        if (aciList == null) {
            return resultSet;
        }

        Iterator iter = aciList.iterator();

        while (iter.hasNext()) {
            String aci = (String) iter.next();
            Set objs = dsServices.search(token, 
                AMStoreConnection.getAMSdkBaseDN(),"(&"
                            + AMSearchFilterManager
                                    .getGlobalSearchFilter(AMObject.GROUP)
                            + "(iplanet-am-role-aci-list=" + aci + "))",
                    AMConstants.SCOPE_SUB);

            if (objs.size() < 2) {
                resultSet.add(aci);
            }
        }

        return resultSet;
    }

    /**
     * Removes aci based on the set of DN:ACI.
     * 
     * @param aciList
     *            Set of DN:ACI
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    void removeAci(Set aciList) throws AMException, SSOException {
        if (aciList == null) {
            return;
        }

        if (aciList.isEmpty()) {
            return;
        }

        Set aciSet = new TreeSet(aciList);

        Iterator iter = aciSet.iterator();
        DN targetDN = null;
        Set acis = new HashSet();
        boolean needUpdate = false;
        boolean denied = false;
        AMObjectImpl targetObj = null;

        while (iter.hasNext()) {
            String aci = (String) iter.next();

            int index = aci.indexOf(":aci:");

            if (index != -1) {
                DN tmpDN = new DN(aci.substring(0, index));
                String oldAci = aci.substring(index + 5).trim();

                if (targetDN == null) {
                    targetDN = tmpDN;

                    try {
                        targetObj = new AMObjectImpl(token,
                                targetDN.toString(), UNKNOWN_OBJECT_TYPE);
                        acis = targetObj.getAttribute("aci");

                        if (acis.remove(oldAci)) {
                            needUpdate = true;
                        }
                    } catch (Exception ex) {
                        if (debug.messageEnabled()) {
                            debug.message("AMObject.removeAci :"
                                    + targetDN.toString()
                                    + " read access denied." + ex);
                        }

                        denied = true;
                    }
                } else if (tmpDN.equals(targetDN)) {
                    if (!denied) {
                        if (acis.remove(oldAci)) {
                            needUpdate = true;
                        }
                    }
                } else {
                    if ((!denied) && needUpdate) {
                        try {
                            targetObj.setAttribute("aci", acis);
                            targetObj.store();
                        } catch (Exception ex) {
                            if (debug.messageEnabled()) {
                                debug.message("AMObject.removeAci :"
                                        + targetDN.toString()
                                        + " write access denied." + ex);
                            }
                        }
                    }

                    needUpdate = false;
                    denied = false;
                    targetDN = tmpDN;

                    try {
                        targetObj = new AMObjectImpl(token,
                                targetDN.toString(), UNKNOWN_OBJECT_TYPE);
                        acis = targetObj.getAttribute("aci");

                        if (acis.remove(oldAci)) {
                            needUpdate = true;
                        }
                    } catch (Exception ex) {
                        if (debug.messageEnabled()) {
                            debug.message("AMObject.setAciForRole :"
                                    + targetDN.toString()
                                    + " read access denied." + ex);
                        }

                        denied = true;
                    }
                }
            }
        }

        if (needUpdate) {
            targetObj.setAttribute("aci", acis);
            targetObj.store();
        }
    }

    /**
     * Removes the aci for the admin roles
     * 
     * @param recursive
     *            true if removing admin roles for whole subtree
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    void removeAdminRoleAci(boolean recursive) throws AMException, SSOException 
    {
        String orgDN = getOrganizationDN();
        AMOrganizationImpl org = new AMOrganizationImpl(token, orgDN);

        Set aciSet = org.getAttribute("aci");
        Set newAciSet = new HashSet();
        Iterator iter = aciSet.iterator();
        DN thisDN = new DN(entryDN);

        while (iter.hasNext()) {
            String aci = (String) iter.next();
            int index = aci.indexOf("version 3.0;");

            if (index == -1) {
                newAciSet.add(aci);

                continue;
            }

            index = aci.indexOf("roledn", index);

            if (index == -1) {
                newAciSet.add(aci);

                continue;
            }

            index = aci.indexOf("ldap:///", index);

            if (index == -1) {
                newAciSet.add(aci);

                continue;
            }

            int index2 = aci.lastIndexOf("\"");
            DN roleDN = new DN(aci.substring(index + 8, index2));
            String roleName = ((RDN) roleDN.getRDNs().get(0))
                    .getValues()[0];

            String tmpdn = roleName.replace('_', ',');

            DN tmpDN = new DN(tmpdn);
            if (tmpDN.isDN()) {

                if (!tmpDN.equals(thisDN)) {
                    if (tmpDN.isDescendantOf(thisDN)) {
                        if (!recursive) {
                            newAciSet.add(aci);
                        }
                    } else {
                        newAciSet.add(aci);
                    }
                }
            } else {
                newAciSet.add(aci);
            }
        }

        org.setAttribute("aci", newAciSet);
        org.store();
    }

    // TODO: deprecated remove next release

    /**
     * Unassigns the given policies from this object.
     * 
     * @param serviceName
     *            serviceName
     * @param policyDNs
     *            Set of policy DN string
     * @param toVerify
     *            if true, check if the given policies DN exist
     * 
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    void unassignPolicies(String serviceName, Set policyDNs, boolean toVerify)
            throws AMException, SSOException {
        if ((policyDNs == null) || (policyDNs.isEmpty())) {
            return;
        }

        AMTemplate template = getTemplate(serviceName,
                AMTemplate.POLICY_TEMPLATE);
        unassignPolicies(template, policyDNs, toVerify);
    }

    // TODO: deprecated remove next release

    /**
     * Unassigns the given policies from this object.
     * 
     * @param template
     *            policy template
     * @param policyDNs
     *            Set of policy DN string
     * @param toVerify
     *            if true, check if the given policies DN exist
     * 
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    void unassignPolicies(AMTemplate template, Set policyDNs, boolean toVerify)
            throws AMException, SSOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Method that replace the map key with key found set
     */
    private Map replaceMapKey(Map attributes, Set attributeNames) {
        if (attributeNames != null) {
            Iterator iter = attributeNames.iterator();
            while (iter.hasNext()) {
                String attrName = (String) iter.next();
                String attrNameLower = attrName.toLowerCase();
                if (attributes.containsKey(attrNameLower)) {
                    Object attrValue = attributes.get(attrNameLower);
                    attributes.remove(attrNameLower);
                    attributes.put(attrName, attrValue);
                }
            }
        }
        return attributes;
    }

    /**
     * Method that returns the SSOToken for this AMObjectImpl
     * 
     * @return SSOToken of this AMObjectImpl
     */
    private SSOToken getSSOToken() {
        return token;
    }

    private String setToString(Set set) {
        if (set == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        Iterator iter = set.iterator();

        while (iter.hasNext()) {
            sb.append("    ").append((String) iter.next()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Adds a "dn"(value) entry to the ProfileName table corresponding to the
     * SSOTokenID (key). If no entry exists for the given SSOTokenID the creates
     * a new ent and adds a new SSOTokenListener for the SSOTokenID
     * 
     * @param ssoToken -
     *            a SSOToken
     * @param dn -
     *            a dn String
     */
    private static void addToProfileNameTable(SSOToken ssoToken, String dn)
            throws SSOException {
        if (debug.messageEnabled()) {
            debug.message("In AMObjectImpl."
                    + "addToProfileNameTable(SSOToken,dn)..");
        }

        Hashtable pTable = profileNameTable;

        synchronized (pTable) {
            // Check if the entry exists corresponding to this session
            Set dnList = (Set) pTable.get(ssoToken.getPrincipal().getName());

            if (dnList == null) {
                // No entry corressponding to session
                // Add a new SSOTokenListener
                AMSSOTokenListener ssoTokenListener = new AMSSOTokenListener(
                        ssoToken.getPrincipal().getName());
                ssoToken.addSSOTokenListener(ssoTokenListener);
                dnList = new HashSet();
                pTable.put(ssoToken.getPrincipal().getName(), dnList);
            }

            dnList.add(dn);
        }
    }

    private String mapToString(Map map) {
        if (map == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        Iterator iter = map.keySet().iterator();

        while (iter.hasNext()) {
            String attributeName = (String) (iter.next());

            if (attributeName.equalsIgnoreCase(USER_PASSWORD_ATTRIBUTE)
                    || attributeName.equalsIgnoreCase(
                            USER_ENCRYPTED_PASSWORD_ATTRIBUTE)) {
                continue;
            }

            Set attributeValue = (Set) (map.get(attributeName));
            Iterator iter2 = attributeValue.iterator();

            while (iter2.hasNext()) {
                sb.append("    ").append(attributeName).append(": ").append(
                        (String) iter2.next()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Sends event notifications to all the listeners that correspond to a DN
     * whose suffix which ends with affectedDN in the objImpListeners.
     * <p>
     * 
     * @param affectedDN -
     *            String which has been stripped to reflect the subtree of DN's
     *            that will be affected in case of cos related changes
     * @param dpEvent -
     *            a AMEvent
     * 
     */
    protected static void notifyAffectedDNs(String affectedDN, AMEvent dpEvent) 
    {
        if (debug.messageEnabled()) {
            debug.message("In AMObjectImpl.notifyAffectedDNs(..): ");
        }

        synchronized (objImplListeners) { // To double check (synchronized)

            Iterator mapItr = objImplListeners.entrySet().iterator();

            while (mapItr.hasNext()) {
                Map.Entry me = (Map.Entry) mapItr.next();

                if (((String) (me.getKey())).endsWith(affectedDN)) {
                    Set objImplSet = (Set) me.getValue();
                    Iterator setItr = objImplSet.iterator();

                    while (setItr.hasNext()) {
                        AMObjectImpl dpObjImpl = (AMObjectImpl) setItr.next();
                        dpObjImpl.sendEvents(dpEvent);
                    }
                }
            }
        }
    }

    /**
     * Removes the entry corresponding to given SSO token and distinguished name
     * from the profile name table.
     * 
     * @param ssoToken
     *            Single-Sign-On Token.
     * @param dn
     *            distinguished name.
     */
    private static void removeFromProfileNameTable(SSOToken ssoToken, String dn)
    {
        // Remove the dn from the profileNameTable corresponding to
        // this session.
        if (debug.messageEnabled()) {
            debug.message("In ProfileService."
                    + "removeFromProfileNameTable(SSOToken,dn)..");
        }

        Hashtable pTable = profileNameTable;

        if ((pTable == null) || pTable.isEmpty()) {
            return; // Silent return;
        }

        synchronized (pTable) {
            String principal;

            // Check if the entry exists corresponding to this session
            try {
                principal = ssoToken.getPrincipal().getName();
            } catch (SSOException ssoe) {
                debug.error("AMObjectImpl.removeFromProfileNameTable(): "
                        + "Could not update PFN table");

                return;
            }

            Set dnList = (Set) pTable.get(principal);

            if (dnList != null) {
                dnList.remove(dn);

                if (dnList.isEmpty()) {
                    // Do we need to remove the PSSOTokenListner added here?
                    // How?
                    pTable.remove(principal);
                }
            }
        }
    }

    /**
     * This method sends the event to all the event listeners.
     * 
     * @param dpEvent -
     *            a AMEvent generated
     */
    private void sendEvents(AMEvent dpEvent) {
        synchronized (listeners) {
            Iterator iterator = listeners.iterator();

            while (iterator.hasNext()) {
                AMEventListener listener = (AMEventListener) iterator.next();

                try {
                    switch (dpEvent.getEventType()) {
                    case AMEvent.OBJECT_CHANGED:
                    case AMEvent.OBJECT_EXPIRED:
                        listener.objectChanged(dpEvent);
                        break;

                    case AMEvent.OBJECT_REMOVED:
                        listener.objectRemoved(dpEvent);
                        break;

                    case AMEvent.OBJECT_RENAMED:
                        listener.objectRenamed(dpEvent);
                        break;

                    default:
                    // print some error message and continue
                    }
                } catch (Throwable t) {
                    // even if one listener misbehaves this code should
                    // not crash; just ignore the bad listener
                }
            }
        }
    }

    private AMHashMap integrateLocale() throws AMException, SSOException {
        // If USER or DYNAMIC template, then check to see if
        // preferredLocale is being set (and accordingly set
        // preferredLanguage too. Fix for comms backward compatibility!
        if (AMCommonUtils.integrateLocale
                && (profileType == USER || profileType == 
                    AMTemplate.DYNAMIC_TEMPLATE)
                && stringValueModMap.containsKey("preferredLocale")) {
            Set prefLoc = (Set) stringValueModMap.get("preferredLocale");
            // Set prefLang = new HashSet();
            Set prefLang = (Set) stringValueModMap.get("preferredLanguage");
            if (prefLang == null)
                prefLang = new HashSet();
            Iterator it = prefLoc.iterator();
            while (it.hasNext()) {
                String prefLocVal = (String) it.next();
                String prefLangVal = prefLocVal.replace('_', '-');
                prefLang.add(prefLangVal);
            }
            if (prefLang.isEmpty()) {
                // Make sure this attribute exists in the entry, before trying
                // to
                // delete it.
                Set pL = getAttribute("preferredLanguage");
                if (pL != null && !pL.isEmpty()) {
                    stringValueModMap.put("preferredLanguage", prefLang);
                }
            } else {
                stringValueModMap.put("preferredLanguage", prefLang);
            }
        }
        // vice-versa of above Fix for comms backward compatibility
        if (AMCommonUtils.integrateLocale
                && (profileType == USER || profileType == 
                    AMTemplate.DYNAMIC_TEMPLATE)
                && stringValueModMap.containsKey("preferredLanguage")) {
            Set prefLang = (Set) stringValueModMap.get("preferredLanguage");
            // Set prefLoc = new HashSet();
            Set prefLoc = (Set) stringValueModMap.get("preferredLocale");
            if (prefLoc == null)
                prefLoc = new HashSet();
            Iterator it = prefLang.iterator();
            while (it.hasNext()) {
                String prefLangVal = (String) it.next();
                String prefLocVal = prefLangVal.replace('-', '_');
                prefLoc.add(prefLocVal);
            }
            if (prefLoc.isEmpty()) {
                // Check to see if preferredLocale exists
                // before trying to delete it.
                Set pL = getAttribute("preferredLocale");
                if (pL != null && !pL.isEmpty()) {
                    stringValueModMap.put("preferredLocale", prefLoc);
                }
            } else {
                stringValueModMap.put("preferredLocale", prefLoc);
            }
        }
        // }
        return stringValueModMap;
    }

    private Map integrateLocaleForTemplateCreation(Map attributes) {
        if (AMCommonUtils.integrateLocale) {
            AMHashMap sdkMap = new AMHashMap();
            sdkMap.copy(attributes);
            if (sdkMap.containsKey("preferredLocale")) {
                Set pLoc = (Set) sdkMap.get("preferredLocale");
                Set pLang = new HashSet();
                Iterator it = pLoc.iterator();
                while (it.hasNext()) {
                    String prefLangVal = (String) it.next();
                    String prefLocVal = prefLangVal.replace('_', '-');
                    pLang.add(prefLocVal);
                }
                sdkMap.put("preferredLanguage", pLang);
            } else if (sdkMap.containsKey("preferredLanguage")) {
                Set pLang = (Set) sdkMap.get("preferredLanguage");
                Set pLoc = new HashSet();
                Iterator it = pLang.iterator();
                while (it.hasNext()) {
                    String prefLangVal = (String) it.next();
                    String prefLocVal = prefLangVal.replace('-', '_');
                    pLoc.add(prefLocVal);
                }
                sdkMap.put("preferredLocale", pLoc);
            }
            attributes.clear();
            attributes.putAll(sdkMap);

        }
        return attributes;
    }
}
