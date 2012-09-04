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
 * $Id: SessionRepository.java,v 1.4 2008/06/25 05:41:31 qcheng Exp $
 *
 */

package com.iplanet.dpro.session.service;

import com.iplanet.am.util.SystemProperties;

import java.lang.reflect.Method;

/**
 * <code>SessionRepository</code> represents the session
 * repository , default repository 
 * is <code>OpenDJPersistentStore</code>.
 */

public class SessionRepository {

    private static final String  OPENDJ_REPOSITORY_CLASS =
            "org.forgerock.openam.session.ha.amsessionstore.store.opendj.OpenDJPersistentStore";

    private static final String REPOSITORY_CLASS = SystemProperties.get(
            AMSessionRepository.REPOSITORY_CLASS_PROPERTY, OPENDJ_REPOSITORY_CLASS);

    private static AMSessionRepository sessionRepository = null;

    /**
     * Private, do not allow instantiation.
     */
    private SessionRepository() {
    }

    /**
     * Common Get Instance method to obtain access to
     * Service Methods.
     *
     * @return AMSessionRepository Singleton Instance.
     * @throws Exception
     */
    public static synchronized AMSessionRepository getInstance()
            throws Exception {
        if (sessionRepository == null) {
            Class c = Class.forName(REPOSITORY_CLASS);
            Method factoryMethod = c.getDeclaredMethod("getInstance");
            sessionRepository  = (AMSessionRepository) factoryMethod.invoke(null, null);
        }
        return sessionRepository;
    }
}
