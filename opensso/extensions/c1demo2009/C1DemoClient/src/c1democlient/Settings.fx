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
 * $Id: Settings.fx,v 1.2 2009/06/11 05:29:43 superpat7 Exp $
 */

package c1democlient;

import c1democlient.Constants;
import c1democlient.Home;
import c1democlient.Main;
import c1democlient.ui.TextButton;
import javafx.ext.swing.SwingButton;
import javafx.scene.CustomNode;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

def titleFont: Font = Constants.arialBold16;
def subtitleFont: Font = Constants.arialRegular12;

public function doSettings(): Void {
    if ( Main.account == null ) {
        ErrorMessage.doError("Error loading account settings", Home.doHome );
    } else {
        Main.goto(Settings{});
    }
}

public class Settings extends CustomNode {
    public override function create():Node {
        id = "Settings";

        VBox {
            content: [
                Text {
                    font: Constants.arialRegular12
                    content: Main.accountPhone
                }
                VBox {
                    content: [
                        Text {
                            font: titleFont
                            content: "Billing Address"
                        }
                        Text {
                            font: subtitleFont
                            content: "{Main.account.addressLine1}\n"
                            "{Main.account.addressCity}, {Main.account.addressState} {Main.account.addressZip}"
                            wrappingWidth: 210
                        }
                    ]
                }
                SwingButton {
                    text: "Change Billing Address"
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
