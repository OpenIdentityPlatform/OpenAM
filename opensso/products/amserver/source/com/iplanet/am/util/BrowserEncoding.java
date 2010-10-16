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
 * $Id: BrowserEncoding.java,v 1.2 2008/06/25 05:41:27 qcheng Exp $
 *
 */

package com.iplanet.am.util;

import com.iplanet.services.cdm.G11NSettings;

/**
 * This method maps IANA name of charset to Java charsets. Even though Java uses
 * IANA names for charsets, it differs in some exceptional cases such as
 * Shift_JIS vs SJIS Most of the modern web containers have bridged the
 * difference and this class may not be necessary for future web containers at
 * all.
 */
public class BrowserEncoding {

    private static G11NSettings g11nSettings;
    static {
        g11nSettings = G11NSettings.getInstance();
    }

    private BrowserEncoding() {
    }

    static public String mapHttp2JavaCharset(String str) {
        return g11nSettings.getJavaCharset(str);

    }

}
