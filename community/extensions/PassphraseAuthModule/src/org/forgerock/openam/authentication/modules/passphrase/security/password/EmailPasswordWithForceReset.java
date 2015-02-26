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

package org.forgerock.openam.authentication.modules.passphrase.security.password;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.SendFailedException;

import org.apache.commons.lang.StringUtils;

import com.iplanet.sso.SSOException;
import org.forgerock.openam.authentication.modules.passphrase.common.utility.CommonUtilities;
import org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.password.plugins.EmailPassword;
import com.sun.identity.password.ui.model.PWResetException;
import com.sun.identity.password.ui.model.PWResetModel;
import com.sun.identity.password.ui.model.PWResetModelImpl;
import com.sun.identity.password.ui.model.PWResetResBundleCacher;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;

/**
 * This Email plugin sends a mail in HTML format rather than the OpenSSO's plain text mail.
 * Also this enables the Password reset flag which is not actually happening by default.
 * 
 * @author Richard Daniel, Gowtham Mani
 */
public class EmailPasswordWithForceReset extends EmailPassword {
	private static Debug debug = Debug.getInstance("CustomModule");
	private PWResetModel model = new PWResetModelImpl();

	/**
	 * Notifies user when password is changed.
	 * 
	 * @param user <code>AMIdentity</code> object
	 * @param password  new password
	 * @param locale user locale
	 * @throws PWResetException if password cannot be notified
	 */
	public void notifyPassword(AMIdentity user, String password, Locale locale) throws PWResetException {
		try {
			Map<String, Set<?>> mapEcriture = new HashMap<String, Set<?>>();
			Set<String> attribVals = new HashSet<String>();
			attribVals.add("true");
			mapEcriture.put(CommonUtilities.getProperty(PassphraseConstants.USER_PASSWORD_RESET_FLAG_ATTRIBUTE), attribVals);
			user.setAttributes(mapEcriture);
			user.store();
		} catch (Exception e) {
			debug.error("Error enabling the password reset flag: ", e);
		} finally {
			sendCustomMail(user, password, locale);
		}
	}

	/**
	 * This method is used to send a  custom mail in HTML format.
	 * 
	 * @param user
	 * @param password
	 * @param locale
	 * @throws PWResetException
	 */
	public void sendCustomMail(AMIdentity user, String password, Locale locale) throws PWResetException {
		ResourceBundle rb = null;
		try {
			Set<?> localeSet = user.getAttribute(Constants.USER_LOCALE_ATTR);
			Locale userLocale = null;
			
			if ((localeSet == null) || localeSet.isEmpty()) {
				userLocale = locale;
			} else {
				String localeStr = localeSet.iterator().next().toString();
				userLocale = com.iplanet.am.util.Locale.getLocale (localeStr);
			}

			rb = PWResetResBundleCacher.getBundle(PWResetModel.DEFAULT_RB, userLocale);
			String subject = rb.getString("resetSubject.message");

			String emailAddress = CollectionHelper.getMapAttr(user.getAttributes(), PWResetModel.USER_MAIL_ATTR);;
			if (StringUtils.isBlank(emailAddress)) {
				model.debugWarning("There is no email address for this user.");
				throw new PWResetException(rb.getString("noEmail.message"));
			} else {
				String firstName = CollectionHelper.getMapAttr(user.getAttributes(), CommonUtilities.getProperty(PassphraseConstants.FIRST_NAME_ATTRIBUTE));
				String lastName = CollectionHelper.getMapAttr(user.getAttributes(), CommonUtilities.getProperty(PassphraseConstants.LAST_NAME_ATTRIBUTE));
				
				Map<String, String> dataMap = new HashMap<String, String>();
				dataMap.put("USER_NAME", firstName + " " + lastName);
				dataMap.put("PASSWORD", password);
				CommonUtilities.sendEmailToUser(emailAddress, subject, dataMap, PassphraseConstants.MAIL_TEMPLATE_PATH_PASSWORD, userLocale);
			}
		} catch (SSOException e) {
			model.debugWarning("EmailPassword.notifyPassword", e);
			throw new PWResetException(e);
		} catch (IdRepoException e) {
			model.debugWarning("EmailPassword.notifyPassword", e);
			throw new PWResetException(e);
		} catch (SendFailedException e) {
			model.debugWarning("EmailPassword.notifyPassword", e);
			throw new PWResetException(rb.getString("sendEmailFailed.message"));
		} catch (MessagingException e) {
			model.debugWarning ("EmailPassword.notifyPassword", e);
			throw new PWResetException(e);
		}
	}
}