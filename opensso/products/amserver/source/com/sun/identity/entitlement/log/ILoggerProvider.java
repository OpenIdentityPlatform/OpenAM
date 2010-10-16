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
 * $Id: ILoggerProvider.java,v 1.1 2009/08/19 05:40:35 veiming Exp $
 */

package com.sun.identity.entitlement.log;

import java.util.logging.Logger;

/**
 * This interface defines a method to return logger object.
 */
public interface ILoggerProvider {
    /**
     * System property name for setting logger provider class
     */
    String SYSTEM_PROPERTY_LOG_PROVIDER = "com.sun.identity.log.loggerprovider";

    /**
     * Returns logger object.
     *
     * @param name Name of logger.
     * @return logger object.
     */
    Logger getLogger(String name);
}
