<%--
/*
 * ident "@(#)DateTimeWindow.jsp 1.4 04/05/13 SMI"
 * 
 * Copyright 2002-2004 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 */
--%>
<%@ page language="java" %> 
<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato"%> 
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc"%>

<jato:useViewBean 
 className="com.sun.web.ui.servlet.datetime.DateTimeWindowViewBean">

<!-- Header -->
<cc:header name="Header"
 pageTitle="Calendar"
 copyrightYear="2003"
 onLoad="checkInput()"
 baseName="com.sun.web.ui.resources.Resources"
 bundleID="tagBundle"
 preserveFocus="true"
 preserveScroll="true"
 isPopup="true">
 
<cc:form name="dtForm" method="post">

<!-- Masthead -->
<cc:secondarymasthead name="Masthead" />

<cc:pagetitle name="PageTitle" bundleID="tagBundle" pageTitleText="datetime.calendar"
 showPageTitleSeparator="true" showPageButtonsTop="false" showPageButtonsBottom="true">

<div><cc:spacer name="Spacer1" width="1" height="10" /></div>

<div class="ConMgn"><cc:datetime name="DateTime" /></div>

<!-- hidden field to store the selected date -->                             
<cc:hidden name="TextName" />
<cc:hidden name="DTWinName" />
<cc:hidden name="InputValid" />
<cc:hidden name="ParentFormName" />

</cc:pagetitle>

<script type="text/javascript">
function checkInput() {
    // if the hidden field InputValid is null or not "true", do nothing
    if (document.dtForm.elements['DateTimeWindow.InputValid'] == null ||
            document.dtForm.elements['DateTimeWindow.InputValid'].value != "true") {
        return;
    }

    // valid data was entered, set the parent date time windows hidden fields
    targetDTWinName = document.dtForm.elements['DateTimeWindow.DTWinName'].value;

    parentFormName = document.dtForm.elements['DateTimeWindow.ParentFormName'].value;
    parentForm = window.opener.document.forms[parentFormName];
                                                                           
    // transfer the start day
    parentForm.elements[targetDTWinName + '.startDate'].value =
        document.dtForm.elements['DateTimeWindow.DateTime.startDate'].value;

    // if the text name hidden field contains a value, set that text input with the start date
    textNameHidden = document.dtForm.elements['DateTimeWindow.TextName']
    if (textNameHidden != null && textNameHidden.value != "") {
        parentForm.elements[textNameHidden.value].value =
            document.dtForm.elements['DateTimeWindow.DateTime.startDate'].value;
    }

    // transfer the start hour (if any)
    var menu = document.dtForm.elements['DateTimeWindow.DateTime.startHour'];
    if (menu != null) {
        parentForm.elements[targetDTWinName + '.startHour'].value =
            menu.options[menu.selectedIndex].value;
    } else {
        // no start hour means start date only, no need to check any further
        window.close();
    }

    // transfer the start minute (if any)
    menu  = document.dtForm.elements['DateTimeWindow.DateTime.startMinute'];
    if (menu != null) {
        parentForm.elements[targetDTWinName + '.startMinute'].value =
            menu.options[menu.selectedIndex].value;
    }

    // transfer the end hour (if any)
    menu = document.dtForm.elements['DateTimeWindow.DateTime.endHour'];
    if (menu != null) {
        parentForm.elements[targetDTWinName + '.endHour'].value =
            menu.options[menu.selectedIndex].value;
    }

    // transfer the end minute (if any)
    menu = document.dtForm.elements['DateTimeWindow.DateTime.endMinute'];
    if (menu != null) {
        parentForm.elements[targetDTWinName + '.endMinute'].value =
            menu.options[menu.selectedIndex].value;
    }

    // transfer the repeat interval (if any)
    menu = document.dtForm.elements['DateTimeWindow.DateTime.repeatIntervalMenu'];
    if (menu != null) {
        parentForm.elements[targetDTWinName + '.repeatInterval'].value =
            menu.options[menu.selectedIndex].value;
    }

    // transfer the repeat period (if any)
    if (document.dtForm.elements['DateTimeWindow.DateTime.repeatLimitPeriod'] != null) {
        parentForm.elements[targetDTWinName + '.repeatPeriod'].value =
            document.dtForm.elements['DateTimeWindow.DateTime.repeatLimitPeriod'].value;
    }

    // transfer the repeat unit (if any)
    menu = document.dtForm.elements['DateTimeWindow.DateTime.repeatLimitUnit'];
    if (menu != null && menu.selectedIndex != -1) {
        parentForm.elements[targetDTWinName + '.repeatUnit'].value =
            menu.options[menu.selectedIndex].value;
    }
    
    // close the popup window
    window.close();
}

// netscape & mozilla seem to ignore the call to check date from body onload
checkInput();
</script> 

</cc:form>
</cc:header>
</jato:useViewBean>
