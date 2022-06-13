package com.cumulocity.sdk.client.cep;

import com.cumulocity.rest.representation.cep.CepModuleRepresentation;
import com.cumulocity.sdk.client.common.JavaSdkITBase;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

//Disabled till fixing https://cumulocity.atlassian.net/browse/MTM-46135
@Disabled
public class CepApiIT extends JavaSdkITBase {

    final CepApi cepApi = platform.getCepApi();

    @BeforeAll
    public static void makeSureCepEsperIsSubscribed() throws IOException {
        applicationApi.unsubscribeApamaAndSubscribeEsper();
    }

    @AfterEach
    public void cleanup() {
        final PagedCepModuleCollectionRepresentation cep = cepApi.getModules().get();
        final Iterable<CepModuleRepresentation> it = cep.allPages();
        for (CepModuleRepresentation cepModule : Lists.newLinkedList(it)) {
            cepApi.delete(cepModule);
        }
    }

    @Test
    void shouldCreateCepModule() throws IOException {
        //Given
        final String cepModuleFile = cepModuleFile("cep/test-module.epl");
        //When
        final CepModuleRepresentation cepModule = cepApi.create(cepModuleFile);
        //Then
        assertThat(cepModule).isNotNull();
        assertThat(cepModule.getId()).isNotNull().isNotEmpty();
    }

    @Test
    void shouldDeleteCepModule() throws IOException {
        //Given
        final CepModuleRepresentation cepModule = cepApi.create(cepModuleFile("cep/test-module.epl"));
        //When
        cepApi.delete(cepModule);
        //Then
        assertThat(cepModule).isNotNull();
        assertThat(cepModule.getId()).isNotNull().isNotEmpty();
    }

    private String cepModuleFile(String module) throws IOException {
        URL resource = Resources.getResource(module);
        return Resources.toString(resource, Charsets.UTF_8);
    }

}
