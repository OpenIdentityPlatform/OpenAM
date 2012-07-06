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
 * $Id: fedletapplication.aspx,v 1.6 2009/11/12 00:36:50 ggennaro Exp $
 */
--%>
<%@ Page Language="C#" MasterPageFile="~/site.master" %>
<%@ Import Namespace="System.IO" %>
<%@ Import Namespace="System.Xml" %>
<%@ Import Namespace="Sun.Identity.Saml2" %>
<%@ Import Namespace="Sun.Identity.Saml2.Exceptions" %>

<asp:Content ID="Content1" ContentPlaceHolderID="content" runat="server">

    <%
        string errorMessage = null;
        string errorTrace = null;
        AuthnResponse authnResponse = null;
        ServiceProviderUtility serviceProviderUtility = null;

        try
        {
            serviceProviderUtility = (ServiceProviderUtility)Cache["spu"];
            if (serviceProviderUtility == null)
            {
                serviceProviderUtility = new ServiceProviderUtility(Context);
                Cache["spu"] = serviceProviderUtility;
            }

            authnResponse = serviceProviderUtility.GetAuthnResponse(Context);
        }
        catch (Saml2Exception se)
        {
            errorMessage = se.Message;
            errorTrace = se.StackTrace;
            if (se.InnerException != null)
                errorTrace += "<br/>" + se.InnerException.StackTrace;
        }
        catch (ServiceProviderUtilityException spue)
        {
            errorMessage = spue.Message;
            errorTrace = spue.StackTrace;
            if (spue.InnerException != null)
                errorTrace += "<br/>" + spue.InnerException.StackTrace;
        }
    %>

    <h1>Sample Application with OpenSSO and ASP.NET</h1>
    <p>
    Once succesfully authenticated by your OpenSSO deployment, your browser was redirected
    to this location with a SAML response. This response can be consumed as follows:
    </p>

    <div class="code">
    AuthnResponse authnResponse = null;
    try
    {
        ServiceProviderUtility serviceProviderUtility = new ServiceProviderUtility(Context);
        authnResponse = serviceProviderUtility.GetAuthnResponse(Context);
    }
    catch (Saml2Exception se)
    {
        // invalid AuthnResponse received
    }
    catch (ServiceProviderUtilityException spue)
    {
        // issues with deployment (reading metadata)
    }
    </div>
    
    <% if (errorMessage != null) { %>
        <p>
        However, an error occured:
        </p>
<div class="code">
<%=Server.HtmlEncode(errorMessage) %><br />
<%=Server.HtmlEncode(errorTrace) %>
</div>

    <% } else { %>
        <p>
        Once the <span class="resource">AuthnResponse</span> object has been retrieved, you could
        easily access attributes from the response as demonstrated below:
        </p>
        
        <table class="output">
        <tr>
            <th>Method</th>
            <th>Returns</th>
            <th>Output</th>
        </tr>
        <tr>
            <td>authnResponse.XmlDom</td>
            <td>System.Xml.XPath.IXPathNavigable</td>
            <td>
                <form action="javascript:void();" method="get">
                <textarea rows="5" cols="60"><%
                    StringWriter stringWriter = new StringWriter();
                    XmlTextWriter xmlWriter = new XmlTextWriter(stringWriter);
                    XmlDocument xml = (XmlDocument)authnResponse.XmlDom;
                    xml.WriteTo(xmlWriter);
                    Response.Write(Server.HtmlEncode(stringWriter.ToString()));
                %></textarea>
                </form>
            </td>
        </tr>
        <tr>
            <td>authnResponse.SubjectNameId</td>
            <td>System.String</td>
            <td><%=Server.HtmlEncode(authnResponse.SubjectNameId)%></td>
        </tr>
        <tr>
            <td>authnResponse.SessionIndex</td>
            <td>System.String</td>
            <td><%=Server.HtmlEncode(authnResponse.SessionIndex)%></td>
        </tr>
        <tr>
            <td>authnResponse.Attributes</td>
            <td>System.Collections.Hashtable</td>
            <td>
                <table class="samlAttributes">
                <tr>
                  <th>key</th>
                  <th>value(s)</th>
                </tr>
                <%
                    if (authnResponse.Attributes.Count == 0)
                    {
                        Response.Write("<tr>\n");
                        Response.Write("  <td colspan='2'><i>No attributes found in the response</i></td>\n");
                        Response.Write("</tr>\n");
                    }
                    else
                    {
                        foreach (string key in authnResponse.Attributes.Keys)
                        {
                            ArrayList values = (ArrayList)authnResponse.Attributes[key];

                            Response.Write("<tr>\n");
                            Response.Write("<td>" + Server.HtmlEncode(key) + "</td>\n");
                            Response.Write("<td>\n");
                            foreach (string value in values)
                            {
                                Response.Write(Server.HtmlEncode(value) + "<br/>\n");
                            }
                            Response.Write("</td>\n");
                            Response.Write("</tr>\n");
                        }
                    }
                %>
                </table>
            </td>
        </tr>
        </table>
        
<%
    string fedletUrl = Request.Url.AbsoluteUri.Substring(0, Request.Url.AbsoluteUri.LastIndexOf("/") + 1);
    Hashtable identityProviders = serviceProviderUtility.IdentityProviders;
    IdentityProvider idp = (IdentityProvider)identityProviders[authnResponse.Issuer];
    StringBuilder sloListItems = new StringBuilder();
    string sloListItemFormat = "<li><a href=\"{0}\">Run {1} initiated Single Logout using {2} binding</a></li>";

    if (idp != null)
    {
        string idpDeployment = null;
        string idpMetaAlias = null;
        string pattern = "(.+?/opensso).+?/metaAlias(.+?)$";
        Match m = null;

        foreach (XmlNode node in idp.SingleLogOutServiceLocations)
        {
            string location = node.Attributes["Location"].Value;
            if (location != null)
            {
                m = Regex.Match(location, pattern);
                if (m.Success && m.Groups.Count == 3)
                {
                    idpDeployment = m.Groups[1].Value;
                    idpMetaAlias = m.Groups[2].Value;
                }
                break;
            }
        }

        if (!String.IsNullOrEmpty(idpDeployment) && !String.IsNullOrEmpty(idpMetaAlias))
        {
            string idpUrlFormat = "{0}/IDPSloInit?metaAlias={1}&binding={2}&RelayState={3}";
            string idpUrl = string.Empty;

            idpUrl = Server.HtmlEncode(String.Format(idpUrlFormat, idpDeployment, idpMetaAlias, Saml2Constants.HttpRedirectProtocolBinding, fedletUrl));
            sloListItems.Append(String.Format(sloListItemFormat, idpUrl, "Identity Provider", "HTTP Redirect"));
            idpUrl = Server.HtmlEncode(String.Format(idpUrlFormat, idpDeployment, idpMetaAlias, Saml2Constants.HttpPostProtocolBinding, fedletUrl));
            sloListItems.Append(String.Format(sloListItemFormat, idpUrl, "Identity Provider", "HTTP POST"));
            idpUrl = Server.HtmlEncode(String.Format(idpUrlFormat, idpDeployment, idpMetaAlias, Saml2Constants.HttpSoapProtocolBinding, fedletUrl));
            sloListItems.Append(String.Format(sloListItemFormat, idpUrl, "Identity Provider", "SOAP"));
        } 
    }

    string spUrlFormat = "spinitiatedslo.aspx?idpEntityID={0}&SubjectNameId={1}&SessionIndex={2}&binding={3}&RelayState={4}";
    string spUrl = string.Empty;

    spUrl = Server.HtmlEncode(String.Format(spUrlFormat, idp.EntityId, authnResponse.SubjectNameId, authnResponse.SessionIndex, Saml2Constants.HttpRedirectProtocolBinding, fedletUrl));
    sloListItems.Append(String.Format(sloListItemFormat, spUrl, "Fedlet", "HTTP Redirect"));
    spUrl = Server.HtmlEncode(String.Format(spUrlFormat, idp.EntityId, authnResponse.SubjectNameId, authnResponse.SessionIndex, Saml2Constants.HttpPostProtocolBinding, fedletUrl));
    sloListItems.Append(String.Format(sloListItemFormat, spUrl, "Fedlet", "HTTP POST"));
    spUrl = Server.HtmlEncode(String.Format(spUrlFormat, idp.EntityId, authnResponse.SubjectNameId, authnResponse.SessionIndex, Saml2Constants.HttpSoapProtocolBinding, fedletUrl));
    sloListItems.Append(String.Format(sloListItemFormat, spUrl, "Fedlet", "SOAP"));

%>

        <p>Use one of the links below to perform Single Log Out with <b><%=idp.EntityId %></b>:</p>
        <ul>
            <%=sloListItems.ToString() %>
        </ul>
        
    <% } %>

    <p>
    Return to the <a href="default.aspx">homepage</a> to try other examples available in this sample application.
    </p>

</asp:Content>
