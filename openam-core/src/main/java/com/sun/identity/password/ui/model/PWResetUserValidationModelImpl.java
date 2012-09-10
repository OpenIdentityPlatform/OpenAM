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
 * $Id: PWResetUserValidationModelImpl.java,v 1.3 2010/01/28 08:17:10 bina Exp $
 *
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */

package com.sun.identity.password.ui.model;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <code>PWResetUserValidationModelImpl</code> defines a set of methods that
 * are required by password reset user validation viewbean.
 */
public class PWResetUserValidationModelImpl extends PWResetModelImpl
    implements PWResetUserValidationModel {
    
    /**
     * Name of password reset user validation attribute
     */
    private static final String PW_RESET_USER_VALIDATE_ATTR =
        "iplanet-am-password-reset-userValidate";
    
    /**
     * Name of password reset base DN attribute
     */
    private static final String PW_RESET_BASE_DN_ATTR =
        "iplanet-am-password-reset-baseDN";
    
    /**
     * Name of password reset user search filter
     */
    private static final String PW_RESET_SEARCH_FILTER_ATTR =
        "iplanet-am-password-reset-searchFilter";
    
    /**
     * Name of user service login status attribute
     */
    private static final String USER_SERVICE_LOGIN_STATUS =
        "iplanet-am-user-login-status";
    
    /**
     * Name of user service account life attribute
     */
    private static final String USER_SERVICE_ACCOUNT_LIFE =
        "iplanet-am-user-account-life";
    
    /**
     * Name of user service ns account lockout attribute
     */
    private static final String USER_SERVICE_NS_LOCKOUT =  "nsaccountlock";
    
    /**
     *  Name for string false value
     */
    public static final String STRING_FALSE = "false";
    
    private boolean realmFlag = false;
    private boolean validRealm = false;
    private String userRealm = null;
    
    /**
     * Constructs a password reset user validation model object
     *
     */
    public PWResetUserValidationModelImpl() {
        super();
    }
    
    /**
     * Returns user attribute configured in password reset service.
     *
     * @param orgDN Realm name.
     * @return user attribute configured in password reset service.
     */
    public String getUserAttr(String orgDN) {
        try {
            return getAttributeValue(orgDN, PW_RESET_USER_VALIDATE_ATTR);
        } catch (SSOException e) {
            debug.warning("PWResetUserValidationModelImpl.getUserAttr", e);
        } catch (SMSException e) {
            debug.error("PWResetUserValidationModelImpl.getUserAttr", e);
        }
        return null;
    }
    
    /**
     * Returns <code>true</code> if the user exists. If more than one users is
     * found then it will return false and view bean will display an error
     * message.
     *
     * @param userAttrValue User attribute value to search for.
     * @param userAttrName User attribute name to search for.
     * @param realm Base realm
     * @return <code>true</code> if user exists.
     */
    public boolean isUserExists(
        String userAttrValue,
        String userAttrName,
        String realm
        ) {
        boolean found = false;
        try {
            AMIdentityRepository amir = new AMIdentityRepository(
                ssoToken, realm);
            
            Map searchMap = new HashMap(2);
            Set searchSet = new HashSet(2);
            searchSet.add(userAttrValue);
            searchMap.put(userAttrName, searchSet);
            
            IdSearchControl isCtl = new IdSearchControl();
            isCtl.setSearchModifiers(IdSearchOpModifier.AND, searchMap);
            IdSearchResults isr = amir.searchIdentities(
                IdType.USER, "*", isCtl);
            Set results = isr.getSearchResults();
            
            if ((results != null) && !results.isEmpty()) {
                if (results.size() > 1) {
                    errorMsg = getLocalizedString(
                        "multipleUsersExists.message");
                } else {
                    AMIdentity amid = (AMIdentity)results.iterator().next();
                    userRealm = amid.getRealm();
                    userId = amid.getUniversalId();
                    found = true;
                }
            } else {
                errorMsg = getLocalizedString("userNotExists.message");
                writeLog("logUserNotExists.message", userAttrName);
            }
        } catch (SSOException e) {
            debug.error("PWResetUserValidationModelImpl.isUserExists", e);
            errorMsg = getErrorString(e);
        } catch (IdRepoException e) {
            debug.error("PWResetUserValidationModelImpl.isUserExists", e);
            errorMsg = getErrorString(e);
        }
        return found;
    }
    
    /**
     * Gets user validation title
     *
     * @return user validation title
     */
    public String getUserValidateTitleString() {
        return getLocalizedString("userValidate.title");
    }
    
    /**
     * Gets next button label
     *
     * @return next button label
     */
    public String getNextBtnLabel() {
        return getLocalizedString("next.button");
    }
    
    /**
     * Gets the base DN stored in password reset service. For the
     * case when the template does not exists for the organization DN passed,
     * it will set the base DN to organization DN if its from URL,
     * otherwise it will set base DN to default which is root.
     *
     * @param realm Realm name.
     * @return base DN
     */
    private String getBaseDN(String realm) {
        String baseDN = null;
        try {
            baseDN = getOrgAttributeValue(realm, PW_RESET_BASE_DN_ATTR);
        } catch (SSOException e) {
            debug.warning("PWResetUserValidationModelImpl.getBaseDN", e);
        } catch (SMSException e) {
            debug.error("PWResetUserValidationModelImpl.getBaseDN", e);
        }
        
        if (baseDN == null || baseDN.length() == 0) {
            if (realmFlag) {
                baseDN = realm;
            } else {
                try {
                    Set set = getDefaultAttrValues(getPWResetServiceSchema(),
                        PW_RESET_BASE_DN_ATTR);
                    baseDN = getFirstElement(set);
                } catch (SSOException e) {
                    debug.warning("PWResetUserValidationModelImpl.getBaseDN",e);
                } catch (SMSException e) {
                    debug.error("PWResetUserValidationModelImpl.getBaseDN",e);
                }
                if (baseDN == null || baseDN.length() == 0) {
                    baseDN = getRootSuffix();
                }
            }
        }
        return baseDN;
    }
    
    private String getOrgAttributeValue(String realm, String attrName)
    throws SSOException, SMSException {
        OrganizationConfigManager mgr = new OrganizationConfigManager(
            ssoToken, realm);
        Map attrValues = mgr.getAttributes(PW_RESET_SERVICE);
        Set values = (Set)attrValues.get(attrName);
        return ((values != null) && !values.isEmpty()) ?
            (String)values.iterator().next() : "";
    }
    
    /**
     * Sets the organization DN flag
     *
     * @param value organization flag value either true or false
     */
    public void setRealmFlag(boolean value) {
        realmFlag = value;
    }
    
    /**
     * Returns <code>true</code> if the realm is valid.
     *
     * @return <code>true</code> if the realm is valid.
     */
    public boolean isValidRealm() {
        return validRealm;
    }
    
    /**
     * Returns the localized string for attribute name in the user
     * service.
     *
     * @param attrName attribute name
     * @return localized string for the attribute
     */
    public String getLocalizedStrForAttr(String attrName) {
        String str = attrName;
        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                USER_SERVICE, ssoToken);
            if (mgr != null) {
                ServiceSchema schema = mgr.getSchema(SchemaType.USER);
                if (schema != null) {
                    AttributeSchema attrSchema =
                        schema.getAttributeSchema(attrName);
                    if (attrSchema != null) {
                        String key = attrSchema.getI18NKey();
                        str = getL10NAttributeName(mgr, key);
                    }
                }
            }
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("Could not get localized string for attribute " +
                    attrName, ssoe);
            }
        } catch (SMSException smse ){
            if (debug.warningEnabled()) {
                debug.warning("Could not get localized string for attribute " +
                    attrName, smse);
            }
        }
        return str;
    }
    
    /**
     * Returns missing user attribute message.
     *
     * @param userAttrName user attribute name.
     * @return missing user attribute message.
     */
    public String getMissingUserAttrMessage(String userAttrName) {
        errorMsg = getLocalizedString("missingUserAttr.message");
        return errorMsg;
    }
    
    /**
     * Returns true if the user is active and account is not expired.
     * This method will use the user DN stored in the model to
     * determine if the user's account is active or has expired.
     *
     * @param realm organization DN
     * @return true if user is active and account is not expired.
     */
    public boolean isUserActive(String realm) {
        boolean active = false;
        try {
            AMIdentity user= IdUtils.getIdentity(ssoToken, userId);
            boolean isUserActive = user.isActive();
            String loginStatus = getUserAttributeValue(
                user,  USER_SERVICE_LOGIN_STATUS, ACTIVE);
            String lockout = getUserAttributeValue(
                user, USER_SERVICE_NS_LOCKOUT, STRING_FALSE);
            
            if (!isUserActive ||
                !loginStatus.equalsIgnoreCase(ACTIVE) ||
                !lockout.equalsIgnoreCase(STRING_FALSE)) {
                errorMsg = getLocalizedString("userNotActive.message");
                writeLog("accountInactive.message", userId);
            } else if (isAccountExpired(user)) {
                errorMsg = getLocalizedString("userAccountExpired.message");
                writeLog("accountExpired.message", userId);
            } else if (isUserLockout(userId, realm)) {
                String obj[] = { userId };
                informationMsg = getLocalizedString("lockoutMsg.message");
                writeLog("accountLocked.message", userId);
            } else {
                active = true;
            }
        } catch (SSOException e) {
            debug.warning("PWResetUserValidationModelImpl.isUserActive", e);
            errorMsg = getErrorString(e);
        } catch (IdRepoException e) {
            debug.warning("PWResetUserValidationModelImpl.isUserActive", e);
            errorMsg = getErrorString(e);
        }
        return active;
    }
    
    /**
     * Gets user attribute value for a given user object and attribute name.
     * It will set attribute value to a default value if the value is null.
     *
     * @param user <code>AMIdentity</code> object
     * @param attrName attribute name
     * @param defaultValue default value for a attribute
     * @return attribute value
     * @throws SSOExcepion
     * @throws AMException
     */
    private String getUserAttributeValue(
        AMIdentity user,
        String attrName,
        String defaultValue
        ) throws SSOException, IdRepoException {
        String value = getUserAttributeValue(user, attrName);
        return ((value == null) || (value.length() == 0)) ?
            defaultValue : value;
    }
    
    
    private boolean isAccountExpired(
        AMIdentity user)
        throws SSOException, IdRepoException {
        boolean expired = false;
        String accountLife = getUserAttributeValue(
            user, USER_SERVICE_ACCOUNT_LIFE);
        if ((accountLife != null) && (accountLife.length() > 0)) {
            Date expDate = Locale.parseNormalizedDateString(accountLife);
            if (expDate != null) {
                expired = expDate.before(new Date());
            }
        }
        return expired;
    }
    
    private String getUserAttributeValue(AMIdentity user, String attrName)
    throws SSOException, IdRepoException {
        Set set = user.getAttribute(attrName);
        return getFirstElement(set);
    }
    
    /**
     * Returns realm name. If the given realm name is null or blank.
     * then root realm will be returned.
     *
     * @param realm Realm name.
     * @return Realm Name
     * @throws PWResetException if unable to get realm or realm does not exists
     */
    public String getRealm(String realm) throws PWResetException {
        String classMethod = "PWResetUserValidationModelImpl:getRealm: ";
	String orgName = realm;
        if ((realm != null) && (realm.length() > 0)) {
            try {
		orgName = IdUtils.getOrganization(ssoToken,realm);
                if (debug.messageEnabled()) {
                    debug.message(classMethod +"realm is :" + orgName);
                }
            } catch (Exception e) {
                debug.warning("PWResetUserValidationModelImpl.getRealm", e);
                errorMsg = getErrorString(e);
                throw new PWResetException(errorMsg);
            }
        } else {
            realm = "/";
        }
        setValidRealm(orgName);
        return orgName;
    }
    
    /**
     * Sets the valid realm flag.
     *
     * @param realm Realm Name.
     */
    public void setValidRealm(String realm) {
        validRealm = true;
        readPWResetProfile(realm);
        localeContext.setOrgLocale(realm);
    }
    
    /**
     * Realm for the user resetting password.
     *
     * @return the realm for the user resetting password.
     */
    public String getUserRealm() {
        return userRealm;
    }
}
