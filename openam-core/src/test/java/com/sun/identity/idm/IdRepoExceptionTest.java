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
package com.sun.identity.idm;

import static org.fest.assertions.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class IdRepoExceptionTest {

    @DataProvider
    public Object[][] regexData() {
        return new Object[][] {
                { "should match fully", "should match fully" },
                { "it's the remix to ignition : there", "it's the remix to ignition : there"},
                { "hello : there uid=", "hello"},
                { "is it me you're looking for uid=lionel,ou=people", "is it me you're looking for" },
                { "is it me you're : looking for uid=lionel,ou=people", "is it me you're" },
        };
    }

    @Test (dataProvider = "regexData")
    public void testRegex(String match, String result) {

        //given
        Object[] args = { null, null, match };
        IdRepoException idre = new IdRepoException(null, IdRepoErrorCode.LDAP_EXCEPTION,
                IdRepoErrorCode.LDAP_EXCEPTION, args);

        //when
        String answer = idre.getConstraintViolationDetails();

        //then
        assertThat(answer).isEqualTo(result);
    }



}
