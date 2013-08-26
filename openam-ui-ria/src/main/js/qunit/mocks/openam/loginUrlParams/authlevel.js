/*global require, define*/
define(function () {

    return function (server) {
    
        server.respondWith(
            "POST",   
            "/openam/json/users/?_action=idFromSession",
            [
                401, 
                { 
                    "Date": "Thu, 22 Aug 2013 16:12:34 GMT",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=80",
                    "Content-Length": "69",
                    "Content-Type": "application/json;charset=ISO-8859-1"
                },
                "{ \"code\": 401, \"reason\": \"Unauthorized\", \"message\": \"Access denied\" }"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openam/json/auth/1/authenticate?locale=en-US",
            [
                200, 
                { 
                    "Date": "Thu, 22 Aug 2013 16:12:34 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=77",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"authId\": \"eyAiYWxnIjogIkhTMjU2IiwgInR5cCI6ICJqd3QiIH0.eyAib3RrIjogIjNrN3A5NjIycm9pZGhua2FybzE1NzN0YnM4IiwgInNlc3Npb25JZCI6ICJBUUlDNXdNMkxZNFNmY3o1ZDhNT1JMblFEVGdBMlVjMXJHenFWZ1BtU3p2cTM0WS4qQUFKVFNRQUNNREVBQWxOTEFCTTNOamMxTmpBeU9EUTJPREEzTlRVek5ERTIqIiwgInJlYWxtIjogImRjPW9wZW5hbSxkYz1mb3JnZXJvY2ssZGM9b3JnIiB9.nBvcqRDmOBtO8817kKqQFLxgNjceiFGkgFNgeh-iuS8\", \"template\": \"\", \"stage\": \"DataStore1\", \"callbacks\": [ { \"type\": \"NameCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" User Name: \" } ], \"input\": [ { \"name\": \"IDToken1\", \"value\": \"\" } ] }, { \"type\": \"PasswordCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" Password: \" } ], \"input\": [ { \"name\": \"IDToken2\", \"value\": \"\" } ] } ] }"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openam/json/auth/1/authenticate?authlevel=2&authIndexType=level&authIndexValue=2&locale=en-US",
            [
                400, 
                { 
                    "Date": "Thu, 22 Aug 2013 16:12:43 GMT",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "close",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"errorMessage\": \"com.sun.identity.authentication.spi.AuthLoginException: No Configuration found|noConfig.jsp\" }"
            ]
        );
    
        server.respondWith(
            "GET",   
            "templates/openam/RESTLoginUnavailableTemplate.html",
            [
                200, 
                { 
                    "Date": "Thu, 22 Aug 2013 16:12:43 GMT",
                    "Last-Modified": "Sat, 27 Jul 2013 00:48:57 GMT",
                    "Server": "Apache/2.2.22 (Unix) DAV/2 PHP/5.3.15 with Suhosin-Patch mod_ssl/2.2.22 OpenSSL/0.9.8x",
                    "ETag": "&quot;807cec-fd-4e2739fcd7040&quot;",
                    "Content-Type": "text/html",
                    "Connection": "Keep-Alive",
                    "Accept-Ranges": "bytes",
                    "Keep-Alive": "timeout=5, max=99",
                    "Content-Length": "253"
                },
                "<div class=\"container-shadow\" id=\"login-container\">\n    <div class=\"column-layout\" style=\"padding-top: 30px;\">\n        <div>\n            <h1 id=\"header\" class=\"align-center\">{{t \"openam.authentication.unavailable\"}}</h1>\n        </div>\n    </div>\n</div>"
            ]
        );

    };

});
