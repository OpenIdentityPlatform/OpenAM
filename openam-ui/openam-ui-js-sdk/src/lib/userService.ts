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

import type { AuthError, UserAuthData, UserData } from "./types";

class UserService {


  //http://openam.example.org:8080/openam/json/users?_action=idFromSession

  //http://openam.example.org:8080/openam/json/realms/root/users/demo

  private userUrlTemplate: string;
  private usersUrl: string;

  constructor(openamUrl: string) {
    this.userUrlTemplate = openamUrl.concat("/json/realms/{realm}/users/{userId}");
    this.usersUrl = openamUrl.concat("/json/users");
  }


  getUserIdFromSession = async (): Promise<UserAuthData | null> => {
    try {
      const response = await fetch(this.usersUrl.concat("?_action=idFromSession"), {
        method: "POST",
        mode: "cors",
        credentials: "include",
        headers: {
          "Content-Type": "application/json"
        },
      })
      if (!response.ok) {
        return null;
      }
      return await response.json();
    } catch (e) {
      if (import.meta.env.MODE === 'development') {
        console.log("error getting user id from session", e)
        console.log("fallback to demo user")
        return JSON.parse(usersSuccessfulResponse)
      } else {
        console.log("request error ocurred:", e)
      }
      return null;
    }
  }

  getUserData = async (userId: string, realm: string): Promise<UserData> => {
    try {
      const userUrl = this.getUserUrlFromTemplate(userId, realm);

      const response = await fetch(userUrl, {
        method: "GET",
        mode: "cors",
        credentials: "include",
        headers: {
          "Content-Type": "application/json"
        },
      })
      return await response.json();
    } catch (e) {
      if (import.meta.env.MODE === 'development') {
        console.log("error getting user data", e)
        console.log("fallback to demo user data")
        return JSON.parse(testUserData);
      } else {
        throw e
      }
    }
  }

  saveUserData = async (userId: string, realm: string, userData: UserData): Promise<UserData> => {
    const userUrl = this.getUserUrlFromTemplate(userId, realm);

    const dataToUpdate = {
      givenName: userData.givenName,
      sn: userData.sn,
      mail: userData.mail,
      telephoneNumber: userData.telephoneNumber
    }

    try {
      const response = await fetch(userUrl, {
        method: "PUT",
        mode: "cors",
        credentials: "include",
        headers: {
          "Accept-API-Version": "resource=2.0, protocol=1.0",
          "Content-Type": "application/json"
        },
        body: JSON.stringify(dataToUpdate),
      }
      )
      return await response.json();
    }

    catch (e) {
      if (import.meta.env.MODE === 'development') {
        console.log("error getting user data", e)
        console.log("fallback to demo user data")
        return JSON.parse(testUserData);
      } else {
        throw e
      }
    }
  }

  savePassword = async (userId: string, realm: string, password: string): Promise<void> => {
    const userUrl = this.getUserUrlFromTemplate(userId, realm);

    const dataToUpdate = {
      userPassword: password
    }
    try {
      const response = await fetch(userUrl, {
        method: "PUT",
        mode: "cors",
        credentials: "include",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(dataToUpdate),
      }
      )
      if(!response.ok) {
        const data = await response.json() as AuthError
        throw new Error(data.message)
      }
    } catch (e) {
      if (import.meta.env.MODE === 'development') {
        console.log("error saving password, assume its ok", e)
      } else {
        throw e
      }
    }
  }


  private getUserUrlFromTemplate(userId: string, realm: string): string {
    if (!realm || realm === "" || realm === "/") {
      realm = "root";
    }
    return this.userUrlTemplate.replace("{realm}", realm).replace("{userId}", userId);
  }

}



export { UserService }

const usersSuccessfulResponse = `{
    "id": "demo",
    "realm": "/",
    "dn": "id=demo,ou=user,dc=openam,dc=openidentityplatform,dc=org",
    "successURL": "/openam/console",
    "fullLoginURL": "/openam/UI/Login?realm=%2F"
}`

// const userUnauthorizedResponse = `{"code":401,"reason":"Unauthorized","message":"Access Denied"}`

const testUserData = `{
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
        "demo"
    ],
    "cn": [
        "demo"
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


