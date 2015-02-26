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
 * $Id: NotificationDetail.fx,v 1.2 2009/06/11 05:29:43 superpat7 Exp $
 */

package c1democlient;

import c1democlient.Constants;
import c1democlient.Home;
import c1democlient.Main;
import c1democlient.ui.TextButton;
import javafx.scene.CustomNode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

def titleFont: Font = Constants.arialBold16;
def subtitleFont: Font = Constants.arialRegular12;

public function doNotificationDetail(notificationTime: String, messageText: String): Void {
    Main.goto(NotificationDetail{
        notificationTime: notificationTime
        messageText: messageText
        });
}

public class NotificationDetail extends CustomNode {
    public var notificationTime: String;
    public var messageText: String;
    public override function create():Node {
        id = "Notification";

        VBox {
            content: [
                Text {
                    font: Constants.arialRegular12
                    content: "Phone number: {Main.phoneNumber}";
                }
                HBox {
                    content: [
                        Text {
                            font: Constants.arialBold12
                            content: "Date/Time:"
                        }
                        Text {
                            font: Constants.arialRegular12
                            content: notificationTime
                            wrappingWidth: 210
                        }
                    ]
                }
                VBox {
                    content: [
                        Text {
                            font: Constants.arialBold12
                            content: "Content:"
                        }
                        Text {
                            font: Constants.arialRegular12
                            content: messageText
                            wrappingWidth: 210
                        }
                    ]
                }
                TextButton {
                    font: titleFont
                    content: "Back"
                    action: Home.doHome
                }
            ]
            spacing: 12
        }
    }
}

