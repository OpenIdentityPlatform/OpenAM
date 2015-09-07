package org.forgerock.openam.scripting.rest.batch.helpers;/*
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

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ScriptResponseTest {

    @Test
    public void shouldBackAndForth() throws IOException {
        // Given
        ScriptResponse input = new ScriptResponse();
        input.putResponse("PartOne", JsonValueBuilder.toJsonValue("{ }"));
        input.putResponse("PartTwo", JsonValueBuilder.toJsonValue("{ }"));

        JsonValue jsonValue = input.build();
        ObjectMapper om = new ObjectMapper();

        // When
        ScriptResponse output = om.readValue(jsonValue.toString(), ScriptResponse.class);

        //then
        assertEquals(output.getResponses(), input.getResponses());
    }
}
