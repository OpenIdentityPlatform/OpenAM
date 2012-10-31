// ident "@(#)wizard.js 1.14 04/09/13 SMI"
// 
// Copyright 2004 by Sun Microsystems, Inc. All rights reserved.
// Use is subject to license terms.
//
// This Javascript file should be included in any page that uses the
// CCWizardTag cc:wizard.
// 

//    Once the CCWizardTag is rendered there will exist
//    a javascript object instance called "WizardWindow_Wizard".
//    that provides the following methods
//
//    setFocusElementName(name)
//
//	This method identifies the element in the pagelet that
//	should obtain the focus when the page loads.
//	The name parameter is the "name" attribute of the component tag.
//	It is usually the fully qualified name of the element.
//
//	It returns the name of the element that was last set to
//	have the focus.
//
//	In a popup wizard window based on CCWizardWindowTag
//	cc:wizardwindow, the fully qualified name would be
//
//	WizardWindow.Wizard.<wizardpagecontainerview>.<tagname>
//
//	where "wizardpagecontainerview" is the name of the
//	container view returned by WizardInterface.getPageClass()
//	and "tagname" is the tag's name attribute.
//
//    setNextButtonDisabled(true_or_false, msg)
//    setPreviousButtonDisabled(true_or_false, msg)
//    setFinishButtonDisabled(true_or_false, msg)
//    setCancelButtonDisabled(true_or_false, msg)
//    setCloseButtonDisabled(true_or_false, msg)
//
//	These methods disable the associated button if the 
//	true_or_false parameter is true. If the parameter is false
//	the button is enabled. It returns true if the operation was
//	successful.
//
//	msg is a string that will become the tooltip of the
//	disabled button. If msg is null or true_or_false is false
//	(enabling the button) msg is ignored.
//
//   Since the pagelet markup is read before the cc:wizard tag 
//   is completely rendered, it is not sufficient to just reference
//   the methods in a <script> tag as
//
//      WizardWindow_Wizard.setNextButtonDisabled(true, null);
//
//   since the code will run at the time it is read. Since the
//   tag has not compeletely rendered at this point, the result
//   of this call will be overwritten. There is a member called
//
//	WizardWindow_Wizard.pageInit
//
//   and it should be set to a javascript function name containing
//   statements setting up the page as required on display.
//   For example
//
//	<script type="text/javascript">
//	    function wizardPageInit() {
//		WizardWindow_Wizard.setFocusElementName(
//		    "WizardWindow.Wizard.WizardPage3View.UserIdField");
//	    }
//	    WizardWindow_Wizard.pageInit = wizardPageInit;
//	</script>
//
//   The framework will invoke
//
//	WizardWindow_Wizard.pageInit()
//
//   after the tag has finished rendering.
//   Statements that result in effects at runtime or during user
//   interaction, like enabling a disabled button can occur outside
//   this function, as in event handlers.
//
//   For exmaple this call in the onChange handler will enable the
//   next button which was disabled in a pageInit() call.
//      <td valign="top" align="left" rowspan="1" colspan="1">
//	    <cc:textarea name ="DescriptionText" bundleID="testBundle"
//		cols="20" rows="5" 
//		onChange =
//   "javascript: WizardWindow_Wizard.setNextButtonDisabled(false, null)();"/>
//      </td>
//   
//   Additionally a developer may change the onClick event for any
//   of the wizard buttons by setting the associated xxxClicked member
//   of the WizardWindow_Wizard javascript object.
//   For example in the application's wizardPageInit method the
//   following fragment replaces the default nextClicked method with
//   and application defined javascript method "myNextClicked".
//
//   ...
//       WizardWindow_Wizard.nextClicked = myNextClicked;
//   ...
//
//   The developer must ensure that the semantics of the default method
//   ccWizardNextClicked, as defined in this file, are maintained.
//
//
// jsQualifier uniquiely identifies the components of this
// wizard instance.
// formName is the form that contains the wizard tag
//
function CCWizard(jsQualifier, formName) {

    this.NEXT = 0;
    this.PREVIOUS = 1;
    this.FINISH = 2;
    this.CANCEL = 3;
    this.CLOSE = 4;

    this.buttonState = new Array(5);

    this.buttonState[this.NEXT] =
	new ccWizButtonState(jsQualifier + ".nextButton", null, null);
    this.buttonState[this.PREVIOUS] =
	new ccWizButtonState(jsQualifier + ".previousButton", null, null);
    this.buttonState[this.FINISH] =
	new ccWizButtonState(jsQualifier + ".finishButton", null, null);
    this.buttonState[this.CANCEL] =
	new ccWizButtonState(jsQualifier + ".cancelButton", null, null);
    this.buttonState[this.CLOSE] =
	new ccWizButtonState(jsQualifier + ".cancelButton", null, null);

    this.formName = formName;
    this.jsQualifier = jsQualifier;

    // Methods
    this.setFocusForm = ccWizardSetFocusForm;
    this.setFocusElementName = ccWizardSetFocusElementName;
    this.setNextButtonDisabled = ccWizardSetNextButtonDisabled;
    this.setPreviousButtonDisabled = ccWizardSetPreviousButtonDisabled;
    this.setFinishButtonDisabled = ccWizardSetFinishButtonDisabled;
    this.setCancelButtonDisabled = ccWizardSetCancelButtonDisabled;

    // Developer can set this method and it will be
    // called after the tag has rendered.
    //
    this.pageInit = ccWizardPageInit;

    // Event handlers
    //
    this.nextClicked = ccWizardNextClicked;
    this.previousClicked = ccWizardPreviousClicked;
    this.cancelClicked = ccWizardCancelClicked;
    this.finishClicked = ccWizardFinishClicked;
    this.closeClicked = ccWizardCloseClicked;
    this.gotoStepClicked = ccWizardGotoStepClicked;
 
    // data for posting revisiting previous step warning
    // this should never be set (except perhaps, by a 2.0 app)
    this.revisitWarning = null;

    // maintains the last element that had focus
    this.focusElementName = null;
}

// Button object to hold state for NS4
//
function ccWizButtonState(name, disabled, msg) {

    // Qualified name
    //
    this.name = name;

    // Need to maintaind state for NS 4
    //
    this.disabled = disabled;

    // In Nav 4 the developer must specify a message to instruct
    // the user that a button has been disabled and what to do
    // next
    //
    this.msg = msg;
}

// return true if the operation was successful false if not
function ccWizardSetNextButtonDisabled(t, msg) {
    return ccWizardSetButtonDisabled(this.buttonState[this.NEXT],
	this.formName, t, msg);
}

// return true if the operation was successful false if not
function ccWizardSetCancelButtonDisabled(t, msg) {
    return ccWizardSetButtonDisabled(this.buttonState[this.CANCEL],
	this.formName, t, msg);
}

// return true if the operation was successful false if not
function ccWizardSetFinishButtonDisabled(t, msg) {
    return ccWizardSetButtonDisabled(this.buttonState[this.FINISH],
	this.formName, t, msg);
}

// return true if the operation was successful false if not
function ccWizardSetPreviousButtonDisabled(t, msg) {
    return ccWizardSetButtonDisabled(this.buttonState[this.PREVIOUS],
	this.formName, t, msg);
}

// return true if the operation was successful false if not
function ccWizardSetCloseButtonDisabled(t, msg) {
    return ccWizardSetButtonDisabled(this.buttonState[this.CLOSE],
	this.formName, t, msg);
}
// Save the state and set the message for NS 4
// If msg is null get the button element's DisabledTitle
//
function ccWizardSetButtonDisabled(button, formName, tOrf, msg) {

    var result;

    // If the msg is not null set the disabled title
    // If its nav4 save the state locally
    if (!is_nav4) {
	// Set the disabled title if msg is not null
	//
	if (tOrf == true && msg != null) {
	    ccWizardSetButtonDisabledTitle(button.name, formName, msg);
	}
	result = ccSetButtonDisabled(button.name, formName, tOrf);
    } else {
	button.disabled = tOrf;
	if (tOrf == true) {
	    if (msg != null) {
		button.msg = msg;
	    } else {
		button.msg =
		    ccWizardGetButtonDisabledTitle(button.name, formName);
	    }
	}
	result = true;
    }
    return result;
}

function ccWizardSetButtonDisabledTitle(button, formName, msg) {
    var childNameTitleDisabled = button + "." + "TitleDisabledHiddenField";
    var element = ccGetElement(childNameTitleDisabled, formName);
    if (element != null) {
	element.value = msg;
    }
}

function ccWizardGetButtonDisabledTitle(button, formName) {
    var childNameTitleDisabled = button + "." + "TitleDisabledHiddenField";
    var element = ccGetElement(childNameTitleDisabled, formName);
    if (element != null) {
	return element.value;
    } else {
	return "";
    }
}

// If its not nav4 then the button can't be
// disabled or else we wouldn't be here
//
function ccWizardIsButtonDisabled(button) {

    if (!is_nav4) {
	return false;
    } else {
	if (button.disabled == true) {
	   alert(button.msg);
	   return true;
	} else {
	   return false;
	}
    }
}

function ccWizardNextClicked() {
    return !ccWizardIsButtonDisabled(this.buttonState[this.NEXT]);
}

function ccWizardPreviousClicked() {
    // No confirmation message is displayed, return the button state.
    return !ccWizardIsButtonDisabled(this.buttonState[this.PREVIOUS]);
}

function ccWizardFinishClicked() {
    return !ccWizardIsButtonDisabled(this.buttonState[this.FINISH]);
}

function ccWizardCloseClicked() {
    return !ccWizardIsButtonDisabled(this.buttonState[this.CLOSE]);
}

function ccWizardCancelClicked(confirmMsg) {
    var disabled = ccWizardIsButtonDisabled(this.buttonState[this.CANCEL]);
    if (!disabled && confirmMsg != null) {
	disabled = !confirm(confirmMsg);
    }
    return !disabled;
}

function ccWizardGotoStepClicked() {
    return true;
}

function ccWizardPageInit() {
}

// Return the last form that had the focus
function ccWizardSetFocusForm(f) {
    var lastform = this.formName;
    if (f != null) {
	this.formName = f;
    }
    return lastform;
}

// Return the last element that had focus.
function ccWizardSetFocusElementName(e) {
    var lastfocusel = this.focusElementName;
    if (e != null) {
	this.focusElementName = e;
    }
    return lastfocusel;
}

// wizardObj is the js wizard object instance
// Is resize only relevant in popup windows ?
//
function wizOnLoad(wizardObj) {

    var elem = null;
    if (wizardObj != null && wizardObj.focusElementName != null &&
	    wizardObj.formName != null) {
	elem = ccGetElement(wizardObj.focusElementName, wizardObj.formName);
    }
    resize_hack();
    if (elem != null) {
	elem.focus();
    } 
}

// used only for popup window and IE
function resize_hack () {
    if (is_ie5up) {
	var bdy = document.getElementById('WizBdy');
	if (bdy != null) {
	    bdy.style.height = document.body.clientHeight - 145;
	    if (document.getElementById('help')) {
		document.getElementById('help').style.height =
		    document.body.clientHeight - 90
	    }
	    if (document.getElementById('steps')) {
		document.getElementById('steps').style.height =
		    document.body.clientHeight - 90
	    }
	}
    }
}

function ccWizardForwardAndClose(refreshForm, refreshCmdChild) {
    if (refreshForm != '' && refreshCmdChild != '') {
	var f = window.opener.document.forms[refreshForm];

	// Get action URI and query substrings.
	var uri = "";
	var url = f.action;
	var queryParams = "";
	var index = url.indexOf("?");

	if (index == -1) {
	    uri = url; // Use URL when query params are not found.
	} else {
	    uri = url.substring(0, index);
	    queryParams = "&" + url.substring(index + 1, url.length);
	}

	// Set form action url and submit.
	f.action = uri + "?" + refreshCmdChild + "=" + queryParams;
	f.submit();
    }
    close();
}

// return true if user clicks OK to revisit previous page
// For backward compatibility only - always return true.
function ccWizardConfirmRevisitStep(revisitWarning) {
    return true;
}
