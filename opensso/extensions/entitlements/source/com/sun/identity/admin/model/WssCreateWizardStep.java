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
 * $Id: WssCreateWizardStep.java,v 1.2 2009/07/23 20:46:53 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.util.HashMap;
import java.util.Map;

public enum WssCreateWizardStep {

    WSP_ENDPOINT_SECURITY(0),
    WSP_SIGN_ENCRYPT(1),
    WSC_SERVICENAME_SECURITY(2),
    SUMMARY(3);
    
    private final int stepNumber;
    private static final Map<Integer, WssCreateWizardStep> intValues =
            new HashMap<Integer, WssCreateWizardStep>() {
                {
                    put(WSP_ENDPOINT_SECURITY.toInt(), WSP_ENDPOINT_SECURITY);
                    put(WSC_SERVICENAME_SECURITY.toInt(), WSC_SERVICENAME_SECURITY);
                    put(WSP_SIGN_ENCRYPT.toInt(), WSP_SIGN_ENCRYPT);
                    put(SUMMARY.toInt(), SUMMARY);
                }
            };

    WssCreateWizardStep(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public int toInt() {
        return stepNumber;
    }

    public static WssCreateWizardStep valueOf(int i) {
        return intValues.get(Integer.valueOf(i));
    }
}
