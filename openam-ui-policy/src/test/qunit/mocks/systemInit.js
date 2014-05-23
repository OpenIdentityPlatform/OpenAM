/*global require, define*/
define([
    "text!locales/en/translation.json",
    "text!themeConfig.json",
    "text!libs/less-1.5.1-min.js",
    "text!css/styles.less",
    "text!css/common/config.less",
    "text!css/common/helpers.less",
    "text!css/common/layout.less",
    "text!css/common/forms.less",
    "text!templates/policy/BaseTemplate.html",
    "text!templates/common/NavigationTemplate.html",
    "text!templates/common/FooterTemplate.html"
    ], function () {

    /* an unfortunate need to duplicate the file names here, but I haven't
       yet found a way to fool requirejs into doing dynamic dependencies */
    var staticFiles = [
            "locales/en/translation.json",
            "themeConfig.json",
            "libs/less-1.5.1-min.js",
            "css/styles.less",
            "css/common/config.less",
            "css/common/helpers.less",
            "css/common/layout.less",
            "css/common/forms.less",
            "templates/policy/BaseTemplate.html",
            "templates/common/NavigationTemplate.html",
            "templates/common/FooterTemplate.html"
        ],
        deps = arguments;

    return function (server) {

        _.each(staticFiles, function (file, i) {
            server.respondWith(
                "GET",
                new RegExp(file.replace(/([\/\.\-])/g, "\\$1") + "$"),
                [
                    200,
                    { },
                    deps[i]
                ]
            );
        });

        server.respondWith(
            "POST",   
            new RegExp("/openam/json/users\\?_action=idFromSession$"),
            [
                200, 
                { },
                "{\"id\":\"amadmin\",\"realm\":\"/\",\"dn\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"successURL\":\"/openam/console\",\"fullLoginURL\":\"/openam/UI/Login?realm=%2F&goto=http%3A%2F%2Famserver.restful.com%2Fopenam-policy-debug%2F\"}"
            ]
        );

    };

});
