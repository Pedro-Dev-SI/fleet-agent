package com.br.langchain4j;


import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class ModularityTest {

    @Test
    void shouldRespectModuleBoundaries() {
        ApplicationModules.of(Langchain4jApplication.class).verify();
    }
}
