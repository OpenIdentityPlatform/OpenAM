<%@ page import="org.forgerock.oauth2.core.OAuth2Constants" %>
<%@ page import="com.sun.identity.idm.AMIdentity" %>
<%@ page import="com.iplanet.sso.SSOTokenManager" %>
<%@ page import="com.iplanet.sso.SSOToken" %>
<%@ page import="com.iplanet.am.util.SystemProperties" %>
<%@ page import="com.sun.identity.common.DNUtils" %>
<%@ page import="com.sun.identity.idm.IdType" %>
<%@ page import="com.iplanet.sso.SSOException" %>
<%
    String adminUserDN = "";
    AMIdentity adminUserId = null;
    try {
        SSOTokenManager sMgr = SSOTokenManager.getInstance();
        SSOToken ssoToken = sMgr.createSSOToken(request);

        // This will give you the 'amAdmin' user dn
        String adminUser = SystemProperties.get(
                "com.sun.identity.authentication.super.user");
        if (adminUser != null) {
            adminUserDN = DNUtils.normalizeDN(adminUser);
            // This will give you the 'amAdmin' Identity
            adminUserId = new AMIdentity(ssoToken, adminUser,
                    IdType.USER, "/", null);
        }

        // This will be your incoming user/token.
        AMIdentity user = new AMIdentity(ssoToken);

        if ((!adminUserDN.equals(DNUtils.normalizeDN(
                ssoToken.getPrincipal().getName()))) &&
                (!user.equals(adminUserId))) {

            out.println("You do not have the privilege to create a OAuth 2 client");
            return;
        }
    } catch (SSOException e) {
        response.sendRedirect("../UI/Login");
    }
%>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Client Registration</title>
    <script src="../js/jquery.js"></script>
	<script>
        $(document).ready(function () {

            //add buttons
            $('#add_url').click(function(){
                console.log("add_url Pressed");
                var value = $("input[id='<%=OAuth2Constants.OAuth2Client.REDIRECT_URI%>']").val();
                $("select[id='<%=OAuth2Constants.OAuth2Client.REDIRECT_URI%>']")
                        .append("<option value=\"" + value + "\">" + value + "</option>");
            });
            $('#add_scope').click(function(){
                console.log("add_scope Pressed");
                var value = $("input[id='<%=OAuth2Constants.OAuth2Client.SCOPES%>']").val();
                $("select[id='<%=OAuth2Constants.OAuth2Client.SCOPES%>']")
                        .append("<option value=\"" + value + "\">" + value + "</option>");
            });
            $('#add_default_scope').click(function(){
                console.log("add_default_scope Pressed");
                var value = $("input[id='<%=OAuth2Constants.OAuth2Client.DEFAULT_SCOPES%>']").val();
                $("select[id='<%=OAuth2Constants.OAuth2Client.DEFAULT_SCOPES%>']")
                        .append("<option value=\"" + value + "\">" + value + "</option>");
            });
            $('#add_display_name').click(function(){
                console.log("add_display_name Pressed");
                var value = $("input[id='<%=OAuth2Constants.OAuth2Client.NAME%>']").val();
                $("select[id='<%=OAuth2Constants.OAuth2Client.NAME%>']")
                        .append("<option value=\"" + value + "\">" + value + "</option>");
            });
            $('#add_dispaly_description').click(function(){
                console.log("add_dispaly_description Pressed");
                var value = $("input[id='<%=OAuth2Constants.OAuth2Client.DESCRIPTION%>']").val();
                $("select[id='<%=OAuth2Constants.OAuth2Client.DESCRIPTION%>']")
                        .append("<option value=\"" + value + "\">" + value + "</option>");
            });

            //remove buttons
            $('#remove_url').click(function(){
                console.log("remove_url Pressed");
                $("select[id='<%=OAuth2Constants.OAuth2Client.REDIRECT_URI%>'] option:selected").remove();
            });
            $('#remove_scope').click(function(){
                console.log("remove_scope Pressed");
                $("select[id='<%=OAuth2Constants.OAuth2Client.SCOPES%>'] option:selected").remove();
            });
            $('#remove_default_scope').click(function(){
                console.log("remove_default_scope Pressed");
                $("select[id='<%=OAuth2Constants.OAuth2Client.DEFAULT_SCOPES%>'] option:selected").remove();
            });
            $('#remove_display_name').click(function(){
                console.log("remove_display_name Pressed");
                $("select[id='<%=OAuth2Constants.OAuth2Client.NAME%>'] option:selected").remove();
            });
            $('#remove_display_description').click(function(){
                console.log("remove_display_description Pressed");
                $("select[id='<%=OAuth2Constants.OAuth2Client.DESCRIPTION%>'] option:selected").remove();
            });

              //form submit
            $('#client').submit(function(){
                /*
                Data should be in this form
                 {
                 "client_id":["testClient"],
                 "realm":["/"]
                 "userpassword":["secret12"],
                 "com.forgerock.openam.oauth2provider.clientType":["Confidential"],
                 "com.forgerock.openam.oauth2provider.redirectionURIs":
                     ["www.client.com","www.example.com"],
                 "com.forgerock.openam.oauth2provider.scopes":["cn","sn"],
                 "com.forgerock.openam.oauth2provider.defaultScopes":["cn"],
                 "com.forgerock.openam.oauth2provider.name":["My Test Client"],
                 "com.forgerock.openam.oauth2provider.description":["OAuth 2.0 Client"]
                 }
                 */
                var jsonData = {};
                console.log("client submit");
                var s = "<%=OAuth2Constants.OAuth2Client.USERPASSWORD%>".replace(/\./g, "\\\\.");
                var password1 = $("#" + s).val();
                s = s + "2";
                var password2 = $("#" + s).val();
                //password not the same
                if (password1 != password2){
                    $('#message').html("<b>Client password must be the same.<b><br><br>");
                    return false;
                }

                //get the options arrays
                var redirection_options = $("select[id='<%=OAuth2Constants.OAuth2Client.REDIRECT_URI%>']")
                        .prop('options');
                var scopes_options = $("select[id='<%=OAuth2Constants.OAuth2Client.SCOPES%>']")
                        .prop('options');
                var default_scope_options = $("select[id='<%=OAuth2Constants.OAuth2Client.DEFAULT_SCOPES%>']")
                        .prop('options');
                var display_names_options = $("select[id='<%=OAuth2Constants.OAuth2Client.NAME%>']")
                        .prop('options');
                var display_descriptions_options = $("select[id='<%=OAuth2Constants.OAuth2Client.DESCRIPTION%>']")
                        .prop('options');

                s = "<%=OAuth2Constants.OAuth2Client.CLIENT_ID%>".replace(/\./g, "\\\\.");
                var client_id = $("#" + s).val();
                if (client_id.length <= 0){
                    $('#message').html("<b>Client ID is required.<b><br><br>");
                    return false;
                }

                s = "<%=OAuth2Constants.OAuth2Client.REALM%>".replace(/\./g, "\\\\.");
                var realm = $("#" + s).val();
                if (realm.length <= 0){
                    $('#message').html("<b>Realm is required.<b><br><br>");
                    return false;
                }

                var clientType = $("input[name='<%=OAuth2Constants.OAuth2Client.CLIENT_TYPE%>']:checked").val();
                if (realm === 'undefined' || realm.length <= 0){
                    $('#message').html("<b>Client Type is required.<b><br><br>");
                    return false;
                }

                //add user, pass, and realm to the dat json object
                var temp = new Array();
                temp.push(client_id);
                jsonData["<%=OAuth2Constants.OAuth2Client.CLIENT_ID%>"] = temp;
                temp = new Array();
                temp.push(password1);
                jsonData["<%=OAuth2Constants.OAuth2Client.USERPASSWORD%>"] = temp;
                temp = new Array();
                temp.push(realm);
                jsonData["<%=OAuth2Constants.OAuth2Client.REALM%>"] = temp;
                temp = new Array();
                temp.push(clientType);
                jsonData["<%=OAuth2Constants.OAuth2Client.CLIENT_TYPE%>"] = temp;


                //get the selection options as a single string
                temp = new Array();
                for (var i= 0; i < redirection_options.length; i++){
                    temp.push(redirection_options[i].value);
                }
                jsonData["<%=OAuth2Constants.OAuth2Client.REDIRECT_URI%>"] = temp;

                temp = new Array();
                for (var i= 0; i < scopes_options.length; i++){
                    temp.push(scopes_options[i].value);
                }
                jsonData["<%=OAuth2Constants.OAuth2Client.SCOPES%>"] = temp;

                temp = new Array();
                for (var i= 0; i < default_scope_options.length; i++){
                    temp.push(default_scope_options[i].value);
                }
                jsonData["<%=OAuth2Constants.OAuth2Client.DEFAULT_SCOPES%>"] = temp;

                temp = new Array();
                for (var i= 0; i < display_names_options.length; i++){
                    temp.push(display_names_options[i].value);
                }
                jsonData["<%=OAuth2Constants.OAuth2Client.NAME%>"] = temp;

                temp = new Array();
                for (var i= 0; i < display_descriptions_options.length; i++){
                    temp.push(display_descriptions_options[i].value);
                }
                jsonData["<%=OAuth2Constants.OAuth2Client.DESCRIPTION%>"] = temp;

                $.ajax({
                    headers: {
                        Accept : "application/json; charset=utf-8",
                        "Content-Type": "application/json; charset=utf-8"
                    },
                    type: "POST",
                    url: "../frrest/oauth2/client/?_action=create",
                    cache: false,
                    data: JSON.stringify(jsonData)
                }).done(function( msg ) {
                    $('#message').html("<b>Client Created.<b><br><br>");
                }).fail(function( jqXHR, textStatus ) {
                    $('#message').html("<b>Client Creation Failed.<b><br><br>");
                });
                return false;
            });
        });
	</script>
</head>
<body>
	<h1>Register a Client</h1>
	<form name="client" action="" method="POST" id="client">
		<div id="message"></div>
        Realm:<br>
        <input type="input" name="<%=OAuth2Constants.OAuth2Client.REALM%>" value="" id="<%=OAuth2Constants.OAuth2Client.REALM%>"/>
        <br><br>
        Client ID:<br>
        <input type="input" name="<%=OAuth2Constants.OAuth2Client.CLIENT_ID%>" value="" id="<%=OAuth2Constants.OAuth2Client.CLIENT_ID%>"/>
        <br><br>
		Client Password:<br>
        <input type="password" name="<%=OAuth2Constants.OAuth2Client.USERPASSWORD%>" value="" id="<%=OAuth2Constants.OAuth2Client.USERPASSWORD%>"/>
        <br><br>
		Client Password: (confirm)<br>
        <input type="password" name="<%=OAuth2Constants.OAuth2Client.USERPASSWORD%>2" value="" id="<%=OAuth2Constants.OAuth2Client.USERPASSWORD%>2"/>
        <br><br>
		Client Type:<br>
        <input type="radio" name="<%=OAuth2Constants.OAuth2Client.CLIENT_TYPE%>" value="Confidential" checked /><label>Confidential</label><br>
	    <input type="radio" name="<%=OAuth2Constants.OAuth2Client.CLIENT_TYPE%>" value="Public"/><label>Public</label>
        <br><br>
		Redirection URL(s):<br>
        <select id="<%=OAuth2Constants.OAuth2Client.REDIRECT_URI%>" multiple="single" name="<%=OAuth2Constants.OAuth2Client.REDIRECT_URI%>"></select>
        <input type="button" name="remove_url" id="remove_url" value="Remove"/><br>
		<input type="text" name="url" value="" id ="<%=OAuth2Constants.OAuth2Client.REDIRECT_URI%>"/>
        <input type="button" name="add_url" id="add_url" value="Add"/>
        <br><br>
		Scope(s):<br>
        <select id="<%=OAuth2Constants.OAuth2Client.SCOPES%>" multiple="single" name="<%=OAuth2Constants.OAuth2Client.SCOPES%>"></select>
        <input type="button" name="remove_scope" id="remove_scope" value="Remove"/>
        <br>
        <input type="text" name="url" value="" id ="<%=OAuth2Constants.OAuth2Client.SCOPES%>"/>
        <input type="button" name="add_scope" id="add_scope" value="Add"/>
        <br><br>
		Default Scope(s):<br>
        <select id="<%=OAuth2Constants.OAuth2Client.DEFAULT_SCOPES%>" multiple="single" name="<%=OAuth2Constants.OAuth2Client.DEFAULT_SCOPES%>"></select>
        <input type="button" name="remove_default_scope" id="remove_default_scope" value="Remove"/>
        <br>
        <input type="text" name="url" value="" id ="<%=OAuth2Constants.OAuth2Client.DEFAULT_SCOPES%>"/>
        <input type="button" name="add_default_scope" id="add_default_scope" value="Add"/>
        <br><br>
		Display Name(s):<br>
        <select id="<%=OAuth2Constants.OAuth2Client.NAME%>" multiple="single" name="<%=OAuth2Constants.OAuth2Client.NAME%>"></select>
        <input type="button" name="remove_display_name" id="remove_display_name" value="Remove"/><br>
        <input type="text" name="url" value="" id ="<%=OAuth2Constants.OAuth2Client.NAME%>"/>
        <input type="button" name="add_display_name" id="add_display_name" value="Add"/><br><br>
        Display Description(s):<br>
        <select id="<%=OAuth2Constants.OAuth2Client.DESCRIPTION%>" multiple="single" name="<%=OAuth2Constants.OAuth2Client.DESCRIPTION%>"></select>
        <input type="button" name="remove_display_description" id="remove_display_description" value="Remove"/><br>
        <input type="text" name="url" value="" id ="<%=OAuth2Constants.OAuth2Client.DESCRIPTION%>"/>
        <input type="button" name="add_dispaly_description" id="add_dispaly_description" value="Add"/><br><br>

		<input type="Submit" name="save" id="submit"/>
						
	</form>
</body>
