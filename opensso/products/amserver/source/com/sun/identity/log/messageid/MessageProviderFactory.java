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
 * $Id: MessageProviderFactory.java,v 1.4 2008/06/25 05:43:37 qcheng Exp $
 *
 */



package com.sun.identity.log.messageid;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This factory provides us when message provider for different components.
 * Sample code is as follow.
 * <pre>
 *      try {
 *          LogMessageProvider provider = MessageProviderFactory.getProvider(
 *              "Console");
 *      } catch (IOException e) {
 *          System.out.println(e.getMessage());
 *      }
 * </pre>
 * This will return a message provider class for console.
 * <code>ConsoleLogMessageIDs.xml</code> is the XML file used for getting all
 * message IDs. <code>"Console"</code> + <code>"LogMessageIDs.xml"</code> where
 * <code>"Console"</code> is the name passed into the <code>getProvider</code>
 * method.
 */
public final class MessageProviderFactory {
    
    /**
     * Default package for message XML file.
     */
    public static final String DEFAULT_MESSAGE_ID_XML_DIR =
        "com/sun/identity/log/messageid";

    /**
     * Suffix of message ID XML file.
     */
    public static final String MESSAGEID_XML_SUFFIX = "LogMessageIDs.xml";

    /**
     * Instance of factory.
     */
    private static MessageProviderFactory instance = new
        MessageProviderFactory();

    /**
     * Map contains references to message provider.
     */
    private Map mapProviders = new HashMap();

    private MessageProviderFactory() {
    }

    /**
     * Returns an instance of provider.
     *
     * @param name Name of provider
     * @return an instance of provider.
     * @throws IOException if corresponding XML file is not found.
     */
    public static LogMessageProvider getProvider(String name)
        throws IOException {
        return instance.getMessageProvider(name, null);
    }

    /**
     * Returns an instance of provider.
     *
     * @param name Name of provider
     * @param packageName Package name where log message id XML file is located.
     * @return an instance of provider.
     * @throws IOException if corresponding XML file is not found.
     */
    public static LogMessageProvider getProvider(
        String name,
        String packageName
    ) throws IOException {
        return instance.getMessageProvider(name, packageName);
    }

    /**
     * Returns an instance of provider.
     *
     * @param name Name of provider.
     * @param packageName Package name where log message id XML file is located.
     * @return an instance of provider.
     * @throws IOException if corresponding XML file is not found.
     */
    private synchronized LogMessageProvider getMessageProvider(
        String name,
        String packageName
    ) throws IOException {
        LogMessageProvider p = (LogMessageProvider)mapProviders.get(name);

        if (p == null) {
            if (packageName == null) {
                packageName = DEFAULT_MESSAGE_ID_XML_DIR;
            }
            p = new LogMessageProviderBase(
                packageName + "/" + name + MESSAGEID_XML_SUFFIX);
            mapProviders.put(name, p);
        }

        return p;
    }
}
