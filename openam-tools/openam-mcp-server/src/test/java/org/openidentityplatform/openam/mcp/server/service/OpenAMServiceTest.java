package org.openidentityplatform.openam.mcp.server.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.openidentityplatform.openam.mcp.server.config.OpenAMConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class OpenAMServiceTest {
    protected final ObjectMapper objectMapper;

    protected final OpenAMConfig openAMConfig;

    public OpenAMServiceTest() {
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.openAMConfig = new OpenAMConfig(
                null,
                false,
                null,
                null,
                "iPlanetDirectoryPro",
                null,
                null);
    }


    RestClient restClient;
    RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    RestClient.RequestBodyUriSpec requestBodyUriSpec;
    RestClient.RequestBodySpec requestBodySpec;
    RestClient.ResponseSpec responseSpec;

    @BeforeEach
    public void setupMocks() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setAttribute("tokenId", "test-token-id");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));

        restClient = mock(RestClient.class);
        requestHeadersUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);


        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        when(restClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(anyMap())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.header(anyString(), anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);

        when(restClient.delete()).thenReturn(requestHeadersUriSpec);
    }
}
