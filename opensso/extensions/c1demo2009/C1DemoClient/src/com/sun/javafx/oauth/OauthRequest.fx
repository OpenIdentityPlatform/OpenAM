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
 * $Id: OauthRequest.fx,v 1.2 2009/06/11 05:29:46 superpat7 Exp $
 */

package com.sun.javafx.oauth;

import java.lang.Exception;
import java.lang.Long;
import java.lang.StringBuffer;
import java.io.InputStream;
import javafx.data.Pair;
import javafx.io.http.HttpRequest;
import javafx.io.http.URLConverter;

import com.sun.javafx.io.http.impl.Profile;
import com.sun.javafx.io.http.impl.DefaultProfile;

import org.bouncycastle.crypto.HMACSigner;

public def PLAINTEXT: String = "PLAINTEXT";
public def HMACSHA1: String = "HMAC-SHA1";

public def OAUTH_VERSION: String = "1.0";

public class OauthRequest {
    public-init var consumerKey: String;
    public-init var consumerSecret: String;

    public-init var requestUrl: String;
    public-init var authUrl: String;
    public-init var accessUrl: String;

    public-init var signatureMethod: String;

    public-init var request: HttpRequest;
    public-init var urlParameters: Pair[];

    public-init var token: String;
    public-init var tokenSecret: String;
    public-init var tokenExpires: Long;

    public var VERBOSE: Boolean = true;

    var requestRequest: HttpRequest = HttpRequest {
        onInput: function(is: InputStream) {
            parseResponse(is);
        }

        onResponseCode: function(code: Integer) {
            debugPrint("CODE:{code}");
        }

        onResponseMessage: function(msg: String) {
            debugPrint("MESG:{msg}");
        }

        onException: function(e: Exception) {
            e.printStackTrace();
        }

        onDone: function() {
            debugPrint("request request DONE");
            authRequest.location = buildAuthURL(authUrl);
            authRequest.start();
            // FIXME to keep the script from exiting
            java.lang.Thread.currentThread().sleep(1000);
        }
    }

    // FIXME Cannot authenicate as of Feb 2009 because we
    // have no browser component to render the authenication webpage
    // currently sent by the Yahoo! Oauth server.
    var authRequest: HttpRequest = HttpRequest {
        onInput: function(is: InputStream) {
            is.close();
        }

        onResponseCode: function(code: Integer) {
            debugPrint("CODE:{code}");
        }

        onResponseMessage: function(msg: String) {
            debugPrint("MESG:{msg}");
        }

        onException: function(e: Exception) {
            e.printStackTrace();
        }

        onDone: function() {
            debugPrint("AuthRequest DONE");
            accessRequest.location = buildRequestURL(accessRequest.method, accessUrl);
            accessRequest.start();
            // FIXME to keep the script from exiting
            java.lang.Thread.currentThread().sleep(1000);
        }
    }

    var accessRequest: HttpRequest = HttpRequest {        
        onInput: function(is: InputStream) {
            parseResponse(is);
        }

        onResponseCode: function(code: Integer) {
            debugPrint("CODE:{code}");
        }

        onResponseMessage: function(msg: String) {
            debugPrint("MESG:{msg}");
        }

        onException: function(e: Exception) {
            e.printStackTrace();
        }

        onDone: function() {
            debugPrint("access request DONE");
            debugPrint("Starting user request");
            request.location = buildRequestURL(request.method, request.location);
            request.start();
        }
    }

    public function start() {
        // Check if signatureMethod is supported
        if (signatureMethod != PLAINTEXT and signatureMethod != HMACSHA1) {
            throw
            new java.lang.IllegalArgumentException("Unsupported signature method: {signatureMethod}");
        }

        // Check if token exist and if it has expired or not if so
        // refresh token or just connect with the current token.
        // else get a new token
        if (token != null and tokenSecret != null) {
            if (tokenExpires.longValue() >= currentTimeInSeconds().longValue()) {
                accessRequest.location = buildRequestURL(accessRequest.method, accessUrl);
                accessRequest.start();
            } else {
                request.location = buildRequestURL(request.method, request.location);
                request.start();
            }
        } else {
            requestRequest.location = buildRequestURL(requestRequest.method, requestUrl);
            requestRequest.start();
        }
        // FIXME to keep the script from exiting
        java.lang.Thread.currentThread().sleep(1000);
    }

    function buildRequestURL(method: String, url: String): String {
        var params: Pair[];
        var time = currentTimeInSeconds();
        def signatureKey: String = "{consumerSecret}&{tokenSecret}";
        
        insert
        Pair {
            name: "oauth_consumer_key";
            value: consumerKey;
        } into params;

        insert
        Pair {
            name: "oauth_nonce";
            value: Long.toString(time.longValue() * 1000);
        } into params;

        insert
        Pair {
            name: "oauth_signature_method";
            value: signatureMethod;
        } into params;
        
        if (signatureMethod == PLAINTEXT) {
            insert
            Pair {
                name: "oauth_signature";
                value: signatureKey;
            } into params;
        }

        insert
        Pair {
            name: "oauth_timestamp";
            value: Long.toString(time);
        } into params;

        if (url == accessUrl or url == request.location) {
            insert
            Pair {
                name: "oauth_token";
                value: token;
            } into params;
        }
        
        insert
        Pair {
            name: "oauth_version";
            value: OAUTH_VERSION;
        } into params;

        if (url == request.location and urlParameters != null) {
            params = sortParameters(params);
        }

        def encoder: URLConverter = URLConverter {};
        if (signatureMethod == HMACSHA1) {
            def profile: Profile = DefaultProfile.load();
            def key: String = signatureKey;
            def text: String = "{method}&{profile.encodeURL(url)}&{profile.encodeURL(encoder.encodeParameters(params))}";
            def signature = sign(key, text);
            insert
            Pair {
                name: "oauth_signature";
                value: signature;
            } into params;
        }

        var str: String = "{url}?{encoder.encodeParameters(params)}";
        debugPrint("REQUESTURL: {str}");
        return str;
    }

    function buildAuthURL(url: String): String {
        var params: Pair[];

        insert
        Pair {
            name: "oauth_token";
            value: token;
        } into params;
        var s: String = "{url}?{URLConverter{}.encodeParameters(params)}";
        debugPrint("AUTHURL:{s}");
        return s;
    }

    function parseResponse(is: InputStream) {
        try {
            var i: Integer;
            var sb = new StringBuffer();
            while (

            (i = is.read()) != - 1) {
                sb.append(i as Character);
            }
            debugPrint("RESPONSE: {sb}");
            for (p in URLConverter{}.decodeParameters(sb.toString())) {
                debugPrint("REQUEST RESPONSE: {p}");
                if (p.name.equals("oauth_token")) {
                    token = p.value;
                } else if (p.name.equals("oauth_token_secret")) {
                    tokenSecret = p.value;
                } else if (p.name.equals("oauth_expires_in")) {
                    tokenExpires = Long.parseLong(p.value) + currentTimeInSeconds().longValue();
                }
            }
        } catch (e: Exception) {
            e.printStackTrace();
        } finally {
            is.close();
        }
    }

    function sortParameters(param: Pair[]): Pair[] {
        var pp: Pair[] = param;
        for (u in urlParameters) {
            var i = 0;
            for (p in pp) {
                if ((p.name.compareTo(u.name)) > 0) {
                    break;
                }
                i++;
            }
            insert u before pp[i];
        }
        return pp;
    }

    function currentTimeInSeconds(): Long {
        return java.lang.System.currentTimeMillis() / 1000;
    }

    function sign(key: String, text: String): String {
        return HMACSigner.sign(key.getBytes(), text.getBytes());
    }

    function debugPrint(str: String): Void {
        if (VERBOSE) {
            println(str);
        }
    }
}

