// ident "@(#)addremove.js 1.13 04/02/19 SMI"
// 
// Copyright 2003-2004 by Sun Microsystems, Inc. All rights reserved.
// Use is subject to license terms.
//
// This Javascript file should be included in any page that uses the
// CCAddRemoveTag. It defines a javascript object for the add/remove and its
// related data / methods.
// 
// This Javascript could be placed inline in the tag but it cleans up the
// HTML output immensely by including a separate file


// Define the add/remove object type and the related object functions
function CCAddRemove(jsQualifier, availableList, selectedList, formName,
        addErrorMsg, addAllErrorMsg, removeErrorMsg, removeAllErrorMsg,
        moveErrorMsg, separator, defaultOptionValue, allowAvailableMove) {
    // initialize all the pointers to add/remove data & objects
    this.availableList = availableList;
    this.selectedList = selectedList;
    this.addButton = jsQualifier + ".AddButton";
    this.addAllButton = jsQualifier + ".AddAllButton";
    this.removeButton = jsQualifier + ".RemoveButton";
    this.removeAllButton = jsQualifier + ".RemoveAllButton";
    this.moveUpButton = jsQualifier + ".MoveUpButton";
    this.moveDownButton = jsQualifier + ".MoveDownButton";
    this.availableOptions = this.availableList.options;
    this.selectedOptions = this.selectedList.options;
    this.formName = formName;
    this.availableHiddenText = jsQualifier + ".AvailableTextField";
    this.selectedHiddenText = jsQualifier + ".SelectedTextField";
    this.addErrorMsg = addErrorMsg;
    this.addAllErrorMsg = addAllErrorMsg;
    this.removeErrorMsg = removeErrorMsg;
    this.removeAllErrorMsg = removeAllErrorMsg;
    this.moveErrorMsg = moveErrorMsg;
    this.separator = separator;
    this.defaultOptionValue = defaultOptionValue;
    this.allowAvailableMove = allowAvailableMove;
    this.sortOnAdd = false;

    // attach CCAddRemove object methods
    this.add = ccAddRemoveAdd;
    this.addAll = ccAddRemoveAddAll;
    this.availableLength = ccAddRemoveGetAvailableLength;
    this.availableSelection = ccAddRemoveGetAvailableSelection;
    this.disableButton = ccAddRemoveDisableButton;
    this.handleAvailableDblClick = ccAddRemoveHandleAvailableDblClick;
    this.handleAvailableOnChange = ccAddRemoveHandleAvailableOnChange;
    this.handleReload = ccAddRemoveHandleReload;
    this.handleSelectedDblClick = ccAddRemoveHandleSelectedDblClick;
    this.handleSelectedOnChange = ccAddRemoveHandleSelectedOnChange;
    this.moveUp = ccAddRemoveMoveUp;
    this.moveDown = ccAddRemoveMoveDown;
    this.remove = ccAddRemoveRemove;
    this.removeAll = ccAddRemoveRemoveAll;
    this.selectedLength = ccAddRemoveGetSelectedLength;
    this.selectedSelection = ccAddRemoveGetSelectedSelection;
    this.updateMove = ccAddRemoveUpdateMove;
    this.updateMoveButtons = ccAddRemoveUpdateMoveButtons;
    this.afterRemove = ccAddRemoveAfterRemove;
    this.afterAdd = ccAddRemoveAfterAdd;
    this.getOriginalIndex = ccGetOriginalIndex;

    // de-selected items need to placed back in th available list in the
    // original order
    this.originalOptions = new Array(this.availableLength());

    for (i = 0 ; i < this.availableLength(); i++) {
        this.originalOptions[i] = new Option(
            this.availableOptions[i].text, this.availableOptions[i].value);
    }

    if (this.allowAvailableMove && this.availableLength() > 1) {
        // availabe items can be moved - initially enable the move down button
        this.disableButton(this.moveDownButton, false);
    }
}

// This OO function maps to CCAddRemove::add()
//
// It will be called when an item is to be added from the avaiable list to the
// selected list (such as when the add button is clicked).
function ccAddRemoveAdd() {
    // see if anything is selected in the available list
    if (this.availableSelection() == -1) {
        // no, show an error
        alert(this.addErrorMsg);
        return;
    } else {
        // move all the selected items in the available list
        this.selectedOptions[this.selectedLength() - 1] = null;
        
        // deselect everything in the selected list
        this.selectedList.selectedIndex = -1;
        
        // keep moving selected items until there aren't any more valid ones
        while (this.availableSelection() != -1 &&
               this.availableOptions[this.availableSelection()].value !=
               this.defaultOptionValue &&
               this.availableSelection() < this.availableLength() - 1) {
            // get the next selected option to move
            var curSelection =
                this.availableOptions[this.availableSelection()];

            if (this.sortOnAdd == true) {
                // get the original index of the option to move
                var oIndex = this.getOriginalIndex(curSelection);
                
                // now determine where we to insert the deselected option
                var insertAfter = this.selectedLength() - 1;
                
                // loop backwards thru the current selected options
                for (insertAfter; insertAfter > -1; insertAfter--) {
                    // get the current option in the available list
                    var option = this.selectedOptions[insertAfter];
                    
                    // test if the current option needs to be moved down
                    if (oIndex < this.getOriginalIndex(option)) {
                        // move this option down
                        this.selectedOptions[insertAfter + 1] =
                            new Option(option.text, option.value);
                        if (option.selected) {
                            this.selectedOptions[insertAfter + 1].selected =
                                true;
                        }
                    } else {
                        // current option goes just before the deselected one
                        break;
                    }
                }
            } else {
                var insertAfter = this.selectedLength() - 1;
            }

            // insert the selected option one past the index we found
            this.selectedOptions[++insertAfter] =
                new Option(curSelection.text, curSelection.value);

            // items added to the selected list should be selected
            this.selectedOptions[insertAfter].selected = true;

            // remove the option we just moved from the available list
            this.availableOptions[this.availableSelection()] = null;
        }
        
        // since we removed it before, we need to append the spacer option also
        this.selectedOptions[this.selectedLength()] =
            new Option(this.availableOptions[this.availableLength() - 1].text,
                this.availableOptions[this.availableLength() - 1].value);
        
        // selection is now in the selected list - disable the add button
        this.disableButton(this.addButton, true);
        
        // check if the add all button is present
        if (ccGetElement(this.addAllButton, this.formName) != null) {
            // it is, see if we need to disable it
            if (this.availableLength() < 2) {
                // no valid items for selection - disable add all
                this.disableButton(this.addAllButton, true);
            }
        }
        
        // perform default processing after we've added something
        this.afterAdd();

        // sync the state of the lists
        this.updateMove();
    }
}

// This OO function maps to CCAddRemove::addAll()
//
// It will be called when all the available items are to be moved to the
// selected list (when the add all button is clicked).
function ccAddRemoveAddAll() {
    // test if there is anything to move
    if (this.availableLength() < 2) {
        // there isn't, display the appropriate error msg
        alert(this.addAllErrorMsg);
        return;
    } else {
        // deselect all the selected options
        this.selectedList.selectedIndex = -1;

        // add each item in the available list
        for (i = 0; i < this.availableOptions.length - 1; i++) {
            this.availableOptions[i].selected = true;
        }

        this.add();
        
        // disable the add all button
        if (ccGetElement(this.addAllButton, this.formName) != null) {
            this.disableButton(this.addAllButton, true);
        }
        
        // perform the standard after add processing
        this.afterAdd();
        
        // sync the state of the lists
        this.updateMove();
    }
}

// This OO function maps to CCAddRemove::remove()
//
// It will be called when the remove buttons is pressed or a selected list item
// is double clicked
function ccAddRemoveRemove() {
    // test if anything to remove is selected
    if (this.selectedList.selectedIndex == -1) {
        // nothing selected, show an error
        alert(this.removeErrorMsg);
        return;
    } else {
        // move all the selected items to the available list
        while (this.selectedList.selectedIndex != -1 && 
               this.selectedOptions[this.selectedList.selectedIndex].value !=
               this.defaultOptionValue) {

            // pointer to the current selection to remove
            var curSelection =
                this.selectedOptions[this.selectedList.selectedIndex];

            // the original index of the de-selected option
            var oIndex = this.getOriginalIndex(curSelection);

            // move the spacer item down
            var spacerOption = 
                this.availableOptions[this.availableLength() - 1];

            this.availableOptions[this.availableLength()] =
                new Option(spacerOption.text, spacerOption.value);
            
            // now determine where we to insert the deselected option
            var insertAfter = this.availableLength() - 2;

            // loop backwards thru the current available options
            for (insertAfter; insertAfter > -1; insertAfter--) {
                // get the current option in the available list
                var option = this.availableOptions[insertAfter];

                // test if the current option needs to be moved down
                if (oIndex < this.getOriginalIndex(option)) {
                    // move this option down
                    this.availableOptions[insertAfter + 1] =
                        new Option(option.text, option.value);
                    if (option.selected) {
                        this.availableOptions[insertAfter + 1].selected = true;
                    }
                } else {
                    // the current option goes just before the deselected one
                    break;
                }
            }

            // insert the deselected option one past the index we found
            this.availableOptions[++insertAfter] =
                new Option(curSelection.text, curSelection.value);
            
            // set the newly created item as selected
            this.availableOptions[insertAfter].selected = true;
            
            // remove the item we just moved from the selected list
            this.selectedOptions[this.selectedList.selectedIndex] = null;
        }
        
        // selection is now in the available list - enable the add button
        this.disableButton(this.addButton, false);

        // test if add all button is visible. also note that if the remove all
        // btn is displayed, HCI 2.0 dictates that add all MUST be displayed
        if (ccGetElement(this.addAllButton, this.formName) != null) {
            this.disableButton(this.addAllButton, false);
        }
        
        // test if the remove all btn is visible.
        if (ccGetElement(this.removeAllButton, this.formName) != null) {
            // it is, disable remove all btn if selected length == 0
            if (this.selectedLength() < 2) {
                this.disableButton(this.removeAllButton, true);
            }
        }
        
        // clear the selected selection, disable move buttons
        this.afterRemove();

        // sync the state of the lists
        this.updateMove();
    }
}

// This OO function maps to CCAddRemove::removeAll
//
// It will be called when the remove all button is pressed
function ccAddRemoveRemoveAll() {
    // check if there are any items to remove first
    if (this.selectedLength() < 2) {
        // there are NOT, show an error
        alert(this.removeAllErrorMsg);
        return;
    } else {
        // deselect all the items in the available list
        this.availableList.selectedIndex = -1;

        // remove each item in the selected list
        for (i = 0; i < this.selectedOptions.length - 1; i++) {
            this.selectedOptions[i].selected = true;
        }

        this.remove();
    }

    // selected list is now empty - disable all buttons except add btns
    this.disableButton(this.addButton, false);

	// test if add all button is visible. also note that if the remove all
    if (ccGetElement(this.addAllButton, this.formName) != null) {
        this.disableButton(this.addAllButton, false);
    }

    // test if the remove all button is visible
    if (ccGetElement(this.removeAllButton, this.formName) != null) {
        // it is, disable it
        this.disableButton(this.removeAllButton, true);
    }

    // in solaris netscape 4, users must must deselect items one at a time
    // since this is less than desirable, we will only highlight the last item
    // in netscape 4
    if (is_nav4) {
        this.availableList.selectedIndex = this.availableLength() - 2;
    }
	
	// clear the selected selection, disable move buttons
    this.afterRemove();

    // sync up the state of the lists
	this.updateMove();
}

// This OO function maps to CCAddRemove::handleAvailableDblClick()
//
// This method will be called when an item in the available list box is double
// clicked
function ccAddRemoveHandleAvailableDblClick() {
    // determine what was double clicked
    var index = this.availableList.selectedIndex; 

    // if the new selection in the available list is moveable, call add()
    if (index > -1 && index < this.availableLength() - 1) {
        this.add();
    } else {
        // make sure the separator isn't highlighted
        this.availableOptions[this.availableLength() - 1].selected = false;
    }
}

// This OO function maps to CCAddRemove::handleAvailableOnChange()
//
// It will be called when the onChange event for the available list box is
// received. It will maintain the enable / disable state of the add button.
// Note that the Add All button state is maintained in the view class
function ccAddRemoveHandleAvailableOnChange() {
    // don't allow the no select spacer item to remain selected
    if (this.availableOptions[this.availableLength() - 1].selected == true) {
        this.availableOptions[this.availableLength() - 1].selected = false;
        if (this.availableList.selectedIndex == -1) {
            // disable add button
            this.disableButton(this.addButton, true);
        }
        return;
    }

    // determine the new selected index of the available list
    var index = this.availableList.selectedIndex;

    // test if a selectable item is now selected
    if (index != -1 && index < this.availableLength() - 1) {
        // at least one is, enable the add button
        this.disableButton(this.addButton, false);
    } else {
        // disable the button
        this.disableButton(this.addButton, true);
    }

    // HCI dictates that the selection only be in 1 list at a time
    this.selectedList.selectedIndex = -1;
    
    // we just cleared any selection in selected list so disable remove & move
    this.disableButton(this.removeButton, true);

    if (this.allowAvailableMove) {
        // available items can move, update the state of the move buttons
        this.updateMoveButtons(index);
    } else {
        // disable the move buttons
        this.disableButton(this.moveUpButton, true);
        this.disableButton(this.moveDownButton, true);
    }
}

// This functions maps to CCAddRemove::handleReload
// 
// It will be called after the HTML for the add/remove tag has been rendered.
// Some browsers maintain (on the client side) the selection state of list 
// boxes during/after page refresh; this method will ensure that the button
// states are properly set after reload
function ccAddRemoveHandleReload() {
    // check for selection in either list box
    if (this.availableList.selectedIndex == -1 &&
            this.selectedList.selectedIndex == -1) {
        // if nothing else is selected, select the first item in the available
        this.availableList.selectedIndex = 0;
        
        // now enable the add button just in case it's not
        this.disableButton(this.addButton, false);
    } else {
        // get the available list selection
        var index = this.availableList.selectedIndex;

        // see if anything is even in the available list
        if (this.availableLength() < 2) {
            // no valid items available - disable add buttons
            this.disableButton(this.addButton, true);
            this.disableButton(this.addAllButton, true);
        } else {
            // test if something valid is selected
            if (index > -1 && index < this.availableLength() - 2) {
                this.disableButton(this.addButton, false);
            } else {
                this.disableButton(this.removeButton, false);
            }
        }

        // get the selected list index
        index = this.selectedList.selectedIndex;

        // test if there is even anything in the selected list
        if (this.selectedLength() < 2) {
            // nothing valid is in selected, disable buttons
            this.disableButton(this.removeButton, true);
            this.disableButton(this.removeAllButton, true);
            this.disableButton(this.moveUpButton, true);
            this.disableButton(this.moveDownButton, true);
        } else {
            // test if anything is selected in the selected list
            if (index != -1) {
                // if something is selected, enable remove btn
                this.disableButton(this.removeButton, false);
                
                this.updateMoveButtons(index);
            }
        }
    }
}

// This OO function maps to CCAddRemove::handleSelectedDblClick()
//
// This method will be called when an item in the selected list is double
// clicked
function ccAddRemoveHandleSelectedDblClick() {
    // determine what was double clicked
    var index = this.selectedList.selectedIndex; 

    // if the new selection in the available list is moveable, call remove()
    if (index > -1 && index < this.selectedLength() - 1) {
        this.remove();
    } else {
        // make sure the separator isn't highlighted
        this.selectedOptions[this.selectedLength() - 1].selected = false;
    }
}

// This OO function maps to CCAddRemove::handleSelectedOnChange()
//
// It will be called when the onChange event for the selected list box is
// received. It will maintain the enable / disable state of the remove button.
// Note that the Remove All button state is maintained in the view class
function ccAddRemoveHandleSelectedOnChange() {
    // determine the new selected index of the selected list
    var index = this.selectedList.selectedIndex;

    // don't allow the no select spacer item to remain selected
    if (this.selectedOptions[this.selectedLength() - 1].selected == true) {
        this.selectedOptions[this.selectedLength() - 1].selected = false;
        // disable the remove button if necessary
        if (this.selectedList.selectedIndex == -1) {
            // disable remove and move buttons
            this.disableButton(this.removeButton, true);
            this.disableButton(this.moveUpButton, true);
            this.disableButton(this.moveDownButton, true);
        }

        // we're done
        return;
    }

    // test if a removeable item is now selected
    if (index != -1 && index < this.selectedLength() - 1) {
        // at least one is, enable remove btn
        this.disableButton(this.removeButton, false);
    } else {
        // nothing valid is selected - disable the remove and move buttons
        this.disableButton(this.removeButton, true);

        // are the move up and down buttons present?
        if (this.moveUpButton != null && this.moveDownButton != null) {
            // yes, disable them
            this.disableButton(this.moveUpButton, true);
            this.disableButton(this.moveDownButton, true);
        }
    }

    // HCI dictates that the selection only be in 1 list at a time
    this.availableList.selectedIndex = -1;

    // we just cleared any selection in available list so disable add
    this.disableButton(this.addButton, true);

    // update the states of the move up and down buttons
    this.updateMoveButtons(index);
}

// This OO function maps to CCAddRemove::moveUp()
//
// If possible, move up any selected items in the selected list 
function ccAddRemoveMoveUp() {
    var targetList;
    
    // determine which list contains item(s) to move down
    if (this.availableList.selectedIndex != -1) {
        targetList = this.availableList;
    } else {
        targetList = this.selectedList;
    }

    var targetOptions = targetList.options;

    // get the first selected index in the selected list
    var index = targetList.selectedIndex;
    
    // index to begin checking items at
    var begin = -1;

    // if nothing valid is selected, display an error msg
    if (index == -1 || targetOptions[index].value ==
            this.defaultOptionValue) {
        alert(this.moveErrorMsg)
        return;
    } else {
        // otherwise loop thru all the items in the selected list
        for (i = 0; i < targetOptions.length; i++) {
            // test if a valid selection
            if (targetOptions[i].selected && targetOptions[i].value !=
                    this.defaultOptionValue) {
                // this item is selected - move it up if possible
                if (i - 1 > begin) {
                    iotext = targetOptions[i-1].text;
                    iovalue = targetOptions[i-1].value;
                    targetOptions[i - 1].text = targetOptions[i].text;
                    targetOptions[i-1].value = targetOptions[i].value;
                    targetOptions[i-1].selected = true;
                    targetOptions[i].text = iotext;
                    targetOptions[i].value = iovalue;
                    targetOptions[i].selected = false;
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

// This OO function maps to CCAddRemove::moveDown()
//
// This function will be called when the move down button is pressed
function ccAddRemoveMoveDown() {
    var targetList;
    
    // determine which list contains item(s) to move down
    if (this.availableList.selectedIndex != -1) {
        targetList = this.availableList;
    } else {
        targetList = this.selectedList;
    }

    var targetOptions = targetList.options;

    // get the selected index of the selected list
    var index = targetList.selectedIndex;

    var end = targetOptions.length - 1;

    if (index < 0 || index > end - 1 ||
            targetOptions[index].value == this.defaultOptionValue) {
        alert(this.moveErrorMsg)
        return;
    } else {
        for (i = end; i >= 0; i--) {
            if (targetOptions[i].selected &&
                    targetOptions[i].value != this.defaultOptionValue) {
                if (i + 1 < end) {
                    iotext = targetOptions[i].text;
                    iovalue = targetOptions[i].value;
                    targetOptions[i].text = targetOptions[i+1].text;
                    targetOptions[i].value = targetOptions[i+1].value;
                    targetOptions[i].selected = false;
                    targetOptions[i+1].text = iotext;
                    targetOptions[i+1].value = iovalue;
                    targetOptions[i+1].selected = true;
                    
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

// Convenience function to return the length of the given options list.
function ccAddRemoveGetAvailableLength() {
    // check for null param
    if (this.availableOptions != null) {
        // not null, return the length
        return this.availableOptions.length;
    } else {
        // null param, just return 0
        return 0;
    }
}

// Convenience function to return the length of the given options list.
function ccAddRemoveGetSelectedLength() {
    // check for null param
    if (this.selectedOptions != null) {
        // not null, return the length
        return this.selectedOptions.length;
    } else {
        // null param, just return 0
        return 0;
    }
}

// This OO function maps to CCAddRemove::availableSelection()
//
// Convienence functio to return the selected index of the available list
function ccAddRemoveGetAvailableSelection() {
    return this.availableList.selectedIndex;
}

// This OO function maps to CCAddRemove::selectedSelection()
//
// Convienence functio to return the selected index of the available list
function ccAddRemoveGetSelectedSelection() {
    return this.selectedList.selectedIndex;
}

// This OO function maps to CCAddRemove::disableButton()
//
// This function sets the button with the given name to be either disabled (2nd
// param is true) or enabled (2nd param is false).
function ccAddRemoveDisableButton(button, disabled) {
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

// This OO function maps to CCAddRemove::updateMove()
//
// This function will be called whenever items are moved from one list to the
// other. It maintains the state of the list selections in the hidden text
// fields
function ccAddRemoveUpdateMove() {

    var concatStr = null;
    var maxAvailableSelection = -1;
    var maxSelectedSelection = -1;
    
    for (i = 0; i < this.availableLength() - 1; i++) {
        if (concatStr == null) {
            concatStr = this.availableOptions[i].text + this.separator +
                this.availableOptions[i].value;
        } else {
            concatStr += this.separator + this.availableOptions[i].text +
                this.separator + this.availableOptions[i].value;
        }
        if (this.availableOptions[i].selected) {
            maxAvailableSelection = i;
        }
    }
    
    // get a pointer to the available hidden text field
    var hiddenText = ccGetElement(this.availableHiddenText, this.formName);

    // store the available options in it
    hiddenText.value = concatStr;
    
    concatStr = null;
    for (i = 0; i < this.selectedLength() - 1; i++) {
        if (concatStr == null) {
            concatStr = this.selectedOptions[i].text + this.separator + 
                this.selectedOptions[i].value;
        } else {
            concatStr += this.separator + this.selectedOptions[i].text +
                this.separator + this.selectedOptions[i].value;
        }
        if (this.selectedOptions[i].selected) {
            maxSelectedSelection = i;
        }
    }

    if (is_nav4) {
        this.availableList.selectedIndex = maxAvailableSelection;
        this.selectedList.selectedIndex = maxSelectedSelection;
    }

    // get a pointer to the selected hidden text field
    hiddenText = ccGetElement(this.selectedHiddenText, this.formName)

    // store the selected options in it
    hiddenText.value = concatStr;
}

// This OO function maps to CCAddRemove::updateMoveButtons()
//
// This function will be called whenever items are moved from one list to the
// other. It maintains the state of the move up and down buttons.
function ccAddRemoveUpdateMoveButtons(index) {
    if (ccGetElement(this.moveUpButton, this.formName) == null ||
            ccGetElement(this.moveDownButton, this.formName) == null) {
        // the move buttons are not present, nothing to do
        return;
    }

    var hasMultipleSelections = false;
    var lastSelection = index;
    var selectionsContiguous = true;

    var targetList;
    
    // determine which list contains the moved item(s)
    if (this.availableList.selectedIndex != -1) {
        targetList = this.availableList;
    } else {
        targetList = this.selectedList;
    }

    var targetOptions = targetList.options;
    
    for (i = index + 1; i < targetOptions.length - 1; i++) {
        if (targetOptions[i].selected == true) {
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
            } else {
                // still room to move up, keep move up enabled
                this.disableButton(this.moveUpButton, false);
            }
            
            // see if the last valid item is selected
            if (lastSelection < targetOptions.length - 2) {
                // room to move down, enable the move down button
                this.disableButton(this.moveDownButton, false);
            } else {
                // disable move down
                this.disableButton(this.moveDownButton, true);
            }
        } else {
            // selections are NOT contiguous - enable both up and down
            this.disableButton(this.moveUpButton, false);
            this.disableButton(this.moveDownButton, false);
        }
    } else {
        // only one selected item is selected - first set move up button state
        if (index > 0) {
            // enable the move up button
            this.disableButton(this.moveUpButton, false);
        } else {
            // disable move up button
            this.disableButton(this.moveUpButton, true);
        }

        // now set the move down button state
        if (index < (targetOptions.length - 2)) {
            // enable move down button
            this.disableButton(this.moveDownButton, false);
        } else {
            // disable move down button
            this.disableButton(this.moveDownButton, true);
        }

    }
}

// This OO function maps to CCAddRemove::afterRemove()
//
// This method handles common after remove actions such as clearing the
// selection of the selected list (HCI requirement) and disabling the move
// buttons
function ccAddRemoveAfterRemove() {
    // disable the remove btn
    this.disableButton(this.removeButton, true);

    // enable the add buttons
    this.disableButton(this.addButton, false);
    this.disableButton(this.addAllButton, false);

    // clear any selection in the selected list
    this.selectedList.selectedIndex = -1;
    
    // update the state of the move buttons if present
    this.disableButton(this.moveUpButton, true);
    this.disableButton(this.moveDownButton, true);

    if (this.allowAvailableMove) {
        // available items can move, update move buttons
        this.updateMoveButtons(this.availableSelection());
    }
}

// This OO function maps to CCAddRemove::afterAdd()
//
// This method handles common after add actions such as clearing the
// selection of the available list (HCI requirement) and updating the state of
// the move buttons
function ccAddRemoveAfterAdd() {
    // disable the add button
    this.disableButton(this.addButton, true);

    // enable the remove button
    this.disableButton(this.removeButton, false);
    
    // if the remove all button is displayed, enable it
    this.disableButton(this.removeAllButton, false);
    
    // update the state of the move buttons
    this.updateMoveButtons(this.selectedSelection());
    
    // clear any selection in the available list
    this.availableList.selectedIndex = -1;
}

// This OO function maps to CCAddRemove::getOriginalIndex()
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
/* Helper functions used by the CCAddRemove javascript class. Note that the */
/* following are NOT class methods                                          */
/****************************************************************************/

// Helper function to determine if the given list box has multiple selections
function ccAddRemove_hasMultipleSelections(list) {
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
