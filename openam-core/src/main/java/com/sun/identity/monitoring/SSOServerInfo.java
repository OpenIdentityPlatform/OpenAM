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
 * $Id: SSOServerInfo.java,v 1.1 2009/06/19 02:23:16 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.monitoring;

import java.util.Hashtable;

public class SSOServerInfo {
    String serverID;
    String siteID;
    String serverProtocol;
    String serverName;
    String serverURI;
    String serverPort;
    boolean isEmbeddedDS;
    Hashtable<String, String> siteIDTable;
    Hashtable<String, String> serverIDTable;
    Hashtable<String, String> namingTable;
    String startDate;

    public SSOServerInfo() {
    }

    private SSOServerInfo (SSOServerInfoBuilder asib) {
        serverID = asib.serverID;
        siteID = asib.siteID;
        serverProtocol = asib.serverProtocol;
        serverName = asib.serverName;
        serverURI = asib.serverURI;
        serverPort = asib.serverPort;
        isEmbeddedDS = asib.isEmbeddedDS;
        siteIDTable = asib.siteIDTable;
        serverIDTable = asib.serverIDTable;
        namingTable = asib.namingTable;
        startDate = asib.startDate;
    }

    public static class SSOServerInfoBuilder {
        String serverID;
        String siteID;
        String serverProtocol;
        String serverName;
        String serverURI;
        String serverPort;
        boolean isEmbeddedDS;
        Hashtable<String, String> siteIDTable;
        Hashtable<String, String> serverIDTable;
        Hashtable<String, String> namingTable;
        String startDate;

        public SSOServerInfoBuilder(String svrId, String siteId) {
            serverID = svrId;
            siteID = siteId;
        }

        public SSOServerInfoBuilder svrProtocol (String svrProtocol){
            serverProtocol = svrProtocol;
            return this;
        }

        public SSOServerInfoBuilder svrName (String svrName) {
            serverName = svrName;
            return this;
        }

        public SSOServerInfoBuilder svrURI (String svrURI) {
            serverURI = svrURI;
            return this;
        }

        public SSOServerInfoBuilder svrPort (String svrPort) {
            serverPort = svrPort;
            return this;
        }

        public SSOServerInfoBuilder embeddedDS (boolean isEmbDS) {
            isEmbeddedDS = isEmbDS;
            return this;
        }

        public SSOServerInfoBuilder siteIdTable (Hashtable<String, String> siteIdTab) {
            siteIDTable = siteIdTab;
            return this;
        }

        public SSOServerInfoBuilder svrIdTable (Hashtable<String, String> svrIdTab) {
            serverIDTable = svrIdTab;
            return this;
        }

        public SSOServerInfoBuilder namingTable (Hashtable<String, String> namingTab) {
            namingTable = namingTab;
            return this;
        }

        public SSOServerInfoBuilder startDate (String stDate) {
            startDate = stDate;
            return this;
        }

        public SSOServerInfo build() {
            return new SSOServerInfo (this);
        }
    }
}


