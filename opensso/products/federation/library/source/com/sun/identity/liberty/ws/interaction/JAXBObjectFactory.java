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
 * $Id: JAXBObjectFactory.java,v 1.3 2008/08/06 17:28:10 exu Exp $
 *
 */

package com.sun.identity.liberty.ws.interaction;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.liberty.ws.interaction.jaxb.ObjectFactory;

/**
 * Class that provides access to a singleton 
 * <code>com.sun.identity.liberty.ws.interaction.jaxb.ObjectFactory
 * </code>. You would use the object factory to create instances of classes
 * in package <code>com.sun.identity.liberty.ws.interaction.jaxb</code>
 */
public class JAXBObjectFactory {

    private static ObjectFactory objectFactory = new ObjectFactory();
    private static Debug debug = Debug.getInstance("libIDWSF");

    /** Gets singleton instance of 
     * <code>com.sun.identity.liberty.ws.interaction.jaxb.ObjectFactory</code>.
     * @return singleton instance of 
     * <code>com.sun.identity.liberty.ws.interaction.jaxb.ObjectFactory</code>.
     */
    public static ObjectFactory getObjectFactory() {
        if (objectFactory == null) {
            objectFactory = new ObjectFactory();
            if (debug.messageEnabled()) {
                debug.message("JAXBObjectFactory.getObjectFactory():"
                        + "constructed sigleton instance");
            }
        }
        return objectFactory;
    }

    /* Adding a private default constructor to prevent compiler generating
     * public default constructor
     */
    private JAXBObjectFactory() {
    }

}
