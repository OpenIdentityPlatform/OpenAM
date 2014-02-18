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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.license;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

public class UserInputTest {

    @Test
    public void checkOutputisSameAsInput() throws IOException {
        //given
        String actualInput = "Testing";
        PrintStream out = mock(PrintStream.class);

        ByteArrayInputStream in = new ByteArrayInputStream(actualInput.getBytes());
        UserInput userInput = new UserInput(out, in);

        //when
        String receivedInput = userInput.getUserInput("Text to display");

        //then
        assertEquals(receivedInput, actualInput);
    }



}
