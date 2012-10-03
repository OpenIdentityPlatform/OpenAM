//<!--
//
// ident "@(#)dynamic.js 1.20 04/09/20 SMI"
// 
// Copyright 2004 by Sun Microsystems, Inc. All rights reserved.
// Use is subject to license terms.
//
// This Javascript code will provide methods for dynamic enabling and
// disabling of Common Console components. In addition, this script
// uses the variables of browserVersion.js for client-side browser
// sniffing. Always include both files in your JSP page.

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Common methods
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// Get the document form for the given name.
function ccGetForm(formName) {
    if (formName == null) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("formName parameter is null.");
	return null; 
    }

    // Get form.
    var form = eval("document." + formName);

    return form;
}

// Get the document element for the given name.
function ccGetElement(elementName, formName) {
    // Validate params.
    if (elementName == null) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("elementName parameter is null.");
	return null;
    }

    // Get form
    var form = ccGetForm(formName);

    if (form == null) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid formName parameter: " + formName);
	return null;
    }

    // Get element.
    var element = form.elements[elementName];

    return element;
}

// Get all document elements for the given name. Note: there could be
// more than one element by the same name on a given page (e.g.,
// upper and lower action table buttons/menus).
function ccGetElements(elementName, formName) {
    // Validate params.
    if (elementName == null) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("elementName parameter is null.");
	return null;
    }

    // Get form
    var form = ccGetForm(formName);

    if (form == null) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid formName parameter: " + formName);
	return null;
    }

    // Get elements.
    var elements = new Array();

    for (i = 0, k = 0; i < form.elements.length; i++) {
	var e = form.elements[i];
	if (e.name == elementName) {
	    elements[k] = e;
	    k++
	}
    }

    return elements;
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Button methods
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// Set the disabled state of CC buttons for given element and form names.
function ccSetButtonDisabled(elementName, formName, disabled) {
    // Hidden field names.
    var childNameDisabled      = elementName + "." + "DisabledHiddenField";
    var childNameTitleDisabled = elementName + "." + "TitleDisabledHiddenField";
    var childNameTitleEnabled  = elementName + "." + "TitleEnabledHiddenField";

    // Get element.
    var element = ccGetElement(elementName, formName);

    // Set button disabled state.
    if (element != null && element.name) {
	return ccSetButtonState(element,
	    ccGetElement(childNameDisabled, formName),
	    ccGetElement(childNameTitleDisabled, formName),
	    ccGetElement(childNameTitleEnabled, formName),
	    disabled);
    } else {
	// There could be more one element by the same name on a given
	// page (e.g., upper and lower action table buttons/menus), so
	// we need to set the disabled state for all.
	var elements = ccGetElements(elementName, formName);
	var disabledHiddenFields =
	    ccGetElements(childNameDisabled, formName);
	var titleDisabledHiddenFields =
	    ccGetElements(childNameTitleDisabled, formName);
	var titleEnabledHiddenFields =
	    ccGetElements(childNameTitleEnabled, formName);

	for (i = 0; elements != null && i < elements.length; i++) {
	    var result = ccSetButtonState(
		elements[i],
		(disabledHiddenFields != null && i < disabledHiddenFields.length)
		    ? disabledHiddenFields[i] : null,
		(titleDisabledHiddenFields != null && i < titleDisabledHiddenFields.length)
		    ? titleDisabledHiddenFields[i] : null,
		(titleEnabledHiddenFields != null && i < titleEnabledHiddenFields.length)
		    ? titleEnabledHiddenFields[i] : null,
		disabled);

	    if (!result)
		return false;
	}
    }

    return true;
}

// Set the state of CC buttons for given elements.
function ccSetButtonState(element, disabledHiddenField,
	titleDisabledHiddenField, titleEnabledHiddenField, disabled) {
    // Class names.
    var classNamePrimary               = "Btn1";
    var classNamePrimaryDisabled       = "Btn1Dis";
    var classNamePrimaryMini           = "Btn1Mni";
    var classNamePrimaryMiniDisabled   = "Btn1MniDis";
    var classNameSecondary             = "Btn2";
    var classNameSecondaryDisabled     = "Btn2Dis";
    var classNameSecondaryMini         = "Btn2Mni";
    var classNameSecondaryMiniDisabled = "Btn2MniDis";

    // Validate params.
    if (!ccValidateButton(element, disabledHiddenField, disabled))
	return false;

    // Get boolean value to ensure correct data type.
    disabled = new Boolean(disabled).valueOf();

    // Set disabled state.
    disabledHiddenField.value = disabled;
    element.disabled = disabled;

    // Set styles.
    if (disabled) {
	// Set title attribute.
	if (titleDisabledHiddenField != null)
	    element.title = titleDisabledHiddenField.value;

	// Set class attribute.
	if (element.className.indexOf(classNamePrimaryMini) != -1) {
	    element.className = classNamePrimaryMiniDisabled;
	} else if (element.className.indexOf(classNamePrimary) != -1) {
	    element.className = classNamePrimaryDisabled;
	} else if (element.className.indexOf(classNameSecondaryMini) != -1) {
	    element.className = classNameSecondaryMiniDisabled;
	} else if (element.className.indexOf(classNameSecondary) != -1) {
	    element.className = classNameSecondaryDisabled;
	}
    } else {
	// Set title attribute.
	if (titleEnabledHiddenField != null)
	    element.title = titleEnabledHiddenField.value;

	// Set class attribute.
	if (element.className.indexOf(classNamePrimaryMini) != -1) {
	    element.className = classNamePrimaryMini;
	} else if (element.className.indexOf(classNamePrimary) != -1) {
	    element.className = classNamePrimary;
	} else if (element.className.indexOf(classNameSecondaryMini) != -1) {
	    element.className = classNameSecondaryMini;
	} else if (element.className.indexOf(classNameSecondary) != -1) {
	    element.className = classNameSecondary;
	}
    }

    return true;
}

// Helper method to validate CC buttons.
function ccValidateButton(element, disabledHiddenField, disabled) {
    // Class names.
    var classNameDefault = "Btn1Def";

    // Netscape 4.x does not support disabled state, just return.
    if (is_nav4) {
	return false;
    }

    // Validate element type.
    if (element == null
	    || !element.name
	    || element.type.toLowerCase() != "submit") {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid element type.");
	return false;
    }

    // Validate element class name. The default button cannot be disabled.
    if (element.className == null
	    || element.className.indexOf("Btn") == -1
	    || element.className.indexOf(classNameDefault) != -1) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid element class name.");
	return false;
    }

    // Validate disabled hidden field.
    if (disabledHiddenField == null) {
	// If dynamic attribute was not set, no hidden fields will be
	// output by the tag.
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Disabled state cannot be set.");
	return false;
    }

    // Validate disabled param.
    if (disabled == null) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("disabled parameter is null.");
	return false;
    }

    return true;
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Drop down menu methods
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// Set the disabled state of CC drop down menus for given element and
// form names.
function ccSetDropDownMenuDisabled(elementName, formName, disabled) {
    // Hidden field names.
    var childNameDisabled      = elementName + "." + "DisabledHiddenField";
    var childNameTitleDisabled = elementName + "." + "TitleDisabledHiddenField";
    var childNameTitleEnabled  = elementName + "." + "TitleEnabledHiddenField";
    var childNameOptionDisabled = elementName + "." +
	"OptionDisabledHiddenField";

    // Get element.
    var element = ccGetElement(elementName, formName);

    // Set disabled state.
    if (element != null && element.name) {
	return ccSetDropDownMenuState(element,
	    ccGetElement(childNameDisabled, formName),
	    ccGetElement(childNameTitleDisabled, formName),
	    ccGetElement(childNameTitleEnabled, formName),
	    ccGetElement(childNameOptionDisabled, formName),
	    disabled);
    } else {
	// There could be more one element by the same name on a given
	// page (e.g., upper and lower action table buttons/menus), so
	// we need to set the disabled state for all.
	var elements = ccGetElements(elementName, formName);
	var disabledHiddenFields =
	    ccGetElements(childNameDisabled, formName);
	var titleDisabledHiddenFields =
	    ccGetElements(childNameTitleDisabled, formName);
	var titleEnabledHiddenFields =
	    ccGetElements(childNameTitleEnabled, formName);
	var optionDisabledHiddenFields =
	    ccGetElements(childNameOptionDisabled, formName);

	for (i = 0; elements != null && i < elements.length; i++) {
	    var result = ccSetDropDownMenuState(
		elements[i],
		(disabledHiddenFields != null && i < disabledHiddenFields.length)
		    ? disabledHiddenFields[i] : null,
		(titleDisabledHiddenFields != null && i < titleDisabledHiddenFields.length)
		    ? titleDisabledHiddenFields[i] : null,
		(titleEnabledHiddenFields != null && i < titleEnabledHiddenFields.length)
		    ? titleEnabledHiddenFields[i] : null,
		(optionDisabledHiddenFields != null && i < optionDisabledHiddenFields.length)
		    ? optionDisabledHiddenFields[i] : null,
		disabled);

	    if (!result)
		return false;
	}
    }

    return true;
}

// Set the state of CC drop down menus for given elements.
function ccSetDropDownMenuState(element, disabledHiddenField,
	titleDisabledHiddenField, titleEnabledHiddenField,
	optionDisabledHiddenField, disabled) {
    // Validate params.
    if (!ccValidateDropDownMenu(element, disabledHiddenField, disabled))
	return false;

    // Get boolean value to ensure correct data type.
    disabled = new Boolean(disabled).valueOf();

    // Set disabled state.
    disabledHiddenField.value = disabled;
    element.disabled = disabled;

    // Set flag indicating option was selected while HTML element was
    // disabled. Do not set disabled options as selected.
    //
    // "false" = enabled
    // "FALSE" = enabled and selected
    //
    if (element.selectedIndex >= 0) {
	// Get element option.
	var option = element.options[element.selectedIndex];

	if (optionDisabledHiddenField != null && !option.disabled) {
	    optionDisabledHiddenField.value = "FALSE";
	}
    }

    if (disabled) {
	if (titleDisabledHiddenField != null)
	    element.title = titleDisabledHiddenField.value;
    } else {
	if (titleEnabledHiddenField != null)
	    element.title = titleEnabledHiddenField.value;
    }

    return true;
}

// Helper method to validate CC drop down menus.
function ccValidateDropDownMenu(element, disabledHiddenField, disabled) {
    // Class names.
    var classNameJump     = "MnuJmp";
    var classNameStandard = "MnuStd";

    // Netscape 4.x does not support disabled state, just return.
    if (is_nav4) {
	return false;
    }

    // Validate element type.
    if (element == null
	    || !element.name
	    || element.type.toLowerCase() != "select-one") {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid element type.");
	return false;
    }

    // Validate element class name.
    if (element.className == null
	    || !(element.className == classNameJump
	    ||   element.className == classNameStandard)) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid element class name.");
	return false;
    }

    // Validate disabled hidden field.
    if (disabledHiddenField == null) {
	// If dynamic attribute was not set, no hidden fields will be
	// output by the tag.
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Disabled state cannot be set.");
	return false;
    }

    // Validate disabled param.
    if (disabled == null) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("disabled parameter is null.");
	return false;
    }

    return true;
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Drop down menu option methods
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// Set the disabled state of CC drop down menu options for given
// element and form names.
function ccSetDropDownMenuOptionDisabled(elementName, formName, disabled,
	index) {
    // Hidden field names.
    var childNameDisabled      = elementName + "." +
	"OptionDisabledHiddenField" + index;
    var childNameTitleDisabled = elementName + "." +
	"OptionTitleDisabledHiddenField" + index;
    var childNameTitleEnabled  = elementName + "." +
	"OptionTitleEnabledHiddenField" + index;

    // Get element.
    var element = ccGetElement(elementName, formName);

    // Set disabled state.
    if (element != null && element.name) {
	return ccSetDropDownMenuOptionState(element,
	    ccGetElement(childNameDisabled, formName),
	    ccGetElement(childNameTitleDisabled, formName),
	    ccGetElement(childNameTitleEnabled, formName),
	    disabled, index);
    } else {
	// There could be more one element by the same name on a given
	// page (e.g., upper and lower action table buttons/menus), so
	// we need to set the disabled state for all.
	var elements = ccGetElements(elementName, formName);
	var disabledHiddenFields =
	    ccGetElements(childNameDisabled, formName);
	var titleDisabledHiddenFields =
	    ccGetElements(childNameTitleDisabled, formName);
	var titleEnabledHiddenFields =
	    ccGetElements(childNameTitleEnabled, formName);

	for (i = 0; elements != null && i < elements.length; i++) {
	    var result = ccSetDropDownMenuOptionState(
		elements[i],
		(disabledHiddenFields != null && i < disabledHiddenFields.length)
		    ? disabledHiddenFields[i] : null,
		(titleDisabledHiddenFields != null && i < titleDisabledHiddenFields.length)
		    ? titleDisabledHiddenFields[i] : null,
		(titleEnabledHiddenFields != null && i < titleEnabledHiddenFields.length)
		    ? titleEnabledHiddenFields[i] : null,
		disabled, index);

	    if (!result)
		return false;
	}
    }

    return true;
}

// Set the state of CC drop down menu options for given elements.
function ccSetDropDownMenuOptionState(element, disabledHiddenField,
	titleDisabledHiddenField, titleEnabledHiddenField,
	disabled, index) {
    // Class names.
    var classNameJumpOption             = "MnuJmpOpt";
    var classNameJumpOptionDisabled     = "MnuJmpOptDis";
    var classNameStandardOption         = "MnuStdOpt";
    var classNameStandardOptionDisabled = "MnuStdOptDis";

    // Validate params.
    if (!ccValidateDropDownMenuOption(element, disabledHiddenField,
	    disabled, index)) {
	return false;
    }

    // Get element option.
    var option = element.options[index];

    // Get boolean value to ensure correct data type.
    disabled = new Boolean(disabled).valueOf();

    // Set disabled state.
    disabledHiddenField.value = disabled;
    option.disabled = disabled;

    if (disabled) {
	// Set title attribute.
	if (titleDisabledHiddenField != null)
	    option.title = titleDisabledHiddenField.value;

	// Set class attribute.
	if (option.className.indexOf(classNameJumpOption) != -1) {
	    option.className = classNameJumpOptionDisabled;
	} else {
	    option.className = classNameStandardOptionDisabled;
	}
    } else {
	// Set title attribute.
	if (titleEnabledHiddenField != null)
	    option.title = titleEnabledHiddenField.value;

	// Set class attribute.
	if (option.className.indexOf(classNameJumpOption) != -1) {
	    option.className = classNameJumpOption;
	} else {
	    option.className = classNameStandardOption;
	}
    }

    return true;
}

// Helper method to validate CC drop down menus.
function ccValidateDropDownMenuOption(element, disabledHiddenField,
	disabled, index) {
    // Class names.
    var classNameJump                    = "MnuJmp";
    var classNameJumpOption              = "MnuJmpOpt";
    var classNameJumpOptionSelected      = "MnuJmpOptSel";
    var classNameJumpOptionSeparator     = "MnuJmpOptSep";
    var classNameStandard                = "MnuStd";
    var classNameStandardOption          = "MnuStdOpt";
    var classNameStandardOptionSelected  = "MnuStdOptSel";
    var classNameStandardOptionSeparator = "MnuStdOptSep";

    // Netscape 4.x does not support disabled state, just return.
    if (is_nav4) {
	return false;
    }

    // Validate element type.
    if (element == null
	    || !element.name
	    || element.type.toLowerCase() != "select-one") {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid element type.");
	return false;
    }

    // Validate element class name.
    if (element.className == null
	    || !(element.className == classNameJump
	    ||   element.className == classNameStandard)) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid element class name.");
	return false;
    }

    // Validate disabled hidden field.
    if (disabledHiddenField == null) {
	// If dynamic attribute was not set, no hidden fields will be
	// output by the tag.
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Disabled state cannot be set.");
	return false;
    }

    // Validate disabled param.
    if (disabled == null) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("disabled parameter is null.");
	return false;
    }

    // Validate index.
    if (index == null || index < 0) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("index parameter is null or < 0.");
	return false;
    }

    // Validate index against option length.
    if (!(index < element.options.length)) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("index parameter > option length.");
	return false;
    }

    // Get element option.
    var option = element.options[index];

    // Validate element class name. The "none selected" and separator
    // options cannot be disabled.
    if (option.className == null
	    || (option.className.indexOf(classNameJumpOption) == -1
		&& option.className.indexOf(classNameStandardOption) == -1)
	    || option.className.indexOf(
		classNameJumpOptionSelected) != -1
	    || option.className.indexOf(
		classNameJumpOptionSeparator) != -1
	    || option.className.indexOf(
		classNameStandardOptionSelected) != -1
	    || option.className.indexOf(
		classNameStandardOptionSeparator) != -1) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid element class name.");
	return false;
    }

    return true;
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Selectable list methods
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// Set the disabled state of CC selectable lists for given element and
// form names.
function ccSetSelectableListDisabled(elementName, formName, disabled) {
    // Hidden field names.
    var childNameDisabled      = elementName + "." + "DisabledHiddenField";
    var childNameTitleDisabled = elementName + "." + "TitleDisabledHiddenField";
    var childNameTitleEnabled  = elementName + "." + "TitleEnabledHiddenField";
    var childNameOptionDisabled = elementName + "." +
	"OptionDisabledHiddenField";

    // Get element.
    var element = ccGetElement(elementName, formName);

    // Set disabled state.
    if (element != null && element.name) {
	return ccSetSelectableListState(element,
	    ccGetElement(childNameDisabled, formName),
	    ccGetElement(childNameTitleDisabled, formName),
	    ccGetElement(childNameTitleEnabled, formName),
	    ccGetElement(childNameOptionDisabled, formName),
	    disabled);
    } else {
	// There could be more one element by the same name on a given
	// page (e.g., upper and lower action table buttons/menus), so
	// we need to set the disabled state for all.
	var elements = ccGetElements(elementName, formName);
	var disabledHiddenFields =
	    ccGetElements(childNameDisabled, formName);
	var titleDisabledHiddenFields =
	    ccGetElements(childNameTitleDisabled, formName);
	var titleEnabledHiddenFields =
	    ccGetElements(childNameTitleEnabled, formName);
	var optionDisabledHiddenFields =
	    ccGetElements(childNameOptionDisabled, formName);

	for (i = 0; elements != null && i < elements.length; i++) {
	    var result = ccSetSelectableListState(
		elements[i],
		(disabledHiddenFields != null && i < disabledHiddenFields.length)
		    ? disabledHiddenFields[i] : null,
		(titleDisabledHiddenFields != null && i < titleDisabledHiddenFields.length)
		    ? titleDisabledHiddenFields[i] : null,
		(titleEnabledHiddenFields != null && i < titleEnabledHiddenFields.length)
		    ? titleEnabledHiddenFields[i] : null,
		(optionDisabledHiddenFields != null && i < optionDisabledHiddenFields.length)
		    ? optionDisabledHiddenFields[i] : null,
		disabled);

	    if (!result)
		return false;
	}
    }

    return true;
}

// Set the state of CC selectable lists for given elements.
function ccSetSelectableListState(element, disabledHiddenField,
	titleDisabledHiddenField, titleEnabledHiddenField,
	optionDisabledHiddenField, disabled) {
    // Class names.
    var classNameList                  = "Lst";
    var classNameListDisabled          = "LstDis";
    var classNameListMonospace         = "LstMno";
    var classNameListMonospaceDisabled = "LstMnoDis";

    // Validate params.
    if (!ccValidateSelectableList(element, disabledHiddenField, disabled))
	return false;

    // Get boolean value to ensure correct data type.
    disabled = new Boolean(disabled).valueOf();

    // Set disabled state.
    disabledHiddenField.value = disabled;
    element.disabled = disabled;

    // Set value to indicate option was selected while the 
    // HTML element was disabled. Do not set disabled options 
    // as selected.
    //
    // "false" = enabled
    // "FALSE" = enabled and selected
    //
    for (i = 0; i < element.options.length; i++) {
	// Get element option.
	var option = element.options[i];

	if (option.selected && !option.disabled
		&& optionDisabledHiddenField != null) {
	    optionDisabledHiddenField.value = "FALSE";
	}
    }

    if (disabled) {
	// Set title attribute.
	if (titleDisabledHiddenField != null)
	    element.title = titleDisabledHiddenField.value;

	// Set class attribute.
	if (element.className.indexOf(classNameListMonospace) != -1) {
	    element.className = classNameListMonospaceDisabled;
	} else {
	    element.className = classNameListDisabled;
	}
    } else {
	// Set title attribute.
	if (titleEnabledHiddenField != null)
	    element.title = titleEnabledHiddenField.value;

	// Set class attribute.
	if (element.className.indexOf(classNameListMonospace) != -1) {
	    element.className = classNameListMonospace;
	} else {
	    element.className = classNameList;
	}
    }

    return true;
}

// Helper method to validate CC selectable lists.
function ccValidateSelectableList(element, disabledHiddenField, disabled) {
    // Class names.
    var classNameList = "Lst";

    // Netscape 4.x does not support disabled state, just return.
    if (is_nav4) {
	return false;
    }

    // Validate element type.
    if (element == null
	    || !(element.type.toLowerCase() == "select-one"
	    ||   element.type.toLowerCase() == "select-multiple")) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid element type.");
	return false;
    }

    // Validate element class name.
    if (element.className == null 
	    || element.className.indexOf(classNameList) == -1) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid element class name.");
	return false;
    }

    // Validate disabled hidden field.
    if (disabledHiddenField == null) {
	// If dynamic attribute was not set, no hidden fields will be
	// output by the tag.
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Disabled state cannot be set.");
	return false;
    }

    // Validate disabled param.
    if (disabled == null) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("disabled parameter is null.");
	return false;
    }

    return true;
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Selectable list option methods
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// Set the disabled state of CC selectable list options for given
// element and form names.
function ccSetSelectableListOptionDisabled(elementName, formName, disabled,
	index) {
    // Hidden field names.
    var childNameDisabled      = elementName + "." +
	"OptionDisabledHiddenField" + index;
    var childNameTitleDisabled = elementName + "." +
	"OptionTitleDisabledHiddenField" + index;
    var childNameTitleEnabled  = elementName + "." +
	"OptionTitleEnabledHiddenField" + index;

    // Get element.
    var element = ccGetElement(elementName, formName);

    // Set disabled state.
    if (element != null && element.name) {
	return ccSetSelectableListOptionState(element,
	    ccGetElement(childNameDisabled, formName),
	    ccGetElement(childNameTitleDisabled, formName),
	    ccGetElement(childNameTitleEnabled, formName),
	    disabled, index);
    } else {
	// There could be more one element by the same name on a given
	// page (e.g., upper and lower action table buttons/menus), so
	// we need to set the disabled state for all.
	var elements = ccGetElements(elementName, formName);
	var disabledHiddenFields =
	    ccGetElements(childNameDisabled, formName);
	var titleDisabledHiddenFields =
	    ccGetElements(childNameTitleDisabled, formName);
	var titleEnabledHiddenFields =
	    ccGetElements(childNameTitleEnabled, formName);

	for (i = 0; elements != null && i < elements.length; i++) {
	    var result = ccSetSelectableListOptionState(
		elements[i],
		(disabledHiddenFields != null && i < disabledHiddenFields.length)
		    ? disabledHiddenFields[i] : null,
		(titleDisabledHiddenFields != null && i < titleDisabledHiddenFields.length)
		    ? titleDisabledHiddenFields[i] : null,
		(titleEnabledHiddenFields != null && i < titleEnabledHiddenFields.length)
		    ? titleEnabledHiddenFields[i] : null,
		disabled, index);

	    if (!result)
		return false;
	}
    }

    return true;
}

// Set the state of CC selectable list options for given elements.
function ccSetSelectableListOptionState(element, disabledHiddenField,
	titleDisabledHiddenField, titleEnabledHiddenField,
	disabled, index) {
    // Class names.
    var classNameOption         = "LstOpt";
    var classNameOptionDisabled = "LstOptDis";

    // Validate params.
    if (!ccValidateSelectableListOption(element, disabledHiddenField,
	    disabled, index)) {
	return false;
    }

    // Get element option.
    var option = element.options[index];

    // Get boolean value to ensure correct data type.
    disabled = new Boolean(disabled).valueOf();

    // Set disabled state.
    disabledHiddenField.value = disabled;
    option.disabled = disabled;

    if (disabled) {
	// Set title attribute.
	if (titleDisabledHiddenField != null)
	    option.title = titleDisabledHiddenField.value;

	// Set class attribute.
	option.className = classNameOptionDisabled;
    } else {
	// Set title attribute.
	if (titleEnabledHiddenField != null)
	    option.title = titleEnabledHiddenField.value;

	// Set class attribute.
	option.className = classNameOption;
    }

    return true;
}

// Helper method to validate CC selectable list options.
function ccValidateSelectableListOption(element, disabledHiddenField,
	disabled, index) {
    // Class names.
    var classNameList            = "Lst";
    var classNameOption          = "LstOpt";
    var classNameOptionSelected  = "LstOptSel";
    var classNameOptionSeparator = "LstOptSep";

    // Netscape 4.x does not support disabled state, just return.
    if (is_nav4) {
	return false;
    }

    // Validate element type.
    if (element == null
	    || !(element.type.toLowerCase() == "select-one"
	    ||   element.type.toLowerCase() == "select-multiple")) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid element type.");
	return false;
    }

    // Validate element class name.
    if (element.className == null 
	    || element.className.indexOf(classNameList) == -1) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid element class name.");
	return false;
    }

    // Validate disabled hidden field.
    if (disabledHiddenField == null) {
	// If dynamic attribute was not set, no hidden fields will be
	// output by the tag.
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Disabled state cannot be set.");
	return false;
    }

    // Validate disabled param.
    if (disabled == null) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("disabled parameter is null.");
	return false;
    }

    // Validate index.
    if (index == null || index < 0) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("index parameter is null or < 0.");
	return false;
    }

    // Validate index against option length.
    if (!(index < element.options.length)) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("index parameter > option length.");
	return false;
    }

    // Get element option.
    var option = element.options[index];

    // Validate element class name. The "none selected" and separator
    // options cannot be disabled.
    if (option.className == null
	    || option.className.indexOf(classNameOption) == -1
	    || option.className.indexOf(classNameOptionSelected) != -1
	    || option.className.indexOf(classNameOptionSeparator) != -1) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid element class name.");
	return false;
    }

    return true;
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Text Field methods
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// Set the disabled state of CC text fields.
function ccSetTextFieldDisabled(elementName, formName, disabled) {
    // Hidden field names.
    var childNameDisabled      = elementName + "." +
	"DisabledHiddenField";
    var childNameTitleDisabled = elementName + "." +
	"TitleDisabledHiddenField";
    var childNameTitleEnabled  = elementName + "." +
	"TitleEnabledHiddenField";
    var childNameValueDisabled = elementName + "." +
	"ValueDisabledHiddenField";

    // Get element.
    var element = ccGetElement(elementName, formName);

    // Set disabled state.
    if (element != null && element.name) {
	return ccSetTextFieldState(element,
	    ccGetElement(childNameDisabled, formName),
	    ccGetElement(childNameTitleDisabled, formName),
	    ccGetElement(childNameTitleEnabled, formName),
	    ccGetElement(childNameValueDisabled, formName),
	    disabled);
    } else {
	// There could be more one element by the same name on a given
	// page (e.g., upper and lower action table buttons/menus), so
	// we need to set the disabled state for all.
	var elements = ccGetElements(elementName, formName);
	var disabledHiddenFields =
	    ccGetElements(childNameDisabled, formName);
	var titleDisabledHiddenFields =
	    ccGetElements(childNameTitleDisabled, formName);
	var titleEnabledHiddenFields =
	    ccGetElements(childNameTitleEnabled, formName);
	var valueDisabledHiddenFields =
	    ccGetElements(childNameValueDisabled, formName);

	for (i = 0; elements != null && i < elements.length; i++) {
	    var result = ccSetTextFieldState(
		elements[i],
		(disabledHiddenFields != null && i < disabledHiddenFields.length)
		    ? disabledHiddenFields[i] : null,
		(titleDisabledHiddenFields != null && i < titleDisabledHiddenFields.length)
		    ? titleDisabledHiddenFields[i] : null,
		(titleEnabledHiddenFields != null && i < titleEnabledHiddenFields.length)
		    ? titleEnabledHiddenFields[i] : null,
		(valueDisabledHiddenFields != null && i < valueDisabledHiddenFields.length)
		    ? valueDisabledHiddenFields[i] : null,
		disabled);

	    if (!result)
		return false;
	}
    }

    return true;
}

// Set the state of CC text fields.
function ccSetTextFieldState(element, disabledHiddenField, 
	titleDisabledHiddenField, titleEnabledHiddenField,
	valueDisabledHiddenField, disabled) {
    // Style Class names.
    var classNameTextField 	   = "TxtFld";
    var classNameTextFieldDisabled = "TxtFldDis";
    var classNameTextArea 	   = "TxtAra";
    var classNameTextAreaDisabled  = "TxtAraDis";
    	

    // Netscape 4.x does not support disabled state, just return.
    if (is_nav4) {
	return false;
    }

    // Validate element type. 
    if (element == null  
            || !(element.type.toLowerCase() == "text" 
                || element.type.toLowerCase() == "textarea" 
                || element.type.toLowerCase() == "password")) { 
        // Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid element type."); 
        return false; 
    } 
 
    // Validate disabled hidden field.
    if (disabledHiddenField == null) {
	// If dynamic attribute was not set, no hidden fields will be
	// output by the tag.
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Disabled state cannot be set.");
	return false;
    }

    // Validate disabled param.
    if (disabled == null) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("disabled parameter is null.");
	return false;
    }

    // Get boolean value to ensure correct data type.
    disabled = new Boolean(disabled).valueOf();

    // Set disabled state.
    disabledHiddenField.value = disabled;
    element.disabled = disabled;

    if (disabled) {
	// Set title attribute.
	if (titleDisabledHiddenField != null)
	    element.title = titleDisabledHiddenField.value;

	// Set value attribute.
	if (valueDisabledHiddenField != null)
	    valueDisabledHiddenField.value = element.value;

	// Set class attribute - textfield and password use the same classes
	if (element.type.toLowerCase() == "textarea") 
	    element.className = classNameTextAreaDisabled;
	else 
	    element.className = classNameTextFieldDisabled;
    } else {
	// Set title attribute.
	if (titleEnabledHiddenField != null)
	    element.title = titleEnabledHiddenField.value;

	// Set class attribute - textfield and password use the same classes
	if (element.type.toLowerCase() == "textarea") 
	    element.className = classNameTextArea;
	else 
	    element.className = classNameTextField;
    }

    return true;
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Text Area methods
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// Toggle the disabled state of CC text areas.
function ccSetTextAreaDisabled(elementName, formName, disabled) {
    return ccSetTextFieldDisabled(elementName, formName, disabled);
}

/// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// CheckBox methods
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// Set the disabled state of CC checkboxes.
function ccSetCheckBoxDisabled(elementName, formName, disabled) {
    // Hidden field names.
    var childNameDisabled      = elementName + "." +
	"DisabledHiddenField";
    var childNameTitleDisabled = elementName + "." +
	"TitleDisabledHiddenField";
    var childNameTitleEnabled  = elementName + "." +
	"TitleEnabledHiddenField";
    var childNameValueDisabled = elementName + "." +
	"ValueDisabledHiddenField";

    // Get element.
    var element = ccGetElement(elementName, formName);

    // Set disabled state.
    if (element != null && element.name) {
	return ccSetCheckBoxState(element,
	    ccGetElement(childNameDisabled, formName),
	    ccGetElement(childNameTitleDisabled, formName),
	    ccGetElement(childNameTitleEnabled, formName),
	    ccGetElement(childNameValueDisabled, formName),
	    disabled);
    } else {
	// There could be more one element by the same name on a given
	// page (e.g., upper and lower action table buttons/menus), so
	// we need to set the disabled state for all.
	var elements = ccGetElements(elementName, formName);
	var disabledHiddenFields =
	    ccGetElements(childNameDisabled, formName);
	var titleDisabledHiddenFields =
	    ccGetElements(childNameTitleDisabled, formName);
	var titleEnabledHiddenFields =
	    ccGetElements(childNameTitleEnabled, formName);
	var valueDisabledHiddenFields =
	    ccGetElements(childNameValueDisabled, formName);

	for (i = 0; elements != null && i < elements.length; i++) {
	    var result = ccSetCheckBoxOptionState(
		elements[i],
		(disabledHiddenFields != null && i < disabledHiddenFields.length)
		    ? disabledHiddenFields[i] : null,
		(titleDisabledHiddenFields != null && i < titleDisabledHiddenFields.length)
		    ? titleDisabledHiddenFields[i] : null,
		(titleEnabledHiddenFields != null && i < titleEnabledHiddenFields.length)
		    ? titleEnabledHiddenFields[i] : null,
		(valueDisabledHiddenFields != null && i < valueDisabledHiddenFields.length)
		    ? valueDisabledHiddenFields[i] : null,
		disabled);

	    if (!result)
		return false;
	}
    }

    return true;
}

// Set the state of CC checkboxes.
function ccSetCheckBoxState(element, disabledHiddenField, 
	titleDisabledHiddenField, titleEnabledHiddenField,
	valueDisabledHiddenField, disabled) {
    // Class names.
    var classNameCheckBox 	  = "Cb";
    var classNameCheckBoxDisabled = "CbDis";			

    // Netscape 4.x does not support disabled state, just return.
    if (is_nav4) {
	return false;
    }

    // Validate element type.
    if (element == null || element.type.toLowerCase() != "checkbox") {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid element type.");
	return false;
    }

    // Validate disabled hidden field.
    if (disabledHiddenField == null) {
	// If dynamic attribute was not set, no hidden fields will be
	// output by the tag.
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Disabled state cannot be set.");
	return false;
    }

    // Validate disabled param.
    if (disabled == null) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("disabled parameter is null.");
	return false;
    }

    // Validate element class name.
    if (is_gecko && (element.className == null
	    || !(element.className == classNameCheckBox
	    ||   element.className == classNameCheckBoxDisabled))) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid element class name.");
	return false;
    }

    // Get boolean value to ensure correct data type.
    disabled = new Boolean(disabled).valueOf();

    // Set disabled state.
    disabledHiddenField.value = disabled;
    element.disabled = disabled;

    if (disabled) {
	// Set title attribute.
	if (titleDisabledHiddenField != null)
	    element.title = titleDisabledHiddenField.value;

	// Set value attribute.
	if (valueDisabledHiddenField != null)
	    valueDisabledHiddenField.value = element.checked;

	// Set class attribute.
	if (is_gecko)
	    element.className = classNameCheckBoxDisabled;
    } else {
	// Set title attribute.
	if (titleEnabledHiddenField != null)
	    element.title = titleEnabledHiddenField.value;

	// Set class attribute.
	if (is_gecko)
	    element.className = classNameCheckBox;
    }

    return true;
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// RadioButton methods
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// Set the disabled state of CC radiobuttons.
function ccSetRadioButtonDisabled(elementName, formName, disabled) {
    // Hidden field names.
    var childNameDisabled      = elementName + "." +
	"DisabledHiddenField";
    var childNameTitleDisabled = elementName + "." +
	"TitleDisabledHiddenField";
    var childNameTitleEnabled  = elementName + "." +
	"TitleEnabledHiddenField";
    var childNameValueDisabled = elementName + "." +
	"ValueDisabledHiddenField";
	
    // Validate params.
    if (elementName == null) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("elementName parameter is null.");
	return false;
    }

    // Get form.
    var form = ccGetForm(formName);

    if (form == null) {
        // Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid formname parameter: " + formname);
	return null;
    }
    
    // Set disabled state.
    for (i = 0; i < form.elements.length; i++) {
	// Get element.
	var element = form.elements[i];

	if (element == null || element.name != elementName)
	    continue;

	var result = ccSetRadioButtonState(element,
	    ccGetElement(childNameDisabled, formName),
	    ccGetElement(childNameTitleDisabled, formName),
	    ccGetElement(childNameTitleEnabled, formName),
	    ccGetElement(childNameValueDisabled, formName),
	    disabled);

	if (!result)
	    return false;
    }

    return true;
}

// Set the state of CC radiobuttons.
function ccSetRadioButtonState(element, disabledHiddenField, 
	titleDisabledHiddenField, titleEnabledHiddenField,
	valueDisabledHiddenField, disabled) {
    // Class names.
    var classNameRadioButton 	     = "Rb";
    var classNameRadioButtonDisabled = "RbDis";		
	
    // Netscape 4.x does not support disabled state, just return.
    if (is_nav4) {
	return false;
    }

    // Validate element type.
    if (element == null || element.type.toLowerCase() != "radio") {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid element type.");
	return false;
    }

    // Validate element class name.
    if (is_gecko && (element.className == null
            || !(element.className == classNameRadioButton
	    ||   element.className == classNameRadioButtonDisabled))) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Invalid element class name.");
	return false;
    }

    // Validate disabled hidden field.
    if (disabledHiddenField == null) {
	// If dynamic attribute was not set, no hidden fields will be
	// output by the tag.
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("Disabled state cannot be set.");
	return false;
    }

    // Validate disabled param.
    if (disabled == null) {
	// Alerts must be configured to only show up for developers. For now
	// they are commented out. See bug 5039157.
	// alert("disabled parameter is null.");
	return false;
    }

    // Get boolean value to ensure correct data type.
    disabled = new Boolean(disabled).valueOf();

    // Set disabled state.
    disabledHiddenField.value = disabled;
    element.disabled = disabled;

    if (disabled) {
	// Set title attribute.
	if (titleDisabledHiddenField != null)
	    element.title = titleDisabledHiddenField.value;
	
	// Set class attribute.
	if (is_gecko)
	    element.className = classNameRadioButtonDisabled;

	// Set disabled value.
	if (valueDisabledHiddenField != null && element.checked)
	    valueDisabledHiddenField.value = element.value;
    } else {
	// Set title attribute.
	if (titleEnabledHiddenField != null)
	    element.title = titleEnabledHiddenField.value;

	// Set class attribute.
	if (is_gecko)
	    element.className = classNameRadioButton;
    }

    return true;
}

//-->
