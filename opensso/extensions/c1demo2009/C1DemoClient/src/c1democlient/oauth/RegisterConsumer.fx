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
 * $Id: RegisterConsumer.fx,v 1.2 2009/06/11 05:29:46 superpat7 Exp $
 */

package c1democlient.oauth;
import java.lang.Exception;
import java.lang.StringBuffer;
import javafx.io.http.HttpRequest;

public function registerConsumer(consumerKey: String, consumerSecret: String, consumerName: String, uri: String, action: function(): Void): Void {
    println("registerConsumer {consumerKey} {consumerSecret} {consumerName}");

    var param: String = "cons_key={consumerKey}&secret={consumerSecret}&name={consumerName}&signature_method=HMAC-SHA1";

    var request: HttpRequest = HttpRequest {
        location: uri
        method: HttpRequest.POST;

        onException: function(exception: Exception) {
            println("Error: {exception}");
        }
        onResponseCode: function(responseCode:Integer) {
            println("{responseCode} from {request.location}");
        }
        onOutput: function(os: java.io.OutputStream) {
            try {
                println("Writing {param} to {request.location}");
                os.write(param.getBytes());
            } finally {
                os.close();
            }
        }
        onInput: function(is: java.io.InputStream) {
            var i: Integer;
            var sb = new StringBuffer();
            while (

            (i = is.read()) != - 1) {
                sb.append(i as Character);
            }
            println("registerConsumer response: {sb}");
            is.close();
            if ( action != null ) {
                action();
            }
        }
    }

    request.setHeader("Content-Type", "application/x-www-form-urlencoded");
    request.setHeader("Content-Length", "{param.length()}");

    request.start();
}

