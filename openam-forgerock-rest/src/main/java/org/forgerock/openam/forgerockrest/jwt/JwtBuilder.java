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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.jwt;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class to create an initial JWT or reconstruct a JWT object from a string.
 */
public class JwtBuilder {

    /**
     * Creates a new PlaintextJWT.
     *
     * @return A PlaintextJWT.
     */
    public PlaintextJwt jwt() {
        return new PlaintextJwt();
    }

    /**
     * Given a JWT as a String, this method will determine the type of JWT the String represents and creates the
     * appropriate JWT object for it.
     *
     * @param jwt The JWT String.
     * @return The reconstructed JWT object.
     */
    public Jwt recontructJwt(String jwt) {

        try {
            JwtString jwtString = new JwtString(jwt);

            JSONObject headerJson = new JSONObject(jwtString.getHeader());

            JwtType jwtType = JwtType.valueOf(headerJson.getString("typ"));
            switch (jwtType) {
                case JWT: {

                    PlaintextJwt plaintextJwt = new PlaintextJwt(reconstructJwtHeaders(jwtString.getHeader()),
                            reconstructJwtContent(jwtString.getContent()));

                    String algorithm = plaintextJwt.getHeader("alg");
                    if (algorithm != null && !"none".equals(algorithm)) {
                        return new SignedJwt(plaintextJwt, jwtString.getThirdPart());
                    } else {
                        return plaintextJwt;
                    }
                }
    //        case JWS: {
    //
    //        }
    //        case JWE: {
    //
    //        }
                default: {
                    throw new JWTBuilderException("Unable to reconstruct JWT");
                }
            }
        } catch (JSONException e) {
            throw new JWTBuilderException("Unable to reconstruct JWT", e);
        }
    }

    /**
     * Reconstructs a JWT's headers from a String.
     *
     * @param header The JWT's header as a String.
     * @return A Map of the JWT's headers.
     * @throws JSONException If there is a problem parsing the headers String.
     */
    private Map<String, String> reconstructJwtHeaders(String header) throws JSONException {

        Map<String, String> jwtHeader = new HashMap<String, String>();

        JSONObject headerJson = new JSONObject(header);

        Iterator<String> iter = headerJson.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            jwtHeader.put(key, headerJson.getString(key));
        }

        return jwtHeader;
    }

    /**
     * Reconstructs a JWT's payload content from a String.
     *
     * @param content The JWT's content as a String.
     * @return A Map of the JWT's content.
     * @throws JSONException If there is a problem parsing the content String.
     */
    private Map<String, Object> reconstructJwtContent(String content) throws JSONException {

        Map<String, Object> jwtContent = new LinkedHashMap<String, Object>();

        JSONObject headerJson = new JSONObject(content);

        Iterator<String> iter = headerJson.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            jwtContent.put(key, headerJson.get(key));
        }

        return jwtContent;
    }
}
