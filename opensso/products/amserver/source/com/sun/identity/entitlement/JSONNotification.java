/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: JSONNotification.java,v 1.1 2009/09/14 23:02:40 veiming Exp $
 */

package com.sun.identity.entitlement;

import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONNotification {
    

    private JSONNotification() {
    }

    public static String toJSONString(
        String realm,
        String privilegeName,
        Set<String> resources,
        Type type
    ) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("type", type.getMessage());

        if ((realm != null) && (realm.length() > 0) && !realm.equals("/")) {
            jo.put("realm", realm);
        }
        
        jo.put("privilegeName", privilegeName);
        jo.put("resources", resources);
        return jo.toString();
    }


    public enum Type {
        CREATE ("CREATE"),
        DELETE ("DELETE"),
        MODIFY ("MODIFY");

        private final String message;

        Type(String message) {
            this.message = message;
        }

        String getMessage() {
            return message;
        }
    }

}
