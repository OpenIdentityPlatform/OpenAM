/*
 * Copyright (c) 2012 ForgeRock AS. All rights reserved.
 *
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
 * information: "Portions Copyrighted [2012] [ForgeRock Inc]".
 *
 */
package com.sun.identity.sm.model;

import com.sun.identity.shared.Constants;
import org.forgerock.json.fluent.JsonValue;
import com.sun.identity.shared.OAuth2Constants;
import org.opends.server.protocols.ldap.LDAPAttribute;
import org.opends.server.types.RawAttribute;

import java.util.*;

public class TokenDataEntry {
    private String dn;
    private JsonValue attributeValues;
    private static final String FR_OAUTH2TOKEN = "frOAuth2Tokens";

    public TokenDataEntry(JsonValue value){
        attributeValues = value.get("value");
        dn = value.get(OAuth2Constants.CoreTokenParams.ID).required().asString();
        if (dn == null){
            dn = UUID.randomUUID().toString();
        }
    }

    public List<RawAttribute> getAttrList(){
        List<RawAttribute> attrList = new ArrayList<RawAttribute>(attributeValues.size());

       Map<String, Object> map = attributeValues.asMap();
       for (String key :map.keySet()){
           List<String> valueList = new ArrayList<String>();
           Object o = map.get(key);
           if (o instanceof Collection){
               for (Object o2 : (Collection)o){
                   if (o2 != null){
                       valueList.add(o2.toString());
                   }
               }
           } else {
               if (o != null){
                   valueList.add(o.toString());
               }
           }
           if (!valueList.isEmpty()){
               attrList.add(new LDAPAttribute(key, valueList));
           }
       }
       return attrList;
    }

    public List<LDAPAttribute> getObjectClasses(){
        List<String> valueList = new ArrayList<String>();
        valueList.add(Constants.TOP);
        valueList.add(FR_OAUTH2TOKEN);
        LDAPAttribute ldapAttr= new LDAPAttribute(Constants.OBJECTCLASS, valueList);
        List<LDAPAttribute> objectClasses = new ArrayList<LDAPAttribute>();
        objectClasses.add(ldapAttr);
        return objectClasses;
    }

    public String getDN(){
        return dn;
    }
}
