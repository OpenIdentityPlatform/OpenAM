// ident "@(#)orderablelist.js 1.9 04/01/19 SMI"
// 
// Copyright 2003 by Sun Microsystems, Inc. All rights reserved.
// Use is subject to license terms.
//
// This Javascript file should be included in any page that uses the
// CCOrderableListTag. It defines a javascript object for orderable list
// and its related data / methods.
// 
// This Javascript could be placed inline in the tag but it cleans up the
// HTML output immensely by including a separate file


// Define the ordrablelist object type and the related object functions
function CCOrderableList(jsQualifier, selectedList, formName, separator,
    defaultOptionValue) {

    // initialize all the pointers to orderable list data & objects
    this.selectedList = selectedList;
    this.moveUpButton = jsQualifier + ".MoveUpButton";
    this.moveDownButton = jsQualifier + ".MoveDownButton";
    this.moveTopButton = jsQualifier + ".MoveTopButton";
    this.moveBottomButton = jsQualifier + ".MoveBottomButton";
    this.selectedOptions = this.selectedList.options;
    this.formName = formName;
    this.selectedHiddenText = jsQualifier + ".SelectedTextField";
    this.separator = separator;
    this.defaultOptionValue = defaultOptionValue;

    // attach CCOrderableList object methods
    this.disableButton = ccOrderableListDisableButton;
    this.handleReload = ccOrderableListHandleReload;
    this.handleSelectedOnChange = ccOrderableListHandleSelectedOnChange;
    this.moveUp = ccOrderableListMoveUp;
    this.moveDown = ccOrderableListMoveDown;
    this.moveTop = ccOrderableListMoveTop;
    this.moveBottom = ccOrderableListMoveBottom;
    this.selectedLength = ccOrderableListGetSelectedLength;
    this.selectedSelection = ccOrderableListGetSelectedSelection;
    this.updateMove = ccOrderableListUpdateMove;
    this.updateMoveButtons = ccOrderableListUpdateMoveButtons;
    this.getOriginalIndex = ccGetOriginalIndex;

}


// This functions maps to CCOrderableList::handleReload
// 
// It will be called after the HTML for the orderable list tag has been
// rendered. Some browsers maintain (on the client side) the selection state of 
// list boxes during/after page refresh; this method will ensure that the button
// states are properly set after reload
function ccOrderableListHandleReload() {
    // check for selection in listbox
    if (this.selectedList.selectedIndex != -1) {
        // get the selected list index
        index = this.selectedList.selectedIndex;

        // test if there is even anything in the selected list
        if (this.selectedLength() < 2) {
            // nothing valid is in selected, disable buttons
            this.disableButton(this.moveUpButton, true);
            this.disableButton(this.moveDownButton, true);
        } else {
            // test if anything is selected in the selected list
            if (index != -1) {
                this.updateMoveButtons(index);
            }
        }
    }
}

// This OO function maps to CCOrderableList::handleSelectedOnChange()
//
// It will be called when the onChange event for the selected list box is
// received. 
function ccOrderableListHandleSelectedOnChange() {
    // determine the new selected index of the selected list
    var index = this.selectedList.selectedIndex;

    // don't allow the no select spacer item to remain selected
    if (this.selectedOptions[this.selectedLength() - 1].selected == true) {
        this.selectedOptions[this.selectedLength() - 1].selected = false;
        // disable the move button if necessary
        if (this.selectedList.selectedIndex == -1) {
            // disable move buttons
            this.disableButton(this.moveUpButton, true);
            this.disableButton(this.moveDownButton, true);
        }

        // we're done
        return;
    }

    // update the states of the move up/down buttons
    this.updateMoveButtons(index);
}

// This OO function maps to CCOrderableList::moveUp()
//
// If possible, move up any selected items in the selected list 
function ccOrderableListMoveUp() {

    // get the first selected index in the selected list
    var index = this.selectedList.selectedIndex;
    
    // index to begin checking items at
    var begin = -1;

    // if nothing valid is selected, display an error msg
    if (index == -1 || this.selectedOptions[index].value ==
            this.defaultOptionValue) {
        return;
    } else {
        // otherwise loop thru all the items in the selected list
        for (i = 0; i < this.selectedLength(); i++) {
            // test if a valid selection
            if (this.selectedOptions[i].selected &&
                    this.selectedOptions[i].value !=
                    this.defaultOptionValue) {
                // this item is selected - move it up if possible
                if (i - 1 > begin) {
                    iotext = this.selectedOptions[i-1].text;
                    iovalue = this.selectedOptions[i-1].value;
                    this.selectedOptions[i - 1].text =
                        this.selectedOptions[i].text;
                    this.selectedOptions[i-1].value =
                        this.selectedOptions[i].value;
                    this.selectedOptions[i-1].selected = true;
                    this.selectedOptions[i].text = iotext;
                    this.selectedOptions[i].value = iovalue;
                    this.selectedOptions[i].selected = false;
                    begin = i - 1; 
                } else {
                    begin = i;
                }
            }
        }
    }
    
    // update the state of the move up and down buttons
    this.updateMoveButtons(index - 1);
    
    // sync up the lists
    this.updateMove();
}

// This OO function maps to CCOrderableList::moveDown()
//
// This function will be called when the move down button is pressed
function ccOrderableListMoveDown() {
    // get the selected index of the selected list
    var index = this.selectedList.selectedIndex;

    var end = this.selectedLength() - 1;

    if (index < 0 || index > end - 1 ||
            this.selectedOptions[index].value == this.defaultOptionValue) {
        return;
    } else {
        for (i = end; i >= 0; i--) {
            if (this.selectedOptions[i].selected &&
                    this.selectedOptions[i].value !=
                    this.defaultOptionValue) {
                if (i + 1 < end) {
                    iotext = this.selectedOptions[i].text;
                    iovalue = this.selectedOptions[i].value;
                    this.selectedOptions[i].text =
                        this.selectedOptions[i+1].text;
                    this.selectedOptions[i].value =
                        this.selectedOptions[i+1].value;
                    this.selectedOptions[i].selected = false;
                    this.selectedOptions[i+1].text = iotext;
                    this.selectedOptions[i+1].value = iovalue;
                    this.selectedOptions[i+1].selected = true;
                    
                    end = i + 1;
                } else {
                    end = i;
                }
            }
        }
    }
    
    // we just moved down so we must be able to move up now
    this.disableButton(this.moveUpButton, false);

    // check for / handle multiple selections
    this.updateMoveButtons(index + 1);

    // sync up lists
    this.updateMove();
}

// This OO function maps to CCOrderableList::moveTop()
//
// If possible, move selected items to the top of the list 
function ccOrderableListMoveTop() {
    // get the first selected index in the selected list
    var index = this.selectedList.selectedIndex;
    
    // index to begin checking items at
    var begin = -1;

    // index to top if multiple items are selected
    // they must remain in order
    var top = 0;
    // if nothing valid is selected, display an error msg
    if (index == -1 || this.selectedOptions[index].value ==
            this.defaultOptionValue) {
        return;
    } else {
        // otherwise loop thru all the items in the selected list
        for (i = 0; i < this.selectedLength(); i++) {
            // test if a valid selection
            if (this.selectedOptions[i].selected &&
                    this.selectedOptions[i].value !=
                    this.defaultOptionValue) {
                // this item is selected - move it up if possible
                if (i - 1 > begin) {
		    // save the value in the top slot
                    iotext = this.selectedOptions[top].text;
                    iovalue = this.selectedOptions[top].value;
                
		    // move the selected value to the top slot,
		    // mark it selected, deselect the slot it
		    // came from from
		    this.selectedOptions[top].text =
			 this.selectedOptions[i].text;
                    this.selectedOptions[top].value =
                        this.selectedOptions[i].value;
                    this.selectedOptions[top].selected = true;
                    this.selectedOptions[i].selected = false;

		    // push down values between top and
		    // the selected value
		    for (j = i; j > (top + 1) ; j--) {
			// move the item
			this.selectedOptions[j].text =
				this.selectedOptions[j - 1].text;
			this.selectedOptions[j].value =
				this.selectedOptions[j - 1].value;

			// if the item being moved is selected
			// it must remain selected
			if (this.selectedOptions[j - 1].selected) {
			    this.selectedOptions[j].selected = true;
			    this.selectedOptions[j - 1].selected = false;
			}
		    }

		    // restore the value that used to
		    // be in the top slot to the list
		    this.selectedOptions[top + 1].text = iotext;
		    this.selectedOptions[top + 1].value = iovalue;

		    // increment the new top so selected 
		    // items will remain in order when moved.
		    top = top + 1;  
			
                    begin = i - 1; 
                } else {
                    begin = i;
		    top++;
                }
            }
        }
    }
    
    // update the state of the move up and down buttons
    this.updateMoveButtons(0);
    
    // sync up the lists
    this.updateMove();
}
 // This OO function maps to CCOrderableList::moveBottom()
//
// If possible, move selected items to the top of the list 
function ccOrderableListMoveBottom() {

    // get the first selected index in the selected list
    var index = this.selectedList.selectedIndex;
    
    // index to begin checking items at
    var end = this.selectedLength() - 1;

    // index to bottom if multiple items are selected
    // they must remain in order
    var bot = this.selectedLength() - 2;
    // if nothing valid is selected, display an error msg

    if (index < 0 || index > end - 1 ||
            this.selectedOptions[index].value == this.defaultOptionValue) {
        return;
    } else {
        // otherwise loop thru all the items in the selected list
        for (i = end; i >= 0; i--) {
            // test if a valid selection
            if (this.selectedOptions[i].selected &&
                    this.selectedOptions[i].value !=
                    this.defaultOptionValue) {
                // this item is selected - move it down if possible
                if (i + 1 < end) {
		    // save the value in the top slot
                    iotext = this.selectedOptions[bot].text;
                    iovalue = this.selectedOptions[bot].value;
                
		    // move the selected value to the top slot
		    // and leave it selected
		    this.selectedOptions[bot].text =
                        this.selectedOptions[i].text;
                    this.selectedOptions[bot].value =
                        this.selectedOptions[i].value;
                    this.selectedOptions[bot].selected = true;
                    this.selectedOptions[i].selected = false;

		    // push down values between bot and
		    // the selected value
		    for (j = i; j < (bot - 1) ; j++) {
			// move the item
			this.selectedOptions[j].text =
				this.selectedOptions[j + 1].text;
			this.selectedOptions[j].value =
				this.selectedOptions[j + 1].value;

			// if the item being moved is selected
			// it must remain selected
			if (this.selectedOptions[j + 1].selected) {
			    this.selectedOptions[j].selected = true;
			    this.selectedOptions[j + 1].selected = false;
			}
		    }

		    // restore the value that used to
		    // be in the top slot to the list
		    this.selectedOptions[bot - 1].text = iotext;
		    this.selectedOptions[bot - 1].value = iovalue;

		    // increment the new top so selected 
		    // items will remain in order when moved.
		    bot = bot - 1;  
			
                    end = i + 1;
                } else {
                    end = i;
		    bot--;
                }
            }
        }
    }
    
    // update the state of the move up and down buttons
    this.updateMoveButtons(this.selectedLength() - 2);
    
    // sync up the lists
    this.updateMove();
}

// Convenience function to return the length of the given options list.
function ccOrderableListGetSelectedLength() {
    // check for null param
    if (this.selectedOptions != null) {
        // not null, return the length
        return this.selectedOptions.length;
    } else {
        // null param, just return 0
        return 0;
    }
}

// This OO function maps to CCOrderableList::selectedSelection()
//
// Convienence functio to return the selected index of the selected list
function ccOrderableListGetSelectedSelection() {
    return this.selectedList.selectedIndex;
}

// This OO function maps to CCOrderableList::disableButton()
//
// This function sets the button with the given name to be either disabled (2nd
// param is true) or enabled (2nd param is false).
function ccOrderableListDisableButton(button, disabled) {
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

// This OO function maps to CCOrderableList::updateMove()
//
// This function will be called whenever items are moved from one list to the
// other. It maintains the state of the list selections in the hidden text
// fields
function ccOrderableListUpdateMove() {

    var concatStr = null;
    var maxSelectedSelection = -1;
    
    for (i = 0; i < this.selectedLength() - 1; i++) {
        var optText = this.selectedOptions[i].text;
        var optVal = this.selectedOptions[i].value;
        if (optText == '') {
            optText = ' ';
        }
        if (optVal == '') {
            optVal = ' ';
        }

        if (concatStr == null) {
            concatStr = optText + this.separator + optVal;
        } else {
            concatStr += this.separator + optText + this.separator + optVal;
        }
        if (this.selectedOptions[i].selected) {
            maxSelectedSelection = i;
        }
    }

    if (is_nav4) {
        this.selectedList.selectedIndex = maxSelectedSelection;
    }

    // get a pointer to the selected hidden text field
    hiddenText = ccGetElement(this.selectedHiddenText, this.formName)

    // store the selected options in it
    hiddenText.value = concatStr;
}

// This OO function maps to CCOrderableList::updateMoveButtons()
//
// This function will be called whenever items are moved from one list to the
// other. It maintains the state of the move up and down buttons.
function ccOrderableListUpdateMoveButtons(index) {

    if (ccGetElement(this.moveUpButton, this.formName) == null ||
            ccGetElement(this.moveDownButton, this.formName) == null) {
        // the move buttons are not present, nothing to do
        return;
    }

    var hasMultipleSelections = false;
    var lastSelection = index;
    var selectionsContiguous = true;
    
    for (i = index + 1; i < this.selectedLength() - 1; i++) {
        if (this.selectedOptions[i].selected == true) {
            // we found another selection in the selected list
            hasMultipleSelections = true;
            
            // test if there was a non-selected item in between
            if (i != lastSelection + 1) {
                // something in between was UNselected
                selectionsContiguous = false;
            }
            
            // update the value of the last selected index we found
            lastSelection = i;
        }
    }

    // test if all the selected items are adjacent in the list
    if (hasMultipleSelections == true) {
        if (selectionsContiguous == true) {
            // all selected items are adjacent... see if first item is selected
            if (index < 1) {
                // disable the move up button since the first item is selected
                this.disableButton(this.moveUpButton, true);
                this.disableButton(this.moveTopButton, true);
            } else {
                // still room to move up, keep move up enabled
                this.disableButton(this.moveUpButton, false);
                this.disableButton(this.moveTopButton, false);
            }
            
            // see if the last valid item is selected
            if (lastSelection < this.selectedLength() - 2) {
                // room to move down, enable the move down button
                this.disableButton(this.moveDownButton, false);
                this.disableButton(this.moveBottomButton, false);
            } else {
                // disable move down
                this.disableButton(this.moveDownButton, true);
                this.disableButton(this.moveBottomButton, true);
            }
        } else {
            // selections are NOT contiguous - enable both up and down
            this.disableButton(this.moveUpButton, false);
            this.disableButton(this.moveDownButton, false);
            this.disableButton(this.moveTopButton, false);
            this.disableButton(this.moveBottomButton, false);
        }
    } else {
        // only one selected item is selected - first set move up button state
        if (index > 0) {
            // enable the move up button
            this.disableButton(this.moveUpButton, false);
            this.disableButton(this.moveTopButton, false);
        } else {
            // disable move up button
            this.disableButton(this.moveUpButton, true);
            this.disableButton(this.moveTopButton, true);
        }

        // now set the move down button state
        if (index < this.selectedLength() - 2) {
            // enable move down button
            this.disableButton(this.moveDownButton, false);
            this.disableButton(this.moveBottomButton, false);
        } else {
            // disable move down button
            this.disableButton(this.moveDownButton, true);
            this.disableButton(this.moveBottomButton, true);
        }

    }
}

// This OO function maps to CCOrderableList::getOriginalIndex()
//
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

/****************************************************************************/
/* Helper functions used by the CCOrderableList javascript class.           */
/* Note that the following following are NOT class methods                  */
/****************************************************************************/

// Helper function to determine if the given list box has multiple selections
function ccOrderableList_hasMultipleSelections(list) {
    // boolean indicating if we found multiple selections
    var hasMultipleSelections = false;

    // get the selected index - note this only indicates the first selection
    // there can still be others later in the list
    var index = list.selectedIndex;

    // test if something valid is selected
    if (index < 0 || index > list.length - 2) {
        return false;
    } else {
        // test each item in the list that follows index
        for (i = index + 1; i < list.length -1; i++) {
            if (list.options[i].selected) {
                // set foundAnother to true & break
                hasMultipleSelections = true;
                break;
            }
        }
    }

    // return the boolean flag inidicating multi selections
    return hasMultipleSelections;
}
