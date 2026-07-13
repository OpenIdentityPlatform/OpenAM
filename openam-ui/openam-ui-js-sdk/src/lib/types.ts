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

export interface CallbackOutput {
  name: string;
  value: number | string | string[];
}

export interface CallbackInput {
  name: string;
  value: number | string | string[];
}

export interface Callback {
  type: 'NameCallback' | 'PasswordCallback' | 'ConfirmationCallback' | 'TextOutputCallback';
  output: CallbackOutput[];
  input: CallbackInput[];
}

export interface AuthData {
  authId: string;
  template: string;
  stage: string;
  header: string;
  infoText: string[];
  callbacks: Callback[];
}

export interface SuccessfulAuth {
  tokenId: string;
  successUrl: string;
  realm: string;
}

export interface AuthError {
  code: number
  reason: string;
  message: string;
}

export interface UserAuthData {
  id: string;
  realm: string;
  dn: string;
  successURL: string;
  fullLoginURL: string;
}

export interface UserData {
  username: string;
  realm: string;
  uid: string[];
  universalid: string[];
  oath2faEnabled: string[];
  objectClass: string[];
  inetUserStatus: string[];
  dn: string[];
  sn: string[];
  cn: string[];
  createTimestamp: string[];
  modifyTimestamp: string[];
  roles: string[];
  givenName?: string[];
  mail?: string[];
  telephoneNumber?: string[];
}

export type AuthResponse = AuthData | SuccessfulAuth | AuthError;
