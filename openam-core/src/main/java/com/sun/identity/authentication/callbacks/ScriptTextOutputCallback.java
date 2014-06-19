/*
* The contents of this file are subject to the terms of the Common Development and
* Distribution License (the License). You may not use this file except in compliance with the
* License.
*
* You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
* specific language governing permission and limitations under the License.
*
* When distributing Covered Software, include this CDDL Header Notice in each file and include
* the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
* Header, with the fields enclosed by brackets [] replaced by your own identifying
* information: "Portions copyright [year] [name of copyright owner]".
*
* Copyright 2014 ForgeRock AS.
*/
package com.sun.identity.authentication.callbacks;

import javax.security.auth.callback.TextOutputCallback;

/**
 * Adds script inclusion functionality to the {@link TextOutputCallback} so that we can
 * differentiate scripts from normal textual information types (information, warning, error).
 */
public class ScriptTextOutputCallback extends TextOutputCallback {

    //used by RESTLoginView.js
    public static final int SCRIPT = 4;

    /**
     * Constructor that "creates" it as an INFORMATION TextOutputCallback.
     *
     * @param message The script which will be inserted into the page receiving this callback
     */
    public ScriptTextOutputCallback(String message) {
        super(TextOutputCallback.INFORMATION, message);
    }

    /**
     * Overrides the {@link TextOutputCallback}'s messageType to always return that
     * we are a script.
     *
     * @return The constant indicator we are of type SCRIPT
     */
    @Override
    public int getMessageType() {
        return ScriptTextOutputCallback.SCRIPT;
    }
}
