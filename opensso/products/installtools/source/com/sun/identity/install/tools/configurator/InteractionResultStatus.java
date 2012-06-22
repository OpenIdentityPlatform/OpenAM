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
 * $Id: InteractionResultStatus.java,v 1.2 2008/06/25 05:51:21 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import com.sun.identity.install.tools.util.ResultStatus;

public class InteractionResultStatus extends ResultStatus {

    public static final int INT_STATUS_CONTINUE = 0;

    public static final int INT_STATUS_BACK = 1;

    public static final int INT_STATUS_ABORT = 2;

    public static final int INT_STATUS_START_OVER = 3;

    public static final String STR_STATUS_CONTINUE = "CONTINUE";

    public static final String STR_STATUS_BACK = "BACK";

    public static final String STR_STATUS_ABORT = "ABORT";

    public static final String STR_STATUS_START_OVER = "START_OVER";

    public static final InteractionResultStatus STATUS_CONTINUE = 
        new InteractionResultStatus(
            STR_STATUS_CONTINUE, INT_STATUS_CONTINUE);

    public static final InteractionResultStatus STATUS_BACK = 
        new InteractionResultStatus(STR_STATUS_BACK, INT_STATUS_BACK);

    public static final InteractionResultStatus STATUS_ABORT = 
        new InteractionResultStatus(STR_STATUS_ABORT, INT_STATUS_ABORT);

    public static final InteractionResultStatus STATUS_START_OVER = 
        new InteractionResultStatus(
            STR_STATUS_START_OVER, INT_STATUS_START_OVER);

    private static final InteractionResultStatus[] values = 
        new InteractionResultStatus[] { STATUS_CONTINUE, STATUS_BACK, 
        STATUS_ABORT, STATUS_START_OVER };

    public static InteractionResultStatus get(String status) {
        return (InteractionResultStatus) ResultStatus.get(status, values);
    }

    public static InteractionResultStatus get(int status) {
        return (InteractionResultStatus) ResultStatus.get(status, values);
    }

    private InteractionResultStatus(String name, int value) {
        super(name, value);
    }
}
