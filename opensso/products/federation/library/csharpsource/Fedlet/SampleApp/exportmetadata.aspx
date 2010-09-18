<%--
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2010 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: exportmetadata.aspx,v 1.1 2010/01/12 18:04:55 ggennaro Exp $
 */
--%>
<%@ Page Language="C#" %>
<%@ Import Namespace="System.Security.Cryptography" %>
<%@ Import Namespace="System.Security.Cryptography.X509Certificates" %>
<%@ Import Namespace="System.Net" %>
<%@ Import Namespace="System.Xml" %>
<%@ Import Namespace="Sun.Identity.Saml2" %>
<%@ Import Namespace="Sun.Identity.Saml2.Exceptions" %>
<%
    /*
     * Exports the metadata for the hosted service provider (aka .NET Fedlet)
     * 
     * Following are the list of supported query parameters:
     * 
     * Query Parameter    Description
     * ---------------    -----------
     * sign               Sign the metadata with the specified signing 
     *                    certificate.  Defaults to "false" since siging
     *                    certificates are not, by default, configured.
     */

    try
    {
        ServiceProviderUtility serviceProviderUtility = (ServiceProviderUtility)Cache["spu"];
        if (serviceProviderUtility == null)
        {
            serviceProviderUtility = new ServiceProviderUtility(Context);
            Cache["spu"] = serviceProviderUtility;
        }

        bool signMetadata = Saml2Utils.GetBoolean(Request.Params["sign"]);
        
        Response.ContentType = "text/xml";
        Response.Write(serviceProviderUtility.ServiceProvider.GetExportableMetadata(signMetadata));
    }
    catch (ServiceProviderException spue)
    {
        Response.StatusCode = (int) HttpStatusCode.InternalServerError;
        Response.StatusDescription = spue.Message;
        Response.End();
    }
%>