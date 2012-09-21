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
 * $Id: SMSessionData.java,v 1.2 2008/06/25 05:43:21 qcheng Exp $
 *
 */

package com.sun.identity.console.session.model;

import java.io.Serializable;

/* - NEED NOT LOG - */

/** 
 * Class that provides methods to set/get session data.
 */
public class SMSessionData implements Serializable 
{
    
    private String userId = null;
    private String id = null;
    private long timeRemain;
    private long maxSessionTime;
    private long idleTime;
    private long maxIdleTime;

    /**
     * Sets session id.
     *
     * @param value session id.
     */
    public void setId(String value) {
        id = value;
    }

    /**
     * Sets user id.
     *
     * @param value user id.
     */
    public void setUserId(String value) {
        userId = value;
    }

    /**
     * Sets time left.
     *
     * @param value Time left.
     */
    public void setTimeRemain(long value) {
        timeRemain = value;
    }

    /**
     * Sets max session time left.
     *
     * @param value Session max time.
     */
    public void setMaxSessionTime(long value) {
        maxSessionTime = value;
    }

    /**
     * Sets idle time.
     *
     * @param value Idle time.
     */
    public void setIdleTime(long value) {
        idleTime = value;
    }

    /**
     * Set maximum idle time.
     *
     * @param value Maximum idle time.
     */
    public void setMaxIdleTime(long value) {
        maxIdleTime = value;
    }

    /**
     * Returns session ID.
     *
     * @return session ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns user ID.
     *
     * @return user ID.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns time left.
     *
     * @return time left.
     */
    public long getTimeRemain() {
        return timeRemain;
    }

    /**
     * Returns maximum session time.
     *
     * @return maximum session time.
     */
    public long getMaxSessionTime() {
        return maxSessionTime;
    }

    /**
     * Returns idle time.
     *
     * @return idle time.
     */
    public long getIdleTime() {
        return idleTime;
    }

    /**
     * Returns max idle time.
     *
     * @return max idle time.
     */
    public long getMaxIdleTime() {
        return maxIdleTime;
    }
}
