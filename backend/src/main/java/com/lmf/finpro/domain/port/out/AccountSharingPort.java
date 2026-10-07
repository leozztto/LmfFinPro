package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.BlockingLink;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mover contas de grupo mexe em tudo que aponta para elas de uma vez, por isso é uma porta própria
 * (operações em lote) e não parte do repositório de contas. Quem chama deve rodar numa única
 * transação.
 */
public interface AccountSharingPort {

    /**
     * Metas de economia que ligam uma das contas a outra de fora do conjunto: mover só uma ponta
     * deixaria a meta atravessando dois grupos. Transferências não bloqueiam: as que cruzam a
     * fronteira são divididas em {@link #moveToHousehold}.
     */
    List<BlockingLink> findBlockingLinks(Set<Long> accountIds);

    /** Categorias usadas pelas transações e recorrências dessas contas. */
    Set<Long> findCategoryIdsUsedBy(Set<Long> accountIds);

    Set<Long> findClientIdsUsedBy(Set<Long> accountIds);

    Set<Long> findTagIdsUsedBy(Set<Long> accountIds);

    /** Troca, nas transações e recorrências dessas contas, a categoria (antiga -> nova). */
    void remapCategories(Set<Long> accountIds, Map<Long, Long> oldToNew);

    void remapClients(Set<Long> accountIds, Map<Long, Long> oldToNew);

    void remapTags(Set<Long> accountIds, Map<Long, Long> oldToNew);

    int countTransactions(Set<Long> accountIds);

    /**
     * Passa para o grupo as contas e tudo o que é delas: importações, recorrências, anexos e as
     * transferências e metas entre elas. As transações seguem a conta (não têm grupo próprio). Uma
     * transferência entre uma conta movida e outra que fica é dividida em duas, uma por espaço.
     */
    void moveToHousehold(Set<Long> accountIds, Long targetHouseholdId);
}
