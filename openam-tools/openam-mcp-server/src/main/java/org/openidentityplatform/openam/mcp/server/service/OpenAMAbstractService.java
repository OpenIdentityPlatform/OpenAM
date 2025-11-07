package org.openidentityplatform.openam.mcp.server.service;

import org.openidentityplatform.openam.mcp.server.config.OpenAMConfig;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public abstract class OpenAMAbstractService {

    protected final RestClient openAMRestClient;

    protected final OpenAMConfig openAMConfig;

    protected final static String DEFAULT_REALM = "root";

    public OpenAMAbstractService(RestClient openAMRestClient, OpenAMConfig openAMConfig) {
        this.openAMRestClient = openAMRestClient;
        this.openAMConfig = openAMConfig;
    }

    protected String getTokenId() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        return (String) attrs.getAttribute("tokenId", RequestAttributes.SCOPE_REQUEST);
    }

    protected String getRealmOrDefault(String realm) {
        return realm != null ? realm : DEFAULT_REALM;
    }

}
