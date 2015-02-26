/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [2012] [Forgerock AS]"
 */

package org.forgerock.openam.authentication.modules.passphrase.security.login.module;

import java.security.AccessController;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.naming.Context;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.apache.commons.lang.StringUtils;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import org.forgerock.openam.authentication.modules.passphrase.common.utility.CommonUtilities;
import org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants;
import org.forgerock.openam.authentication.modules.passphrase.exception.MandatoryAttributeException;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.authentication.modules.ldap.LDAPAuthUtils;
import com.sun.identity.authentication.modules.ldap.LDAPPrincipal;
import com.sun.identity.authentication.modules.ldap.LDAPUtilException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.spi.UserNamePasswordValidationException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.BackwardCompSupport;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdServices;
import com.sun.identity.idm.IdServicesFactory;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.opends.server.protocols.ldap.LDAPResultCode;
//import org.forgerock.opendj.ldap.LDAPResultCode;
import org.forgerock.opendj.ldap.ResultCode;
//import org.opends.server.types.SearchScope;
import org.forgerock.opendj.ldap.SearchScope;
//import org.opends.server.protocols.ldap.

/**
 * The Authentication module authenticates the user against the Enterprise Directory server
 * and on successful authentication user will be logged-in to the portal else user will be authenticated against Active Directory,
 * and migrates the users, groups to the Enterprise Directory Server.
 *  
 * @author Satheesh M, Sendhil R, Saravanan V
 */
@SuppressWarnings("unchecked")
public class AuthenticationModule extends AMLoginModule {

	private static final String moduleName = "AuthenticationModule";
	private static Debug debug = Debug.getInstance(moduleName);

	private static final String INVALID_CHARS = "iplanet-am-auth-ldap-invalid-chars";

	private LDAPAuthUtils ldapUtil;
	private static final String bundleName = "amAuthLDAP";
	private ResourceBundle bundle = null;
	private String validatedUserID;
	private String userName;
	private String userPassword;
	private String regEx;
	private String bindDN;
	private int currentState;
	private boolean isProfileCreationEnabled;
	private boolean getCredentialsFromSharedState;
	
	private Set userCreationAttrs = new HashSet();
	private Map sharedState;
	private String serverHost;
	private int serverPort;
	private Map currentConfig;

	private Principal userPrincipal;

	public void init(Subject subject, Map sharedState, Map options) {
		currentConfig = options;
		bundle = amCache.getResBundle(bundleName, getLoginLocale());
		this.sharedState = sharedState;
	}

	/**
	 * This method extracts the LDAP credentials and usage from the passed in context configuration
	 * of an authentication module instance. 
	 * 
	 * @param contextConfig
	 * @return
	 * @throws LDAPUtilException 
	 * @throws AuthLoginException
	 */
	public boolean initializeLDAP(Map contextConfig) throws LDAPUtilException {
		serverHost = null;

			if (contextConfig != null) {
				serverHost = CollectionHelper.getServerMapAttr(contextConfig, PassphraseConstants.SERVER_HOST);
				String baseDN = CollectionHelper.getMapAttr(contextConfig, PassphraseConstants.BASE_DN);

				bindDN = CollectionHelper.getMapAttr(contextConfig, PassphraseConstants.BIND_DN);
				String bindPassword = CollectionHelper.getMapAttr(contextConfig, PassphraseConstants.BIND_PWD);
				String userNamingAttr = CollectionHelper.getMapAttr(contextConfig, PassphraseConstants.USER_NAMING_ATTR);
				
				String searchFilter = CollectionHelper.getMapAttr(contextConfig, PassphraseConstants.SEARCH_FILTER, "");
				boolean ssl = Boolean.valueOf(CollectionHelper.getMapAttr(contextConfig, PassphraseConstants.SSL, "false")).booleanValue();
				String searchScope_ = CollectionHelper.getMapAttr(contextConfig, PassphraseConstants.SEARCH_SCOPE, "SUBTREE");

				String authLevel = "0";
				if (authLevel != null) {
					try {
						setAuthLevel(Integer.parseInt(authLevel));
					} catch (NumberFormatException e) {
						debug.error("Unable to set auth level " + authLevel);
					}
				}
                                SearchScope s = SearchScope.WHOLE_SUBTREE;
				//int searchScope = 2;
				if (searchScope_.equalsIgnoreCase(PassphraseConstants.OBJECT)) {
					//searchScope = 0;
                                        s = SearchScope.BASE_OBJECT;
				} else if (searchScope_.equalsIgnoreCase(PassphraseConstants.ONELEVEL)) {
					//searchScope = 1;
                                        s = SearchScope.SINGLE_LEVEL;
				}

				String returnUserDN = CollectionHelper.getMapAttr(currentConfig, ISAuthConstants.LDAP_RETURNUSERDN, "true");
				regEx = CollectionHelper.getMapAttr(contextConfig, INVALID_CHARS, "*|(|)|&|!");

				// set LDAP Parameters
				int index = serverHost.indexOf(':');
				serverPort = PassphraseConstants.SERVER_PORT;

				if (index != -1) {
					serverPort = Integer.parseInt(serverHost.substring(index + 1));
					serverHost = serverHost.substring(0, index);
				}

				isProfileCreationEnabled = isDynamicProfileCreationEnabled();
				// set the optional attributes here
				ldapUtil = new LDAPAuthUtils(serverHost, serverPort, ssl, bundle, baseDN, debug);
				//SearchScope s;
                                //s = s.valueOf(searchScope);
                                ldapUtil.setScope(s);
				ldapUtil.setFilter(searchFilter);
				ldapUtil.setUserNamingAttribute(userNamingAttr);
				
				Set<String> userSearchAttrs = new HashSet<String>(2);
				userSearchAttrs.add(userNamingAttr);
				ldapUtil.setUserSearchAttribute(userSearchAttrs);
				ldapUtil.setAuthPassword(bindPassword);
				ldapUtil.setAuthDN(bindDN);
				ldapUtil.setReturnUserDN(returnUserDN);
				ldapUtil.setUserAttributes(userCreationAttrs);
				ldapUtil.setDynamicProfileCreationEnabled(isProfileCreationEnabled);
				return true;
			}
		return false;
	}

	/**
	 * This is the main method which authenticates the user against ED External and ED Internal & initiates the migration
	 * for an external user and group synchronisation.
	 */
	public int process(Callback[] callbacks, int state) throws AuthLoginException {
		currentState = state;
			
		// Start of login process. User enters his/her user name and password
		// if there are no exceptions, we store the user name and password within
		// a sharedstate object (essentially like a http session) for use later
		if (currentState == ISAuthConstants.LOGIN_START) {
			if (callbacks != null && callbacks.length == 0) {
				userName = (String) sharedState.get(ISAuthConstants.SHARED_STATE_USERNAME);
				userPassword = (String) sharedState.get(ISAuthConstants.SHARED_STATE_PASSWORD);
				if (userName == null || userPassword == null) {
					return ISAuthConstants.LOGIN_START;
				}
				getCredentialsFromSharedState = true;
			} else {
				userName = ((NameCallback) callbacks[0]).getName();
				userPassword = charToString(((PasswordCallback) callbacks[1]).getPassword(), callbacks[1]);
			}
			if (userPassword == null || userPassword.length() == 0) {
				throw new InvalidPasswordException(bundleName, "InvalidUP", null);
			}
			storeUsernamePasswd(userName, userPassword);

			
			// Trying to authenticate the user
			try {
				//Authentication Against ED_External
				authenticateModule(PassphraseConstants.ED_EXTERNAL_INSTANCE_NAME);
				
				AMIdentity user = CommonUtilities.getUser(userName, CommonUtilities.getProperty(PassphraseConstants.ED_EXTERNAL_REALM));
				String status = CollectionHelper.getMapAttr(user.getAttributes(), "inetUserStatus");
				if (status != null && status.equalsIgnoreCase("Inactive")) {
					debug.message("User Account is inactive for the user: " + userName);
					replaceHeader(PassphraseConstants.AUTH_MODULE_ERROR_STATE, "This user is not active. Contact your system administrator.");
					currentState = PassphraseConstants.AUTH_MODULE_ERROR_STATE;
					return currentState;
				}
				
				// User successfully authenticated against Internal ED
				setForceCallbacksRead(false);
				return currentState;
				
			}catch (AMConfigurationException amce) {
				debug.error("Configuration of authentication module seems incorrect : ",amce);
				replaceHeader(PassphraseConstants.AUTH_MODULE_ERROR_STATE, "Internal system error. Please contact the Service Desk.");
				currentState = PassphraseConstants.AUTH_MODULE_ERROR_STATE;
				return currentState;
			}catch (UserNamePasswordValidationException unpve){
				debug.error("User name or password contains invalid characters or validateUserName has thrown this exception : ",unpve);
				replaceHeader(PassphraseConstants.AUTH_MODULE_ERROR_STATE, "User name or password contains invalid characters. Please check the details provided.");
				currentState = PassphraseConstants.AUTH_MODULE_ERROR_STATE;
				return currentState;
			}catch (LDAPUtilException ex){
				
				if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
					getCredentialsFromSharedState = false;
					return ISAuthConstants.LOGIN_START;
				}
				setFailureID((ldapUtil != null) ? ldapUtil.getUserId(userName) : userName);
				int temp = (ex.getResultCode()).intValue();
				switch (temp) {
				case LDAPResultCode.NO_SUCH_OBJECT:
					//authenticate against Internal ED
					try{
						authenticateModule(PassphraseConstants.ED_INTERNAL_INSTANCE_NAME);
						
						AMIdentity user = CommonUtilities.getUser(userName, CommonUtilities.getProperty(PassphraseConstants.ED_INTERNAL_REALM));
						String status = CollectionHelper.getMapAttr(user.getAttributes(), "inetUserStatus");
						if (status != null && status.equalsIgnoreCase("Inactive")) {
							debug.message("User Account is inactive for the user: " + userName);
							replaceHeader(PassphraseConstants.AUTH_MODULE_ERROR_STATE, "This user is not active. Contact your system administrator.");
							currentState = PassphraseConstants.AUTH_MODULE_ERROR_STATE;
							return currentState;
						}
						
						//User successfully authenticated against External ED
						setForceCallbacksRead(false);
						return currentState;
						
					}catch (AMConfigurationException amce1) {
						debug.error("Configuration of authentication module seems incorrect : ",amce1);
						replaceHeader(PassphraseConstants.AUTH_MODULE_ERROR_STATE, "Internal system error. Please contact the Service Desk.");
						currentState = PassphraseConstants.AUTH_MODULE_ERROR_STATE;
						return currentState;
					}catch (UserNamePasswordValidationException unpve1){
						debug.error("User name or password contains invalid characters or validateUserName has thrown this exception : ",unpve1);
						replaceHeader(PassphraseConstants.AUTH_MODULE_ERROR_STATE, "User name or password contains invalid characters. Please check the details provided.");
						currentState = PassphraseConstants.AUTH_MODULE_ERROR_STATE;
						return currentState;
					}catch (LDAPUtilException ex1){
						
						if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
							getCredentialsFromSharedState = false;
							return ISAuthConstants.LOGIN_START;
						}
						setFailureID((ldapUtil != null) ? ldapUtil.getUserId(userName) : userName);
						int val = (ex1.getResultCode()).intValue();
						switch (val) {
						case LDAPResultCode.NO_SUCH_OBJECT:
							debug.message("User is not available in both Enterprise Directory Internal and External Realms",ex1);
							replaceHeader(PassphraseConstants.AUTH_MODULE_ERROR_STATE, "You've entered a wrong UserId.");
							currentState = PassphraseConstants.AUTH_MODULE_ERROR_STATE;
							return currentState;	
						case LDAPResultCode.INVALID_CREDENTIALS:
							debug.message("Invalid password provided for the user: " + userName);
							String failureUserID = ldapUtil.getUserId();
							throw new InvalidPasswordException(bundleName, "InvalidUP", null, failureUserID, null);
						case LDAPResultCode.UNWILLING_TO_PERFORM:
							debug.message("User Account is inactive for the user: " + userName);
							replaceHeader(PassphraseConstants.AUTH_MODULE_ERROR_STATE, "This user is not active. Contact your system administrator.");
							currentState = PassphraseConstants.AUTH_MODULE_ERROR_STATE;
							return currentState;
						case LDAPResultCode.INAPPROPRIATE_AUTHENTICATION:
							debug.message("Inappropriate authentication for the user: " + userName);
							throw new AuthLoginException(moduleName, "InappAuth", null);
						case LDAPResultCode.CONSTRAINT_VIOLATION:
							debug.message("Exceed password retry limit for the user: " + userName);
							throw new AuthLoginException(moduleName, ISAuthConstants.EXCEED_RETRY_LIMIT, null);
						default:
							debug.error("Error occured while authenticating the user: " + userName, ex1);
							throw new AuthLoginException(moduleName, "LDAP Exception", null);
						}
					} catch (SSOException ssoe1) {
						debug.error("An SSO Exception was thrown while accessing the user.getAttributes method.",ssoe1);
						replaceHeader(PassphraseConstants.AUTH_MODULE_ERROR_STATE, "Internal system error. Please contact the Service Desk.");
						currentState = PassphraseConstants.AUTH_MODULE_ERROR_STATE;
						return currentState;
					} catch (IdRepoException idre1) {
						debug.error("An SSO Exception was thrown while accessing the user.getAttributes method.",idre1);
						replaceHeader(PassphraseConstants.AUTH_MODULE_ERROR_STATE, "Internal system error. Please contact the Service Desk.");
						currentState = PassphraseConstants.AUTH_MODULE_ERROR_STATE;
						return currentState;
					}
					
				case LDAPResultCode.INVALID_CREDENTIALS:
					debug.message("Invalid password provided for the user: " + userName);
					String failureUserID = ldapUtil.getUserId();
					throw new InvalidPasswordException(bundleName, "InvalidUP", null, failureUserID, null);
				case LDAPResultCode.UNWILLING_TO_PERFORM:
					debug.message("User Account is inactive for the user: " + userName);
					replaceHeader(PassphraseConstants.AUTH_MODULE_ERROR_STATE, "This user is not active. Contact your system administrator.");
					currentState = PassphraseConstants.AUTH_MODULE_ERROR_STATE;
					return currentState;
				case LDAPResultCode.INAPPROPRIATE_AUTHENTICATION:
					debug.message("Inappropriate authentication for the user: " + userName);
					throw new AuthLoginException(moduleName, "InappAuth", null);
				case LDAPResultCode.CONSTRAINT_VIOLATION:
					debug.message("Exceed password retry limit for the user: " + userName);
					throw new AuthLoginException(moduleName, ISAuthConstants.EXCEED_RETRY_LIMIT, null);
				default:
					debug.error("Error occured while authenticating the user: " + userName, ex);
					throw new AuthLoginException(moduleName, "LDAP Exception", null);
				}

			} catch (SSOException ssoe) {
				debug.error("An SSO Exception was thrown while accessing the user.getAttributes method.",ssoe);
				replaceHeader(PassphraseConstants.AUTH_MODULE_ERROR_STATE, "Internal system error. Please contact the Service Desk.");
				currentState = PassphraseConstants.AUTH_MODULE_ERROR_STATE;
				return currentState;
			} catch (IdRepoException idre) {
				debug.error("An SSO Exception was thrown while accessing the user.getAttributes method.",idre);
				replaceHeader(PassphraseConstants.AUTH_MODULE_ERROR_STATE, "Internal system error. Please contact the Service Desk.");
				currentState = PassphraseConstants.AUTH_MODULE_ERROR_STATE;
				return currentState;
			}
		}
		
		currentState = PassphraseConstants.AUTH_MODULE_ERROR_STATE;
		return currentState;
	}

	/*private void setOrgDN(String realm) {
		CallbackHandler handler = getCallbackHandler();
        Callback[] callbacks = new Callback[1];
        try {
            callbacks[0] = new LoginStateCallback();
            handler.handle(callbacks);
            LoginState loginState = ((LoginStateCallback) callbacks[0]).getLoginState();
        } catch (Exception e) {
            debug.message("Error occured setting the org name: ", e);
        }
	}*/

	/**
	 * This method is used to authenticate the user against the passed in Authentication module name.
	 * 
	 * @param instanceName
	 * @param datastoreName
	 * @throws AMConfigurationException 
	 * @throws AuthLoginException 
	 * @throws LDAPUtilException 
	 * @throws Exception
	 */
	private void authenticateModule(String instanceName) throws AMConfigurationException, UserNamePasswordValidationException, LDAPUtilException {
		String edConfigName = CommonUtilities.getProperty(instanceName);
		String configName = AMAuthConfigUtils.getAuthConfigName(AuthContext.IndexType.MODULE_INSTANCE, edConfigName, DNMapper.orgNameToDN("/"), PassphraseConstants.HTML);

		AppConfigurationEntry[] entries = Configuration.getConfiguration().getAppConfigurationEntry(configName);
		if (entries == null || entries.length != 1)
			throw new AMConfigurationException("No Authentication Module " + instanceName + " exists with the name: " + edConfigName);
		Map edContextConfig = entries[0].getOptions();
		initializeLDAP(edContextConfig);
		
		validateUserName(userName, regEx);
		ldapUtil.authenticateUser(userName, userPassword);
		currentState = ISAuthConstants.LOGIN_SUCCEED;
		
		// The following code is required because sometimes the 
		// ldapUtil.authenticateUser method does not throw a proper
		// exception and still returns null. We test the user id and see if its null
		// and handle the exception ourselves
		validatedUserID = ldapUtil.getUserId();
		if (validatedUserID == null)
			throw new LDAPUtilException("noUserMatchFound", ResultCode.valueOf(LDAPResultCode.NO_SUCH_OBJECT), null);
	}

	/**
	 * This method creates a new user in the DataStore based on the create privilege granted to the
	 * specific DataStore.
	 * 
	 * @param userId
	 * @param attributes
	 * @param realm
	 * @return
	 * @throws Exception
	 */
	private AMIdentity createNewUser(String userId, Map attributes, String realm) throws Exception {
		attributes.put("inetuserstatus", getSetValue("Active"));
		attributes.put("userpassword", getSetValue(sharedState.get(ISAuthConstants.SHARED_STATE_PASSWORD).toString()));
		AMIdentity newUser = null;
		try {
			SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
			AMIdentityRepository repo = new AMIdentityRepository(token, CommonUtilities.getProperty(realm));
			
			// This is required to set entity name to naming attribute field in the creation view for user.
			BackwardCompSupport support = BackwardCompSupport.getInstance();
			support.beforeCreate("user", userId, attributes);
			
			newUser = repo.createIdentity(IdUtils.getType("user"), userId, attributes);
			debug.message("The External User '" + newUser.getName() + "' is created in ED successfully.");
			return newUser;
		} catch (Exception e) {
			debug.error("Error creating new user: " + userName, e);
			attributes.remove("userpassword");
			debug.error("Failed User entry: " + attributes);
			throw e;
		}
	}

	/**
	 * This method deletes the user entry from the passed in context config irrespective of the OpenSSO DataStore
	 * privilege configuration.
	 * 
	 * @param userDN
	 * @param contextConfig
	 * @throws Exception
	 */
	private void deleteUserEntry(String userDN, ServiceConfig datastoreConfig) throws Exception {
		LdapContext context = getLdapContext(datastoreConfig);
		context.destroySubcontext(userDN);
		context.close();
	}
	
	/**
	 * This method verifies if the authenticated user is an external user, creates the user in AD,
	 * checks and creates the associated user groups and associate it with the newly created group.
	 * It finally deletes the existing user entry from AD. 
	 * 
	 * @param userDN
	 */
	/*private void migrateUser(String userDN) throws Exception {
		AMIdentity user = CommonUtilities.getUser(userName, CommonUtilities.getProperty(PassphraseConstants.AD_REALM));
		userName = user.getName();
		String mailId = CollectionHelper.getMapAttr(user.getAttributes(), "mail", "").toLowerCase();
		
		boolean isADwritable = String.valueOf(CommonUtilities.getProperty(PassphraseConstants.AD_WRITE_CHANGES)).equalsIgnoreCase("true");
		boolean isInternal = mailId.endsWith(ConstantsID) || mailId.endsWith(PassphraseConstants.CLEARNET_ID);
		
		Set<AMIdentity> mappedGroups = user.getMemberships(IdType.GROUP);
		
		SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
		HashSet<String> userAttributesSet = getCommonUserAttributs(token, isInternal?PassphraseConstants.ED_INTERNAL_DATASTORE_NAME:PassphraseConstants.ED_EXTERNAL_DATASTORE_NAME);
		Map<String, Set<String>> userAttributes = user.getAttributes(userAttributesSet);
		
		// copy the mnemonic to few banking attributes
		int index = userName.indexOf('@');
		String mnemonic = index != -1? userName.substring(index + 1).toUpperCase():null;
		if (mnemonic != null) {
			String copyAttrNames = CommonUtilities.getProperty(PassphraseConstants.COPY_MNEMONIC_ATTRS);
			if (copyAttrNames != null) {
				String[] attrs = copyAttrNames.split(",");
				for (String attributeName : attrs) {
					Set<String> attr = userAttributes.get(attributeName);
					String existingValue = null;
					if (attr != null && !attr.isEmpty())
						existingValue = attr.iterator().next();
					if (StringUtils.isEmpty(existingValue))
						userAttributes.put(attributeName.trim(), getSetValue(mnemonic));
				}
			}
		}

		// remove few attributes which are data-store specific
		String ignoreAttrs = CommonUtilities.getProperty(PassphraseConstants.IGNORE_USER_ATTRS);
		if (ignoreAttrs != null) {
			String[] attrs = ignoreAttrs.split(",");
			for (String attribute : attrs) {
				userAttributes.remove(attribute);
			}
		}
		
		// do a cleanup to avoid the error LdapErr: DSID-0C090B38
		for (String attrName : userAttributes.keySet()) {
			if (userAttributes.get(attrName).isEmpty())
				userAttributes.remove(attrName);
		}
		
		//Mandatory Attributes validation (cn,givenName,sso-bankingrecid,sso-passphrase,sn) and corresponding values
		String mandatoryAttrs = CommonUtilities.getProperty(PassphraseConstants.MANDATORY_USER_ATTRS);
		boolean isPresent = true;
		if (mandatoryAttrs != null) {
			String[] attrs = mandatoryAttrs.split(",");
			for (String attribute : attrs) {
				isPresent = userAttributes.containsKey(attribute);
				if (!isPresent)
					throw new MandatoryAttributeException();							
			}
		}

		//To Ignore Country Object class error
		String countryAttr = "c";
		boolean cEnabled = userAttributes.containsKey(countryAttr);
		if (!cEnabled) {
			debug.message("Adding default country to user: " + userName + " to avoid Country Object class error");
			userAttributes.put(countryAttr, getSetValue(PassphraseConstants.COUNTRY));
		}
		
		//External User migration to ED
		String realm = isInternal? PassphraseConstants.ED_INTERNAL_REALM:PassphraseConstants.ED_EXTERNAL_REALM;
		createNewUser(userName, userAttributes, realm);
		
		//User Group migration to ED
		try {
			if (mappedGroups.size() > 0) {
				IdServices idServices = IdServicesFactory.getDataStoreServices();
				String orgName = DNMapper.orgNameToDN(CommonUtilities.getProperty(realm));
				for (AMIdentity group : mappedGroups) {
					// if the group exists in ED, there would be two distinguished names one pointing to group entry
					// in AD and the other one in ED.
					if (group.getAttribute("dn").size() < 2) {
						try {
							idServices.create(token, IdType.GROUP, group.getName(), Collections.EMPTY_MAP, orgName);
							debug.message("The External User group '" + group.getName() + "' is created in ED successfully.");
						} catch (Exception e) {
							debug.message("Error occured creating group: '" + group.getName() + "' in ED, assuming it already exists.");
						}
					}
					idServices.modifyMemberShip(token, IdType.GROUP, group.getName(), getSetValue(userName), IdType.USER, IdRepo.ADDMEMBER, orgName);
				}
			}
			
			//If AD has write permission delete the user from Active Directory and do user store.
			if (isADwritable) {
				try {
					deleteUserEntry(userDN, getDataStoreConfig(PassphraseConstants.AD_DATASTORE_NAME, PassphraseConstants.AD_REALM));
					debug.message("External User: " + userDN + ", is removed from AD successfully.");
				} catch (Exception e) {
					debug.error("Error occure while deleting the AD user: " + userDN, e);
				}
			}
			debug.message("The External user '" + userName + "' is migrated from AD datastore to ED successfully.");
		} catch (Exception e) {
			debug.error("Error occured in migrating the Groups for the external user: " + userName, e);
			debug.message("Rolling out the created user..");
			ServiceConfig config = isInternal? getDataStoreConfig(PassphraseConstants.ED_INTERNAL_DATASTORE_NAME, PassphraseConstants.ED_INTERNAL_REALM)
											 : getDataStoreConfig(PassphraseConstants.ED_EXTERNAL_DATASTORE_NAME, PassphraseConstants.ED_EXTERNAL_REALM);
			String ed_userDN = CommonUtilities.getMemberDn(userName, config.getAttributes());
			deleteUserEntry(ed_userDN, config);
			debug.message("The ED User Entry: " + ed_userDN + ", is rolled out successfully.");
			throw e;
		}
	}*/

	/**
	 * Utility method to wrap the passed string in to a Set. 
	 * 
	 * @param value
	 * @return
	 */
	private Set<String> getSetValue(String value) {
		Set<String> set = new HashSet<String>(1);
		if (value != null)
			set.add(value);
		return set;
	}

	/**
	 * Returns the principal.
	 * 
	 * @return principal.
	 */
	public Principal getPrincipal() {
		if (userPrincipal != null) {
			return userPrincipal;
		} else if (validatedUserID != null) {
			userPrincipal = new LDAPPrincipal(validatedUserID);
			return userPrincipal;
		} else {
			return null;
		}
	}

	/**
	 * Cleans up state fields.
	 */
	public void destroyModuleState() {
		validatedUserID = null;
		userPrincipal = null;
	}

	/**
	 * To free up the memory once used.
	 */
	public void nullifyUsedVars() {
		bundle = null;
		userName = null;
		userPassword = null;
		regEx = null;
		userCreationAttrs = null;
		sharedState = null;
		currentConfig = null;
	}

	/**
	 * Utility method to retrieve password from the pwd callback.
	 * 
	 * @param tmpPassword
	 * @param cbk
	 * @return
	 */
	private String charToString(char[] tmpPassword, Callback cbk) {
		if (tmpPassword == null)
			tmpPassword = new char[0];
		char[] pwd = new char[tmpPassword.length];
		System.arraycopy(tmpPassword, 0, pwd, 0, tmpPassword.length);
		((PasswordCallback) cbk).clearPassword();
		return new String(pwd);
	}

	/**
	 * This method retrieves the common set of user attributes available in common with AD and ED
	 * data stores.
	 * 
	 * @param token
	 * @param migrationDatastoreName 
	 * @return
	 * @throws SSOException
	 * @throws SMSException
	 */
	/*private HashSet<String> getCommonUserAttributs(SSOToken token, String migrationDatastoreName) throws SSOException, SMSException {
		ServiceConfigManager svcCfgMgr = new ServiceConfigManager(IdConstants.REPO_SERVICE, token);
		ServiceConfig cfg = svcCfgMgr.getOrganizationConfig("/", null);
		ServiceConfig adConfig = cfg.getSubConfig(CommonUtilities.getProperty(PassphraseConstants.AD_DATASTORE_NAME));
		ServiceConfig edConfig = cfg.getSubConfig(CommonUtilities.getProperty(migrationDatastoreName));
		
		HashSet<String> adAttributes = (HashSet<String>) adConfig.getAttributes().get("sun-idrepo-ldapv3-config-user-attributes");
		HashSet<String> edAttributes = (HashSet<String>) edConfig.getAttributes().get("sun-idrepo-ldapv3-config-user-attributes");
		
		edAttributes.retainAll(adAttributes);
		return edAttributes;
	}*/
	
	/**
	 * This method retrieves the data store configuration based on the passed in data store name.
	 * 
	 * @param dataStoreName
	 * @return
	 * @throws SSOException
	 * @throws SMSException
	 */
	private ServiceConfig getDataStoreConfig(String dataStoreName, String realm) throws SSOException, SMSException {
		SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
		ServiceConfigManager svcCfgMgr = new ServiceConfigManager(IdConstants.REPO_SERVICE, token);
		ServiceConfig cfg = svcCfgMgr.getOrganizationConfig(CommonUtilities.getProperty(realm), null);
		
		return cfg.getSubConfig(CommonUtilities.getProperty(dataStoreName));
	}	

	/**
	 * This method creates the LDAP context from the passed data store config info.
	 * 
	 * @param contextConfig
	 * @return
	 * @throws Exception
	 */
	public static LdapContext getLdapContext(ServiceConfig config) throws Exception {
		Map contextConfig = config.getAttributes();
		String adminName = CollectionHelper.getMapAttr(contextConfig, "sun-idrepo-ldapv3-config-authid");
		String adminPassword = CollectionHelper.getMapAttr(contextConfig, "sun-idrepo-ldapv3-config-authpw");
		boolean sslEnabled = Boolean.valueOf(CollectionHelper.getMapAttr(contextConfig, "sun-idrepo-ldapv3-config-ssl-enabled", "false")).booleanValue();
		String ldapUrl = CollectionHelper.getServerMapAttr(contextConfig, "sun-idrepo-ldapv3-config-ldap-server");		
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, adminName);
		env.put(Context.SECURITY_CREDENTIALS, adminPassword);
		env.put(Context.PROVIDER_URL, "ldap://" + ldapUrl);
		if (sslEnabled)
			env.put(Context.SECURITY_PROTOCOL, "ssl");

		return new InitialLdapContext(env, null);
	}
}