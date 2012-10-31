//<!--
//
// ident	"@(#)stylesheet.js 1.6 03/11/25 SMI"
//
// Copyright 2002-2003 by Sun Microsystems, Inc. All rights reserved.
// Use is subject to license terms.
//
// This Javascript code will add a stylesheet according to the variables
// set via the browserVersion.js Javascript file.
//
    document.write("<link href='");

    if (is_ie6up) {
        // IE 6.x or above.
        document.write("/com_sun_web_ui/css/css_ie6up.css");
    } else if (is_ie5up) {
        // IE 5.x or above.
        document.write("/com_sun_web_ui/css/css_ie5win.css");
    } else if (is_gecko) {
        // Netscape 6/7, Mozilla
        document.write("/com_sun_web_ui/css/css_ns6up.css");
    } else if (is_nav4 && is_win) {
        // Netscape 4 Windows.
        document.write("/com_sun_web_ui/css/css_ns4win.css");
    } else if (is_nav4) {
        // Netscape 4 Solaris & Linux.
        document.write("/com_sun_web_ui/css/css_ns4sol.css");
    } else {
        // All others
        document.write("/com_sun_web_ui/css/css_ns6up.css");
    }

    document.write("' type='text/css' rel='stylesheet' />");
//-->
