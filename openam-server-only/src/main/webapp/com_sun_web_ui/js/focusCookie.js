//<!--
//
// ident "@(#)focusCookie.js 1.5 04/05/09 SMI"
// 
// Copyright 2004 by Sun Microsystems, Inc. All rights reserved.
// Use is subject to license terms.
//
// This Javascript code will maintain element focus during a page
// reload.
//
// Note: If there is more than one element with the same name, the ID
// will be tested. If a unique ID is not used, the focus is applied to
// the first element found. In order to maintain focus, elements must
// appear within a form; however, this is not required for links.

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Constructors
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// construct a javascript object for maintaining focus via a cookie
function ccFocusCookie(name, path) {
    // call the base constructor
    this(name);

    this.$path = path;
}

// this constructor is called by the one above and is left for compatability
// with 2.0 apps
function ccFocusCookie(name) {
    // All predefined properties of this object begin with '$' because
    // we don't want to store these values in the cookie.
    this.$cookieName = name;

    // Default properties.
    this.id   = "";
    this.name = "";
    this.form = "";

    // Object methods from cookie.js
    this.get   = ccGetCookie;
    this.load  = ccLoadCookie;
    this.reset = ccResetCookie;
    this.show  = ccShowCookie;
    this.store = ccStoreCookie;

    // Object methods from focusCookie.js
    this.restore = ccRestoreFocusCookie;
    this.set     = ccSetFocusCookie;
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Functions
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// This function will restore the cookie value.
function ccRestoreFocusCookie() {
    // Load cookie value.
    this.load();
    var elements = null;

    // If name isn't set, don't parse elements.
    if (this.name == "")
        return false;

    // If form doesn't exist, it's a link.
    if (this.form)
        elements = eval("document." + this.form + ".elements");
    else
        elements = document.links;

    // Parse each form element and test name and id.
    for (i = 0; i < elements.length; i++) {
        if (this.name == elements[i].name) {
            if (this.id != "" && this.id != elements[i].id)
                continue;
            
            // Test if element is disabled.
            if (elements[i].disabled)
                return false;
            
            // Set element focus.
            elements[i].focus();
            return true;
        }
    }

    return false;
}

// This function will set the cookie value.
function ccSetFocusCookie(element) {
    if (element == null)
	return false;

    // Set object properties.
    if (element.id)
        this.id = element.id;
    else
        this.id = "";
    if (element.name)
        this.name = element.name;
    else
        this.name = "";
    if (element.form)
        this.form = element.form.name;
    else
        this.form = "";

    // Store cookie value.
    this.store();

    return true;
}

//-->
