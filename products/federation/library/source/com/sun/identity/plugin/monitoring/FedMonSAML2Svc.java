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
 * $Id: FedMonSAML2Svc.java,v 1.2 2009/09/23 22:26:14 bigfatrat Exp $
 *
 */

package com.sun.identity.plugin.monitoring;


/**
 *  This interface defines methods which will be invoked by the
 *  Federation Framework to update monitoring-related counters
 */

public interface FedMonSAML2Svc {

    String IDP_ARTIF_CACHE = "idpArtifCache";
    String IDP_ARTIF_ISSUED = "idpArtifIssued";
    String IDP_ASSERT_CACHE = "idpAssertCache";
    String IDP_ASSERT_ISSUED = "idpAssertIssued";
    String IDP_RQTS_RCVD = "idpRqtRcvd";
    String IDP_INVAL_RQTS_RCVD = "idpInvalRqtRcvd";
    String SP_VAL_ASSERTS_RCVD = "spValidAssertRcvd";
    String SP_RQTS_SENT = "spRqtSent";
    String SP_INVAL_ARTIFS_RCVD = "spInvalArtifRcvd";

    /**
     * Initializes the provider.
     */
    public void init();

    /**
     * Increment the Federation (SP) session counter.
     */
    public void incFedSessionCount();

    /**
     * Decrement the Federation (SP) session counter.
     */
    public void decFedSessionCount();

    /**
     * Set the Federation (SP) session counter.
     */
    public void setFedSessionCount(long count);

    /**
     * Increment the IDP session counter.
     */
    public void incIdpSessionCount();

    /**
     * Decrement the IDP session counter.
     */
    public void decIdpSessionCount();

    /**
     * Set the IDP session counter.
     */
    public void setIdpSessionCount(long count);

    /**
     *  Increment a counter for an IDP in a realm. 
     *  @param realm name of the IDP's realm
     *  @param idpName the name of the IDP
     *  @param counter the IDP counter to increment
     */
    public void incIDPCounter (String realm, String idpName, String counter);

    /**
     *  decrement a counter for an IDP in a realm.
     *  @param realm name of the IDP's realm
     *  @param idpName the name of the IDP
     *  @param counter the IDP counter to decrement
     */
    public void decIDPCounter (String realm, String idpName, String counter);

}
