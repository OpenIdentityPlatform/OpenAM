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
            "/openam/json/authenticate?realm=%2Ftest%2F&locale=en-US",
            [
                200, 
                { 
                    "Date": "Mon, 19 Aug 2013 21:02:58 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=76",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"authId\": \"eyAiYWxnIjogIkhTMjU2IiwgInR5cCI6ICJqd3QiIH0.eyAib3RrIjogImY0cmtzMDlpMWNvYmc5cWoyczB1Y3A2cGFiIiwgInNlc3Npb25JZCI6ICJBUUlDNXdNMkxZNFNmY3hEUWg0Q0xFbWw0WTh5dG9lQU1wUkV1djE0alNLQWY4Yy4qQUFKVFNRQUNNREVBQWxOTEFCTTFNREF5TWpjek5EQXhNVFk1TXpnMk56VTEqIiwgInJlYWxtIjogIm89dGVzdCxvdT1zZXJ2aWNlcyxkYz1vcGVuYW0sZGM9Zm9yZ2Vyb2NrLGRjPW9yZyIgfQ.GuxEw1053ODhyg67BcZAKVDwjGoyUvxaw5cZjy-5e5I\", \"template\": \"\", \"stage\": \"DataStore1\", \"callbacks\": [ { \"type\": \"NameCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" User Name: \" } ], \"input\": [ { \"name\": \"IDToken1\", \"value\": \"\" } ] }, { \"type\": \"PasswordCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" Password: \" } ], \"input\": [ { \"name\": \"IDToken2\", \"value\": \"\" } ] } ] }"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openam/json/authenticate?realm=%2Ftest%2F&realm=%2Ftest%2F&locale=en-US",
            [
                200, 
                { 
                    "Date": "Mon, 19 Aug 2013 21:02:58 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=76",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"authId\": \"eyAiYWxnIjogIkhTMjU2IiwgInR5cCI6ICJqd3QiIH0.eyAib3RrIjogImY0cmtzMDlpMWNvYmc5cWoyczB1Y3A2cGFiIiwgInNlc3Npb25JZCI6ICJBUUlDNXdNMkxZNFNmY3hEUWg0Q0xFbWw0WTh5dG9lQU1wUkV1djE0alNLQWY4Yy4qQUFKVFNRQUNNREVBQWxOTEFCTTFNREF5TWpjek5EQXhNVFk1TXpnMk56VTEqIiwgInJlYWxtIjogIm89dGVzdCxvdT1zZXJ2aWNlcyxkYz1vcGVuYW0sZGM9Zm9yZ2Vyb2NrLGRjPW9yZyIgfQ.GuxEw1053ODhyg67BcZAKVDwjGoyUvxaw5cZjy-5e5I\", \"template\": \"\", \"stage\": \"DataStore1\", \"callbacks\": [ { \"type\": \"NameCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" User Name: \" } ], \"input\": [ { \"name\": \"IDToken1\", \"value\": \"\" } ] }, { \"type\": \"PasswordCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" Password: \" } ], \"input\": [ { \"name\": \"IDToken2\", \"value\": \"\" } ] } ] }"
            ]
        );

    };

});
