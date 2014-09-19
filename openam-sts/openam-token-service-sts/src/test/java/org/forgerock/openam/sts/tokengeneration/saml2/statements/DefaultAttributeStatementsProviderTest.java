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
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.AttributeStatement;
import com.sun.identity.saml2.common.SAML2Exception;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;


public class DefaultAttributeStatementsProviderTest {
    private final String ATTRIBUTE_NAME = "email";
    private final String ATTRIBUTE_VALUE = "<AttributeValue>authenticated</AttributeValue>";
    private SAML2Config saml2Config;
    private SSOToken mockToken;
    private AttributeMapper mockAttributeMapper;
    private Map<String, String> attributeMap;
    private List<Attribute> attributeList;

    @BeforeTest
    public void setup() throws TokenCreationException, SAML2Exception {
        attributeMap = new HashMap<String, String>();
        attributeMap.put(ATTRIBUTE_NAME, "mail");

        mockAttributeMapper = mock(AttributeMapper.class);
        mockToken = mock(SSOToken.class);
        Attribute attribute = AssertionFactory.getInstance().createAttribute();
        attribute.setName(ATTRIBUTE_NAME);
        List<String> attributeValueList = new ArrayList<String>();
        attributeValueList.add(ATTRIBUTE_VALUE);
        attribute.setAttributeValue(attributeValueList);
        attributeList = new ArrayList<Attribute>();
        attributeList.add(attribute);
        when(mockAttributeMapper.getAttributes(mockToken, attributeMap)).thenReturn(attributeList);

        saml2Config = createSAML2Config();
    }

    @Test
    public void testAttributeSettings() throws TokenCreationException {
        DefaultAttributeStatementsProvider defaultProvider = new DefaultAttributeStatementsProvider();
        List<AttributeStatement> statements = defaultProvider.get(mockToken, saml2Config, mockAttributeMapper);
        AttributeStatement statement = statements.get(0);
        Attribute attr = (Attribute)statement.getAttribute().get(0);
        assertTrue(ATTRIBUTE_VALUE.equals(attr.getAttributeValue().get(0)));
    }

    private SAML2Config createSAML2Config() {
        Map<String, String> attributeMap = new HashMap<String, String>();
        attributeMap.put("email", "mail");
        return SAML2Config.builder()
                .attributeMap(attributeMap)
                .nameIdFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent")
                .spEntityId("http://host.com/sp/entity/id")
                .build();
    }
}
