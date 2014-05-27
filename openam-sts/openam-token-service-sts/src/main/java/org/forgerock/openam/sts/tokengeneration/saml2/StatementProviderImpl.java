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

package org.forgerock.openam.sts.tokengeneration.saml2;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.AttributeMapper;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.AttributeStatementsProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.AuthenticationStatementsProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.AuthzDecisionStatementsProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.ConditionsProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.DefaultAttributeMapper;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.DefaultAttributeStatementsProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.DefaultAuthenticationStatementsProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.DefaultAuthzDecisionStatementsProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.DefaultConditionsProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.DefaultSubjectProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.SubjectProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.KeyInfoFactory;

import javax.inject.Inject;

/**
 * @see org.forgerock.openam.sts.tokengeneration.saml2.StatementProvider
 */
public class StatementProviderImpl implements StatementProvider {
    private final KeyInfoFactory keyInfoFactory;

    @Inject
    StatementProviderImpl(KeyInfoFactory keyInfoFactory) {
        this.keyInfoFactory = keyInfoFactory;
    }

    public ConditionsProvider getConditionsProvider(SAML2Config saml2Config) throws TokenCreationException {
        String customProvider = saml2Config.getCustomConditionsProviderClassName();
        if ((customProvider != null) && !customProvider.isEmpty()) {
            return createCustomProviderInstance(customProvider, ConditionsProvider.class);
        }
        return new DefaultConditionsProvider();
    }

    public SubjectProvider getSubjectProvider(SAML2Config saml2Config) throws TokenCreationException {
        String customProvider = saml2Config.getCustomSubjectProviderClassName();
        if ((customProvider != null) && !customProvider.isEmpty()) {
            return createCustomProviderInstance(customProvider, SubjectProvider.class);
        }
        return new DefaultSubjectProvider(keyInfoFactory);
    }

    public AuthenticationStatementsProvider getAuthenticationStatementsProvider(SAML2Config saml2Config) throws TokenCreationException{
        String customProvider = saml2Config.getCustomAuthenticationStatementsProviderClassName();
        if ((customProvider != null) && !customProvider.isEmpty()) {
            return createCustomProviderInstance(customProvider, AuthenticationStatementsProvider.class);
        }
        return new DefaultAuthenticationStatementsProvider();
    }

    public AttributeStatementsProvider getAttributeStatementsProvider(SAML2Config saml2Config) throws TokenCreationException {
        String customProvider = saml2Config.getCustomAttributeStatementsProviderClassName();
        if ((customProvider != null) && !customProvider.isEmpty()) {
            return createCustomProviderInstance(customProvider, AttributeStatementsProvider.class);
        }
        return new DefaultAttributeStatementsProvider();
    }

    public AuthzDecisionStatementsProvider getAuthzDecisionStatementsProvider(SAML2Config saml2Config) throws TokenCreationException {
        String customProvider = saml2Config.getCustomAuthzDecisionStatementsProviderClassName();
        if ((customProvider != null) && !customProvider.isEmpty()) {
            return createCustomProviderInstance(customProvider, AuthzDecisionStatementsProvider.class);
        }
        return new DefaultAuthzDecisionStatementsProvider();
    }

    public AttributeMapper getAttributeMapper(SAML2Config saml2Config) throws TokenCreationException {
        String customProvider = saml2Config.getCustomAttributeMapperClassName();
        if ((customProvider != null) && !customProvider.isEmpty()) {
            return createCustomProviderInstance(customProvider, AttributeMapper.class);
        }
        return new DefaultAttributeMapper(saml2Config.getAttributeMap());
    }

    private <T> T createCustomProviderInstance(String className, Class<T> desiredClass) throws TokenCreationException {
        try {
            return Class.forName(className).asSubclass(desiredClass).newInstance();
        } catch (ClassNotFoundException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "The registered custom Provider, " + className + ", cannot be found: " + e);
        } catch (InstantiationException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "The registered custom Provider, " + className + ", cannot be instantiated: " + e);
        } catch (IllegalAccessException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "The registered custom Provider, " + className + ", cannot be accessed: " + e);
        }
    }
}
