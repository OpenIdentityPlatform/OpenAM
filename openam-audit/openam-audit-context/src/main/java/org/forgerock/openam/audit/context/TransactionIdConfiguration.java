/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.audit.context;

/**
 * Responsible for deciding whether or not transaction ID received as HTTP header should be accepted.
 * <p/>
 * The decision should be made based on the value of the System Property "org.forgerock.http.TrustTransactionHeader";
 * unfortunately, due to cyclic dependency issues, it's not possible to access the com.iplanet.am.util.SystemProperties
 * from this module.
 *
 * @since 13.0.0
 */
public interface TransactionIdConfiguration {

    /**
     * Establishes whether or not HTTP header "X-ForgeRock-TransactionId" should be trusted.
     *
     * @return true if the System Property "org.forgerock.http.TrustTransactionHeader" is true; false otherwise.
     */
    boolean trustHttpTransactionHeader();
}
