/*global require, define*/
define(function () {

    return function (server) {
    
        server.respondWith(
            "POST",   
            "/openam/json/users/?_action=idFromSession",
            [
                401, 
                { 
                    "Date": "Sat, 17 Aug 2013 00:06:38 GMT",
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
            "/openam/json/auth/1/authenticate?gotoOnFail=%23gotoOnFailTest&locale=en-US",
            [
                200, 
                { 
                    "Date": "Sat, 17 Aug 2013 00:06:38 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=75",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"authId\": \"eyAiYWxnIjogIkhTMjU2IiwgInR5cCI6ICJqd3QiIH0.eyAib3RrIjogIjZsNmkxczg1YW11OGNxdGNscjhydW9zNTBsIiwgInNlc3Npb25JZCI6ICJBUUlDNXdNMkxZNFNmY3dqaWFHLTMzTGVFcFFHR3o0LWFTeTNNODZaSXRDVFptRS4qQUFKVFNRQUNNREVBQWxOTEFCUXROVGt5TnpVMU9UWTNOak01TURVM05qUTJPQS4uKiIsICJyZWFsbSI6ICJkYz1vcGVuYW0sZGM9Zm9yZ2Vyb2NrLGRjPW9yZyIgfQ.VBf-HtmTrF592zce1DoZe6UlrVwrYcs57vXlN44Pf_Q\", \"template\": \"\", \"stage\": \"DataStore1\", \"callbacks\": [ { \"type\": \"NameCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" User Name: \" } ], \"input\": [ { \"name\": \"IDToken1\", \"value\": \"\" } ] }, { \"type\": \"PasswordCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" Password: \" } ], \"input\": [ { \"name\": \"IDToken2\", \"value\": \"\" } ] } ] }"
            ]
        );
    
        server.respondWith(
            "GET",   
            "templates/common/NavigationTemplate.html",
            [
                200, 
                { 
                    "Date": "Sat, 17 Aug 2013 00:06:38 GMT",
                    "Last-Modified": "Thu, 11 Jul 2013 17:53:56 GMT",
                    "Server": "Apache/2.2.22 (Unix) DAV/2 PHP/5.3.15 with Suhosin-Patch mod_ssl/2.2.22 OpenSSL/0.9.8x",
                    "ETag": "&quot;153d92-18-4e14013f7b500&quot;",
                    "Content-Type": "text/html",
                    "Connection": "Keep-Alive",
                    "Accept-Ranges": "bytes",
                    "Keep-Alive": "timeout=5, max=79",
                    "Content-Length": "24"
                },
                "<ul class=\"menu\">\n</ul>\n"
            ]
        );
    
        server.respondWith(
            "POST",   
            "/openam/json/auth/1/authenticate",
            [
                401, 
                { 
                    "Date": "Sat, 17 Aug 2013 00:06:45 GMT",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=100",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"errorMessage\": \"Invalid Password!!\" }"
            ]
        );

    };

});
