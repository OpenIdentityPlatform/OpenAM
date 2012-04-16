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
 * $Id: QueuedActionBean.java,v 1.3 2009/08/13 13:27:00 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.ManagedBeanResolver;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class QueuedActionBean implements Serializable {
    private List<PhaseEventAction> phaseEventActions = new ArrayList<PhaseEventAction>();

    public List<PhaseEventAction> getPhaseEventActions() {
        return phaseEventActions;
    }

    public static QueuedActionBean getInstance() {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        QueuedActionBean qab = (QueuedActionBean)mbr.resolve("queuedActionBean");
        return qab;
    }
}
