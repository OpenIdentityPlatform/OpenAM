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
 * $Id: Statement.fx,v 1.2 2009/06/11 05:29:43 superpat7 Exp $
 */

package c1democlient;

import c1democlient.Constants;
import c1democlient.Home;
import c1democlient.Main;
import c1democlient.model.Phone;
import c1democlient.oauth.OauthRequest;
import c1democlient.parser.PhonesPullParser;
import c1democlient.ui.TableNode;
import c1democlient.ui.TextButton;
import java.lang.Exception;
import javafx.io.http.HttpRequest;
import javafx.scene.CustomNode;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextOrigin;

public function doStatement(): Void {
    loadPhones(Main.account.accountNumber);
}

function loadPhones(accountNumber:String): Void {
    println("Call Phones Restful Web Service via OAuth...");

    var oauthRequest: OauthRequest = OauthRequest {
        consumerKey: Main.consumerKey;
        consumerSecret: Main.consumerSecret;
        token: Main.token;
        tokenSecret: Main.tokenSecret;
        tokenExpires: Main.tokenExpires;
        signatureMethod: OauthRequest.HMACSHA1;

        request: HttpRequest {
            location: "http://{Constants.host}:{Constants.port}/{Constants.contextRoot}/resources/accounts/{accountNumber}/phoneCollection/"
            method: HttpRequest.GET

            onException: function(exception: Exception) {
                println("Error: {exception}");
            }
            onResponseCode: function(responseCode:Integer) {
                println("{responseCode} from {oauthRequest.request.location}");
                if (responseCode != 200) {
                    ErrorMessage.doError("Error loading phone details", Home.doHome );
                }
            }
            onInput: function(input: java.io.InputStream) {
                var tableContent: Node[];
                var phones: Phone[];

                try {
                    var parser = PhonesPullParser {
                        onDone: function( data:Phone[] ) {
                            phones = data;
                        }
                    };
                    parser.parse(input);
                    for ( phone in phones ) {
                        var textBox = TextButton {
                            font: Constants.arialBold12
                            content: phone.userName.split(" ")[0];
                            action: function(): Void {
                                PhoneStatement.doPhoneStatement(phone, doStatement);
                            }
                        }
                        var number = Text {
                            content: phone.phoneNumber;
                            textOrigin: TextOrigin.TOP;
                        }
                        var cost = Text {
                            content: "$23.45"
                            textOrigin: TextOrigin.TOP;
                            textAlignment: TextAlignment.RIGHT
                        }

                        insert textBox into tableContent;
                        insert number into tableContent;
                        insert cost into tableContent;
                    }
                    Main.goto(Statement {
                        tableContent: tableContent
                    });
                } finally {
                    input.close();
                }
            }
        }
    }

    oauthRequest.start();
}

public class Statement extends CustomNode {
    var tableContent: Node[];

    public override function create():Node {
        id = "Statement";

        VBox {
            content: [
                Text {
                    font: Constants.arialRegular12
                    content: Main.accountPhone
                }
                TableNode {
                    height: 200
                    rowHeight: 25
                    rowSpacing: 0
                    columnWidths: [50, 100, 60]
                    tableFill: Color.WHITE
                    evenRowFill: Color.WHITE
                    oddRowFill: Color.LIGHTBLUE
                    selectedRowFill: Color.WHITE
                    selectedIndex: -1
                    content: tableContent
                }
                TextButton {
                    font: Constants.arialBold16
                    content: "Back"
                    action: Home.doHome
                }
            ]
            spacing: 10
        }
    }
}
