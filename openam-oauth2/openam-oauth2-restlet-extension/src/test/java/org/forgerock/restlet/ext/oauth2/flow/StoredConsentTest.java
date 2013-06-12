package org.forgerock.restlet.ext.oauth2.flow;


import com.sun.identity.idm.AMIdentity;
import org.forgerock.openam.oauth2.provider.OAuth2ProviderSettings;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.testng.PowerMockTestCase;
import org.restlet.Request;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

@PrepareForTest({OAuth2Utils.class, AMIdentity.class})
@SuppressStaticInitializationFor("org.forgerock.openam.oauth2.utils.OAuth2Utils")
public class StoredConsentTest extends PowerMockTestCase {

    @Test
    public void testStoreConsent() throws Exception{

        PowerMockito.mockStatic(OAuth2Utils.class);
        AMIdentity id = PowerMockito.mock(AMIdentity.class);
        OAuth2ProviderSettings settings = PowerMockito.mock(OAuth2ProviderSettings.class);

        String userID = "testUser";
        String clientID = "testClient";
        String scopes = "scope1 scope2";
        String attribute = "attribute";

        //create the expected attribute set that will be sent to the AMIdentity
        Map expectedAttrs = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(clientID + " " + scopes);

        expectedAttrs.put(attribute, set);

        doNothing().when(id).setAttributes(expectedAttrs);
        doNothing().when(id).store();

        when(OAuth2Utils.getIdentity(anyString(), anyString())).thenReturn(id);
        when(OAuth2Utils.getSettingsProvider(any(Request.class))).thenReturn(settings);
        when(settings.getSharedConsentAttributeName()).thenReturn(attribute);

        AuthorizeServerResource resource = new AuthorizeServerResource();
        resource.saveConsent(userID, clientID, scopes);
    }

    @Test
    public void testSavedConsent() throws Exception{

        PowerMockito.mockStatic(OAuth2Utils.class);
        AMIdentity id = PowerMockito.mock(AMIdentity.class);
        OAuth2ProviderSettings settings = PowerMockito.mock(OAuth2ProviderSettings.class);

        String userID = "testUser";
        String clientID = "testClient";
        Set<String> scopes = new HashSet<String>();
        scopes.add("scope1");
        scopes.add("scope2");
        String attribute = "attribute";

        //create the expected attribute set that will be sent to the AMIdentity
        Set<String> set = new HashSet<String>();
        set.add(clientID + " " + "scope1" + " " + "scope2");

        when(id.getAttribute(attribute)).thenReturn(set);

        when(OAuth2Utils.getIdentity(anyString(), anyString())).thenReturn(id);
        when(OAuth2Utils.getSettingsProvider(any(Request.class))).thenReturn(settings);
        when(settings.getSharedConsentAttributeName()).thenReturn(attribute);
        when(OAuth2Utils.getRealm(any(Request.class))).thenReturn("/");

        AuthorizeServerResource resource = new AuthorizeServerResource();
        assert (resource.savedConsent(userID, clientID, scopes) == true);
    }

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new org.powermock.modules.testng.PowerMockObjectFactory();
    }
}
