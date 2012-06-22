/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ScheduleableGroupAction.java,v 1.2 2008/06/25 05:52:52 qcheng Exp $
 *
 */

package com.sun.identity.common;

/**
 * ScheduleableGroupAction is used as a separated handler for
 * InstantGroupRunnable and PeriodicGroupRunnable. Function doGroupAction() will
 * be run on the object instants when it is the time.
 */

public interface ScheduleableGroupAction {
    
    /**
     * The function to run on key object when there is time.
     */
    
    public void doGroupAction(Object key);
}
