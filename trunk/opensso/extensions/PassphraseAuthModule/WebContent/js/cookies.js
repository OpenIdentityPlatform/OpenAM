/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [2012] [Forgerock AS]"
 */

function createCookie(name,value,days) {
	if (days) {
		var date = new Date();
		date.setTime(date.getTime()+(days*24*60*60*1000));
		var expires = "; expires="+date.toGMTString();
	}
	else var expires = "";
	document.cookie = name+"="+value+expires+"; path=/";
}

function readCookie(name) {
	var nameEQ = name + "=";
	var ca = document.cookie.split(';');
	for(var i=0;i < ca.length;i++) {
		var c = ca[i];
		while (c.charAt(0)==' ') c = c.substring(1,c.length);
		if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
	}
	return null;
}

function eraseCookie(name) {
	createCookie(name,"",-1);
}
function redirectToAuth() {
    var params = getQueryParameters();
    // var url = 'UI/Login';
	// redirecting to agreement page with query parameters
	var url = '/opensso/agreement.html';
    if (params != '') {
        url += params;
    }
	top.location.replace(url);
}

function checkCookie(){
	
	if(readCookie('isAgreed')=='true'){
		// do nothing or erase cookie or set cookie value to false
		//createCookie('isAgreed', 'false', 1);
	}else{
		redirectToAuth();
		//this time redirection will be to agreement.html in place of UI/Login
	}
}
function redirectToAuth1() {
    var params = getQueryParameters();
    var url = 'UI/Login';
	// setting the cookie for one day
    createCookie('isAgreed','true',1);
	if (params != '') {
        url += params;
    }
	
	top.location.replace(url);
}

function getQueryParameters() {
    var loc = '' + location;
    var idx = loc.indexOf('?');
    if (idx != -1) {
        return loc.substring(idx);
    } else {
        return '';
    }
}
