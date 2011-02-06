<!--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
  
   The contents of this file are subject to the terms
   of the Common Development and Distribution License
   (the License). You may not use this file except in
   compliance with the License.


   You can obtain a copy of the License at
   https://opensso.dev.java.net/public/CDDLv1.0.html or
   opensso/legal/CDDLv1.0.txt
   See the License for the specific language governing
   permission and limitations under the License.


   When distributing Covered Code, include this CDDL
   Header Notice in each file and include the License file
   at opensso/legal/CDDLv1.0.txt.
   If applicable, add the following below the CDDL Header,
   with the fields enclosed by brackets [] replaced by
   your own identifying information:
   "Portions Copyrighted [year] [name of copyright owner]"

   $Id: home.jsp,v 1.7 2009/08/01 00:21:52 sean_brydon Exp $

-->

<%@ include file="init.jspf" %>

<html>
<head>
<title>Book Flight With Great Air</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />
</head>
<body>

<%@ include file="header.jspf" %>

<p>&nbsp;</p>
&lt; <a href="Readme.html">SAMLv2 Sample Page</a>
<p>&nbsp;</p>                                                                                
    <h3><center><%= myTitle%> 
            appreciates your business<%= userLoggedIn ? ", " + userLabel : ""%></center></h3>
    <hr/>
    <table cellpadding="2" cellspacing="2" border="0" width="100%">
    <tr>
    <td valign="top" align="left">
    <% if(iAmIdp) { %>  
    Followings are the tasks that can be performed on the Identity Provider:
    <% } else { %>  
    Followings are the tasks that can be performed on the Service Provider:
    <% } %>
    </td>
    </tr>
    <tr>
    <td valign="top" align="left">  </td>
    </tr>
    <tr>
    <!-- Login/Logout prompt -->
    <td valign="top" align="left">
      <ul>
        <li>
        <% if(!userLoggedIn) { %>   <!-- user not logged in -->
                    <% if(iAmIdp) { %>      <!-- not logged in, i am idp -->
                            <a href="<%= localLoginUrl %>?goto=<%= thisUrl %>">
                                Local Login</a>
                    <% } else { %>          <!-- not logged in, i am sp -->
                            <a href="<%= appBase %>spssoinit?metaAlias=<%= myMetaAlias %>&idpEntityID=<%= partnerEntityID %>&<%= SAML2Constants.BINDING %>=HTTP-Artifact&RelayState=<%= thisUrl %>">
                            SAMLv2 Login through IDP, secure service provided by <%=  idpTitle%></a>
                         </li>                        
                         <li>                           
                            <a href="<%= localLoginUrl %>?goto=<%= thisUrl %>">
                                Local Login</a>
                    <% } %>
        <%  } else { %>             <!-- user logged in -->
                    <% if(iAmIdp) { %>      <!-- logged in, i am idp -->
                            <a href="<%= appBase %>IDPSloInit?<%= SAML2Constants.BINDING %>=<%= SAML2Constants.HTTP_REDIRECT %>&RelayState=<%= thisUrl %>">
                               SAMLv2 Logout</a>
                    <% } else { %>          <!-- logged in, i am sp -->
                            <% if(isLocalLogin) { %>                           
                                <a href="<%= localLogoutUrl %>?goto=<%= thisUrl %>">
                                    Local Logout</a>
                         </li>
                         <li>
                                <a href="<%= appBase %>spssoinit?metaAlias=<%= myMetaAlias %>&idpEntityID=<%= partnerEntityID %>&<%= SAML2Constants.BINDING %>=HTTP-Artifact&RelayState=<%= thisUrl %>">
                                   SAMLv2 Login through IDP, secure service provided by <%=  idpTitle%></a>                                  
                            <% } else { %> 
                                <a href="<%= appBase %>SPSloInit?idpEntityID=<%= partnerEntityID %>&<%= SAML2Constants.BINDING %>=<%= SAML2Constants.HTTP_REDIRECT %>&RelayState=<%= thisUrl %>">
                                SAMLv2 Logout</a>
                            <% } %>                               
                                
                    <% } %>
        <%  } %>
        </li>

            <!-- Federate/Defederate prompt only if user is logged in -->
                <% if(userLoggedIn) { %>             <!-- user logged in -->
                    <% if(federatedWithPartner) { %> <!-- federated -->
                        <% if(iAmIdp) { %>           <!-- federated, i am idp -->
            <li>
                                <a href="<%= appBase %>IDPMniInit?metaAlias=<%= myMetaAlias %>&spEntityID=<%= partnerEntityID %>&requestType=Terminate&RelayState=<%= thisUrl %>">
                                    Terminate Federation with <%= partnerTitle %></a>
            </li>
                        <% } else { %>               <!-- federated, i am sp -->
            <li>
                                <a href="<%= appBase %>SPMniInit?metaAlias=<%= myMetaAlias %>&idpEntityID=<%= partnerEntityID %>&requestType=Terminate&RelayState=<%= thisUrl %>">
                                    Terminate Federation with <%= partnerTitle %></a>
            </li>
                        <% } %>
                <%  } else if(iAmIdp) { %>           <!-- not federated, i am idp -->
            <li>
                                <a href="<%= appBase %>idpssoinit?metaAlias=<%= myMetaAlias %>&spEntityID=<%= partnerEntityID %>&<%= SAML2Constants.BINDING %>=<%= SAML2Constants.HTTP_ARTIFACT %>&RelayState=<%= thisUrl %>">
                                    Federate with <%= partnerTitle %></a>
            </li>
                        <% } else { %>               <!-- not federated, i am sp -->
            <li>
                                <a href="<%= appBase %>spssoinit?metaAlias=<%= myMetaAlias %>&idpEntityID=<%= partnerEntityID %>&<%= SAML2Constants.BINDING %>=HTTP-Artifact&RelayState=<%= thisUrl %>">
                                    Federate with <%= partnerTitle %></a>
            </li>
                        <% } %>
                <%  } %>

        <!-- links to hosted pages and pages hosted by partner -->
        <% if (userLoggedIn) { %>   <!-- user logged in -->
            <% if (iAmIdp) { %>     <!-- logged in, i am idp -->
            <li>
                        <a href="reserveFlight.jsp">
                            Reserve Flight with us, <%= myTitle %>
                        </a>
            </li>
            <li>
                        <a href="<%= appBase %>idpssoinit?metaAlias=<%= myMetaAlias %>&spEntityID=<%= partnerEntityID %>&<%= SAML2Constants.BINDING %>=<%= SAML2Constants.HTTP_ARTIFACT %>&RelayState=<%= reserveCarWithPartnerUrl %>">
                            Reserve Car with our associate, <%= partnerTitle %>
                        </a>
            </li>
            <% } else {%>           <!-- logged in, i am sp -->
            <li>
                        <a href="<%= reserveCarUrl %>">
                            Reserve Car with us, <%= myTitle %>
                        </a>
            </li>
            <% } %>
        <% } else { %>              <!-- user not logged in -->
            <% if (iAmIdp) { %>     <!-- not logged in, i am idp -->
            <li>
                        <a href="<%= localLoginUrl %>?goto=<%= reserveFlightUrl %>">
                            Reserve Flight with us, <%= myTitle %>
                        </a>
            </li>
            <li>
                        <a href="<%= appBase %>idpssoinit?metaAlias=<%= myMetaAlias %>&spEntityID=<%= partnerEntityID %>&<%= SAML2Constants.BINDING %>=<%= SAML2Constants.HTTP_ARTIFACT %>&RelayState=<%= reserveCarWithPartnerUrl %>">
                            Reserve Car with our associate, <%= partnerTitle %>
                        </a>
            </li>
            <% } else {%>           <!-- not logged in, i am sp -->
            <li>
                        <a href="reserveCar.jsp">
                            <a href="<%= appBase %>spssoinit?metaAlias=<%= myMetaAlias %>&idpEntityID=<%= partnerEntityID %>&<%= SAML2Constants.BINDING %>=HTTP-Artifact&RelayState=<%= reserveCarUrl %>">
                                Reserve Car with us, <%= myTitle %>
                            </a>
            </li>
            <% } %>
        <% } %>

    <!-- show link to partner sample home -->
    <% if (partnerSampleHomeUrl != null) { %> 
            <li>
                    <a href="<%= partnerSampleHomeUrl %>">
                            <%= partnerTitle %> Sample Home</a> 
            </li>
    <% } %>
        </td>
      </tr>
    </table>

</body>
</html>
