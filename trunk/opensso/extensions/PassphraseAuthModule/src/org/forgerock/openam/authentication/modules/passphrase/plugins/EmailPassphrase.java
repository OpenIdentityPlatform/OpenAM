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

package org.forgerock.openam.authentication.modules.passphrase.plugins;

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
import org.forgerock.openam.authentication.modules.passphrase.ui.model.PPResetException;
import org.forgerock.openam.authentication.modules.passphrase.ui.model.PPResetModel;
import org.forgerock.openam.authentication.modules.passphrase.ui.model.PPResetModelImpl;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.password.ui.model.PWResetModel;
import com.sun.identity.password.ui.model.PWResetResBundleCacher;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;

/**
 * This class is used to send a mail in HTML format rather than the OpenSSO's plain text mail
 * when a passphrase is changed.
 * 
 * @author Satheesh M
 */
public class EmailPassphrase implements NotifyPassphrase {
	private static Debug debug = Debug.getInstance("CustomModule");
	private PPResetModel model = new PPResetModelImpl();

	/**
	 * Notifies user when passphrase is changed.
	 * 
	 * @param user <code>AMIdentity</code> object
	 * @param passphrase  new passphrase
	 * @param locale user locale
	 * @throws PPResetException if passphrase cannot be notified
	 */
	public void notifyPassphrase(AMIdentity user, String passphrase, Locale locale) throws PPResetException {
		try {
			Map<String, Set<?>> mapEcriture = new HashMap<String, Set<?>>();
			Set<String> passphraseStore = new HashSet<String>();
			passphraseStore.add("true");
			mapEcriture.put(CommonUtilities.getProperty(PassphraseConstants.USER_PASSPHRASE_RESET_FLAG_ATTRIBUTE), passphraseStore);
			user.setAttributes(mapEcriture);
			user.store();
		} catch (Exception e) {
			debug.error("Error enabling the passphrase reset flag: ", e);
		} finally {
			sendCustomMail(user, passphrase, locale);
		}
	}
	
	/**
	 * This method is used to send a custom mail in HTML format.
	 * 
	 * @param user
	 * @param password
	 * @param locale
	 * @throws PWResetException
	 */
	public void sendCustomMail(AMIdentity user, String passphrase, Locale locale) throws PPResetException {
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

			rb = PWResetResBundleCacher.getBundle(PPResetModel.DEFAULT_RB, userLocale);
			String subject = rb.getString("resetSubject.message");

			String emailAddress = CollectionHelper.getMapAttr(user.getAttributes(), PWResetModel.USER_MAIL_ATTR);;
			if (StringUtils.isBlank(emailAddress)) {
				model.debugWarning("There is no email address for this user.");
				throw new PPResetException(rb.getString("noEmail.message"));
			} else {
				String firstName = CollectionHelper.getMapAttr(user.getAttributes(), CommonUtilities.getProperty(PassphraseConstants.FIRST_NAME_ATTRIBUTE));
				String lastName = CollectionHelper.getMapAttr(user.getAttributes(), CommonUtilities.getProperty(PassphraseConstants.LAST_NAME_ATTRIBUTE));
				
				Map<String, String> dataMap = new HashMap<String, String>();
				dataMap.put("USER_NAME", firstName + " " + lastName);
				dataMap.put("PASSPHRASE", passphrase);
				CommonUtilities.sendEmailToUser(emailAddress, subject, dataMap, PassphraseConstants.MAIL_TEMPLATE_PATH_PASSPHRASE, userLocale);
			}
		} catch (SSOException e) {
			model.debugWarning("EmailPassphrase.notifyPassphrase", e);
			throw new PPResetException(e);
		} catch (IdRepoException e) {
			model.debugWarning("EmailPassphrase.notifyPassphrase", e);
			throw new PPResetException(e);
		} catch (SendFailedException e) {
			model.debugWarning("EmailPassphrase.notifyPassphrase", e);
			throw new PPResetException(rb.getString("sendEmailFailed.message"));
		} catch (MessagingException e) {
			model.debugWarning("EmailPassphrase.notifyPassphrase", e);
			throw new PPResetException(e);
		}
	}
}