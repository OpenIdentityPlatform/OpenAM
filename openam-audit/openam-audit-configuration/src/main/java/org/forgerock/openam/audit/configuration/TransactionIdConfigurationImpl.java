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
package org.forgerock.openam.audit.configuration;

import org.forgerock.openam.audit.context.TransactionIdConfiguration;

import com.iplanet.am.util.SystemProperties;

/**
 * Responsible for deciding whether or not transaction ID received as HTTP header should be accepted.
 *
 * @since 13.0.0
 */
final class TransactionIdConfigurationImpl implements TransactionIdConfiguration {

    @Override
    public boolean trustHttpTransactionHeader() {
        return SystemProperties.getAsBoolean("org.forgerock.http.TrustTransactionHeader", false);
    }
}
