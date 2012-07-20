/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 * Copyright © 2011 Cybernetica AS.
 * 
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.authentication.modules.oauth2;

import com.sun.identity.authentication.spi.AuthLoginException;

import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.*;


public class DefaultAttributeMapper implements AttributeMapper {

    public DefaultAttributeMapper() {
    }

    //@Override
    public Map<String, Set<String>> getAttributes(Set<String> attributeMapConfiguration, 
                   String svcProfileResponse) 
            throws AuthLoginException {

        OAuthUtil.debugMessage("defaultAttributeMapper.getAttributes: " +
                attributeMapConfiguration);
        JSONObject json;
        try {
            json = new JSONObject((String) svcProfileResponse);
        } catch (JSONException ex) {
            OAuthUtil.debugError("OAuth.process(): JSONException: " + ex.getMessage());
                    throw new AuthLoginException(BUNDLE_NAME,
                            ex.getMessage(), null);
        }
        Map<String, Set<String>> attr = new HashMap<String, Set<String>>();
        String responseName = "";
        String localName = "";
        for(String entry : attributeMapConfiguration) {
            try {
                if (entry.indexOf("=") == -1) {
                    OAuthUtil.debugMessage("defaultAttributeMapper.getAttributes: " + "Invalid entry." + entry);
                    continue;
                }
                StringTokenizer st = new StringTokenizer(entry, "=");
                responseName = st.nextToken();
                localName = st.nextToken();
                OAuthUtil.debugMessage("defaultAttributeMapper.getAttributes: " +
                        responseName + ":" + localName);

                String data = "";
                if (responseName != null && responseName.indexOf(".") != -1) {
                    StringTokenizer parts = new StringTokenizer(responseName, ".");
                    data = json.getJSONObject(parts.nextToken()).getString(parts.nextToken());
                } else {
                    data = json.getString(responseName);
                }

                attr.put(localName, OAuthUtil.addToSet(new HashSet<String>(), data));
            } catch (JSONException ex) {
                OAuthUtil.debugError("defaultAttributeMapper.getAttributes: Could not "
                        + "get the attribute" + responseName, ex);
            }
        }
        
	return attr;

    }
}
