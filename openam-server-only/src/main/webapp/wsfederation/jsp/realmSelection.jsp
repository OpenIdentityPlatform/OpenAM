<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

   Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved

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

   $Id: realmSelection.jsp,v 1.10 2009/10/29 00:00:00 exu Exp $

    Portions Copyrighted 2013-2016 ForgeRock AS.
--%>

<%@page
    import="java.util.*"
    import="com.sun.identity.shared.debug.Debug"
    import="com.sun.identity.wsfederation.common.WSFederationConstants"
    import="com.sun.identity.wsfederation.common.WSFederationUtils"
    import="com.sun.identity.wsfederation.meta.WSFederationMetaManager"
    import="com.sun.identity.wsfederation.meta.WSFederationMetaUtils"
    import="com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement"
    import="com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement"
    import="org.owasp.esapi.ESAPI"
%>
<%
    Debug debug = WSFederationUtils.debug;
    String jspFile = "realmSelection.jsp: ";
    String wreply = (String)request.getParameter("wreply");
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + wreply, wreply,
        "URL", 2000, false)){
            wreply = "";
    }
    String wctx = (String)request.getParameter("wctx");
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + wctx, wctx,
        "HTTPParameterValue", 2000, true)){
            wctx = null;
    }

    if (debug.messageEnabled()) {
        debug.message(jspFile + "wreply: "+wreply);
    }

    String spMetaAlias = WSFederationMetaUtils.getMetaAliasByUri(
        request.getRequestURI());
    if ( spMetaAlias==null || spMetaAlias.length()==0) {
        response.sendError(response.SC_BAD_REQUEST, "Null metaAlias"
                /* TODO SAML2Utils.bundle.getString("nullSPEntityID") */);
        return;
    }

    WSFederationMetaManager metaManager =
        WSFederationUtils.getMetaManager();
    String spEntityId =
        metaManager.getEntityByMetaAlias(spMetaAlias);
    String spRealm = WSFederationMetaUtils.getRealmByMetaAlias(spMetaAlias);
    Map<String,List<String>> spConfig =
        WSFederationMetaUtils.getAttributes(
        metaManager.getSPSSOConfig(spRealm,spEntityId));
    String accountRealmCookieName =
        spConfig.get(WSFederationConstants.ACCOUNT_REALM_COOKIE_NAME).get(0);

    String selectedRealm = (String)request.getParameter("realm_list");
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + selectedRealm,
        selectedRealm, "HTTPParameterValue", 2000, true)){
            selectedRealm = null;
    }
    if (debug.messageEnabled()) {
        debug.message(jspFile + "Selected realm: " + selectedRealm);
    }
    if ( selectedRealm != null )
    {
        StringBuffer url = new StringBuffer(wreply);
        url.append("?whr=");
        url.append(selectedRealm);
        if (wctx != null) {
            url.append("&wctx=");
            url.append(wctx);
        }
        if (debug.messageEnabled()) {
            debug.message(jspFile + "Redirecting to: "+url);
        }
        response.sendRedirect(url.toString());
        return;
    }

    String contextPath = request.getContextPath();
%>
<html>
<head>
<title>OpenAM (Realm Selection)</title>
<link rel="stylesheet" href="<%= contextPath %>/css/styles.css" type="text/css" />
<script language="JavaScript" src="<%= contextPath %>/js/browserVersion.js"></script>
<script language="JavaScript" src="<%= contextPath %>/js/auth.js"></script>
<script language="JavaScript">

    writeCSS('<%= contextPath %>');

    function formSubmit() {
        var frm = document.forms['realm_form'];

        if (frm != null) {
            frm.submit();
        }
    }

</script>
<script type="text/javascript"><!--// Empty script so IE5.0 Windows will draw table and button borders
//-->
</script>
</head>

<body class="LogBdy" onload="placeCursorOnFirstElm();">

  <table border="0" cellpadding="0" cellspacing="0" align="center" title="">
    <tr>
      <td width="50%"><img src="<%= contextPath %>/images/dot.gif" width="1" height="1" alt="" /></td>
      <td><img src="<%= contextPath %>/images/dot.gif" width="728" height="1" alt="" /></td>
      <td width="50%"><img src="<%= contextPath %>/images/dot.gif" width="1" height="1" alt="" /></td>
    </tr>
    <tr class="LogTopBnd" style="background-image: url(<%= contextPath %>/images/gradlogtop.jpg);
    background-repeat: repeat-x; background-position: left top;">
      <td><img src="<%= contextPath %>/images/dot.gif" width="1" height="30" alt="" /></td>
      <td>&nbsp;</td>
    </tr>
    <tr>
      <td class="LogMidBnd" style="background-image: url(<%= contextPath %>/images/gradlogsides.jpg);
        background-repeat:repeat-x;background-position:left top;">&nbsp;</td>
      <td class="LogCntTd" style="background-image: url(<%= contextPath %>/images/login-backimage-long.jpg);
        background-repeat:no-repeat;background-position:left top;" height="435" align="center" valign="middle">
        <table border="0" background="<%= contextPath %>/images/dot.gif" cellpadding="0" cellspacing="0"
        width="100%" title="">
          <tr>
            <td width="260"><img src="<%= contextPath %>/images/dot.gif" width="260" height="245" alt="" /></td>
            <td width="415" bgcolor="#ffffff" valign="top">
        <form name="realm_form" action="<%=request.getRequestURI()%>"
            onSubmit="formSubmit(); return false;" method="post">
              <table border="0" cellspacing="0" cellpadding="0">
                <tr>
                  <td colspan="2">
		      <img src="<%= contextPath %>/images/dot.gif" width="1" height="25" alt="" />
		  </td>
                </tr>

        <!-- Header display -->

	<tr>
        <td nowrap="nowrap"></td>
        <td><div class="logTxtSvrNam">

	    Account Realm Selection

	</div></td>
	</tr>
	<!-- End of Header display -->

        <!-- text box display -->
	<tr>

	<td nowrap="nowrap"><div class="logLbl">

            <span class="LblLev2Txt">
            <label for="IDToken1">
                 &nbsp;
            </label></span></div>

	</td>

	<td><div class="logInp">

        <input type="hidden" name="wreply" value="<%=wreply%>" />
<%
        if ( wctx != null && wctx.length() > 0 ) {
%>
        <input type="hidden" name="wctx" value="<%=wctx%>" />
<%
        }
%>
        <select name="realm_list">
<%
            String accountRealmCookieValue = null;
            Cookie cookies[] = request.getCookies();
            if (cookies != null) {
              for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals( accountRealmCookieName ))
                {
                  accountRealmCookieValue = cookies[i].getValue();
                  break;
                }
              }
            }
            if (debug.messageEnabled()) {
                debug.message(jspFile + "Account Realm Cookie: " +
                    accountRealmCookieValue);
            }

            for (String idpEntityId :
                metaManager.getAllRemoteIdentityProviderEntities(spRealm))
            {
                FederationElement idp =
                    metaManager.getEntityDescriptor(spRealm,
                    idpEntityId);
                IDPSSOConfigElement idpconfig =
                    metaManager.getIDPSSOConfig(spRealm,
                    idpEntityId);

                if ( metaManager.isTrustedProvider(spRealm,
                    spEntityId, idpEntityId) ) {
                    String issuerName = metaManager.
                        getTokenIssuerName(idp);

                    String displayName =
                        WSFederationMetaUtils.getAttribute(idpconfig,
                        WSFederationConstants.DISPLAY_NAME);

                    if (debug.messageEnabled()) {
                        debug.message(jspFile + "account realm key: " +
                            issuerName + " display name: " + displayName);
                    }

                    if (displayName == null || displayName.length() == 0){
                        displayName = issuerName;
                    }
%>
                    <option value="<%=issuerName%>"
                        <%=((accountRealmCookieValue != null) &&
                        (accountRealmCookieValue.equals(issuerName))?"selected":"")%>>
                        <%=displayName%>
                    </option>
<%
                }
            }
%>
        </select>

                </div>
	</td>
	</tr>
	<!-- end of textBox -->
	<!-- Submit button -->
	<tr>
	<td><img src="<%= contextPath %>/images/dot.gif"
        width="1" height="15" alt="" /></td>
	    <script language="javascript">
		markupButton(
		    'Proceed',
		   	"javascript:formSubmit()");
	    </script>
	</tr>
	<!-- end of Submit button -->

        <tr>
            <td>&nbsp;</td>
        </tr>
	<tr>
            <td><img src="<%= contextPath %>/images/dot.gif"
            width="1" height="33" alt="" /></td>
	    <td>&nbsp;</td>
	</tr>
        </table>
        </form>
      </td>
      <td width="45"><img src="<%= contextPath %>/images/dot.gif"
      width="45" height="245" alt="" /></td>
    </tr>
    </table>
    </td>
    <td class="LogMidBnd" style="background-image: url(<%= contextPath %>/images/gradlogsides.jpg);
    background-repeat:repeat-x;background-position:left top;">&nbsp;</td>
    </tr>
    <tr class="LogBotBnd" style="background-image: url(<%= contextPath %>/images/gradlogbot.jpg);
    background-repeat:repeat-x;background-position:left top;">
      <td>&nbsp;</td>
      <td>
        <div class="logCpy"><span class="logTxtCpy">
          Copyright &copy; 2008-2016, ForgeRock AS. All Rights Reserved. Use of this software is subject to the terms and conditions of the ForgeRock&#8482; License and Subscription Agreement.
        </div>
      </td>
      <td>&nbsp;</td>
    </tr>
  </table>
</body>
</html>
