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
 * $Id: SSOServerRealmFedInfo.java,v 1.1 2009/06/19 02:23:17 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.monitoring;

import java.util.Map;
import java.util.Set;

public class SSOServerRealmFedInfo {
    String realmName;
    Set<String> cots;
    Map<String, Map<String, String>> samlv2Ents;
    Map<String, Map<String, String>> wsEnts;
    Map<String, Map<String, String>> idffEnts;
    Map<String, Map<String, Set<String>>> membEnts;

    public SSOServerRealmFedInfo() {
    }

    private SSOServerRealmFedInfo (SSOServerRealmFedInfoBuilder asib) {
        realmName = asib.realmName;
        cots = asib.COTs;
        samlv2Ents = asib.samlv2Ents;
        wsEnts = asib.wsEnts;
        idffEnts = asib.idffEnts;
        membEnts = asib.membEnts;
    }

    public static class SSOServerRealmFedInfoBuilder {
        String realmName;
        Set<String> COTs;
        Map<String, Map<String, String>> samlv2Ents;
        Map<String, Map<String, String>> wsEnts;
        Map<String, Map<String, String>> idffEnts;
        Map<String, Map<String, Set<String>>> membEnts;

        public SSOServerRealmFedInfoBuilder(String rlm) {
            realmName = rlm;
        }

        public SSOServerRealmFedInfoBuilder cots (Set<String> cots) {
            COTs = cots;
            return this;
        }

        public SSOServerRealmFedInfoBuilder samlv2Entities (Map<String, Map<String, String>> samlv2ents) {
            samlv2Ents = samlv2ents;
            return this;
        }

        public SSOServerRealmFedInfoBuilder wsEntities (Map<String, Map<String, String>> wsents) {
            wsEnts = wsents;
            return this;
        }

        public SSOServerRealmFedInfoBuilder idffEntities (Map<String, Map<String, String>> idffents) {
            idffEnts = idffents;
            return this;
        }

        public SSOServerRealmFedInfoBuilder membEntities (Map<String, Map<String, Set<String>>> membents) {
            membEnts = membents;
            return this;
        }

        public SSOServerRealmFedInfo build() {
            return new SSOServerRealmFedInfo (this);
        }
    }
}

