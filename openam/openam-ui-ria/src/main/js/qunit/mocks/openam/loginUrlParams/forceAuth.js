/*global require, define*/
define(function () {

    return function (server) {
    
        server.respondWith(
            "POST",   
            "/openam/json/users/?_action=idFromSession",
            [
                401, 
                { 
                    "Date": "Thu, 22 Aug 2013 16:39:32 GMT",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=82",
                    "Content-Length": "69",
                    "Content-Type": "application/json;charset=ISO-8859-1"
                },
                "{ \"code\": 401, \"reason\": \"Unauthorized\", \"message\": \"Access denied\" }"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openam/json/authenticate?locale=en-US",
            [
                200, 
                { 
                    "Date": "Thu, 22 Aug 2013 16:39:32 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=79",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"authId\": \"eyAiYWxnIjogIkhTMjU2IiwgInR5cCI6ICJqd3QiIH0.eyAib3RrIjogIjZtbDRiMmZldGc2dmJxZXRndDk5OG8yYjhuIiwgInNlc3Npb25JZCI6ICJBUUlDNXdNMkxZNFNmY3l0ZEZiaXI5R3MtcUs2b0tjQWJaelVyRUhZZ0FhQWNBZy4qQUFKVFNRQUNNREVBQWxOTEFCTTFPVEl6TVRFd056STBPRGsxT0RFeU56a3kqIiwgInJlYWxtIjogImRjPW9wZW5hbSxkYz1mb3JnZXJvY2ssZGM9b3JnIiB9.RwlcQDHpJGD9cgaJODCGo_U7Oxch_a2nb1gQL_n4-xU\", \"template\": \"\", \"stage\": \"DataStore1\", \"callbacks\": [ { \"type\": \"NameCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" User Name: \" } ], \"input\": [ { \"name\": \"IDToken1\", \"value\": \"\" } ] }, { \"type\": \"PasswordCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" Password: \" } ], \"input\": [ { \"name\": \"IDToken2\", \"value\": \"\" } ] } ] }"
            ]
        );
    
        server.respondWith(
            "POST",   
            "/openam/json/authenticate",
            [
                200, 
                { 
                    "Date": "Thu, 22 Aug 2013 16:39:47 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=100",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"tokenId\": \"AQIC5wM2LY4SfcwAvg9WHSj8Zbbb2gh-_HCXHPAMUwGU1PU.*AAJTSQACMDEAAlNLABM0MDUzNDkyNDIxOTE3NDg2NTI2*\" }"
            ]
        );
    
        server.respondWith(
            "POST",   
            "/openam/json/users/?_action=idFromSession",
            [
                200, 
                { 
                    "Date": "Thu, 22 Aug 2013 16:39:47 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=99",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"id\":\"demo\",\"realm\":\"/\",\"dn\":\"id=demo,ou=user,dc=openam,dc=forgerock,dc=org\"}"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openam/json//users/demo",
            [
                200, 
                { 
                    "Date": "Thu, 22 Aug 2013 16:39:47 GMT",
                    "Server": "Apache-Coyote/1.1",
                    "ETag": "&quot;0&quot;",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json;charset=UTF-8",
                    "Cache-Control": "no-cache",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=98"
                },
                "{\"name\":\"demo\",\"realm\":\"/\",\"uid\":[\"demo\"],\"userpassword\":[\"{SSHA}xXzKPfc91C3x3DDyW1ZEVHzrLM5y9p4DjP/glA==\"],\"sn\":[\"demo\"],\"cn\":[\"demo\"],\"iplanet-am-user-auth-config\":[\"ldapService\"],\"inetuserstatus\":[\"Active\"],\"objectclass\":[\"devicePrintProfilesContainer\",\"person\",\"sunIdentityServerLibertyPPService\",\"inetorgperson\",\"sunFederationManagerDataStore\",\"iPlanetPreferences\",\"iplanet-am-auth-configuration-service\",\"organizationalperson\",\"sunFMSAML2NameIdentifier\",\"inetuser\",\"forgerock-am-dashboard-service\",\"iplanet-am-managed-person\",\"iplanet-am-user-service\",\"sunAMAuthAccountLockout\",\"top\"],\"universalid\":[\"id=demo,ou=user,dc=openam,dc=forgerock,dc=org\"]}"
            ]
        );
    
        server.respondWith(
            "GET",   
            "templates/user/DefaultBaseTemplate.html",
            [
                200, 
                { 
                    "Date": "Thu, 22 Aug 2013 16:39:47 GMT",
                    "Last-Modified": "Sat, 27 Jul 2013 00:46:59 GMT",
                    "Server": "Apache/2.2.22 (Unix) DAV/2 PHP/5.3.15 with Suhosin-Patch mod_ssl/2.2.22 OpenSSL/0.9.8x",
                    "ETag": "&quot;82c40a-430-4e27398c4e6c0&quot;",
                    "Content-Type": "text/html",
                    "Connection": "Keep-Alive",
                    "Accept-Ranges": "bytes",
                    "Keep-Alive": "timeout=5, max=97",
                    "Content-Length": "1072"
                },
                "\n<div id=\"container\" class=\"container\">\n    <div id=\"header\" class=\"container\">\n        <div id=\"logo\" class=\"float-left\">\n            <a href=\"#\" title=\"{{theme.settings.logo.title}}\"><img src=\"{{theme.settings.logo.src}}\" width=\"{{theme.settings.logo.width}}\" height=\"{{theme.settings.logo.height}}\" alt=\"{{theme.settings.logo.alt}}\" /> </a>\n        </div>\n        <div id=\"user-nav\" class=\"float-right\">\n            <div id=\"loginContent\">\n                <span id=\"user_name\"></span>\n                <span class=\"hr-vertical\">|</span>\n                <a href=\"#profile/\" id=\"profile_link\">{{t \"common.user.profile\"}}</a>\n                <span class=\"\">|</span>\n                <a href=\"#profile/change_security_data/\" id=\"security_link\">{{t \"templates.user.UserProfileTemplate.changeSecurityData\"}}</a>\n                <span class=\"\">|</span>\n                <a href=\"#logout/\" id=\"logout_link\">{{t \"common.form.logout\"}}</a>\n            </div>\n        </div> \n    </div>\n\n    <div id=\"menu\" class=\"menubar\"></div>\n\n    <div id=\"content\" class=\"content\"></div>\n</div>\n"
            ]
        );
    
        server.respondWith(
            "GET",   
            "templates/openam/UserProfileTemplate.html",
            [
                200, 
                { 
                    "Date": "Thu, 22 Aug 2013 16:39:47 GMT",
                    "Last-Modified": "Thu, 11 Jul 2013 20:46:49 GMT",
                    "Server": "Apache/2.2.22 (Unix) DAV/2 PHP/5.3.15 with Suhosin-Patch mod_ssl/2.2.22 OpenSSL/0.9.8x",
                    "ETag": "&quot;78312f-11b6-4e1427e3f2040&quot;",
                    "Content-Type": "text/html",
                    "Connection": "Keep-Alive",
                    "Accept-Ranges": "bytes",
                    "Keep-Alive": "timeout=5, max=99",
                    "Content-Length": "4534"
                },
                "<div class=\"content-bg\">\n\n    <h3>{{t \"common.user.userProfile\"}}</h3>\n\n    <form class=\"form two-columns\" id=\"UserProfileForm\">\n        <input type=\"hidden\" name=\"dn\">\n        <div class=\"column-layout\">\n            <div class=\"column2\">\n                <div class=\"field\">\n                    <label class=\"light\">{{t \"common.user.username\"}}</label>\n                    <input type=\"text\" name=\"name\"  data-validator=\"required\" readonly/>\n                    <span></span>\n                    <div class=\"validation-message\"></div>\n                </div>\n                \n                <div class=\"field\">\n                    <label class=\"light\">{{t \"common.user.givenName\"}}</label>\n                    <input type=\"text\" name=\"givenname\" data-validator=\"required\" />\n                    <span></span>\n                    <div class=\"validation-message\"></div>\n                </div>\n                \n                <div class=\"field\">\n                    <label class=\"light\">{{t \"common.user.emailAddress\"}}</label> \n                    <input type=\"text\" name=\"mail\" /> \n                    <span></span>\n                    <div class=\"validation-message\"></div>\n                </div>\n            \n                <div class=\"field\">\n                    <label class=\"light\">{{t \"common.user.address1\"}}</label>\n                    <input type=\"text\" name=\"postaladdress\" />\n                    <span></span>\n                    <div class=\"validation-message\"></div>\n                </div>\n<!--                 \n                <div class=\"field\">\n                    <label class=\"light\">{{t \"common.user.country\"}}</label>\n                    <select name=\"country\"></select>\n                    <span></span>\n                    <div class=\"validation-message\"></div>\n                </div>\n                \n                <div class=\"field\">\n                    <label class=\"light\">{{t \"common.user.stateProvince\"}}</label>\n                    <select name=\"stateProvince\"></select>\n                    <span></span>\n                    <div class=\"validation-message\"></div>\n                </div>\n                \n -->\n            </div>\n            <div class=\"column2\">\n                <div class=\"field\">\n                    <label class=\"light\"></label> \n                    <a href=\"{{url 'changeSecurityData'}}\" id=\"securityDialogLink\" class=\"ice hrefInput\">\n                        {{t \"templates.user.UserProfileTemplate.changeSecurityData\"}}\n                    </a>\n                </div>\n                \n                <div class=\"field\">\n                    <label class=\"light\">{{t \"common.user.familyName\"}}</label>\n                    <input type=\"text\" name=\"sn\" data-validator=\"required\" />\n                    <span></span>\n                    <div class=\"validation-message\"></div>\n                </div>\n                \n                <div class=\"field\">\n                    <label class=\"light\">{{t \"common.user.phoneNumber\"}}</label>\n                    <input type=\"text\" name=\"telephonenumber\" data-validator=\"required\" />\n                    <span></span>\n                    <div class=\"validation-message\"></div>\n                </div>\n                <!-- \n                <div class=\"field\">\n                    <label class=\"light\">{{t \"common.user.address2\"}}</label>\n                    <input type=\"text\" name=\"address2\" />\n                    <span></span>\n                    <div class=\"validation-message\"></div>\n                </div>\n                \n                <div class=\"field\">\n                    <label class=\"light\">{{t \"common.user.city\"}}</label>\n                    <input type=\"text\" name=\"city\" />\n                    <span></span>\n                    <div class=\"validation-message\"></div>\n                </div>\n                \n                <div class=\"field\">\n                    <label class=\"light\">{{t \"common.user.postalCode\"}}</label>\n                    <input type=\"text\" name=\"postalCode\" />\n                    <span></span>\n                    <div class=\"validation-message\"></div>\n                </div>\n                  -->\n                <div class=\"field\" style=\"margin-left: 240px; margin-top: 30px; height: auto\">\n                    <input type=\"submit\" name=\"saveButton\" value=\"{{t \"common.form.update\"}}\" class=\"button active\" />\n                    <input type=\"button\" name=\"resetButton\" value=\"{{t \"common.form.reset\"}}\" class=\"button gray\"/>\n                </div>\n            </div>\n        </div>\n    </form>\n\n</div>\n"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openam/json/authenticate?ForceAuth=true&locale=en-US",
            [
                200, 
                { 
                    "Date": "Thu, 22 Aug 2013 16:40:04 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=100",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"authId\": \"eyAiYWxnIjogIkhTMjU2IiwgInR5cCI6ICJqd3QiIH0.eyAib3RrIjogIjNocmEwdm1qOHEwOTZibHFqNDBuc2M2ZXFyIiwgInNlc3Npb25JZCI6ICJBUUlDNXdNMkxZNFNmY3d5UjVEVFpaVVRBWkdDUDJ0R0ducXVTbjU4aDdJd3lFYy4qQUFKVFNRQUNNREVBQWxOTEFCTTJNRFV3TVRjMU1UQTBPVEEzTlRnME5ETXgqIiwgInJlYWxtIjogImRjPW9wZW5hbSxkYz1mb3JnZXJvY2ssZGM9b3JnIiB9.ExEtiFrghcnzRxkyQyNFECk5utClsXh5OEla9dBwwLs\", \"template\": \"\", \"stage\": \"DataStore1\", \"callbacks\": [ { \"type\": \"NameCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" User Name: \" } ], \"input\": [ { \"name\": \"IDToken1\", \"value\": \"\" } ] }, { \"type\": \"PasswordCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" Password: \" } ], \"input\": [ { \"name\": \"IDToken2\", \"value\": \"\" } ] } ] }"
            ]
        );

    };

});
