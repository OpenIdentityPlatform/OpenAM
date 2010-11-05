<%--
/*
 * ident "@(#)FileChooserWindow.jsp 1.10 04/10/05 SMI"
 * 
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 */
--%>
<%@ page language="java" %> 
<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato"%> 
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc"%>

<%
    String srcName = (request.getSession().getAttribute("srcName") != null)
	? (String) request.getSession().getAttribute("srcName")
	: "";

    String srcAlt = (request.getSession().getAttribute("srcAlt") != null)
	? (String) request.getSession().getAttribute("srcAlt")
	: "";
%>

<jato:useViewBean 
 className="com.sun.web.ui.servlet.filechooser.FileChooserWindowViewBean">

<!-- Header -->
<cc:header name="Header"
 pageTitle="filechooser.title"
 copyrightYear="2004"
 baseName="com.sun.web.ui.resources.Resources"
 bundleID="tagBundle"
 onLoad="checkAndClose();"
 preserveFocus="true"
 preserveScroll="true"
 isPopup="true">

<script type="text/javascript">
      function checkAndClose() {
	size = window.opener.document.forms[0].elements.length;
	numElements = document.fcForm.elements.length;
	for (i=0; i < size; i++) {
	    if (document.fcForm.elements['FileChooserWindow.filelist'].value == "")
		break;
	    if (window.opener.document.forms[0].elements[i].name == 
		    document.fcForm.elements['FileChooserWindow.fieldname'].value) {
		window.opener.document.forms[0].elements[i].value =
		    document.fcForm.elements['FileChooserWindow.filelist'].value;
		document.fcForm.elements['FileChooserWindow.filelist'].value = "";
		window.close();
	    }
	}
      }
</script>

<cc:form name="fcForm" method="post">

<!-- Masthead -->
<cc:secondarymasthead name="Masthead" src="<%=srcName %>"
 alt="<%=srcAlt %>" />

<!-- Alert -->
<div class="ConMgn">
<br />
<cc:alertinline name="Alert" bundleID="tagBundle" />
</div>

<cc:pagetitle name="PageTitle" bundleID="tagBundle"
 pageTitleText="filechooser.title"
 showPageTitleSeparator="true"
 showPageButtonsTop="false"
 showPageButtonsBottom="true">

<cc:filechooser name="FileChooser" />

<!-- hidden field to set the list of files selected -->
<cc:hidden name="filelist" />
<cc:hidden name="fieldname" />

<script type="text/javascript">
    if (!is_ie) {
        // except in ie, onload call to checkAndClose doesn't close win, call again
        checkAndClose();
    }
</script>

</cc:pagetitle>
</cc:form>
</cc:header>
</jato:useViewBean>
