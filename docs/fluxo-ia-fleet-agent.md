# Fluxo de IA — Fleet Agent

O diagrama destaca o fluxo de inteligência artificial do projeto. A locadora funciona como o cenário de negócio no qual o LangChain4j combina contexto conversacional, RAG e chamadas de ferramentas, mantendo as regras e alterações de estado sob responsabilidade do backend Java.

```mermaid
flowchart LR
    Client(["Cliente"])

    subgraph Fleet["Fleet Agent — Spring Boot Modular Monolith"]
        direction LR

        API["POST /api/assistant"]

        subgraph AI["Módulo AI"]
            direction TB
            Guardrails["Guardrails<br/>entrada e saída"]
            Memory["Chat Memory<br/>sessionId · 10 mensagens"]
            Agent["AI Service<br/>LangChain4j"]
            Model["LLM<br/>Ollama ou Gemini"]

            Guardrails --> Agent
            Memory <--> Agent
            Agent <--> Model
        end

        subgraph ToolLayer["Tool Calling"]
            direction TB
            ToolRouter["Tools escolhidas pela LLM"]
            CustomerTools["Customer Tools<br/>buscar · cadastrar"]
            RentalTools["Rental Tools<br/>disponibilidade · reservar · cancelar"]
            QuotationTools["Quotation Tools<br/>calcular cotação"]

            ToolRouter --> CustomerTools
            ToolRouter --> RentalTools
            ToolRouter --> QuotationTools
        end

        subgraph JavaBackend["Regras de negócio em Java"]
            direction TB
            Customer["Módulo Customer"]
            Rental["Módulo Rental"]
            Notification["Módulo Notification"]
        end

        subgraph Data["PostgreSQL 17"]
            direction TB
            Relational[("Dados relacionais<br/>clientes · veículos · reservas")]
            Vector[("pgvector<br/>embeddings do conhecimento")]
        end

        API --> Agent
        Agent -->|"Tool Calling"| ToolRouter

        CustomerTools -->|"customer::api"| Customer
        RentalTools -->|"rental::api"| Rental
        QuotationTools -->|"rental::api"| Rental

        Customer --> Relational
        Rental --> Relational
        Rental -. "Eventos AFTER_COMMIT" .-> Notification

        subgraph Knowledge["Módulo Knowledge — RAG"]
            direction LR
            Docs["Documentos Markdown"]
            Chunking["Chunks<br/>300 + overlap 30"]
            Embeddings["Embeddings<br/>nomic-embed-text"]
            Retriever["Busca semântica<br/>top 3 · score ≥ 0.65"]

            Docs --> Chunking --> Embeddings
            Retriever -->|"Contexto + fontes"| Agent
        end

        Embeddings --> Vector
        Vector --> Retriever
    end

    Client --> API

    classDef client fill:#10242b,stroke:#38bdf8,color:#e6f7ff,stroke-width:2px;
    classDef ai fill:#241b3a,stroke:#a78bfa,color:#f5f3ff,stroke-width:2px;
    classDef aiSupport fill:#191f2d,stroke:#7c8aa5,color:#e5e7eb;
    classDef tool fill:#342514,stroke:#f59e0b,color:#fff7e6,stroke-width:2px;
    classDef backend fill:#14291e,stroke:#4ade80,color:#ecfdf5,stroke-width:2px;
    classDef data fill:#17212b,stroke:#60a5fa,color:#eff6ff,stroke-width:2px;
    classDef rag fill:#102a30,stroke:#22d3ee,color:#ecfeff,stroke-width:2px;

    class Client client;
    class Agent ai;
    class Guardrails,Memory,Model aiSupport;
    class ToolRouter,CustomerTools,RentalTools,QuotationTools tool;
    class Customer,Rental,Notification backend;
    class Relational,Vector data;
    class Docs,Chunking,Embeddings,Retriever rag;

    style Fleet fill:#0b1017,stroke:#526173,stroke-width:2px,color:#f8fafc
    style AI fill:#101522,stroke:#6d5cae,color:#f8fafc
    style ToolLayer fill:#17130d,stroke:#a66a13,color:#f8fafc
    style JavaBackend fill:#0e1812,stroke:#318c50,color:#f8fafc
    style Data fill:#0e151d,stroke:#3e6f9e,color:#f8fafc
    style Knowledge fill:#0c191d,stroke:#21899a,color:#f8fafc
```

## Leitura do fluxo

1. O cliente envia uma mensagem para a API do assistente.
2. O AI Service combina o histórico da sessão, os guardrails e o modelo configurado.
3. O módulo Knowledge recupera contexto semântico no PGVector e devolve os trechos relevantes com suas fontes.
4. Quando a intenção exige uma operação, a LLM seleciona uma tool.
5. A tool chama um contrato público dos módulos Java; ela não implementa a regra de negócio.
6. Customer e Rental validam e persistem as alterações no PostgreSQL.
7. Mudanças em reservas geram eventos internos consumidos pelo módulo Notification após o commit.

> A LLM interpreta a intenção e escolhe ferramentas. O backend Java continua sendo a autoridade sobre regras, validações e estado do sistema.
