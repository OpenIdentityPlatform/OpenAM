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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.forgerockrest.utils;

import org.forgerock.openam.services.email.MailServer;

import java.lang.reflect.InvocationTargetException;

/**
 * Responsible for loading the {@link org.forgerock.openam.services.email.MailServer}
 * implementation.
 *
 * Simplifies the reflection based code around this task.
 */
public class MailServerLoader {
    /**
     * Load the mail server implementation based on the class name and realm.
     *
     * @param clazz Non null class name.
     * @param realm Non null realm to associate with the mail server.
     * @return A mail server implementation.
     *
     * @throws IllegalStateException If there was any error resolving the class.
     */
    public MailServer load(String clazz, String realm) throws IllegalStateException {
       try {
            return (MailServer) Class.forName(clazz).getDeclaredConstructor(String.class).newInstance(realm);
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
