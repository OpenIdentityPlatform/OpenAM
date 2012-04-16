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
 * $Id: RestService.java,v 1.1 2009/12/01 02:09:57 veiming Exp $
 */

package com.sun.identity.oauth.service;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;

/**
 *
 * @author dennis
 */
public class RestService extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> s = new HashSet<Class<?>>();
        s.add(RequestTokenRequest.class);
        s.add(ConsumerResource.class);
        s.add(ConsumerResource.class);
        s.add(RequestTokenResource.class);
        s.add(AccessTokenResource.class);
        s.add(ConsumerRequest.class);
        s.add(AccessTokenRequest.class);
        s.add(AuthorizationFactory.class);
        s.add(NoBrowserAuthorization.class);
        return s;
    }
}
