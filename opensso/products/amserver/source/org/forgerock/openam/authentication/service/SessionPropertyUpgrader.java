/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
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
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.authentication.service;

import com.iplanet.dpro.session.service.InternalSession;
import java.util.Enumeration;

/**
 * This class is used in case of session upgrade for copying session properties
 * from the old session into the new one. Subclasses should implement
 * {@link #shouldCopy(java.lang.String)} in order to control which properties
 * are needed to copy into the new session.
 * In case you want to modify the copyable session property you are encouraged
 * to override {@link #updateProperty(com.iplanet.dpro.session.service.InternalSession,
 * java.lang.String, java.lang.String)} method.
 *
 * @author Peter Major
 * @supported.all.api
 */
public abstract class SessionPropertyUpgrader {

    /**
     * Entry point for LoginState. This method is called during session upgrade
     * in order to copy session attributes from one session to another.
     *
     * @param oldSession The previous session
     * @param newSession The new session
     * @param forceAuth Whether the authentication was forced
     */
    public final void populateProperties(InternalSession oldSession, InternalSession newSession, boolean forceAuth) {
        Enumeration<String> allProperties = oldSession.getPropertyNames();
        while (allProperties.hasMoreElements()) {
            String key = allProperties.nextElement();
            String value = (String) oldSession.getProperty(key);
            if (shouldCopy(key)) {
                if (!forceAuth) {
                    updateProperty(newSession, key, value);
                } else {
                    updateProperty(oldSession, key, value);
                }
            }
        }
    }

    /**
     * This method updates a session property in the session with the given value.
     * Override this method if you want to change some properties during the
     * upgrade process.
     *
     * NOTE: If you override this, you SHOULD call super.updateProperty(..)
     * at the end of your implementation with the updated values.
     *
     * @param session Session object where the property should be set
     * @param property Name of the property to set
     * @param value Value of the given session property
     */
    public void updateProperty(InternalSession session, String property, String value) {
        if (value != null) {
            session.putProperty(property, value);
        }
    }

    /**
     * This method decides whether a given session property should be copied to
     * the new session.
     * 
     * @param key The name of the session property which we want to decide to copy
     * @return <code>true</code> if the property with the given key should be
     * copied into the new session
     */
    public abstract boolean shouldCopy(String key);
}
