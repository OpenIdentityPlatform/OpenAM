<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
  
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
  
   $Id: unittest.jsp,v 1.1 2009/08/19 05:41:04 veiming Exp $
  
--%>

<%@ page pageEncoding="UTF-8" %>
<%@ page 
    import="
        com.sun.identity.unittest.TestHarness,
        java.util.Iterator,
        java.util.Map,
        java.util.Set,
        java.util.TreeSet"
%>

<html>
<head>
    <title>OpenSSO - Developer's Unit Tests</title>
    <link rel="stylesheet" type="text/css" href="../com_sun_web_ui/css/css_ns6up.css" />
    <link rel="styleSheet" href="../console/css/commontask.css" type="text/css" rel="stylesheet" />
    <link rel="styleSheet" href="../console/css/css_master.css" type="text/css" rel="stylesheet" />
    <link rel="shortcut icon" href="../com_sun_web_ui/images/favicon/favicon.ico" type="image/x-icon" />
    <style>
    .highlight {background-color:#CCCCCC}
    .unhighlight {background-color:transparent}
    </style>
</head>
<body class="DefBdy">
    <div class="SkpMedGry1"><a href="#SkipAnchor3860"><img src="../com_sun_web_ui/images/other/dot.gif" alt="Jump to End of Masthead" border="0" height="1" width="1" /></a></div><div class="MstDiv">
    <table class="MstTblBot" title="" border="0" cellpadding="0" cellspacing="0" width="100%">
        <tr>
        <td class="MstTdTtl" width="99%">
        <br />
        <div class="MstDivTtl"><img name="AMConfig.configurator.ProdName" src="../console/images/PrimaryProductName.png" alt="OpenSSO" border="0" /></div>
        </td>
        <td class="MstTdLogo" width="1%"><img name="AMConfig.configurator.BrandLogo" src="../com_sun_web_ui/images/other/javalogo.gif" alt="Java(TM) Logo" border="0" height="55" width="31" /></td>
        </tr>
    </table>
    <table class="MstTblEnd" border="0" cellpadding="0" cellspacing="0" width="100%"><tr><td><img name="RMRealm.mhCommon.EndorserLogo" src="../com_sun_web_ui/images/masthead/masthead-sunname.gif" alt="Sun(TM) Microsystems, Inc." align="right" border="0" height="10" width="108" /></td></tr></table>
    </div>
    <table class="SkpMedGry1" border="0" cellpadding="5" cellspacing="0" width="100%"><tr><td><img src="../com_sun_web_ui/images/other/dot.gif" alt="Jump to End of Masthead" border="0" height="1" width="1" /></a></td></tr></table>
    <table border="0" cellpadding="10" cellspacing="0" width="100%"><tr><td></td></tr></table>

<table cellpadding=5 width="100%">
<tr>
<td>
<span class="TskPgeSbHdr">Developer's Unit Tests</span>
<hr size="1" noshade="yes" />

<style>
    ul {
        list-style-type: none;
    }
</style>

<script language="javascript">
    function selectAllTests(cb) {
        var frm = document.forms['test'];
        var elements = frm.elements;
        for (var i = 0; i < elements.length; i++) {
            var elm = elements[i];
            if ((elm.type) && (elm.type == 'checkbox')) {
                        elm.checked = cb.checked;
            }
        }
    }

    function selectpkg(cb) {
        var name = cb.name;
        var frm = document.forms['test'];
        var elements = frm.elements;
        for (var i = 0; i < elements.length; i++) {
            var elm = elements[i];
            if ((elm.type) && (elm.type == 'checkbox')) {
                if (elm.name.indexOf(name + '.') == 0) {
                    if (elm.className != 'pkg') {
                        var chop = elm.name.substring(name.length +2);
                        if (chop.indexOf('.jsp') != -1) {
                            chop = chop.substring(0, chop.length -4);
                        }
                        if (chop.indexOf('.') == -1) {
                            elm.checked = cb.checked;
                        }
                    }
                }
            }
        }
    }

    function expand(name, anchor) {
        var e = document.getElementById(name);
        if (e.style.display == 'none') {
            e.style.display = '';
            anchor.innerHTML = '<b><img border=0 width="11" height="11" src="collapse.gif" /></b>';
        } else {
            e.style.display = 'none';
            anchor.innerHTML = '<b><img border=0 width="11" height="11" src="expand.gif" /></b>';
        }
    }

    function getAllTests() {
        var frm = document.forms['test'];
        var elements = frm.elements;
        var tests = '';
        for (var i = 0; i < elements.length; i++) {
            var elm = elements[i];
            if ((elm.type) && (elm.type == 'checkbox')) {
                if (elm.checked && (elm.className == 'test')) {
                    if (tests != '') {
                        tests += ',';
                    }
                    tests += elm.name;
                }
            }
        }
        if (tests == '') {
            alert('Please select at least one test to run.');
            return false;
        } else {
            elements['tests'].value = tests;
            return true;
        }
    }
</script>

<form name="test" action="unittestexe.jsp" method="post" onSubmit="return getAllTests();" >
<table width="100%">
<td align="left" nowrap="nowrap" valign="bottom">
<div class="TtlBtnDiv">
<input name="submit" type="submit" class="Btn1" value="Submit" onmouseover="javascript: if (this.disabled==0) this.className='Btn1Hov'" onmouseout="javascript: if (this.disabled==0) this.className='Btn1'" onblur="javascript: if (this.disabled==0) this.className='Btn1'" onfocus="javascript: if (this.disabled==0) this.className='Btn1Hov'" />
<input name="submit" type="reset" class="Btn1" value="Reset" onmouseover="javascript: if (this.disabled==0) this.className='Btn1Hov'" onmouseout="javascript: if (this.disabled==0) this.className='Btn1'" onblur="javascript: if (this.disabled==0) this.className='Btn1'" onfocus="javascript: if (this.disabled==0) this.className='Btn1Hov'" />
</td></tr>
</table>
<input type="hidden" name="tests" />

<ul>
    <li><input name="all" type="checkbox" title="Select/unselect the entire set of tests" class="pkg" onClick="selectAllTests(this);" />Select all tests
    <li>
    <ul>
<%
    Map mapPkgNameToClasses = TestHarness.getTests(
        getServletConfig().getServletContext());
    Set set = new TreeSet();
    set.addAll(mapPkgNameToClasses.keySet());

    for (Iterator i = set.iterator(); i.hasNext(); ) {
        String pkgname = (String)i.next();
%>
        <li  onmouseover="this.childNodes[4].className='highlight'" onmouseout="this.childNodes[4].className='unhighlight'"><a href="#" onClick="expand('children.<%= pkgname %>', this); return false;"><img border=0 width="11" height="11" src="expand.gif" /></a> <input name="<%= pkgname %>" type="checkbox" title="Select/unselect the entire set of tests under this package" class="pkg" onClick="selectpkg(this);" /> <span>&nbsp;<%= pkgname %>&nbsp;</span>
        <ul id='children.<%= pkgname %>' style="display:none">

<%
        Set tests = (Set)mapPkgNameToClasses.get(pkgname);
        for (Iterator j = tests.iterator(); j.hasNext(); ) {
            String testname = (String)j.next();
            String value = testname;
            String label = testname;

            if (testname.endsWith(".jsp")) {
                testname = testname.replace('/', '.');
                int idx = label.lastIndexOf("/");
                label = label.substring(idx+1);
            }
%>
            <li onmouseover="this.childNodes[2].className='highlight'" onmouseout="this.childNodes[2].className='unhighlight'"><input class="test" name="<%= testname %>" value="<%= value %>" type="checkbox" title="Select/unselect this test to run" /> <span>&nbsp;<%= label %>&nbsp;</span></li>
<%
        }
%>
        </ul></li>
<%
    }
%>

</ul>
</li>
</ul>

</form>

</td></tr>
</table>
</body>
</html>

