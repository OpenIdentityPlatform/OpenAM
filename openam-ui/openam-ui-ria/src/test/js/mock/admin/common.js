/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

/*global define*/
define("mock/admin/common", [
], function () {
    return function (server) {
        server.respondWith(
            "POST",
            /\/json\/users\?_action=idFromSession$/,
            [
                401,
                { },
                JSON.stringify(
                    {
                        "code": 401,
                        "reason": "Unauthorized",
                        "message": "Access Denied"
                    }
                )
            ]
        );

        server.respondWith(
            "POST",
            /\/json\/authenticate\?/,
            [
                200,
                { },
                JSON.stringify(
                    {"authId": "eyAidHlwIjogIkpXVCIsICJhbGciOiAiSFMyNTYiIH0.eyAib3RrIjogImNjMTJrNzBtNjdjbmljNW8xYjg4cjJlNGlsIiwgInJlYWxtIjogImRjPW9wZW5hbSxkYz1mb3JnZXJvY2ssZGM9b3JnIiwgInNlc3Npb25JZCI6ICJBUUlDNXdNMkxZNFNmY3o0YVJFNGYxM1VjRzhkcjd5Q0JnN0pXVHZkNTJFQl9LVS4qQUFKVFNRQUNNREVBQWxOTEFCTXlPVGN3TnpVeE5qSTJORGczT0RVME9EWTAqIiB9.C8E6amMpAnRidFnqLmQwSDjUEYdvYqo5_SfWi9k8kOI", "template": "", "stage": "DataStore1", "header": "Sign in to OpenAM", "callbacks": [
                        {"type": "NameCallback", "output": [
                            {"name": "prompt", "value": "User Name:"}
                        ], "input": [
                            {"name": "IDToken1", "value": ""}
                        ]},
                        {"type": "PasswordCallback", "output": [
                            {"name": "prompt", "value": "Password:"}
                        ], "input": [
                            {"name": "IDToken2", "value": ""}
                        ]}
                    ]}
                )
            ]
        );
    };
});
