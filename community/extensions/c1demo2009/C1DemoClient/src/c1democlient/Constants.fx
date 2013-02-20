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
 * $Id: Constants.fx,v 1.2 2009/06/11 05:29:43 superpat7 Exp $
 */

package c1democlient;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.FontPosture;

public def sunBlue = Color.rgb(83,130,161);

public def arialRegular10: Font = Font.font("Arial", FontWeight.REGULAR, 10);
public def arialRegular12: Font = Font.font("Arial", FontWeight.REGULAR, 12);
public def arialBold12: Font = Font.font("Arial", FontWeight.BOLD, 12);
public def arialBold16: Font = Font.font("Arial", FontWeight.BOLD, 16);
public def arialBold20: Font = Font.font("Arial", FontWeight.BOLD, 20);
public def arialBoldItalic20: Font = Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 20);
public def arialBoldItalic18: Font = Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 18);

public def host = "localhost";
public def port = "8080";
public def contextRoot = "C1DemoServer";
