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
 * $Id: StatementDetail.fx,v 1.2 2009/06/11 05:29:43 superpat7 Exp $
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

public function doStatementDetail( phoneNumberTo: String,
    callDurationSecs: Number, callTime: String, back: function(): Void ): Void {
    Main.goto(StatementDetail{
        phoneNumberTo: phoneNumberTo
        callDurationSecs: callDurationSecs
        callTime: callTime
        back: back
        });
}

public class StatementDetail extends CustomNode {
    public var phoneNumberTo: String;
    public var callDurationSecs: Number;
    public var callTime: String;
    public var back: function(): Void;
    public override function create():Node {
        id = "Call";

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
                            content: callTime
                        }
                    ]
                }
                HBox {
                    content: [
                        Text {
                            font: Constants.arialBold12
                            content: "Duration:"
                        }
                        Text {
                            font: Constants.arialRegular12
                            content: "{callDurationSecs} seconds"
                        }
                    ]
                }
                HBox {
                    content: [
                        Text {
                            font: Constants.arialBold12
                            content: "To:"
                        }
                        Text {
                            font: Constants.arialRegular12
                            content: phoneNumberTo
                        }
                    ]
                }
                TextButton {
                    font: titleFont
                    content: "Back"
                    action: back
                }
            ]
            spacing: 12
        }
    }
}

