<%--
/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All Rights Reserved
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
 *
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
        AttributeQueryResponse queryResponse = null;
        ServiceProviderUtility serviceProviderUtility = (ServiceProviderUtility)Cache["spu"];
        if (serviceProviderUtility == null)
        {
            serviceProviderUtility = new ServiceProviderUtility(Context);
            Cache["spu"] = serviceProviderUtility;
        }

        NameValueCollection parameters = Saml2Utils.GetRequestParameters(Request);
        string idpEntityId = parameters[Saml2Constants.IdpEntityId];

        try
        {
            // Check for required parameters...
            if (String.IsNullOrEmpty(idpEntityId))
            {
                throw new ServiceProviderUtilityException("IDP Entity ID not specified.");
            }
            else if (String.IsNullOrEmpty(parameters[Saml2Constants.SubjectNameId]))
            {
                throw new ServiceProviderUtilityException("SubjectNameId not specified.");
            }

            List<SamlAttribute> attributes = new List<SamlAttribute>();

            if (!String.IsNullOrEmpty(parameters["attr1"]))
            {
                attributes.Add(new SamlAttribute(parameters["attr1"]));
            }
            if (!String.IsNullOrEmpty(parameters["attr2"]))
            {
                attributes.Add(new SamlAttribute(parameters["attr2"]));
            }
            if (!String.IsNullOrEmpty(parameters["attr3"]))
            {
                attributes.Add(new SamlAttribute(parameters["attr3"]));
            }
            if (attributes.Count == 0)
            {
                throw new ServiceProviderUtilityException("No Attributes specified.");
            }

            if (!String.IsNullOrEmpty(parameters["attr4"]))
            {
                parameters.Add(Saml2Constants.X509SubjectName, Boolean.TrueString);
                parameters[Saml2Constants.SubjectNameId] = parameters["attr4"];
            }

            queryResponse = serviceProviderUtility.SendAttributeQueryRequest(Context, idpEntityId, parameters, attributes);

            if (queryResponse.IsEncrypted())
            {
                queryResponse.Decrypt(serviceProviderUtility.ServiceProvider);
            }

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
    <h1>Fedlet Attribute Query Response</h1><hr/>
    <p></p>
    <% if (errorMessage != null)
       { %>
    <p>
        Attribute Query error:
    </p>
    <div class="code">
        <%=Server.HtmlEncode(errorMessage) %><br />
        <%=Server.HtmlEncode(errorTrace) %>
    </div>

    <% }
       else
       { %>
    <table class="samlAttributes">
        <tr>
            <th>key</th>
            <th>value(s)</th>
        </tr>
        <%
           if (queryResponse.Attributes.Count == 0)
           {
               Response.Write("<tr>\n");
               Response.Write("  <td colspan='2'><i>No attributes found in the response</i></td>\n");
               Response.Write("</tr>\n");
           }
           else
           {
               foreach (string key in queryResponse.Attributes.Keys)
               {
                   ArrayList values = (ArrayList)queryResponse.Attributes[key];

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

    <%
           if (queryResponse.Attributes.Count == 0)
           {
    %>
    <p>SAML2 response XML:</p>
    <textarea rows="9" cols="120">
        <%StringWriter stringWriter = new StringWriter();
          XmlTextWriter xmlWriter = new XmlTextWriter(stringWriter);
          XmlDocument xml = (XmlDocument)queryResponse.XmlDom;
          xml.WriteTo(xmlWriter);
          Response.Write(Server.HtmlEncode(stringWriter.ToString()));%><br />
    </textarea>
    <% 
           }
    %>

    <% } %>
    <p>
        Return <a href="javascript:history.go(-1)">back</a> to try another set of Attributes.
    </p>
</asp:Content>
