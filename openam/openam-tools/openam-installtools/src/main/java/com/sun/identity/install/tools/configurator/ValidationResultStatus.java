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
 * $Id: ValidationResultStatus.java,v 1.2 2008/06/25 05:51:25 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import com.sun.identity.install.tools.util.ResultStatus;

public class ValidationResultStatus extends ResultStatus {

    public static final int INT_STATUS_SUCCESS = 0;

    public static final int INT_STATUS_FAILED = 1;

    public static final int INT_STATUS_WARNING = 2;

    public static final String STR_STATUS_SUCCESS = "SUCCESS";

    public static final String STR_STATUS_FAILED = "FAILED";

    public static final String STR_STATUS_WARNING = "WARNING";

    public static final ValidationResultStatus STATUS_SUCCESS = 
        new ValidationResultStatus(STR_STATUS_SUCCESS, INT_STATUS_SUCCESS);
    

    public static final ValidationResultStatus STATUS_FAILED = 
        new ValidationResultStatus(STR_STATUS_FAILED, INT_STATUS_FAILED);

    public static final ValidationResultStatus STATUS_WARNING = 
        new ValidationResultStatus(STR_STATUS_WARNING, INT_STATUS_WARNING);

    private ValidationResultStatus(String statusString, int value) {
        super(statusString, value);
    }

    /*
     * Helper function
     */
    public boolean isSuccessful() {
        return equals(STATUS_SUCCESS);
    }

}
