/*
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
* Copyright 2026 3A Systems LLC.
*/

/*
 * This is the default OAuth2 Access Token Modification script. It is executed when a stateless
 * (JWT) OAuth2 access token or refresh token is issued, allowing additional claims to be added to,
 * or existing claims to be modified/removed from, the token payload.
 *
 * Defined variables:
 *
 * accessToken - org.forgerock.openam.oauth2.ScriptableAccessToken
 *               The token being issued. Use the following methods to modify it:
 *                   accessToken.setField(String name, Object value)  - add or override a claim
 *                   accessToken.getField(String name)                - read a claim or context value
 *                   accessToken.removeField(String name)             - remove a custom claim
 *               Read-only context values exposed via getField(...) include: "tokenName",
 *               "sub", "realm", "clientId", "scope", "grantType", "acr", "amr".
 *
 * scopes      - java.util.Set<String>
 *               The scopes granted to the token.
 *
 * identity    - com.sun.identity.idm.AMIdentity (may be null)
 *               The identity of the resource owner, when resolvable.
 *
 * session     - com.iplanet.sso.SSOToken (may be null)
 *               The user's session, present if the request carried a valid session.
 *
 * requestProperties - java.util.Map<String, Object>
 *               Selected properties of the OAuth2 request, e.g. "clientId", "realm", "grantType".
 *
 * logger      - com.sun.identity.shared.debug.Debug
 *               The "OAuth2Provider" debug logger instance.
 *
 * The script does not need to return a value: mutations applied to the accessToken object are
 * merged into the issued token.
 */

// Example: propagate the authentication context class reference and authentication modules.
def acr = accessToken.getField("acr")
if (acr != null) {
    accessToken.setField("acr", acr)
}

def amr = accessToken.getField("amr")
if (amr != null) {
    accessToken.setField("amr", amr)
}

// Example: expose the resource owner's email as a custom claim.
// if (identity != null) {
//     def mail = identity.getAttribute("mail")
//     if (mail != null && !mail.isEmpty()) {
//         accessToken.setField("email", mail.iterator().next())
//     }
// }

if (logger.messageEnabled()) {
    logger.message("OAuth2 Access Token Modification script executed for scopes: " + scopes)
}


