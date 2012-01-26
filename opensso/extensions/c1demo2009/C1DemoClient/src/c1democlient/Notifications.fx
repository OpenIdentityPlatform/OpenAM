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
 * $Id: Notifications.fx,v 1.2 2009/06/11 05:29:42 superpat7 Exp $
 */

package c1democlient;

import c1democlient.Constants;
import c1democlient.Main;
import c1democlient.model.Notification;
import c1democlient.model.Phone;
import c1democlient.oauth.OauthRequest;
import c1democlient.parser.NotificationsPullParser;
import c1democlient.ui.TableNode;
import c1democlient.ui.TextButton;
import java.lang.Exception;
import javafx.data.Pair;
import javafx.io.http.HttpRequest;
import javafx.scene.CustomNode;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public function doNotifications(phone:Phone, back: function(): Void): Void {
    println("Call Notifications Restful Web Service via OAuth...");

    var params: Pair[];
    insert Pair {
        name: "expandLevel";
        value: "2";
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
            location: "http://{Constants.host}:{Constants.port}/{Constants.contextRoot}/resources/phones/{phone.phoneNumber}/notificationCollection/"
            method: HttpRequest.GET

            onException: function(exception: Exception) {
                println("Error: {exception}");
            }
            onResponseCode: function(responseCode:Integer) {
                println("{responseCode} from {oauthRequest.request.location}");
                if (responseCode != 200) {
                    ErrorMessage.doError("Error loading notifications", back );
                }
            }
            onInput: function(input: java.io.InputStream) {
                var tableContent: Node[];
                var notifications: Notification[];

                try {
                    var parser = NotificationsPullParser {
                        onDone: function( data:Notification[] ) {
                            notifications = data;
                        }
                    };
                    parser.parse(input);
                    for ( notification in notifications ) {
                        // Cook the notification time a little - replace 'T' with a space
                        var notificationTimeStr = notification.notificationTime.replaceAll("T", " ");

                        // Now trim off the seconds and timezone
                        var lastColon = notificationTimeStr.lastIndexOf(":");
                        notificationTimeStr = notificationTimeStr.substring(0, lastColon);
                        lastColon = notificationTimeStr.lastIndexOf(":");
                        notificationTimeStr = notificationTimeStr.substring(0, lastColon);

                        var messageText: String = notification.messageText;
                        if ( messageText.length() > 16 ) {
                            messageText = "{messageText.substring(0,12)}...";
                        }

                        var notificationTime = TextButton {
                            font: Constants.arialRegular12
                            content: notificationTimeStr;
                            action: function(): Void {
                                NotificationDetail.doNotificationDetail(notificationTimeStr, notification.messageText);
                            }
                        }

                        var textBox = TextButton {
                            font: Constants.arialBold12
                            content: messageText;
                            action: function(): Void {
                                NotificationDetail.doNotificationDetail(notificationTimeStr, notification.messageText);
                            }
                        }

                        insert notificationTime into tableContent;
                        insert textBox into tableContent;
                    }

                    Main.goto(Notifications {
                        phone: phone
                        back: back
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

public class Notifications extends CustomNode {
    public var phone: Phone;
    public var tableContent: Node[];
    public var back: function(): Void;

    public override function create():Node {
        id = "Notifications";
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
                    columnWidths: [105, 110]
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
                    action: back
                }
            ]
            spacing: 10
        }
    }
}
