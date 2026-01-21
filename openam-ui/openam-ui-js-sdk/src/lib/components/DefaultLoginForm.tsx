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

import type { LoginForm } from "./types";
import { getConfig } from "../config";

const DefaultLoginForm: LoginForm = ({ authData, setCallbackValue, doLogin }) => {

    const config = getConfig();

    const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const submitEvent = e.nativeEvent as SubmitEvent;
        const submitter = submitEvent.submitter;

        if (submitter instanceof HTMLButtonElement || submitter instanceof HTMLInputElement) {
            doLogin(submitter.value);
        } else {
            doLogin('');
        }
        
    }

    return <div>
        <h2>{authData.header}</h2>
        <form onSubmit={handleSubmit}>
            {authData.callbacks.filter((cb) => cb.type !== 'ConfirmationCallback').map((cb, i) => {
                const id = `callback_${i}`;
                return <div key={id} className="form-group">
                    <config.CallbackElement callback={cb} setCallbackValue={(val) => setCallbackValue(i, val)} />
                </div>
            })}
            <div className="button-group">
                <config.ActionElements callbacks={authData.callbacks} />
            </div>
        </form>
    </div>
}

export default DefaultLoginForm