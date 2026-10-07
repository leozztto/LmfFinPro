package com.lmf.finpro.application.household;

import com.lmf.finpro.application.FlowLog;
import com.lmf.finpro.domain.exception.HouseholdPermissionException;
import com.lmf.finpro.domain.exception.HouseholdRuleException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.BlockingLink;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.Household;
import com.lmf.finpro.domain.model.HouseholdMembership;
import com.lmf.finpro.domain.model.Tag;
import com.lmf.finpro.domain.port.out.AccountOwnershipPort;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.AccountSharingPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.HouseholdRepositoryPort;
import com.lmf.finpro.domain.port.out.TagRepositoryPort;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Passa contas do espaço pessoal para um grupo compartilhado, levando junto o histórico
 * (transações, importações, recorrências, anexos, e as transferências e metas entre as contas
 * movidas). É uma <b>mudança de dono</b>, não uma cópia: depois disso as contas deixam de aparecer
 * no espaço pessoal e passam a ser vistas e editadas por todos os membros.
 *
 * <p>Categorias, clientes e tags são de cada grupo, então as que as transações movidas usam são
 * recriadas no grupo de destino (reaproveitando as que já existem com o mesmo nome) e as transações
 * passam a apontar para elas. As categorias padrão do sistema não mudam. Orçamentos, impostos e
 * dívidas ficam onde estão: não pertencem a uma conta.
 *
 * <p>Uma transferência entre uma conta movida e outra que fica é dividida: cada espaço guarda a sua
 * perna e vê só o que é seu (a conta conjunta não expõe a pessoal). Já uma meta de economia liga
 * duas contas dentro da mesma meta e pertence a um grupo só, então, se ela liga uma conta movida a
 * outra que ficaria para trás, o pedido é recusado e a pessoa precisa incluir as duas contas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountSharingApplicationService {

    private final HouseholdRepositoryPort householdRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final TagRepositoryPort tagRepositoryPort;
    private final AccountSharingPort accountSharingPort;
    private final AccountOwnershipPort accountOwnershipPort;

    @Transactional
    public AccountSharingResult shareAccounts(
            Long currentUserId, Long targetHouseholdId, Set<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            throw new HouseholdRuleException("Escolha ao menos uma conta para compartilhar");
        }
        // Qualquer membro compartilha as próprias contas (não só o dono): o dono controla quem
        // entra, mas o que cada um traz é escolha de cada um.
        householdRepositoryPort
                .findMembership(targetHouseholdId, currentUserId)
                .orElseThrow(
                        () -> new HouseholdPermissionException("Você não participa deste grupo"));
        Household target =
                householdRepositoryPort
                        .findById(targetHouseholdId)
                        .orElseThrow(() -> new ResourceNotFoundException("Grupo não encontrado"));
        if (!target.isShared()) {
            throw new HouseholdRuleException(
                    "Só é possível compartilhar contas com um grupo compartilhado");
        }
        Long personalHouseholdId =
                householdRepositoryPort
                        .findPersonalMembership(currentUserId)
                        .orElseThrow(
                                () ->
                                        new HouseholdPermissionException(
                                                "Espaço pessoal não encontrado"))
                        .householdId();

        Set<Long> ids = new LinkedHashSet<>(accountIds);
        for (Long accountId : ids) {
            // Só contas do próprio espaço pessoal: de outro grupo (ou de outra pessoa) é "não
            // encontrada", sem revelar que existe.
            accountRepositoryPort
                    .findById(accountId)
                    .filter(account -> account.belongsTo(personalHouseholdId))
                    .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));
        }

        return move(ids, personalHouseholdId, targetHouseholdId, currentUserId);
    }

    /**
     * Devolve contas do grupo ao espaço pessoal de quem pede, com todo o histórico: o inverso de
     * {@link #shareAccounts}. É uma mudança de dono, e o histórico inteiro vai junto, inclusive os
     * lançamentos que outros membros fizeram na conta. Por isso só pode quem trouxe ou criou a
     * conta; quando o dono é desconhecido (conta compartilhada antes de a conta ter dono), o dono
     * do grupo.
     */
    @Transactional
    public AccountSharingResult unshareAccounts(
            Long currentUserId, Long householdId, Set<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            throw new HouseholdRuleException("Escolha ao menos uma conta para descompartilhar");
        }
        HouseholdMembership membership =
                householdRepositoryPort
                        .findMembership(householdId, currentUserId)
                        .orElseThrow(
                                () ->
                                        new HouseholdPermissionException(
                                                "Você não participa deste grupo"));
        Household household =
                householdRepositoryPort
                        .findById(householdId)
                        .orElseThrow(() -> new ResourceNotFoundException("Grupo não encontrado"));
        if (!household.isShared()) {
            throw new HouseholdRuleException(
                    "Só é possível descompartilhar contas de um grupo compartilhado");
        }
        Long personalHouseholdId =
                householdRepositoryPort
                        .findPersonalMembership(currentUserId)
                        .orElseThrow(
                                () ->
                                        new HouseholdPermissionException(
                                                "Espaço pessoal não encontrado"))
                        .householdId();

        Set<Long> ids = new LinkedHashSet<>(accountIds);
        for (Long accountId : ids) {
            accountRepositoryPort
                    .findById(accountId)
                    .filter(account -> account.belongsTo(householdId))
                    .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));
            Long owner = accountOwnershipPort.findOwner(accountId).orElse(null);
            boolean allowed =
                    currentUserId.equals(owner) || (owner == null && membership.isOwner());
            if (!allowed) {
                throw new HouseholdPermissionException(
                        "Só quem trouxe ou criou a conta pode descompartilhá-la.");
            }
        }

        return move(ids, householdId, personalHouseholdId, currentUserId);
    }

    /** Leva as contas de um espaço para outro, com tudo o que é delas. */
    private AccountSharingResult move(
            Set<Long> ids, Long sourceHouseholdId, Long targetHouseholdId, Long currentUserId) {
        List<BlockingLink> blocking = accountSharingPort.findBlockingLinks(ids);
        if (!blocking.isEmpty()) {
            throw new HouseholdRuleException(blockedMessage(blocking));
        }

        remapCategories(ids, sourceHouseholdId, targetHouseholdId);
        remapClients(ids, sourceHouseholdId, targetHouseholdId);
        remapTags(ids, sourceHouseholdId, targetHouseholdId);

        int transactions = accountSharingPort.countTransactions(ids);
        accountSharingPort.moveToHousehold(ids, targetHouseholdId);
        // Quem move é, por definição, o dono (trouxe a conta, ou é quem a recebe de volta).
        accountOwnershipPort.recordOwner(ids, currentUserId);

        FlowLog.detail("accounts", ids.size());
        FlowLog.detail("transactions", transactions);
        log.info(
                "Contas movidas de={} para={} userId={} contas={} transações={}",
                sourceHouseholdId,
                targetHouseholdId,
                currentUserId,
                ids.size(),
                transactions);
        return new AccountSharingResult(ids.size(), transactions);
    }

    private void remapCategories(Set<Long> accountIds, Long sourceId, Long targetId) {
        Set<Long> used = accountSharingPort.findCategoryIdsUsedBy(accountIds);
        if (used.isEmpty()) {
            return;
        }
        Map<String, Category> inTarget =
                categoryRepositoryPort.findAllVisibleToUser(targetId).stream()
                        .filter(category -> category.isOwnedBy(targetId))
                        .collect(
                                Collectors.toMap(
                                        category -> key(category.name(), category.type()),
                                        Function.identity(),
                                        (first, second) -> first,
                                        HashMap::new));
        Map<Long, Long> oldToNew = new HashMap<>();
        for (Long categoryId : used) {
            Category category = categoryRepositoryPort.findById(categoryId).orElse(null);
            // Categoria padrão do sistema (sem grupo) vale para todos: não precisa de cópia.
            if (category == null || !category.isOwnedBy(sourceId)) {
                continue;
            }
            Category copy =
                    inTarget.computeIfAbsent(
                            key(category.name(), category.type()),
                            k ->
                                    categoryRepositoryPort.save(
                                            Category.create(
                                                    targetId,
                                                    category.name(),
                                                    category.type(),
                                                    category.color(),
                                                    category.icon())));
            oldToNew.put(categoryId, copy.id());
        }
        accountSharingPort.remapCategories(accountIds, oldToNew);
    }

    private void remapClients(Set<Long> accountIds, Long sourceId, Long targetId) {
        Set<Long> used = accountSharingPort.findClientIdsUsedBy(accountIds);
        if (used.isEmpty()) {
            return;
        }
        Map<String, Client> inTarget =
                clientRepositoryPort.findAllByHouseholdId(targetId).stream()
                        .collect(
                                Collectors.toMap(
                                        client -> key(client.name()),
                                        Function.identity(),
                                        (first, second) -> first,
                                        HashMap::new));
        Map<Long, Long> oldToNew = new HashMap<>();
        for (Long clientId : used) {
            Client client = clientRepositoryPort.findById(clientId).orElse(null);
            if (client == null || !client.belongsTo(sourceId)) {
                continue;
            }
            Client copy =
                    inTarget.computeIfAbsent(
                            key(client.name()),
                            k ->
                                    clientRepositoryPort.save(
                                            Client.create(
                                                    targetId,
                                                    client.name(),
                                                    client.email(),
                                                    client.phone(),
                                                    client.documentType(),
                                                    client.documentNumber(),
                                                    client.workType(),
                                                    client.notes(),
                                                    client.color(),
                                                    client.active())));
            oldToNew.put(clientId, copy.id());
        }
        accountSharingPort.remapClients(accountIds, oldToNew);
    }

    private void remapTags(Set<Long> accountIds, Long sourceId, Long targetId) {
        Set<Long> used = accountSharingPort.findTagIdsUsedBy(accountIds);
        if (used.isEmpty()) {
            return;
        }
        Map<String, Tag> inTarget =
                tagRepositoryPort.findAllByHouseholdId(targetId).stream()
                        .collect(
                                Collectors.toMap(
                                        Tag::name,
                                        Function.identity(),
                                        (first, second) -> first,
                                        HashMap::new));
        Map<Long, Long> oldToNew = new HashMap<>();
        for (Long tagId : used) {
            Tag tag = tagRepositoryPort.findById(tagId).orElse(null);
            if (tag == null || !tag.belongsTo(sourceId)) {
                continue;
            }
            Tag copy =
                    inTarget.computeIfAbsent(
                            tag.name(),
                            k ->
                                    tagRepositoryPort.save(
                                            Tag.create(targetId, tag.name(), tag.color())));
            oldToNew.put(tagId, copy.id());
        }
        accountSharingPort.remapTags(accountIds, oldToNew);
    }

    private static String blockedMessage(List<BlockingLink> blocking) {
        String links =
                blocking.stream()
                        .map(
                                link ->
                                        "meta de economia entre \""
                                                + link.accountName()
                                                + "\" e \""
                                                + link.otherAccountName()
                                                + "\"")
                        .collect(Collectors.joining("; "));
        return "Há vínculos com contas que não estão na seleção ("
                + links
                + "). Compartilhe as contas ligadas juntas.";
    }

    private static String key(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }

    private static String key(String name, Enum<?> type) {
        return key(name) + "|" + type.name();
    }
}
