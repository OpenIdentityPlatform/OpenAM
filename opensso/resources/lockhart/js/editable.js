function CCEditableList(jsQualifier, listbox, formName, separator,
    defaultOptionValue, defaultOptionLabel, bempty, bglobal) {

    var frm = document.forms[formName];
    this.listbox = listbox;
    this.addButton = jsQualifier + ".addButton";
    this.deleteButton = jsQualifier + ".deleteButton";
    this.textbox = frm.elements[jsQualifier + ".textField"];
    this.textbox2 = frm.elements[jsQualifier + ".valueTextField"];
    this.selectedOptions = this.listbox.options;
    this.formName = formName;
    this.selectedHiddenText = frm.elements[jsQualifier + ".selectedTextField"];
    this.separator = separator;
    this.defaultOptionValue = defaultOptionValue;
    this.defaultOptionLabel = defaultOptionLabel;
    this.allowempty = (bempty) && (bempty == '1');
    this.bglobalmap = (bglobal) && (bglobal == '1');

    // attach editableList object methods
    this.disableButton = editableListDisableButton;
    this.handleReload = editableListHandleReload;
    this.handleSelectedOnChange = editableListHandleSelectedOnChange;
    this.addToList = editableListAdd;
    this.removeFromList = editableListRemove;
    this.selectedLength = editableListGetSelectedLength;
    this.selectedSelection = editableListGetSelectedSelection;
    this.getOriginalIndex = ccGetOriginalIndex;
    this.updateHiddenField = updateHiddenField;
}


// This functions maps to editableList::handleReload
// 
// It will be called after the HTML for the orderable list tag has been
// rendered. Some browsers maintain (on the client side) the selection state of 
// list boxes during/after page refresh; this method will ensure that the button
// states are properly set after reload
function editableListHandleReload() {
    // check for selection in listbox
    if (this.listbox.selectedIndex != -1) {
        // test if there is even anything in the selected list
        if (this.selectedLength() < 2) {
            // nothing valid is in selected, disable buttons
            this.disableButton(this.deleteButton, true);
        }
    }
}

// It will be called when the onChange event for the selected list box is
// received. 
function editableListHandleSelectedOnChange() {
    // determine the new selected index of the selected list
    var index = this.listbox.selectedIndex;

    // don't allow the no select spacer item to remain selected
    if (this.selectedOptions[this.selectedLength() - 1].selected == true) {
        this.selectedOptions[this.selectedLength() - 1].selected = false;
    }

    if (this.listbox.selectedIndex != -1) {
	this.disableButton(this.deleteButton, false);
    } else {
	this.disableButton(this.deleteButton, true);
    }
}

function editableListAdd() {
    var str = this.textbox.value;
    str = str.replace(/\s+$/, '').replace(/^\s+/, '');
    var len = str.length;	

    if (this.allowempty || (len > 0)) {

        if (this.textbox2) {
            var str2 = this.textbox2.value;
            str2 = str2.replace(/\s+$/, '').replace(/^\s+/, '');
            var len2 = str2.length;
            if (((len2 == 0) && (len > 0)) || ((len == 0) && (len2 == 0))){
                alert(msgMapListInvalidEntry);
                return;
            }
            if ((str.indexOf('[') != -1) || (str.indexOf(']') != -1)) {
                alert(msgMapListInvalidKey);
                return;
            }
            if ((str2.indexOf('[') != -1) || (str2.indexOf(']') != -1)) {
                alert(msgMapListInvalidValue);
                return;
            }
            if ((len == 0) && (len2 > 0)) {
                if (this.bglobalmap) {
                    str = str2;
                } else {
                    alert(msgMapListInvalidNoKey);
                    return;
                }
            } else {
                str = '[' + str + ']=' + str2;
            }
        }

	var size = this.selectedOptions.length;
	var txt = this.selectedOptions[size-1].text;
	var val = this.selectedOptions[size-1].value;

	// If the size of new item is longer than the current size of 
	// the listbox, adjust the size of the spacer item to 80% of 
	// that size. This will prevent the listbox from shrinking.
	if (len > txt.length) {
	    var txtLen = Math.round((len * 80) / 100);
	    var label = "";	
	
	    for (var i = 0; i < txtLen; i++) {
		label += this.defaultOptionLabel; 
	    }
	    txt = label;
	}	

	this.selectedOptions[size] = new Option(txt, val);

	this.selectedOptions[size-1].text = str;
	this.selectedOptions[size-1].value = str;

	this.selectedOptions.selectedIndex = -1;
	this.selectedOptions.selectedIndex = size-1;

	this.updateHiddenField();
	this.disableButton(this.deleteButton, false);

        this.textbox.value = "";
        if (this.textbox2) {
            this.textbox2.value = "";
        }
    }
}

function editableListRemove() {
    var size = this.selectedOptions.length;

    for (var i = size-1; i >= 0; --i) {
	var opt = this.selectedOptions[i];

	if ((opt.selected) && (this.allowempty || (opt.value != ""))) {
	    this.selectedOptions[i] = null;
	}
    }

    this.updateHiddenField();
    this.disableButton(this.deleteButton, true);
}

function updateHiddenField() {
    var values = '';
    var size = this.selectedOptions.length;
    for (var i = 0; i < size-1; i++) {
	var opt = this.selectedOptions[i];

	if (this.allowempty || (opt.value != "")) {
	    if (values != '') {
		values += this.separator;
	    }
            var optVal = opt.value;
            if (optVal == '') {
                optVal = ' ';
            }
            var optText = opt.text;
            if (optText == '') {
                optText = ' ';
            }
	    values += optText + this.separator + optVal;
	}
    }
    
    this.selectedHiddenText.value = values;
}

// Convenience function to return the length of the given options list.
function editableListGetSelectedLength() {
    return (this.selectedOptions != null) ? this.selectedOptions.length : 0;
}

// Convienence function to return the selected index of the selected list
function editableListGetSelectedSelection() {
    return this.listbox.selectedIndex;
}

// This function sets the button with the given name to be either disabled (2nd
// param is true) or enabled (2nd param is false).
function editableListDisableButton(button, disabled) {
    if (ccGetElement(button, this.formName) == null || disabled == null) {
        // bad param(s)
        return;
    }

    // if we're in NS4 or button or disabled were null don't do anything
    if (is_nav4 != 1) {
        // some browser other than NS4 - call js included via ccdynamic.js
        ccSetButtonDisabled(button, this.formName, disabled);
    }
}

// Returns an int indicating the index of the given option in the original
// available list. Will return -1 if the given option is not found.
function ccGetOriginalIndex(option) {
    var originalIndex = -1;

    for (i = 0; i < this.originalOptions.length; i++) {
        if (option.text == this.originalOptions[i].text &&
                option.value == this.originalOptions[i].value) {
            originalIndex = i;
            break;
        }
    }

    // return the original index of the given option or -1 if not found
    return originalIndex;
}

