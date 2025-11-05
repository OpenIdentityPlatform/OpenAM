package org.openidentityplatform.openam.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record UserSearchResponse(@JsonProperty("result") List<UserDTO> result,
        @JsonProperty("resultCount") int resultCount,
        @JsonProperty("pagedResultsCookie") String pagedResultsCookie,
        @JsonProperty("totalPagedResultsPolicy") String totalPagedResultsPolicy,
        @JsonProperty("totalPagedResults") int totalPagedResults,
        @JsonProperty("remainingPagedResults") int remainingPagedResults)
{
}

