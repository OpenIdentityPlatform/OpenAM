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
 * $Id: IVerifierOutput.java,v 1.3 2008/06/25 05:43:40 qcheng Exp $
 *
 */



package com.sun.identity.log.spi;


/**
 * Provides an interface to define the actions that need to be taken
 * depending on the return value of the Log Verification process.
 *
 * @supported.all.api
 */
public interface IVerifierOutput {
    /**
     * Returns true if an action is successfully done based on result of a
     * verification process.
     *
     * @param logName name of the log on which verification was carried out.
     * @param result result of the verification process.
     * @return true if the action is successfully done.
     */
    public boolean doVerifierAction(String logName, boolean result);
}
