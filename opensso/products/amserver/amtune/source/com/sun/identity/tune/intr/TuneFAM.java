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
 * $Id: TuneFAM.java,v 1.4 2008/08/29 10:35:44 kanduls Exp $
 */

package com.sun.identity.tune.intr;

import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.constants.FAMConstants;

/**
 * <code>TuneFAM<\code> is a abstract class which implements Tuning and 
 * and declares all the methods that need to be implemented for tuning FAM.
 * 
 */
public abstract class TuneFAM implements Tuning, FAMConstants {
    
    /**
     * abstract method for tuning FAM Server configuration
     */
    protected abstract void tuneFAMServerConfig() throws AMTuneException;
    
     /**
     * abstract method for tuning OpenSSO Serverconfig.xml
     */
    protected abstract void tuneServerConfig() throws AMTuneException;
    
    /**
     * abstract method for tuning LDAP connection Pool.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected abstract void tuneLDAPConnPool() throws AMTuneException;
    
    /**
     * Creates password file.
     * 
     * @throws com.sun.identity.tune.common.AMTuneException
     */
     
    protected abstract void writePasswordToFile() throws AMTuneException;
    
    /**
     * Deletes the password file.
     * 
     */
    protected abstract void deletePasswordFile();
}
