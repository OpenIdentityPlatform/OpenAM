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
 * Copyright 2014-2015 ForgeRock AS.
 */
import com.iplanet.sso.SSOException
import com.sun.identity.idm.IdRepoException

/*
 * Defined variables:
 * logger - always presents, the "OAuth2Provider" debug logger instance
 * claims - always present, default server provided claims
 * accessToken - always present, the OAuth2 access token
 * session - present if the request contains the session cookie, the user's session object
 * identity - always present, the identity of the resource owner
 * scopes - always present, the requested scopes
 *
 * Required to return a Map of claims to be added to the id_token claims
 */

// user session not guaranteed to be present
boolean sessionPresent = session != null

profileAttributeMap = [
    "email": ["email": "mail"],
    "address": ["address": "postaladdress"],
    "phone": ["phone": "telephonenumber"],
    "profile": [
            "given_name": "givenname",
            "zoneinfo": "preferredtimezone",
            "family_name": "sn",
            "locale": "preferredlocale",
            "name": "cn"
    ]
]

def fromSet = { claim, attr ->
    if (attr != null && attr.size() == 1){
        attr.iterator().next()
    } else if (attr != null && attr.size() > 1){
        attr
    } else if (logger.warningEnabled()) {
        logger.warning("OpenAMScopeValidator.getUserInfo(): Got an empty result for scope=" + claim);
    }
}

if (logger.messageEnabled()) {
    scopes.findAll { s -> !("openid".equals(s) || profileAttributeMap.containsKey(s)) }.each { s ->
        logger.message("OpenAMScopeValidator.getUserInfo()::Message: scope not bound to claims: " + s)
    }
}

scopes.findAll { s -> !"openid".equals(s) && profileAttributeMap.containsKey(s) }.inject(claims) { map, s ->
    attributes = profileAttributeMap.get(s)
    map << attributes.collectEntries([:]) { claim, attribute ->
        try {
            [ claim,  fromSet(claim, identity.getAttribute(attribute)) ]
        } catch (IdRepoException e) {
            if (logger.warningEnabled()) {
                logger.warning("OpenAMScopeValidator.getUserInfo(): Unable to retrieve attribute=" + attribute, e);
            }
        } catch (SSOException e) {
            if (logger.warningEnabled()) {
                logger.warning("OpenAMScopeValidator.getUserInfo(): Unable to retrieve attribute=" + attribute, e);
            }
        }
    }
}.findAll { map -> map.value != null }