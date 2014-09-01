/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AddProviderToCOT.java,v 1.2 2008/06/25 05:50:01 qcheng Exp $
 *
 */

package com.sun.identity.workflow;

import com.sun.identity.cot.COTConstants;
import com.sun.identity.cot.COTException;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.CircleOfTrustManager;
import java.util.Collections;

/**
 * Add a provider to a circle of trust.
 */
public class AddProviderToCOT {
    private AddProviderToCOT() {
    }

    public static void addToCOT(
        String realm,
        String cot,
        String entityId
    ) throws COTException {
        CircleOfTrustManager cotManager = new CircleOfTrustManager();
        if (!cotManager.getAllCirclesOfTrust(realm).contains(cot)) {
            CircleOfTrustDescriptor desc = new CircleOfTrustDescriptor(
                cot, realm, COTConstants.ACTIVE,"", null, null, null, null,
                Collections.EMPTY_SET);
            cotManager.createCircleOfTrust(realm, desc);
        }
        cotManager.addCircleOfTrustMember(realm, cot, COTConstants.SAML2, 
            entityId);
    }
}
