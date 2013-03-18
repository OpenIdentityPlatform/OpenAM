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

   $Id: saeIDPApp.jsp,v 1.12 2009/03/10 20:12:10 exu Exp $

--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="com.sun.identity.sae.api.SecureAttrs"%>
<%@ page import="com.sun.identity.sae.api.Utils"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="com.sun.identity.common.SystemConfigurationUtil"%>
<%!
public void jspInit()
{
}
%>
<%
    String deployuri = SystemConfigurationUtil.getProperty(
        "com.iplanet.am.services.deploymentDescriptor");
    if ((deployuri == null) || (deployuri.length() == 0)) {
        deployuri = "../../..";
    }
%>
<html>
<head>
<title>Secure Attributes Exchange IDP APP SAMPLE</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />
</head>
<body>
<%@ include file="header.jspf" %>
<br><b>Secure Attributes Exchange IDP APP SAMPLE</b><br>
<% 
    request.setCharacterEncoding("UTF-8");
    // Crypto type to be used with local <OpenAM>-IDP
    String cryptotype     = SecureAttrs.SAE_CRYPTO_TYPE_SYM;
    // For SYM: Shared secret with local <OpenAM>-IDP
    // For ASYM: Private Key Alias for IDP-APP's signing cert
    String secret     = "secret12";
    // For SYM: Shared secret with local <OpenAM>-IDP. Same value as secret.
    // For ASYM: Public Key Alias for  <OpenAM>-IDP.
    String encSecret = secret;
    String encryptionAlg = "DES";
    String encryptionStrength = "56";
    // Keystore path (for asym signing)
    String keystore = "";
    // Keystore Password (for asym signing)
    String keypass = "";
    // Private key Password (for asym signing)
    String privkeypass = "";
    // identity of this application : this string should match a already 
    // registered application in one of the hosted IDP extended metadata.
    String idpAppName = request.getRequestURL().toString();

    // <OpenAM>-IDP hosted SAE url that will act like the gateway.
    String  saeServiceURL="http://sa.idp.com:8080/sa/idpsaehandler/metaAlias/idp";

    // String representing authenticated user.
    String userid = "testuser";
    String authlevel = "0";
    // String representing profile attributes of authenticated user
    String mail   = "testuser@foo.com";
    String branch = "mainbranch" ;

    // SP-App to be invoked with profile attributes above.
    String spapp  = "http://www.spp.com:8080/sp/samples/saml2/sae/saeSPApp.jsp";
    // Whether cached SecureAttrs class instance should be used
    String usecached = "on";
    String useencryption = "on";

    // Private key Password (for asym signing)
    if (request.getMethod().equals("GET"))
    {
%>
<br>
This sample represents an IDP-App wishing to securely invoke a remote SP-App and pass it some secure attributes (mail and branch).
<br>
<br>
IDP-App -sae---> IDP-&lt;OpenAM> --samlv2---> SP-&lt;OpenAM> --sae--> SP-App
<br>
<br>
<b>Prerequisites :</b>
<br>
IDP=Identity Provider  SP=Service Provider
<br>
i) Trust key (shared secret for symmetric crypto  or privatekey for asymmetric signing; shared secret for symmetric crypto data encryption or publickey for asymmetric data encryption) & this application provisioned on IDP-&lt;OpenAM> in one of the hosted extended metadata - you will enter the same appname and secret here.
<br>
ii) SP_App and corresponding shared secret or key-pair provisioned on SP-&lt;OpenAM> and destination SP-App. You will enter SP-App here.
<br>
iii) "auto-federation" and corresponding attributes setup (branch and mail) on both SP-&lt;OpenAM> and IDP-&lt;OpenAM> ends.
<br>
iv) SP-App is already deployed and ready to accept requests.
<br>
<br>
<hr>
<b>Please Fill up the following form :</b> (Note that it is assumed userid you are about to enter is already authenticated.)
<br><br>
    <form method="POST">
      <table>
        <tr>
          <td>Userid on local IDP : </td>
          <td><input  type="text" name="userid" value="<%=userid%>"></td>
        </tr>
        <tr>
          <td>Authenticated auth level : </td>
          <td><input  type="text" name="authlevel" value="<%=authlevel%>"></td>
        </tr>
        <tr>
          <td>mail attribute : </td>
          <td><input  type="text" name="mail" value="<%=mail%>"></td>
        </tr>
        <tr>
          <td>branch attribute : </td>
          <td><input  type="text" name="branch" value="<%=branch%>"></td>
        </tr>
        <tr>
          <td>SP App URL : </td>
          <td><input  type="text" name="spapp" size=80 value="<%=spapp%>"></td>
        </tr>
        <tr>
          <td>SAE URL on IDP end: </td>
          <td><input type="text" name="saeurl" size=80 value=<%=saeServiceURL%>></td>
        </tr>
        <tr>
          <td>This application's identity (should match Secret below) : </td>
          <td><input  type="text" name="idpappname" size=80 value="<%=idpAppName%>"></td>
        </tr>
        <tr>
          <td>Crypto Type : </td>
          <td>
           <select  name="cryptotype" >
              <option <%= cryptotype.equals("symmetric") ? "SELECTED" : ""%> value="symmetric">symmetric</option>
              <option <%= cryptotype.equals("asymmetric") ? "SELECTED" : ""%> value="asymmetric">asymmetric</option>
           </select>
          </td>
        </tr>
        <tr>
          <td>Signing Shared Secret / This App's Private Key alias : </td>
          <td><input  type="text" name="secret" value="<%=secret%>"></td>
        </tr>
        <tr>
          <td>Enable encryption: </td>
          <td><input  type="checkbox" name="useencryption"></td>
        </tr>
        <tr>
          <td>Encryption Shared Secret / IDP's Public Key alias : </td>
          <td><input  type="text" name="encSecret" value="<%=encSecret%>"></td>
        </tr>
        <tr>
          <td>Encryption Algorithm : </td>
          <td><input  type="text" name="encAlgorithm" value="<%=encryptionAlg%>"></td>
        </tr>
        <tr>
          <td>Encryption Strength : </td>
          <td><input  type="text" name="encStrength" value="<%=encryptionStrength%>"></td>
        </tr>
        <tr>
          <td>Use Cached SecureAttrs instance: </td>
          <td><input  type="checkbox" name="usecached" checked="true"></td>
        </tr>
        <tr> <td colspan=2><hr></td> </tr>
        <tr>
          <td>Key store path (asymmetric only) : </td>
          <td><input  type="text" name="keystore" value="<%=keystore%>"></td>
        </tr>
        <tr>
          <td>Key store password (asymmetric only) : </td>
          <td><input  type="text" name="keypass" value="<%=keypass%>"></td>
        </tr>
        <tr>
          <td>Private Key password (asymmetric only) : </td>
          <td><input  type="text" name="privkeypass" value="<%=privkeypass%>"></td>
        </tr>
        <tr> <td colspan=2><hr></td> </tr>
        <tr>
          <td><input  type="submit" value="Generate URL"></td>
          <td></td>
        </tr>
      </table>
    </form>

<%  } else  {// POST
        HashMap map = new HashMap();
        userid = request.getParameter("userid");    
        authlevel = request.getParameter("authlevel");
        mail = request.getParameter("mail");    
        branch = request.getParameter("branch");    
        spapp = request.getParameter("spapp");    
        saeServiceURL = request.getParameter("saeurl");    
        idpAppName = request.getParameter("idpappname");    
        cryptotype = request.getParameter("cryptotype");    
        secret = request.getParameter("secret");    
        encSecret = request.getParameter("encSecret");
        keystore = request.getParameter("keystore");    
        keypass = request.getParameter("keypass");    
        usecached = request.getParameter("usecached");    
        privkeypass = request.getParameter("privkeypass");    
        useencryption = request.getParameter("useencryption");
        encryptionAlg = request.getParameter("encAlgorithm");
        System.out.println("Encryption alg" + encryptionAlg);
        encryptionStrength = request.getParameter("encStrength");

        // Check if we already have a cached SecureAttrs instance.
        String mySecAttrInstanceName = "sample"+cryptotype;
        SecureAttrs sa = SecureAttrs.getInstance(mySecAttrInstanceName);
     
        if (sa == null || usecached == null) {
          out.println("Obtaining new SecureAttrs instance");
          Properties saeparams = new Properties();
          if (SecureAttrs.SAE_CRYPTO_TYPE_ASYM.equals(cryptotype)) {
            saeparams.setProperty(SecureAttrs.SAE_CONFIG_KEYSTORE_TYPE, "JKS");
            saeparams.put(SecureAttrs.SAE_CONFIG_PRIVATE_KEY_ALIAS, secret);
            saeparams.put(SecureAttrs.SAE_CONFIG_KEYSTORE_FILE, keystore);
            saeparams.put(SecureAttrs.SAE_CONFIG_KEYSTORE_PASS, keypass);
            saeparams.put(SecureAttrs.SAE_CONFIG_PRIVATE_KEY_PASS, privkeypass);
            saeparams.put(SecureAttrs.SAE_CONFIG_DATA_ENCRYPTION_ALG, encryptionAlg);
            saeparams.put(SecureAttrs.SAE_CONFIG_ENCRYPTION_KEY_STRENGTH, encryptionStrength);
          } else {
            saeparams.put(SecureAttrs.SAE_CONFIG_DATA_ENCRYPTION_ALG, encryptionAlg);
            saeparams.put(SecureAttrs.SAE_CONFIG_ENCRYPTION_KEY_STRENGTH, encryptionStrength);
          }
          SecureAttrs.init(mySecAttrInstanceName, cryptotype, saeparams);
          sa = SecureAttrs.getInstance(mySecAttrInstanceName);
        } else
          out.println("Using cached SecureAttrs instance");

        map.put("branch",branch); 
        map.put("mail",mail); 
        // Following code secures attributes
        map.put(SecureAttrs.SAE_PARAM_USERID, userid); 
        map.put(SecureAttrs.SAE_PARAM_AUTHLEVEL, authlevel);
        map.put(SecureAttrs.SAE_PARAM_SPAPPURL, spapp); 
        map.put(SecureAttrs.SAE_PARAM_IDPAPPURL, idpAppName);
        String encodedString = null;
         if(useencryption != null) {
           encodedString = sa.getEncodedString(map, secret, encSecret);
        } else {
           encodedString = sa.getEncodedString(map, secret);
        }

        out.println("<br>Setting up the following params:");
        out.println("<br>branch="+branch);
        out.println("<br>mail="+mail);
        out.println("<br>"+SecureAttrs.SAE_PARAM_USERID+"="+userid);
        out.println("<br>"+SecureAttrs.SAE_PARAM_AUTHLEVEL+"="+authlevel);
        out.println("<br>"+SecureAttrs.SAE_PARAM_SPAPPURL+"="+spapp);
        out.println("<br>"+SecureAttrs.SAE_PARAM_IDPAPPURL+"="+idpAppName);

        HashMap slomap = new HashMap();
        slomap.put(SecureAttrs.SAE_PARAM_CMD,SecureAttrs.SAE_CMD_LOGOUT); 
        slomap.put(SecureAttrs.SAE_PARAM_APPSLORETURNURL, request.getRequestURL().toString());; 
        slomap.put(SecureAttrs.SAE_PARAM_IDPAPPURL, idpAppName);
        String sloencodedString = null;
        if(useencryption != null) {
           sloencodedString = sa.getEncodedString(slomap, secret, encSecret);
        } else {
           sloencodedString = sa.getEncodedString(slomap, secret);
        }

        // We are ready to format the URLs to invoke the SP-App and Single logout
        String url = null;
        String slourl = null;
        String postForm = null;
        HashMap pmap = new HashMap();
        pmap.put(SecureAttrs.SAE_PARAM_DATA, encodedString);
        if (saeServiceURL.indexOf("?") > 0) {
            url = saeServiceURL+"&" +
                  SecureAttrs.SAE_PARAM_IDPAPPURL+"="+idpAppName + "&" +
                  SecureAttrs.SAE_PARAM_DATA+"="+encodedString;
            slourl = saeServiceURL+"&" +
                     SecureAttrs.SAE_PARAM_IDPAPPURL+"="+idpAppName + "&" +
                     SecureAttrs.SAE_PARAM_DATA+"=" +sloencodedString;
        }
        else {
            url = saeServiceURL+"?" +
                  SecureAttrs.SAE_PARAM_IDPAPPURL+"="+idpAppName + "&" +
                  SecureAttrs.SAE_PARAM_DATA+"="+encodedString;
            slourl = saeServiceURL+"?" +
                     SecureAttrs.SAE_PARAM_IDPAPPURL+"="+idpAppName + "&" +
                     SecureAttrs.SAE_PARAM_DATA+"=" +sloencodedString;
        }

        // This function is a simple wrapper to create a form - to
        // autosubmit the form via javascriopt chnage false to true.
        pmap.put(SecureAttrs.SAE_PARAM_IDPAPPURL, idpAppName);
        postForm = Utils.formFromMap(saeServiceURL, pmap, false);
        out.println(postForm);

        out.println("<br><br>Click here to invoke the remote SP App via http GET to local IDP : "+spapp+"  :  <a href="+url+">ssourl</a>");
        out.println("<br><br>Click here to invoke the remote SP App via http POST to IDP : "+spapp+"  :  <input type=\"button\" onclick=\"document.forms['saeform'].submit();\" value=POST>");
        out.println("<br><br>This URL will invoke global Logout : <a href="+slourl+">slourl</a>");
    }
%>
</body>
</html>
