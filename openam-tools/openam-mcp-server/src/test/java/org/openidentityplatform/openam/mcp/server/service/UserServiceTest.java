package org.openidentityplatform.openam.mcp.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openidentityplatform.openam.mcp.server.model.SearchResponseDTO;
import org.openidentityplatform.openam.mcp.server.model.User;
import org.openidentityplatform.openam.mcp.server.model.UserDTO;
import org.springframework.core.ParameterizedTypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class UserServiceTest extends OpenAMServiceTest {

    UserService userService = null;

    @BeforeEach()
    @Override
    public void setupMocks() {
        super.setupMocks();
        userService = new UserService(restClient, openAMConfig);

    }

    @Test
    void getUsersTest() throws Exception {

        InputStream is = getClass().getClassLoader().getResourceAsStream("users/users-list-response.json");
        SearchResponseDTO<UserDTO> userSearchResponse = objectMapper.readValue(is, new TypeReference<>() {});
        when(responseSpec.body(eq(new ParameterizedTypeReference<SearchResponseDTO<UserDTO>>() {}))).thenReturn(userSearchResponse);

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