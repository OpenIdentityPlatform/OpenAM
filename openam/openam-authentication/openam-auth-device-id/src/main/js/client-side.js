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
 * Portions Copyrighted 2013-2014 ForgeRock AS.
 */

var collectScreenInfo = function () {
        var screenInfo = {};
        if (screen) {
            if (screen.width) {
                screenInfo.screenWidth = screen.width;
            }

            if (screen.height) {
                screenInfo.screenHeight = screen.height;
            }

            if (screen.pixelDepth) {
                screenInfo.screenColourDepth = screen.pixelDepth;
            }
        } else {
            console.warn("Cannot collect screen information. screen is not defined.");
        }
        return screenInfo;
    },
    collectTimezoneInfo = function () {
        var timezoneInfo =  {}, offset = new Date().getTimezoneOffset();

        if (offset) {
            timezoneInfo.timezone = offset;
        } else {
            console.warn("Cannot collect timezone information. timezone is not defined.");
        }

        return timezoneInfo;
    },
    collectBrowserPluginsInfo = function () {

        if (navigator && navigator.plugins) {
            var pluginsInfo = {}, i, plugins = navigator.plugins;
            pluginsInfo.installedPlugins = "";

            for (i = 0; i < plugins.length; i++) {
                pluginsInfo.installedPlugins = pluginsInfo.installedPlugins + plugins[i].filename + ";";
            }

            return pluginsInfo;
        } else {
            console.warn("Cannot collect browser plugin information. navigator.plugins is not defined.");
            return {};
        }

    },
// Getting geolocation takes some time and is done asynchronously, hence need a callback which is called once geolocation is retrieved.
    collectGeolocationInfo = function (callback) {
        var geolocationInfo = {},
            successCallback = function(position) {
                geolocationInfo.longitude = position.coords.longitude;
                geolocationInfo.latitude = position.coords.latitude;
                callback(geolocationInfo);
            }, errorCallback = function(error) {
                console.warn("Cannot collect geolocation information. " + error.code + ": " + error.message);
                callback(geolocationInfo);
            };
        if (navigator && navigator.geolocation) {
            // NB: If user chooses 'Not now' on Firefox neither callback gets called
            //     https://bugzilla.mozilla.org/show_bug.cgi?id=675533
            navigator.geolocation.getCurrentPosition(successCallback, errorCallback);
        } else {
            console.warn("Cannot collect geolocation information. navigator.geolocation is not defined.");
            callback(geolocationInfo);
        }
    },
    collectBrowserFontsInfo = function () {
        var fontsInfo = {}, i, fontsList = ["cursive","monospace","serif","sans-serif","fantasy","default","Arial","Arial Black",
            "Arial Narrow","Arial Rounded MT Bold","Bookman Old Style","Bradley Hand ITC","Century","Century Gothic",
            "Comic Sans MS","Courier","Courier New","Georgia","Gentium","Impact","King","Lucida Console","Lalit",
            "Modena","Monotype Corsiva","Papyrus","Tahoma","TeX","Times","Times New Roman","Trebuchet MS","Verdana",
            "Verona"];
        fontsInfo.installedFonts = "";

        for (i = 0; i < fontsList.length; i++) {
            if (fontDetector.detect(fontsList[i])) {
                fontsInfo.installedFonts = fontsInfo.installedFonts + fontsList[i] + ";";
            }
        }
        return fontsInfo;
    },
    devicePrint = {};

devicePrint.screen = collectScreenInfo();
devicePrint.timezone = collectTimezoneInfo();
devicePrint.plugins = collectBrowserPluginsInfo();
devicePrint.fonts = collectBrowserFontsInfo();

if (navigator.userAgent) {
    devicePrint.userAgent = navigator.userAgent;
}
if (navigator.appName) {
    devicePrint.appName = navigator.appName;
}
if (navigator.appCodeName) {
    devicePrint.appCodeName = navigator.appCodeName;
}
if (navigator.appVersion) {
    devicePrint.appVersion = navigator.appVersion;
}
if (navigator.appMinorVersion) {
    devicePrint.appMinorVersion = navigator.appMinorVersion;
}
if (navigator.buildID) {
    devicePrint.buildID = navigator.buildID;
}
if (navigator.platform) {
    devicePrint.platform = navigator.platform;
}
if (navigator.cpuClass) {
    devicePrint.cpuClass = navigator.cpuClass;
}
if (navigator.oscpu) {
    devicePrint.oscpu = navigator.oscpu;
}
if (navigator.product) {
    devicePrint.product = navigator.product;
}
if (navigator.productSub) {
    devicePrint.productSub = navigator.productSub;
}
if (navigator.vendor) {
    devicePrint.vendor = navigator.vendor;
}
if (navigator.vendorSub) {
    devicePrint.vendorSub = navigator.vendorSub;
}
if (navigator.language) {
    devicePrint.language = navigator.language;
}
if (navigator.userLanguage) {
    devicePrint.userLanguage = navigator.userLanguage;
}
if (navigator.browserLanguage) {
    devicePrint.browserLanguage = navigator.browserLanguage;
}
if (navigator.systemLanguage) {
    devicePrint.systemLanguage = navigator.systemLanguage;
}

// Attempt to collect geo-location information and return this with the data collected so far.
// Otherwise, if geo-location fails or takes longer than 30 seconds, auto-submit the data collected so far.
autoSubmitDelay = 30000;
output.value = JSON.stringify(devicePrint);
collectGeolocationInfo(function(geolocationInfo) {
    devicePrint.geolocation = geolocationInfo;
    output.value = JSON.stringify(devicePrint);
    submit();
});
