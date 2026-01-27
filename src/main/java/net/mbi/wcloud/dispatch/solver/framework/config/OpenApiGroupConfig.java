package net.mbi.wcloud.dispatch.solver.framework.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiGroupConfig {

    @Bean
    public GroupedOpenApi planApi() {
        return GroupedOpenApi.builder()
                .group("plan")
                .packagesToScan("net.mbi.wcloud.dispatch.solver.controller.plan")
                .build();
    }

    @Bean
    public GroupedOpenApi taskApi() {
        return GroupedOpenApi.builder()
                .group("task")
                .packagesToScan("net.mbi.wcloud.dispatch.solver.controller.task")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .packagesToScan("net.mbi.wcloud.dispatch.solver.controller.admin")
                .build();
    }
}
