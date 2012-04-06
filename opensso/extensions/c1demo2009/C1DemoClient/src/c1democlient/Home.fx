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
 * $Id: Home.fx,v 1.2 2009/06/11 05:29:43 superpat7 Exp $
 */

package c1democlient;

import c1democlient.Constants;
import c1democlient.Home;
import c1democlient.Login;
import c1democlient.Main;
import c1democlient.Settings;
import c1democlient.ui.TextButton;
import javafx.scene.CustomNode;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public function doHome(): Void {
    Main.goto(Home{});
}

def titleFont: Font = Constants.arialBold16;
def subtitleFont: Font = Constants.arialRegular12;

public class Home extends CustomNode {
    public override function create():Node {
        id = "MyAccount Home";

        var node: Node;

        if ( Main.account != null ) {
        //if ( true ) {
            node = VBox {
                content: [
                    Text {
                        font: Constants.arialRegular12
                        content: Main.accountPhone
                    }
                    VBox {
                        content: [
                            TextButton {
                                font: titleFont
                                content: "Statement"
                                action: Statement.doStatement
                            }
                            Text {
                                font: subtitleFont
                                content: "See your bills, call logs, etc"
                                wrappingWidth: 210
                            }
                        ]
                    }
                    VBox {
                        content: [
                            TextButton {
                                font: titleFont
                                content: "Account Settings"
                                action: Settings.doSettings
                            }
                            Text {
                                font: subtitleFont
                                content: "Control your account"
                                wrappingWidth: 210
                            }
                        ]
                    }
                    VBox {
                        content: [
                            TextButton {
                                font: titleFont
                                content: "Family Settings"
                                action: FamilySettings.doFamilySettings
                            }
                            Text {
                                font: subtitleFont
                                content: "Control all of the numbers for this account"
                                wrappingWidth: 210
                            }
                        ]
                    }
                    VBox {
                        content: [
                            TextButton {
                                font: titleFont
                                content: "Notifications"
                                action: function(): Void {
                                    Notifications.doNotifications(Main.phone, doHome)
                                };
                            }
                            Text {
                                font: subtitleFont
                                content: "Keep yourself up to date"
                                wrappingWidth: 210
                            }
                        ]
                    }
                    TextButton {
                        font: titleFont
                        content: "Logout"
                        action: Login.doLogin
                    }
                ]
                spacing: 12
            }
        } else {
            node = VBox {
                content: [
                    Text {
                        font: Constants.arialRegular12
                        content: Main.accountPhone
                    }
                    VBox {
                        content: [
                            TextButton {
                                font: titleFont
                                content: "Phone Statement"
                                action: function(): Void {
                                    PhoneStatement.doPhoneStatement(Main.phone, doHome);
                                }
                            }
                            Text {
                                font: subtitleFont
                                content: "See your bills, call logs, etc"
                                wrappingWidth: 210
                            }
                        ]
                    }
                    VBox {
                        content: [
                            TextButton {
                                font: titleFont
                                content: "Phone Settings"
                                action: function(): Void {
                                    PhoneSettings.doPhoneSettings(Main.phone, doHome);
                                }
                            }
                            Text {
                                font: subtitleFont
                                content: "Control your phone"
                                wrappingWidth: 210
                            }
                        ]
                    }
                    VBox {
                        content: [
                            TextButton {
                                font: titleFont
                                content: "Notifications"
                                action: function(): Void {
                                    Notifications.doNotifications(Main.phone, doHome)
                                };
                            }
                            Text {
                                font: subtitleFont
                                content: "Keep yourself up to date"
                                wrappingWidth: 210
                            }
                        ]
                    }
                    TextButton {
                        font: titleFont
                        content: "Logout"
                        action: Login.doLogin
                    }
                ]
                spacing: 12
            }
        }

        return node;
    }
}

