# Contrato preliminar da interface web

Esta etapa não inicia servidor HTTP nem abre portas. Ela define apenas os limites que a implementação futura deverá respeitar.

## Segurança obrigatória

- escutar somente em loopback até que proxy reverso, TLS e modelo de implantação sejam definidos;
- armazenar somente SHA-256 do token administrativo;
- aceitar credenciais exclusivamente por `Authorization: Bearer`;
- comparar hashes em tempo constante;
- rejeitar origens não declaradas e nunca aceitar `*`;
- limitar o corpo entre 1 KiB e 1 MiB;
- não retornar stack traces, consultas SQL, caminhos locais ou credenciais;
- gerar um identificador de requisição para correlação;
- encaminhar mudanças ao serviço responsável, sem regras de negócio no controlador;
- nunca acessar API Bukkit não thread-safe a partir de threads HTTP.

## DTOs iniciais

- `ServerStatusResponse`: status, versões e instante da resposta;
- `PublicSettingsResponse`: idioma, persistência e modo debug;
- `ApiErrorResponse`: código estável, mensagem segura e identificador da requisição.

## Decisões pendentes antes da implementação HTTP

- proxy reverso e terminação TLS;
- rotação e revogação de tokens;
- limitação de requisições por origem;
- trilha de auditoria administrativa;
- proteção CSRF caso sejam utilizados cookies;
- política de sessões e níveis de acesso;
- estratégia segura para comunicação com a thread principal do servidor.
