<%--
/*
 * ident "@(#)WizardWindow.jsp 1.15 04/05/07 SMI"
 * 
 * Copyright 2004 by Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 */
--%>
<%@ page language="java" %> 
<%@page import="com.sun.web.ui.common.CCI18N" %>
<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato"%> 
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc"%>

<%
    // Parameters
    //
    // wizWinMsthdSrc - application image for masthead
    // wizWinMsthdAlt - alternate src
    // wizWinBaseName - resource file path
    // wizWinBundleId - bundleId
    // wizWinTitle - page title
    //
    // Not used here but available
    //
    // wizard info - a list of application wizard defined name value
    //           pairs relevant to the application "backend" wizard class
    //		 For example the XML wizard needs the xml file path and
    //		 a name. So we'll use 
    //
    //		 wizName for the name
    //		 wizXmlFile for the XML file path
    //
    //		 wizModelClassName - is a framework defined name
    //		 for the application wizard class. The wizard info
    //		 parameters will be passed to this class

    /*
     * Eventually when proper default keys are place in this resource
     * file. Should also use define constants from a CCWizard class
     *
     */
    String pMsthdSrc =
	request.getParameter("WizardWindow.wizWinMsthdSrc") == null ?
	"wizard.window.secondary.src" :
	request.getParameter("WizardWindow.wizWinMsthdSrc");
    String pBaseName =
	request.getParameter("WizardWindow.wizWinBaseName") == null ?
	"com.sun.web.ui.resources.Resources" :
	request.getParameter("WizardWindow.wizWinBaseName");
    String pBundleId =
	request.getParameter("WizardWindow.wizWinBundleId") == null ?
	"tagBundle" : request.getParameter("WizardWindow.wizWinBundleId");

    // Get query parameters.
    String pWinTitle = 
	(request.getParameter("WizardWindow.wizWinTitle") != null)
	    ? request.getParameter("WizardWindow.wizWinTitle")
	    : "wizard.window.title";
    String pMsthdAlt = 
	(request.getParameter("WizardWindow.wizWinMsthdAlt") != null)
	    ? request.getParameter("WizardWindow.wizWinMsthdAlt")
	    : "wizard.window.secondary.alt";
%>

<jato:useViewBean
	className="com.sun.web.ui.servlet.wizard.WizardWindowViewBean">

<!-- Header -->
<cc:header name="Header"
	pageTitle="<%=pWinTitle %>"
	copyrightYear="2004"
	onResize="javascript: resize_hack()"
	onLoad="javascript: wizOnLoad(WizardWindow_Wizard)"
	baseName="<%=pBaseName %>"
	bundleID="<%=pBundleId %>"
	preserveFocus="true"
	preserveScroll="true"
    	isPopup="true">

<cc:form name="wizWinForm" method="post">

<!-- Secondary Masthead -->
<cc:secondarymasthead name="Masthead" bundleID="<%=pBundleId %>"
    src="<%=pMsthdSrc %>" alt="<%=pMsthdAlt %>"/>

<!-- Wizard -->
<!-- bundleId may be overloaded from the wizard class or model -->
<cc:wizard name="Wizard" />

<cc:hidden name="wizName"/>
<cc:hidden name="wizClassName"/>
<cc:hidden name="wizWinMsthdSrc"/>
<cc:hidden name="wizWinMsthdAlt"/>
<cc:hidden name="wizWinBaseName"/>
<cc:hidden name="wizWinBundleId"/>
<cc:hidden name="wizWinTitle"/>
<cc:hidden name="wizWinName"/>
<cc:hidden name="wizBtnForm"/>
<cc:hidden name="wizRefreshCmdChild"/>

</cc:form>
</cc:header>
</jato:useViewBean>
