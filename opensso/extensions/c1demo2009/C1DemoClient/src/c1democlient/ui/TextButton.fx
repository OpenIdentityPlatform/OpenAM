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
 * $Id: TextButton.fx,v 1.2 2009/06/11 05:29:47 superpat7 Exp $
 */

package c1democlient.ui;

import javafx.scene.CustomNode;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextOrigin;

import c1democlient.Constants;

public class TextButton extends CustomNode {
    public var color: Color = Color.BLACK;
    public var highlightColor: Color = Constants.sunBlue;
    public var font: Font;
    public var content: String;

    public var action: function(): Void;

    var rect: Rectangle;
    var text: Text;

    public override var onMouseReleased = function(e: MouseEvent) {
        if ( action != null ) {
            action();
        }
    }

    public override var onMouseEntered = function(e: MouseEvent) {
        text.fill = highlightColor;
        text.underline = true;
    }

    public override var onMouseExited = function(e: MouseEvent) {
        text.fill = color;
        text.underline = false;
    }

    public override function create():Node {
        Group {
            content: [
                rect = Rectangle
                {
                    height: bind text.layoutBounds.height
                    width: bind text.layoutBounds.width
                    opacity: 0.0
                },
                text = Text
                {
                    fill: color;
                    font: font;
                    content: content;
                    underline: false;
                    textOrigin: TextOrigin.TOP;
                }
            ]
        };
    }
}
