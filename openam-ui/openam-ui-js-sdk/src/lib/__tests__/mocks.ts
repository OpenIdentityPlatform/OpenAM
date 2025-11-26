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
import type { AuthData, AuthError, SuccessfulAuth, UserAuthData, UserData } from "../types"

const authDataJSON = `{
  "authId": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJvdGsiOiJsa21mODI5dHEzbmhraDNyNmVsbGZtYWpybCIsInJlYWxtIjoiZGM9b3BlbmFtLGRjPW9wZW5pZGVudGl0eXBsYXRmb3JtLGRjPW9yZyIsInNlc3Npb25JZCI6IkFRSUM1d00yTFk0U2ZjekloNTRQLTZ1czRod0tSa09ibWFKa251U0p3SUxNYi1VLipBQUpUU1FBQ01ERUFBbE5MQUJNMk56VTVOVEF5T1RrNU5UUXpOemM0T1RZNEFBSlRNUUFBKiJ9.0lYgF063co7bcg_-xbabvrZponm7NMq3s-IeYPaf9Js",
  "template": "",
  "stage": "DataStore1",
  "header": "Sign in to OpenAM",
  "infoText": [
    "",
    ""
  ],
  "callbacks": [
    {
      "type": "NameCallback",
      "output": [
        {
          "name": "prompt",
          "value": "User Name:"
        }
      ],
      "input": [
        {
          "name": "IDToken1",
          "value": "demo"
        }
      ]
    },
    {
      "type": "PasswordCallback",
      "output": [
        {
          "name": "prompt",
          "value": "Password:"
        }
      ],
      "input": [
        {
          "name": "IDToken2",
          "value": "changeit"
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
            "Register device",
            "Skip this step"
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
          "value": "1"
        }
      ]
    }
  ]
}`

const successfulAuthJSON = `{
    "tokenId": "AQIC5wM2LY4SfcwIaAQY6dwlk4xEQjX9v59vw3gRzpGwfTI.*AAJTSQACMDEAAlNLABM2NDI1MzUyMDYwODgwODYyNzkyAAJTMQAA*",
    "successUrl": "/openam/console",
    "realm": "/"
}`


const authErrorJSON = `{"code":401,"reason":"Unauthorized","message":"Authentication Failed"}`

const userAuthDataJSON = `{
    "id": "demo",
    "realm": "/",
    "dn": "id=demo,ou=user,dc=openam,dc=openidentityplatform,dc=org",
    "successURL": "/openam/console",
    "fullLoginURL": "/openam/UI/Login?realm=%2F"
}`

const userDataJSON = `{
    "username": "demo",
    "realm": "/",
    "uid": [
        "demo"
    ],
    "universalid": [
        "id=demo,ou=user,dc=openam,dc=openidentityplatform,dc=org"
    ],
    "oath2faEnabled": [
        "1"
    ],
    "objectClass": [
        "iplanet-am-managed-person",
        "inetuser",
        "sunFederationManagerDataStore",
        "sunFMSAML2NameIdentifier",
        "devicePrintProfilesContainer",
        "inetorgperson",
        "sunIdentityServerLibertyPPService",
        "iPlanetPreferences",
        "pushDeviceProfilesContainer",
        "iplanet-am-user-service",
        "forgerock-am-dashboard-service",
        "organizationalperson",
        "top",
        "kbaInfoContainer",
        "sunAMAuthAccountLockout",
        "person",
        "oathDeviceProfilesContainer",
        "iplanet-am-auth-configuration-service"
    ],
    "inetUserStatus": [
        "Active"
    ],
    "dn": [
        "uid=demo,ou=people,dc=openam,dc=openidentityplatform,dc=org"
    ],
    "sn": [
        "John"
    ],
    "cn": [
        "John Doe"
    ],
    "createTimestamp": [
        "20250805142017Z"
    ],
    "modifyTimestamp": [
        "20250925124445Z"
    ],
    "roles": [
        "ui-self-service-user"
    ]
}`

export const mockAuthData = JSON.parse(authDataJSON) as AuthData
export const mockSuccessfulAuth = JSON.parse(successfulAuthJSON) as SuccessfulAuth
export const mockAuthError = JSON.parse(authErrorJSON) as AuthError

export const mockUserAuthData = JSON.parse(userAuthDataJSON) as UserAuthData
export const mockUserData = JSON.parse(userDataJSON) as UserData