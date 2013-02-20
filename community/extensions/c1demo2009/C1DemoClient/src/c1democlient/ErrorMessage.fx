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
 * $Id: ErrorMessage.fx,v 1.2 2009/06/11 05:29:43 superpat7 Exp $
 */

package c1democlient;

import c1democlient.Constants;
import c1democlient.ui.TextButton;
import javafx.scene.CustomNode;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.text.Text;

public function doError(message: String, back: function(): Void ): Void {
    Main.goto(ErrorMessage {
        message: message
        back: back
    });
}

public class ErrorMessage extends CustomNode {
    public var message: String;
    public var back: function(): Void;

    public override function create():Node {
        id = "Error";
        VBox {
            content: [
                Text {
                    font: Constants.arialRegular12
                    content: message
                    wrappingWidth: 210
                }
                TextButton {
                    font: Constants.arialBold16
                    content: "Back"
                    action: back
                }
            ]
            spacing: 12
        }
    }
}
