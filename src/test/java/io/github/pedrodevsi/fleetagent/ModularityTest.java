package io.github.pedrodevsi.fleetagent;


import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class ModularityTest {

    @Test
    void shouldRespectModuleBoundaries() {
        ApplicationModules.of(FleetAgentApplication.class).verify();
    }
}
