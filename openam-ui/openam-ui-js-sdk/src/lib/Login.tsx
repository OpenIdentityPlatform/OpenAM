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

import { useEffect, useState } from "react";
import { LoginService } from "./loginService";
import type { AuthData, AuthError, AuthResponse, SuccessfulAuth } from "./types";
import { getConfig } from "./config";

const config = getConfig();

export type LoginProps = {
    loginService: LoginService;
    successfulAuthHandler: (userData: SuccessfulAuth) => void;
    errorAuthHandler: (authError: AuthError) => void;
}

const Login: React.FC<LoginProps> = ({ loginService, successfulAuthHandler, errorAuthHandler }) => {

    function isAuthError(response: unknown): response is AuthError {
        return typeof response === 'object' && response !== null && 'code' in response && 'message' in response;
    }

    function isAuthData(response: unknown): response is AuthData {
        return typeof response === 'object' && response !== null && 'authId' in response
            && 'callbacks' in response && Array.isArray(response.callbacks);
    }

    function isSuccessfulAuth(response: unknown): response is SuccessfulAuth {
        return typeof response === 'object' && response !== null && 'tokenId' in response
            && 'successUrl' in response && typeof response.tokenId === 'string' && typeof response.successUrl === 'string';
    }

    function handleAuthResponse(response: AuthResponse) {
        if (isAuthData(response)) {
            setAuthData(response)
        } else if (isAuthError(response)) {
            errorAuthHandler(response);
        } else if (isSuccessfulAuth(response)) {
            successfulAuthHandler(response);
        } else {
            console.error("Unknown response format", response);
        }
    }

    const [authData, setAuthData] = useState<AuthData | null>(null);

    useEffect(() => {
        const initAuth = async () => {
            const authResponse = await loginService.init()
            handleAuthResponse(authResponse);
        }
        initAuth();

    }, [])

    const setCallbackValue = (i: number, val: string | number) => {
        if (!authData) {
            return;
        }
        const newAuthData = loginService.setCallbackValue(i, val, authData);
        setAuthData(newAuthData);
    }

    const doLogin = async (action: string) => {
        if (!authData) {
            return
        }
        
        const newAuthData = loginService.setConfirmationActionValue(action, authData);

        const authResponse = await loginService.submitCallbacks(newAuthData)

        handleAuthResponse(authResponse);
    }

    if (authData) {
        return <config.loginForm authData={authData} setCallbackValue={setCallbackValue} doLogin={doLogin} />
    }
    return <></>
}

export default Login