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
 * $Id: EmailPassword.java,v 1.2 2008/06/25 05:43:41 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */
package com.sun.identity.password.plugins;

import com.iplanet.am.util.AMSendMail;
import com.iplanet.services.cdm.G11NSettings;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.password.ui.model.PWResetException;
import com.sun.identity.password.ui.model.PWResetModel;
import com.sun.identity.password.ui.model.PWResetModelImpl;
import com.sun.identity.password.ui.model.PWResetResBundleCacher;
import com.sun.identity.shared.Constants;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Set;
import java.util.ResourceBundle;
import javax.mail.SendFailedException;
import javax.mail.MessagingException;

/**
 * <code>EmailPassword</code> defines a set of methods
 * that are required to notify a user when a password is changed.
 */
public class EmailPassword implements NotifyPassword {

    private PWResetModel model = new PWResetModelImpl();
    static private G11NSettings g11nSettings = G11NSettings.getInstance();
    static private String bundleName = PWResetModel.DEFAULT_RB;
    private Locale userLocale = null;

    /**
     * Notifies user when password is changed.
     *
     * @param user <code>AMIdentity</code> object
     * @param password new password
     * @param locale user locale
     * @throws PWResetException if password cannot be notified
     */
    public void notifyPassword(AMIdentity user, String password, Locale locale) throws PWResetException {
        ResourceBundle rb = null;
        try {
            Set<String> set = user.getAttribute(model.getMailAttribute(user.getRealm()));
	    Set<String> localeSet = user.getAttribute(Constants.USER_LOCALE_ATTR);

	    if (localeSet == null || localeSet.isEmpty()) {
		userLocale = locale;
	    } else {
		String localeStr = localeSet.iterator().next();
		userLocale = com.sun.identity.shared.locale.Locale.getLocale(localeStr);
	    }

            rb = PWResetResBundleCacher.getBundle(bundleName, userLocale);

            if (set == null || set.isEmpty()) {
                model.debugWarning("There is no email address for this user.");
                throw new PWResetException(rb.getString("noEmail.message"));
            } else {
                String emailAddress = set.iterator().next();
                sendEmailToUser(emailAddress, password);
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

    /**
     * Sents reset password email to the user.
     *
     * @param emailAddres email address of the user
     * @param password new password
     */
    private void sendEmailToUser(String emailAddress, String password) 
        throws MessagingException 
    {
        String obj[] = new String[1];
        obj[0] = password;
	ResourceBundle rb = PWResetResBundleCacher.getBundle(
                 bundleName,userLocale);
        String msg = MessageFormat.format(
            rb.getString("resetPassMail.message"), (Object[])obj);
        String subject = rb.getString("resetSubject.message");
        String to[] = new String[1];
        to[0] = emailAddress;
        String from = rb.getString("fromAddress.label");
	String charset = g11nSettings.getDefaultCharsetForLocale(userLocale);
        AMSendMail sendMail = new AMSendMail();
        sendMail.postMail(to, subject, msg, from,charset);
    }
}
