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

package com.sun.javafx.oauth;

import java.lang.StringBuffer;
import java.io.InputStream;

import javafx.io.http.HttpRequest;

import javafx.data.Pair;

import javafx.stage.Stage;

var request1: OauthRequest;
var request2: OauthRequest;

public function nextRequest(): Void {
    var ps: Pair[];
    insert Pair {
        name: "qwerty";
        value: "FOO";
    } into ps;
    insert Pair {
        name: "dog";
        value: "FOOD";
    } into ps;

    request2 = OauthRequest {
        consumerKey: "key";
        consumerSecret: "secret";
        signatureMethod: OauthRequest.HMACSHA1;
        urlParameters: ps;
        token: request1.token;
        tokenSecret: request1.tokenSecret;
        tokenExpires: request1.tokenExpires;

        request: HttpRequest {
            location: "http://term.ie/oauth/example/echo_api.php";
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
                println("DONE DONE DONE");
            }
        }
    }
    request2.start();
}


public function test(): Void {
    var params: Pair[];
    insert Pair {
        name: "def";
        value: "FOO";
    } into params;
    insert Pair {
        name: "_q";
        value: "FOO";
    } into params;
    insert Pair {
        name: "th_d";
        value: "FOO";
    } into params;
    insert Pair {
        name: "auth_a";
        value: "FOO";
    } into params;  
    insert Pair {
        name: "method";
        value: "FOO";
    } into params;

    request1 = OauthRequest {
        consumerKey: "key";
        consumerSecret: "secret";
        requestUrl: "http://term.ie/oauth/example/request_token.php";
        authUrl: "http://term.ie/oauth/example/request_token.php";
        accessUrl: "http://term.ie/oauth/example/access_token.php";
        signatureMethod: OauthRequest.HMACSHA1;
        urlParameters: params;

        request: HttpRequest {
            location: "http://term.ie/oauth/example/echo_api.php";
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
                nextRequest();
            }
        }               
    }
    request1.start();    
}
