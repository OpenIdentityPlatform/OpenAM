/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LegacyServer.java,v 1.2 2008/06/25 05:42:28 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.common.configuration;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * This class encapulate the server instance based on legacy platform service.
 */
public class LegacyServer {
    String name;
    String id;
    Set sites;

    /**
     * Creates an instance of <code>LegacyServer</code>.
     *
     * @param svr Formated server data.
     */
    public LegacyServer(String svr) {
        StringTokenizer st = new StringTokenizer(svr, "|");
        String name = st.nextToken();
        String id = st.nextToken();
        sites = new HashSet();

        while (st.hasMoreTokens()) {
            sites.add(st.nextToken());
        }
    }

    void addSite(String siteId) {
        sites.add(siteId);
    }

    void removeSite(String siteId) {
        sites.remove(siteId);
    }

    boolean belongToSite(String siteId) {
        return sites.contains(siteId);
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append(name)
            .append("|")
            .append(id);
        for (Iterator i = sites.iterator(); i.hasNext(); ) {
            buff.append("|")
                .append((String)i.next());
        }
        return buff.toString();
    }
}
