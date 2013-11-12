/*global require, define*/
define(function () {

    return function (server) {
    
        server.respondWith(
            "POST",   
            "/openam/json/users/?_action=idFromSession",
            [
                401, 
                { 
                    "Date": "Mon, 19 Aug 2013 21:02:58 GMT",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=79",
                    "Content-Length": "69",
                    "Content-Type": "application/json;charset=ISO-8859-1"
                },
                "{ \"code\": 401, \"reason\": \"Unauthorized\", \"message\": \"Access denied\" }"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openam/json/authenticate?service=ldapService&authIndexType=service&authIndexValue=ldapService&locale=en-US",
            [
                200, 
                { 
                    "Date": "Mon, 19 Aug 2013 22:11:39 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=81",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"authId\": \"eyAiYWxnIjogIkhTMjU2IiwgInR5cCI6ICJqd3QiIH0.eyAib3RrIjogImhvOTVzZ3E2NWVuMm03ZGM1N2ZibHQyNm1rIiwgImF1dGhJbmRleFZhbHVlIjogImRlbW8iLCAiYXV0aEluZGV4VHlwZSI6ICJ1c2VyIiwgInNlc3Npb25JZCI6ICJBUUlDNXdNMkxZNFNmY3llWTFmS0pyS1ROMTNUQ01CYVlZZEQySG5kellNcGNLQS4qQUFKVFNRQUNNREVBQWxOTEFCUXROakEyTnpJeE5UZ3lNalkzTURZNU5qTTFNdy4uKiIsICJyZWFsbSI6ICJkYz1vcGVuYW0sZGM9Zm9yZ2Vyb2NrLGRjPW9yZyIgfQ.eMJZWLft35hnexMuMFwUKLflEm1BV2xTl5hQWEpl_EI\", \"template\": \"\", \"stage\": \"DataStore1\", \"callbacks\": [ { \"type\": \"NameCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" User Name: \" } ], \"input\": [ { \"name\": \"IDToken1\", \"value\": \"\" } ] }, { \"type\": \"PasswordCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" Password: \" } ], \"input\": [ { \"name\": \"IDToken2\", \"value\": \"\" } ] } ] }"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openam/json/authenticate?service=ldapService&authIndexType=service&authIndexValue=ldapService",
            [
                200, 
                { 
                    "Date": "Mon, 19 Aug 2013 22:11:39 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=81",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"authId\": \"eyAiYWxnIjogIkhTMjU2IiwgInR5cCI6ICJqd3QiIH0.eyAib3RrIjogImhvOTVzZ3E2NWVuMm03ZGM1N2ZibHQyNm1rIiwgImF1dGhJbmRleFZhbHVlIjogImRlbW8iLCAiYXV0aEluZGV4VHlwZSI6ICJ1c2VyIiwgInNlc3Npb25JZCI6ICJBUUlDNXdNMkxZNFNmY3llWTFmS0pyS1ROMTNUQ01CYVlZZEQySG5kellNcGNLQS4qQUFKVFNRQUNNREVBQWxOTEFCUXROakEyTnpJeE5UZ3lNalkzTURZNU5qTTFNdy4uKiIsICJyZWFsbSI6ICJkYz1vcGVuYW0sZGM9Zm9yZ2Vyb2NrLGRjPW9yZyIgfQ.eMJZWLft35hnexMuMFwUKLflEm1BV2xTl5hQWEpl_EI\", \"template\": \"\", \"stage\": \"DataStore1\", \"callbacks\": [ { \"type\": \"NameCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" User Name: \" } ], \"input\": [ { \"name\": \"IDToken1\", \"value\": \"\" } ] }, { \"type\": \"PasswordCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" Password: \" } ], \"input\": [ { \"name\": \"IDToken2\", \"value\": \"\" } ] } ] }"
            ]
        );

    };

});
