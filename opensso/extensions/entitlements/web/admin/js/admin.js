/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: admin.js,v 1.11 2009/06/04 11:49:35 veiming Exp $
 */

var submitTimeout = null;

function doSubmit(element) {
        iceSubmitPartial(
            document.getElementById("form"),
            element,
            MouseEvent.CLICK
        );
        submitTimeout = null;
        setFocus(element);
}

function submitNow(element) {
    if (submitTimeout != null) {
        clearTimeout(submitTimeout);
    }
    submitTimeout = setTimeout(function(){doSubmit(element)}, 1000);
}

function resizeFrame(f) {
    f.style.height = f.contentWindow.document.body.scrollHeight+'px';
}

function focusWizardStep(step) {
    href = '#wstep'+step;
    window.location.href = href;
}
