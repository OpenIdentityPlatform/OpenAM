/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: OauthTest.fx,v 1.2 2009/06/11 05:29:46 superpat7 Exp $
 */

package c1democlient.oauth;

import c1democlient.Main;
import java.io.InputStream;
import java.lang.StringBuffer;
import javafx.data.Pair;
import javafx.io.http.HttpRequest;

public function test(): Void {
    registerConsumer(testRequest);
}

/*
 * Do this only once (ever!) - don't do it every time the app starts!
 */
public function registerConsumer(action: function(): Void ): Void {
    RegisterConsumer.registerConsumer(Main.consumerKey,
    Main.consumerSecret,
    Main.consumerName,
    Main.oauthRegistrationUrl,
    action);
}

public function testRequest(): Void {
    var request1: OauthRequest = OauthRequest {
        consumerKey: Main.consumerKey;
        consumerSecret: Main.consumerSecret;
        requestUrl: Main.oauthRequestUrl;
        authUrl: Main.oauthAuthUrl;
        accessUrl: Main.oauthAccessUrl;
        username: "1234567890";
        password: "qqq";
        signatureMethod: OauthRequest.HMACSHA1;

        request: HttpRequest {
            location: "http://localhost:8080/C1DemoServer/resources/phones/1112223333/";
            onInput: function(is: InputStream) {
                var i: Integer;
                var sb = new StringBuffer();
                while (

                (i = is.read()) != - 1) {
                    sb.append(i as Character);
                }
                println("MYRESPONSE: {sb}");
                is.close();
            }

            onResponseCode: function(code: Integer) {
                println("CODE:{code}");
            }

            onResponseMessage: function(msg: String) {
                println("MESG:{msg}");
            }

            onDone: function() {
                println("DONE");
            }
        }
    }
    request1.start();
}

