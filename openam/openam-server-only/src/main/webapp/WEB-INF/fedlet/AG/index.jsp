<%--
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

   $Id: index.jsp,v 1.5 2009/01/27 18:02:53 weisun2 Exp $

   Portions copyright 2013 ForgeRock AS
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ page import="com.sun.identity.saml2.common.SAML2Exception" %>
<%@ page import="com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement" %>
<%@ page import="com.sun.identity.saml2.jaxb.metadata.SingleSignOnServiceElement" %>
<%@ page import="com.sun.identity.saml2.meta.SAML2MetaException" %>
<%@ page import="com.sun.identity.saml2.meta.SAML2MetaManager" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.io.File" %>
<%@ page import="java.io.InputStream" %>
<%@ page import="java.io.FileOutputStream" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>

<%@ page import="com.sun.identity.saml2.assertion.*"%>
<%@ page import="com.sun.identity.saml2.common.*" %>
<%@ page import="java.util.Date,com.sun.identity.saml2.assertion.impl.*,java.util.ArrayList,java.util.List" %>
<%@ page import="com.sun.identity.fedlet.ag.*"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<%
    String deployuri = request.getRequestURI();
    int slashLoc = deployuri.indexOf("/", 1);
    if (slashLoc != -1) {
        deployuri = deployuri.substring(0, slashLoc);
    }
    String fedletHomeDir = System.getProperty("com.sun.identity.fedlet.home");
    if ((fedletHomeDir == null) || (fedletHomeDir.trim().length() == 0)) {
        if (System.getProperty("user.home").equals(File.separator)) {
            fedletHomeDir = File.separator + "fedlet";
        } else {
            fedletHomeDir = System.getProperty("user.home") +
                File.separator + "fedlet";
        }
    }
%>

<html>
    

<head>
    <title>Validate Fedlet Setup</title>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
    <link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />
</head>

<body>
<div class="MstDiv"><table width="100%" border="0" cellpadding="0" cellspacing="0" class="MstTblTop" title="">
<tbody><tr>
<td nowrap="nowrap">&nbsp;</td>
<td nowrap="nowrap">&nbsp;</td>
</tr></tbody></table>

<table width="100%" border="0" cellpadding="0" cellspacing="0" class="MstTblBot" title="">
<tbody><tr>
<td class="MstTdTtl" width="99%">
<div class="MstDivTtl"><img name="ProdName" src="<%= deployuri %>/console/images/PrimaryProductName.png" 
alt="" /></div></td><td class="MstTdLogo" width="1%"><img name="RMRealm.mhCommon.BrandLogo" 
src="<%= deployuri %>/com_sun_web_ui/images/other/javalogo.gif" alt="Java(TM) Logo" border="0" 
height="55" width="31" /></td></tr></tbody></table>
<table class="MstTblEnd" border="0" cellpadding="0" cellspacing="0" width="100%">
    <tbody><tr><td><img name="RMRealm.mhCommon.EndorserLogo" 
    src="<%= deployuri %>/com_sun_web_ui/images/masthead/masthead-sunname.gif" 
    alt="Sun(TM) Microsystems, Inc." align="right" border="0" height="10" width="108" />
    </td></tr></tbody></table></div><div class="SkpMedGry1">
        <a name="SkipAnchor2089" id="SkipAnchor2089"></a></div>
<div class="SkpMedGry1"><a href="#SkipAnchor4928">
    <img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" 
    alt="Jump Over Tab Navigation Area. Current Selection is: Access Control" 
    border="0" height="1" width="1" /></a></div>

<form name="form" method="GET" action="index.jsp">
    <input type="hidden" name="hidden" value=""/>
</form>
<% 
    String[] files = null;
    int flag = 0;
    String IDPid = "null";
    String SPid = "null";
    String SPurl = "null";
    if (request.getParameter("hidden") == null){
        File file = new File(fedletHomeDir + File.separator + "idp.xml");
        if (!file.exists()){
%> 

<script language="JavaScript" type="text/javascript">
<!--

    var result = window.confirm("idp.xml dose not exist, do you want to import the test IDP metadata?");
    if(result == true){
        document.form.hidden.value = "true";
         document.form.submit();
    }
//-->
</script>
<%      } else {
             
             MetaDataParser lparser = new MetaDataParser();
            IDPid = lparser.getIDPEntityID();
            SPid = lparser.getSPEntityID();
            SPurl = lparser.getSPbaseUrl();
        }
           
        files = new String[] {                
                ".keypass",
                ".storepass",
                "keystore.jks"};
            
    } else {
            
         files = new String[] {
             "idp.xml",
             "idp-extended.xml",
             };
         flag = 1;
            
    }
                
    File dir = new File(fedletHomeDir);
            
    ServletContext servletCtx = getServletConfig().getServletContext();
    for (int i = 0; i < files.length; i++) {
        String source = "/conf/" + files[i];
        String dest =  dir.getPath() + File.separator + files[i];
        FileOutputStream fos = null;
        InputStream src = null;
        try {
            src = servletCtx.getResourceAsStream(source);
            if (src != null) {
                fos = new FileOutputStream(dest);
                int length = 0;
                byte[] bytes = new byte[1024];
                while ((length = src.read(bytes)) != -1) {
                    fos.write(bytes, 0, length);
                }
            } else {
                throw new SAML2Exception("File " + source + 
                    " could not be found in fedlet.war");
            }
        } catch (IOException e) {
            throw new SAML2Exception(e.getMessage());
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (src != null) {
                     src.close();
                }
            } catch (IOException ex) {
                //ignore
            }
        }
    }
    if (flag ==1) {
        MetaDataParser lparser = new MetaDataParser();
        lparser.createCOT();
        IDPid = lparser.getIDPEntityID();
        SPid = lparser.getSPEntityID();
        SPurl = lparser.getSPbaseUrl();
    }
%>
                
<%if(request.getMethod().equals("POST"))
    {%>
    <jsp:useBean id="newpackage" class="com.sun.identity.fedlet.ag.UserData" scope="session"/>
    <jsp:setProperty name="newpackage" property="*"/> 
    <BR>

    
    
    <%AssertionGen ag = new AssertionGen();                    
        String encodedResMsg = SAML2Utils.encodeForPOST(ag.getResponse(newpackage.getselos(),
                newpackage.getselos2()));
        MetaDataParser lparser = new MetaDataParser();
        String relayState = null;
        String acsURL = lparser.getSPbaseUrl();
        
        if(newpackage.getrelaystate() != null){
            relayState = newpackage.getrelaystate();
        }
        SAML2Utils.postToTarget(request, response, "SAMLResponse",
                   encodedResMsg, "RelayState", relayState, acsURL);
    %>
    <BR>


    <%
    }else{%>
<BR>
<BR>    

<TABLE BORDER="1" CELLPADDING="3" CELLSPACING="1">
    
<%  for (int i = 0; i < 2; i++) { %>
    <TR>
        <%if(i==1){
            %>
        <TD ROWSPAN="0">
            SP
            <%}else{%>
        <TD>
            IDP
            <%}%>
            <CENTER>
            
            </CENTER>
        </TD>
        <%if(i==1){
            %>
       
        <tr>
        <TD>
            EntityID
        </TD>
        <TD>
            <%=SPid%>
        </TD>
        </tr>
        <tr>
   
        <TD>
            Assertion Consumer URL
        </TD>
        <TD>
            <%=SPurl%>
        </TD>
        </tr>
    
        <%}else{%>
        
        <TD>
            EntityID
        </TD>
        <TD>
            <%=IDPid%>
        </TD>
        
        <%}%>
    </TR>
<%  } %>
</TABLE>
<BR>
<TABLE BORDER="1">
    
    <form name="MainForm" method="POST" action="index.jsp">
        <TR>
            <TD>Enter RelayState</TD> 
            <TD COLSPAN="0">
            <INPUT ID="relaystate" TYPE=TEXT NAME="relaystate" SIZE=50>
            
            </TD>
        </TR>
        <TR>
            <TD ROWSPAN="0">
                Attributes
            </TD>
            <TD>NAME</TD>
            <TD>VALUE</TD>
        </TR>
        <TR>
            <TD>
                <select name="selos" size="10" multiple="multiple" >
                    <option value="null" selected>null</option>
                </select>
            </TD>
            <TD>
                <select name="selos2" size="10" multiple="multiple" >
                    <option value="null" selected>null</option>
                </select>
            </TD>
        </TR>
        <TR>
            <TD COLSPAN="0"> 
                Attribute name  :<INPUT ID="attrname" TYPE=TEXT NAME="attrname" SIZE=20><BR>
                Attribute value :<INPUT ID="attrvalue" TYPE=TEXT NAME="attrvalue" SIZE=20><BR>
                <input type="button" value="Add"
                       onclick="ios++; insert(this.form.selos, this.form.selos2 
                           ,this.form.attrname.value , this.form.attrvalue.value);" />
                <input type="button" value="Remove"
                 onclick="remove(this.form.selos, this.form.selos2);" />
                 
                <INPUT TYPE=SUBMIT >
         </TD>
       </TR>
    </form>
</TABLE>
<%}%>






</html>

<script language="JavaScript" type="text/javascript">
<!--
var ios = -1;
var ios1 = 0;

function insert(theSel, theSel2, newText, newText2)
{
    
            if(ios < 1){
                
                window.alert("Delete the null values");
                ios = -1;
            }else{

          if (theSel.length == 0) {
            var newOpt1 = new Option(newText, newText);
            theSel.options[0] = newOpt1;
            theSel.selectedIndex = 0;
            var newOpt2 = new Option(newText2, newText2);
            theSel2.options[0] = newOpt2;
            theSel2.selectedIndex = 0;
          } else if (theSel.selectedIndex != -1) {
            var selText = new Array();
            var selValues = new Array();
            var selIsSel = new Array();
            var newCount = -1;
            var selText2 = new Array();
            var selValues2 = new Array();
            var selIsSel2 = new Array();

            var newSelected = -1;
            var i;
            for(i=0; i<theSel.length; i++)
            {
              newCount++;


              if (newCount == theSel.length-1) {
                selText[newCount] = theSel.options[i].text;
                selValues[newCount] = theSel.options[i].value;
                selIsSel[newCount] = theSel.options[i].selected;

                selText2[newCount] = theSel2.options[i].text;
                selValues2[newCount] = theSel2.options[i].value;
                selIsSel2[newCount] = theSel2.options[i].selected;
                newCount++;
                selText[newCount] = newText;
                selValues[newCount] = newText;
                selIsSel[newCount] = false;

                selText2[newCount] = newText2;
                selValues2[newCount] = newText2;
                selIsSel2[newCount] = false;
                newSelected = newCount;
                break;
              }
              selText[newCount] = theSel.options[i].text;
              selValues[newCount] = theSel.options[i].value;
              selIsSel[newCount] = theSel.options[i].selected;
              selText2[newCount] = theSel2.options[i].text;
              selValues2[newCount] = theSel2.options[i].value;
              selIsSel2[newCount] = theSel2.options[i].selected;
            }
            for(i=0; i<=newCount; i++)
            {
              var newOpt = new Option(selText[i], selValues[i]);
              theSel.options[i] = newOpt;
              theSel.options[i].selected = selIsSel[i];
              var newOpt2 = new Option(selText2[i], selValues2[i]);
              theSel2.options[i] = newOpt2;
              theSel2.options[i].selected = selIsSel2[i];
            }
          }
         }
}

function remove(theSel, theSel2)
{
  var selIndex = theSel.selectedIndex;
  var flag = 1;
  if (selIndex != -1) {
    for(i=theSel.length-1; i>=0; i--)
    {
      if(theSel.options[i].selected)
      {
        theSel.options[i] = null;
        theSel2.options[i] = null;
        if(ios != -1){
        ios--;}else{ ios = 1;
                    flag = 0;
                }
      }
    }
    if (theSel.length > 0) {
      theSel.selectedIndex = selIndex == 0 ? 0 : selIndex - 1;
    }
  }
  if(theSel.length == 0 && flag !=0){
      insertOldSchool(theSel, theSel2, "null", "null");
      ios = -1;
  }
      
}


//-->
</script>
