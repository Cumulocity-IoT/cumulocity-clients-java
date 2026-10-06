package com.cumulocity.microservice.customdecoders.api.configuration;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.MapperFeature;
import tools.jackson.datatype.joda.JodaModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfiguration {

    @Bean
    public JsonMapper objectMapper(){
        // Jackson 2 defaults: decoder results are a wire contract (dates as epoch
        // timestamps), which Jackson 3's own defaults would change.
        return JsonMapper.builderWithJackson2Defaults()
                .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .addModule(new JodaModule())
                .build();
    }
}
