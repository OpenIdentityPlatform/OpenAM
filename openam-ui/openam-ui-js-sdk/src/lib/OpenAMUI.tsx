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
import type { AuthError, SuccessfulAuth, UserAuthData } from "./types";
import { UserService } from "./userService";
import { LoginService } from "./loginService";
import Login from "./Login";
import { getConfig } from "./config";

const OpenAMUI: React.FC = () => {

    const config = getConfig();
    const loginService = new LoginService(config.getOpenAmUrl());
    const userService = new UserService(config.getOpenAmUrl());

    const [userAuthData, setUserAuthData] = useState<UserAuthData | null>(null);

    const [error, setError] = useState<AuthError | null>(null);

    useEffect(() => {
        if (error) {
            return;
        }
        const initAuth = async () => {
            const userData = await userService.getUserIdFromSession()
            setUserAuthData(userData);
        }
        initAuth();
    }, [error])

    const doRedirect = (url: string) => {
        const absoluteUrlPattern = /^(?:[a-z+]+:)?\/\//i;
        if(absoluteUrlPattern.test(url)) {
            window.location.href = url;
        } else {
            window.location.href = config.openamServer.concat(url)    
        }
    }

    const successfullAuthHandler = async (successfulAuth : SuccessfulAuth) => {
        if(config.redirectOnSuccessfulLogin){
            doRedirect(successfulAuth.successUrl);
            return;
        }
        const userData = await userService.getUserIdFromSession()
        setUserAuthData(userData);
    }

    const errorAuthHandler = (authError: AuthError) => {
        setError(authError);
    }

    if(error) {
        return <config.errorForm error={error} resetError={() => setError(null)} />;
    }
    if (userAuthData && userAuthData.id) {
        if(config.redirectOnSuccessfulLogin) {
            doRedirect(userAuthData.successURL);
            return;
        }
        return <config.userForm userAuthData={userAuthData} userService={userService} errorAuthHandler={errorAuthHandler}  />;
    }

    return <Login loginService={loginService} successfulAuthHandler={successfullAuthHandler} errorAuthHandler={errorAuthHandler} />;
};

export default OpenAMUI;