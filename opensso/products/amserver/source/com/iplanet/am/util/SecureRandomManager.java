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
 * $Id: SecureRandomManager.java,v 1.2 2008/06/25 05:41:28 qcheng Exp $
 *
 */

package com.iplanet.am.util;

import java.security.SecureRandom;

public class SecureRandomManager {

    private static final String SECURE_RANDOM_IMPL_KEY = 
        "com.iplanet.security.SecureRandomFactoryImpl";

    private static SecureRandom secureRandom = null;

    public static SecureRandom getSecureRandom() throws Exception {
        if (secureRandom == null) {
            String secureRandomImplClass = SystemProperties.get(
                    SECURE_RANDOM_IMPL_KEY,
                    "com.iplanet.am.util.SecureRandomFactoryImpl");
            SecureRandomFactory secureRandomFactory = 
                (SecureRandomFactory) Class
                    .forName(secureRandomImplClass).newInstance();
            secureRandom = secureRandomFactory.getSecureRandomInstance();
        }

        return secureRandom;
    }

}
