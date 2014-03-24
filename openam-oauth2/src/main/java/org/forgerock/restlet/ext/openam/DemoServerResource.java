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

import org.forgerock.openam.oauth2.provider.impl.OpenAMUser;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class DemoServerResource extends ServerResource {
    @Get
    public Representation demo() {
        StringBuilder sb = new StringBuilder("");
        if (getRequest().getClientInfo().getUser() instanceof OpenAMUser) {
            OpenAMUser user = (OpenAMUser) getRequest().getClientInfo().getUser();
            sb.append("Identifier: ").append(user.getIdentifier()).append("\n");
            sb.append("Token: ").append(user.getToken()).append("\n");
        } else {
            sb.append("ERROR - No OpenAM User");
            sb.append(getRequest().getClientInfo().getUser());
        }
        return new StringRepresentation(sb);
    }

}
