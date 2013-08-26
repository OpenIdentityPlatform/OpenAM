/*global require, define*/
define(function () {

    return function (server) {
    
        server.respondWith(
            "GET",   
            "/openam/json/auth/1/authenticate?goto=%23gotoTest&locale=en-US",
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
            "POST",   
            "/openam/json/auth/1/authenticate",
            [
                200, 
                { 
                    "Date": "Fri, 16 Aug 2013 22:55:03 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=100",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"tokenId\": \"AQIC5wM2LY4SfcyTKdB7lrZSjEqfBhMM5aebDKaHVnZaMjU.*AAJTSQACMDEAAlNLABMzMTg3Nzg5ODg5NjA5Njg0ODI4*\" }"
            ]
        );
    
        server.respondWith(
            "POST",   
            "/openam/json/auth/1/authenticate?",
            [
                200, 
                { 
                    "Date": "Fri, 16 Aug 2013 22:55:03 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=100",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"tokenId\": \"AQIC5wM2LY4SfcyTKdB7lrZSjEqfBhMM5aebDKaHVnZaMjU.*AAJTSQACMDEAAlNLABMzMTg3Nzg5ODg5NjA5Njg0ODI4*\" }"
            ]
        );
    
        server.respondWith(
            "POST",   
            "/openam/json/users/?_action=idFromSession",
            [
                200, 
                { 
                    "Date": "Fri, 16 Aug 2013 22:55:03 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=99",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"id\":\"demo\",\"realm\":\"/\",\"dn\":\"id=demo,ou=user,dc=huck,dc=forgerock,dc=com\"}"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openam/json//users/demo",
            [
                200, 
                { 
                    "Date": "Fri, 16 Aug 2013 22:55:03 GMT",
                    "Server": "Apache-Coyote/1.1",
                    "ETag": "&quot;0&quot;",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json;charset=UTF-8",
                    "Cache-Control": "no-cache",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=98"
                },
                "{\"name\":\"demo\",\"realm\":\"/\",\"uid\":[\"demo\"],\"userpassword\":[\"{SSHA}xXzKPfc91C3x3DDyW1ZEVHzrLM5y9p4DjP/glA==\"],\"sn\":[\"demo\"],\"cn\":[\"demo\"],\"inetuserstatus\":[\"Active\"],\"objectclass\":[\"devicePrintProfilesContainer\",\"person\",\"sunIdentityServerLibertyPPService\",\"inetorgperson\",\"sunFederationManagerDataStore\",\"iPlanetPreferences\",\"iplanet-am-auth-configuration-service\",\"organizationalperson\",\"sunFMSAML2NameIdentifier\",\"inetuser\",\"forgerock-am-dashboard-service\",\"iplanet-am-managed-person\",\"iplanet-am-user-service\",\"sunAMAuthAccountLockout\",\"top\"],\"universalid\":[\"id=demo,ou=user,dc=openam,dc=forgerock,dc=org\"]}"
            ]
        );

    };

});
