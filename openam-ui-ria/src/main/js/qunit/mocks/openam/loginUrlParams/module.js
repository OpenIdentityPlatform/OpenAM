/*global require, define*/
define(function () {

    return function (server) {
    
        server.respondWith(
            "POST",   
            "/openam/json/users/?_action=idFromSession",
            [
                401, 
                { 
                    "Date": "Mon, 19 Aug 2013 22:58:19 GMT",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=78",
                    "Content-Length": "69",
                    "Content-Type": "application/json;charset=ISO-8859-1"
                },
                "{ \"code\": 401, \"reason\": \"Unauthorized\", \"message\": \"Access denied\" }"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openam/json/auth/1/authenticate?module=OATH&authIndexType=module&authIndexValue=OATH&locale=en-US",
            [
                200, 
                { 
                    "Date": "Mon, 19 Aug 2013 22:58:19 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=75",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"authId\": \"eyAiYWxnIjogIkhTMjU2IiwgInR5cCI6ICJqd3QiIH0.eyAib3RrIjogImtzZnZhb3EzMzdzYzFwa2FjbjY4NzR0ZWg0IiwgImF1dGhJbmRleFZhbHVlIjogIk9BVEgiLCAiYXV0aEluZGV4VHlwZSI6ICJtb2R1bGVfaW5zdGFuY2UiLCAic2Vzc2lvbklkIjogIkFRSUM1d00yTFk0U2Zjemx1cVYtbm1vSnVIdkpSZ0U3OXh1cFpFVFE3SzR4SVFjLipBQUpUU1FBQ01ERUFBbE5MQUJRdE16RTVPRFUwTURjeU1ERXhPVGs0TnpZNE5BLi4qIiwgInJlYWxtIjogImRjPW9wZW5hbSxkYz1mb3JnZXJvY2ssZGM9b3JnIiB9.ffJ8DjNoZCgXILI3WgZ2dYR6pYkqtSDUtvnt4jcCXQg\", \"template\": \"\", \"stage\": \"OATH1\", \"callbacks\": [ { \"type\": \"PasswordCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \"One Time Password:\" } ], \"input\": [ { \"name\": \"IDToken1\", \"value\": \"\" } ] }, { \"type\": \"ConfirmationCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \"\" }, { \"name\": \"messageType\", \"value\": 0 }, { \"name\": \"options\", \"value\": [ \"Submit OTP Code\" ] }, { \"name\": \"optionType\", \"value\": -1 }, { \"name\": \"defaultOption\", \"value\": 0 } ], \"input\": [ { \"name\": \"IDToken2\", \"value\": 0 } ] } ] }"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openam/json/auth/1/authenticate?module=OATH&authIndexType=module&authIndexValue=OATH",
            [
                200, 
                { 
                    "Date": "Mon, 19 Aug 2013 22:58:19 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=75",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"authId\": \"eyAiYWxnIjogIkhTMjU2IiwgInR5cCI6ICJqd3QiIH0.eyAib3RrIjogImtzZnZhb3EzMzdzYzFwa2FjbjY4NzR0ZWg0IiwgImF1dGhJbmRleFZhbHVlIjogIk9BVEgiLCAiYXV0aEluZGV4VHlwZSI6ICJtb2R1bGVfaW5zdGFuY2UiLCAic2Vzc2lvbklkIjogIkFRSUM1d00yTFk0U2Zjemx1cVYtbm1vSnVIdkpSZ0U3OXh1cFpFVFE3SzR4SVFjLipBQUpUU1FBQ01ERUFBbE5MQUJRdE16RTVPRFUwTURjeU1ERXhPVGs0TnpZNE5BLi4qIiwgInJlYWxtIjogImRjPW9wZW5hbSxkYz1mb3JnZXJvY2ssZGM9b3JnIiB9.ffJ8DjNoZCgXILI3WgZ2dYR6pYkqtSDUtvnt4jcCXQg\", \"template\": \"\", \"stage\": \"OATH1\", \"callbacks\": [ { \"type\": \"PasswordCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \"One Time Password:\" } ], \"input\": [ { \"name\": \"IDToken1\", \"value\": \"\" } ] }, { \"type\": \"ConfirmationCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \"\" }, { \"name\": \"messageType\", \"value\": 0 }, { \"name\": \"options\", \"value\": [ \"Submit OTP Code\" ] }, { \"name\": \"optionType\", \"value\": -1 }, { \"name\": \"defaultOption\", \"value\": 0 } ], \"input\": [ { \"name\": \"IDToken2\", \"value\": 0 } ] } ] }"
            ]
        );
    
        server.respondWith(
            "GET",   
            "templates/openam/authn/OATH1.html",
            [
                404, 
                { 
                    "Date": "Mon, 19 Aug 2013 22:58:19 GMT",
                    "Server": "Apache/2.2.22 (Unix) DAV/2 PHP/5.3.15 with Suhosin-Patch mod_ssl/2.2.22 OpenSSL/0.9.8x",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=74",
                    "Content-Length": "244",
                    "Content-Type": "text/html; charset=iso-8859-1"
                },
                "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n<html><head>\n<title>404 Not Found</title>\n</head><body>\n<h1>Not Found</h1>\n<p>The requested URL /openam-debug/templates/openam/authn/OATH1.html was not found on this server.</p>\n</body></html>\n"
            ]
        );
    
        server.respondWith(
            "GET",   
            "templates/openam/RESTLoginTemplate.html",
            [
                200, 
                { 
                    "Date": "Mon, 19 Aug 2013 22:58:19 GMT",
                    "Last-Modified": "Sat, 27 Jul 2013 00:48:57 GMT",
                    "Server": "Apache/2.2.22 (Unix) DAV/2 PHP/5.3.15 with Suhosin-Patch mod_ssl/2.2.22 OpenSSL/0.9.8x",
                    "ETag": "&quot;807cf2-261-4e2739fcd7040&quot;",
                    "Content-Type": "text/html",
                    "Connection": "Keep-Alive",
                    "Accept-Ranges": "bytes",
                    "Keep-Alive": "timeout=5, max=81",
                    "Content-Length": "609"
                },
                "<style>\n#header{\n    width: {{theme.settings.lessVars.login-container-width}};\n}\n</style>\n<div class=\"container-shadow\" id=\"login-container\">\n    <div class=\"column-layout\" style=\"padding-top: 30px;\">\n        <div>\n            <form action=\"\" method=\"post\" class=\"form\">\n                <fieldset>\n    \n                    {{#each reqs.callbacks}}\n                    <div class=\"field {{#if isSubmit}}field-submit{{/if}}\">\n                        {{callbackRender}}\n                    </div>\n                    {{/each}}\n    \n                </fieldset>\n            </form>\n        </div>\n    </div>\n</div>"
            ]
        );

    };

});
