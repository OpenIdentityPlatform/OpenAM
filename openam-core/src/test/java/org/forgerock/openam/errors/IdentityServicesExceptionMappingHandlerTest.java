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
package org.forgerock.openam.errors;

import static org.mockito.BDDMockito.*;
import static org.testng.Assert.*;

import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idsvcs.GeneralFailure;
import com.sun.identity.idsvcs.IdServicesException;
import com.sun.identity.idsvcs.ObjectNotFound;
import org.testng.annotations.Test;

public class IdentityServicesExceptionMappingHandlerTest {

    private IdentityServicesExceptionMappingHandler handler = new IdentityServicesExceptionMappingHandler();

    @Test
    public void unknownErrorReturnsGeneralFailureWithMessage() {
        //given
        IdRepoException mockException = mock(IdRepoException.class);
        given(mockException.getErrorCode()).willReturn("-1");
        given(mockException.getMessage()).willReturn("Message");

        //when
        IdServicesException exp = handler.handleError(mockException);

        //then
        assertTrue(exp instanceof GeneralFailure);
        assertTrue(exp.getMessage().equals("Message"));
    }

    @Test
    public void knownLdapErrorReturns() {
        //given
        IdRepoException mockException = mock(IdRepoException.class);
        given(mockException.getLDAPErrorCode()).willReturn(String.valueOf(IdentityServicesException.LDAP_NO_SUCH_OBJECT));

        //when
        IdServicesException exp = handler.handleError(mockException);

        //then
        assertTrue(exp instanceof ObjectNotFound);
    }

    @Test
    public void knownErrorReturns() {
        //given
        IdRepoException mockException = mock(IdRepoException.class);
        given(mockException.getErrorCode()).willReturn(String.valueOf(IdentityServicesException.GENERAL_OBJECT_NOT_FOUND));

        //when
        IdServicesException exp = handler.handleError(mockException);

        //then
        assertTrue(exp instanceof ObjectNotFound);
    }

}
