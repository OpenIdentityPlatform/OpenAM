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
 * $Id: PhoneSettings.fx,v 1.2 2009/06/11 05:29:43 superpat7 Exp $
 */

package c1democlient;

import c1democlient.Constants;
import c1democlient.Main;
import c1democlient.oauth.OauthRequest;
import c1democlient.model.Phone;
import c1democlient.parser.PhonePullParser;
import c1democlient.ui.TextButton;
import java.lang.Exception;
import javafx.data.Pair;
import javafx.ext.swing.SwingCheckBox;
import javafx.io.http.HttpRequest;
import javafx.scene.CustomNode;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public function doPhoneSettings(phone:Phone, back: function(): Void): Void {
    println("Call Phone Restful Web Service via OAuth...");

    var oauthRequest: OauthRequest = OauthRequest {
        consumerKey: Main.consumerKey;
        consumerSecret: Main.consumerSecret;
        token: Main.token;
        tokenSecret: Main.tokenSecret;
        tokenExpires: Main.tokenExpires;
        signatureMethod: OauthRequest.HMACSHA1;

        request: HttpRequest {
            location: "http://{Constants.host}:{Constants.port}/{Constants.contextRoot}/resources/phones/{phone.phoneNumber}/"
            method: HttpRequest.GET

            onException: function(exception: Exception) {
                println("Error: {exception}");
            }
            onResponseCode: function(responseCode:Integer) {
                println("{responseCode} from {oauthRequest.request.location}");
                if (responseCode != 200) {
                    ErrorMessage.doError("Error loading phone settings", function(): Void {
                        doPhoneSettings(phone, back);
                    } );
                }
            }
            onInput: function(input: java.io.InputStream) {
                try {
                    var p: Phone;
                    var parser = PhonePullParser {
                        onDone: function( data:Phone ) {
                            p = data;
                        }
                    };
                    parser.parse(input);
                    Main.goto( PhoneSettings {
                        phone: p
                        back: back
                    });
                } finally {
                    input.close();
                }
            }
        }
    }

    oauthRequest.start();
}



public class PhoneSettings extends CustomNode {
    public var phone: Phone;
    public var back: function(): Void;
    // dirty is true if the user has clicked any checkboxes -> we need to write
    // data back
    public var dirty: Boolean = false;

    public override function create():Node {
        id = "Settings for {phone.phoneNumber}";

        var vbox = VBox {
            spacing: 5
            content: [
                Text {
                    font: Constants.arialRegular12
                    content: Main.accountPhone
                }
                Text {
                    font: Constants.arialBold16
                    content: "Purchasing"
                }
                SwingCheckBox {
                    selected: bind phone.canDownloadRingtones with inverse
                    text: "Allow Ringtones"
                    disable: ( Main.account == null )
                    foreground: if ( Main.account != null ) then Color.BLACK else Color.GRAY
                    action: function(): Void { dirty = true; }
                }
                SwingCheckBox {
                    selected: bind phone.canDownloadMusic with inverse
                    text: "Allow Music"
                    disable: ( Main.account == null )
                    foreground: if ( Main.account != null ) then Color.BLACK else Color.GRAY
                    action: function(): Void { dirty = true; }
                }
                SwingCheckBox {
                    selected: bind phone.canDownloadVideo with inverse
                    text: "Allow Video"
                    disable: ( Main.account == null )
                    foreground: if ( Main.account != null ) then Color.BLACK else Color.GRAY
                    action: function(): Void { dirty = true; }
                }
            ]
        }

        if ( Main.account != null ) {
            insert Text {
                    font: Constants.arialRegular12
                    content: "Phone number: {phone.phoneNumber}"
            } after vbox.content[1];
            insert Text {
                    font: Constants.arialBold16
                    content: "Administrative Functions"
            } into vbox.content;
            insert SwingCheckBox {
                    selected: bind phone.headOfHousehold with inverse
                    text: "Head of Household"
                    action: function(): Void { dirty = true; }
            } into vbox.content;
        }

        insert TextButton {
            font: Constants.arialBold16
            content: "Back"
            action: function(): Void {
                if ( dirty ) {
                    saveSettings();
                } else {
                    back();
                }

            }
        } into vbox.content;
        
        return vbox;
    }

    function saveSettings(): Void {
        println("saveSettings {phone.phoneNumber} via OAuth");

        //var param: String = "action=patch&content={phone.marshall()}";

        var params: Pair[];
        insert Pair {
            name: "action";
            value: "patch";
        } into params;
        insert Pair {
            name: "content";
            value: phone.marshall();
        } into params;

        var oauthRequest: OauthRequest = OauthRequest {
            consumerKey: Main.consumerKey;
            consumerSecret: Main.consumerSecret;
            token: Main.token;
            tokenSecret: Main.tokenSecret;
            tokenExpires: Main.tokenExpires;
            signatureMethod: OauthRequest.HMACSHA1;
            urlParameters: params;

            request: HttpRequest {
                location: "http://{Constants.host}:{Constants.port}/{Constants.contextRoot}/resources/phones/{phone.phoneNumber}/"
                method: HttpRequest.POST;

                onException: function(exception: Exception) {
                    println("Error: {exception}");
                }
                onResponseCode: function(responseCode:Integer) {
                    println("{responseCode} from {oauthRequest.request.location}");
                    if (responseCode == 204) { // No content
                        back();
                    } else {
                        ErrorMessage.doError("Error saving phone settings", function(): Void {
                            Main.goto(this);
                        } );
                    }
                }
                onOutput: function(output: java.io.OutputStream) {
                    try {
                        //println("Writing {param} to {oauthRequest.request.location}");
                        //output.write(param.getBytes());
                    } finally {
                        output.close();
                    }
                }
            }
        }

        oauthRequest.request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        //oauthRequest.request.setHeader("Content-Length", "{param.length()}");
        oauthRequest.request.setHeader("Content-Length", "0");

        oauthRequest.start();
    }
}
