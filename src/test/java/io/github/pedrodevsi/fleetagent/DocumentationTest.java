package io.github.pedrodevsi.fleetagent;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

public class DocumentationTest {

    private final ApplicationModules modules =
            ApplicationModules.of(FleetAgentApplication.class);

    @Test
    void generateDocumentation() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}
