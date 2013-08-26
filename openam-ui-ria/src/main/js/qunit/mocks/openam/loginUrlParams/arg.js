/*global require, define*/
define(function () {

    return function (server) {
    
        server.respondWith(
            "GET",   
            "/openam/json/auth/1/authenticate?arg=newsession&locale=en-US",
            [
                200, 
                { 
                    "Date": "Fri, 16 Aug 2013 22:54:38 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=75",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"authId\": \"eyAiYWxnIjogIkhTMjU2IiwgInR5cCI6ICJqd3QiIH0.eyAib3RrIjogImt1dHI5bWt0MHBrYnVpdThtdTA4bXJjdThkIiwgInNlc3Npb25JZCI6ICJBUUlDNXdNMkxZNFNmY3dTdVBDcTlDdTMwa1FRMzIxVVJleXBXb3RvQU9lUFBpSS4qQUFKVFNRQUNNREVBQWxOTEFCTXRORFF3T1RRd01Ea3pOakkyTWpnd01ESTAqIiwgInJlYWxtIjogImRjPW9wZW5hbSxkYz1mb3JnZXJvY2ssZGM9b3JnIiB9.VcRRbEn2Rf7esykfIsag4TQi5mgxsf0xL3k3f-dJm3Q\", \"template\": \"\", \"stage\": \"DataStore1\", \"callbacks\": [ { \"type\": \"NameCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" User Name: \" } ], \"input\": [ { \"name\": \"IDToken1\", \"value\": \"\" } ] }, { \"type\": \"PasswordCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" Password: \" } ], \"input\": [ { \"name\": \"IDToken2\", \"value\": \"\" } ] } ] }"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openam/json/auth/1/authenticate?",
            [
                200, 
                { 
                    "Date": "Fri, 16 Aug 2013 22:54:38 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=75",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"authId\": \"eyAiYWxnIjogIkhTMjU2IiwgInR5cCI6ICJqd3QiIH0.eyAib3RrIjogImt1dHI5bWt0MHBrYnVpdThtdTA4bXJjdThkIiwgInNlc3Npb25JZCI6ICJBUUlDNXdNMkxZNFNmY3dTdVBDcTlDdTMwa1FRMzIxVVJleXBXb3RvQU9lUFBpSS4qQUFKVFNRQUNNREVBQWxOTEFCTXRORFF3T1RRd01Ea3pOakkyTWpnd01ESTAqIiwgInJlYWxtIjogImRjPW9wZW5hbSxkYz1mb3JnZXJvY2ssZGM9b3JnIiB9.VcRRbEn2Rf7esykfIsag4TQi5mgxsf0xL3k3f-dJm3Q\", \"template\": \"\", \"stage\": \"DataStore1\", \"callbacks\": [ { \"type\": \"NameCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" User Name: \" } ], \"input\": [ { \"name\": \"IDToken1\", \"value\": \"\" } ] }, { \"type\": \"PasswordCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" Password: \" } ], \"input\": [ { \"name\": \"IDToken2\", \"value\": \"\" } ] } ] }"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openam/json/auth/1/authenticate?arg=newsession",
            [
                200, 
                { 
                    "Date": "Fri, 16 Aug 2013 22:54:38 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=75",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"authId\": \"eyAiYWxnIjogIkhTMjU2IiwgInR5cCI6ICJqd3QiIH0.eyAib3RrIjogImt1dHI5bWt0MHBrYnVpdThtdTA4bXJjdThkIiwgInNlc3Npb25JZCI6ICJBUUlDNXdNMkxZNFNmY3dTdVBDcTlDdTMwa1FRMzIxVVJleXBXb3RvQU9lUFBpSS4qQUFKVFNRQUNNREVBQWxOTEFCTXRORFF3T1RRd01Ea3pOakkyTWpnd01ESTAqIiwgInJlYWxtIjogImRjPW9wZW5hbSxkYz1mb3JnZXJvY2ssZGM9b3JnIiB9.VcRRbEn2Rf7esykfIsag4TQi5mgxsf0xL3k3f-dJm3Q\", \"template\": \"\", \"stage\": \"DataStore1\", \"callbacks\": [ { \"type\": \"NameCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" User Name: \" } ], \"input\": [ { \"name\": \"IDToken1\", \"value\": \"\" } ] }, { \"type\": \"PasswordCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" Password: \" } ], \"input\": [ { \"name\": \"IDToken2\", \"value\": \"\" } ] } ] }"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openam/json/auth/1/authenticate?arg=newsession&authIndexType=module&authIndexValue=Login Url Param Tests",
            [
                200, 
                { 
                    "Date": "Fri, 16 Aug 2013 22:54:38 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=75",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"authId\": \"eyAiYWxnIjogIkhTMjU2IiwgInR5cCI6ICJqd3QiIH0.eyAib3RrIjogImt1dHI5bWt0MHBrYnVpdThtdTA4bXJjdThkIiwgInNlc3Npb25JZCI6ICJBUUlDNXdNMkxZNFNmY3dTdVBDcTlDdTMwa1FRMzIxVVJleXBXb3RvQU9lUFBpSS4qQUFKVFNRQUNNREVBQWxOTEFCTXRORFF3T1RRd01Ea3pOakkyTWpnd01ESTAqIiwgInJlYWxtIjogImRjPW9wZW5hbSxkYz1mb3JnZXJvY2ssZGM9b3JnIiB9.VcRRbEn2Rf7esykfIsag4TQi5mgxsf0xL3k3f-dJm3Q\", \"template\": \"\", \"stage\": \"DataStore1\", \"callbacks\": [ { \"type\": \"NameCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" User Name: \" } ], \"input\": [ { \"name\": \"IDToken1\", \"value\": \"\" } ] }, { \"type\": \"PasswordCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" Password: \" } ], \"input\": [ { \"name\": \"IDToken2\", \"value\": \"\" } ] } ] }"
            ]
        );

    };

});
