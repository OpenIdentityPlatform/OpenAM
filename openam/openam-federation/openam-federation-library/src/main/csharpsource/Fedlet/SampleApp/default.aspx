<%--
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
 * $Id: default.aspx,v 1.6 2009/11/13 18:23:36 ggennaro Exp $
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc.
 */
--%>
<%@ Page Language="C#" MasterPageFile="~/site.master"%>
<%@ Import Namespace="System.Xml" %>
<%@ Import Namespace="Sun.Identity.Saml2" %>
<%@ Import Namespace="Sun.Identity.Saml2.Exceptions" %>
<asp:Content ID="Content1" ContentPlaceHolderID="content" runat="server">
<%
    string fedletUrl = Request.Url.AbsoluteUri;
    fedletUrl = fedletUrl.Substring(0, fedletUrl.LastIndexOf("/")) + "/fedletapplication.aspx";
%>

    <h1>Sample Application with OpenAM and ASP.NET</h1>
    <p>
    This sample application demonstrates a simple Fedlet with OpenAM and
    a .NET application. Please be sure to follow the instructions of the
    README file to ensure your sample application will function correctly.
    </p>

    <h2>Based on the README file, you should have...</h2>
    <ol class="instructions">
        <li>A Circle of Trust within your OpenAM deployment.</li>
        <li>A hosted Identity Service Provider within your OpenAM deployment.</li>
        <li>
            This sample application configured with metadata edited appropriately
            and placed into this application's <span class="resource">App_Data/</span>
            folder.
            <ol class="summary">
                <li>
                    The HTTP-POST service location should have been edited appropriately
                    within your OpenAM deployment for this Service Provider.<br />
                    For example:
                    <span class="resource"><%=Server.HtmlEncode(fedletUrl)%></span>
                </li>
                <li>
                    Optionally added attribute mappings to be passed within the assertion
                    to this sample application.
                </li>
            </ol>
        </li>
        <li>
            Placed the <span class="resource">Fedlet.dll</span> within this 
            application's <span class="resource">Bin/</span> folder.
        </li>
    </ol>

    <h2>To try it out...</h2>

    <%
        StringBuilder idpListItems = new StringBuilder();
        string idpListItemFormat = "<li><a href=\"{0}\">Run Identity Provider initiated Single Sign-On using {1} binding</a></li>";
        string idpUrlFormat = "{0}/idpssoinit?NameIDFormat=urn:oasis:names:tc:SAML:2.0:nameid-format:transient&metaAlias={1}&spEntityID={2}&binding={3}";
        
        StringBuilder spListItems = new StringBuilder();
        string spListItemFormat = "<li><a href=\"{0}\">Run Fedlet (SP) initiated Single Sign-On using {1} binding</a></li>";
        string spUrlFormat = "spinitiatedsso.aspx?idpEntityId={0}&binding={1}";
        
        string errorMessage = null;
        bool hasMultipleIdps = false;
        bool spSupportsArtifact = false;
        bool spSupportsPost = false;

        try
        {
            ServiceProviderUtility serviceProviderUtility;

            serviceProviderUtility = (ServiceProviderUtility)Cache["spu"];
            if (serviceProviderUtility == null)
            {
                serviceProviderUtility = new ServiceProviderUtility(Context);
                Cache["spu"] = serviceProviderUtility;
            }

            hasMultipleIdps = (serviceProviderUtility.IdentityProviders.Count > 1);
            spSupportsPost = !string.IsNullOrEmpty(serviceProviderUtility.ServiceProvider.GetAssertionConsumerServiceLocation(Saml2Constants.HttpPostProtocolBinding));
            spSupportsArtifact = !string.IsNullOrEmpty(serviceProviderUtility.ServiceProvider.GetAssertionConsumerServiceLocation(Saml2Constants.HttpArtifactProtocolBinding));

            foreach (string idpEntityId in serviceProviderUtility.IdentityProviders.Keys)
            {
                IdentityProvider idp = (IdentityProvider) serviceProviderUtility.IdentityProviders[idpEntityId];

                // create the idp initiated links
                string deploymentUrl = null;
                string metaAlias = null;
                
                foreach (XmlNode node in idp.SingleSignOnServiceLocations)
                {
                    string binding = node.Attributes["Binding"].Value;
                    string location = node.Attributes["Location"].Value;
                    if (binding != null && location != null)
                    {
                        UriBuilder uri = new UriBuilder(location);
                        if (uri != null)
                        {
                            string[] v = uri.Path.Split('/');
                            if (v != null && location.Contains("metaAlias") && v.Length > 2)
                            {
                                deploymentUrl = uri.Scheme + "://" + uri.Host + (uri.Port > 0 ? ":" + uri.Port : "") + "/" + v[1];
                                metaAlias = "/" + v[v.Length - 1];
                                break;
                            }
                        }
                    }
                }

                if (!string.IsNullOrEmpty(deploymentUrl) && !string.IsNullOrEmpty(metaAlias))
                {
                    string idpListItem;
                    string idpUrl;

                    idpListItems.Append("<p>Using <b>" + Server.HtmlEncode(idp.EntityId) + "</b>:</p>");
                    idpListItems.Append("<ul>");
                    
                    idpUrl = Server.HtmlEncode(
                                            string.Format(
                                                idpUrlFormat,
                                                deploymentUrl,
                                                metaAlias,
                                                serviceProviderUtility.ServiceProvider.EntityId,
                                                Saml2Constants.HttpPostProtocolBinding));
                    idpListItem = string.Format(idpListItemFormat, idpUrl, "HTTP POST");
                    idpListItems.Append(idpListItem);

                    idpUrl = Server.HtmlEncode(
                                            string.Format(
                                                idpUrlFormat,
                                                deploymentUrl,
                                                metaAlias,
                                                serviceProviderUtility.ServiceProvider.EntityId,
                                                Saml2Constants.HttpArtifactProtocolBinding));
                    idpListItem = string.Format(idpListItemFormat, idpUrl, "HTTP Artifact");
                    idpListItems.Append(idpListItem);

                    idpListItems.Append("</ul>");
                }
                
                // create the sp initiated links
                if (spSupportsPost)
                {
                    string spUrl = Server.HtmlEncode(
                                                string.Format(
                                                        spUrlFormat, 
                                                        idp.EntityId, 
                                                        Saml2Constants.HttpPostProtocolBinding));
                    string spListItem = string.Format(spListItemFormat, spUrl, "HTTP POST");
                    spListItems.Append(spListItem);
                }
                if (spSupportsArtifact)
                {
                    string spUrl = Server.HtmlEncode(
                                        string.Format(
                                                spUrlFormat, 
                                                idp.EntityId, 
                                                Saml2Constants.HttpArtifactProtocolBinding));
                    string spListItem = string.Format(spListItemFormat, spUrl, "HTTP Artifact");
                    spListItems.Append(spListItem);
                }
            }

            if (idpListItems.Length == 0)
            {
                idpListItems.Append("<li>IDP initiated SSO is currently only supported with an OpenAM deployment.</li>");
            }
            if (spListItems.Length == 0)
            {
                spListItems.Append("<li>SP initiated SSO requires either HTTP-POST or HTTP-Artifact assertion consumer service locations to be configured.</li>");
            }
        }
        catch (ServiceProviderUtilityException spue)
        {
            errorMessage = spue.Message;
        }
    %>

    <% if( errorMessage == null ) { %>
    
        <p>
        Perform the IDP initiated Single Sign On to take you to the OpenAM login form. 
        Upon successfull login, you will be taken to the location configured for your Fedlet
        for this sample application.  
        </p>
        
        <%=idpListItems.ToString()%>
        
        <p>
        Alternatively, you can perform SP initiated Single Sign On with the link(s) provided
        below.
        </p>
        
        <ul>
        <%=spListItems.ToString()%>
        </ul>
        
        <% if( hasMultipleIdps ) { %>
            <p>
            Since you have multiple identity providers specified, you can optionally
            <a href="spinitiatedsso.aspx">use the IDP Discovery Service</a> 
            to perform Single Sign On with your preferred IDP if you have specified 
            the reader service within your circle-of-trust file.
            </p>
        <% } %>

        <p>
        The above demonstrates how a .NET developer could issue a redirect
        to non-authenticated users from their .NET application to the OpenAM 
        login page for authentication.
        </p>
    
    <% } else { %>
    
        <p>
        Please be sure to follow the instructions within the README file as well as review the 
        information above.  
        </p>
        <p>
        The following error was encountered:<br />
        <%=errorMessage %>
        </p>
    
    <% } %>


</asp:Content>
