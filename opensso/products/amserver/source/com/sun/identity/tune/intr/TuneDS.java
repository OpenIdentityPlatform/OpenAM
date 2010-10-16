/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: TuneDS.java,v 1.1 2008/07/02 18:56:22 kanduls Exp $
 */

package com.sun.identity.tune.intr;

import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.constants.DSConstants;

/**
 * <code>TuneDS<\code> is a abstract class which implements Tuning and declares
 * methods that are required for tuning Directory Server.
 */
public abstract class TuneDS implements Tuning, DSConstants {
    
    /**
     * abstract method to stop the Directory Server.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected abstract void stopDS() throws AMTuneException;
    
    /**
     * abstract method to start the Directory Server.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected abstract void startDS() throws AMTuneException;
    
    /**
     * abstract method to backup the Directory Server.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected abstract void backUpDS() throws AMTuneException;
    
    /**
     * abstract method to create password file.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected abstract void writePasswordToFile() throws AMTuneException;
    
    /**
     * abstract method to delete password file.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected abstract void deletePasswordFile();
}
