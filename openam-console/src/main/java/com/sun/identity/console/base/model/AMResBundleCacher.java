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
 * $Id: AMResBundleCacher.java,v 1.2 2008/06/25 05:42:50 qcheng Exp $
 *
 */

package com.sun.identity.console.base.model;

import java.util.Locale;
import java.util.ResourceBundle;

/* - NEED NOT LOG - */

/**
 * Utility to cache resource bundle object. It leverage on
 * <code>com.iplanet.am.util.AMResourceBundleCache</code>
 */
public class AMResBundleCacher {
    /**
     * Gets resource bundle
     *
     * @param name of bundle
     * @param locale of bundle
     * @return resource bundle
     */
    public static ResourceBundle getBundle(String name, Locale locale) {
        AMResourceBundleCache cache = AMResourceBundleCache.getInstance();
        ResourceBundle rb = cache.getResBundle(name, locale);

        if (rb == null) {
            rb = cache.getResBundle(AMAdminConstants.DEFAULT_RB, locale);
        }

        return rb;
    }
}
