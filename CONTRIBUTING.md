# Contribuindo com o PluginBase

## Requisitos

- JDK 25
- Maven 3.9.6 ou superior

## Verificação obrigatória

Antes de enviar uma alteração, execute:

```bash
mvn clean verify
```

O comando valida ambiente, estilo, testes, cobertura e análise estática. Alterações não devem reduzir a cobertura total abaixo do limite definido no `pom.xml`.

## Organização

- regras de negócio ficam em `service`;
- persistência fica em `repository` e `database`;
- comandos e listeners apenas adaptam entradas;
- novos serviços devem possuir contrato público quando forem consumidos externamente;
- novas migrações devem ser idempotentes e nunca modificar scripts já aplicados;
- código público deve possuir JavaDoc;
- credenciais e dados sensíveis nunca devem ser enviados aos logs.

## Commits e pull requests

Mantenha cada alteração pequena e coesa. Descreva o comportamento alterado, os testes adicionados e qualquer impacto em configuração ou migração.
