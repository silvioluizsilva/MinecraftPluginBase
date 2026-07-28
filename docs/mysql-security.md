# MySQL: segredos e TLS

## Segredos

- Nunca versione `plugins/PluginBase/config.yml` com credenciais reais.
- Use uma senha exclusiva, longa e rotacionável para cada ambiente.
- Restrinja o usuário ao banco do plugin e aos privilégios estritamente necessários; não use contas administrativas.
- Guarde cópias de produção em cofre de segredos ou variável protegida do painel de hospedagem. Ao gerar o `config.yml`, limite permissões de leitura ao operador do servidor.
- Em caso de exposição, altere a senha, revogue sessões/hosts que não sejam necessários e revise os logs de acesso.

## TLS

Em produção, o servidor MySQL deve apresentar certificado válido e o cliente deve validar a cadeia e o nome do host. Não use parâmetros que desativem a verificação, como `useSSL=false`, `trustServerCertificate=true` ou `verifyServerCertificate=false`.

Mantenha `useSSL=true` em `database.parameters` e configure o conector com os parâmetros de verificação e a CA fornecidos pelo provedor. Antes da ativação, teste a conexão a partir do host Paper e confirme que o certificado corresponde ao endereço configurado.

## Operação

- Libere a porta MySQL apenas para os IPs dos servidores Paper autorizados.
- Separe bancos e usuários entre desenvolvimento, homologação e produção.
- Não registre URL JDBC, senha, token ou conteúdo de exceções não sanitizadas.
- Faça backup, teste restauração e monitore falhas de autenticação, TLS e reconexão.
