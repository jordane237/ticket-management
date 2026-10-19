package com.jordane237.ticketmanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Swagger / OpenAPI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("API de gestion des tickets de support")
                .description("API developpee pour l'epreuve KFOKAM48 (Batch 2 - Phase 3). "
                        + "Endpoints : creation de ticket, liste avec filtre par statut, fermeture. "
                        + "Regles metier : RG-01 (validation du titre), RG-02 (unicite du titre), "
                        + "RG-03 (fermeture).")
                .version("1.0.0"));
    }
}
