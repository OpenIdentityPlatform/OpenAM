package org.openidentityplatform.openam.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SearchResponseDTO<T>(@JsonProperty("result") List<T> result,
                                   @JsonProperty("resultCount") int resultCount,
                                   @JsonProperty("pagedResultsCookie") String pagedResultsCookie,
                                   @JsonProperty("totalPagedResultsPolicy") String totalPagedResultsPolicy,
                                   @JsonProperty("totalPagedResults") int totalPagedResults,
                                   @JsonProperty("remainingPagedResults") int remainingPagedResults) {
}
