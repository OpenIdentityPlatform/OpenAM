package org.forgerock.restlet.ext.oauth2.flow;

import com.sun.identity.shared.OAuth2Constants;
import org.fest.assertions.Condition;
import org.fest.assertions.MapAssert;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.Test;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.assertTrue;

public class SAML20BearerServerResourceTest extends AbstractFlowTest {

    @Test
    public void testValidRequest() throws Exception {

        Reference reference = new Reference("riap://component/test/oauth2/access_token");
        Request request = new Request(Method.POST, reference);
        Response response = new Response(request);


        Form parameters = new Form();
        parameters.add(OAuth2Constants.Params.GRANT_TYPE, OAuth2Constants.SAML20.GRANT_TYPE_URI);
        parameters.add(OAuth2Constants.SAML20.ASSERTION, "");
        parameters.add(OAuth2Constants.Params.CLIENT_ID, "");
        parameters.add(OAuth2Constants.Params.STATE, "random");
        request.setEntity(parameters.getWebRepresentation());

        // handle
        getClient().handle(request, response);
        assertTrue(MediaType.APPLICATION_JSON.equals(response.getEntity().getMediaType()));
        JacksonRepresentation<Map> representation =
                new JacksonRepresentation<Map>(response.getEntity(), Map.class);

        // assert
        assertThat(representation.getObject()).includes(
                MapAssert.entry(OAuth2Constants.Params.TOKEN_TYPE, OAuth2Constants.Bearer.BEARER),
                MapAssert.entry(OAuth2Constants.Params.EXPIRES_IN, 3600)).is(new Condition<Map<?, ?>>() {
            @Override
            public boolean matches(Map<?, ?> value) {
                return value.containsKey(OAuth2Constants.Params.ACCESS_TOKEN);
            }
        });
    }
}
