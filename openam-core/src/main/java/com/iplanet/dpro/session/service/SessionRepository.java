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

/**
 * <code>SessionRepository</code> represents the session
 * repository , default repository 
 * is <code>JMQSessionRepository</code>
 * @see com.iplanet.dpro.session.JMQSessionRepository
 */

public class SessionRepository {

    private static final String DEFAULT_REPOSITORY_CLASS = 
        "com.iplanet.dpro.session.JMQSessionRepository";

    private static final String REPOSITORY_CLASS_PROPERTY = 
        "com.sun.am.session.SessionRepositoryImpl";

    private static final String REPOSITORY_CLASS = SystemProperties.get(
            REPOSITORY_CLASS_PROPERTY, DEFAULT_REPOSITORY_CLASS);

    private static AMSessionRepository sessionRepository = null;

    /**
     * @return the instance of AMSessionRepository
     * @throws Exception
     */
    public static synchronized AMSessionRepository getInstance()
            throws Exception {
        if (sessionRepository == null) {
            sessionRepository = (AMSessionRepository) Class.forName(
                    REPOSITORY_CLASS).newInstance();
        }
        return sessionRepository;
    }
}
