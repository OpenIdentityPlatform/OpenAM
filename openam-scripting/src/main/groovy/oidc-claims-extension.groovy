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
* requestedClaims - Map<String, Set<String>>
*                  always present, not empty if the request contains a claims parameter and server has enabled
*                  claims_parameter_supported, map of requested claims to possible values, otherwise empty,
*                  requested claims with no requested values will have a key but no value in the map. A key with
*                  a single value in its Set indicates this is the only value that should be returned.
* Required to return a Map of claims to be added to the id_token claims
*/

// user session not guaranteed to be present
boolean sessionPresent = session != null

def fromSet = { claim, attr ->
    if (attr != null && attr.size() == 1){
        attr.iterator().next()
    } else if (attr != null && attr.size() > 1){
        attr
    } else if (logger.warningEnabled()) {
        logger.warning("OpenAMScopeValidator.getUserInfo(): Got an empty result for claim=$claim");
    }
}

attributeRetriever = { attribute, claim, identity, requested ->
    if (requested == null || requested.isEmpty()) {
        fromSet(claim, identity.getAttribute(attribute))
    } else if (requested.size() == 1) {
        requested.iterator().next()
    } else {
        throw new RuntimeException("No selection logic for $claim defined. Values: $requested")
    }
}

// [ {claim}: {attribute retriever}, ... ]
claimAttributes = [
        "email": attributeRetriever.curry("mail"),
        "address": { claim, identity, requested -> [ "formatted" : attributeRetriever("postaladdress", claim, identity, requested) ] },
        "phone_number": attributeRetriever.curry("telephonenumber"),
        "given_name": attributeRetriever.curry("givenname"),
        "zoneinfo": attributeRetriever.curry("preferredtimezone"),
        "family_name": attributeRetriever.curry("sn"),
        "locale": attributeRetriever.curry("preferredlocale"),
        "name": attributeRetriever.curry("cn")
]

// {scope}: [ {claim}, ... ]
scopeClaimsMap = [
        "email": [ "email" ],
        "address": [ "address" ],
        "phone": [ "phone_number" ],
        "profile": [ "given_name", "zoneinfo", "family_name", "locale", "name" ]
]

if (logger.messageEnabled()) {
    scopes.findAll { s -> !("openid".equals(s) || scopeClaimsMap.containsKey(s)) }.each { s ->
        logger.message("OpenAMScopeValidator.getUserInfo()::Message: scope not bound to claims: $s")
    }
}

def computeClaim = { claim, requestedValues ->
    try {
        [ claim, claimAttributes.get(claim)(claim, identity, requestedValues) ]
    } catch (IdRepoException e) {
        if (logger.warningEnabled()) {
            logger.warning("OpenAMScopeValidator.getUserInfo(): Unable to retrieve attribute=$attribute", e);
        }
    } catch (SSOException e) {
        if (logger.warningEnabled()) {
            logger.warning("OpenAMScopeValidator.getUserInfo(): Unable to retrieve attribute=$attribute", e);
        }
    }
}

scopes.findAll { s -> !"openid".equals(s) && scopeClaimsMap.containsKey(s) }.inject(claims) { map, s ->
    scopeClaims = scopeClaimsMap.get(s)
    map << scopeClaims.findAll { c -> !requestedClaims.containsKey(c) }.collectEntries([:]) { claim -> computeClaim(claim, null) }
}.findAll { map -> map.value != null } << requestedClaims.collectEntries([:]) { claim, requestedValue ->
    computeClaim(claim, requestedValue)
}
