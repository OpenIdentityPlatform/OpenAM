/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define*/

define(["org/forgerock/openam/extensions/authmodules/adaptivedeviceprint/AbstractDevicePrintInfoCollector",
        "org/forgerock/openam/extensions/authmodules/adaptivedeviceprint/infocollectors/lib/fontdetect"],
        function(AbstractDevicePrintInfoCollector, fontDetector) {

    var obj = new AbstractDevicePrintInfoCollector(),
        fontsList = ["cursive","monospace","serif","sans-serif","fantasy","default","Arial","Arial Black",
        "Arial Narrow","Arial Rounded MT Bold","Bookman Old Style","Bradley Hand ITC","Century","Century Gothic",
        "Comic Sans MS","Courier","Courier New","Georgia","Gentium","Impact","King","Lucida Console","Lalit",
        "Modena","Monotype Corsiva","Papyrus","Tahoma","TeX","Times","Times New Roman","Trebuchet MS","Verdana",
        "Verona"];


    obj.gatherInformation = function() {
        console.log(fontDetector);
        var result = {}, i;
        result.installedFonts = "";
        for (i = 0; i < fontsList.length; i++) {
            if (fontDetector.detect(fontsList[i]))      {
                result.installedFonts = result.installedFonts + fontsList[i] + ";";
            }
        }
        return result;
    };

    return obj;
});