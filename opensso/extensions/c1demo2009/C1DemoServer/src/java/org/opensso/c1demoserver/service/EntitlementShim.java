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
 * $Id: EntitlementShim.java,v 1.2 2009/06/11 05:29:44 superpat7 Exp $
 */

package org.opensso.c1demoserver.service;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import javax.ws.rs.core.MultivaluedMap;

public class EntitlementShim {
    private static String serviceUrl = "http://localhost:8080/entitlement";
    private static Client client = Client.create();
    private static String realm = "/";

    public static boolean isAllowed(String subject, String action, String resource)
    {
        String url = serviceUrl + "/ws/1/entitlement/decision";

        WebResource webResource = client.resource(url);

        MultivaluedMap queryParams = new MultivaluedMapImpl();
        queryParams.add("realm", realm);
        queryParams.add("subject", subject);
        queryParams.add("action", action);
        queryParams.add("resource", resource);

        String s = webResource.queryParams(queryParams).get(String.class);

        return s.trim().equalsIgnoreCase("allow");
    }
}
