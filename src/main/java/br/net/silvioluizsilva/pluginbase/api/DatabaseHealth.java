package br.net.silvioluizsilva.pluginbase.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Visão imutável e sem credenciais da saúde do banco.
 *
 * @param state estado atual
 * @param reconnectAttempt número da tentativa atual
 * @param lastConnectedAt última conexão bem-sucedida
 * @param lastFailureAt última falha
 * @param nextReconnectAt próxima tentativa programada
 * @param unavailableSince início da indisponibilidade
 */
public record DatabaseHealth(DatabaseState state, long reconnectAttempt, Optional<Instant> lastConnectedAt,
                             Optional<Instant> lastFailureAt, Optional<Instant> nextReconnectAt,
                             Optional<Instant> unavailableSince) {

    /**
     * Calcula o tempo total da indisponibilidade atual.
     *
     * @param now instante de referência
     * @return duração não negativa
     */
    public Duration unavailableFor(Instant now) {
        return unavailableSince.map(start -> Duration.between(start, now).isNegative()
                ? Duration.ZERO : Duration.between(start, now)).orElse(Duration.ZERO);
    }
}
