/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RestrictedTokenContext.java,v 1.4 2008/06/25 05:43:59 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.session.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.sun.identity.shared.encode.Base64;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

/**
 * Utility to attach the context for token restriction checking to the current
 * thread and marshalling/unmarshalling context value
 */

public class RestrictedTokenContext {

    /* The LocalThread for the currentcontext*/
    private static ThreadLocal currentContext = new ThreadLocal();

    /* The Object prefix*/
    private static final String OBJECT_PREFIX = "object:";

    /*The Token Prefix*/
     private static final String TOKEN_PREFIX = "token:";

    /**
     * Returns the current context of the running thread
     * 
     * @return object containing the current context
     */
    public static Object getCurrent() {
        return currentContext.get();
    }

    /**
     * Performs an action while temporary replacing the current token
     * restriction checking context associated with the running thread After
     * returning from action run() method original context is restored
     * 
     * @param context
     *            context to be used with the action
     * @param action
     *            action to be performed
     * @return object
     * @throws Exception if the there was an error.
     */
    public static Object doUsing(Object context, RestrictedTokenAction action)
            throws Exception {
        Object savedContext = currentContext.get();
        try {
            currentContext.set(context);
            return action.run();
        } finally {
            currentContext.set(savedContext);
        }
    }

    /**
     * Serialize the current context to a string
     * 
     * @param context
     *            to be serialized
     * @return string containing the serialized object
     * @throws Exception if the there was an error.
     */
    public static String marshal(Object context) throws Exception {
        if (context instanceof SSOToken) {
            return TOKEN_PREFIX + ((SSOToken) context).getTokenID().toString();
        } else {
            // perform general Java serialization
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(bs);
            os.writeObject(context);
            os.flush();
            os.close();
            return OBJECT_PREFIX + Base64.encode(bs.toByteArray());
        }
    }

    /**
     * Deserialize the context from the string created by previous call to
     * marshal()
     * 
     * @param data
     *            string containing serialized context
     * @return deserialized context object
     * @throws Exception if the there was an error.
     */
    public static Object unmarshal(String data) throws Exception {
        if (data.startsWith(TOKEN_PREFIX)) {
            return SSOTokenManager.getInstance().createSSOToken(
                    data.substring(TOKEN_PREFIX.length()));
        } else if (data.startsWith(OBJECT_PREFIX)) {
            // perform general Java deserialization
            ObjectInputStream is = new ObjectInputStream(
                    new ByteArrayInputStream(Base64.decode(data
                            .substring(OBJECT_PREFIX.length()))));
            return is.readObject();
        } else {
            throw new IllegalArgumentException("Bad context value:" + data);
        }
    }

    /**
     * Clears the current context from this Thread Local
     *
     */
    public static void clear() {
        currentContext.remove();
    }
}
