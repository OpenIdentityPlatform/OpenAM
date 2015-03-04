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

boolean loggerPresent = logger != null
boolean claimsPresent = claims != null
boolean accessTokenPresent = accessToken != null
boolean sessionPresent = session != null
boolean identityPresent = identity != null
boolean scopesPresent = scopes != null

//claims.put("sub", accessToken.getResourceOwnerId())
//claims.put("updated_at", )

Map<String, Object> scopeToUserProfileAttributes = [
    "email": "mail",
    "address": "postaladdress",
    "phone": "telephonenumber",
    "profile": [
            "given_name": "givenname",
            "zoneinfo": "preferredtimezone",
            "family_name": "sn",
            "locale": "preferredlocale",
            "name": "cn"
    ]
]

for (int i = 0; i < scopes.size(); i++) {
    scope = scopes.getAt(i)
    if ("openid".equals(scope)) {
        continue
    }

    Object attributes = scopeToUserProfileAttributes.get(scope)
    if (attributes == null) {
        if (logger.warningEnabled()) {
            logger.error("OpenAMScopeValidator.getUserInfo()::Invalid Scope in token scope=" + scope)
        }
    } else if (attributes instanceof String) {
        Set<String> attr = null;

        //if the attribute is a string get the attribute
        try {
            attr = identity.getAttribute((String)attributes);
        } catch (IdRepoException e) {
            if (logger.warningEnabled()) {
                logger.warning("OpenAMScopeValidator.getUserInfo(): Unable to retrieve attribute=" + attributes, e);
            }
        } catch (SSOException e) {
            if (logger.warningEnabled()) {
                logger.warning("OpenAMScopeValidator.getUserInfo(): Unable to retrieve attribute=" + attributes, e);
            }
        }

        //add a single object to the response.
        if (attr != null && attr.size() == 1){
            claims.put(scope, attr.iterator().next());
        } else if (attr != null && attr.size() > 1){ // add a set to the response
            claims.put(scope, attr);
        } else {
            if (logger.warningEnabled()) {
                //attr is null or attr is empty
                logger.warning("OpenAMScopeValidator.getUserInfo(): Got an empty result for attribute=" + attributes + " of scope=" + scope);
            }
        }
    } else if (attributes instanceof Map){

        //the attribute is a collection of attributes
        //for example profile can be address, email, etc...
        if (attributes != null && !((Map<String,String>) attributes).isEmpty()){
            for (int j = 0; j < ((Map<String, String>) attributes).entrySet().size(); j++){
                entry = ((Map<String, String>) attributes).entrySet().getAt(j)
                String attribute = null;
                attribute = entry.getValue();
                Set<String> attr = null;

                //get the attribute
                try {
                    attr = identity.getAttribute(attribute);
                } catch (IdRepoException e) {
                    if (logger.warningEnabled()) {
                        logger.warning("OpenAMScopeValidator.getUserInfo(): Unable to retrieve attribute=" + attributes, e);
                    }
                } catch (SSOException e) {
                    if (logger.warningEnabled()) {
                        logger.warning("OpenAMScopeValidator.getUserInfo(): Unable to retrieve attribute=" + attributes, e);
                    }
                }

                //add the attribute value(s) to the response
                if (attr != null && attr.size() == 1){
                    claims.put(entry.getKey(), attr.iterator().next());
                } else if (attr != null && attr.size() > 1){
                    claims.put(entry.getKey(), attr);
                } else {
                    if (logger.warningEnabled()) {
                        //attr is null or attr is empty
                        logger.warning("OpenAMScopeValidator.getUserInfo(): Got an empty result for scope=" + scope);
                    }
                }
            }
        }
    }
}
return claims;