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
 * $Id: TuneOS.java,v 1.1 2008/07/02 18:56:22 kanduls Exp $
 */

package com.sun.identity.tune.intr;

import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.constants.OSConstants;
import com.sun.identity.tune.constants.AMTuneConstants;

public abstract class TuneOS implements Tuning, AMTuneConstants, OSConstants {
    
    /**
     * OS Kernel tuning will go into this method.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected abstract void tuneKernel() throws AMTuneException;
    
    /**
     * TCP parameter tuning will go into this method.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected abstract void tuneTCP() throws AMTuneException;
            
}
