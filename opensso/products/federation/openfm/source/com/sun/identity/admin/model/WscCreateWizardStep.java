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
 * $Id: WscCreateWizardStep.java,v 1.2 2009/10/16 19:39:19 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.util.HashMap;
import java.util.Map;

public enum WscCreateWizardStep {

    WSC_PROFILE(0),
    WSC_USING_STS(1),
    WSC_SECURITY(2),
    WSC_SIGN_ENCRYPT(3),
    WSC_SAML(4),
    SUMMARY(5);
    
    private final int stepNumber;
    private static final Map<Integer, WscCreateWizardStep> intValues =
            new HashMap<Integer, WscCreateWizardStep>() {
                {
                    put(WSC_PROFILE.toInt(), WSC_PROFILE);
                    put(WSC_USING_STS.toInt(), WSC_USING_STS);
                    put(WSC_SECURITY.toInt(), WSC_SECURITY);
                    put(WSC_SIGN_ENCRYPT.toInt(), WSC_SIGN_ENCRYPT);
                    put(WSC_SAML.toInt(), WSC_SAML);
                    put(SUMMARY.toInt(), SUMMARY);
                }
            };

    WscCreateWizardStep(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public int toInt() {
        return stepNumber;
    }

    public static WscCreateWizardStep valueOf(int i) {
        return intValues.get(Integer.valueOf(i));
    }
}
