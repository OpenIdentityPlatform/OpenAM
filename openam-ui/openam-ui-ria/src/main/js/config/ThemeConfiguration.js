/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

define("config/ThemeConfiguration", {
    themes: {
        // There must be a theme named "default".
        "default": {
            // An ordered list of URLs to stylesheets that will be applied to every page.
            stylesheets: ["css/bootstrap-3.3.5-custom.css", "css/structure.css", "css/theme.css"],
            // A path that is prepended to every relative URL when fetching resources (including images, stylesheets and
            // HTML template files).
            path: "",
            // A URL to a favicon icon
            icon: "favicon.ico",
            settings: {
                // This logo is displayed on user profile pages.
                logo: {
                    // The URL of the image.
                    src: "images/logo-horizontal.png",
                    // The title attribute used on <img> tags.
                    title: "ForgeRock",
                    // The alt attribute used on <img> tags.
                    alt: "ForgeRock",
                    // The width of the logo as a CSS length.
                    width: "202px"
                },
                // This logo is displayed on login pages.
                loginLogo: {
                    // The URL of the image.
                    src: "images/login-logo.png",
                    // The title attribute used on <img> tags.
                    title: "ForgeRock",
                    // The alt attribute used on <img> tags.
                    alt: "ForgeRock",
                    // The height of the logo as a CSS length.
                    height: "104px",
                    // The width of the logo as a CSS length.
                    width: "210px"
                },
                // The footer is displayed on every page.
                footer: {
                    // A contact email address.
                    mailto: "info@forgerock.com",
                    // A contact phone number. If empty, it will not be displayed.
                    phone: ""
                }
            }
        },
        "fr-dark-theme": {
            // An ordered list of URLs to stylesheets that will be applied to every page.
            stylesheets: [
                "themes/dark/css/bootstrap.min.css",
                "css/structure.css",
                "themes/dark/css/theme-dark.css"
            ],
            settings: {
                loginLogo: {
                    src: "themes/dark/images/login-logo-white.png",
                    title: "ForgeRock",
                    alt: "ForgeRock",
                    height: "228px",
                    width: "220px"
                }
            }
        }
    },
    // Each mapping will be tested in order. The theme from the first matching mapping will be used. If no mapping
    // matches then the theme "default" will be used.
    mappings: [
        // Use the theme with the key "my-theme" if the realm is either /my-realm or /my/sub-realm.
        //{ theme: "my-theme", realms: ["/my-realm", "/my/sub-realm"] }
        // Use the theme "my-second-theme" if the realm starts with /a. e.g. /ab or /a/c.
        //{ theme: "my-second-theme", realms: [/^\/a/] }
        // Use the theme "my-third-theme" if the realm is /a and the authentication chain is auth-chain-1.
        //{ theme: "my-third-theme", realms: ["/a"], authenticationChains: ["auth-chain-1"] }
        // Use the theme "my-fourth-theme" if the default authentication chain is in use.
        //{ theme: "my-fourth-theme", authenticationChains: [""] }
    ]
});
