package net.mbi.wcloud.dispatch.solver.framework.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_BEARER = "bearerAuth";

    @Bean
    public OpenAPI dispatchSolverOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("dispatch-solver API")
                        .description("Dispatch Solver - 调度求解服务接口文档")
                        .version("1.0.0")
                        .contact(new Contact().name("dispatch-solver team")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_BEARER,
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_BEARER));
    }
}
