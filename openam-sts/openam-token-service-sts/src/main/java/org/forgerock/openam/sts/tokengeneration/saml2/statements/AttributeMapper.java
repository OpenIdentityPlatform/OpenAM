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

import com.iplanet.sso.SSOToken;
import com.sun.identity.saml2.assertion.Attribute;
import org.forgerock.openam.sts.TokenCreationException;

import java.util.List;
import java.util.Map;

/**
 * TODO: if we have multiple AttributeStatements, how does this map to the AttributeMapper? Does each statement have to
 * be identified, or ??. Are multiple AttributeStatements common?
 * Defines the concerns of mapping attributes into SAML2 AttributeStatements.
 *
 */
public interface AttributeMapper {
    /**
     *
     * @param token  The SSOToken corresponding to the subject whose attributes will be referenced.
     * @param attributeMap Contains the mapping of saml attributes (Map keys) to local OpenAM LDAP attributes (Map values). The
     *                     keys will define the name of the attributes, and the data pulled from the subject's directory entry
     *                     will define the value corresponding to this attribute name. If no state is present corresponding
     *                     to this attribute name, then it will not appear in the AttributeStatement.
     * @return This list of populated SAML2 Attribute instances.
     * @throws TokenCreationException if an exception is encountered mapping attributes. TODO: more fine-grained, and action-specific
     * exceptions?
     */
    List<Attribute> getAttributes(SSOToken token, Map<String, String> attributeMap) throws TokenCreationException;
}
