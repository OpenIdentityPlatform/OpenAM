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

import type React from "react";
import type { AuthData, AuthError, Callback, UserData } from '../types';

// renders a login form with callbacks
export type LoginForm = React.FC<{
  authData: AuthData,
  setCallbackValue: (i: number, val: string) => void,
  doLogin: (action: string) => void
}> 

// renders a callback such as NameCallback, PasswordCallback and so on
export type CallbackElement = React.FC<{
    callback: Callback
    setCallbackValue: (val: string) => void
}>

// renders a user profile form
export type UserForm = React.FC<{
  userData: UserData,
  setUserData: (userData: UserData) => void
  saveHandler: () => void
}>

// renders an authentication error form
export type ErrorForm = React.FC<{
    error: AuthError,
    resetError: () => void
}>

// renders submit buttons; if there are no ConfirmationCallback in the callbacks array, renders the default button
export type ActionElements = React.FC<{callbacks: Callback[]}>
