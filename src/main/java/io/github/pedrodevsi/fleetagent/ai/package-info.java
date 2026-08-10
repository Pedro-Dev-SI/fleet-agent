// ai/package-info.java
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "customer::api",
                "rental::api"
        }
)
package io.github.pedrodevsi.fleetagent.ai;