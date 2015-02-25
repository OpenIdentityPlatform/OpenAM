/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SampleServletBase.java,v 1.2 2008/06/25 05:52:10 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock AS.
*/

package com.sun.identity.agents.sample;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

public abstract class SampleServletBase extends HttpServlet {

    public void doGet(HttpServletRequest request, 
        HttpServletResponse response) 
        throws ServletException, IOException
    {
        processRequest(request, response);
    }

    public void doPost(HttpServletRequest request, 
        HttpServletResponse response)
        throws ServletException, IOException 
    {
        processRequest(request, response);
    }

    public abstract void processRequest(
        HttpServletRequest request, 
        HttpServletResponse response)
        throws ServletException, IOException;
}
