package org.openidentityplatform.openam.mcp.server;

import org.openidentityplatform.openam.mcp.server.config.OpenAMConfig;
import org.openidentityplatform.openam.mcp.server.service.UserService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OpenAmMcpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpenAmMcpServerApplication.class, args);
    }

    @Bean
    public RestClient getOpenAMRestClient(OpenAMConfig openAMConfig) {
        return RestClient.builder().baseUrl(openAMConfig.url())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json").build();
    }

    @Bean
    public ToolCallbackProvider getTools(UserService userService) {
        return MethodToolCallbackProvider.builder().toolObjects(userService).build();
    }
}


