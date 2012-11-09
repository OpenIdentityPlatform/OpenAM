/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [2012] [ForgeRock Inc]"
 */
package org.forgerock.restlet.ext.oauth2.representation;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.engine.local.DirectoryServerResource;

public class ClassDirectoryServerResource extends DirectoryServerResource {

    /**
     * Returns a representation of the resource at the target URI. Leverages the
     * client dispatcher of the parent directory's context.
     * 
     * @param resourceUri
     *            The URI of the target resource.
     * @return A response with the representation if success.
     */
    protected Response getRepresentation(String resourceUri, boolean wait_for_Restlet_fix) {
        Request request = new Request(Method.GET, resourceUri);
        request.getAttributes().put("org.restlet.clap.classLoader",
                ClassDirectoryServerResource.class.getClassLoader());
        return getClientDispatcher().handle(request);
    }

}
