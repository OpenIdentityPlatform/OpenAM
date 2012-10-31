/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: password.js,v 1.2 2008/06/25 05:44:27 qcheng Exp $
 *
 */

/** 
 * writes the corresponding css based on browser type 
 *
 * @param serviceUri
 *
 */
function writeCSS(serviceUri) {
    document.write("<link href='" + serviceUri);

    if (is_ie6up) {
        // IE 6.x or above.
        document.write("/css/css_ie6up.css");
    } else if (is_ie5up) {
        // IE 5.x or above.
        document.write("/css/css_ie5win.css");
    } else if (is_gecko) {
        // Netscape 6/7, Mozilla
        document.write("/css/css_ns6up.css");
    } else if (is_nav4 && is_win) {
        // Netscape 4 Windows.
        document.write("/css/css_ns4win.css");
    } else if (is_nav4) {
        // Netscape 4 Solaris & Linux.
        document.write("/css/css_ns4sol.css");
    } else {
        // All others
        document.write("/css/css_ns6up.css");
    }

    document.write("' type='text/css' rel='stylesheet' />");
}

/**
 * marks button
 *
 * @param label of button
 * @param href of button
 */
function markupButton(label, name) {
    label = "&nbsp;" + strTrim(label) + "&nbsp;";
    document.write("<td>");
    document.write("<div class=\"logBtn\">");    
    document.write("<input name=\"");
    document.write(name);
    document.write("\" type=\"submit\"");
    document.write(" class=\"Btn1Def\" value=\"");
    document.write(label);    
    document.write("\" onmouseover=\"javascript: if (this.disabled==0) this.className='Btn1DefHov'\"");
    document.write(" onmouseout=\"javascript: if (this.disabled==0) this.className='Btn1Def'\"");
    document.write(" onblur=\"javascript: if (this.disabled==0) this.className='Btn1Def'\"");
    document.write(" onfocus=\"javascript: if (this.disabled==0) this.className='Btn1DefHov'\"");    
    document.write("/></div></td>");
}

/**
 * trims leading and trailing spaces of a string
 *
 * @param str - string to trim
 * @return trimmed string
 */
function strTrim(str){
    return str.replace(/^\s+/,'').replace(/\s+$/,'')
}
