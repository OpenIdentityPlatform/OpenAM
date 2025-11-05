package org.openidentityplatform.openam.mcp.server.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openidentityplatform.openam.mcp.server.config.OpenAMConfig;
import org.openidentityplatform.openam.mcp.server.model.User;
import org.openidentityplatform.openam.mcp.server.model.UserDTO;
import org.openidentityplatform.openam.mcp.server.model.UserSearchResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final ObjectMapper objectMapper;

    public UserServiceTest() {
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    UserService userService = null;
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

        userService = new UserService(restClient, new OpenAMConfig(
                null,
                false,
                null,
                null,
                "iPlanetDirectoryPro",
                null,
                null
        ));

    }

    @Test
    void getUsersTest() throws Exception {

        InputStream is = getClass().getClassLoader().getResourceAsStream("users/users-list-response.json");
        UserSearchResponse userSearchResponse = objectMapper.readValue(is, UserSearchResponse.class);

        when(responseSpec.body(UserSearchResponse.class)).thenReturn(userSearchResponse);

        List<User> userList = userService.getUsers(null, null);
        assertEquals(userList.size(), 2);
    }

    @Test
    void setUserAttributeTest() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("users/user-response.json");
        UserDTO userDTO = objectMapper.readValue(is, UserDTO.class);

        when(responseSpec.body(UserDTO.class)).thenReturn(userDTO);

        User user = userService.setUserAttribute(null, "demo", "familyName", "Flintstone");
        assertNotNull(user);
    }

    @Test
    void setUserPassword() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("users/user-response.json");
        UserDTO userDTO = objectMapper.readValue(is, UserDTO.class);

        when(responseSpec.body(UserDTO.class)).thenReturn(userDTO);

        User user = userService.setUserPassword(null, "demo", "passw0rd");
        assertNotNull(user);
    }

    @Test
    void deleteUser() {
        when(responseSpec.body(eq(new ParameterizedTypeReference<Map<String, String>>() {}))).thenReturn(Map.of("success", "true"));

        Map<String, String> result = userService.deleteUser(null, "demo");
        assertEquals("true", result.get("success"));
    }
}