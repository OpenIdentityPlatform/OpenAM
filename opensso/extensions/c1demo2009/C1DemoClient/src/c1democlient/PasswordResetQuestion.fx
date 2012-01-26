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
 * $Id: PasswordResetQuestion.fx,v 1.2 2009/06/11 05:29:43 superpat7 Exp $
 */

package c1democlient;

import c1democlient.Constants;
import c1democlient.Main;
import c1democlient.parser.OtpPullParser;
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

public function doPasswordResetQuestion(question: String): Void {
    Main.goto(PasswordResetQuestion {
        question: question
    });
}

public class PasswordResetQuestion extends CustomNode {
    public var question: String;
    public var answer: String;

    var questionText: Text;
    var answerTextBox: SwingPasswordField;

    public override function create():Node {
        id = "Authentication";

        VBox {
            content: [
                Text {
                    font: Constants.arialRegular12
                    content: Main.accountPhone
                }
                HBox {
                    content: [
                        questionText = Text {
                            font: Constants.arialRegular12
                            fill: Color.BLACK
                            content: question
                            textOrigin: TextOrigin.TOP
                            translateY: bind (answerTextBox.boundsInLocal.height - questionText.boundsInLocal.height) / 2;
                        }
                        answerTextBox = SwingPasswordField {
                            columns: 5
                            action: submitAnswer
                        }
                    ]
                    spacing: 5
                }
                SwingButton {
                    text: "Proceed"
                    action: submitAnswer
                }
            ]
            spacing: 10
        }
    }

    function submitAnswer(): Void {
        println("submitAnswer {answerTextBox.text}");

        var param: String = "action=auth2&content=<auth2><answer>{answerTextBox.text}</answer></auth2>";

        var request: HttpRequest = HttpRequest {
            location: "http://{Constants.host}:{Constants.port}/{Constants.contextRoot}/resources/challenges/{Main.phoneNumber}/"
            method: HttpRequest.POST;

            onException: function(exception: Exception) {
                println("Error: {exception}");
            }
            onResponseCode: function(responseCode:Integer) {
                println("{responseCode} from {request.location}");
                if (responseCode != 200) {
                    ErrorMessage.doError("Error verifying answer", function(): Void {
                        Main.goto(this);
                    });
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
            onInput: function(input: java.io.InputStream) {
                try {
                    var parser = OtpPullParser {
                        onDone: function( data:String ) {
                            println("otp = {data}");
                            SetPassword.doSetPassword(data);
                        }
                    };
                    parser.parse(input);
                } finally {
                    input.close();
                }
            }
        }

        request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setHeader("Content-Length", "{param.length()}");

        request.start();
    }
}
