# Verificação de vulnerabilidades

O build Maven executa o OWASP Dependency-Check na fase `verify` para cada módulo que herda o `pluginbase-build-parent`.

## Política

- Vulnerabilidades com CVSS maior ou igual a 7 bloqueiam o build.
- Achados abaixo desse limiar continuam registrados nos relatórios HTML e JSON e devem ser avaliados antes de uma entrega.
- Falhas durante a análise bloqueiam o build; uma verificação de segurança indisponível não é tratada como aprovação. O Sonatype OSS Index permanece desabilitado enquanto não houver credencial própria, porque o serviço exige autenticação e bloqueia consultas anônimas. Ele é complementar à NVD e à lista CISA; para reativá-lo, configurar um servidor Maven com token fora do repositório e definir `ossIndexServerId`.
- Dependências `provided` e de teste não são analisadas, pois não são empacotadas nos JARs entregues.

## Relatórios e exceções

Os relatórios são gravados em `target/dependency-check-report.html` e `target/dependency-check-report.json` de cada módulo.

Uma supressão só pode ser criada após confirmar um falso positivo. Ela deve identificar o achado, explicar a justificativa, registrar quem aprovou e definir uma data de reavaliação. Nunca use supressões amplas para contornar uma vulnerabilidade real.

Na primeira execução, o Dependency-Check pode levar vários minutos para baixar e processar a base pública de vulnerabilidades. Depois disso, as atualizações usam o cache local.
