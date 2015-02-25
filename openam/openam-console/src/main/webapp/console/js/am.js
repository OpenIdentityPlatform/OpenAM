/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: am.js,v 1.10 2009/07/20 23:02:31 asyhuang Exp $
 *
 */

/*
 * Portions Copyright 2014 ForgeRock AS
 */

/**
 * URL of JATO component that handles script upload.
 * @type {string}
 */
var SCRIPT_UPLOAD_URL = "ScriptUploader";

var origFrmAction = '';
var tblBtnCounter = new Array();

function openNewWindow() {
    var feature = 'directories=no,location=0,menubar=0,status=0,titlebar=yes,toolbar=no,scrollbars=yes,width=800,height=600,resizable=yes';
    var openwin = window.open('', 'newwindow', feature);
    openwin.focus();
}

function submitButton(btn, val) {
    var frm = document.forms[0];
    frm.target = 'newwindow';
    origFrmAction = frm.action;
    frm.action += '?attrname=' + val;
    setTimeout("resetForm()", 1000);
}

/**
 * Submit a dynamic validation request for the specific attribute.
 * @param btn The button from which this request originated.
 * @param val The name of the attribute for which the validation is needed.
 */
function submitValidate(btn, val) {
    var frm = document.forms[0];
    frm.target = '';
    origFrmAction = frm.action;
    frm.action += '?dynamic_validation=true&attrname=' + val;
    setTimeout("resetForm()", 1000);
}

/**
 * Submits a dynamic request to upload a file on a property sheet.
 * @param btn The button from which this request originated.
 * @param val The name of the attribute for which the validation is needed.
 */
function submitFileUpload(btn, val) {
    var uploadWindow = window.open(SCRIPT_UPLOAD_URL, val, 'height=300,width=650');
    uploadWindow.focus();
}

function resetForm() {
    var frm = document.forms[0];
    frm.target = '';
    frm.action = origFrmAction;
}

/**
 * Enables and Disables the Delete button of a table.
 *
 * @param formName Name of form.
 * @param tblName Name of table.
 * @param counterName Name of counter.
 * @param btn Button object.
 * @param trigger Object that triggers this event.
 */
function toggleTblButtonState(formName, tblName, counterName, btn, trigger) {
    toggleTblButtonStateEx(formName, tblName, counterName, btn, trigger, false);
}

/**
 * Enables and Disables the Delete button of a table.
 *
 * @param formName Name of form.
 * @param tblName Name of table.
 * @param counterName Name of counter.
 * @param btn Button object.
 * @param trigger Object that triggers this event.
 * @param singleCheckbox true if button is enable is only one checkbox iss
 * selected.
 */
function toggleTblButtonStateEx(formName, tblName, counterName, btn, trigger,
    singleCheckbox) {
    if (tblBtnCounter[counterName] == undefined) {
	tblBtnCounter[counterName] = 0;
    }
    var prevState = (singleCheckbox) ? (tblBtnCounter[counterName] != 1) :
        (tblBtnCounter[counterName] <= 0);

    if (trigger.name.indexOf('DeselectAllHref') != -1) {
	tblBtnCounter[counterName] = 0;
    } else if (trigger.name.indexOf('SelectAllHref') != -1) {
	tblBtnCounter[counterName] = countCheckboxesInTable(formName, tblName);
    } else {
	if (trigger.checked) {
	    tblBtnCounter[counterName]++;
	} else {
	    tblBtnCounter[counterName]--;
	}
    }

    var currState = (singleCheckbox) ? (tblBtnCounter[counterName] != 1) :
        (tblBtnCounter[counterName] <= 0);

    if (btn) {
	if (prevState != currState) {
	    ccSetButtonDisabled(btn, formName, currState);
	}
    }
}

function countCheckboxesInTable(formName, tblName) {
    var frm = document.forms[formName];
    var cbCount = 0;
                                                                                
    for (var i = 0; i < frm.elements.length; i++) {
	var e = frm.elements[i];
        if ((e.type == 'checkbox') &&
	    (e.name.indexOf(tblName + '.SelectionCheckbox') != -1)
        ) {
	    cbCount++;
	}
    }
                                                                                
    return cbCount;
}

function getXmlHttpRequestObject() {
    if(window.ActiveXObject) {
        return new ActiveXObject("Microsoft.XMLHTTP"); //IE
    } else {
        return new XMLHttpRequest(); 
    }
}

function ajaxGet(req, url, callback) {
    if (req.readyState == 4 || req.readyState == 0) {
        req.open("GET", url, true);
        req.onreadystatechange = callback;
        req.send(null);
    }
}

function ajaxPost(req, url, params, callback) {
    if (req.readyState == 4 || req.readyState == 0) {
        req.open("POST", url, true);
        req.setRequestHeader("Content-type",
           "application/x-www-form-urlencoded");
        req.setRequestHeader("Content-length", params.length);
        req.setRequestHeader("Connection", "close");
        req.onreadystatechange = callback;
        req.send(params);
    }
}

function fade() {
    if( window.innerHeight && window.scrollMaxY ) {// Firefox 
        pageHeight = window.innerHeight + window.scrollMaxY;
    } else if( document.body.scrollHeight > document.body.offsetHeight ) { 
        // all but Explorer Mac
        pageHeight = document.body.scrollHeight;
    } else { // works in Explorer 6 Strict, Mozilla (not FF) and Safari
        pageHeight = document.body.offsetHeight + document.body.offsetTop;
    }
        
    var maindiv = document.getElementById('main');
    maindiv.style.height = pageHeight + 'px';
    maindiv.style.opacity = 0.6;
    maindiv.style.backgroundColor = 'darkgray';
    document.getElementById('dlg').style.display='block';
}

function getWindowHeight() {
    var myHeight = 0;
    if( typeof( window.innerWidth ) == 'number' ) {
        //Non-IE
        myHeight = window.innerHeight;
    } else if( document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight ) ) {
        //IE 6+ in 'standards compliant mode'
        myHeight = document.documentElement.clientHeight;
    } else if( document.body && ( document.body.clientWidth || document.body.clientHeight ) ) {
        //IE 4 compatible
        myHeight = document.body.clientHeight;
    }
    return myHeight;
}

function focusMain() {
    var maindiv = document.getElementById('main');
    maindiv.style.opacity = 1.0
    maindiv.style.backgroundColor = 'white';
    document.getElementById('dlg').style.display='none';
}

function chooseRadio(frm, name, value) {
    var r = frm.elements[name];
    for (var i = 0; i < r.length; i++) {
        if (r[i].value == value) {
            r[i].checked = true;
        }
    }
}

function getRadioVal(frm, name) {
    var r = frm.elements[name];
    for (var i = 0; i < r.length; i++) {
        if (r[i].checked) {
            return r[i].value;
        }
    }
}

function selectOption(frm, name, value) {
    var selectObj = frm.elements[name];
    var sz = selectObj.options.length;
    for (var i = 0; i < sz; i++) {
        if (selectObj.options[i].value == value) {
            selectObj.options[i].selected = true;
            return;
        }
    }
}

function getSelectedOption(frm, name) {
    var selectObj = frm.elements[name];
    var sz = selectObj.options.length;
    for (var i = 0; i < sz; i++) {
        if (selectObj.options[i].selected) {
            return selectObj.options[i].value;
        }
    }
    return "";
}

function clearOptions(frm, name) {
    var selectObj = frm.elements[name];
    var sz = selectObj.options.length;
    for (var i = sz-1; i >=0; --i) {
        selectObj.options[i] = null;
   }
}

function addOption(frm, name, value) {
    var selectObj = frm.elements[name];
    selectObj.options[selectObj.options.length] = new Option(value, value);
}

function constructArray(strArray) {
    var sIdx = strArray.indexOf("{");
    var eIdx = strArray.indexOf("}");

    while ((sIdx > -1) && (eIdx > -1)) {
        var str = strArray.substring(sIdx+1, eIdx);
        var idx = str.indexOf("=");
        if (idx != -1) {
            var arrayName = str.substring(0, idx);
            eval(arrayName + '= new Array();');
            str = str.substring(idx+1);
            idx = str.indexOf(",");
            var secIdx = 0;

            while (idx != -1) {
                eval(arrayName + '[' + secIdx + ']=\'' + str.substring(0, idx) + '\';');
                str = str.substring(idx+1);
                idx = str.indexOf(",");
                secIdx++;
            }
            if (str != "") {
                eval(arrayName + '[' + secIdx + ']=\'' + str + '\';');
            }
        }
        strArray = strArray.substring(eIdx +1);
        sIdx = strArray.indexOf("{");
        eIdx = strArray.indexOf("}");
    }
}

function disableButton(frm, btnName, bDisable) {
    var btn = frm.elements[btnName];
    if (btn) {
        if (bDisable) {
            btn.className = 'Btn1Dis';
            btn.disabled = 'disabled';
        } else {
            btn.className = 'Btn1';
            btn.disabled = 0;
        }
    }
}

function escapeEx(d) {
    var escaped = encodeURIComponent(d);
    return escaped.replace(/\+/g, "%2B");
}

function hexToString(str) {
    var result = '';
    var idx = str.indexOf('\\u');
    while (idx != -1) {
        if (idx > 0) {
            result += str.substring(0, idx);
        }
        var tmp = str.substring(idx, idx+6);
        str = str.substring(idx+6);
        eval("result += '" + tmp + "'");
        idx = str.indexOf('\\u');
    }
    result += str;
    return result;
}

