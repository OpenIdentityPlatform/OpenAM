/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2012 ForgeRock Inc.
 */
package org.forgerock.openam.forgerockrest;

import java.lang.String;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;


import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Router;


/**
 * A simple {@code Map} based collection resource provider.
 */
public final class TestDispatcher {


    private TestDispatcher() {

    }

    static public void initDispatcher(Router router) {
        //router.addRoute(EQUALS, "/test", new TestResource());// Just a simply READ to make sure dispatching works
    }
}