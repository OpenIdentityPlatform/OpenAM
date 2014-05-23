/*global require, define*/
define([
    "text!templates/policy/PolicyHome.html"
    ], function () {

    /* an unfortunate need to duplicate the file names here, but I haven't
       yet found a way to fool requirejs into doing dynamic dependencies */
    var staticFiles = [
            "templates/policy/PolicyHome.html"
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

    };

});
