package br.inatel.tcc.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "TCC API",
        version = "1.0.0",
        description = "Documentação interativa da API do backend do TCC.",
        contact = Contact(name = "Desenvolvedor", email = "dimitri.schulz@ges.inatel.br")
    ),
    security = [SecurityRequirement(name = "bearerAuth")]
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Insira o token JWT retornado no login. Obs: Não precisa digitar 'Bearer ' na frente, apenas o token."
)
class SwaggerConfig
