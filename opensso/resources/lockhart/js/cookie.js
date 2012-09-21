//<!--
//
// ident "@(#)cookie.js 1.3 04/05/09 SMI"
// 
// Copyright 2004 by Sun Microsystems, Inc. All rights reserved.
// Use is subject to license terms.
//
// This Javascript code contains common functions used to maintain
// cookies. This file is used with focusCookie.js and scrollCookie.js,
// for example.

// This function will get the cookie value.
function ccGetCookie() {
    // Get document cookie.
    var cookie = document.cookie;

    // Parse ccScrollCookie value.
    var pos = cookie.indexOf(this.$cookieName + "=");

    if (pos == -1)
        return null;

    var start = pos + this.$cookieName.length + 1;

    var end = cookie.indexOf(";", start);

    if (end == -1)
        end = cookie.length;

    // return cookie value
    return cookie.substring(start, end);
}

// This function will load the cookie value.
function ccLoadCookie() {
    // Get document cookie.
    var cookieVal = this.get();

    if (cookieVal == null)
        return false;

    // Break cookie into names and values.
    var a = cookieVal.split('&');

    // Break each pair into an array.
    for (var i = 0; i < a.length; i++) {
        a[i] = a[i].split(':');
    }

    // Set name and values for this object.
    for (var i = 0; i < a.length; i++) {
        this[a[i][0]] = unescape(a[i][1]);
    }

    return true;
}

// This function will reset the cookie value.
function ccResetCookie() {
    // Clear cookie value.
    document.cookie = this.$cookieName + "=";

    return true;
}

// This function will display the cookie value.
function ccShowCookie() {
    alert(this.$cookieName + " = " + this.get());
    return true;
}

// This function will store the cookie value.
function ccStoreCookie() {
    // Create cookie value by looping through object properties
    var cookieVal = "";

    // Since cookies use the equals and semicolon signs as separators,
    // we'll use colons and ampersands for each variable we store.
    for (var prop in this) {
        // Ignore properties that begin with '$' and methods.
        if (prop.charAt(0) == '$' || typeof this[prop] == 'function')
            continue;
        
        if (cookieVal != "")
            cookieVal += '&';
        
        cookieVal += prop + ':' + escape(this[prop]);
    }

    var cookieString = this.$cookieName + "=" + cookieVal;

    if (this.$path != null) {
        cookieString += ";path=" + this.$path;
    }
    // Store cookie value.
    document.cookie = cookieString;

    return true;
}

//-->
