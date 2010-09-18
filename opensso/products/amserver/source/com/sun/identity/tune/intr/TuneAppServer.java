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
 * $Id: TuneAppServer.java,v 1.2 2008/08/29 10:00:22 kanduls Exp $
 */

package com.sun.identity.tune.intr;

import com.sun.identity.tune.common.AMTuneException;

/**
 * <code>TuneAppServer<\code> is a interface which implements Tuning and 
 * declares the methods for tuning Application Server.
 * 
 */
public abstract class TuneAppServer implements Tuning {
    /**
     * This method modify s the domian.xml file with recommended values.
     */
    protected abstract void tuneDomainXML() throws AMTuneException;
    
    /**
     * Deletes the password file, appserver tunner should implement this method
     * so the no password file is left out in the temporary directory
     */
    protected abstract void deletePasswordFile();
}
