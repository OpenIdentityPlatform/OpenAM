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
 * $Id: Main.fx,v 1.2 2009/06/11 05:29:42 superpat7 Exp $
 */

package c1democlient;

import c1democlient.DemoPane;
import c1democlient.DemoScene;
import c1democlient.Login;
import c1democlient.model.Account;
import c1democlient.model.Phone;
import java.lang.Object;
import javafx.lang.FX;
import javafx.scene.Node;
import javafx.stage.Stage;

 // Application Bounds
public var stageWidth: Number = 240;
public var stageHeight: Number = 340;

// In this sample, we obtain the phone number from a command line argument. In
// reality we would read it from configuration or some API.
public var phoneNumber: String = FX.getArgument("phoneNumber").toString();

public var accountPhone: String;

public var account: Account;
public var phone: Phone;

// OAuth stuff
public var consumerKey: String = "http://localhost:8080/TokenService/resources/oauth/v1/consumer/javafx";
public var consumerSecret: String = "javafx_secret";
public var consumerName: String = "javafx";
public var token: String = null;
public var tokenSecret: String = null;
public var tokenExpires: Long = 0;

public var tokenService: String = "http://localhost:8080/TokenService/resources/oauth/v1/";
public var oauthRegistrationUrl: String = "{Main.tokenService}consumer_registration";
public var oauthRequestUrl: String = "{tokenService}get_request_token";
public var oauthAuthUrl: String = "{tokenService}NoBrowserAuthorization";
public var oauthAccessUrl: String = "{tokenService}get_access_token";

var demoScene: DemoScene;

public def stage: Stage = Stage {
    title: "MyAccount"
    width: stageWidth
    height: stageHeight
    scene: demoScene = DemoScene {
        demoPane: DemoPane {
        }
    }
}

public function goto(node: Node): Void
{
    demoScene.demoPane.currentPane = node;
}

function run() {
    Login.doLogin();
}