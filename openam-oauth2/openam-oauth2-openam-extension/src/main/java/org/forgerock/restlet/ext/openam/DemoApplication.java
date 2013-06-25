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
 * "Portions Copyrighted [2012] [Forgerock Inc]"
 */

package org.forgerock.restlet.ext.openam;

import org.forgerock.openam.oauth2.provider.impl.OpenAMServerAuthorizer;
import org.forgerock.restlet.ext.openam.server.OpenAMServletAuthenticator;
import org.restlet.Application;
import org.restlet.Restlet;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class DemoApplication extends Application {
    /**
     * Creates a inbound root Restlet that will receive all incoming calls. In
     * general, instances of Router, Filter or Finder classes will be used as
     * initial application Restlet. The default implementation returns null by
     * default. This method is intended to be overridden by subclasses.
     * 
     * @return The inbound root Restlet.
     */
    @Override
    public Restlet createInboundRoot() {
        OpenAMServletAuthenticator root = new OpenAMServletAuthenticator(getContext(), null);
        root.setEnroler(new OpenAMEnroler());
        OpenAMServerAuthorizer authorizer = new OpenAMServerAuthorizer("OAUTH2");
        authorizer.setNext(DemoServerResource.class);
        root.setNext(authorizer);
        return root;
    }
}
