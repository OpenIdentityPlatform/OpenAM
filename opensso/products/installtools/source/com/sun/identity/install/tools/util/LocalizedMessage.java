/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LocalizedMessage.java,v 1.3 2008/06/25 05:51:29 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util;

import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.ResourceBundle;

public class LocalizedMessage {

    public static synchronized LocalizedMessage get(String id) {
        return get(id, STR_TOOLSMSG_GROUP, null);
    }

    public static synchronized LocalizedMessage get(String id, Object[] args) {
        return get(id, STR_TOOLSMSG_GROUP, args);
    }

    public static synchronized LocalizedMessage get(String id, String group) {
        return get(id, group, null);
    }

    public static synchronized LocalizedMessage get(String id, String group,
            Object[] args) {
        LocalizedMessage localizedMessage = null;
        ResourceBundle bundle = (ResourceBundle) getResourceBundles()
                .get(group);
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(group);
            getResourceBundles().put(group, bundle);
        }

        try {
            String message = bundle.getString(id);
            if (message != null) {
                if (args == null) {
                    localizedMessage = new LocalizedMessage(message);
                } else {
                    localizedMessage = new LocalizedMessage(message, args);
                }
            }
        } catch (java.util.MissingResourceException mre) {
        }
        return localizedMessage;
    }

    public String toString() {
        return getMessage();
    }

    public String getMessage() {
        return message;
    }

    private LocalizedMessage(String message) {
        setMessage(message);
    }

    private LocalizedMessage(String message, Object[] args) {
        MessageFormat fmt = new MessageFormat(message);
        setMessage(fmt.format(args));
    }

    private void setMessage(String message) {
        this.message = message;
    }

    private static Hashtable getResourceBundles() {
        return _resourceBundles;
    }

    private String message;

    private static Hashtable _resourceBundles = new Hashtable();

    public static final String STR_TOOLSMSG_GROUP = "amToolsMessages";
}
