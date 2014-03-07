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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.oauth2.openid;

import static com.sun.identity.shared.OAuth2Constants.ShortClientAttributeNames.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.constructor;
import static org.powermock.api.support.membermodification.MemberMatcher.constructorsDeclaredIn;
import static org.powermock.api.support.membermodification.MemberMatcher.defaultConstructorIn;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;

import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.oauth2.model.Client;
import org.forgerock.openam.oauth2.provider.ClientDAOFactory;
import org.forgerock.openam.oauth2.provider.OAuth2ProviderSettings;
import org.forgerock.openam.oauth2.provider.impl.OpenAMClientDAO;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.powermock.reflect.Whitebox;
import org.restlet.Request;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.MediaType;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@PrepareForTest({ClientDAOFactory.class, OAuth2Utils.class, ChallengeResponse.class, OpenAMClientDAO.class, SSOToken.class})
public class ConnectClientRegistrationTest extends PowerMockTestCase {
    String inputJson;
    Map<String, Object> testInput;
    Set<String> idTokenSigningAlgsSupported;
    Set<String> responseTypesSupported;
    Set<String> claimsSupported;

    public static final String APPLICATION_TYPE_DEFAULT = "web";
    public static List<String> RESPONSE_TYPES_DEFAULT = new ArrayList<String>();
    public static List<String> CLIENT_DESCRIPTION_DEFAULT = new ArrayList<String>();;
    public static final String CLIENT_ID_DEFAULT = "client id";
    public static final String CLIENT_NAME_DEFAULT = "client name";
    public static final String CLIENT_SECRET_DEFAULT = "client secret";
    public static final String CLIENT_SESSION_URI_DEFAULT = "client session uri";
    public static final String CLIENT_TYPE_DEFAULT = "public";
    public static final String CLIENT_URI_DEFAULT = "client uri";
    public static List<String> CONTACTS_DEFAULT = new ArrayList<String>();
    public static List<String> DEFAULT_ACR_VALUES_DEFAULT = new ArrayList<String>();
    public static final String DEFAULT_MAX_AGE_DEFAULT = "100";
    public static List<String> DEFAULT_SCOPES_DEFAULT = new ArrayList<String>();
    public static List<String> SCOPES_DEFAULT = new ArrayList<String>();
    public static List<String> DISPLAY_NAME_DEFAULT = new ArrayList<String>();
    public static List<String> GRANT_TYPES_DEFAULT = new ArrayList<String>();
    public static final String ID_TOKEN_ENCRYPTED_RESONSE_ENC_DEFAULT = "HS256";
    public static final String ID_TOKEN_ENCRYPTED_RESPONSE_ALG_DEFAULT = "HS256";
    public static final String ID_TOKEN_SIGNED_RESPONSE_ALG_DEFAULT = "HS256";
    public static final String INITIATE_LOGIN_URI_DEFAULT = "init login uri";
    public static final String JWKS_DEFAULT = "jwks uri1";
    public static final String JWKS_URI_DEFAULT = "jwks uri2";
    public static final String REQUEST_OBJECT_ENCRYPTION_ALG_DEFAULT = "HS256";
    public static final String REQUEST_OBJECT_ENCRYPTION_ENC_DEFAULT = "HS256";
    public static final String REQUEST_OBJECT_SIGNING_ALG_DEFAULT = "HS256";
    public static List<String> REQUEST_URIS_DEFAULT = new ArrayList<String>();
    public static final String REQUIRE_AUTH_TIME_DEFAULT = "true";
    public static final String LOGO_URI_DEFAULT = "logo uri";
    public static final String POLICY_URI_DEFAULT = "policy uri";
    public static final String POST_LOGOUT_REDIRECT_URIS_DEFAULT = "post logout redirect uri";
    public static final String REALM_DEFAULT = "realm";
    public static List<String> REDIRECT_URIS_DEFAULT = new ArrayList<String>();
    public static final String SECTOR_IDENTIFIER_URI_DEFAULT = "sector id uri";
    public static final String SUBJECT_TYPE_DEFAULT = "Public";
    public static final String TOKEN_ENDPOINT_AUTH_METHOD_DEFAULT = "client_secret_post";
    public static final String TOKEN_ENDPOINT_AUTH_SIGNING_ALG_DEFAULT = "HS256";
    public static final String TOS_URI_DEFAULT = "tos uri";
    public static final String USERINFO_ENCRYPTED_RESONSE_ENC_DEFAULT = "HS256";
    public static final String USERINFO_ENCRYPTED_RESPONSE_ALG_DEFAULT = "HS256";
    public static final String USERINFO_SIGNED_RESPONSE_ALG_DEFAULT = "HS256";
    public static final String REGISTRATION_ACCESS_TOKEN_DEFAULT = "registration access token";

    @BeforeClass
    public void oneTimeSetup() {
        RESPONSE_TYPES_DEFAULT.add("code");
        RESPONSE_TYPES_DEFAULT.add("token");
        RESPONSE_TYPES_DEFAULT.add("id_token");
        RESPONSE_TYPES_DEFAULT.add("code token");
        RESPONSE_TYPES_DEFAULT.add("code token id_token");
        RESPONSE_TYPES_DEFAULT.add("token id_token");
        CONTACTS_DEFAULT.add("contact1");
        CONTACTS_DEFAULT.add("contact2");
        CLIENT_DESCRIPTION_DEFAULT.add("Description1");
        CLIENT_DESCRIPTION_DEFAULT.add("Description2");
        DEFAULT_ACR_VALUES_DEFAULT.add("acr1");
        DEFAULT_ACR_VALUES_DEFAULT.add("acr2");
        DEFAULT_SCOPES_DEFAULT.add("phone");
        DEFAULT_SCOPES_DEFAULT.add("email");
        DEFAULT_SCOPES_DEFAULT.add("address");
        DEFAULT_SCOPES_DEFAULT.add("openid");
        DEFAULT_SCOPES_DEFAULT.add("profile");
        SCOPES_DEFAULT.add("phone");
        SCOPES_DEFAULT.add("email");
        SCOPES_DEFAULT.add("openid");
        SCOPES_DEFAULT.add("profile");
        SCOPES_DEFAULT.add("address");
        DISPLAY_NAME_DEFAULT.add("name1");
        DISPLAY_NAME_DEFAULT.add("name2");
        GRANT_TYPES_DEFAULT.add("grant1");
        GRANT_TYPES_DEFAULT.add("grant2");
        REQUEST_URIS_DEFAULT.add("uri1");
        REQUEST_URIS_DEFAULT.add("uri1");
        REDIRECT_URIS_DEFAULT.add("redirect1");
        REDIRECT_URIS_DEFAULT.add("redirect1");

        testInput = new HashMap<String, Object>();
        testInput.put(APPLICATION_TYPE.getType(), APPLICATION_TYPE_DEFAULT);
        testInput.put(RESPONSE_TYPES.getType(), RESPONSE_TYPES_DEFAULT);
        testInput.put(CLIENT_DESCRIPTION.getType(), CLIENT_DESCRIPTION_DEFAULT);
        testInput.put(CLIENT_ID.getType(), CLIENT_ID_DEFAULT);
        testInput.put(CLIENT_NAME.getType(), CLIENT_NAME_DEFAULT);
        testInput.put(CLIENT_SECRET.getType(), CLIENT_SECRET_DEFAULT);
        testInput.put(CLIENT_SESSION_URI.getType(), CLIENT_SESSION_URI_DEFAULT);
        testInput.put(CLIENT_TYPE.getType(), CLIENT_TYPE_DEFAULT);
        testInput.put(CLIENT_URI.getType(), CLIENT_URI_DEFAULT);
        testInput.put(CONTACTS.getType(), CONTACTS_DEFAULT);
        testInput.put(DEFAULT_ACR_VALUES.getType(), DEFAULT_ACR_VALUES_DEFAULT);
        testInput.put(DEFAULT_MAX_AGE.getType(), DEFAULT_MAX_AGE_DEFAULT);
        testInput.put(DEFAULT_SCOPES.getType(), DEFAULT_SCOPES_DEFAULT);
        testInput.put(SCOPES.getType(), SCOPES_DEFAULT);
        testInput.put(DISPLAY_NAME.getType(), DISPLAY_NAME_DEFAULT);
        testInput.put(GRANT_TYPES.getType(), GRANT_TYPES_DEFAULT);
        testInput.put(ID_TOKEN_ENCRYPTED_RESONSE_ENC.getType(), ID_TOKEN_ENCRYPTED_RESONSE_ENC_DEFAULT);
        testInput.put(ID_TOKEN_ENCRYPTED_RESPONSE_ALG.getType(), ID_TOKEN_ENCRYPTED_RESPONSE_ALG_DEFAULT);
        testInput.put(ID_TOKEN_SIGNED_RESPONSE_ALG.getType(), ID_TOKEN_SIGNED_RESPONSE_ALG_DEFAULT);
        testInput.put(INITIATE_LOGIN_URI.getType(), INITIATE_LOGIN_URI_DEFAULT);
        testInput.put(JWKS.getType(), JWKS_DEFAULT);
        testInput.put(JWKS_URI.getType(), JWKS_URI_DEFAULT);
        testInput.put(REQUEST_OBJECT_ENCRYPTION_ALG.getType(), REQUEST_OBJECT_ENCRYPTION_ALG_DEFAULT);
        testInput.put(REQUEST_OBJECT_ENCRYPTION_ENC.getType(), REQUEST_OBJECT_ENCRYPTION_ENC_DEFAULT);
        testInput.put(REQUEST_OBJECT_SIGNING_ALG.getType(), REQUEST_OBJECT_SIGNING_ALG_DEFAULT);
        testInput.put(REQUEST_URIS.getType(), REQUEST_URIS_DEFAULT);
        testInput.put(REQUIRE_AUTH_TIME.getType(), REQUIRE_AUTH_TIME_DEFAULT);
        testInput.put(LOGO_URI.getType(), LOGO_URI_DEFAULT);
        testInput.put(POLICY_URI.getType(), POLICY_URI_DEFAULT);
        testInput.put(POST_LOGOUT_REDIRECT_URIS.getType(), POST_LOGOUT_REDIRECT_URIS_DEFAULT);
        testInput.put(REALM.getType(), REALM_DEFAULT);
        testInput.put(REDIRECT_URIS.getType(), REDIRECT_URIS_DEFAULT);
        testInput.put(SECTOR_IDENTIFIER_URI.getType(), SECTOR_IDENTIFIER_URI_DEFAULT);
        testInput.put(SUBJECT_TYPE.getType(), SUBJECT_TYPE_DEFAULT);
        testInput.put(TOKEN_ENDPOINT_AUTH_METHOD.getType(), TOKEN_ENDPOINT_AUTH_METHOD_DEFAULT);
        testInput.put(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.getType(), TOKEN_ENDPOINT_AUTH_SIGNING_ALG_DEFAULT);
        testInput.put(TOS_URI.getType(), TOS_URI_DEFAULT);
        testInput.put(USERINFO_ENCRYPTED_RESONSE_ENC.getType(), USERINFO_ENCRYPTED_RESONSE_ENC_DEFAULT);
        testInput.put(USERINFO_ENCRYPTED_RESPONSE_ALG.getType(), USERINFO_ENCRYPTED_RESPONSE_ALG_DEFAULT);
        testInput.put(USERINFO_SIGNED_RESPONSE_ALG.getType(), USERINFO_SIGNED_RESPONSE_ALG_DEFAULT);
        testInput.put(REGISTRATION_ACCESS_TOKEN.getType(), REGISTRATION_ACCESS_TOKEN_DEFAULT);

        JsonValue json = new JsonValue(new HashMap<String, Object>());
        for ( Map.Entry<String, Object> entry : testInput.entrySet()) {
            json.put(entry.getKey().toString(), entry.getValue());
        }

        inputJson = json.toString();

        idTokenSigningAlgsSupported = new HashSet<String>();
        idTokenSigningAlgsSupported.add("HS256");

        responseTypesSupported = new HashSet<String>();
        responseTypesSupported.add("code");
        responseTypesSupported.add("token");
        responseTypesSupported.add("id_token");
        responseTypesSupported.add("code token");
        responseTypesSupported.add("token id_token");
        responseTypesSupported.add("code token id_token");

        claimsSupported = new HashSet<String>();
        claimsSupported.add("phone");
        claimsSupported.add("email");
        claimsSupported.add("address");
        claimsSupported.add("openid");
        claimsSupported.add("profile");

    }

    @Test
    public void testShouldCreateClient() throws IOException {

        //given
        Representation representation = PowerMockito.mock(Representation.class);
        Request request = PowerMockito.mock(Request.class);
        ChallengeResponse challengeResponse = PowerMockito.mock(ChallengeResponse.class);
        OAuth2ProviderSettings oAuth2ProviderSettings = PowerMockito.mock(OAuth2ProviderSettings.class);
        PowerMockito.mockStatic(ClientDAOFactory.class);
        PowerMockito.mockStatic(OAuth2Utils.class);
        PowerMockito.mockStatic(AdminTokenAction.class);
        PowerMockito.mockStatic(AccessController.class);
        SSOToken token = PowerMockito.mock(SSOToken.class);

        AdminTokenAction adminTokenAction = PowerMockito.mock(AdminTokenAction.class);

        Whitebox.setInternalState(AdminTokenAction.class, "instance", adminTokenAction);

        when(AccessController.doPrivileged(any(AdminTokenAction.class))).thenReturn(token);

        suppress(constructorsDeclaredIn(OpenAMClientDAO.class));
        //OpenAMClientDAO openAMClientDAO = Whitebox.newInstance(OpenAMClientDAO.class);
        OpenAMClientDAO openAMClientDAO = PowerMockito.mock(OpenAMClientDAO.class);

        when(representation.getMediaType()).thenReturn(MediaType.APPLICATION_JSON);

        when(ClientDAOFactory.newOpenAMClientDAO(anyString(),
                any(Request.class),
                any(SSOToken.class))).thenReturn(openAMClientDAO);
        doNothing().when(openAMClientDAO).create(any(Client.class));

        when(oAuth2ProviderSettings.getTheIDTokenSigningAlgorithmsSupported()).thenReturn(idTokenSigningAlgsSupported);
        when(oAuth2ProviderSettings.getResponseTypes()).thenReturn(responseTypesSupported);
        when(oAuth2ProviderSettings.getSupportedClaims()).thenReturn(claimsSupported);

        when(OAuth2Utils.getRealm(any(Request.class))).thenReturn("/");
        when(OAuth2Utils.getSettingsProvider(any(Request.class))).thenReturn(oAuth2ProviderSettings);

        when(request.getChallengeResponse()).thenReturn(challengeResponse);
        when(challengeResponse.getRawValue()).thenReturn("ACCESS_TOKEN");


        ConnectClientRegistration connectClientRegistration = PowerMockito.spy(new ConnectClientRegistration());
        when(connectClientRegistration.getRequest()).thenReturn(request);

        JsonRepresentation jsonRepresentation = new JsonRepresentation(inputJson);

        //when
        Representation outputRepresentation = connectClientRegistration.createClient(jsonRepresentation);

        JacksonRepresentation<Map> rep = new JacksonRepresentation<Map>(outputRepresentation, Map.class);
        JsonValue input = new JsonValue(rep.getObject());

        //then
        for (Map.Entry<String,Object> entry : testInput.entrySet()) {
            JsonValue value = input.get(entry.getKey());
            if (value.isNull()) {
                continue;
            } else if (value.isString()) {
                value.asString().equalsIgnoreCase(entry.getValue().toString());
            } else if (value.isList()) {
                value.asList(String.class).containsAll((List<String>)entry.getValue());
            } else {
                assert(false);
            }
        }
    }
}
