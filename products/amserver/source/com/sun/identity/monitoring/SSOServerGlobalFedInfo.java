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
 * $Id: SSOServerGlobalFedInfo.java,v 1.1 2009/06/19 02:23:16 bigfatrat Exp $
 *
 */

package com.sun.identity.monitoring;

import java.util.ArrayList;

public class SSOServerGlobalFedInfo {
    ArrayList saml1TPs;

    public SSOServerGlobalFedInfo() {
    }

    private SSOServerGlobalFedInfo (SSOServerGlobalFedInfoBuilder asib) {
        saml1TPs = asib.saml1TPs;
    }

    public static class SSOServerGlobalFedInfoBuilder {
        ArrayList saml1TPs;

        public SSOServerGlobalFedInfoBuilder saml1TPs (ArrayList saml1TP) {
            saml1TPs = saml1TP;
            return this;

        }

        public SSOServerGlobalFedInfo build() {
            return new SSOServerGlobalFedInfo (this);
        }
    }
}


