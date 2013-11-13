/*
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package com.sun.identity.saml2.profile;

import static com.sun.identity.saml2.common.SAML2Constants.*;
import com.sun.identity.saml2.jaxb.metadata.SingleLogoutServiceElement;
import com.sun.identity.saml2.jaxb.metadata.impl.SingleLogoutServiceElementImpl;
import java.util.ArrayList;
import java.util.List;
import static org.fest.assertions.Assertions.*;
import org.testng.annotations.Test;

@Test
public class SLOLocationTest {

    public void sameBindingReturnedWhenAvailable() {
        List<SingleLogoutServiceElement> endpoints = new ArrayList<SingleLogoutServiceElement>();
        endpoints.add(endpointFor(HTTP_REDIRECT, "redirect"));
        endpoints.add(endpointFor(HTTP_POST, "post"));
        endpoints.add(endpointFor(SOAP, "soap"));
        SingleLogoutServiceElement result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_REDIRECT);
        assertThat(result.getBinding()).isEqualTo(HTTP_REDIRECT);
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_POST);
        assertThat(result.getBinding()).isEqualTo(HTTP_POST);
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, SOAP);
        assertThat(result.getBinding()).isEqualTo(SOAP);
    }

    public void asynchronousBindingIsPreferredOverSynchronous() {
        List<SingleLogoutServiceElement> endpoints = new ArrayList<SingleLogoutServiceElement>();
        endpoints.add(endpointFor(HTTP_POST, "post"));
        endpoints.add(endpointFor(SOAP, "soap"));
        SingleLogoutServiceElement result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_REDIRECT);
        assertThat(result.getBinding()).isEqualTo(HTTP_POST);
        endpoints.set(0, endpointFor(HTTP_REDIRECT, "redirect"));
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_POST);
        assertThat(result.getBinding()).isEqualTo(HTTP_REDIRECT);
    }

    public void asynchronousBindingsAreNotReturnedWhenRequestingSynchronous() {
        List<SingleLogoutServiceElement> endpoints = new ArrayList<SingleLogoutServiceElement>();
        endpoints.add(endpointFor(HTTP_REDIRECT, "redirect"));
        endpoints.add(endpointFor(HTTP_POST, "post"));
        SingleLogoutServiceElement result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, SOAP);
        assertThat(result).isNull();
    }

    public void nullReturnedIfNoBindingAvailable() {
        List<SingleLogoutServiceElement> endpoints = new ArrayList<SingleLogoutServiceElement>();
        SingleLogoutServiceElement result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_REDIRECT);
        assertThat(result).isNull();
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_POST);
        assertThat(result).isNull();
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, SOAP);
        assertThat(result).isNull();
    }

    public void synchronousBindingReturnedIfNoAsynchronousAvailable() {
        List<SingleLogoutServiceElement> endpoints = new ArrayList<SingleLogoutServiceElement>();
        endpoints.add(endpointFor(SOAP, "soap"));
        SingleLogoutServiceElement result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_REDIRECT);
        assertThat(result.getBinding()).isEqualTo(SOAP);
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_POST);
        assertThat(result.getBinding()).isEqualTo(SOAP);
    }

    private SingleLogoutServiceElement endpointFor(String binding, String location) {
        SingleLogoutServiceElement ret = new SingleLogoutServiceElementImpl();
        ret.setBinding(binding);
        ret.setLocation(location);
        return ret;
    }
}
