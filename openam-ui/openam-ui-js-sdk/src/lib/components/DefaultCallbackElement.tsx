/**
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
 * Copyright 2025 3A Systems LLC.
 */

import { useEffect } from "react";
import type { Callback } from "../types";
import type { CallbackElement } from "./types";

const ScriptElement = (scriptText: string) => {
    useEffect(() => {
        const script = document.createElement('script');
        script.innerHTML = scriptText;
        document.body.appendChild(script);
        return () => {
            if (document.body.contains(script)) {
                document.body.removeChild(script);
            }
        };
    }, []);

    return null; // This component renders nothing in the DOM
}

const DefaultCallbackElement: CallbackElement = ({ callback, setCallbackValue }) => {

    let inputId;
    if (callback.input) {
        inputId = callback.input[0].name;
    }

    const renderTextOutputCallback = (callback: Callback) => {
        const propMap = Object.fromEntries(callback.output.map((o) => [o.name, o.value]))
        const messageType = propMap['messageType']
        const message = propMap['message'] as string
        switch (messageType) {
            case "0":
            case "1":
            case "2":
                return <p>{message}</p>
            case "4":
                return ScriptElement(message);
            default:
                console.log(`unknown message type: ${messageType}`)
                return <></>;
        }
    }

    switch (callback.type) {
        case "NameCallback":
            return <input id={inputId} placeholder={callback.output[0].value as string}
                onChange={(e) => setCallbackValue(e.target.value)} type="text" name={inputId}
                value={callback.input[0].value} required={true} />
        case "PasswordCallback":
            return <input id={inputId} placeholder={callback.output[0].value as string}
                onChange={(e) => setCallbackValue(e.target.value)} type="password" name={inputId}
                value={callback.input[0].value} required={true} />
        case "TextOutputCallback":
            return renderTextOutputCallback(callback)
        default:
            return null
    }
}



export default DefaultCallbackElement;