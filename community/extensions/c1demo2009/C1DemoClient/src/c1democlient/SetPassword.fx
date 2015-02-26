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
 * $Id: SetPassword.fx,v 1.2 2009/06/11 05:29:43 superpat7 Exp $
 */

package c1democlient;

import c1democlient.Constants;
import c1democlient.ErrorMessage;
import c1democlient.Login;
import c1democlient.Main;
import c1democlient.SetPassword;
import c1democlient.ui.SwingPasswordField;
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

/**
 * @author pat
 */

public function doSetPassword(otp: String)
{
    Main.goto(SetPassword{
        otp: otp
    });
}

public class SetPassword extends CustomNode {
    public var otp: String;

    var passwordPromptText: Text;
    var passwordTextBox: SwingPasswordField;
    var confirmPromptText: Text;
    var confirmTextBox: SwingPasswordField;

    public override function create():Node {
        id = "Set Password";

        VBox {
            content: [
                HBox {
                    content: [
                        Text {
                            font: Constants.arialRegular12
                            fill: Color.BLACK
                            content: "Your phone number:"
                        }
                        Text {
                            font: Constants.arialBold12
                            fill: Color.BLACK
                            content: bind Main.phoneNumber;
                        }
                    ]
                },
                Text {
                    font: Constants.arialRegular12
                    fill: Color.BLACK
                    wrappingWidth: 210
                    content: "Please enter a new password for your "
                        "account. Your password should be easy "
                        "for you to remember, but should "
                        "combine letters, numbers and "
                        "punctuation."
                }
                HBox {
                    content: [
                        passwordPromptText = Text {
                            font: Constants.arialRegular12
                            fill: Color.BLACK
                            content: "New Password:"
                            textOrigin: TextOrigin.TOP
                            translateY: bind (passwordTextBox.boundsInLocal.height - passwordPromptText.boundsInLocal.height) / 2;
                        },
                        passwordTextBox = SwingPasswordField {
                            columns: 5
                            translateX: 47 // NASTY - but it will do for now
                        }
                    ]
                    spacing: 5
                }
                HBox {
                    content: [
                        confirmPromptText = Text {
                            font: Constants.arialRegular12
                            fill: Color.BLACK
                            content: "Confirm New Password:"
                            textOrigin: TextOrigin.TOP
                            translateY: bind (confirmTextBox.boundsInLocal.height - confirmPromptText.boundsInLocal.height) / 2;
                        },
                        confirmTextBox = SwingPasswordField {
                            columns: 5
                            action: submitPassword
                        }
                    ]
                    spacing: 5
                }
                SwingButton {
                    text: "Set Password"
                    action: submitPassword
                }
            ]
            spacing: 5
        }
    }

    function submitPassword(): Void {
        println("submitPassword {passwordTextBox.text} {confirmTextBox.text}");

        if ( not passwordTextBox.text.equals(confirmTextBox.text) ) {
            ErrorMessage.doError("Passwords don't match - try again!", function(): Void {
                Main.goto( this );
            } );
            return;
        }

        var param: String = "action=setPassword&content=<setPassword><otp>{otp}</otp><password>{passwordTextBox.text}</password></setPassword>";

        var request: HttpRequest = HttpRequest {
            location: "http://{Constants.host}:{Constants.port}/{Constants.contextRoot}/resources/challenges/{Main.phoneNumber}/"
            method: HttpRequest.POST;

            onException: function(exception: Exception) {
                println("Error: {exception}");
            }
            onResponseCode: function(responseCode:Integer) {
                println("{responseCode} from {request.location}");
                if (responseCode == 204) { // No content
                    // Need to login here with new password!
                    Login.loadPhone(Main.phoneNumber, passwordTextBox.text);
                } else {
                    ErrorMessage.doError("Error setting password", function(): Void {
                        Main.goto( this );
                    } );
                }
            }
            onOutput: function(output: java.io.OutputStream) {
                try {
                    println("Writing {param} to {request.location}");
                    output.write(param.getBytes());
                } finally {
                    output.close();
                }
            }
        }

        request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setHeader("Content-Length", "{param.length()}");

        request.start();
    }
}