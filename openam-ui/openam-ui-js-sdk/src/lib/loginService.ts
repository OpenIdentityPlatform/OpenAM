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

import type { AuthData, AuthResponse } from "./types";

class LoginService {

  private authURL: string;

  constructor(openamUrl: string) {
    this.authURL = openamUrl.concat("/json/realms/root/authenticate");
  }

  buildAuthUrl(realm: string | null, service: string | null): string {
    const params = new URLSearchParams()
    
    let authURL = this.authURL;
    if(realm) {
      params.append("realm", realm);
    }
    if(service) {
      params.append("authIndexType", "service");
      params.append("authIndexValue", service)
    }
    if(params.size) {
      authURL = authURL.concat("?").concat(params.toString())
    }
    return authURL;
  }
  
  async init(realm: string | null, service: string | null): Promise<AuthResponse> {

    const authURL = this.buildAuthUrl(realm, service)

    try {
      const response = await fetch(authURL, {
        method: "POST",
        mode: "cors",
        credentials: "include",
        headers: {
          "Content-Type": "application/json"
        },
      })
      return await response.json();
      
    } catch (e) {
      if(import.meta.env.MODE === 'development') {
        console.log("fallback to test data", e)
        return JSON.parse(authError) as AuthResponse;
      } else {
        throw e
      }
    }
  }

  async submitCallbacks(authData: AuthData, realm: string | null, service: string | null): Promise<AuthResponse> {
    const authURL = this.buildAuthUrl(realm, service)
    try {
      const response = await fetch(authURL, {
        method: "POST",
        mode: "cors",
        credentials: "include",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(authData),
      })
      return await response.json();
    } catch (e) {
      if(import.meta.env.MODE === 'development') {
        console.log("error posting data", e, JSON.stringify(authData))
        console.log("fallback to test data", e)
        return JSON.parse(successfulAuth) as AuthResponse;
      } else {
        throw e
      }
    }
  }

  setCallbackValue(index: number, value: string | number, authData: AuthData): AuthData {

    return {
      ...authData,
      callbacks: authData.callbacks.map((cb, cbIdx) =>
        index === cbIdx ?
          {
            ...cb,
            input: cb.input.map((input, inpIdx) =>
              inpIdx === 0 ? { ...input, value: value } : input
            ),
          }
          : cb
      )
    };
  }

  setConfirmationActionValue(action: string, authData: AuthData): AuthData {
    const callbacks = authData.callbacks;
    const callbackIdx = callbacks.findIndex((cb) => (cb.type === 'ConfirmationCallback'));
    if(callbackIdx < 0) {
      return authData;
    }
    const opts = callbacks[callbackIdx].output.find((o) => (o.name === 'options'))?.value as string[];
    if (!Array.isArray(opts)) {
      return authData;
    }

    const actionIdx = opts.findIndex((val) => val === action);
    if(actionIdx < 0) {
      return authData;
    }
    return this.setCallbackValue(callbackIdx, actionIdx, authData);
  }

}


const authenticatorOATHMockData = `{
    "authId": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdXRoSW5kZXhWYWx1ZSI6Im9hdGgiLCJvdGsiOiJwZnY3Zm04bzYyMzhvZ2FlcGJyOG1jcmpwcCIsImF1dGhJbmRleFR5cGUiOiJzZXJ2aWNlIiwicmVhbG0iOiJkYz1vcGVuYW0sZGM9b3BlbmlkZW50aXR5cGxhdGZvcm0sZGM9b3JnIiwic2Vzc2lvbklkIjoiQVFJQzV3TTJMWTRTZmN4TDFHSkJvOVJsWWxJZ2RHWFNsc0NGUHhnNWlDMm1Tc3MuKkFBSlRTUUFDTURFQUFsTkxBQlF0T0RVM09ETTJPVGN5TWpNMk1ETTJPVGt6TndBQ1V6RUFBQS4uKiJ9.iGjdeF08zb0pOOpmt8JiRIg0iH32KNZDqPQu7bidjx4",
    "template": "",
    "stage": "AuthenticatorOATH5",
    "header": "Register your device with OpenAM",
    "infoText": [],
    "callbacks": [
        {
            "type": "TextOutputCallback",
            "output": [
                {
                    "name": "message",
                    "value": "\\n            Scan the barcode image below with the ForgeRock Authenticator App. Once registered click the button to\\n            enter your verification code and login.\\n        "
                },
                {
                    "name": "messageType",
                    "value": "0"
                }
            ]
        },
        {
            "type": "TextOutputCallback",
            "output": [
                {
                    "name": "message",
                    "value": "console.log('hello from text output callback')"
                },
                {
                    "name": "messageType",
                    "value": "4"
                }
            ]
        },
        {
            "type": "ConfirmationCallback",
            "output": [
                {
                    "name": "prompt",
                    "value": ""
                },
                {
                    "name": "messageType",
                    "value": 0
                },
                {
                    "name": "options",
                    "value": [
                        "Login using verification code"
                    ]
                },
                {
                    "name": "optionType",
                    "value": -1
                },
                {
                    "name": "defaultOption",
                    "value": 0
                }
            ],
            "input": [
                {
                    "name": "IDToken3",
                    "value": 0
                }
            ]
        }
    ]
}`

const successfulAuth = `{
    "tokenId": "AQIC5wM2LY4SfcwIaAQY6dwlk4xEQjX9v59vw3gRzpGwfTI.*AAJTSQACMDEAAlNLABM2NDI1MzUyMDYwODgwODYyNzkyAAJTMQAA*",
    "successUrl": "/openam/console",
    "realm": "/"
}`

const authError = `{"code":401,"reason":"Unauthorized","message":"Authentication Failed"}`


export { LoginService }
