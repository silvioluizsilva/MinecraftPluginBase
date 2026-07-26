package br.net.silvioluizsilva.pluginbase.api;

/** Representa o estado operacional público do banco de dados. */
public enum DatabaseState {
    /** Infrastructure created; the initial asynchronous connection is pending. */ INITIALIZING,
    /** Banco desativado por configuração. */ DISABLED,
    /** Primeira conexão em andamento. */ CONNECTING,
    /** Pool validado e disponível. */ CONNECTED,
    /** Plugin ativo sem banco disponível. */ DEGRADED,
    /** Tentativa de recuperação em andamento. */ RECONNECTING,
    /** Falha não recuperável de configuração. */ FAILED,
    /** Encerramento em andamento. */ STOPPING,
    /** Infraestrutura encerrada. */ STOPPED
}
