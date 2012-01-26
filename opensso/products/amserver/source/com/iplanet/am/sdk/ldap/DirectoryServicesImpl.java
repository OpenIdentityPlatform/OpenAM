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
 * $Id: DirectoryServicesImpl.java,v 1.14 2009/11/20 23:52:51 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk.ldap;

import java.security.AccessController;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPUrl;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.RDN;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMEntryExistsException;
import com.iplanet.am.sdk.AMEventManagerException;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMHashMap;
import com.iplanet.am.sdk.AMInvalidDNException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMObjectListener;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMPreCallBackException;
import com.iplanet.am.sdk.AMRole;
import com.iplanet.am.sdk.AMSDKBundle;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.am.sdk.AMServiceUtils;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMTemplate;
import com.iplanet.am.sdk.AMUser;
import com.iplanet.am.sdk.AMUserEntryProcessed;
import com.iplanet.am.sdk.common.IComplianceServices;
import com.iplanet.am.sdk.common.IDCTreeServices;
import com.iplanet.am.sdk.common.IDirectoryServices;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.ldap.ModSet;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.AccessRightsException;
import com.iplanet.ums.AssignableDynamicGroup;
import com.iplanet.ums.CreationTemplate;
import com.iplanet.ums.DynamicGroup;
import com.iplanet.ums.EntryAlreadyExistsException;
import com.iplanet.ums.EntryNotFoundException;
import com.iplanet.ums.FilteredRole;
import com.iplanet.ums.Guid;
import com.iplanet.ums.InvalidSearchFilterException;
import com.iplanet.ums.ManagedRole;
import com.iplanet.ums.OrganizationalUnit;
import com.iplanet.ums.PeopleContainer;
import com.iplanet.ums.PersistentObject;
import com.iplanet.ums.SchemaManager;
import com.iplanet.ums.SearchControl;
import com.iplanet.ums.SearchResults;
import com.iplanet.ums.SizeLimitExceededException;
import com.iplanet.ums.SortKey;
import com.iplanet.ums.StaticGroup;
import com.iplanet.ums.TemplateManager;
import com.iplanet.ums.TimeLimitExceededException;
import com.iplanet.ums.UMSException;
import com.iplanet.ums.UMSObject;
import com.iplanet.ums.cos.COSManager;
import com.iplanet.ums.cos.COSNotFoundException;
import com.iplanet.ums.cos.COSTemplate;
import com.iplanet.ums.cos.DirectCOSDefinition;
import com.iplanet.ums.cos.ICOSDefinition;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * A class which manages all the major Directory related operations. Contains
 * functionality to create, delete and manange directory entries.
 * 
 * This class should not be used directly when caching mode is on.
 * 
 */
public class DirectoryServicesImpl implements AMConstants, IDirectoryServices {

    private static final String LDAP_CONNECTION_ERROR_CODES = 
        "com.iplanet.am.ldap.connection.ldap.error.codes.retries";

    private static HashSet retryErrorCodes = new HashSet();

    // String constants
    protected static final String EXTERNAL_ATTRIBUTES_FETCH_ENABLED_ATTR = 
        "iplanet-am-admin-console-external-attribute-fetch-enabled";

    protected static String NSROLEDN_ATTR = "nsroledn";

    protected static String NSROLE_ATTR = "nsrole";

    public static Debug debug = CommonUtils.debug;

    public static boolean isUserPluginInitialized = false; // first time flag

    private static AMUserEntryProcessed userEntry = null;

    private String[] aName = { "objectclass" };

    private SearchControl scontrol = new SearchControl();

    // A handle to Singleton instance
    private static IDirectoryServices instance;

    private static EventManager eventManager;

    private static Map listeners = new HashMap();

    protected DCTreeServicesImpl dcTreeImpl;

    protected ComplianceServicesImpl complianceImpl;

    protected CallBackHelper callBackHelper;

    protected SSOToken internalToken;

    static {
        String retryErrs = SystemProperties.get(LDAP_CONNECTION_ERROR_CODES);
        if (retryErrs != null) {
            StringTokenizer stz = new StringTokenizer(retryErrs, ",");
            while (stz.hasMoreTokens()) {
                retryErrorCodes.add(stz.nextToken().trim());
            }
        }
    }

    /**
     * Ideally this constructor should be private, since we are extending this
     * class, it needs to be public. This constructor should not be used to
     * create an instance of this class.
     * 
     * <p>
     * Use <code>AMDirectoryWrapper.getInstance()</code> to create an
     * instance.
     */
    public DirectoryServicesImpl() {
        internalToken = CommonUtils.getInternalToken();
        scontrol.setSearchScope(SearchControl.SCOPE_BASE);
        dcTreeImpl = new DCTreeServicesImpl();
        complianceImpl = new ComplianceServicesImpl();
        callBackHelper = new CallBackHelper();
    }

    protected static synchronized IDirectoryServices getInstance() {
        if (instance == null) {
            debug.message("DirectoryServicesImpl.getInstance(): Creating a "
                    + "new Instance of DirectoryServicesImpl()");
            instance = new DirectoryServicesImpl();
        }
        return instance;
    }

    // *************************************************************************
    // Some local utility methods related to the operations performed. Generic
    // UMSException & LDAPException Processing:
    // TODO: Refactor these to some other class
    // *************************************************************************
    protected String getEntryName(UMSException e) {
        DN dn = getExceptionDN(e);
        String entryName = "";
        if (dn != null) {
            entryName = ((RDN) dn.getRDNs().get(0)).getValues()[0];
        }
        return entryName;
    }

    private DN getExceptionDN(UMSException e) {
        DN dn = null;
        String msg = e.getMessage();
        if (msg != null) {
            // This is hack??
            int index = msg.indexOf("::");
            if (index != -1) {
                String errorDN = msg.substring(0, index);
                dn = new DN(errorDN);
                if (!dn.isDN()) {
                    dn = null;
                }
            }
        }
        return dn;
    }

    private String getEntryNotFoundMsgID(int objectType) {
        switch (objectType) {
        case AMObject.ROLE:
        case AMObject.MANAGED_ROLE:
        case AMObject.FILTERED_ROLE:
            return "465";
        case AMObject.GROUP:
        case AMObject.DYNAMIC_GROUP:
        case AMObject.STATIC_GROUP:
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            return "466";
        case AMObject.ORGANIZATION:
            return "467";
        case AMObject.USER:
            return "468";
        case AMObject.ORGANIZATIONAL_UNIT:
            return "469";
        case AMObject.PEOPLE_CONTAINER:
            return "470";
        case AMObject.GROUP_CONTAINER:
            return "471";
        default:
            return "461";
        }
    }

    private String getEntryExistsMsgID(int objectType) {
        switch (objectType) {
        case AMObject.ROLE:
        case AMObject.MANAGED_ROLE:
        case AMObject.FILTERED_ROLE:
            return "472";
        case AMObject.GROUP:
        case AMObject.DYNAMIC_GROUP:
        case AMObject.STATIC_GROUP:
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            return "473";
        case AMObject.ORGANIZATION:
            return "474";
        case AMObject.USER:
            return "475";
        case AMObject.ORGANIZATIONAL_UNIT:
            return "476";
        case AMObject.PEOPLE_CONTAINER:
            return "477";
        case AMObject.GROUP_CONTAINER:
            return "483";
        default:
            return "462";
        }
    }

    /**
     * Method which does some generic processing of the UMSException and throws
     * an appropriate AMException
     * 
     * @param SSOToken
     *            the SSOToken of the user performing the operation
     * @param ue
     *            the UMSException thrown
     * @param defaultErrorCode -
     *            the default error code of the localized message to be used if
     *            a generic error occurs
     * @throws AMException
     *             a suitable AMException with specific message indicating the
     *             error.
     */
    private void processInternalException(SSOToken token, UMSException ue,
            String defaultErrorCode) throws AMException {
        try {
            LDAPException lex = (LDAPException) ue.getRootCause();
            if (lex != null) {
                int errorCode = lex.getLDAPResultCode();
                // Check for specific error conditions
                switch (errorCode) {
                case LDAPException.CONSTRAINT_VIOLATION: // LDAP Constraint
                    // Violated
                    throw new AMException(ue.getMessage(), "19", ue);
                case LDAPException.TIME_LIMIT_EXCEEDED:
                    throw new AMException(token, "3", ue);
                case LDAPException.SIZE_LIMIT_EXCEEDED:
                    throw new AMException(token, "4", ue);
                case LDAPException.NOT_ALLOWED_ON_RDN:
                    throw new AMException(token, "967", ue);
                case LDAPException.ADMIN_LIMIT_EXCEEDED:
                    throw new AMException(token, "968", ue);
                default:
                    throw new AMException(token, defaultErrorCode, ue);
                }
            } else {
                throw new AMException(token, defaultErrorCode, ue);
            }
        } catch (Throwable ex) { // Cannot obtain the specific error
            if (ex instanceof AMException) {
                throw ((AMException) ex);
            } else {
                if (debug.messageEnabled()) {
                    debug.message("Unknown exception in process "
                            + "internal exception", ex);
                }
                throw new AMException(token, defaultErrorCode);
            }
        }
    }

    /**
     * Method to check if the CallBack plugins are enabled for reading external
     * attributes.
     */
    protected static boolean isExternalGetAttributesEnabled(String orgDN) {
        // Obtain the ServiceConfig
        Set attrVal;
        SSOToken token = (SSOToken) AccessController
                .doPrivileged(AdminTokenAction.getInstance());

        try {
            // Get the org config
            ServiceConfig sc = AMServiceUtils.getOrgConfig(token, orgDN,
                    ADMINISTRATION_SERVICE);
            if (sc != null) {
                Map attributes = sc.getAttributes();
                attrVal = (Set) attributes
                        .get(EXTERNAL_ATTRIBUTES_FETCH_ENABLED_ATTR);
            } else {
                attrVal = getDefaultGlobalConfig(token,
                        EXTERNAL_ATTRIBUTES_FETCH_ENABLED_ATTR);
            }
        } catch (Exception ee) {
            attrVal = getDefaultGlobalConfig(token,
                    EXTERNAL_ATTRIBUTES_FETCH_ENABLED_ATTR);
        }
        boolean enabled = false;
        if (attrVal != null && !attrVal.isEmpty()) {
            String val = (String) attrVal.iterator().next();
            enabled = (val.equalsIgnoreCase("true"));
        }
        if (debug.messageEnabled()) {
            debug.message("DirectoryServicesImpl."
                    + "isExternalGetAttributeEnabled() = " + enabled);
        }

        return enabled;
    }

    private static Set getDefaultGlobalConfig(SSOToken token, String attrName) {
        // Org Config may not exist. Get default values
        if (debug.messageEnabled()) {
            debug.message("AMCommonUtils.getDefaultGlobalConfig() "
                    + "Organization config for service ("
                    + ADMINISTRATION_SERVICE + "," + attrName
                    + ") not found. Obtaining default service "
                    + "config values ..");
        }
        try {
            Map defaultValues = AMServiceUtils.getServiceConfig(token,
                    ADMINISTRATION_SERVICE, SchemaType.ORGANIZATION);
            if (defaultValues != null) {
                return (Set) defaultValues.get(attrName);
            }
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning("AMCommonUtils.getDefaultGlobalConfig(): "
                        + "Unable to get default global config information", e);
            }
        }
        return null;
    }

    // *************************************************************************
    // Some other Private methods
    // *************************************************************************
    /**
     * Gets the user post plugin instance. Returns a null if plugin not
     * configured could not be loaded. TODO: REMOVE after few releases.
     * Supported through AMCallBack
     */
    public static AMUserEntryProcessed getUserPostPlugin() {
        if (!isUserPluginInitialized) {
            // TODO: REMOVE after Portal moves to new API's
            String implClassName = SystemProperties
                    .get(USER_ENTRY_PROCESSING_IMPL);
            if ((implClassName != null) && (implClassName.length() != 0)) {
                try {
                    userEntry = (AMUserEntryProcessed) Class.forName(
                            implClassName).newInstance();
                    if (debug.messageEnabled()) {
                        debug.message("DirectoryServicesImpl." 
                                + "getUserPostPlugin: Class " + implClassName
                                + " instantiated.");
                    }
                } catch (ClassNotFoundException c) {
                    debug.error("DirectoryServicesImpl.getUserPostPlugin(): "
                            + "Class not found: " + implClassName, c);
                } catch (InstantiationException ie) {
                    debug.error("DirectoryServicesImpl.getUserPostPlugin(): "
                            + "Unable to instantiate: " + implClassName, ie);
                } catch (IllegalAccessException le) {
                    debug.error("DirectoryServicesImpl.getUserPostPlugin(): "
                            + "IllegalAccessException: " + implClassName, le);
                }
            }
            isUserPluginInitialized = true;
        }
        return userEntry;
    }

    public IDCTreeServices getDCTreeServicesImpl() {
        return dcTreeImpl;
    }

    public IComplianceServices getComplianceServicesImpl() {
        return complianceImpl;
    }

    // *************************************************************************
    // All public methods related to DS Operations.
    // *************************************************************************
    /**
     * Checks if the entry exists in the directory.
     * 
     * @param token
     *            a valid SSOToken
     * @param entryDN
     *            The DN of the entry that needs to be checked
     * @return true if the entryDN exists in the directory, false otherwise
     */
    public boolean doesEntryExists(SSOToken token, String entryDN) {
        try {
            PersistentObject po = UMSObject.getObject(internalToken, new Guid(
                    entryDN));
        } catch (UMSException ue) {
            /*
             * The very first time when 'Agents' gets selected from the
             * Navigation menu of IS console, there will be no
             * ou=agents,ROOT_SUFFIX in the directory. Only it gets created when
             * a new agent gets created. So do not log this message.
             */

            if (entryDN.indexOf("agents") < 0) {
                if (debug.messageEnabled()) {
                    debug.message(
                            "DirectoryServicesImpl.doesProfileExist(): + "
                                    + "Exception caught: ", ue);
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Gets the type of the object given its DN.
     * 
     * @param token
     *            token a valid SSOToken
     * @param dn
     *            DN of the object whose type is to be known.
     * 
     * @throws AMException
     *             if the data store is unavailable or if the object type is
     *             unknown
     * @throws SSOException
     *             if ssoToken is invalid or expired.
     */
    public int getObjectType(SSOToken token, String dn) throws AMException,
            SSOException {
        return (getObjectType(token, dn, null));
    }

    /**
     * Gets the type of the object given its DN.
     * 
     * @param token
     *            token a valid SSOToken
     * @param dn
     *            DN of the object whose type is to be known.
     * @param cachedAttributes
     *            cached attributes of the user
     * 
     * @throws AMException
     *             if the data store is unavailable or if the object type is
     *             unknown
     * @throws SSOException
     *             if ssoToken is invalid or expired.
     */
    public int getObjectType(SSOToken token, String dn, Map cachedAttributes)
            throws AMException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("DirectoryServicesImpl.getObjectType() Getting "
                    + "object type for: " + dn);
        }

        if (!DN.isDN(dn)) {
            throw new AMInvalidDNException(AMSDKBundle.getString("157"), "157");
        }

        SSOTokenManager.getInstance().validateToken(token);
        Set objectClasses = null;

        // Check if object classes are cached, if not get from directory
        if (cachedAttributes == null
                || (objectClasses = (Set) 
                        cachedAttributes.get("objectclass")) == null) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryServicesImpl.getObjectType() Making "
                        + " LDAP call to get objectclass attributes for DN: "
                        + dn);
            }

            Set attrNames = new HashSet(1);
            attrNames.add("objectclass");

            Map attributes = getAttributes(token, dn, attrNames,
                    AMObject.UNDETERMINED_OBJECT_TYPE);
            if (attributes.size() == 1) {
                objectClasses = (Set) attributes.get("objectclass");
            }
        }
        // Determine the object type
        if (objectClasses != null) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryServicesImpl.getObjectType()- DN: "
                        + dn + " cachedAttributes: " + objectClasses);
            }
            Iterator itr = objectClasses.iterator();
            int possibleOT = -1;
            while (itr.hasNext()) {
                String tStr = (String) itr.next();
                int objectType = getObjectType(tStr);
                if (objectType == AMObject.ROLE) {
                    possibleOT = objectType;
                    continue;
                } else if (objectType != AMObject.UNKNOWN_OBJECT_TYPE) {
                    if (debug.messageEnabled()) {
                        debug.message("DirectoryServicesImpl.getObjectType("
                                + "token, entryDN, cachedAttributes)- DN: "
                                + dn + " objectType: " + objectType);
                    }
                    return objectType;
                }
            }
            if (possibleOT != -1) {
                if (debug.messageEnabled()) {
                    debug.message("DirectoryServicesImpl.getObjectType("
                            + "token, entryDN, cachedAttributes)- DN: " + dn
                            + " objectType: " + possibleOT);
                }
                return possibleOT;
            }
            throw new AMException(AMSDKBundle.getString("156"), "156");
        }
        throw new AMException(AMSDKBundle.getString("151"), "151");
    }

    /**
     * Gets the attributes for this entryDN from the corresponding DC Tree node.
     * The attributes are fetched only for Organization entries in DC tree mode.
     * 
     * @param token
     *            a valid SSOToken
     * @param entryDN
     *            the dn of the entry
     * @param attrNames
     *            attribute names
     * @param byteValues
     *            <code>true</code> if result in byte
     * @param objectType
     *            the object type.
     * @return an AttrSet of values or null if not found
     * @throws AMException
     *             if error encountered in fetching the DC node attributes.
     */
    public Map getDCTreeAttributes(SSOToken token, String entryDN,
            Set attrNames, boolean byteValues, int objectType)
            throws AMException, SSOException {
        // Already an RFC String
        String rootDN = AMStoreConnection.getAMSdkBaseDN();
        if (dcTreeImpl.isRequired() && (objectType == AMObject.ORGANIZATION)
                && (!CommonUtils.formatToRFC(entryDN).equalsIgnoreCase(rootDN)))
        {
            String dcNode = dcTreeImpl.getCanonicalDomain(internalToken,
                    entryDN);
            if (dcNode != null) {
                String names[] = (attrNames == null ? null
                        : (String[]) attrNames.toArray(new String[attrNames
                                .size()]));
                AttrSet dcAttrSet = dcTreeImpl.getDomainAttributes(
                        internalToken, entryDN, names);
                return CommonUtils.attrSetToMap(dcAttrSet, byteValues);
            }
        }
        return null;
    }

    /**
     * Checks for Compliance related attributes if applicable. The check can be
     * over-ridden by setting the ignoreCompliance to true
     * 
     * @param attrSet
     *            the attrSet to verify
     * @param ignoreCompliance
     *            if true the check will not take place in Compliance mode.
     * @throws AMException
     */
    private void checkComplianceAttributes(AttrSet attrSet,
            boolean ignoreCompliance) throws AMException {
        if (!ignoreCompliance
                && ComplianceServicesImpl.isComplianceUserDeletionEnabled()) { 
            // Verify for deleted user
            complianceImpl.verifyAttributes(attrSet);
        }
    }

    public Map getAttributes(SSOToken token, String entryDN, int profileType)
            throws AMException, SSOException {
        boolean ignoreCompliance = true;
        boolean byteValues = false;
        return getAttributes(token, entryDN, ignoreCompliance, byteValues,
                profileType);
    }

    public Map getAttributes(SSOToken token, String entryDN, Set attrNames,
            int profileType) throws AMException, SSOException {
        boolean ignoreCompliance = true;
        boolean byteValues = false;
        return getAttributes(token, entryDN, attrNames, ignoreCompliance,
                byteValues, profileType);
    }

    // Note: This API will not be implemented in Cached impl of this interface
    public Map getAttributesFromDS(SSOToken token, String entryDN,
            Set attrNames, int profileType) throws AMException, SSOException {
        boolean ignoreCompliance = true;
        boolean byteValues = false;
        return getAttributesFromDS(token, entryDN, attrNames, ignoreCompliance,
                byteValues, profileType);
    }

    public Map getAttributesByteValues(SSOToken token, String entryDN,
            int profileType) throws AMException, SSOException {
        // fetch byte values
        boolean byteValues = true;
        boolean ignoreCompliance = true;
        return getAttributes(token, entryDN, ignoreCompliance, byteValues,
                profileType);
    }

    public Map getAttributesByteValues(SSOToken token, String entryDN,
            Set attrNames, int profileType) throws AMException, SSOException {
        // fetch byte values
        boolean byteValues = true;
        boolean ignoreCompliance = true;
        return getAttributes(token, entryDN, attrNames, ignoreCompliance,
                byteValues, profileType);
    }

    /**
     * Gets all attributes corresponding to the entryDN. This method obtains the
     * DC Tree node attributes and also performs compliance related verification
     * checks in compliance mode. Note: In compliance mode you can skip the
     * compliance checks by setting ignoreCompliance to "false".
     * 
     * @param token
     *            a valid SSOToken
     * @param entryDN
     *            the DN of the entry whose attributes need to retrieved
     * @param ignoreCompliance
     *            a boolean value specificying if compliance related entries
     *            need to ignored or not. Ignored if true.
     * @return a Map containing attribute names as keys and Set of values
     *         corresponding to each key.
     * @throws AMException
     *             if an error is encountered in fetching the attributes
     */
    public Map getAttributes(SSOToken token, String entryDN,
            boolean ignoreCompliance, boolean byteValues, int profileType)
            throws AMException, SSOException {
        try {
            // Obtain attributes from directory
            PersistentObject po = UMSObject.getObjectHandle(token, new Guid(
                    entryDN));

            AttrSet attrSet = po.getAttributes(po.getAttributeNames());

            /*
             * Add this 'dn' explicitly to the result set and return. reason:
             * when queried with this entrydn/dn the lower level api/ ldapjdk
             * does not return this attribute, but returns other ones.
             */

            attrSet.add(new Attr("dn", entryDN));
            attrSet.add(new Attr("entryDN", entryDN));

            // Perform Compliance related checks
            checkComplianceAttributes(attrSet, ignoreCompliance);

            AMHashMap attributes = (AMHashMap) CommonUtils.attrSetToMap(
                    attrSet, byteValues);
            Map dcAttributes = getDCTreeAttributes(token, entryDN, null,
                    byteValues, profileType);
            attributes.copy(dcAttributes);
            return attributes;
        } catch (IllegalArgumentException ie) {
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.getAttributes(): "
                        + "Unable to get attributes: ", ie);
            }
            String locale = CommonUtils.getUserLocale(token);
            throw new AMException(AMSDKBundle.getString("330", locale), "330");
        } catch (UMSException e) {
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.getAttributes(): "
                        + "Unable to get attributes: ", e);
            }
            // Extract the ldap error code from Exception
            throw new AMException(token, "330", e);
        }
    }
    
    public Map getAttributes(SSOToken token, String entryDN, Set attrNames,
            boolean ignoreCompliance, boolean byteValues, int profileType)
            throws AMException, SSOException {
        
        return getAttributesFromDS(token, entryDN, attrNames, ignoreCompliance, 
                byteValues, profileType);   
    }

    /**
     * Gets the specific attributes corresponding to the entryDN. This method
     * obtains the DC Tree node attributes and also performs compliance related
     * verification checks in compliance mode. Note: In compliance mode you can
     * skip the compliance checks by setting ignoreCompliance to "false".
     * 
     * @param token
     *            a valid SSOToken
     * @param entryDN
     *            the DN of the entry whose attributes need to retrieved
     * @param attrNames
     *            a Set of names of the attributes that need to be retrieved.
     *            The attrNames should not be null.
     * @param ignoreCompliance
     *            a boolean value specificying if compliance related entries
     *            need to ignored or not. Ignored if true.
     * @return a Map containing attribute names as keys and Set of values
     *         corresponding to each key.
     * @throws AMException
     *             if an error is encountered in fetching the attributes
     */
    public Map getAttributesFromDS(SSOToken token, String entryDN, 
            Set attrNames, boolean ignoreCompliance, boolean byteValues, 
            int profileType)
            throws AMException, SSOException {
        
        if (attrNames == null) {
            return getAttributes(token, entryDN, ignoreCompliance, byteValues,
                    profileType);
        }

        try {
            // Convert the attrNames to String[]
            String names[] = (String[]) attrNames.toArray(new String[attrNames
                    .size()]);
            PersistentObject po = UMSObject.getObjectHandle(token, new Guid(
                    entryDN));

            // Perform compliance related checks
            AttrSet attrSet;
            if (!ignoreCompliance
                    && ComplianceServicesImpl.isComplianceUserDeletionEnabled())
            { // check for deleted user by getting complaince attributes
                attrSet = complianceImpl.verifyAndGetAttributes(po, names);
            } else {
                attrSet = po.getAttributes(names);
            }
            AMHashMap attributes = (AMHashMap) CommonUtils.attrSetToMap(
                    attrSet, byteValues);

            // Obtain DC tree attributes if applicable            
            Map dcAttributes = getDCTreeAttributes(token, entryDN, attrNames,
                    byteValues, profileType);
            attributes.copy(dcAttributes);
            return attributes;
        } catch (UMSException e) {
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.getAttributes(): "
                        + "Unable to get attributes: ", e);
            }
            // Extract the ldap error code from Exception
            throw new AMException(token, "330", e);
        }
    }

    public String getOrgSearchFilter(String entryDN) {
        // The search filter calls here should return a global search
        // filter (not default) if a search template cannot be found for this
        // entryDN. It is a hack as entryDN may not be orgDN, but right now
        // only solution.
        String orgSearchFilter = SearchFilterManager.getSearchFilter(
                AMObject.ORGANIZATION, entryDN, null, true);
        String orgUnitSearchFilter = SearchFilterManager.getSearchFilter(
                AMObject.ORGANIZATIONAL_UNIT, entryDN, null, true);

        StringBuilder sb = new StringBuilder();
        sb.append("(|").append(orgSearchFilter).append(orgUnitSearchFilter);
        sb.append(")");
        return sb.toString();
    }

    /**
     * Gets the Organization DN for the specified entryDN. If the entry itself
     * is an org, then same DN is returned.
     * <p>
     * <b>NOTE:</b> This method will involve serveral directory searches, hence
     * be cautious of Performance hit
     * 
     * @param token
     *            a valid SSOToken
     * @param entryDN
     *            the entry whose parent Organization is to be obtained
     * @return the DN String of the parent Organization
     * @throws AMException
     *             if an error occured while obtaining the parent Organization
     */
    public String getOrganizationDN(SSOToken token, String entryDN)
            throws AMException {
        DN dnObject = new DN(entryDN);
        if (entryDN.length() == 0 || !dnObject.isDN()) {
            debug.error("DirectoryServicesImpl.getOrganizationDN() Invalid DN: "
                    + entryDN);
            throw new AMException(token, "157");
        }

        String organizationDN = null;
        while (organizationDN == null || organizationDN.length() == 0) {
            String childDN = dnObject.toString();
            organizationDN = verifyAndGetOrgDN(token, entryDN, childDN);
            dnObject = dnObject.getParent();
        }
        return organizationDN;
    }

    /**
     * Returns attributes from an external data store.
     * 
     * @param token
     *            Single sign on token of user
     * @param entryDN
     *            DN of the entry user is trying to read
     * @param attrNames
     *            Set of attributes to be read
     * @param profileType
     *            Integer determining the type of profile being read
     * @return A Map of attribute-value pairs
     * @throws AMException
     *             if an error occurs when trying to read external datastore
     */
    public Map getExternalAttributes(SSOToken token, String entryDN,
            Set attrNames, int profileType) throws AMException {
        SSOToken internalToken = CommonUtils.getInternalToken();
        String eDN;
        if (profileType == AMObject.USER) {
            eDN = (new DN(entryDN)).getParent().toString();
        } else {
            eDN = entryDN;
        }
        String orgDN = getOrganizationDN(internalToken, eDN);
        return callBackHelper.getAttributes(token, entryDN, attrNames, orgDN);
    }

    /**
     * Adds or remove static group DN to or from member attribute
     * 'iplanet-am-static-group-dn'
     * 
     * @param token
     *            SSOToken
     * @param members
     *            set of user DN's
     * @param staticGroupDN
     *            DN of the static group
     * @param toAdd
     *            true to add, false to remove
     * @throws AMException
     *             if there is an internal problem with AM Store.
     */
    public void updateUserAttribute(SSOToken token, Set members,
            String staticGroupDN, boolean toAdd) throws AMException {

        if (debug.messageEnabled()) {
            debug.message("DirectoryServicesImpl.updateUserAttribute(): " 
                    + "groupDN:" + staticGroupDN + ", toAdd: " + toAdd 
                    + " members: " + members);
        }   

        Attr attr = new Attr(STATIC_GROUP_DN_ATTRIBUTE, staticGroupDN);
        Iterator itr = members.iterator();
        while (itr.hasNext()) {
            String userDN = (String) itr.next();
            try {
                PersistentObject po = UMSObject.getObjectHandle(token,
                        new Guid(userDN));
                if (toAdd) {
                    po.modify(attr, ModSet.ADD);
                } else {
                    po.modify(attr, ModSet.DELETE);
                }
                po.save();
            } catch (UMSException e) {
                debug.error("DirectoryServicesImpl.updateUserAttribute(): " 
                        + "Failed while trying to set the static groupDN "
                        + staticGroupDN + " for user: " + userDN, e);
                throw new AMException(token, "351", e);
            }
        }
    }

    // *************************************************************************
    // All un-modified methods from DirectoryServicesImpl. (Comments only for
    // reference)
    // *************************************************************************
    private void makeNamingFirst(AttrSet attrSet, String namingAttr,
            String namingValue) {
        int index = attrSet.indexOf(namingAttr);
        if (index == -1) {
            attrSet.add(new Attr(namingAttr, namingValue));
        } else {
            Attr attr = attrSet.elementAt(index);
            attr.removeValue(namingValue);
            String[] values = attr.getStringValues();
            attr = new Attr(namingAttr, namingValue);
            attr.addValues(values);
            attrSet.replace(attr);
        }
    }

    /**
     * When an object is being created and attribute sets are being passed UMS
     * does not overrid objectclasses in the attribute set, with the ones from
     * creation template. This method takes care of that.
     * 
     * @param ct
     * @param aSet
     */
    private AttrSet combineOCs(CreationTemplate ct, AttrSet aSet) {
        // UMS creation template will not append default user
        // objectclasses if the "objectclass" attribute is present
        // so we need to append those default objectclass here
        Attr attr = aSet.getAttribute("objectclass");
        // if (attr != null) {
        // TO: To write a separate method for attrSet combine object class
        // values. Need to avoid conversion from string array to sets.

        // get default user objectclass from creation template
        Attr defAttr = ct.getAttribute("objectclass");
        Set addOCs = (attr != null) ? CommonUtils.stringArrayToSet(attr
                .getStringValues()) : new HashSet();
        Set ctOCs = CommonUtils.stringArrayToSet(defAttr.getStringValues());
        Set finalOCs = CommonUtils.combineOCs(addOCs, ctOCs);
        aSet.remove("objectclass");
        Attr finalOCAttr = new Attr("objectclass", (String[]) finalOCs
                .toArray(new String[finalOCs.size()]));
        aSet.add(finalOCAttr);
        // }
        return aSet;
    }

    /**
     * Method to create a user entry
     */
    private void createUser(SSOToken token, PersistentObject parentObj,
            Map attributes, String profileName) throws UMSException,
            AMEntryExistsException, AMException {
        String orgDN = getOrganizationDN(internalToken, parentObj.getDN());

        // Invoke the Pre Processing plugin
        String entryDN = getNamingAttribute(AMObject.USER) + "=" + profileName
                + "," + parentObj.getDN();
        attributes = callBackHelper.preProcess(token, entryDN, orgDN, null,
                attributes, CallBackHelper.CREATE, AMObject.USER, false);

        AttrSet attrSet = CommonUtils.mapToAttrSet(attributes);
        makeNamingFirst(attrSet, getNamingAttribute(AMObject.USER), 
                profileName);
        // Invoke the user password validation plugin
        UserPasswordValidationHelper pluginImpl = 
            new UserPasswordValidationHelper(token, orgDN);
        try {
            pluginImpl.validate(CommonUtils.attrSetToMap(attrSet));
        } catch (AMException ame) {
            debug.error("DirectoryServicesImpl.createUser(): Invalid "
                    + "characters for user", ame);
            throw ame;
        }

        TemplateManager tempMgr = TemplateManager.getTemplateManager();
        CreationTemplate creationTemp = tempMgr.getCreationTemplate(
                "BasicUser", new Guid(orgDN), TemplateManager.SCOPE_ANCESTORS);
        attrSet = combineOCs(creationTemp, attrSet);

        // User user = new User(creationTemp, attrSet);
        PersistentObject user = new PersistentObject(creationTemp, attrSet);
        try {
            parentObj.addChild(user);
        } catch (AccessRightsException e) {
            if (debug.warningEnabled()) {
                debug.warning(
                        "DirectoryServicesImpl.createUser(): Insufficient "
                                + "Access rights to create user", e);
            }
            throw new AMException(token, "460");
        } catch (EntryAlreadyExistsException ee) {
            if (ComplianceServicesImpl.isComplianceUserDeletionEnabled()) { 
                // COMPLIANCE
                // If the existing entry is marked for deletion, then
                // the error message should be different.
                complianceImpl.checkIfDeletedUser(token, user.getDN());
            }
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.createUser() User "
                        + "already exists: ", ee);
            }
            throw new AMEntryExistsException(token, "328", ee);
        } catch (UMSException ue) {
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.createUser(): Internal "
                        + "Error occurred. Unable to create User Entry", ue);
            }
            processInternalException(token, ue, "324");
        }

        // Invoke Post processing impls
        callBackHelper.postProcess(token, user.getDN(), orgDN, null,
                attributes, CallBackHelper.CREATE, AMObject.USER, false);

        // TODO: REMOVE after Portal moves to new API's
        AMUserEntryProcessed postPlugin = getUserPostPlugin();
        if (postPlugin != null) {
            Map attrMap = CommonUtils.attrSetToMap(attrSet);
            postPlugin.processUserAdd(token, user.getDN(), attrMap);
        }
        EmailNotificationHelper mailerObj = new EmailNotificationHelper(user
                .getDN());
        mailerObj.setUserCreateNotificationList();
        mailerObj.sendUserCreateNotification(attributes);
    }

    /**
     * Method to create a user entry
     */
    private void createEntity(SSOToken token, PersistentObject parentObj,
            int objectType, Map attributes, String profileName)
            throws UMSException, AMEntryExistsException, AMException {
        String orgDN = getOrganizationDN(internalToken, parentObj.getDN());

        // Invoke the Pre Processing plugin
        AttrSet attrSet = CommonUtils.mapToAttrSet(attributes);
        makeNamingFirst(attrSet, getNamingAttribute(objectType), profileName);
        String ctName = getCreationTemplateName(objectType);
        if (ctName == null) {
            // Create a user if no CT defined.
            ctName = "BasicUser";
        }
        TemplateManager tempMgr = TemplateManager.getTemplateManager();
        CreationTemplate creationTemp = tempMgr.getCreationTemplate(ctName,
                new Guid(orgDN), TemplateManager.SCOPE_ANCESTORS);
        attrSet = combineOCs(creationTemp, attrSet);

        PersistentObject user = new PersistentObject(creationTemp, attrSet);
        try {
            parentObj.addChild(user);
        } catch (AccessRightsException e) {
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.createEntity():"
                        + " Insufficient Access rights to create entity", e);
            }
            throw new AMException(token, "460");
        } catch (EntryAlreadyExistsException ee) {
            if (ComplianceServicesImpl.isComplianceUserDeletionEnabled()) { 
                // COMPLIANCE
                // If the existing entry is marked for deletion, then
                // the error message should be different.
                complianceImpl.checkIfDeletedUser(token, user.getDN());
            }
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.createEntity() Entity "
                        + "already exists: ", ee);
            }
            throw new AMEntryExistsException(token, "462", ee);
        } catch (UMSException ue) {
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.createEntity(): Internal "
                        + "Error occurred. Unable to create User Entry", ue);
            }
            processInternalException(token, ue, "324");
        }
    }

    private void createResource(PersistentObject parentObj, Map attributes,
            String profileName) throws UMSException, AMException {
        AttrSet attrSet = CommonUtils.mapToAttrSet(attributes);
        makeNamingFirst(attrSet, getNamingAttribute(AMObject.RESOURCE),
                profileName);

        TemplateManager tempMgr = TemplateManager.getTemplateManager();

        String orgDN = getOrganizationDN(internalToken, parentObj.getDN());
        CreationTemplate creationTemp = tempMgr.getCreationTemplate(
                "BasicResource", new Guid(orgDN),
                TemplateManager.SCOPE_ANCESTORS);

        com.iplanet.ums.Resource resource = new com.iplanet.ums.Resource(
                creationTemp, attrSet);
        parentObj.addChild(resource);
    }

    private void createRole(SSOToken token, PersistentObject parentObj,
            Map attributes, String profileName) throws UMSException,
            AMException {
        // Invoke the Pre Processing plugin

        String orgDN = getOrganizationDN(internalToken, parentObj.getDN());
        String entryDN = getNamingAttribute(AMObject.ROLE) + "=" + profileName
                + "," + parentObj.getDN();
        attributes = callBackHelper.preProcess(token, entryDN, orgDN, null,
                attributes, CallBackHelper.CREATE, AMObject.ROLE, false);

        AttrSet attrSet = CommonUtils.mapToAttrSet(attributes);
        makeNamingFirst(attrSet, getNamingAttribute(AMObject.ROLE), 
                profileName);

        TemplateManager tempMgr = TemplateManager.getTemplateManager();
        CreationTemplate creationTemp = tempMgr.getCreationTemplate(
                "BasicManagedRole", new Guid(orgDN),
                TemplateManager.SCOPE_ANCESTORS);
        attrSet = combineOCs(creationTemp, attrSet);
        com.iplanet.ums.ManagedRole role = new com.iplanet.ums.ManagedRole(
                creationTemp, attrSet);
        parentObj.addChild(role);

        // Invoke Post processing impls
        callBackHelper.postProcess(token, role.getDN(), orgDN, null,
                attributes, CallBackHelper.CREATE, AMObject.ROLE, false);
    }

    private void createOrganization(SSOToken token, PersistentObject parentObj,
            Map attributes, String profileName) throws UMSException,
            AMException, SSOException {
        // Invoke the Pre Processing plugin. Note: we need to obtain
        // the parent org of this organization to obtain the
        // plugin classes for the parent org.

        String orgDN = getOrganizationDN(internalToken, parentObj.getDN());
        String entryDN = getNamingAttribute(AMObject.ORGANIZATION) + "="
                + profileName + "," + parentObj.getDN();
        attributes = callBackHelper
                .preProcess(token, entryDN, orgDN, null, attributes,
                        CallBackHelper.CREATE, AMObject.ORGANIZATION, false);

        AttrSet attrSet = CommonUtils.mapToAttrSet(attributes);
        makeNamingFirst(attrSet, getNamingAttribute(AMObject.ORGANIZATION),
                profileName);

        TemplateManager tempMgr = TemplateManager.getTemplateManager();
        com.iplanet.ums.Organization org = null;
        CreationTemplate creationTemp = tempMgr.getCreationTemplate(
                "BasicOrganization", new Guid(orgDN),
                TemplateManager.SCOPE_ANCESTORS);
        attrSet = combineOCs(creationTemp, attrSet);

        // COMPLIANCE: DCTREE
        if (dcTreeImpl.isRequired()) {
            AttrSet[] attrSetArray = dcTreeImpl.splitAttrSet(parentObj.getDN(),
                    attrSet);
            org = new com.iplanet.ums.Organization(creationTemp,
                    attrSetArray[0]);
            // create the DC node first. If it fails then the org node will not
            // be created at all. No clean up needed afterwards then.
            dcTreeImpl.createDomain(token, new Guid(entryDN), attrSet);
        } else {
            org = new com.iplanet.ums.Organization(creationTemp, attrSet);
        }
        try {
            parentObj.addChild(org);
        } catch (UMSException ue) {
            // clean up DC node
            if (dcTreeImpl.isRequired()) {
                dcTreeImpl.removeDomain(token, entryDN);
            }
            if (ComplianceServicesImpl.isComplianceUserDeletionEnabled()) { 
                // COMPLIANCE
                // If the existing entry is marked for deletion, then
                // the error message should be different.
                complianceImpl.checkIfDeletedOrg(token, org.getDN());
            }
            throw ue;
        }

        if (ComplianceServicesImpl.isAdminGroupsEnabled(org.getDN())) {
            complianceImpl.createAdminGroups(token, org);
        }

        // If Realms is enabled and is configured in backward compatibitly
        // mode, the corresponding realm must also be created.
        if (ServiceManager.isCoexistenceMode()
                && ServiceManager.isRealmEnabled()) {
            try {
                // Check if realm exisits, this throws SMSException
                // if realm does not exist
                new OrganizationConfigManager(token, entryDN);
            } catch (SMSException smse) {
                // Organization does not exist, create it
                if (debug.messageEnabled()) {
                    debug.message("DirectoryServicesImpl::createOrganization "
                            + "creating realm: " + org.getDN());
                }
                try {
                    OrganizationConfigManager ocm = 
                        new OrganizationConfigManager(token, orgDN);
                    ocm.createSubOrganization(profileName, null);
                } catch (SMSException se) {
                    if (debug.messageEnabled()) {
                        debug.message("DirectoryServicesImpl::" 
                                + "createOrganization unable to create realm: "
                                + org.getDN(), se);
                    }
                }
            }
        }

        // If in legacy mode, add the default services
        if (ServiceManager.isCoexistenceMode()) {
            try {
                OrganizationConfigManager ocm = new OrganizationConfigManager(
                        token, entryDN);
                OrganizationConfigManager.loadDefaultServices(token, ocm);
            } catch (SMSException smse) {
                // Unable to load default services
                if (debug.warningEnabled()) {
                    debug.warning("DirectoryServicesImpl::createOrganization "
                            + "Unable to load services: " + org.getDN());
                }
            }
        }

        // Invoke Post processing impls. Note: orgDN is parent org
        callBackHelper.postProcess(token, org.getDN(), orgDN, null, attributes,
                CallBackHelper.CREATE, AMObject.ORGANIZATION, false);
    }

    private void createGroup(SSOToken token, PersistentObject parentObj,
            Map attributes, String profileName) throws UMSException,
            AMException {
        // Invoke the Pre Processing plugin
        String orgDN = getOrganizationDN(internalToken, parentObj.getDN());

        String entryDN = getNamingAttribute(AMObject.GROUP) + "=" + profileName
                + "," + parentObj.getDN();
        attributes = callBackHelper.preProcess(token, entryDN, orgDN, null,
                attributes, CallBackHelper.CREATE, AMObject.GROUP, false);

        AttrSet attrSet = CommonUtils.mapToAttrSet(attributes);
        makeNamingFirst(attrSet, getNamingAttribute(AMObject.GROUP),
                profileName);

        TemplateManager tempMgr = TemplateManager.getTemplateManager();
        CreationTemplate creationTemp = tempMgr.getCreationTemplate(
                "BasicGroup", new Guid(orgDN), TemplateManager.SCOPE_ANCESTORS);
        attrSet = combineOCs(creationTemp, attrSet);
        com.iplanet.ums.StaticGroup sgroup = new com.iplanet.ums.StaticGroup(
                creationTemp, attrSet);
        parentObj.addChild(sgroup);

        Attr um = attrSet.getAttribute(UNIQUE_MEMBER_ATTRIBUTE);
        if (um != null) {
            String[] values = um.getStringValues();
            Set members = new HashSet();
            members.addAll(Arrays.asList(values));
            updateUserAttribute(token, members, sgroup.getDN(), true);
        }
        // Invoke Post processing impls
        callBackHelper.postProcess(token, sgroup.getDN(), orgDN, null,
                attributes, CallBackHelper.CREATE, AMObject.GROUP, false);
    }

    private void createAssignDynamicGroup(SSOToken token,
            PersistentObject parentObj, Map attributes, String profileName)
            throws UMSException, AMException {
        // Invoke the Pre Processing plugin
        String orgDN = getOrganizationDN(internalToken, parentObj.getDN());
        String entryDN = getNamingAttribute(AMObject.GROUP) + "=" + profileName
                + "," + parentObj.getDN();
        attributes = callBackHelper.preProcess(token, entryDN, orgDN, null,
                attributes, CallBackHelper.CREATE,
                AMObject.ASSIGNABLE_DYNAMIC_GROUP, false);

        AttrSet attrSet = CommonUtils.mapToAttrSet(attributes);
        makeNamingFirst(attrSet,
                getNamingAttribute(AMObject.ASSIGNABLE_DYNAMIC_GROUP),
                profileName);

        TemplateManager tempMgr = TemplateManager.getTemplateManager();
        CreationTemplate creationTemp = tempMgr.getCreationTemplate(
                "BasicAssignableDynamicGroup", new Guid(orgDN),
                TemplateManager.SCOPE_ANCESTORS);
        attrSet = combineOCs(creationTemp, attrSet);
        AssignableDynamicGroup adgroup = new AssignableDynamicGroup(
                creationTemp, attrSet);
        adgroup.setSearchFilter("(memberof=" + entryDN + ")");
        adgroup.setSearchScope(com.sun.identity.shared.ldap.LDAPv2.SCOPE_SUB);
        adgroup.setSearchBase(new Guid(orgDN));
        parentObj.addChild(adgroup);

        // Invoke Post processing impls
        callBackHelper.postProcess(token, adgroup.getDN(), orgDN, null,
                attributes, CallBackHelper.CREATE,
                AMObject.ASSIGNABLE_DYNAMIC_GROUP, false);
    }

    private void createDynamicGroup(SSOToken token, PersistentObject parentObj,
            Map attributes, String profileName) throws UMSException,
            AMException {
        // Invoke the Pre Process plugin
        String orgDN = getOrganizationDN(internalToken, parentObj.getDN());
        String entryDN = getNamingAttribute(AMObject.GROUP) + "=" + profileName
                + "," + parentObj.getDN();
        attributes = callBackHelper.preProcess(token, entryDN, orgDN, null,
                attributes, CallBackHelper.CREATE, AMObject.DYNAMIC_GROUP,
                false);

        AttrSet attrSet = CommonUtils.mapToAttrSet(attributes);
        makeNamingFirst(attrSet, getNamingAttribute(AMObject.DYNAMIC_GROUP),
                profileName);

        TemplateManager tempMgr = TemplateManager.getTemplateManager();
        CreationTemplate creationTemp = tempMgr.getCreationTemplate(
                "BasicDynamicGroup", new Guid(orgDN),
                TemplateManager.SCOPE_ANCESTORS);
        attrSet = combineOCs(creationTemp, attrSet);
        com.iplanet.ums.DynamicGroup dgroup = new com.iplanet.ums.DynamicGroup(
                creationTemp, attrSet);
        String filter = dgroup.getSearchFilter();
        if (LDAPUrl.defaultFilter.equalsIgnoreCase(filter)) {
            dgroup.setSearchFilter(SearchFilterManager.getSearchFilter(
                    AMObject.USER, orgDN));
        }
        dgroup.setSearchScope(com.sun.identity.shared.ldap.LDAPv2.SCOPE_SUB);
        dgroup.setSearchBase(new Guid(orgDN));
        parentObj.addChild(dgroup);

        // Invoke Post processing impls
        callBackHelper.postProcess(token, dgroup.getDN(), orgDN, null,
                attributes, CallBackHelper.CREATE, AMObject.DYNAMIC_GROUP,
                false);
    }

    private void createPeopleContainer(PersistentObject parentObj,
            Map attributes, String profileName) throws UMSException,
            AMException {
        AttrSet attrSet = CommonUtils.mapToAttrSet(attributes);
        makeNamingFirst(attrSet, getNamingAttribute(AMObject.PEOPLE_CONTAINER),
                profileName);

        TemplateManager tempMgr = TemplateManager.getTemplateManager();
        String orgDN = getOrganizationDN(internalToken, parentObj.getDN());
        CreationTemplate creationTemp = tempMgr.getCreationTemplate(
                "BasicPeopleContainer", new Guid(orgDN),
                TemplateManager.SCOPE_ANCESTORS);
        attrSet = combineOCs(creationTemp, attrSet);
        com.iplanet.ums.PeopleContainer pc = new PeopleContainer(creationTemp,
                attrSet);
        parentObj.addChild(pc);
    }

    private void createOrganizationalUnit(SSOToken token,
            PersistentObject parentObj, Map attributes, String profileName)
            throws UMSException, AMException {
        // Invoke the Pre Post Plugins
        String orgDN = getOrganizationDN(internalToken, parentObj.getDN());
        String entryDN = getNamingAttribute(AMObject.ORGANIZATIONAL_UNIT) + "="
                + profileName + "," + parentObj.getDN();
        attributes = callBackHelper.preProcess(token, entryDN, orgDN, null,
                attributes, CallBackHelper.CREATE,
                AMObject.ORGANIZATIONAL_UNIT, false);

        AttrSet attrSet = CommonUtils.mapToAttrSet(attributes);
        makeNamingFirst(attrSet,
                getNamingAttribute(AMObject.ORGANIZATIONAL_UNIT), profileName);

        TemplateManager tempMgr = TemplateManager.getTemplateManager();
        CreationTemplate creationTemp = tempMgr.getCreationTemplate(
                "BasicOrganizationalUnit", new Guid(orgDN),
                TemplateManager.SCOPE_ANCESTORS);
        attrSet = combineOCs(creationTemp, attrSet);
        OrganizationalUnit ou = new OrganizationalUnit(creationTemp, attrSet);
        parentObj.addChild(ou);
        // Invoke Post processing impls
        callBackHelper.postProcess(token, ou.getDN(), orgDN, null, attributes,
                CallBackHelper.CREATE, AMObject.ORGANIZATIONAL_UNIT, false);
    }

    private void createGroupContainer(PersistentObject parentObj,
            Map attributes, String profileName) throws UMSException,
            AMException {
        AttrSet attrSet = CommonUtils.mapToAttrSet(attributes);
        makeNamingFirst(attrSet, getNamingAttribute(AMObject.GROUP_CONTAINER),
                profileName);

        TemplateManager tempMgr = TemplateManager.getTemplateManager();
        String orgDN = getOrganizationDN(internalToken, parentObj.getDN());
        CreationTemplate creationTemp = tempMgr.getCreationTemplate(
                "BasicGroupContainer", new Guid(orgDN),
                TemplateManager.SCOPE_ANCESTORS);

        OrganizationalUnit gc = new OrganizationalUnit(creationTemp, attrSet);
        parentObj.addChild(gc);
    }

    private void createFilteredRole(SSOToken token, PersistentObject parentObj,
            Map attributes, String profileName) throws UMSException,
            AMException {
        // Invoke the Pre Processing plugin
        String orgDN = getOrganizationDN(internalToken, parentObj.getDN());
        String entryDN = getNamingAttribute(AMObject.FILTERED_ROLE) + "="
                + profileName + "," + parentObj.getDN();
        attributes = callBackHelper.preProcess(token, entryDN, orgDN, null,
                attributes, CallBackHelper.CREATE, AMObject.FILTERED_ROLE,
                false);

        AttrSet attrSet = CommonUtils.mapToAttrSet(attributes);
        makeNamingFirst(attrSet, getNamingAttribute(AMObject.FILTERED_ROLE),
                profileName);

        TemplateManager tempMgr = TemplateManager.getTemplateManager();
        CreationTemplate creationTemp = tempMgr.getCreationTemplate(
                "BasicFilteredRole", new Guid(orgDN),
                TemplateManager.SCOPE_ANCESTORS);
        attrSet = combineOCs(creationTemp, attrSet);
        if (!attrSet.contains(FilteredRole.FILTER_ATTR_NAME)) {
            Attr attr = new Attr(FilteredRole.FILTER_ATTR_NAME,
                    SearchFilterManager.getSearchFilter(AMObject.USER, orgDN));
            attrSet.add(attr);
        }

        FilteredRole frole = new FilteredRole(creationTemp, attrSet);
        parentObj.addChild(frole);
        // Invoke Post processing impls
        callBackHelper.postProcess(token, frole.getDN(), orgDN, null,
                attributes, CallBackHelper.CREATE, AMObject.FILTERED_ROLE,
                false);
    }

    /**
     * Create an entry in the Directory
     * 
     * @param token
     *            SSOToken
     * @param entryName
     *            name of the entry (naming value), e.g. "sun.com", "manager"
     * @param objectType
     *            Profile Type, ORGANIZATION, AMObject.ROLE, AMObject.USER, etc.
     * @param parentDN
     *            the parent DN
     * @param attributes
     *            the initial attribute set for creation
     */
    public void createEntry(SSOToken token, String entryName, int objectType,
            String parentDN, Map attributes) throws AMEntryExistsException,
            AMException, SSOException {
        try {
            if (entryName == null || entryName.length() == 0) {
                throw new AMException(token, "320");
            } else if (parentDN == null) {
                throw new AMException(token, "322");
            }
            // tmpDN to be used only when validating since the method
            // expects a DN.
            String tmpDN = getNamingAttribute(objectType) + "=" + entryName
                    + "," + parentDN;
            validateAttributeUniqueness(tmpDN, objectType, true, attributes);
            // Get handle to the parent object
            PersistentObject po = UMSObject.getObjectHandle(token, new Guid(
                    parentDN));

            switch (objectType) {
            case AMObject.USER:
                createUser(token, po, attributes, entryName);
                break;
            case AMObject.MANAGED_ROLE:
            case AMObject.ROLE: // same as MANAGED ROLE
                createRole(token, po, attributes, entryName);
                break;
            case AMObject.ORGANIZATION:
                createOrganization(token, po, attributes, entryName);
                break;
            case AMObject.STATIC_GROUP:
            case AMObject.GROUP:
                createGroup(token, po, attributes, entryName);
                break;
            case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                createAssignDynamicGroup(token, po, attributes, entryName);
                break;
            case AMObject.DYNAMIC_GROUP:
                createDynamicGroup(token, po, attributes, entryName);
                break;
            case AMObject.PEOPLE_CONTAINER:
                createPeopleContainer(po, attributes, entryName);
                break;
            case AMObject.ORGANIZATIONAL_UNIT:
                createOrganizationalUnit(token, po, attributes, entryName);
                break;
            case AMObject.GROUP_CONTAINER:
                createGroupContainer(po, attributes, entryName);
                break;
            case AMObject.FILTERED_ROLE:
                createFilteredRole(token, po, attributes, entryName);
                break;
            case AMObject.RESOURCE:
                createResource(po, attributes, entryName);
                break;
            case AMObject.UNDETERMINED_OBJECT_TYPE:
            case AMObject.UNKNOWN_OBJECT_TYPE:
                throw new AMException(token, "326");
            default: // Supported generic type
                createEntity(token, po, objectType, attributes, entryName);
            }
        } catch (AccessRightsException e) {
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.createEntry() "
                        + "Insufficient access rights to create entry: "
                        + entryName, e);
            }
            throw new AMException(token, "460");
        } catch (EntryAlreadyExistsException e) {
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.createEntry() Entry: "
                        + entryName + "already exists: ", e);
            }
            String msgid = getEntryExistsMsgID(objectType);
            String name = getEntryName(e);
            Object args[] = { name };
            throw new AMException(AMSDKBundle.getString(msgid, args), msgid,
                    args);
        } catch (UMSException e) {
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.createEntry() Unable to "
                        + "create entry: " + entryName, e);
            }
            throw new AMException(token, "324", e);
        }
    }

    private void processPreDeleteCallBacks(SSOToken token, String entryDN,
            Map attributes, String organizationDN, int objectType,
            boolean softDelete) throws AMException, SSOException {
        // Call pre-processing user impls
        if (objectType != AMObject.PEOPLE_CONTAINER
                && objectType != AMObject.GROUP_CONTAINER) {
            String parentOrgDN = organizationDN;
            if (objectType == AMObject.ORGANIZATION
                    || objectType == AMObject.ORGANIZATIONAL_UNIT) {
                // Get the parent oganization for this org.
                DN rootDN = new DN(AMStoreConnection.getAMSdkBaseDN());
                DN currentOrgDN = new DN(organizationDN);
                if (!rootDN.equals(currentOrgDN)) {
                    String parentDN = (new DN(organizationDN)).getParent()
                            .toString();
                    parentOrgDN = getOrganizationDN(internalToken, parentDN);
                }
            }
            if (attributes == null) { // Not already retrieved
                attributes = getAttributes(token, entryDN, objectType);
            }
            callBackHelper.preProcess(token, entryDN, parentOrgDN, attributes,
                    null, CallBackHelper.DELETE, objectType, softDelete);
        }
    }

    private void processPostDeleteCallBacks(SSOToken token, String entryDN,
            Map attributes, String organizationDN, int objectType,
            boolean softDelete) throws AMException {
        // Invoke post processing impls
        if (objectType != AMObject.PEOPLE_CONTAINER
                && objectType != AMObject.GROUP_CONTAINER) {
            String parentOrgDN = organizationDN;
            if (objectType == AMObject.ORGANIZATION
                    || objectType == AMObject.ORGANIZATIONAL_UNIT) {
                // Get the parent oganization for this org.
                DN rootDN = new DN(AMStoreConnection.getAMSdkBaseDN());
                DN currentOrgDN = new DN(organizationDN);
                if (!rootDN.equals(currentOrgDN)) {
                    String parentDN = (new DN(organizationDN)).getParent()
                            .toString();
                    parentOrgDN = getOrganizationDN(internalToken, parentDN);
                }
            }
            callBackHelper.postProcess(token, entryDN, parentOrgDN, attributes,
                    null, CallBackHelper.DELETE, objectType, softDelete);
        }
    }

    /**
     * Remove an entry from the directory.
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            dn of the profile to be removed
     * @param objectType
     *            profile type
     * @param recursive
     *            if true, remove all sub entries & the object
     * @param softDelete
     *            Used to let pre/post callback plugins know that this delete is
     *            either a soft delete (marked for deletion) or a purge/hard
     *            delete itself, otherwise, remove the object only
     */
    public void removeEntry(SSOToken token, String entryDN, int objectType,
            boolean recursive, boolean softDelete) throws AMException,
            SSOException {
        if (debug.messageEnabled()) {
            debug.message("DirectoryServicesImpl.removeEntry(): Removing: "
                    + entryDN + " & recursive: " + recursive);
        }
        if (recursive) {
            // will list all entries in the sub-tree and delete them
            // one by one.
            removeSubtree(token, entryDN, softDelete);
        } else {
            removeSingleEntry(token, entryDN, objectType, softDelete);
        }

        // If Organization is deleted, and if realms in enabled and is
        // configured in backward compatibitly mode, the corresponding
        // realm must also be deleted.
        if (objectType == AMObject.ORGANIZATION
                && ServiceManager.isCoexistenceMode()
                && ServiceManager.isRealmEnabled()) {
            try {
                // Check if realm exisits, this throws SMSException
                // if realm does not exist
                OrganizationConfigManager ocm = new OrganizationConfigManager(
                        token, entryDN);
                // Since the above did not throw an exception, the
                // realm must be deleted
                ocm.deleteSubOrganization(null, recursive);
            } catch (SMSException smse) {
                if (debug.messageEnabled()) {
                    debug.message("DirectoryServicesImpl::removeEntry " +
                            "unable to delete corresponding realm: " + entryDN);
                }
            }
        }
    }

    /**
     * Private method to delete a single entry
     */
    private void removeSingleEntry(SSOToken token, String entryDN,
            int objectType, boolean softDelete) throws AMException,
            SSOException {

        Map attributes = null;
        EmailNotificationHelper mailer = null;
        String eDN = entryDN;
        if (objectType == AMObject.USER) {
            eDN = ((new DN(entryDN)).getParent()).toRFCString();
        }
        String orgDN = getOrganizationDN(internalToken, eDN);
        try {

            if (objectType == AMObject.USER) {
                // Extract a delete notification list
                mailer = new EmailNotificationHelper(entryDN);
                mailer.setUserDeleteNotificationList();
            }

            if ((getUserPostPlugin() != null)
                    || (mailer != null && mailer
                            .isPresentUserDeleteNotificationList())) {
                // Obtain the attributes needed to send notification and also
                // call backs as these won't be available after deletion
                attributes = getAttributes(token, entryDN, objectType);
            }

            processPreDeleteCallBacks(token, entryDN, attributes, orgDN,
                    objectType, softDelete);

            // if (recursive) {
            // deleteSubtree(token, entryDN, softDelete);
            // } else {
            if (dcTreeImpl.isRequired()) {
                String rfcDN = CommonUtils.formatToRFC(entryDN);
                dcTreeImpl.removeDomain(internalToken, rfcDN);
            }
            Guid guid = new Guid(entryDN);
            UMSObject.removeObject(token, guid);
            // }
        } catch (AccessRightsException e) {
            debug.error("DirectoryServicesImpl.removeEntry() Insufficient " 
                    + "access rights to remove entry: " + entryDN, e);
            throw new AMException(token, "460");
        } catch (EntryNotFoundException e) {
            String entry = getEntryName(e);
            debug.error("DirectoryServicesImpl.removeEntry() Entry not found: "
                    + entry, e);
            String msgid = getEntryNotFoundMsgID(objectType);
            Object args[] = { entry };
            String locale = CommonUtils.getUserLocale(token);
            throw new AMException(AMSDKBundle.getString(msgid, args, locale),
                    msgid, args);
        } catch (UMSException e) {
            debug.error("DirectoryServicesImpl.removeEntry() Unable to remove: "
                    + " Internal error occurred: ", e);
            throw new AMException(token, "325", e);
        }
        processPostDeleteCallBacks(token, entryDN, attributes, orgDN,
                objectType, softDelete);

        if (objectType == AMObject.USER) {
            AMUserEntryProcessed postPlugin = getUserPostPlugin();
            if (postPlugin != null) {
                // TODO: Remove after deprecating interface
                postPlugin.processUserDelete(token, entryDN, attributes);
            }
            if (mailer != null && mailer.isPresentUserDeleteNotificationList()) 
            {
                mailer.sendUserDeleteNotification(attributes);
            }
        }
    }

    /**
     * Private method used by "removeEntry" to delete an entire subtree
     */
    private void removeSubtree(SSOToken token, String entryDN,
            boolean softDelete) throws AMException, SSOException {
        int type = AMObject.UNKNOWN_OBJECT_TYPE;
        try {
            Guid guid = new Guid(entryDN);
            PersistentObject po = UMSObject
                    .getObjectHandle(internalToken, guid);

            // first get all the children of the object
            SearchControl control = new SearchControl();
            control.setSearchScope(SearchControl.SCOPE_SUB);
            String searchFilter = 
                "(|(objectclass=*)(objectclass=ldapsubEntry))";

            List list = new ArrayList();
            // get number of RDNs in the entry itself
            int entryRDNs = (new DN(entryDN)).countRDNs();
            // to count maximum level of RDNs in the search return
            int maxRDNCount = entryRDNs;
            // go through all search results, add DN to the list, and
            // set the maximun RDN count, will be used to remove DNs
            SearchResults children = po.getChildren(searchFilter, control);
            while (children.hasMoreElements()) {
                PersistentObject object = children.next();
                DN dn = new DN(object.getDN());
                if (debug.messageEnabled()) {
                    debug.message("DirectoryServicesImpl.removeEntry(): "
                            + "found child: " + object.getDN());
                }
                int count = dn.countRDNs();
                if (count > maxRDNCount) {
                    maxRDNCount = count;
                }
                list.add(dn);
            }

            if (debug.messageEnabled()) {
                debug.message("DirectoryServicesImpl.removeEntry(): max "
                        + "RDNs: " + maxRDNCount);
            }

            // go through all search results, delete entries from the
            // bottom up, starting from entries whose's RDN count
            // equals the maxRDNCount
            // TODO : If the list has too many entries, then the multiple
            // iteration in the inner for loop may be the bottleneck.
            // One enhancement to the existing algorithm is to store all
            // the entries by level in a different List. Per Sai's comments
            int len = list.size();
            for (int i = maxRDNCount; i >= entryRDNs; i--) {
                for (int j = 0; j < len; j++) {
                    DN dn = (DN) list.get(j);
                    // check if we need delete it now
                    if (dn.countRDNs() == i) {
                        // remove the entry
                        if (debug.messageEnabled()) {
                            debug.message("DirectoryServicesImpl."
                                    + "removeEntry(): del " + dn.toRFCString());
                        }
                        String rfcDN = dn.toRFCString();
                        type = AMObject.UNKNOWN_OBJECT_TYPE;
                        try {
                            type = getObjectType(internalToken, rfcDN);
                        } catch (AMException ae) {
                            // Not a managed type, just delete it.
                            Guid g = new Guid(rfcDN);
                            UMSObject.removeObject(token, g);
                        }
                        // Managed type. Might need pre/post callbacks.
                        // Do a non-recursive delete
                        if (type != AMObject.UNKNOWN_OBJECT_TYPE
                                && type != AMObject.UNDETERMINED_OBJECT_TYPE) {
                            try {
                                removeSingleEntry(token, rfcDN, type,
                                        softDelete);
                            } catch (AMPreCallBackException amp) {
                                debug.error("DirectoryServicesImpl." 
                                        + "removeSubTree: Aborting delete of: "
                                        + rfcDN 
                                        + " due to pre-callback exception",
                                        amp);
                            }
                        }

                        // remove the deleted entry from the list
                        list.remove(j);
                        // move back pointer, as current element is removed
                        j--;
                        // reduce list length
                        len--;
                    }
                }
            }

        } catch (AccessRightsException e) {
            debug.error("DirectoryServicesImpl.removeEntry() Insufficient " 
                    + "access rights to remove entry: " + entryDN, e);
            throw new AMException(token, "460");
        } catch (EntryNotFoundException e) {
            String entry = getEntryName(e);
            debug.error("DirectoryServicesImpl.removeEntry() Entry not found: "
                    + entry, e);
            String msgid = getEntryNotFoundMsgID(type);
            Object args[] = { entry };
            String locale = CommonUtils.getUserLocale(token);
            throw new AMException(AMSDKBundle.getString(msgid, args, locale),
                    msgid, args);
        } catch (UMSException e) {
            debug.error("DirectoryServicesImpl.removeEntry() Unable to remove: "
                    + " Internal error occurred: ", e);
            throw new AMException(token, "325", e);
        }
    }

    /**
     * Remove group admin role
     * 
     * @param token
     *            SSOToken of the caller
     * @param dn
     *            group DN
     * @param recursive
     *            true to delete all admin roles for all sub groups or sub
     *            people container
     */
    public void removeAdminRole(SSOToken token, String dn, boolean recursive)
            throws SSOException, AMException {
        SSOTokenManager.getInstance().validateToken(token);

        if (debug.messageEnabled()) {
            debug.message("DirectoryServicesImpl.removeAdminRole() dn: " + dn
                    + " recursive: " + recursive);
        }
        // first find out the admin role dn for the group
        DN ldapDN = new DN(dn);
        String orgDN = getOrganizationDN(token, ldapDN.getParent().toString());
        String newdn = dn.replace(',', '_');
        String roleNameAttr = getNamingAttribute(AMObject.ROLE);
        String roleDN = (new StringBuffer().append(roleNameAttr).append("=")
                .append(newdn).append(",").append(orgDN)).toString();

        Set adminRoles = Collections.EMPTY_SET;
        if (recursive) {
            String roleSearchFilter = SearchFilterManager.getSearchFilter(
                    AMObject.ROLE, orgDN);
            StringBuilder sb = new StringBuilder();
            sb.append("(&").append(roleSearchFilter).append("(");
            sb.append(roleNameAttr).append("=*").append(newdn).append("))");
            adminRoles = search(token, orgDN, sb.toString(),
                    SearchControl.SCOPE_ONE);
        } else {
            adminRoles = new HashSet();
            adminRoles.add(roleDN);
        }

        Iterator iter = adminRoles.iterator();
        while (iter.hasNext()) {
            String adminRoleDN = (String) iter.next();
            // remove all members from the role
            try {
                ManagedRole roleObj = (ManagedRole) UMSObject.getObject(token,
                        new Guid(adminRoleDN));
                roleObj.removeAllMembers();
                // removeEntry(token, adminRoleDN, AMObject.ROLE, false, false);
                AMStoreConnection amsc = new AMStoreConnection(internalToken);
                AMRole role = amsc.getRole(adminRoleDN);
                role.delete(recursive);
            } catch (Exception e) {
                if (debug.messageEnabled()) {
                    debug.message("DirectoryServicesImpl.removeAdminRole() "
                            + "Unable to admin roles:", e);
                }
            }
        }
    }

    /**
     * convert search results to a set of DNS
     */
    private Set searchResultsToSet(SearchResults results) throws UMSException {
        Set set = new OrderedSet();
        if (results != null) {
            while (results.hasMoreElements()) {
                PersistentObject one = results.next();
                set.add(one.getGuid().toString());
            }
        }
        return set;
    }

    /**
     * Searches the Directory
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the entry to start the search with
     * @param searchFilter
     *            search filter
     * @param searchScope
     *            search scope, BASE, ONELEVEL or SUBTREE
     * @return Set set of matching DNs
     */
    public Set search(SSOToken token, String entryDN, String searchFilter,
            int searchScope) throws AMException {
        Set resultSet = Collections.EMPTY_SET;
        try {
            PersistentObject po = UMSObject.getObjectHandle(token, new Guid(
                    entryDN));
            SearchControl control = new SearchControl();
            control.setSearchScope(searchScope);
            SearchResults results = po.search(searchFilter, control);
            resultSet = searchResultsToSet(results);
        } catch (UMSException ue) {
            LDAPException lex = (LDAPException) ue.getRootCause();
            int errorCode = lex.getLDAPResultCode();
            if (retryErrorCodes.contains("" + errorCode)) {
                throw new AMException(token, Integer.toString(errorCode), ue);
            }
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.search(token:, entryDN: "
                        + entryDN + ", searchFilter: " + searchFilter
                        + "searchScope: " + searchScope + " error occurred: ",
                        ue);
            }
            processInternalException(token, ue, "341");
        }
        return resultSet;
    }

    /**
     * convert search results to a AMSearchResults object TODO: Refactor code
     */
    private AMSearchResults getSearchResults(SearchResults results,
            SortKey skey, String[] attrNames, Collator collator,
            boolean getAllAttrs) throws UMSException {
        TreeMap tm = null;
        TreeSet tmpTreeSet = null;
        if (skey != null) {
            tm = new TreeMap(collator);
            tmpTreeSet = new TreeSet();
        }

        Set set = new OrderedSet();

        Map map = new HashMap();
        int errorCode = AMSearchResults.SUCCESS;
        try {
            if (results != null) {
                while (results.hasMoreElements()) {
                    PersistentObject po = results.next();
                    String dn = po.getGuid().toString();
                    if (tm != null) {
                        Attr attr = po.getAttribute(skey.attributeName);
                        if (attr != null) {
                            String attrValue = attr.getStringValues()[0];
                            Object obj = tm.get(attrValue);
                            if (obj == null) {
                                tm.put(attrValue, dn);
                            } else if (obj instanceof java.lang.String) {
                                TreeSet tmpSet = new TreeSet();
                                tmpSet.add(obj);
                                tmpSet.add(dn);
                                tm.put(attrValue, tmpSet);
                            } else {
                                ((TreeSet) obj).add(dn);
                            }
                        } else {
                            tmpTreeSet.add(dn);
                        }
                    } else {
                        set.add(dn);
                    }

                    AttrSet attrSet = new AttrSet();
                    if (attrNames != null) {
                        // Support for multiple return values
                        attrSet = po.getAttributes(attrNames, true);
                    } else {
                        /*
                         * Support for multiple return values when attribute
                         * names are not passed as part of the return
                         * attributes. This boolean check is to make sure user
                         * has set the setAllReturnAttributes flag in
                         * AMSearchControl in order to get all attributes or
                         * not.
                         */
                        if (getAllAttrs) {
                            attrSet = po.getAttributes(po.getAttributeNames(),
                                    true);
                        }
                    }
                    map.put(dn, CommonUtils.attrSetToMap(attrSet));
                }
            }
        } catch (SizeLimitExceededException slee) {
            errorCode = AMSearchResults.SIZE_LIMIT_EXCEEDED;
        } catch (TimeLimitExceededException tlee) {
            errorCode = AMSearchResults.TIME_LIMIT_EXCEEDED;
        }

        Integer count = (Integer) results
                .get(SearchResults.VLVRESPONSE_CONTENT_COUNT);
        int countValue;
        if (count == null) {
            countValue = AMSearchResults.UNDEFINED_RESULT_COUNT;
        } else {
            countValue = count.intValue();
        }

        if (tm != null) {
            Object[] values = tm.values().toArray();
            int len = values.length;
            if (skey.reverse) {
                for (int i = len - 1; i >= 0; i--) {
                    Object obj = values[i];
                    if (obj instanceof java.lang.String) {
                        set.add(obj);
                    } else {
                        set.addAll((Collection) obj);
                    }
                }
            } else {
                for (int i = 0; i < len; i++) {
                    Object obj = values[i];
                    if (obj instanceof java.lang.String) {
                        set.add(obj);
                    } else {
                        set.addAll((Collection) obj);
                    }
                }
            }

            Iterator iter = tmpTreeSet.iterator();
            while (iter.hasNext()) {
                set.add(iter.next());
            }
        }

        AMSearchResults searchResults = new AMSearchResults(countValue, set,
                errorCode, map);

        return searchResults;
    }

    // RENAME from searchUsingSearchControl => search()
    /**
     * Search the Directory
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the entry to start the search with
     * @param searchFilter
     *            search filter
     * @param searchControl
     *            search control defining the VLV indexes and search scope
     * @param attrNames
     *            name of attributes
     * @return Set set of matching DNs
     */
    public AMSearchResults search(SSOToken token, String entryDN,
            String searchFilter, SearchControl searchControl,
            String attrNames[]) throws AMException {
        AMSearchResults amResults = null;
        try {
            SortKey[] skeys = searchControl.getSortKeys();
            SortKey skey = null;
            if (skeys != null && skeys.length > 0
                    && skeys[0].attributeName != null) {
                skey = skeys[0];
            }
            String userLocale = CommonUtils.getUserLocale(token);
            if (debug.messageEnabled()) {
                debug.message("DirectoryServicesImpl.search() search with "
                        + "searchcontrol locale = " + userLocale);
            }
            Collator collator = Collator.getInstance(Locale
                    .getLocale(userLocale));

            SearchControl sc;
            if (skey != null) {
                sc = new SearchControl();
                sc.setMaxResults(searchControl.getMaxResults());
                sc.setSearchScope(searchControl.getSearchScope());
                sc.setTimeOut(searchControl.getTimeOut());
            } else {
                sc = searchControl;
            }

            PersistentObject po = UMSObject.getObjectHandle(token, new Guid(
                    entryDN));
            SearchResults results;
            if (attrNames == null) {
                if (skey == null) {
                    results = po.search(searchFilter, sc);
                } else {
                    String[] tmpAttrNames = { skey.attributeName };
                    results = po.search(searchFilter, tmpAttrNames, sc);
                }
            } else {
                if (skey == null) {
                    results = po.search(searchFilter, attrNames, sc);
                } else {
                    String[] tmpAttrNames = new String[attrNames.length + 1];
                    System.arraycopy(attrNames, 0, tmpAttrNames, 0,
                            attrNames.length);
                    tmpAttrNames[attrNames.length] = skey.attributeName;
                    results = po.search(searchFilter, tmpAttrNames, sc);
                }
            }
            amResults = getSearchResults(results, skey, attrNames, collator, sc
                    .isGetAllReturnAttributesEnabled());
        } catch (UMSException ue) {
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.search() with search "
                        + "control entryDN: " + entryDN + " Search Filter: "
                        + searchFilter + " Error occurred: ", ue);
            }
            processInternalException(token, ue, "341");
        }
        return amResults;
    }

    /**
     * Get members for roles, dynamic group or static group
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the role or group
     * @param objectType
     *            objectType of the target object, AMObject.ROLE or
     *            AMObject.GROUP
     * @return Set Member DNs
     */
    public Set getMembers(SSOToken token, String entryDN, int objectType)
            throws AMException {
        try {
            SearchResults results;
            switch (objectType) {
            case AMObject.ROLE:
            case AMObject.MANAGED_ROLE:
                ManagedRole role = (ManagedRole) UMSObject.getObject(token,
                        new Guid(entryDN));
                results = role.getMemberIDs();
                return searchResultsToSet(results);
            case AMObject.FILTERED_ROLE:
                FilteredRole filteredRole = (FilteredRole) UMSObject.getObject(
                        token, new Guid(entryDN));
                results = filteredRole.getMemberIDs();
                return searchResultsToSet(results);
            case AMObject.GROUP:
            case AMObject.STATIC_GROUP:
                StaticGroup group = (StaticGroup) UMSObject.getObject(token,
                        new Guid(entryDN));
                results = group.getMemberIDs();
                return searchResultsToSet(results);
            case AMObject.DYNAMIC_GROUP:
                DynamicGroup dynamicGroup = (DynamicGroup) UMSObject.getObject(
                        token, new Guid(entryDN));
                results = dynamicGroup.getMemberIDs();
                return searchResultsToSet(results);
            case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                // TODO: See if it works after removing this workaround
                // fake object to get around UMS problem.
                // UMS AssignableDynamicGroup has a class resolver, it is
                // added to resolver list in static block. So I need to
                // construct a dummy AssignableDynamicGroup
                AssignableDynamicGroup adgroup = (AssignableDynamicGroup) 
                    UMSObject.getObject(token, new Guid(entryDN));
                results = adgroup.getMemberIDs();
                return searchResultsToSet(results);
            default:
                throw new AMException(token, "114");
            }
        } catch (EntryNotFoundException e) {
            debug.error("DirectoryServicesImpl.getMembers() entryDN " + entryDN
                    + " objectType: " + objectType
                    + " Unable to get members: ", e);
            String msgid = getEntryNotFoundMsgID(objectType);
            String entryName = getEntryName(e);
            Object args[] = { entryName };
            throw new AMException(AMSDKBundle.getString(msgid, args), msgid,
                    args);
        } catch (UMSException e) {
            debug.error("DirectoryServicesImpl.getMembers() entryDN " + entryDN
                    + " objectType: " + objectType
                    + " Unable to get members: ", e);
            LDAPException le = (LDAPException) e.getRootCause();
            if (le != null
                    && (le.getLDAPResultCode() == 
                        LDAPException.SIZE_LIMIT_EXCEEDED || 
                        le.getLDAPResultCode() == 
                            LDAPException.ADMIN_LIMIT_EXCEEDED)) 
            {
                throw new AMException(token, "505", e);
            }
            throw new AMException(token, "454", e);
        }
    }

    /**
     * Renames an entry. Currently used for only user renaming
     * 
     * @param token
     *            the sso token
     * @param objectType
     *            the type of entry
     * @param entryDN
     *            the entry DN
     * @param newName
     *            the new name (i.e., if RDN is cn=John, the value passed should
     *            be "John"
     * @param deleteOldName
     *            if true the old name is deleted otherwise it is retained.
     * @return new <code>DN</code> of the renamed entry
     * @throws AMException
     *             if the operation was not successful
     */
    public String renameEntry(SSOToken token, int objectType, String entryDN,
            String newName, boolean deleteOldName) throws AMException {
        try {
            PersistentObject po = UMSObject.getObjectHandle(token, new Guid(
                    entryDN));
            String newRDN = getNamingAttribute(objectType) + "=" + newName;
            po.rename(newRDN, deleteOldName);
            return po.getDN();
        } catch (AccessRightsException e) {
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.renameEntry(): User does "
                        + "not have sufficient access rights ", e);
            }
            throw new AMException(token, "460");
        } catch (EntryNotFoundException e) {
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.renameEntry(): Entry "
                        + "not found: ", e);
            }
            String msgid = getEntryNotFoundMsgID(objectType);
            String entryName = getEntryName(e);
            Object args[] = { entryName };
            throw new AMException(AMSDKBundle.getString(msgid, args), msgid,
                    args);
        } catch (UMSException ume) {
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.renameEntry(): Unable to "
                        + "rename entry: ", ume);
            }
            throw new AMException(token, "360", ume);
        }
    }

    // TODO: Need to see if the split attributes to a another way of doing
    // this instead of passing an array. Need to see if the domain status can
    // also be set along with other attributes. Also DCTree code needs to use
    // Maps instead of attrSet.
    private Map setDCTreeAttributes(SSOToken token, String entryDN,
            Map attributes, int objectType) throws AMException, SSOException {
        if (objectType == AMObject.ORGANIZATION && dcTreeImpl.isRequired()
                && !entryDN.equals(AMStoreConnection.getAMSdkBaseDN())) {
            AttrSet attrSet = CommonUtils.mapToAttrSet(attributes);
            String status = attrSet.getValue(INET_DOMAIN_STATUS_ATTRIBUTE);
            if (status != null) {
                dcTreeImpl.updateDomainStatus(token, entryDN, status);
            }
            // split up the attrs to be set on DC node and organization node.
            AttrSet[] attrArray = dcTreeImpl.splitAttrSet(entryDN, attrSet);
            attrSet = attrArray[0];
            attributes = CommonUtils.attrSetToMap(attrSet);
            AttrSet domAttrSet = attrArray[1];
            dcTreeImpl.setDomainAttributes(token, entryDN, domAttrSet);
        }
        return attributes;
    }

    private void processPostModifyCallBacks(SSOToken token, String entryDN,
            Map oldAttributes, Map attributes, String organizationDN,
            int objectType) throws AMException {
        if (objectType != AMObject.PEOPLE_CONTAINER
                && objectType != AMObject.GROUP_CONTAINER) {
            String parentOrgDN = organizationDN;
            if (objectType == AMObject.ORGANIZATION
                    || objectType == AMObject.ORGANIZATIONAL_UNIT) {
                // Get the parent oganization for this org.
                // Get the parent oganization for this org.
                DN rootDN = new DN(AMStoreConnection.getAMSdkBaseDN());
                DN currentOrgDN = new DN(organizationDN);
                if (!rootDN.equals(currentOrgDN)) {
                    String parentDN = (new DN(organizationDN)).getParent()
                            .toString();
                    parentOrgDN = getOrganizationDN(internalToken, parentDN);
                }
            }
            callBackHelper.postProcess(token, entryDN, parentOrgDN,
                    oldAttributes, attributes, CallBackHelper.MODIFY,
                    objectType, false);
        }
    }

    private Map processPreModifyCallBacks(SSOToken token, String entryDN,
            Map oldAttributes, Map attributes, String organizationDN,
            int objectType) throws AMException, SSOException {
        if (objectType != AMObject.PEOPLE_CONTAINER
                && objectType != AMObject.GROUP_CONTAINER) {
            String parentOrgDN = organizationDN;
            if (objectType == AMObject.ORGANIZATION
                    || objectType == AMObject.ORGANIZATIONAL_UNIT) {
                // Get the parent oganization for this org.
                DN rootDN = new DN(AMStoreConnection.getAMSdkBaseDN());
                DN currentOrgDN = new DN(organizationDN);
                if (!rootDN.equals(currentOrgDN)) {
                    String parentDN = (new DN(organizationDN)).getParent()
                            .toString();
                    parentOrgDN = getOrganizationDN(internalToken, parentDN);
                }
            }
            if (oldAttributes == null) {
                Set attrNames = attributes.keySet();
                oldAttributes = getAttributes(token, entryDN, attrNames,
                        objectType);
            }
            attributes = callBackHelper.preProcess(token, entryDN, parentOrgDN,
                    oldAttributes, attributes, CallBackHelper.MODIFY,
                    objectType, false);
        }
        return attributes;
    }

    private void modifyPersistentObject(PersistentObject po, Attr attr,
            boolean isAdd, boolean isDelete) {
        if (isAdd) { // Add attribute
            po.modify(attr, ModSet.ADD);
        } else if (isDelete) { // Remove attribute
            po.modify(attr, ModSet.DELETE);
        } else { // Replace attribute
            po.modify(attr, ModSet.REPLACE);
        }
    }

    private void modifyAndSaveEntry(SSOToken token, String entryDN,
            Map stringAttributes, Map byteAttributes, boolean isAdd)
            throws AccessRightsException, EntryNotFoundException, UMSException {
        PersistentObject po = UMSObject.getObjectHandle(token,
                new Guid(entryDN));
        // Add string attributes
        if (stringAttributes != null && !stringAttributes.isEmpty()) {
            Iterator itr = stringAttributes.keySet().iterator();
            while (itr.hasNext()) {
                String attrName = (String) (itr.next());
                if (!attrName.equalsIgnoreCase("dn")) {
                    Set set = (Set) (stringAttributes.get(attrName));
                    String attrValues[] = (set == null) ? null : (String[]) set
                            .toArray(new String[set.size()]);
                    Attr attr = new Attr(attrName, attrValues);
                    /*
                     * AMObjectImpl.removeAttributes(...) sets the values to be
                     * Collections.EMPTY_SET.
                     */
                    modifyPersistentObject(po, attr, isAdd,
                            (set == AMConstants.REMOVE_ATTRIBUTE));
                }
            }
        }

        // Add byte attributes
        if (byteAttributes != null && !byteAttributes.isEmpty()) {
            Iterator itr = byteAttributes.keySet().iterator();
            while (itr.hasNext()) {
                String attrName = (String) (itr.next());
                byte[][] attrValues = (byte[][]) (byteAttributes.get(attrName));
                Attr attr = new Attr(attrName, attrValues);
                modifyPersistentObject(po, attr, isAdd, false);
            }
        }
        po.save();
    }

    // TODO: method rename from setProfileAttributes to setAttributes
    /**
     * Method Set the attributes of an entry.
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the profile whose template is to be set
     * @param objectType
     *            profile type
     * @param stringAttributes
     *            attributes to be set
     * @param byteAttributes
     *            attributes to be set
     * @param isAdd
     *            <code>true</code> if add to existing value;
     *            otherwise replace the existing value.
     */
    public void setAttributes(SSOToken token, String entryDN, int objectType,
            Map stringAttributes, Map byteAttributes, boolean isAdd)
            throws AMException, SSOException {
        Map oldAttributes = null;
        EmailNotificationHelper mailer = null;
        validateAttributeUniqueness(entryDN, objectType, false,
                stringAttributes);
        String eDN = entryDN;
        if (objectType == AMObject.USER) {
            eDN = (new DN(entryDN)).getParent().toString();
        }

        String orgDN = getOrganizationDN(internalToken, eDN);
        try {
            if (debug.messageEnabled()) {
                debug.message("DirectoryServicesImpl.setAttributes() entryDN: "
                        + entryDN);
            }

            if (objectType == AMObject.USER) { // Create user modification list
                // Invoke the user password validation plugin. Note: the
                // validation is done only for String attributes
                UserPasswordValidationHelper pluginImpl = new 
                    UserPasswordValidationHelper(token, orgDN);
                try {
                    pluginImpl.validate(stringAttributes);
                } catch (AMException ame) {
                    debug.error(
                            "DirectoryServicesImpl.setAttributes(): Invalid "
                                    + "characters for user", ame);
                    throw ame;
                }

                // Create a mailter instance
                mailer = new EmailNotificationHelper(entryDN);
                mailer.setUserModifyNotificationList();
            }

            if ((getUserPostPlugin() != null)
                    || (mailer != null && mailer
                            .isPresentUserModifyNotificationList())) {
                Set attrNames = stringAttributes.keySet();
                oldAttributes = getAttributes(token, entryDN, attrNames,
                        objectType);
            }

            // Call pre-processing user impls & get modified attributes
            // Note currently only String attributes supported
            stringAttributes = processPreModifyCallBacks(token, entryDN,
                    oldAttributes, stringAttributes, orgDN, objectType);
            // Set DCTree attributes
            setDCTreeAttributes(token, entryDN, stringAttributes, objectType);
            // modify and save the entry
            modifyAndSaveEntry(token, entryDN, stringAttributes,
                    byteAttributes, isAdd);
        } catch (AccessRightsException e) {
            if (debug.warningEnabled()) {
                debug.warning(
                        "DirectoryServicesImpl.setAttributes() User does "
                                + "not have sufficient access rights: ", e);
            }
            throw new AMException(token, "460");
        } catch (EntryNotFoundException ee) {
            if (debug.warningEnabled()) {
                debug.warning(
                        "DirectoryServicesImpl.setAttributes() Entry not "
                                + "found: ", ee);
            }
            String msgid = getEntryNotFoundMsgID(objectType);
            String entryName = getEntryName(ee);
            Object args[] = { entryName };
            throw new AMException(AMSDKBundle.getString(msgid, args), msgid,
                    args);
        } catch (UMSException e) {
            if (debug.warningEnabled())
                debug.warning("DirectoryServicesImpl.setAttributes() Internal "
                        + "error occurred", e);
            processInternalException(token, e, "452");
        }
        processPostModifyCallBacks(token, entryDN, oldAttributes,
                stringAttributes, orgDN, objectType);

        if (objectType == AMObject.USER) {
            AMUserEntryProcessed postPlugin = getUserPostPlugin();
            if (postPlugin != null) { // Invoke pre processing impls
                postPlugin.processUserModify(token, entryDN, oldAttributes,
                        stringAttributes);
            }
            if (mailer != null && mailer.isPresentUserModifyNotificationList()) 
            {
                mailer.sendUserModifyNotification(token, stringAttributes,
                        oldAttributes);
            }
        }
    }

    /**
     * Changes user password.
     * 
     * @param token Single sign on token
     * @param entryDN DN of the profile whose template is to be set
     * @param attrName password attribute name
     * @param oldPassword old password
     * @param newPassword new password
     * @throws AMException if an error occurs when changing user password
     * @throws SSOException If user's single sign on token is invalid.
     */
    public void changePassword(SSOToken token, String entryDN, String attrName,
        String oldPassword, String newPassword)
        throws AMException, SSOException {

        try {
            PersistentObject po = UMSObject.getObjectHandle(token,
                new Guid(entryDN));
            po.changePassword(entryDN, attrName, oldPassword, newPassword);
        } catch (UMSException umex) {
            debug.error("DirectoryServicesImpl.changePassword: ", umex); 
            throw new AMException(token, "362", umex);
        }
    }

    // ##########Group and role related APIs

    /**
     * Returns an array containing the dynamic group's scope, base dn, and
     * filter.
     */
    public String[] getGroupFilterAndScope(SSOToken token, String entryDN,
            int profileType) throws SSOException, AMException {
        String[] result = new String[3];
        int scope;
        String base;
        String gfilter;
        try {
            DynamicGroup dg = (DynamicGroup) UMSObject.getObject(token,
                    new Guid(entryDN));
            scope = dg.getSearchScope();
            base = dg.getSearchBase().getDn();
            gfilter = dg.getSearchFilter();
            result[0] = Integer.toString(scope);
            result[1] = base;
            result[2] = gfilter;
        } catch (EntryNotFoundException e) {
            debug.error("AMGroupImpl.searchUsers", e);
            String msgid = getEntryNotFoundMsgID(profileType);
            String expectionEntryName = getEntryName(e);
            Object args[] = { expectionEntryName };
            throw new AMException(AMSDKBundle.getString(msgid, args), msgid,
                    args);
        } catch (UMSException e) {
            debug.message("AMGroupImpl.searchUsers", e);
            throw new AMException(AMSDKBundle.getString("341"), "341", e);
        }
        return result;
    }

    /**
     * Sets the filter for a dynamic group in the datastore.
     * 
     * @param token
     * @param entryDN
     * @param filter
     * @throws AMException
     * @throws SSOException
     */
    public void setGroupFilter(SSOToken token, String entryDN, String filter)
            throws AMException, SSOException {
        try {
            DynamicGroup dynamicGroup = (DynamicGroup) UMSObject
                    .getObject(token, new Guid(entryDN));
            dynamicGroup.setSearchFilter(filter);
            dynamicGroup.save();
        } catch (UMSException ume) {
            debug.message("AMDynamicGroup.setSearchFilter() - Unable to "
                    + "setFilter()", ume);
            throw new AMException(token, "352", ume);
        }
    }

    /**
     * @param token
     * @param target
     * @param members
     * @param operation
     * @param profileType
     * @throws UMSException
     * @throws AMException
     */
    private void modifyRoleMembership(SSOToken token, String target,
            Set members, int operation, int profileType) throws UMSException,
            AMException {
        ManagedRole role;
        try {
            role = (ManagedRole) UMSObject.getObject(token, new Guid(target));
        } catch (ClassCastException e) {
            debug.message(
                    "DirectoryServicesImpl.modifyRoleMembership() - Unable to "
                            + "modify role membership", e);
            throw new AMException(token, "350");
        }

        // Since this target cannot be an Org. Get the parent
        String parentDN = role.getParentGuid().getDn();
        String orgDN = getOrganizationDN(token, parentDN);
        if (callBackHelper.isExistsPrePostPlugins(orgDN)) {
            members = callBackHelper.preProcessModifyMemberShip(token, target,
                    orgDN, members, operation, profileType);
            if (members == null || members.isEmpty()) {
                return;
            }
        }
        switch (operation) {
        case ADD_MEMBER:
            Guid[] membersGuid = CommonUtils.toGuidArray(members);
            role.addMembers(membersGuid);
            // COMPLIANCE: if admin role then perform iplanet
            // compilance related operations if needed.
            if (ComplianceServicesImpl.isAdminGroupsEnabled(parentDN)) {
                complianceImpl.verifyAndLinkRoleToGroup(token, membersGuid,
                        target);
            }
            break;
        case REMOVE_MEMBER:
            // UMS does not have Role.removerMembers : TBD
            Object[] entries = members.toArray();
            for (int i = 0; i < entries.length; i++) {
                role.removeMember(new Guid((String) entries[i]));
            }
            // COMPLIANCE: if admin role then perform iplanet
            // compilance related operations if needed.
            if (ComplianceServicesImpl.isAdminGroupsEnabled(parentDN)) {
                complianceImpl.verifyAndUnLinkRoleToGroup(token, members,
                        target);
            }
            break;
        default:
            throw new AMException(token, "114");
        }
        // Make call backs to the plugins to let them know modification to
        // role membership.
        if (callBackHelper.isExistsPrePostPlugins(orgDN)) {
            // Here the new members are just the ones added not the complete Set
            callBackHelper.postProcessModifyMemberShip(token, target, orgDN,
                    members, operation, profileType);
        }
    }

    private void modifyGroupMembership(SSOToken token, String target,
            Set members, int operation, int profileType) throws UMSException,
            AMException {

        StaticGroup group = (StaticGroup) UMSObject.getObject(token, new Guid(
                target));

        // Make call backs to the plugins to let them know modification
        // to role membership.
        // Since this target cannot be an Org. Get the parent
        String parentDN = group.getParentGuid().getDn();

        String orgDN = getOrganizationDN(token, parentDN);
        if (callBackHelper.isExistsPrePostPlugins(orgDN)) {
            members = callBackHelper.preProcessModifyMemberShip(token, target,
                    orgDN, members, operation, profileType);
            if (members == null || members.isEmpty()) {
                return;
            }
        }

        switch (operation) {
        case ADD_MEMBER:
            group.addMembers(CommonUtils.toGuidArray(members));
            updateUserAttribute(token, members, target, true);

            break;
        case REMOVE_MEMBER:
            // UMS does not have Role.removerMembers : TBD
            Object[] entries = members.toArray();
            for (int i = 0; i < entries.length; i++) {
                group.removeMember(new Guid((String) entries[i]));
            }
            updateUserAttribute(token, members, target, false);

            break;
        default:
            throw new AMException(token, "114");
        }

        // Make call backs to the plugins to let them know modification to
        // role membership.
        if (callBackHelper.isExistsPrePostPlugins(orgDN)) {
            // Here the new members are just the ones added not the complete Set
            callBackHelper.postProcessModifyMemberShip(token, target, orgDN,
                    members, operation, profileType);
        }
    }

    private void modifyAssignDynamicGroupMembership(SSOToken token,
            String target, Set members, int operation, int profileType)
            throws UMSException, AMException {
        // fake object to get around UMS problem.
        // UMS AssignableDynamicGroup has a class resolver, it is
        // added to resolver list in static block. So I need to
        // construct a dummy AssignableDynamicGroup
        AssignableDynamicGroup tmpgroup = new AssignableDynamicGroup();
        AssignableDynamicGroup adgroup = (AssignableDynamicGroup) UMSObject
                .getObject(token, new Guid(target));

        // Make call backs to the plugins to let them know modification
        // to role membership.
        // Since this target cannot be an Org. Get the parent
        String parentDN = adgroup.getParentGuid().getDn();
        String orgDN = getOrganizationDN(token, parentDN);
        if (callBackHelper.isExistsPrePostPlugins(orgDN)) {
            members = callBackHelper.preProcessModifyMemberShip(token, target,
                    orgDN, members, operation, profileType);
            if (members == null || members.isEmpty()) {
                return;
            }
        }
        switch (operation) {
        case ADD_MEMBER:
            Guid[] membersGuid = CommonUtils.toGuidArray(members);
            adgroup.addMembers(CommonUtils.toGuidArray(members));
            if (ComplianceServicesImpl
                    .isAdminGroupsEnabled(AMStoreConnection.getAMSdkBaseDN())) {
                complianceImpl.verifyAndLinkGroupToRole(token, membersGuid,
                        target);
            }
            break;
        case REMOVE_MEMBER:
            Object[] entries = members.toArray();
            for (int i = 0; i < entries.length; i++) {
                adgroup.removeMember(new Guid((String) entries[i]));
            }
            // COMPLIANCE: if admin group then perform iplanet
            // compliance related operations if needed.
            if (ComplianceServicesImpl
                    .isAdminGroupsEnabled(AMStoreConnection.getAMSdkBaseDN())) {
                complianceImpl.verifyAndUnLinkGroupToRole(token, members,
                        target);
            }
            break;
        default:
            throw new AMException(token, "114");
        }

        // Make call backs to the plugins to let them know modification to
        // role membership.
        if (callBackHelper.isExistsPrePostPlugins(orgDN)) {
            // Here the new members are just the ones added not the complete Set
            callBackHelper.postProcessModifyMemberShip(token, target, orgDN,
                    members, operation, profileType);
        }
    }

    private AMException generateMemberShipException(SSOToken token,
            String target, int objectType, EntryNotFoundException e) {
        DN errorDN = getExceptionDN(e);
        DN targetDN = new DN(target);
        if (errorDN == null) {
            debug.error("DirectoryServicesImpl.modMemberShip", e);
            Object args[] = { target };
            String locale = CommonUtils.getUserLocale(token);
            return new AMException(AMSDKBundle.getString("461", args, locale),
                    "461", args);
        }
        String entryName = ((RDN) errorDN.getRDNs().get(0)).getValues()[0];

        String errorCode = null;
        if (errorDN.equals(targetDN)) {
            switch (objectType) {
            case AMObject.ROLE:
            case AMObject.MANAGED_ROLE:
                errorCode = "465";
                break;
            case AMObject.GROUP:
            case AMObject.STATIC_GROUP:
            case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                errorCode = "466";
                break;
            }
        } else {
            errorCode = "468";
        }
        debug.error("DirectoryServicesImpl.modMemberShip() - Entry not found "
                + target, e);
        Object args[] = { entryName };
        return new AMException(AMSDKBundle.getString(errorCode, args),
                errorCode, args);

    }

    /**
     * Modify member ship for role or static group
     * 
     * @param token
     *            SSOToken
     * @param members
     *            Set of member DN to be operated
     * @param target
     *            DN of the target object to add the member
     * @param type
     *            type of the target object, AMObject.ROLE or AMObject.GROUP
     * @param operation
     *            type of operation, ADD_MEMBER or REMOVE_MEMBER
     */
    public void modifyMemberShip(SSOToken token, Set members, String target,
            int type, int operation) throws AMException {
        if (debug.messageEnabled()) {
            debug.message("DirectoryServicesImpl.modifyMemberShip: targetDN = <"
                    + target + ">, Members: " + members + ", object Type = "
                    + type + ", Operation = " + operation);
        }
        Iterator itr = members.iterator();
        while (itr.hasNext()) {
            String userDN = (String) itr.next();
            if (userDN.length() == 0 || !DN.isDN(userDN)) {
                debug.error("DirectoryServicesImpl.modifyMemberShip() " 
                        + "Invalid DN: " + userDN);
                throw new AMException(token, "157");
            }
        }
        try {
            switch (type) {
            case AMObject.ROLE:
            case AMObject.MANAGED_ROLE:
                modifyRoleMembership(token, target, members, operation, type);
                break;
            case AMObject.GROUP:
            case AMObject.STATIC_GROUP:
                modifyGroupMembership(token, target, members, operation, type);
                break;
            case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                modifyAssignDynamicGroupMembership(token, target, members,
                        operation, type);
                break;
            default:
                throw new AMException(token, "114");
            }
        } catch (AccessRightsException e) {
            debug.error("DirectoryServicesImpl.modMemberShip() - Insufficient "
                    + "access rights: ", e);
            throw new AMException(token, "460");
        } catch (EntryNotFoundException e) {
            throw generateMemberShipException(token, target, type, e);
        } catch (UMSException e) {
            debug.message("DirectoryServicesImpl.modMemberShip() - Unable to "
                    + "modify membership", e);
            throw new AMException(token, "350", e);
        }
    }

    // *************************************************************************
    // Service Related Functionality
    // *************************************************************************
    /**
     * Get registered services for an organization
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the org
     * @return Set set of service names
     */
    public Set getRegisteredServiceNames(SSOToken token, String entryDN)
            throws AMException {
        try {
            Set attrNames = new HashSet(1);
            attrNames.add(SERVICE_STATUS_ATTRIBUTE);

            // User dsame privileged user to get the registered
            // services. Admins of all levels will need this access
            Map attributes = getAttributes(internalToken, entryDN, attrNames,
                    AMObject.UNDETERMINED_OBJECT_TYPE);
            Set resultSet = Collections.EMPTY_SET;
            if (attributes.size() == 1) {
                resultSet = (Set) attributes.get(SERVICE_STATUS_ATTRIBUTE);
            }

            if (debug.messageEnabled()) {
                debug.message("DirectoryServicesImpl." 
                        + "getRegisteredServiceNames()"
                        + " Registered Service Names for entryDN: "
                        + entryDN + " are: " + resultSet);
            }
            return resultSet;
        } catch (Exception e) {
            debug.error("DirectoryServicesImpl.getRegisteredService", e);
            throw new AMException(token, "455");
        }
    }

    /**
     * Register a service for an org or org unit policy to a profile
     * 
     * @param token
     *            token
     * @param orgDN
     *            DN of the org
     * @param serviceName
     *            Service Name
     */
    public void registerService(SSOToken token, String orgDN,             
            String serviceName) throws AMException, SSOException {
        try {
            // This returns a valid set only if the service has
            // Dynamic attributes
            Set attrNames = getServiceAttributesWithQualifier(token,
                    serviceName);
            if ((attrNames != null) && !attrNames.isEmpty()) {
                PersistentObject po = UMSObject.getObjectHandle(token,
                        new Guid(orgDN));
                DirectCOSDefinition dcos = createCOSDefinition(serviceName,
                        attrNames);
                COSManager cm = COSManager.getCOSManager(token, po.getGuid());
                cm.addDefinition(dcos);
            }
        } catch (AccessRightsException e) {
            debug.error("DirectoryServicesImpl.registerService() "
                    + "Insufficient access rights to register service: "
                    + serviceName, e);
            throw new AMException(token, "460");
        } catch (EntryAlreadyExistsException e) {
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.registerService() "
                        + "Service " + serviceName + " already registered", e);
            }
            Object args[] = { serviceName };
            String locale = CommonUtils.getUserLocale(token);
            throw new AMException(AMSDKBundle.getString("464", args, locale),
                    "464", args);
        } catch (SMSException e) {
            debug.error("DirectoryServicesImpl.registerService() Unable to "
                    + "register service: " + serviceName, e);
            throw new AMException(token, "914");
        } catch (UMSException e) {
            debug.error("DirectoryServicesImpl.registerService() Unable to "
                    + "register service: " + serviceName, e);
            throw new AMException(token, "914", e);
        }
    }

    /**
     * Method to get the attribute names of a service with CosQualifier. For
     * example: Return set could be ["iplanet-am-web-agent-allow-list
     * merge-schemes", "iplanet-am-web-agent-deny-list merge-schemes"] This only
     * returns Dynamic attributes
     */
    private Set getServiceAttributesWithQualifier(SSOToken token,
            String serviceName) throws SMSException, SSOException {
        ServiceSchemaManager ssm = new ServiceSchemaManager(serviceName, token);
        ServiceSchema ss = null;
        try {
            ss = ssm.getSchema(SchemaType.DYNAMIC);
        } catch (SMSException sme) {
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.getServiceNames(): No "
                        + "schema defined for SchemaType.DYNAMIC type");
            }
        }
        if (ss == null) {
            return Collections.EMPTY_SET;
        }

        Set attrNames = new HashSet();
        Set attrSchemaNames = ss.getAttributeSchemaNames();
        Iterator itr = attrSchemaNames.iterator();
        while (itr.hasNext()) {
            String attrSchemaName = (String) itr.next();
            AttributeSchema attrSchema = ss.getAttributeSchema(attrSchemaName);
            String name = attrSchemaName + " " + attrSchema.getCosQualifier();
            attrNames.add(name);
        }
        return attrNames;
    }

    /**
     * Create a COS Definition based on serviceID & attribute set & type. For
     * policy attribute, will set cosattribute to "override" For other
     * attribute, will set cosattribute to "default"
     */
    private DirectCOSDefinition createCOSDefinition(String serviceID,
            Set attrNames) throws UMSException {
        // new attribute set
        AttrSet attrs = new AttrSet();
        // set naming attribute to the serviceID
        Attr attr = new Attr(ICOSDefinition.DEFAULT_NAMING_ATTR, serviceID);
        attrs.add(attr);
        // add cosspecifier
        attr = new Attr(ICOSDefinition.COSSPECIFIER, "nsrole");
        attrs.add(attr);
        // add cosattribute
        attr = new Attr(ICOSDefinition.COSATTRIBUTE);
        Iterator iter = attrNames.iterator();
        while (iter.hasNext()) {
            String attrName = (String) iter.next();
            attr.addValue(attrName);
        }
        attrs.add(attr);

        return new DirectCOSDefinition(attrs);
    }

    // Rename from removeService to unRegisterService
    /**
     * Un register service for a AMro profile.
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the profile whose service is to be removed
     * @param objectType
     *            profile type
     * @param serviceName
     *            Service Name
     * @param type
     *            Template type
     */
    public void unRegisterService(SSOToken token, String entryDN,
            int objectType, String serviceName, int type) throws AMException {
        if (type == AMTemplate.DYNAMIC_TEMPLATE) {

            // TODO:change "cn" to fleasible naming attribute for AMObject.ROLE
            try {
                PersistentObject po = UMSObject.getObjectHandle(token,
                        new Guid(entryDN));

                COSManager cm = null;
                // COS Definition to obtaint depends on different profile type
                switch (objectType) {
                case AMObject.ROLE:
                case AMObject.FILTERED_ROLE:
                    cm = COSManager.getCOSManager(token, po.getParentGuid());
                    break;
                case AMObject.ORGANIZATION:
                case AMObject.ORGANIZATIONAL_UNIT:
                case AMObject.PEOPLE_CONTAINER:
                    cm = COSManager.getCOSManager(token, po.getGuid());
                    break;
                default:
                    // entry other than AMObject.ROLE,FILTERED_ROLE,ORG,PC
                    // does not have COS
                    throw new AMException(token, "450");
                }

                DirectCOSDefinition dcos;
                try {
                    dcos = (DirectCOSDefinition) cm.getDefinition(serviceName);
                } catch (COSNotFoundException e) {
                    if (debug.messageEnabled()) {
                        debug.message("DirectoryServicesImpl." 
                                + "unRegisterService() "
                                + "No COSDefinition found for service: "
                                + serviceName);
                    }
                    Object args[] = { serviceName };
                    String locale = CommonUtils.getUserLocale(token);
                    throw new AMException(AMSDKBundle.getString("463", args,
                            locale), "463", args);
                }

                // Remove the COS Definition and Template
                dcos.removeCOSTemplates();
                cm.removeDefinition(serviceName);
            } catch (AccessRightsException e) {
                debug.error("DirectoryServicesImpl.unRegisterService() "
                        + "Insufficient Access rights to unRegister service: ",
                        e);
                throw new AMException(token, "460");
            } catch (UMSException e) {
                debug.error("DirectoryServicesImpl.unRegisterService: "
                        + "Unable to unregister service ", e);
                throw new AMException(token, "855", e);
            }
        }
    }

    /**
     * Get the AMTemplate DN (COSTemplateDN)
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the profile whose template is to be set
     * @param serviceName
     *            Service Name
     * @param type
     *            the template type, AMTemplate.DYNAMIC_TEMPLATE
     * @return String DN of the AMTemplate
     */
    public String getAMTemplateDN(SSOToken token, String entryDN,
            int objectType, String serviceName, int type) throws AMException {
        String roleDN = null;
        // TBD : get template on flexible naming attribute
        try {
            // get COS Definition depends on different profile type
            switch (objectType) {
            case AMObject.ROLE:
            case AMObject.FILTERED_ROLE:
                roleDN = entryDN;
                PersistentObject po = UMSObject.getObjectHandle(token,
                        new Guid(entryDN));
                return ("cn=\"" + roleDN + "\",cn=" + serviceName + "," + po
                        .getParentGuid().toString());
            case AMObject.ORGANIZATION:
            case AMObject.ORGANIZATIONAL_UNIT:
            case AMObject.PEOPLE_CONTAINER:
                roleDN = "cn=" + CONTAINER_DEFAULT_TEMPLATE_ROLE + ","
                        + entryDN;
                return ("cn=\"" + roleDN + "\",cn=" + serviceName + "," 
                        + entryDN);
            default:
                // entry other that AMObject.ROLE & FILTERED_ROLE & ORG & PC
                // does not have COS
                throw new AMException(token, "450");
            }
        } catch (UMSException e) {
            debug.error(
                    "DirectoryServicesImpl.getAMTemplateDN() Unable to get "
                            + "AMTemplate DN for service: " + serviceName
                            + " entryDN: " + entryDN, e);
            throw new AMException(token, "349", e);
        }
    }

    /**
     * Create an AMTemplate (COSTemplate)
     * 
     * @param token
     *            token
     * @param entryDN
     *            DN of the profile whose template is to be set
     * @param objectType
     *            the entry type
     * @param serviceName
     *            Service Name
     * @param attributes
     *            attributes to be set
     * @param priority
     *            template priority
     * @return String DN of the newly created template
     */
    public String createAMTemplate(SSOToken token, String entryDN,
            int objectType, String serviceName, Map attributes, int priority)
            throws AMException {
        // TBD, each time a Org/PC is created, need to create default role
        COSManager cm = null;
        DirectCOSDefinition dCOS = null;
        String roleDN = null;

        // TBD, change "cn" to flesible naming attrsibute for AMObject.ROLE
        try {
            PersistentObject po = UMSObject.getObjectHandle(token, new Guid(
                    entryDN));
            // get COS Definition depends on different profile type
            switch (objectType) {
            case AMObject.ROLE:
            case AMObject.FILTERED_ROLE:
                roleDN = entryDN;
                cm = COSManager.getCOSManager(token, po.getParentGuid());
                dCOS = (DirectCOSDefinition) cm.getDefinition(serviceName);
                break;
            case AMObject.ORGANIZATION:
            case AMObject.ORGANIZATIONAL_UNIT:
            case AMObject.PEOPLE_CONTAINER:
                roleDN = "cn=" + CONTAINER_DEFAULT_TEMPLATE_ROLE + ","
                        + entryDN;
                cm = COSManager.getCOSManager(token, po.getGuid());
                dCOS = (DirectCOSDefinition) cm.getDefinition(serviceName);
                break;
            default:
                // entry other that AMObject.ROLE & FILTERED_ROLE & ORG & PC
                // does not have COS
                throw new AMException(token, "450");
            }
            // add template priority
            AttrSet attrSet = CommonUtils.mapToAttrSet(attributes);
            if (priority != AMTemplate.UNDEFINED_PRIORITY) {
                Attr attr = new Attr("cospriority");
                attr.addValue("" + priority);
                attrSet.add(attr);
            }
            COSTemplate template = createCOSTemplate(serviceName, attrSet,
                    roleDN);
            dCOS.addCOSTemplate(template);
            return template.getGuid().toString();
        } catch (COSNotFoundException e) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryServicesImpl.createAMTemplate() "
                        + "COSDefinition for service: " + serviceName
                        + " not found: ", e);
            }
            Object[] args = { serviceName };
            String locale = CommonUtils.getUserLocale(token);
            throw new AMException(AMSDKBundle.getString("459", locale), "459",
                    args);
        } catch (EntryAlreadyExistsException e) {
            if (debug.messageEnabled()) {
                debug.message(
                        "DirectoryServicesImpl.createAMTemplate: template "
                                + "already exists for " + serviceName, e);
            }
            String params[] = { serviceName };
            String locale = CommonUtils.getUserLocale(token);
            throw new AMException(AMSDKBundle.getString("854", params, locale),
                    "854", params);
        } catch (AccessRightsException e) {
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.createAMTemplate() "
                        + "Insufficient access rights to create template for: "
                        + serviceName + " & entryDN: " + entryDN, e);
            }
            throw new AMException(token, "460");
        } catch (UMSException e) {
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.createAMTemplate() Unable" 
                        + " to create AMTemplate for: " + serviceName
                        + " & entryDN: " + entryDN, e);
            }
            Object[] args = { serviceName };
            String locale = CommonUtils.getUserLocale(token);
            throw new AMException(AMSDKBundle.getString("459", locale), "459",
                    args, e);
        } catch (Exception e) {
            if (debug.warningEnabled())
                debug.warning("DirectoryServicesImpl.createAMTemplate", e);
            throw new AMException(token, "451");
        }
    }

    /**
     * create COS Template from attribute set for a service, this will involve
     * UMS Creation template for COSTemplate
     * 
     * @param serviceID
     *            Service name
     * @param attrSet
     *            the attribute set
     * @param entryDN
     *            DN of the role
     * @return COSTemplate COS Template created
     */
    private COSTemplate createCOSTemplate(String serviceID, AttrSet attrset,
            String entryDN) throws UMSException {

        TemplateManager tempMgr = TemplateManager.getTemplateManager();
        CreationTemplate basicCOSTemplate = tempMgr.getCreationTemplate(
                "BasicCOSTemplate", null);

        // Now need to add the service object for the "serviceID" to the
        // required attribute set of the cos creatation template
        // need to use schema manager and service manager (TBD)
        // But for now just add "extensibleObject" to it
        COSTemplate cosTemplate = new COSTemplate(basicCOSTemplate, "\""
                + entryDN + "\"");
        cosTemplate.addTemplateAttribute("objectclass", "extensibleObject");

        if (debug.messageEnabled()) {
            debug.message("DirectoryServicesImpl.newCOSTemplate: cn = "
                    + entryDN + " COSTemplate = " + cosTemplate);
        }
        int size = attrset.size();
        for (int i = 0; i < size; i++) {
            Attr attr = attrset.elementAt(i);
            cosTemplate.modify(attr, ModSet.ADD);
        }

        return cosTemplate;
    }

    protected String getNamingAttribute(int objectType) {
        return NamingAttributeManager.getNamingAttribute(objectType);
    }

    /**
     * Gets the naming attribute after reading it from the corresponding
     * creation template. If not found, a default value will be used
     */
    public String getNamingAttribute(int objectType, String orgDN) {
        return NamingAttributeManager.getNamingAttribute(objectType, orgDN);
    }

    /**
     * Get the name of the creation template to use for specified object type.
     */
    public String getCreationTemplateName(int objectType) {
        return NamingAttributeManager.getCreationTemplateName(objectType);
    }

    public String getObjectClassFromDS(int objectType) {
        return getObjectClass(objectType);
    }

    public String getObjectClass(int objectType) {
        return ObjectClassManager.getObjectClass(objectType);
    }

    public int getObjectType(String objectClass) {
        return ObjectClassManager.getObjectType(objectClass);
    }

    public String getSearchFilterFromTemplate(int objectType, String orgDN,
            String searchTemplateName) {
        return SearchFilterManager.getSearchFilterFromTemplate(objectType,
                orgDN, searchTemplateName);
    }

    /**
     * Returns the set of attributes (both optional and required) needed for an
     * objectclass based on the LDAP schema
     * 
     * @param objectclass
     * @return the attributes for the objectclass
     */
    public Set getAttributesForSchema(String objectclass) {
        try {
            SchemaManager sm = SchemaManager.getSchemaManager(internalToken);
            return new HashSet(sm.getAttributes(objectclass));
        } catch (UMSException ue) {
            return Collections.EMPTY_SET;
        }
    }

    /**
     * Validate attribute uniqueness
     * 
     * @param newEntry
     *            true if create a new user
     * @throws AMException
     *             if attribute uniqueness is violated
     */
    void validateAttributeUniqueness(String entryDN, int profileType,
            boolean newEntry, Map modMap) throws AMException {
        boolean attrExists = false;
        if (modMap == null || modMap.isEmpty()) {
            return;
        }

        try {
            if (profileType == AMTemplate.DYNAMIC_TEMPLATE
                    || profileType == AMTemplate.ORGANIZATION_TEMPLATE
                    || profileType == AMTemplate.POLICY_TEMPLATE) {
                // no namespace validation for these objects
                return;
            }
            String[] rdns = LDAPDN.explodeDN(entryDN, false);
            int size = rdns.length;

            if (size < 2) {
                return;
            }

            String orgDN = rdns[size - 1];

            AMStoreConnection amsc = new AMStoreConnection(CommonUtils
                    .getInternalToken());
            DN rootDN = new DN(AMStoreConnection.getAMSdkBaseDN());
            DN thisDN = new DN(orgDN);

            for (int i = size - 2; i >= 0; i--) {
                if (debug.messageEnabled()) {
                    debug.message("AMObjectImpl.validateAttributeUniqueness: " 
                            + "try DN = " + orgDN);
                }

                int type = -1;

                if (!rootDN.isDescendantOf(thisDN)) {
                    try {
                        type = amsc.getAMObjectType(orgDN);
                    } catch (AMException ame) {
                        if (debug.warningEnabled()) {
                            debug.warning("AMObjectImpl." 
                                    + "validateAttributeUniqueness: "
                                    + "Unable to determine object type of "
                                    + orgDN
                                    + " :Attribute uniqueness check aborted..",
                                    ame);
                        }
                        return;
                    }
                }

                Set list = null;
                AMObject amobj = null;

                if (type == AMObject.ORGANIZATION) {
                    AMOrganization amorg = amsc.getOrganization(orgDN);
                    list = amorg.getAttribute(UNIQUE_ATTRIBUTE_LIST_ATTRIBUTE);
                    amobj = amorg;
                } else if (type == AMObject.ORGANIZATIONAL_UNIT) {
                    AMOrganizationalUnit amorgu = amsc
                            .getOrganizationalUnit(orgDN);
                    list = amorgu.getAttribute(UNIQUE_ATTRIBUTE_LIST_ATTRIBUTE);
                    amobj = amorgu;
                }
                if ((list != null) && !list.isEmpty()) {
                    if (debug.messageEnabled()) {
                        debug.message("AMObjectImpl." 
                                + "validateAttributeUniqueness: list ="+ list);
                    }

                    /*
                     * After adding the uniquness attributes 'ou,cn' to the
                     * list, creating a role with the same name as the existing
                     * user say 'amadmin' fails with 'Attribute uniqueness
                     * violation' The filter (|(cn='attrname')) is used for all
                     * objects. Fixed the code to look for 'Role' profile types
                     * and set the filter as
                     * (&(objectclass=ldapsubentry)
                     * (objectclass=nsroledefinition)
                     * (cn='attrname'))
                     * 
                     * The same issue happens when a group is created with
                     * existing user name. Fixed the code to look for 'Group'
                     * profile types and set the filter as
                     * (&(objectClass=groupofuniquenames)
                     * (objectClass=iplanet-am-managed-group)(cn='attrname'))
                     * The logic in the while loop is iterate through the
                     * attribute unique list and check if the list contains the
                     * naming attribute of the object we are trying to create.
                     * If the naming attribute is in the list,then look if the
                     * profile type of the object we are trying to create is
                     * 'role' or 'group', add appropriate objectclasses and the
                     * entry rdn to the search filter. This filter is used to
                     * search the iDS and determine the attribute uniqueness
                     * violation. The boolean variable 'attrExists' is set to
                     * false initially. This variable is set to true when the
                     * profile type is 'role' or 'group'. The check for this
                     * boolean variable decides the number of matching closing
                     * parens of the three different types of filters.
                     */

                    Iterator iter = list.iterator();
                    StringBuffer filterSB = new StringBuffer();
                    StringBuffer newEntrySB = new StringBuffer();
                    filterSB.append("(|");

                    while (iter.hasNext()) {
                        String[] attrList = getAttrList((String) iter.next());
                        Set attr = getAttrValues(attrList, modMap);
                        for (int j = 0; j < attrList.length; j++) {
                            String attrName = attrList[j];
                            if (attrName
                                    .equals(getNamingAttribute(profileType))
                                    && newEntry) {
                                if ((profileType == AMObject.ROLE)
                                        || (profileType == 
                                            AMObject.MANAGED_ROLE)
                                        || (profileType == 
                                            AMObject.FILTERED_ROLE)) 
                                {
                                    newEntrySB.append("(&");
                                    newEntrySB.append(
                                            "(objectclass=ldapsubentry)");
                                    newEntrySB.append("(" +
                                            "objectclass=nsroledefinition)");
                                    attrExists = true;
                                } else if ((profileType == AMObject.GROUP)
                                        || (profileType == 
                                            AMObject.STATIC_GROUP)
                                        || (profileType == 
                                            AMObject.ASSIGNABLE_DYNAMIC_GROUP)
                                        || (profileType == 
                                            AMObject.DYNAMIC_GROUP)) 
                                {
                                    newEntrySB.append("(&");
                                    newEntrySB.append(
                                      "(objectclass=iplanet-am-managed-group)");
                                    newEntrySB.append(
                                            "(objectclass=groupofuniquenames)");
                                    attrExists = true;
                                } else if (profileType == AMObject.ORGANIZATION)
                                {
                                    newEntrySB.append("(&(!");
                                    newEntrySB.append("(objectclass=");
                                    newEntrySB
                                            .append(SMSEntry.OC_REALM_SERVICE);
                                    newEntrySB.append("))");
                                    attrExists = true;
                                }

                                filterSB.append("(").append(rdns[0])
                                        .append(")");
                            }

                            if (attr != null && !attr.isEmpty()) {
                                Iterator itr = attr.iterator();

                                while (itr.hasNext()) {
                                    filterSB.append("(").append(attrName);
                                    filterSB.append("=").append(
                                            (String) itr.next());
                                    filterSB.append(")");
                                }
                            } // if
                        }
                    }
                    if (filterSB.length() > 2) {
                        if (attrExists) {
                            // pre-pend the creation filter part to the filter
                            // This is being done so that the filter is
                            // correctly created as
                            // (&(<creation-filter)(|(<attr filter>)))
                            newEntrySB.append(filterSB.toString()).append("))");
                            filterSB = newEntrySB;
                        } else {
                            filterSB.append(")");
                        }

                        if (debug.messageEnabled()) {
                            debug.message("AMObjectImpl." 
                                    + "validateAttributeUniqueness: "
                                    + "filter = " + filterSB.toString());
                        }

                        Set users = amobj.search(AMConstants.SCOPE_SUB,
                                filterSB.toString());
                        // Check if the entry that is "violating" uniqueness is
                        // the same as the one you are checking for.
                        // In that case,ignore the violation
                        if (users != null && users.size() == 1) {
                            String dn = (String) users.iterator().next();
                            DN dnObject = new DN(dn);
                            if (dnObject.equals(new DN(entryDN))) {
                                return;
                            }
                        }
                        if ((users != null) && !users.isEmpty()) {
                            throw new AMException(AMSDKBundle.getString("162"),
                                    "162");
                        }
                    }
                }

                orgDN = rdns[i] + "," + orgDN;
                thisDN = new DN(orgDN);
            }
        } catch (SSOException ex) {
            if (debug.warningEnabled()) {
                debug.warning("Unable to validate attribute uniqneness", ex);
            }
        }
    }

    /**
     * Private method to convert a comma separated string of attribute names to
     * an Array
     * 
     * @param newEntry
     * @throws AMException
     */
    private String[] getAttrList(String attrNames) {
        StringTokenizer tzer = new StringTokenizer(attrNames, ",");
        int size = tzer.countTokens();
        String[] attrList = new String[size];
        for (int i = 0; i < size; i++) {
            String tmps = (String) tzer.nextToken();
            attrList[i] = tmps.trim();
        }
        return attrList;
    }

    /**
     * Private method to get a set of values for all attributes that exist in
     * the stringModMap of this object, from a given list of attribute names
     * 
     * @param newEntry
     * @throws AMException
     */
    private Set getAttrValues(String[] attrList, Map modMap) {
        Set retSet = new HashSet();
        int size = attrList.length;
        for (int i = 0; i < size; i++) {
            Set tmpSet = (Set) modMap.get((String) attrList[i]);
            if (tmpSet != null && !tmpSet.isEmpty()) {
                retSet.addAll(tmpSet);
            }
        }
        return (retSet);
    }

    public Set getTopLevelContainers(SSOToken token) throws AMException,
            SSOException {

        String userDN = token.getPrincipal().getName();
        AMStoreConnection amsc = new AMStoreConnection(internalToken);
        AMUser auser = amsc.getUser(userDN);
        Set set = new HashSet();

        Set roleDNs = auser.getRoleDNs();
        roleDNs.addAll(auser.getFilteredRoleDNs());
        Iterator iter = roleDNs.iterator();

        while (iter.hasNext()) {
            String roleDN = (String) iter.next();

            if (debug.messageEnabled()) {
                debug.message("DirectoryServicesImpl."
                        + "getTopLevelContainers: roleDN=" + roleDN);
            }

            AMRole role = amsc.getRole(roleDN);
            set.addAll(role.getAttribute(ROLE_MANAGED_CONTAINER_DN_ATTRIBUTE));
        }

        if (set.isEmpty()) {
            String filter = "(|"
                    + SearchFilterManager
                            .getGlobalSearchFilter(AMObject.ORGANIZATION)
                    + SearchFilterManager
                            .getGlobalSearchFilter(AMObject.ORGANIZATIONAL_UNIT)
                    + SearchFilterManager
                            .getGlobalSearchFilter(AMObject.PEOPLE_CONTAINER)
                    + SearchFilterManager
                            .getGlobalSearchFilter(AMObject.DYNAMIC_GROUP)
                    + SearchFilterManager
                            .getGlobalSearchFilter(
                                    AMObject.ASSIGNABLE_DYNAMIC_GROUP)
                    + SearchFilterManager.getGlobalSearchFilter(AMObject.GROUP)
                    + ")";

            set = search(token, AMStoreConnection.getAMSdkBaseDN(), filter, 
                    SCOPE_SUB);
        }

        HashSet resultSet = new HashSet();
        iter = set.iterator();

        while (iter.hasNext()) {
            String containerDN = (String) iter.next();
            DN cDN = new DN(containerDN);
            Iterator iter2 = resultSet.iterator();
            HashSet tmpSet = new HashSet();
            boolean toAdd = true;

            while (iter2.hasNext()) {
                String resultDN = (String) iter2.next();
                DN rDN = new DN(resultDN);

                if (cDN.isDescendantOf(rDN)) {
                    toAdd = false;
                    tmpSet.add(resultDN);

                    break;
                } else if (!rDN.isDescendantOf(cDN)) {
                    tmpSet.add(resultDN);
                }
            }

            if (toAdd) {
                tmpSet.add(containerDN);
            }

            resultSet = tmpSet;
        }

        if (debug.messageEnabled()) {
            debug.message("DirectoryServicesImpl.getTopLevelContainers");
            iter = resultSet.iterator();

            StringBuilder tmpBuffer = new StringBuilder();

            while (iter.hasNext()) {
                String tmpDN = (String) iter.next();
                tmpBuffer.append(tmpDN).append("\n");
            }

            debug.message("containerDNs\n" + tmpBuffer.toString());
        }

        return resultSet;
    }

    /**
     * Gets the Organization DN for the specified entryDN. If the entry itself
     * is an org, then same DN is returned.
     * 
     * @param token
     *            a valid SSOToken
     * @param entryDN
     *            the entry whose parent Organization is to be obtained
     * @param childDN
     *            the immediate entry whose parent Organization is to be
     *            obtained
     * @return the DN String of the parent Organization
     * @throws AMException
     *             if an error occured while obtaining the parent Organization
     */
    public String verifyAndGetOrgDN(SSOToken token, String entryDN,
            String childDN) throws AMException {
        if (entryDN.length() == 0 || !DN.isDN(entryDN)) {
            debug.error("DirectoryServicesImpl.verifyAndGetOrgDN() Invalid "
                    + "DN: " + entryDN);
            throw new AMException(token, "157");
        }

        String organizationDN = null;
        boolean errorCondition = false;
        try {
            PersistentObject po = UMSObject.getObjectHandle(internalToken,
                    new Guid(childDN));

            String searchFilter = getOrgSearchFilter(entryDN);
            SearchResults result = po.search(searchFilter, aName, scontrol);

            if (result.hasMoreElements()) { // found the Organization
                // This loop/iteration of the searchresult is to avoid
                // forceful abandon and to avoid multiple
                // ABANDON logged in directory server access logs.
                while (result.hasMoreElements()) {
                    result.next();
                }
                organizationDN = po.getGuid().toString().toLowerCase();
            }
        } catch (InvalidSearchFilterException e) {
            errorCondition = true;
            debug.error("DirectoryServicesImpl.verifyAndGetOrgDN(): Invalid "
                    + "search filter, unable to get Parent Organization: ", e);
        } catch (UMSException ue) {
            errorCondition = true;
            if (debug.warningEnabled()) {
                debug.warning("DirectoryServicesImpl.verifyAndGetOrgDN(): "
                        + "Unable to Obtain Parent Organization", ue);
            }
            LDAPException lex = (LDAPException) ue.getRootCause();
            int errorCode = lex.getLDAPResultCode();
            if (retryErrorCodes.contains("" + errorCode)) {
                throw new AMException(token, Integer.toString(errorCode), ue);
            }
        }

        if (errorCondition) {
            String locale = CommonUtils.getUserLocale(token);
            throw new AMException(AMSDKBundle.getString("124", locale), "124");
        }
        return organizationDN;
    }

    // Registering for notification
    public void addListener(SSOToken token, AMObjectListener listener,
            Map configMap) throws AMEventManagerException {
        // Validate SSOToken
        try {
            SSOTokenManager.getInstance().validateToken(token);
        } catch (SSOException ssoe) {
            throw (new AMEventManagerException(ssoe.getMessage(), "902"));
        }

        // Add to listeners
        synchronized (listeners) {
            listeners.put(listener, configMap);
            // Check if event service has been started
            if (eventManager == null) {
                eventManager = new EventManager();
                eventManager.addListeners(listeners);
            }
        }
    }
}
