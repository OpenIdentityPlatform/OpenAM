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
 * $Id: admincli.js,v 1.2 2008/06/25 05:44:55 qcheng Exp $
 *
 */

var listboxes = new Array();

function addOption(opt) {
    var frm = document.forms[0];
    var lblb = frm.elements[opt + 'lblb'];
    var lbValue = strTrim(lblb.value);

    if (lbValue != '') {
        var selectBox = frm.elements[opt];
        var optList = selectBox.options;
        optList[optList.length] = new Option(lbValue, lbValue);
        lblb.value = '';
    }
}

function strTrim(str){
    return str.replace(/^\s+/,'').replace(/\s+$/,'')
}

function removeSelFromList(opt) {
    var frm = document.forms[0];
    var list = frm.elements[opt];

    if (list != null) {
        var optList = list.options;
        var size = optList.length;

        for (var i = size-1; i >= 0; --i) {
            var opt = optList[i];
            if ((opt.selected) && (opt.value != "")) {
                optList[i] = null;
            }
        }
    }
}

function selectListBoxes(frm) {
    for (var i = 0; i < listboxes.length; i++) {
        var list = frm.elements[listboxes[i]];
        for (var j = 0; j < list.options.length; j++) {
            list.options[j].selected = true;
        }
    }
}
