/*global define, screen*/

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
 * Portions Copyrighted 2013 ForgeRock Inc.
 */

define(["org/forgerock/openam/extensions/authmodules/adaptivedeviceprint/AbstractDevicePrintInfoCollector",
        "org/forgerock/openam/extensions/authmodules/adaptivedeviceprint/EventManager",
        "org/forgerock/openam/extensions/authmodules/adaptivedeviceprint/Constants"],
        function(AbstractDevicePrintInfoCollector,
                 eventManager,
                 constants) {

    var obj = new AbstractDevicePrintInfoCollector(), position;
    
    obj.gatherInformation = function() {        
        if(obj.position) {
            var ret = {}, i;            
           
            ret.longitude = obj.position.coords.longitude;
            ret.latitude = obj.position.coords.latitude;

            return ret;
        } else {
            console.log("GeolocationCollector: geolocation not readed");
            return null;
        }
    };
    
    obj.getPosition = function() {
        if(navigator && navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(function(pos) {
                obj.position = pos;
                console.log("GeolocationCollector: geolocation readed");
                
                eventManager.sendEvent(constants.EVENT_RECOLLECT_DATA);
            });
        } else {
            console.error("GeolocationCollector: navigator.geolocation not defined"); 
            return null;
        }
    };   
    
    obj.getPosition();
    
    return obj; 
});
