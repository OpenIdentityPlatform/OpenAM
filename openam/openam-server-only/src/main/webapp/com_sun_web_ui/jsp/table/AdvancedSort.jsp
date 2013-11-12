<%--
/*
 * ident "@(#)AdvancedSort.jsp 1.16 04/05/07 SMI"
 * 
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 */
--%>

<%--
   Portions Copyrighted 2013 ForgeRock AS
--%>

<%@ page language="java" %>
<%@ page import="org.owasp.esapi.ESAPI" %>
<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato"%> 
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc"%>

<%
    // Get query parameters.
    String pageTitle = (request.getParameter("pageTitle") != null) ? request.getParameter("pageTitle") : "";
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + pageTitle, pageTitle,
            "HTTPParameterValue", 2000, false)) {
        pageTitle = "";
    }
    String baseName = (request.getParameter("baseName") != null) ? request.getParameter("baseName") : "";
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + baseName, baseName,
            "HTTPParameterValue", 2000, false)) {
        baseName = "";
    }
    String formName = (request.getParameter("formName") != null) ? request.getParameter("formName") : "";
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + formName, formName,
            "HTTPParameterValue", 2000, false)) {
        formName = "";
    }
%>

<jato:pagelet>

<cc:i18nbundle id="tagBundle" baseName="com.sun.web.ui.resources.Resources" />
<cc:i18nbundle id="appBundle" baseName="<%=baseName %>" />

<script type="text/javascript">
function submitValues() {
    // Form names.
    var sf = document.sortForm;
    var pf = window.opener.document.<%=formName %>;

    // Command child name.
    var commandChildName = "<cc:text name='CommandChildNameText'/>";

    // Parent window hidden field names.
    var advancedSortName   = "<cc:text name='AdvancedSortNameText'/>";
    var advancedSortOrder  = "<cc:text name='AdvancedSortOrderText'/>";
    var primarySortName    = "<cc:text name='PrimarySortNameText'/>";
    var primarySortOrder   = "<cc:text name='PrimarySortOrderText'/>";
    var secondarySortName  = "<cc:text name='SecondarySortNameText'/>";
    var secondarySortOrder = "<cc:text name='SecondarySortOrderText'/>";

    // Menu names.
    var advancedSortNameMenu =
	sf.elements["<cc:text name='AdvancedSortNameMenuText'/>"];
    var advancedSortOrderMenu =
	sf.elements["<cc:text name='AdvancedSortOrderMenuText'/>"];
    var primarySortNameMenu =
	sf.elements["<cc:text name='PrimarySortNameMenuText'/>"];
    var primarySortOrderMenu =
	sf.elements["<cc:text name='PrimarySortOrderMenuText'/>"];
    var secondarySortNameMenu =
	sf.elements["<cc:text name='SecondarySortNameMenuText'/>"];
    var secondarySortOrderMenu =
	sf.elements["<cc:text name='SecondarySortOrderMenuText'/>"];

    // Set sort selections.
    if (primarySortNameMenu.selectedIndex > 0) {
	pf.elements[primarySortName].value =
	    primarySortNameMenu.options[
		primarySortNameMenu.selectedIndex].value;
	pf.elements[primarySortOrder].value =
	    primarySortOrderMenu.options[
		primarySortOrderMenu.selectedIndex].value;
    } else {
	pf.elements[primarySortName].value = "";
	pf.elements[primarySortOrder].value = "";
    }

    if (secondarySortNameMenu.selectedIndex > 0
	    && primarySortNameMenu.selectedIndex > 0) {
	pf.elements[secondarySortName].value =
	    secondarySortNameMenu.options[
		secondarySortNameMenu.selectedIndex].value;
	pf.elements[secondarySortOrder].value =
	    secondarySortOrderMenu.options[
		secondarySortOrderMenu.selectedIndex].value;
    } else {
	pf.elements[secondarySortName].value = "";
	pf.elements[secondarySortOrder].value = "";
    }

    if (advancedSortNameMenu.selectedIndex > 0
	    && secondarySortNameMenu.selectedIndex > 0
	    && primarySortNameMenu.selectedIndex > 0) {
	pf.elements[advancedSortName].value =
	    advancedSortNameMenu.options[
		advancedSortNameMenu.selectedIndex].value;
	pf.elements[advancedSortOrder].value =
	    advancedSortOrderMenu.options[
		advancedSortOrderMenu.selectedIndex].value;
    } else {
	pf.elements[advancedSortName].value = "";
	pf.elements[advancedSortOrder].value = "";
    }

    // Get action URI and query substrings.
    var uri = "";
    var url = pf.action;
    var queryParams = "";
    var index = url.indexOf("?");

    if (index == -1) {
	uri = url; // Use URL when query params are not found.
    } else {
	uri = url.substring(0, index);
	queryParams = "&" + url.substring(index + 1, url.length);
    }

    // Set form action url and submit.
    pf.action = uri + "?" + commandChildName + "=" + queryParams;
    pf.submit();
}
</script>

<cc:form name="sortForm" method="post">

<cc:pagetitle name="PageTitle"
 pageTitleText="<%=ESAPI.encoder().encodeForHTML(pageTitle) %>"
 showPageButtonsTop="false"
 bundleID="tagBundle">

<div class="ConMgn">

<table cellpadding="0" cellspacing="10">
 <tr>
  <td>
   <cc:label name="Label" elementName="PrimarySortNameMenu"
    defaultValue="table.advancedSortText1" bundleID="tagBundle" />
  </td>
  <td>
   <cc:dropdownmenu name="PrimarySortNameMenu" bundleID="appBundle" />
  </td>
  <td>
   <cc:dropdownmenu name="PrimarySortOrderMenu" bundleID="tagBundle" />
  </td>
 </tr>
 <tr>
  <td>
   <cc:label name="Label" elementName="SecondarySortNameMenu"
    defaultValue="table.advancedSortText2" bundleID="tagBundle" />
  </td>
  <td>
   <cc:dropdownmenu name="SecondarySortNameMenu" bundleID="appBundle" />
  </td>
  <td>
   <cc:dropdownmenu name="SecondarySortOrderMenu" bundleID="tagBundle" />
 </tr>
 <tr>
  <td>
   <cc:label name="Label" elementName="AdvancedSortNameMenu"
    defaultValue="table.advancedSortText3" bundleID="tagBundle" />
  </td>
  <td>
   <cc:dropdownmenu name="AdvancedSortNameMenu" bundleID="appBundle" />
  </td>
  <td>
   <cc:dropdownmenu name="AdvancedSortOrderMenu" bundleID="tagBundle" />
  </td>
 </tr>
</table>

</div>
</cc:pagetitle>
</cc:form>
</jato:pagelet>
