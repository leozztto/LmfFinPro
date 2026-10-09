package com.lmf.finpro.domain.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Leitura dos dados do titular para a portabilidade (LGPD). Devolve linhas "cruas" (coluna → valor)
 * de uma lista fixa de tabelas: o que não está na lista, como hashes de senha, tokens e chaves de
 * push, nunca sai.
 */
public interface PersonalDataExportPort {

    /** Cadastro do usuário, sem senha, versão de sessão e chave da foto. */
    Map<String, Object> loadUser(Long userId);

    Optional<Map<String, Object>> loadNotificationPreferences(Long userId);

    /** Escolhas do guia de primeiros passos e os e-mails de ativação já enviados (tipo e data). */
    Map<String, Object> loadOnboarding(Long userId);

    /** Aparelhos com notificação push ativada: só a data, sem endpoint nem chaves. */
    List<Map<String, Object>> loadPushDevices(Long userId);

    /** Pessoas do grupo: só nome, papel e data de entrada (nenhum dado de contato). */
    List<Map<String, Object>> loadMembers(Long householdId);

    /** As linhas do grupo, por nome de tabela. */
    Map<String, List<Map<String, Object>>> loadHouseholdData(Long householdId);

    /** Anexos do grupo com a chave no armazenamento, para copiar os arquivos para o pacote. */
    List<StoredAttachment> loadAttachments(Long householdId);

    record StoredAttachment(Long id, String fileName, String storageKey) {}
}
