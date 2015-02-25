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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.tokengeneration.saml2.statements;

import com.sun.identity.saml2.assertion.Conditions;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;

import java.util.Date;

/**
 *  Implementations of this interface will be consulted to obtain the Conditions object included in generated SAML2 assertions.
 *  If the published STS instance does not encapsulate a custom ConditionsProvider, then the DefaultConditionsProvider class
 *  will be referenced to obtain the Conditions statement.
 */
public interface ConditionsProvider {
    /**
     * Called to obtain the Conditions instance to be included in the generated SAML2 assertion
     * @param saml2Config STS-instance-specific SAML2 configurations
     * @param issueInstant The instant at which the enclosing assertion was issued
     * @param saml2SubjectConfirmation The subject confirmation specification
     * @return The generated Conditions instance.
     * @throws TokenCreationException
     */
    Conditions get(SAML2Config saml2Config, Date issueInstant,
                   SAML2SubjectConfirmation saml2SubjectConfirmation) throws TokenCreationException;
}
