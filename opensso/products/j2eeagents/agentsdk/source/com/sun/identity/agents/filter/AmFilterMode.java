/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AmFilterMode.java,v 1.2 2008/06/25 05:51:43 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import com.sun.identity.agents.util.ConstrainedSelection;

/**
 * An agent filter mode
 */
public class AmFilterMode extends ConstrainedSelection {
    
    public static final int INT_MODE_NONE = 0;
    public static final int INT_MODE_SSO_ONLY = 1;
    public static final int INT_MODE_J2EE_POLICY = 2;
    public static final int INT_MODE_URL_POLICY = 3;
    public static final int INT_MODE_ALL = 4;
    
    public static final String STR_MODE_NONE = "NONE";
    public static final String STR_MODE_SSO_ONLY = "SSO_ONLY";
    public static final String STR_MODE_J2EE_POLICY = "J2EE_POLICY";
    public static final String STR_MODE_URL_POLICY = "URL_POLICY";
    public static final String STR_MODE_ALL = "ALL";
    
    public static final AmFilterMode MODE_NONE =
        new AmFilterMode(STR_MODE_NONE, INT_MODE_NONE);
    
    public static final AmFilterMode MODE_SSO_ONLY =
        new AmFilterMode(STR_MODE_SSO_ONLY, INT_MODE_SSO_ONLY);
    
    public static final AmFilterMode MODE_J2EE_POLICY =
        new AmFilterMode(STR_MODE_J2EE_POLICY, INT_MODE_J2EE_POLICY);
    
    public static final AmFilterMode MODE_URL_POLICY =
        new AmFilterMode(STR_MODE_URL_POLICY, INT_MODE_URL_POLICY);
    
    public static final AmFilterMode MODE_ALL =
        new AmFilterMode(STR_MODE_ALL, INT_MODE_ALL);
    
    private static final AmFilterMode[] values = new AmFilterMode[] {
      MODE_NONE, MODE_SSO_ONLY, MODE_J2EE_POLICY, MODE_URL_POLICY, MODE_ALL};
    
    public static AmFilterMode get(int mode) {
        return (AmFilterMode) ConstrainedSelection.get(mode, values);
    }
    
    public static AmFilterMode get(String mode) {
        return (AmFilterMode) ConstrainedSelection.get(mode, values);
    }

    private AmFilterMode(String name, int value) {
        super(name, value);
    }
}
