/*
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
 */
/*
 * Portions Copyrighted 2013 Syntegrity.
 * Portions Copyrighted 2013-2016 ForgeRock Inc.
 */

/*global define*/

define(["org/forgerock/openam/extensions/authmodules/adaptivedeviceprint/Constants",
        "org/forgerock/libwrappers/JQueryWrapper"],
		function(constants, $) {

    /**
     * listenerProxyMap - Association of real listeners and proxies which transforms parameter set
     */
    var obj = {}, listenerProxyMap = [];

    obj.sendEvent = function (eventId, event) {
        console.log("sending event eventId=" + eventId);
        $(document).trigger(eventId, event);
    };

    obj.registerListener = function (eventId, callback) {
        var proxyFunction = function(element, event) {
            console.log("Handiling event");
            callback(event);
        };
        console.log("registering event listener eventId=" + eventId);
        listenerProxyMap[callback] = proxyFunction;
        $(document).on(eventId, proxyFunction);
    };

    obj.unregisterListener = function (eventId, callback) {
        var proxyFunction;
        console.log("unregistering event listener eventId=" + eventId);
        proxyFunction = listenerProxyMap[callback];
        $(document).off(proxyFunction);
    };

    return obj;
});
