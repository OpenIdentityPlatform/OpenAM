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
 * $Id: Login.fx,v 1.2 2009/06/11 05:29:43 superpat7 Exp $
 */

package c1democlient;

import c1democlient.Constants;
import c1democlient.ErrorMessage;
import c1democlient.Main;
import c1democlient.model.Account;
import c1democlient.model.Challenge;
import c1democlient.model.Phone;
import c1democlient.oauth.OauthRequest;
import c1democlient.parser.AccountPullParser;
import c1democlient.parser.ChallengePullParser;
import c1democlient.parser.PhonePullParser;
import c1democlient.PasswordResetQuestion;
import c1democlient.ui.SwingPasswordField;
import c1democlient.ui.TextButton;
import java.lang.Exception;
import javafx.ext.swing.SwingButton;
import javafx.io.http.HttpRequest;
import javafx.scene.CustomNode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextOrigin;

public function doLogin(): Void {
    Main.goto(Login{});
}

public function loadPhone(phoneNumber:String, password:String): Void {
    println("Call Phone Restful Web Service via OAuth...");

    var oauthRequest: OauthRequest = OauthRequest {
        consumerKey: Main.consumerKey;
        consumerSecret: Main.consumerSecret;
        requestUrl: Main.oauthRequestUrl;
        authUrl: Main.oauthAuthUrl;
        accessUrl: Main.oauthAccessUrl;
        username: phoneNumber;
        password: password;
        signatureMethod: OauthRequest.HMACSHA1;

        request: HttpRequest {
            location: "http://{Constants.host}:{Constants.port}/{Constants.contextRoot}/resources/phones/{phoneNumber}/"
            method: HttpRequest.GET

            onException: function(exception: Exception) {
                println("Error: {exception}");
            }
            onResponseCode: function(responseCode:Integer) {
                println("{responseCode} from {oauthRequest.request.location}");
                if (responseCode != 200) {
                    ErrorMessage.doError("Error logging in", doLogin );
                }
            }
            onInput: function(input: java.io.InputStream) {
                try {
                    var parser = PhonePullParser {
                        onDone: function( data:Phone ) {
                            // Success - set context for future requests
                            Main.phone = data;
                            Main.token = oauthRequest.token;
                            Main.tokenSecret = oauthRequest.tokenSecret;
                            if ( oauthRequest.tokenExpires != null ) {
                                Main.tokenExpires = oauthRequest.tokenExpires;
                            }
                            
                            // Now account...
                            loadAccount(Main.phoneNumber);
                        }
                    };
                    parser.parse(input);
                } finally {
                    input.close();
                }
            }
        }
    }

    oauthRequest.start();
}

function loadAccount(phoneNumber:String) {
    println("Call Account Restful Web Service via OAuth...");

    var oauthRequest: OauthRequest = OauthRequest {
        consumerKey: Main.consumerKey;
        consumerSecret: Main.consumerSecret;
        token: Main.token;
        tokenSecret: Main.tokenSecret;
        tokenExpires: Main.tokenExpires;
        signatureMethod: OauthRequest.HMACSHA1;

        request: HttpRequest {
            location: "http://{Constants.host}:{Constants.port}/{Constants.contextRoot}/resources/phones/{phoneNumber}/accountNumber/"
            method: HttpRequest.GET

            onException: function(exception: Exception) {
                println("Exception loading account: {exception.getMessage()}");
            }
            onResponseCode: function(responseCode:Integer) {
                println("{responseCode} from {oauthRequest.request.location}");
                if (responseCode != 200) {
                    // We can't access the account - no problem - just show phone functions
                    Main.account = null;
                    Main.accountPhone = "Phone number: {Main.phoneNumber}";
                    Home.doHome();
                }
            }
            onInput: function(input: java.io.InputStream) {
                try {
                    var parser = AccountPullParser {
                        onDone: function( data:Account ) {
                            // If we get here then we're able to access the account - hurrah!
                            Main.account = data;
                            Main.accountPhone = "Account number: {Main.account.accountNumber}";
                            Home.doHome();
                        }
                    };
                    parser.parse(input);
                } finally {
                    input.close();
                }
            }
        }
    }

    oauthRequest.start();
}

function resetPassword(phoneNumber:String)
{
    println("Call Challenge Restful Web Service...");

    // Submit HttpRequest
    var request: HttpRequest = HttpRequest {
        location: "http://{Constants.host}:{Constants.port}/{Constants.contextRoot}/resources/challenges/{phoneNumber}/"
        method: HttpRequest.GET

        onException: function(exception: Exception) {
            println("Error: {exception}");
        }
        onResponseCode: function(responseCode:Integer) {
            println("{responseCode} from {request.location}");
            if (responseCode != 200) {
                ErrorMessage.doError("Error getting challenge question", doLogin );
            }
        }
        onInput: function(input: java.io.InputStream) {
            try {
                var parser = ChallengePullParser {
                    onDone: function( data:Challenge ) {
                        if (sizeof data.challengeQuestions == 1) {
                            // Ask them the reset question
                            PasswordResetQuestion.doPasswordResetQuestion(data.challengeQuestions[0]);
                        } else {
                            // Ask them the credit card questions
                            VerifyCreditCard.doCreditCard();
                        }
                    }
                };
                parser.parse(input);
            } finally {
                input.close();
            }
        }
    }

    request.start();
}


public class Login extends CustomNode {
    var passwordPromptText: Text;
    var passwordTextBox: SwingPasswordField;

    public override function create():Node {
        id = "MyAccount Login";
        var action = function(): Void {
            loadPhone(Main.phoneNumber, passwordTextBox.text);
        };

        VBox {
            content: [
                Text {
                    font: Constants.arialRegular12
                    content: "Phone number: {Main.phoneNumber}"
                }
                HBox {
                    content: [
                        passwordPromptText = Text {
                            font: Constants.arialRegular12
                            fill: Color.BLACK
                            content: "Password:"
                            textOrigin: TextOrigin.TOP
                            translateY: bind (passwordTextBox.boundsInLocal.height - passwordPromptText.boundsInLocal.height) / 2;
                        },
                        passwordTextBox = SwingPasswordField {
                            columns: 5
                            action: action
                        }
                    ]
                    spacing: 5
                },
                SwingButton {
                    text: "Login"
                    action: action
                },
                TextButton {
                    font: Constants.arialRegular12
                    content: "Set/Reset Password"
                    action: function(): Void {
                        resetPassword(Main.phoneNumber);
                    }
                }
            ]
            spacing: 10
        }
    }
}