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
/*
 * Portions Copyrighted 2013 ForgeRock Inc.
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

    <h1>Fedlet Single Sign On Results</h1><hr/>
    <p>
    Once succesfully authenticated by your OpenAM deployment, your browser was redirected
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
        
        foreach (XmlNode node in idp.SingleLogOutServiceLocations)
        {
            string location = node.Attributes["Location"].Value;
            if (location != null)
            {
                UriBuilder uri = new UriBuilder(location);
                if (uri != null)
                {
                    string[] v = uri.Path.Split('/');
                    if (v != null && location.Contains("metaAlias") && v.Length > 2)
                    {
                        idpDeployment = uri.Scheme + "://" + uri.Host + (uri.Port > 0 ? ":" + uri.Port : "") + "/" + v[1];
                        idpMetaAlias = "/" + v[v.Length - 1];
                        break;
                    }
                }
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
    <h1>Fedlet Attribute Query</h1><hr/>
    <p>Use this form to perform <b>Attribute Query</b> with <b><%=idp.EntityId %></b>:</p>
    <form id="frm_post" action="attrqueryres.aspx" method="POST">
        <table border="0">
            <tbody>
                <tr>
                    <td colspan="2"><b>Subject:</b></td>
                </tr>
                <tr>
                    <td colspan="2">SAML2 Token (Transient)</td>
                </tr>
                <tr>
                    <td>Attribute 1:</td>
                    <td>
                        <input id="Text1" type="text" name="attr1" value="CommonName" size="50" /></td>
                </tr>
                <tr>
                    <td>Attribute 2:</td>
                    <td>
                        <input id="Text2" type="text" name="attr2" value="EmailAddress" size="50" /></td>
                </tr>
                <tr>
                    <td>Attribute 3:</td>
                    <td>
                        <input id="Text3" type="text" name="attr3" value="UserStatus" size="50" /></td>
                </tr>
                <tr>
                    <td><b>Profile Name:</b></td>
                    <td><i>will use the Default when no X.509 Subject DN value below is entered</i></td>
                </tr>
                <tr>
                    <td>X.509 Subject DN:</td>
                    <td>
                        <input id="Text4" type="text" name="attr4" value="" size="100" /></td>
                </tr>
                <tr>
                    <td></td>
                    <td>
                        <input id="Text6" type="hidden" name="idpEntityID" value="<%=idp.EntityId %>" />
                        <input id="Text7" type="hidden" name="SubjectNameId" value="<%=authnResponse.SubjectNameId %>" />
                        <input id="Text5" type="submit" value="send" /></td>
                </tr>
            </tbody>
        </table>
        
    </form>
    
    <h1>Fedlet Single Log Out</h1><hr/>
    <p>Use one of the links below to perform <b>Single Log Out</b> with <b><%=idp.EntityId %></b>:</p>
        <ul>
            <%=sloListItems.ToString() %>
        </ul>
        
    <% } %>

    <br/>
    <p>
    Return to the <a href="default.aspx">homepage</a> to try other examples available in this sample application.
    </p>

</asp:Content>
