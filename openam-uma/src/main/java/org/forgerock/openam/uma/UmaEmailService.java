/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.uma;

import javax.inject.Singleton;
import javax.mail.MessagingException;

import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.openam.services.email.MailServer;
import org.forgerock.openam.services.email.MailServerImpl;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.Reject;

/**
 * An email service for UMA which sends HTML bodied emails and resolves the provided
 * {@literal to} username with the {@literal realm} to the users email address.
 *
 * @since 13.0.0
 */
@Singleton
public class UmaEmailService {

    /**
     * Sends a HTML email to the specified user.
     *
     * @param realm The realm of the user.
     * @param to The username of the user to send the email to.
     * @param subject The subject of the email.
     * @param message The body of the email
     * @throws MessagingException If the email could not be sent.
     */
    public void email(String realm, String to, String subject, String message) throws MessagingException {
        Reject.ifNull(realm, to, subject, message);
        String emailAddress = resolveEmailAddress(to, realm);
        if (emailAddress == null) {
            throw new MessagingException("User, " + to + ", realm, " + realm
                    + ", does not have an email address specified");
        }
        getMailServer(realm).sendHtmlEmail(emailAddress, subject, message);
    }

    private String resolveEmailAddress(String username, String realm) {
        try {
            AMIdentity identity = IdUtils.getIdentity(username, realm);
            if (identity != null) {
                Set<String> mailAttribute = identity.getAttribute("mail");
                if (mailAttribute != null && !mailAttribute.isEmpty()) {
                    return CollectionUtils.getFirstItem(mailAttribute ,null);
                }
            }
        } catch (IdRepoException | SSOException ignored) {
        }
        return null;
    }

    private MailServer getMailServer(String realm) throws MessagingException {
        try {
            ServiceConfigManager mailmgr = new ServiceConfigManager(
                    AccessController.doPrivileged(AdminTokenAction.getInstance()),
                    MailServerImpl.SERVICE_NAME, MailServerImpl.SERVICE_VERSION);
            ServiceConfig mailscm = mailmgr.getOrganizationConfig(realm, null);

            if (!mailscm.exists()) {
                throw new MessagingException("EmailService is not configured for realm, " + realm);
            }

            Map<String, Set<String>> mailattrs = mailscm.getAttributes();
            String mailServerClass = mailattrs.get("forgerockMailServerImplClassName").iterator().next();
            return Class.forName(mailServerClass).asSubclass(MailServer.class).getDeclaredConstructor(String.class)
                    .newInstance(realm);
        } catch (IllegalAccessException | SSOException | InstantiationException | ClassNotFoundException
                | InvocationTargetException | NoSuchMethodException | SMSException e) {
            throw new MessagingException("Failed to load mail server", e);
        }
    }
}
