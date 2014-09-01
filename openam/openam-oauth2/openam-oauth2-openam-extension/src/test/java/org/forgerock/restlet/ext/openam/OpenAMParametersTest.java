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
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.restlet.ext.openam;

import org.restlet.data.Reference;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class OpenAMParametersTest {
    @Test
    public void testGetOpenAMServerRef() throws Exception {
        OpenAMParameters parameters = new OpenAMParameters();
        Reference baseRef = new Reference();
        baseRef.setScheme(parameters.getServerProtocol().getSchemeName());
        baseRef.setHostDomain(parameters.getServerHost());
        baseRef.setHostPort(parameters.getServerPort());
        baseRef.setPath(parameters.getServerDeploymentURI());
        Assert.assertEquals(baseRef.toString(), "http://localhost:8080/openam");
        baseRef.setPath(baseRef.getPath() + "/UI/Login");
        Assert.assertEquals(baseRef.toString(), "http://localhost:8080/openam/UI/Login");
    }
}
