/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: WspCreateWizardStep.java,v 1.1 2009/10/01 18:34:16 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.util.HashMap;
import java.util.Map;

public enum WspCreateWizardStep {

    WSP_PROFILE(0),
    WSP_SECURITY(1),
    WSP_SIGN_ENCRYPT(2),
    WSP_SAML(3),
    SUMMARY(4);
    
    private final int stepNumber;
    private static final Map<Integer, WspCreateWizardStep> intValues =
            new HashMap<Integer, WspCreateWizardStep>() {
                {
                    put(WSP_PROFILE.toInt(), WSP_PROFILE);
                    put(WSP_SECURITY.toInt(), WSP_SECURITY);
                    put(WSP_SIGN_ENCRYPT.toInt(), WSP_SIGN_ENCRYPT);
                    put(WSP_SAML.toInt(), WSP_SAML);
                    put(SUMMARY.toInt(), SUMMARY);
                }
            };

    WspCreateWizardStep(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public int toInt() {
        return stepNumber;
    }

    public static WspCreateWizardStep valueOf(int i) {
        return intValues.get(Integer.valueOf(i));
    }
}
