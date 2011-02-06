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
 * $Id: StsManageWizardStep.java,v 1.1 2009/09/17 21:56:04 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.util.HashMap;
import java.util.Map;

public enum StsManageWizardStep {

    TOKEN_ISSUANCE(0),
    SECURITY(1),
    SIGN_ENCRYPT(2),
    SAML_CONFIG(3),
    TOKEN_VALIDATION(4),
    SUMMARY(5);
    
    private final int stepNumber;
    private static final Map<Integer, StsManageWizardStep> intValues =
            new HashMap<Integer, StsManageWizardStep>() {
                {
                    put(TOKEN_ISSUANCE.toInt(), TOKEN_ISSUANCE);
                    put(SECURITY.toInt(), SECURITY);
                    put(SIGN_ENCRYPT.toInt(), SIGN_ENCRYPT);
                    put(SAML_CONFIG.toInt(), SAML_CONFIG);
                    put(TOKEN_VALIDATION.toInt(), TOKEN_VALIDATION);
                    put(SUMMARY.toInt(), SUMMARY);
                }
            };

    StsManageWizardStep(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public int toInt() {
        return stepNumber;
    }

    public static StsManageWizardStep valueOf(int i) {
        return intValues.get(Integer.valueOf(i));
    }
}
