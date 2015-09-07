/*
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
 * $Id: RestService.java,v 1.1 2009/12/01 02:09:57 veiming Exp $
 *
 * Portions Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.rest;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.forgerock.guice.core.InjectorHolder;

public class RestService extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<LegacyRestEndpoint> endpoints = InjectorHolder.getInstance(
                Key.get(new TypeLiteral<Set<LegacyRestEndpoint>>() {}));
        Set<Class<?>> s = new HashSet<>();
        for (LegacyRestEndpoint endpoint : endpoints) {
            s.add(endpoint.getEndpointClass());
        }
        return s;
    }
}
