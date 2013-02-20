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

import static org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants.MODULE_ENTRY_POINT;
import static org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants.MODULE_PASSPHRASE_AUTHENTICATION;
import static org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants.MODULE_PASSPHRASE_ENTRY;
import static org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants.MODULE_PASSPHRASE_RESET;
import static org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants.MODULE_PASSWORD_RESET;
import static org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants.MODULE_PROFILE_UPDATED;
import static org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants.MODULE_RESET_ANSWERS_ENTRY;
import static org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants.MODULE_SUCCESS;

import java.security.AccessController;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import org.forgerock.openam.authentication.modules.passphrase.common.utility.CommonUtilities;
import org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants;
import org.forgerock.openam.authentication.modules.passphrase.security.login.principal.PassPhrasePrincipal;
import org.forgerock.openam.authentication.modules.passphrase.security.model.PasswordResetOptionsData;
import org.forgerock.openam.authentication.modules.passphrase.security.model.PasswordResetOptionsModel;
import org.forgerock.openam.authentication.modules.passphrase.security.passphrase.PassphraseGenerator;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.console.user.model.UMUserPasswordResetOptionsData;
import com.sun.identity.console.user.model.UMUserPasswordResetOptionsModel;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.EncryptAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
//import com.sun.mfwk.console.clientApi.InvalidArgumentException;
/**
 * This is a OpenSSO custom module class which does the following tasks in the order given below.
 * 		1. Checks if force password reset flag is enabled and forces the user to change the password.
 * 		2. Checks if passphrase is blank and present the user with passphrase entry page (first time user).
 * 		3. Does the second level validation of the user based on the inputs of three random position of the user's passphrase.
 * 		4. Present with a force change passphrase page if passphrase reset flag is enabled.
 * 		5. Validates if the user has provided answers for all the password/passphrase reset questions and forces to enter if not.
 * 
 * The modules are skipped in the flow if not applicable for the current scenario.
 * 
 * @author Satheesh M
 */
@SuppressWarnings("unchecked")
public class CustomModule extends AMLoginModule {
	
	private static Debug debug = Debug.getInstance("CustomModule");
	
	private Map sharedState;
	
	private Map options;
	
	private Principal userPrincipal;
	
	private String userName;

	private AMIdentity user;
	
	PassphraseGenerator lpg;

	/**
	 * Initialise the module.
	 * 
	 * @param subject
	 * @param sharedState
	 * @param options
	 */
	public void init(Subject subject, Map sharedState, Map options) {
		this.sharedState = sharedState;
		this.options = options;
		sharedState.put("PROFILE_UPDATED",false);
	}

	/**
	 * This method checks the current state and decides the validation to be performed
	 * corresponding to the state.
	 */
	public int process(Callback[] callbacks, int state) throws AuthLoginException {
		try {
			userName = (String) sharedState.get("javax.security.auth.login.name");
			HttpServletRequest request = getHttpServletRequest();
			
			if (userName == null) {
				debug.error("The sharedState userId is null, hence getting user from request.");
				debug.message("Session token id is: " + getSSOSession().getTokenID().toString());
				userName = CommonUtilities.getUserName(request);
				debug.message("UserName got from request is: " + userName);
			}
			
			if (userName == null) {
				debug.error("The sharedState userId is null..!!");
				throw new AuthLoginException("Unable to retrieve the user..!!");
			}
			if (user == null)
				user = CommonUtilities.getUser(userName, request);
				request.setAttribute("userName", userName);
			
			// quick fix to avoid the refresh problem
			String submitedState = getSubmitedState(callbacks);
			if (state == MODULE_PROFILE_UPDATED || (submitedState != null && !String.valueOf(state).equals(submitedState))) {
				if (state == MODULE_PASSPHRASE_AUTHENTICATION) {
		        	setRandomPositions();
		        }
				return state;
			}

	        int returnState = MODULE_SUCCESS;

	        switch (state) {
		        case MODULE_ENTRY_POINT:
		            if (isPasswordResetEnabled()) {
		            	returnState = MODULE_PASSWORD_RESET;
		            	sharedState.put("PROFILE_UPDATED", true);
		            } else if (isPassphraseNotEntered()) {
		            	returnState = MODULE_PASSPHRASE_ENTRY;
		            } else {
		            	returnState = MODULE_PASSPHRASE_AUTHENTICATION;
		            }
		            break;
		        case MODULE_PASSWORD_RESET:
		            returnState = savePassword(callbacks);
		            sharedState.put("PROFILE_UPDATED", true);
		            break;
		        case MODULE_PASSPHRASE_ENTRY:
		            returnState = savePassphrase(callbacks, state);
		            sharedState.put("PROFILE_UPDATED", true);
		            break;
		        case MODULE_PASSPHRASE_AUTHENTICATION:
		            returnState = authenticatePassphrase(callbacks);
		            break;
		        case MODULE_PASSPHRASE_RESET:
		        	returnState = savePassphrase(callbacks, state);
		        	sharedState.put("PROFILE_UPDATED", true);
		        	break;
		        case MODULE_RESET_ANSWERS_ENTRY:
		        	returnState = saveResetAnswers(callbacks);
		        	break;
		        default:
		            throw new AuthLoginException("Invalid state: " + state);
	        }

	        if (state != returnState && (returnState == MODULE_PASSPHRASE_ENTRY || returnState == MODULE_PASSPHRASE_RESET || returnState == MODULE_RESET_ANSWERS_ENTRY))
	        	replaceHeader(returnState, " ");
	        
	        if (returnState == MODULE_SUCCESS) {
	        	// retrieve all the attributes which is to be updated
	        	Map<String, Set> attrMap = (Map) sharedState.get("ATTR_MAP");
	        	Map<String, Set> passwordAttrMap = null;
	        	String pwdAttr = CommonUtilities.getProperty(PassphraseConstants.USER_PASSWORD_ATTRIBUTE);
	        	
	        	// workaround to fix the password update bug
	        	if (attrMap != null && attrMap.containsKey(pwdAttr)) {
	        		passwordAttrMap = new HashMap<String, Set>();
	        		passwordAttrMap.put(pwdAttr, attrMap.get(pwdAttr));
	        		attrMap.remove(pwdAttr);
	        	}
	        	
	        	if (passwordAttrMap != null) {
	    			try {
						updatePassword(passwordAttrMap.values().iterator().next());
						debug.message("Password is changed for the user: " + user.getName());
					} catch (InvalidAttributeValueException e) {
						debug.error("Error updating the password for the user: " + user.getName() + ", Error msg: " + e.getMessage());
						
						// if wrong old password is entered
						if (e.getMessage().contains("error code 19 - 00000056"))
							throw new InvalidPasswordException("Invalid Password", userName);
						// if password available in recent password history
						else if (!e.getMessage().contains("error code 19 - password in history"))
							debug.error("", e);
							
		        		sharedState.put("MODULE_PROCESSED", true);
						replaceHeader(MODULE_PASSWORD_RESET, "Password change Failed. This is because you provided a password which was used recently.");
			        	return MODULE_PASSWORD_RESET;
			        }
	        	}
	        	
	    		if (attrMap != null && attrMap.size() > 0) {
	    			user.setAttributes(attrMap);
	    			user.store();
	    			sharedState.put("ATTR_MAP", null);
	    			debug.message("Updated the attributes " + attrMap.keySet() + " for the user: " + user.getName());
	    		}
	        	
	    		if ((Boolean) sharedState.get("PROFILE_UPDATED") == true) {
	    			return MODULE_PROFILE_UPDATED;
	    		}
	    		
	    		String successURL = CollectionHelper.getMapAttr(user.getAttributes(), "iplanet-am-user-success-url");
	    		if (StringUtils.isNotBlank(successURL))
	    			setLoginSuccessURL(successURL);
	    		
	        } else if (returnState == MODULE_PASSPHRASE_AUTHENTICATION) {
	        	setRandomPositions();
	        }
	        
	        return returnState;
		} catch (Exception e) {
			if (e instanceof InvalidPasswordException)
				throw (InvalidPasswordException) e;
			else {
				debug.error("Error occured while processing custom modules for the user: " + user.getName(), e);
				throw new AuthLoginException(e);
			}
		}
	}

	/**
	 * This method does a direct LDAP password change rather than through OpenSSO so as to
	 * have the password policies work without any issues. 
	 * 
	 * @param passwordSet
	 * @throws Exception
	 */
	private void updatePassword(Set passwordSet) throws Exception {
		String password = (String) passwordSet.iterator().next();
		
//		String mailId = CollectionHelper.getMapAttr(user.getAttributes(), "mail", "").toLowerCase();		
//		boolean isInternalUser = mailId.endsWith(PassphraseConstants.ID) || mailId.endsWith(PassphraseConstants.CLEARNET_ID);
		
		LdapContext ctx = CommonUtilities.getLdapContext(CommonUtilities.getContextConfig(user));
		String userdn = CommonUtilities.getUserDN(user);
		
		ModificationItem[] mods = new ModificationItem[1];
//		if (isInternalUser) {
//			// AD does not allow the user password to be changed if the password last updated was less than
//			// 3 days. This is a workaround to bypass it. 
//			mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("pwdLastSet", "0"));
//			ctx.modifyAttributes(userdn, mods);
//		}
		try {
			String pwdAttributeName = CommonUtilities.getProperty(PassphraseConstants.ED_USER_PASSWORD_ATTRIBUTE);
//			if (isInternalUser) {
//				// Password must be both unicode and a quoted string for AD
//				String newQuotedPassword = "\"" + password + "\"";
//				String oldQuotedPassword = "\"" + sharedState.get("javax.security.auth.login.password") + "\"";
//				byte[] newUnicodePassword = newQuotedPassword.getBytes("UTF-16LE");
//				byte[] oldUnicodePassword = oldQuotedPassword.getBytes("UTF-16LE");
//				
//				mods = new ModificationItem[2];
//				mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(pwdAttributeName, oldUnicodePassword));
//				mods[1] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute(pwdAttributeName, newUnicodePassword));
//			} else {
				mods = new ModificationItem[1];
				mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(pwdAttributeName, password));
//			}
			ctx.modifyAttributes(userdn, mods);
			
			// Remove the force password reset flag
			Map<String, Set> attrMap = new HashMap<String, Set>();
			Set<String> passwordResetFlag = new HashSet<String>();
			passwordResetFlag.add("false");
			attrMap.put(CommonUtilities.getProperty(PassphraseConstants.USER_PASSWORD_RESET_FLAG_ATTRIBUTE), passwordResetFlag);
			user.setAttributes(attrMap);
			user.store();
		} finally {
			// Fix to reset the 'pwdLastSet' flag as it doesn't allow to login if an error occurs while
			// resetting the password.
//			if (isInternalUser) {
//				mods = new ModificationItem[1];
//				mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("pwdLastSet", "-1"));
//				ctx.modifyAttributes(userdn, mods);
//			}			
			ctx.close();
		}
	}

	/**
	 * This method validates the user based on the inputs of three random position of the user's passphrase.
	 * 
	 * @param callbacks
	 * @return
	 * @throws Exception
	 */
	private int authenticatePassphrase(Callback[] callbacks) throws Exception {
		String positions = (String) sharedState.get("PASSPHRASE_POSITION");
		positions = positions.substring(1, positions.length() - 1);

		String splitChars[] = positions.split(",");
		int position[] = new int[3];
		for (int i = 0; i < splitChars.length; i++) {
			position[i] = Integer.parseInt(splitChars[i].trim());
		}

		String passphrase = CollectionHelper.getMapAttr(user.getAttributes(), CommonUtilities.getProperty(PassphraseConstants.USER_PASSPHRASE_ATTRIBUTE));
		for (int i = 0; i < 3; i++) {
			if (((PasswordCallback) callbacks[i]).getPassword()[0] != passphrase.charAt(position[i]-1)) {
				debug.error("Invalid Passphrase encountered for user: " + user.getName());
				throw new InvalidPasswordException("Invalid Passphrase", userName);
			}
		}
		lpg = null;
		
		// Determine the next state/module.
        if (isPassphraseResetEnabled())
        	return MODULE_PASSPHRASE_RESET;
        else if (!isResetQNsAnswered())
        	return MODULE_RESET_ANSWERS_ENTRY;
        else
        	return ISAuthConstants.LOGIN_SUCCEED;
	}

	/**
	 * This method is used to validate and change password if force password reset flag is enabled.
	 * 
	 * @param callbacks
	 * @return
	 * @throws Exception
	 */
	private int savePassword(Callback[] callbacks) throws Exception {
        String newPassword = new String(((PasswordCallback)callbacks[0]).getPassword());
        String confirmPassword = new String(((PasswordCallback)callbacks[1]).getPassword());
        String oldPassword = (String) sharedState.get("javax.security.auth.login.password");
           
        try {
			validatePassword(newPassword);
	        // Check minimal password length requirement
	        int newPasswordLength = 0;
	        if (newPassword != null) {
	            newPasswordLength = newPassword.length();
	        }
	        
	        String pwdLength_ = CollectionHelper.getMapAttr(options, CommonUtilities.getProperty(PassphraseConstants.USER_PASSWORD_MIN_LENGTH_ATTRIBUTE));
	        int pwdLength = 8;
	        try {
				pwdLength = Integer.parseInt(pwdLength_);
			} catch (Exception e) {
			}
	        
	        if (newPasswordLength < pwdLength) {
	            if (debug.messageEnabled()) {
	                debug.message("New password is less than the minimal length of " + pwdLength);
	            }
	            getLoginState("CustomModule").logFailed("PasswdMinChars", "CHANGE_USER_PASSWORD_FAILED");
	            replaceHeader(MODULE_PASSWORD_RESET, "The password should not be less than " + pwdLength + " characters.");
	            return MODULE_PASSWORD_RESET;
	        
	        } else if (!newPassword.equals(confirmPassword)) {
	        	getLoginState("CustomModule").logFailed("PasswordsDoNotMatch", "CHANGE_USER_PASSWORD_FAILED");
	            replaceHeader(MODULE_PASSWORD_RESET, "The password and the confirm password do not match.");
	        	return MODULE_PASSWORD_RESET;
	        
	        } else if (newPassword.equals(oldPassword)) {
	        	replaceHeader(MODULE_PASSWORD_RESET, "The new password cannot be the same as current password.");
	        	return MODULE_PASSWORD_RESET;
	        	
	        } else {
	        	validatePWD(newPassword);
				Set<String> attribVals = new HashSet<String>();
				attribVals.add(newPassword);
				cacheAtribute(CommonUtilities.getProperty(PassphraseConstants.USER_PASSWORD_ATTRIBUTE), attribVals);
				// putting the place holder back (if not gives error if pwd reset is processed back on pwd history validation failure)
				replaceHeader(MODULE_PASSWORD_RESET, "#REPLACE#");
	        }
		} catch (Exception e) {
			if (e instanceof InvalidPasswordException)
				throw e;
			debug.error("Error occured in updating user pasword for the user: " + user.getName(), e);
			replaceHeader(MODULE_PASSWORD_RESET, "The password doesn't match the criteria mentioned below.");
        	return MODULE_PASSWORD_RESET;
		}
        
        // Determine the next state/module.
		if (sharedState.get("MODULE_PROCESSED") != null)
			return MODULE_SUCCESS;
		else if (isPassphraseNotEntered())
        	return MODULE_PASSPHRASE_ENTRY;
        else
        	return MODULE_PASSPHRASE_AUTHENTICATION;
	}
	
	/**
	 * This method is used to validate and change passphrase if passphrase not entered (first time user)
	 * or force passphrase reset flag is enabled.
	 * 
	 * @param callbacks
	 * @return
	 * @throws Exception
	 */
	private int savePassphrase(Callback[] callbacks, int state) throws Exception {
        String newPassphrase = new String(((PasswordCallback)callbacks[0]).getPassword());
        String confirmPassphrase = new String(((PasswordCallback)callbacks[1]).getPassword());
           
        // Check minimal password length requirement
        int newPassphraseLength = 0;
        if (newPassphrase != null) {
            newPassphraseLength = newPassphrase.length();
        }
        String passphraseLength_ = CollectionHelper.getMapAttr(options, CommonUtilities.getProperty(PassphraseConstants.USER_PASSPHRASE_MIN_LENGTH_ATTRIBUTE));
        int passphraseLength = 8;
        
        try {
        	passphraseLength = Integer.parseInt(passphraseLength_);
		} catch (Exception e) {
		}
        
        if (newPassphraseLength < passphraseLength) {
            if (debug.messageEnabled()) {
                debug.message("New passphrase is less than the minimal length of " + passphraseLength);
            }
            replaceHeader(state, "The passphrase should not be less than " + passphraseLength + " characters.");
            return state;
        
        } else if (!newPassphrase.equals(confirmPassphrase)) {
            replaceHeader(state, "The passphrase and the confirm passphrase do not match.");
        	return state;
        
        } else if (newPassphrase.matches(".*(\\s)+.*")) {
            replaceHeader(state, "Passphrase can not contain blank spaces within it.");
        	return state;
        
        } else {
			Set<String> attribVals = new HashSet<String>();
			attribVals.add(newPassphrase);
			cacheAtribute(CommonUtilities.getProperty(PassphraseConstants.USER_PASSPHRASE_ATTRIBUTE), attribVals);
			
			// Remove the force passphrase reset flag
			Set<String> passwordResetFlag = new HashSet<String>();
			passwordResetFlag.add("false");
			cacheAtribute(CommonUtilities.getProperty(PassphraseConstants.USER_PASSPHRASE_RESET_FLAG_ATTRIBUTE), passwordResetFlag);
        }
        
        // Determine the next state/module.
        if (isResetQNsAnswered())
        	return MODULE_SUCCESS;
        else
        	return MODULE_RESET_ANSWERS_ENTRY;
	}

	/**
	 * This method is used to capture and save the reset answers for
	 * all the password/passphrase reset questions.
	 * 
	 * @param callbacks
	 * @return
	 * @throws Exception
	 */
	private int saveResetAnswers(Callback[] callbacks) throws Exception {
		Map<String, String> qaMap = (Map<String, String>) sharedState.get("QAMap");
		Set attribVals = new HashSet();
		List<String> answers = new ArrayList<String>();
		
        String answersMinLength_ = CommonUtilities.getProperty(PassphraseConstants.RESET_ANSWERS_MIN_LENGTH);
        int answersMinLength = 2;
        
        try {
        	answersMinLength = Integer.parseInt(answersMinLength_);
		} catch (Exception e) {
		}
		
		for (int i = 0; i < callbacks.length; i++) {
			if (callbacks[i] instanceof NameCallback) {
				NameCallback callback = (NameCallback)callbacks[i];
				if (StringUtils.isEmpty(callback.getName())) {
		            replaceHeader(MODULE_RESET_ANSWERS_ENTRY, "Reset answers cannot be empty.");
		            return MODULE_RESET_ANSWERS_ENTRY;
		        
				} else if (callback.getName().length() < answersMinLength) {
		            replaceHeader(MODULE_RESET_ANSWERS_ENTRY, "Reset answers cannot be less than " + answersMinLength + " characters.");
		            return MODULE_RESET_ANSWERS_ENTRY;
			        
				} else {
		        	if (answers.contains(callback.getName())) {
		        		replaceHeader(MODULE_RESET_ANSWERS_ENTRY, "Same answer cannot be provided for more than one question.");
			            return MODULE_RESET_ANSWERS_ENTRY;
		        	} else {
		        		answers.add(callback.getName());
		        	}
		            String str = qaMap.get(callback.getPrompt()) + "\t" + callback.getName() + "\t" + UMUserPasswordResetOptionsData.DEFAULT_ON;
		            String encryptStr = (String)AccessController.doPrivileged(new EncryptAction(str));
		            attribVals.add(encryptStr);
		        }
			}
		}
		cacheAtribute(UMUserPasswordResetOptionsModel.PW_RESET_QUESTION_ANSWER, attribVals);
		return MODULE_SUCCESS;
	}
	
	/**
	 * This method is invoked at the end of successful authentication session.
	 */
	public Principal getPrincipal() {
		Principal thePrincipal = null;
		if (userPrincipal != null) {
			thePrincipal = userPrincipal;
		} else if (userName != null) {
			userPrincipal = new PassPhrasePrincipal(userName);
			thePrincipal = userPrincipal;
		}
		return thePrincipal;
	}
	
	/**
	 * This method is used to check if passphrase is blank (for first time user).
	 */
	private boolean isPassphraseNotEntered() throws Exception {
		String passphrase = CollectionHelper.getMapAttr(user.getAttributes(), CommonUtilities.getProperty(PassphraseConstants.USER_PASSPHRASE_ATTRIBUTE));
		return StringUtils.isBlank(passphrase);
	}

	/**
	 * This method is used to check if force password reset flag is enabled for the user
	 * or if the password last set has reached the expiry date.
	 * @throws Exception 
	 */
	private boolean isPasswordResetEnabled() throws Exception {
		String isresetenabled = null;
		replaceHeader(MODULE_PASSWORD_RESET, " ");
		try {
			isresetenabled = CollectionHelper.getMapAttr(user.getAttributes(), CommonUtilities.getProperty(PassphraseConstants.USER_PASSWORD_RESET_FLAG_ATTRIBUTE));
		} catch (Exception e) {
			debug.error("Error occured while validating force password reset attribute: ", e);
		}
		if (isresetenabled != null && isresetenabled.equalsIgnoreCase("true"))
			return true;
		
//		String mailId = CollectionHelper.getMapAttr(user.getAttributes(), "mail", "").toLowerCase();		
//		boolean isInternalUser = mailId.endsWith(PassphraseConstants.ID) || mailId.endsWith(PassphraseConstants.CLEARNET_ID);

		// check if the password last set has reached the expiry date
		try {
			// set to current time to ensure the password expiry screen is not put forth if
			// something goes wrong in the below check.
			long pwdLastSet = System.currentTimeMillis();
			LdapContext ctx = CommonUtilities.getLdapContext(CommonUtilities.getContextConfig(user));
			String userdn = CommonUtilities.getUserDN(user);
			
//			if (isInternalUser) {
//				Attributes attrs = ctx.getAttributes(userdn, new String[]{"pwdLastSet"});
//				String pwdLastSet_ = attrs.get("pwdLastSet").get().toString();
//				
//				long yearsFrom1601to1970 = 1970 - 1601;
//				long daysFrom1601to1970 = yearsFrom1601to1970 * 365;
//				daysFrom1601to1970 += yearsFrom1601to1970 / 4; // leap years
//				daysFrom1601to1970 -= 3; // non-leap centuries (1700,1800,1900)
//				long secondsFrom1601to1970 = daysFrom1601to1970 * 24 * 60 * 60;
//				
//				pwdLastSet = ((Long.parseLong(pwdLastSet_.substring(0, pwdLastSet_.length()-7)) - secondsFrom1601to1970) * 1000);
//			} else {
				Attributes attrs = ctx.getAttributes(userdn, new String[]{"passwordExpirationTime"});
				String pwdExpirydate = attrs.get("passwordExpirationTime").get().toString();
				
				// this is the pattern ED store the password expiry date;
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
				Calendar cal = Calendar.getInstance();
				cal.setTime(sdf.parse(pwdExpirydate));
				int edExpiryDays = Integer.parseInt(CommonUtilities.getProperty(PassphraseConstants.ED_SCHEMA_EXPIRY_DAYS));
				cal.add(Calendar.DATE, -edExpiryDays);
				
				pwdLastSet = cal.getTime().getTime();
//			}
			
			float expiryDays = Float.parseFloat(CommonUtilities.getProperty(PassphraseConstants.PASSWORD_EXPIRY_DAYS));
			if (isAdmin())
				expiryDays = Float.parseFloat(CommonUtilities.getProperty(PassphraseConstants.ADMIN_PASSWORD_EXPIRY_DAYS));
			
			if ((System.currentTimeMillis() - pwdLastSet) >= (expiryDays * 24 * 60 * 60 * 1000)) {
				replaceHeader(MODULE_PASSWORD_RESET, "Your password has expired. Please change it.");
				debug.message("Password expired for user " + user.getUniversalId() + ", PWD lastset: " + new Date(pwdLastSet));
				return true;
			}
		} catch (Exception e) {
			debug.error("Error occured while checking password expiry for the user: " + user.getName(), e);
		}
		return false;
	}

	/**
	 * This method is used to check if the user has admin privileges.
	 */
	private boolean isAdmin() {
		if (isSuperAdmin(user.getDN()))
			return true;
		try {
			Set<AMIdentity> groups = user.getMemberships(IdType.GROUP);
			String adminGroup = CommonUtilities.getProperty(PassphraseConstants.ADMIN_GROUP).toLowerCase();
			for (AMIdentity group : groups) {
				if (adminGroup.equalsIgnoreCase(group.getName())) {
					debug.message("Encountered an admin user login: " + user.getName());
					return true;
				}
			}
		} catch (Exception e) {
			debug.error("Error checking admin user: ", e);
		}
		return false;
	}

	/**
	 * This method is used to check if force passphrase reset flag is enabled for the user.
	 */
	private boolean isPassphraseResetEnabled() {
		String isresetenabled = null;
		try {
			isresetenabled = CollectionHelper.getMapAttr(user.getAttributes(), CommonUtilities.getProperty(PassphraseConstants.USER_PASSPHRASE_RESET_FLAG_ATTRIBUTE));
		} catch (Exception e) {
			debug.error("Error occured while validating force password reset attribute for the user: " + user.getName(), e);
		}
		return (isresetenabled != null && isresetenabled.equalsIgnoreCase("true"));
	}

	/**
	 * This method is used to check if the user has provided answers for all the
	 * password/passphrase reset questions.
	 */
	private boolean isResetQNsAnswered() {
		Map<String, String> qaMap = new HashMap<String, String>();
		int i = 0;
		boolean isAnswered = true;
		
		try {
			PasswordResetOptionsModel model = new PasswordResetOptionsModel(getHttpServletRequest(), null);
			List<PasswordResetOptionsData> answers = model.getUserAnswers(user);
			
			for (PasswordResetOptionsData qn : answers) {
				replaceCallback(MODULE_RESET_ANSWERS_ENTRY, i++, new NameCallback(qn.getQuestion()));
				qaMap.put(qn.getQuestionLocalizedName(), qn.getQuestion());
				if (qn.getDataStatus() == PasswordResetOptionsData.DEFAULT_OFF || StringUtils.isEmpty(qn.getAnswer()))
					isAnswered = false;
			}
			sharedState.put("QAMap", qaMap);
		} catch (Exception e) {
			debug.error("Error occured while validating reset questions for the user: " + user.getName(), e);
		}
		return isAnswered;
	}

	/**
	 * This method validates if the password matches at least three from the following categories
	 * 	- English upper case character
	 * 	- English lower case character
	 * 	- Numeric value (0-9)
	 * 	- Special character (e.g. ? #, %)
	 * 
	 * @param password
	 */
	private void validatePWD(String password) throws Exception {
		int test = password.matches(".*([A-Z])+.*")? 1:0;
		test += password.matches(".*([a-z])+.*")? 1:0;
		test += password.matches(".*(\\d)+.*")? 1:0;
		test += password.matches(".*(\\W)+.*")? 1:0;
		
		if (test < 3)
			throw new Exception("Password validation failed");
	}

	/**
	 * This method captures the attributes to be updated at various state and makes it
	 * available in the sharedState. The attributes are updated to the database only
	 * after all the states are processed successfully.
	 * 
	 * @param attrName
	 * @param valueSet
	 */
	private void cacheAtribute(String attrName, Set<String> valueSet) {
		Map<String, Set> attrMap = (Map) sharedState.get("ATTR_MAP");
		if (attrMap == null) {
			attrMap = new HashMap<String, Set>();
			sharedState.put("ATTR_MAP", attrMap);
		}
		attrMap.put(attrName, valueSet);
	}

	/**
	 * This method is used to generate the random passphrase positions and add the
	 * position information to the request and make a copy in the shared state
	 * to validate further. 
	 * 
	 * @throws InvalidArgumentException
	 */
	private void setRandomPositions() throws Exception {
		if (lpg == null)
			lpg = new PassphraseGenerator(user);
		
		replaceHeader(MODULE_PASSPHRASE_AUTHENTICATION, lpg.getPositionString());
		sharedState.put("PASSPHRASE_POSITION", lpg.getPositions());
	}

	/**
	 * This method is used to get the state of the submitted module to avoid the
	 * page refresh problem.
	 * 
	 * @param callbacks
	 * @return
	 */
	private String getSubmitedState(Callback[] callbacks) {
		// a quick fix to load the pwd reset page on pwd history validation failure
		if (sharedState.get("MODULE_PROCESSED") != null)
			return String.valueOf(MODULE_PASSWORD_RESET);
		for (Callback callback_ : callbacks) {
			if (callback_ instanceof ChoiceCallback) {
				ChoiceCallback callback = (ChoiceCallback) callback_;
				if (callback.getPrompt().equals("state"))
					return String.valueOf(callback.getSelectedIndexes()[0] + 1);
			}
		}
		return null;
	}
}