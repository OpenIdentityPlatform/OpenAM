package org.openidentityplatform.openam.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record UserDTO(
        @JsonProperty("username")
        String userName,

        @JsonProperty("sn")
        List<String> sn,

        @JsonProperty("givenName")
        List<String> givenName,

        @JsonProperty("cn")
        List<String> cn,

        @JsonProperty("employeeNumber")
        List<String> employeeNumber,

        @JsonProperty("telephoneNumber")
        List<String> telephoneNumber,

        @JsonProperty("mail")
        List<String> mail

) {}

