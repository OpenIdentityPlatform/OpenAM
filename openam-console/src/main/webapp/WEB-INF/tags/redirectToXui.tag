<%@ tag pageEncoding="UTF-8" %>
<%@ tag import="com.sun.identity.console.XuiRedirectHelper" %>
<%--
  ~ The contents of this file are subject to the terms of the Common Development and
  ~ Distribution License (the License). You may not use this file except in compliance with the
  ~ License.
  ~
  ~ You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
  ~ specific language governing permission and limitations under the License.
  ~
  ~ When distributing Covered Software, include this CDDL Header Notice in each file and include
  ~ the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
  ~ Header, with the fields enclosed by brackets [] replaced by your own identifying
  ~ information: "Portions copyright [year] [name of copyright owner]".
  ~
  ~ Copyright 2015-2016 ForgeRock AS.
  --%>
<%@ tag description="Replaces the current window location with the XUI page for the realm, or Home if XUI is disabled" %>
<%@ attribute name="xuiPath" type="java.lang.String" required="true" description="The path to the XUI page to redirect to" %>
<%@ attribute name="realm" type="java.lang.String" required="true" description="The realm to go back to, or empty string" %>

<script language="javascript">

    function redirectToXui() {
        var realm = ${realm};
        var template = <%= XuiRedirectHelper.isXuiAdminConsoleEnabled() ? "'" + xuiPath + "'" : "'../task/Home'" %>;
        document.location.replace(template.replace("{realm}", realm));
    }

</script>