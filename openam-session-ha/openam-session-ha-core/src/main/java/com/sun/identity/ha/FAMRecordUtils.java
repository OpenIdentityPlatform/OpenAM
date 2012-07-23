/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: FAMRecordUtils.java,v 1.1 2008/07/22 18:12:03 weisun2 Exp $
 */ 
package com.sun.identity.ha;

import java.util.ResourceBundle;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;

/**
 * The <code>FAMRecordUtils</code> contains utility methods for FAM Ha
 * implementation.
 *
 */
public class FAMRecordUtils {
    // The deugging instance
    public static Debug debug = Debug.getInstance("amHa");
    //  FAMRecord Resource bundle
    public static final String BUNDLE_NAME = "amHa";
    // The resource bundle for FAMRecord implementation.
    public static ResourceBundle bundle = Locale.
        getInstallResourceBundle(BUNDLE_NAME);
}        
