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

import { createHashRouter } from "react-router";
import NotFoundPage from "./NotFoundPage";
import { getConfig } from "./config";
import { UserService } from "./userService";
import { LoginService } from "./loginService";
import Home from "./Home";
import Login from "./Login";
import User from "./User";

const config = getConfig();
const userService = new UserService(config.getOpenAmUrl());
const loginService = new LoginService(config.getOpenAmUrl());

const router = createHashRouter([
    {
        path: '/',
        children: [
            {
                path: '/',
                element: <Home userService={userService} />
            },
            {
                path: 'login',
                element: <Login loginService={loginService} />,
            },
            {
                path: 'user',
                element: <User userService={userService} />,
            },
            {
                path: '*',
                element: <NotFoundPage />,
            },
        ],
    },
]);

export default router