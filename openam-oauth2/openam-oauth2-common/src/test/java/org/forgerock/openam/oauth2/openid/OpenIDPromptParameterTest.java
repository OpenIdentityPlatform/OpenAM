/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 */
package org.forgerock.openam.oauth2.openid;

import org.testng.annotations.Test;

public class OpenIDPromptParameterTest {

    @Test
    public void testLoginPrompt() {
        String parameterString = "login";
        OpenIDPromptParameter openIDPromptParameter = new OpenIDPromptParameter(parameterString);
        assert openIDPromptParameter.isValid();
        assert openIDPromptParameter.promptLogin();
    }

    @Test
    public void testConsentPrompt() {
        String parameterString = "consent";
        OpenIDPromptParameter openIDPromptParameter = new OpenIDPromptParameter(parameterString);
        assert openIDPromptParameter.isValid();
        assert openIDPromptParameter.promptConsent();
    }

    @Test
    public void testNoPrompts() {
        String parameterString = "none";
        OpenIDPromptParameter openIDPromptParameter = new OpenIDPromptParameter(parameterString);
        assert openIDPromptParameter.isValid();
        assert openIDPromptParameter.noPrompts();
    }

    @Test
    public void testIsValidWithTwoValidValues() {
        String parameterString = "consent login";
        OpenIDPromptParameter openIDPromptParameter = new OpenIDPromptParameter(parameterString);
        assert openIDPromptParameter.isValid();
    }

    @Test
    public void testIsValidWithTwoNotValidValues() {
        String parameterString = "consent none";
        OpenIDPromptParameter openIDPromptParameter = new OpenIDPromptParameter(parameterString);
        assert !openIDPromptParameter.isValid();
        assert !openIDPromptParameter.promptConsent();
    }

    @Test
    public void testIsValidWithTwoNotValidValues2() {
        String parameterString = "login none";
        OpenIDPromptParameter openIDPromptParameter = new OpenIDPromptParameter(parameterString);
        assert !openIDPromptParameter.isValid();
        assert !openIDPromptParameter.promptLogin();
    }

    @Test
    public void testIsValidWithThreeValues() {
        String parameterString = "consent login none";
        OpenIDPromptParameter openIDPromptParameter = new OpenIDPromptParameter(parameterString);
        assert !openIDPromptParameter.isValid();
        assert !openIDPromptParameter.noPrompts();
    }

    @Test
    public void testIsValidWithNoValues() {
        String parameterString = "";
        OpenIDPromptParameter openIDPromptParameter = new OpenIDPromptParameter(parameterString);
        assert openIDPromptParameter.isValid();
        assert !openIDPromptParameter.promptLogin();
    }

    @Test
    public void testIsValidNullParameters() {
        String parameterString = null;
        OpenIDPromptParameter openIDPromptParameter = new OpenIDPromptParameter(parameterString);
        assert openIDPromptParameter.isValid();
        assert !openIDPromptParameter.promptLogin();
    }
}
