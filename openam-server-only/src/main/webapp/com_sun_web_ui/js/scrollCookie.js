//<!--
//
// ident "@(#)scrollCookie.js 1.4 04/08/23 SMI"
// 
// Copyright 2004 by Sun Microsystems, Inc. All rights reserved.
// Use is subject to license terms.
//
// This Javascript code will maintain scroll bar position during a
// page reload.

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Constructors
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// construct a javascript object for maintaining scroll position via a cookie
function ccScrollCookie(name, path) {
    // call the old base constructor
    this.ccScrollCookie(name);

    this.$path = path;
}

// this constructor is called by the one above and is left for compatability
// with 2.0 apps
function ccScrollCookie(name) {
    // All predefined properties of this object begin with '$' because
    // we don't want to store these values in the cookie.
    this.$cookieName = name;

    // Default properties.
    this.left = "0";
    this.top  = "0";

    // Object methods from cookie.js
    this.get   = ccGetCookie;
    this.load  = ccLoadCookie;
    this.reset = ccResetCookie;
    this.show  = ccShowCookie;
    this.store = ccStoreCookie;

    // Object methods from scrollCookie.js
    this.restore = ccRestoreScrollCookie;
    this.set     = ccSetScrollCookie;
}



// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Functions
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// This function will load the cookie and restore scroll position.
function ccRestoreScrollCookie() {
    // Load cookie value.
    this.load();

    // Set scroll position.
    window.scrollTo(parseInt(this.left), parseInt(this.top));

    return true;
}

// This function will set the cookie value.
function ccSetScrollCookie() {
    // Set object properties.
    this.left = 0;
    this.top  = 0;

    var documentElement = window.document.documentElement;

    if (documentElement && documentElement.scrollTop) {
        this.left = documentElement.scrollLeft;
        this.top  = documentElement.scrollTop;
    } else {
        this.left = window.document.body.scrollLeft;
        this.top  = window.document.body.scrollTop;
    }

    // if the left and top scroll values are still null
    // try to extract it assuming the browser is IE

    if (this.left == null && this.top == null) {
        this.left = window.pageXOffset;
        this.top = window.pageYOffset;
    }

    // Store cookie value.
    this.store();

    return true;
}

//-->
