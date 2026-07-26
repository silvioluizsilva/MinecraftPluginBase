# PluginBase

Base Maven profissional e extensível para plugins Paper, criada por **Sílvio Luiz da Silva**.

Versão publicada: **0.0.1**. O nome do artefato é `pluginbase-0.0.1.jar`.

## Requisitos

- Paper 26.2
- JDK 25 ou 26 (compilação com `--release 25`)
- Maven 3.9+
- MySQL 8.0+ (opcional)

> O Paper adotou uma nova numeração. A API `26.2` corresponde ao Paper `26.2`, e não ao Minecraft `1.21.8`. Para um servidor 1.21.8 seria necessário usar `paper-api:1.21.8-R0.1-SNAPSHOT`, `api-version: '1.21.8'` e Java 21.

## Compilação

```bash
mvn clean verify
```

O plugin compilado será criado em `target/pluginbase-0.0.1.jar`.

O POM pai aceita JDK 25 e 26, mantendo os bytecodes compatíveis com Java 25. O servidor também precisa executar uma versão de Java compatível.

## Instalação

1. Compile o projeto.
2. Copie `target/pluginbase-0.0.1.jar` para a pasta `plugins` do servidor.
3. Inicie o Paper.
4. Ajuste `plugins/PluginBase/config.yml`.

## Banco de dados

O MySQL fica desativado por padrão. Crie previamente o banco e o usuário com uma conta administrativa, adaptando os valores de `src/main/resources/sql/000_database_user.sql`. Depois configure as credenciais e altere `database.enabled` para `true`.

O usuário de execução recebe somente `SELECT`, `INSERT`, `UPDATE`, `DELETE`, `CREATE`, `ALTER` e `INDEX` no banco do plugin. Não recebe permissões administrativas, `DROP` ou `GRANT OPTION`.

Nunca publique o `config.yml` gerado no servidor com uma senha real. Prefira um segredo exclusivo e conexão TLS validada em produção.

### Migrações

O arquivo `sql/migrations.list` é o catálogo explícito e ordenado. Cada script segue o padrão `NNN_descricao.sql` e deve ser idempotente. O executor:

- obtém um bloqueio MySQL para impedir duas instâncias simultâneas;
- cria e consulta `pluginbase_schema_history`;
- verifica SHA-256 de migrações já aplicadas;
- rejeita scripts históricos alterados;
- impede que uma versão antiga execute sobre um esquema mais novo;
- registra descrição, checksum e tempo de execução.

Migrações aplicadas nunca devem ser editadas. Qualquer mudança posterior deve ser criada em um novo arquivo e adicionada ao catálogo. Como o MySQL realiza commit implícito em várias instruções DDL, scripts de estrutura devem continuar idempotentes para permitir recuperação segura após uma interrupção.

Operações normais da aplicação podem usar `DatabaseManager.transaction`, que fornece commit, rollback e fechamento automáticos.

## Comandos

- `/pluginbase` — exibe nome e versão.
- `/pluginbase reload` — recarrega configurações e idioma.
- `/pluginbase status` — exibe tempo ativo e quantidade de serviços.
- `/pluginbase status <database|services>` — detalha um componente.
- `/pbase` — alias do comando principal.

Permissões administrativas: `pluginbase.command.reload` e `pluginbase.command.status`.

Cada subcomando possui uma classe própria e compõe a árvore em `PluginBaseCommand`. `CommandPermissions` centraliza os nós de permissão, `CommandSuggestions` filtra sugestões e `CommandExceptionHandler` impede que erros internos ou stack traces sejam enviados ao jogador.

## Estrutura

```text
br.net.silvioluizsilva.pluginbase
├── api
├── bootstrap
├── command
├── config
├── database
├── exception
├── language
├── listener
├── logging
├── manager
├── model
├── repository
├── scheduler
├── service
├── util
└── web
```

Cada pacote possui responsabilidade própria. Comandos, listeners e a classe principal devem apenas encaminhar chamadas; regras de negócio pertencem a `service`.

### Composição e serviços

`PluginContext` concentra apenas as dependências compartilhadas e evita referências globais estáticas. `ComponentRegistrar` é o ponto único de composição; dele partem os registradores específicos de serviços e listeners.

`ServiceRegistry` resolve serviços por contratos tipados, rejeita duplicidades e impede registros após a inicialização. Os serviços são iniciados na ordem declarada e encerrados na ordem inversa. Se uma inicialização falhar, os serviços anteriores são automaticamente encerrados. Serviços que implementam `ReloadableService` recebem apenas configurações previamente validadas.

Para adicionar um serviço:

1. crie seu contrato público estendendo `ManagedService`;
2. implemente o contrato no pacote `service`;
3. registre contrato e implementação em `ServiceRegistrar`;
4. resolva-o com `context.services().resolve(SeuServico.class)`.

## Listeners e tarefas

Listeners são apenas adaptadores de eventos e ficam declarados em `ListenerRegistrar`; regras de negócio devem ser encaminhadas aos serviços. `ServerLifecycleListener` demonstra o padrão sem alterar o comportamento do servidor.

O contrato `TaskScheduler` oferece execução síncrona, atrasada, repetitiva e assíncrona. Toda tarefa é rastreada, falhas são enviadas ao logger seguro e tarefas pendentes são canceladas no desligamento. Código assíncrono não deve acessar entidades, mundos ou outras partes não thread-safe da API Bukkit.

Esta versão usa o agendador padrão Bukkit/Paper e mantém `folia-supported: false`. Plugins consumidores devem usar as sobrecargas que recebem seu próprio `Plugin` como proprietário, garantindo o cancelamento automático das tarefas no ciclo de vida correto. Não marque o plugin como compatível com Folia antes da implementação de agendamento global, regional e por entidade.

## Internacionalização

As mensagens ficam em `src/main/resources/languages`. O idioma ativo é definido por `language` no `config.yml`. Os textos aceitam MiniMessage.

## Configuração tipada

A leitura do YAML é centralizada em `ConfigManager`. `PluginConfig`, `DatabaseConfig`, `PoolConfig` e `LoggingConfig` são imutáveis e validados antes de serem publicados ao restante do plugin. Uma recarga inválida é rejeitada e a última configuração tipada válida permanece ativa.

## Logs seguros

Todo componente utiliza `PluginLogger`, que centraliza os níveis `INFO`, `WARN`, `ERROR` e `DEBUG`. Senhas, tokens, chaves de API, cabeçalhos Bearer, credenciais JDBC e vetores de bytes ou caracteres são mascarados antes de chegar ao console.

O modo `debug` e a impressão de stack traces são independentes e ficam desativados por padrão. Ative stack traces apenas durante diagnóstico controlado; mensagens de exceção podem conter dados provenientes de serviços externos.

## Licença

Distribuído sob a licença MIT. Consulte `LICENSE`.

## Qualidade e integração contínua

`mvn clean verify` executa obrigatoriamente:

- Maven Enforcer para Maven 3.9.6+ e Java 25;
- Checkstyle para higiene e consistência dos fontes;
- JUnit 5 para testes automatizados;
- JaCoCo para relatório e limite mínimo de 30% de cobertura de linhas;
- SpotBugs em esforço máximo, falhando em alertas médios ou superiores;
- geração do JAR sombreado somente após as verificações anteriores.

O workflow `.github/workflows/quality.yml` repete o processo em pushes e pull requests para `main` e preserva relatórios por 14 dias. O Dependabot verifica dependências Maven semanalmente e ações do GitHub mensalmente. Consulte `CONTRIBUTING.md` antes de enviar alterações.

## API para outros plugins

A versão atual do contrato público é `1.1`. O PluginBase publica `PluginBaseApi` no `ServicesManager` somente depois que banco, serviços e demais componentes terminam de iniciar. A API também oferece acesso JDBC transacional controlado por `DatabaseAccess`, sem expor credenciais aos consumidores.

No `plugin.yml` do consumidor:

```yaml
depend: [PluginBase]
```

No código do consumidor:

```java
PluginBaseApi api = PluginBaseProvider.get();
String pluginVersion = api.pluginVersion();
PluginBaseSettings settings = api.settings();
TaskScheduler scheduler = api.scheduler();
StatusService status = api.status();
```

Para uma integração opcional, use `PluginBaseProvider.find()` e declare `softdepend: [PluginBase]`. A visão `PluginBaseSettings` nunca expõe usuário, senha, URL JDBC ou configuração interna do pool.

Ao compilar outro plugin contra este projeto, use `br.net.silvioluizsilva:pluginbase:0.0.1` com escopo Maven `provided`. O JAR do PluginBase deve estar instalado no servidor e não deve ser incorporado novamente ao JAR consumidor.

Consumidores devem depender apenas das interfaces e records do pacote `br.net.silvioluizsilva.pluginbase.api`. Classes dos demais pacotes são detalhes internos e podem mudar entre versões de desenvolvimento.

## Preparação da interface web

A `beta5` não inicia servidor HTTP nem abre portas. Ela mantém configuração tipada, DTOs, autenticação Bearer por SHA-256 e uma barreira preliminar para origem, tamanho de corpo e credenciais.

## Revisão beta1

- versão exata da API Paper para compilações reproduzíveis;
- driver MySQL e descritor de serviço relocados corretamente;
- SLF4J fornecido pelo servidor e Protobuf isolado no JAR;
- recarga coordenada com restauração da configuração anterior em caso de falha;
- troca segura do pool de banco de dados durante a recarga;
- marcadores MiniMessage tratados como texto literal;
- estados do banco traduzidos;
- ciclo de vida de serviços sem chamadas externas sob o monitor interno;
- parser SQL compatível com a regra de comentários `--` do MySQL.

## Novidades beta2

- API pública `1.1` com acesso transacional controlado ao banco;
- projeto consumidor `PluginExample` distribuído separadamente;
- exemplo de comando Brigadier, listener, serviço, repositório e migração própria;
- tarefas assíncronas associadas ao ciclo de vida do plugin consumidor.

## Novidades beta3

- acesso ao banco solicitado por `api.database(pluginConsumidor)`;
- namespace derivado e validado para cada plugin;
- nomes físicos de tabelas gerados por `database.table(nomeLogico)`;
- histórico central de migrações separado por namespace e versão;
- checksum, bloqueio concorrente e proteção contra downgrade por consumidor;
- rejeição de migrações que referenciem tabelas fora do namespace;
- PluginExample atualizado para utilizar o executor central de migrações.

O namespace reduz conflitos e impede referências cruzadas acidentais nas migrações. Ele não constitui uma barreira contra código malicioso executado dentro da mesma JVM: isolamento rígido exige schemas e usuários MySQL distintos para cada nível de confiança.

## Novidades beta4

- modo degradado controlado quando o MySQL está temporariamente indisponível;
- estados `DISABLED`, `CONNECTING`, `CONNECTED`, `DEGRADED`, `RECONNECTING`, `FAILED`, `STOPPING` e `STOPPED`;
- verificação de saúde a cada 30 segundos, com timeout de 3 segundos;
- reconexão ilimitada iniciando em 60 segundos e crescendo linearmente até 900 segundos;
- variação aleatória de 20% nos intervalos de reconexão;
- uma única tentativa de reconexão por vez;
- métricas de tentativa, próxima reconexão, indisponibilidade, última conexão e última falha;
- comandos `database health` e `database reconnect`;
- bloqueio de transações consumidoras na thread principal;
- timeout JDBC de 30 segundos por instrução;
- alerta lento após 1.000 ms e crítico após 5.000 ms;
- limite de duas transações simultâneas por consumidor, com espera máxima de 2.000 ms;
- PluginExample preparado para aplicar sua migração após a recuperação do banco.

## Etapas reservadas para o encerramento do projeto

- teste integrado com Paper 26.2 e MySQL real;
- publicação da API em um repositório Maven;
- implementação da interface web externa.

## Estabilização beta5

- timeout renomeado para `statement-timeout-seconds`, deixando explícito que vale por instrução JDBC;
- nenhuma thread é encerrada à força quando uma operação ultrapassa os limites de duração;
- proteção contra agendamentos duplicados de reconexão;
- testes determinísticos dos estados desativado, degradado, conectado e encerrado;
- testes do limite de duas transações e da liberação idempotente das vagas;
- consumidores não podem executar `commit`, `rollback`, `setAutoCommit`, `close` ou `unwrap` na conexão gerenciada;
- contrato da API `1.1` protegido por reflexão nos testes;
- inspeção automatizada do JAR disponível em `scripts/verify-jar.ps1`.

Por segurança, o endereço permanece limitado a `127.0.0.1`, `::1` ou `localhost`. A ativação exige ao menos uma origem exata e um hash SHA-256 válido. Curingas de origem e tokens em texto puro são rejeitados.

Consulte `docs/web-api.md` para os limites implementados e as decisões obrigatórias antes de adicionar transporte HTTP.
