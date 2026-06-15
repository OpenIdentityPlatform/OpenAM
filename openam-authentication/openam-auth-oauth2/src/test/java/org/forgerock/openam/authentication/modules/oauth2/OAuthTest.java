package org.forgerock.openam.authentication.modules.oauth2;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

public class OAuthTest {

    @Test
    public void shouldRemoveCredentialAndAccountStatusAttributesFromUpdates() {
        Map<String, Set<String>> attributes = new HashMap<>();
        attributes.put("userPassword", Collections.singleton("password"));
        attributes.put("UserPassword", Collections.singleton("password"));
        attributes.put("userpassword", Collections.singleton("password"));
        attributes.put("inetuserstatus", Collections.singleton("Active"));
        attributes.put("inetUserStatus", Collections.singleton("Active"));
        attributes.put("INETUSERSTATUS", Collections.singleton("Active"));
        attributes.put("cn", Collections.singleton("Alice User"));

        OAuth.removeRestrictedAccountUpdateAttributes(attributes);

        assertEquals(attributes.keySet(), Collections.singleton("cn"));
        assertFalse(attributes.containsKey("userPassword"));
        assertFalse(attributes.containsKey("inetuserstatus"));
    }
}

