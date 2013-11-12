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
 * $Id: FedMonIDFFSvc.java,v 1.2 2009/08/03 18:18:40 bigfatrat Exp $
 *
 */

package com.sun.identity.plugin.monitoring;


/**
 *  This interface defines methods which will be invoked by the
 *  Federation Framework to update monitoring-related counters
 */

public interface FedMonIDFFSvc {

    /**
     * Initializes the provider.
     */
    public void init();

    /**
     * Increment the ID-FF service's ID Local Session Token counter.
     */
    public void incIdLocalSessToken();

    /**
     * Decrement the ID-FF service's ID Local Session Token counter.
     */
    public void decIdLocalSessToken();

    /**
     * Set the ID-FF service's ID Local Session Token counter.
     */
    public void setIdLocalSessToken(long count);

    /**
     * Increment the ID-FF service's ID Authentication request counter.
     */
    public void incIdAuthnRqt();

    /**
     * Increment the ID-FF service's ID Session List counter.
     */
    public void incUserIDSessionList();

    /**
     * Decrement the ID-FF service's ID Session List counter.
     */
    public void decUserIDSessionList();

    /**
     * Set the ID-FF service's ID Session List counter.
     */
    public void setUserIDSessionList(long count);

    /**
     * Increment the ID-FF service's artifact counter.
     */
    public void incArtifacts();

    /**
     * Decrement the ID-FF service's artifact counter.
     */
    public void decArtifacts();

    /**
     * Set the ID-FF service's artifact counter.
     */
    public void setArtifacts(long count);

    /**
     * Increment the ID-FF service's assertion counter.
     */
    public void incAssertions();

    /**
     * Decrement the ID-FF service's assertion counter.
     */
    public void decAssertions();

    /**
     * Set the ID-FF service's assertion counter.
     */
    public void setAssertions(long count);

    /**
     * Set the ID-FF service's relay state indicator.
     * @param state the Relay
     */
    public void setRelayState(long state);

    /**
     * Increment the ID-FF service's Id Destination counter.
     */
    public void incIdDestn();

    /**
     * Decrement the ID-FF service's Id Destination counter.
     */
    public void decIdDestn();

    /**
     * Set the ID-FF service's Id Destination counter.
     */
    public void setIdDestn(long count);

}
