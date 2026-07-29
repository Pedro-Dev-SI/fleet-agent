// ai/package-info.java
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "customer::api",
                "rental::api"
        }
)
package com.br.langchain4j.ai;