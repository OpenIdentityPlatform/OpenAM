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
 * $Id: opensso.js,v 1.2 2008/06/25 05:44:55 qcheng Exp $
 *
 */

function getBrowserLocale() {
    var language;

    if (navigator.browserLanguage) {
        // IE default language for the browser
        language = navigator.browserLanguage
    } else if (navigator.userLanguage) {
        // IE browser language set by the user
        language = navigator.userLanguage
    } else if (navigator.systemLanguage) {
        // IE operating system language
        language = navigator.systemLanguage
    } else if (navigator.language) {
        // Netscape language sniff
        language = navigator.language
    } else {
        language = "en";
    }

    return language.replace(/\-/, '_');
}

function getContextPath() {
    var url = "" + document.location;
    var idx = url.indexOf('://');
    var path = url.substring(idx +3);
    idx = path.indexOf('/');
    path = path.substring(idx+1);
    idx = path.indexOf('/');
    return '/' + path.substring(0, idx);
}
