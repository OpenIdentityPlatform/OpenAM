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

define(["org/forgerock/openam/extensions/authmodules/adaptivedeviceprint/AbstractDevicePrintInfoCollector"],
        function(AbstractDevicePrintInfoCollector) {

    var obj = new AbstractDevicePrintInfoCollector();
    
    obj.gatherInformation = function() {        
        if(navigator && navigator.plugins) {
            var ret = {}, plugins = navigator.plugins, i;
            ret.installedPlugins = "";
            
            for(i = 0; i < plugins.length; i++) {
               ret.installedPlugins = ret.installedPlugins + plugins[i].filename + ";";
            }
            
            ret.installedPlugins = ret.installedPlugins.substr(0, ret.installedPlugins.length - 1);

            return ret;
        } else {
            console.error("BrowserPluginsCollector: navigator.plugins not defined");
            return null;
        }
    };

    return obj;
});
