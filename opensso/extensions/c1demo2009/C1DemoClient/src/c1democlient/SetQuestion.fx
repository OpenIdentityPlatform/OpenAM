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
 * $Id: SetQuestion.fx,v 1.2 2009/06/11 05:29:43 superpat7 Exp $
 */

package c1democlient;

import c1democlient.Constants;
import c1democlient.Main;
import c1democlient.parser.OtpPullParser;
import c1democlient.SetPassword;
import java.lang.Exception;
import java.net.URLEncoder;
import javafx.ext.swing.SwingButton;
import javafx.io.http.HttpRequest;
import javafx.scene.control.TextBox;
import javafx.scene.CustomNode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextOrigin;

public function doSetQuestion(otp: String): Void {
    Main.goto(SetQuestion {
        otp: otp
    });
}

public class SetQuestion extends CustomNode {
    public var otp: String;

    var questionPromptText: Text;
    var questionTextBox: TextBox;
    var answerPromptText: Text;
    var answerTextBox: TextBox;

    public override function create():Node {
        id = "Authentication";

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
                    content: "Please select a secondary authentication "
                        "question for your family account. You "
                        "should select a question that your whole "
                        "family is easily able to answer, but other "
                        "are not."
                }
                HBox {
                    content: [
                        questionPromptText = Text {
                            font: Constants.arialRegular12
                            fill: Color.BLACK
                            content: "Question:"
                            textOrigin: TextOrigin.TOP
                            translateY: bind (questionTextBox.boundsInLocal.height - questionPromptText.boundsInLocal.height) / 2;
                        },
                        questionTextBox = TextBox {
                            columns: 18
                        }
                    ]
                    spacing: 5
                }
                HBox {
                    content: [
                        answerPromptText = Text {
                            font: Constants.arialRegular12
                            fill: Color.BLACK
                            content: "Answer:"
                            textOrigin: TextOrigin.TOP
                            translateY: bind (answerTextBox.boundsInLocal.height - answerPromptText.boundsInLocal.height) / 2;
                        },
                        answerTextBox = TextBox {
                            columns: 18
                            translateX: 8 // NASTY - but it will do for now
                            action: submitQuestion
                        }
                    ]
                    spacing: 5
                }
                SwingButton {
                    text: "Proceed"
                    action: submitQuestion
                }
            ]
            spacing: 5
        }
    }

    function submitQuestion(): Void {
        println("submitQuestion {questionTextBox.text} {answerTextBox.text}");

        var param: String = "action=setQuestion&content=<setQuestion><otp>{otp}</otp><question>{URLEncoder.encode(questionTextBox.text)}</question><answer>{URLEncoder.encode(answerTextBox.text)}</answer></setQuestion>";

        var request: HttpRequest = HttpRequest {
            location: "http://{Constants.host}:{Constants.port}/{Constants.contextRoot}/resources/challenges/{Main.phoneNumber}/"
            method: HttpRequest.POST;

            onException: function(exception: Exception) {
                println("Error: {exception}");
            }
            onResponseCode: function(responseCode:Integer) {
                println("{responseCode} from {request.location}");
                if (responseCode != 200) {
                    ErrorMessage.doError("Error submitting question and answer", function(): Void {
                        doSetQuestion(otp);
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
                        onDone: function( otp:String ) {
                            println("otp = {otp}");
                            SetPassword.doSetPassword(otp);
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
