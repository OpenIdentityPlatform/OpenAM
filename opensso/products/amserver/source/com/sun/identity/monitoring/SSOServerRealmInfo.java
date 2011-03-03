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
 * $Id: SSOServerRealmInfo.java,v 1.1 2009/06/19 02:23:18 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.monitoring;

import java.util.HashMap;
import java.util.Hashtable;

public class SSOServerRealmInfo {
    String realmName;
    HashMap authModules;
    Hashtable serverIDTable;
    Hashtable namingTable;

    public SSOServerRealmInfo() {
    }

    private SSOServerRealmInfo (SSOServerRealmInfoBuilder asib) {
        realmName = asib.realmName;
        authModules = asib.authModules;
        serverIDTable = asib.serverIDTable;
        namingTable = asib.namingTable;
    }

    public static class SSOServerRealmInfoBuilder {
        String realmName;
        HashMap authModules;
        Hashtable siteIDTable;
        Hashtable serverIDTable;
        Hashtable namingTable;

        public SSOServerRealmInfoBuilder(String rlm) {
            realmName = rlm;
        }

        public SSOServerRealmInfoBuilder authModules (HashMap authMods) {
            authModules = authMods;
            return this;
        }

        public SSOServerRealmInfoBuilder siteIdTable (Hashtable siteIdTab) {
            siteIDTable = siteIdTab;
            return this;
        }

        public SSOServerRealmInfoBuilder svrIdTable (Hashtable svrIdTab) {
            serverIDTable = svrIdTab;
            return this;
        }

        public SSOServerRealmInfoBuilder namingTable (Hashtable namingTab) {
            namingTable = namingTab;
            return this;
        }

        public SSOServerRealmInfo build() {
            return new SSOServerRealmInfo (this);
        }
    }
}


