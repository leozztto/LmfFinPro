package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Calendário financeiro de um mês — puro, sem acesso a repositório. Junta num só lugar o que cai em
 * cada dia: transações do mês (pendentes e, se pedido, pagas), ocorrências futuras das recorrências
 * ativas que ainda não viraram transação (previstas) e o vencimento do DAS. Também lista tudo o que
 * está atrasado (pendente com data anterior a hoje), de qualquer mês.
 *
 * <p>Recebe as transações já sem transferências entre contas próprias.
 */
public final class FinancialCalendar {

    private FinancialCalendar() {}

    public enum EntryKind {
        TRANSACTION,
        RECURRING_FORECAST,
        DAS
    }

    public enum EntryStatus {
        PAID,
        PENDING,
        /** Pendente com data anterior a hoje. */
        OVERDUE,
        /** Ocorrência de recorrência que ainda não foi lançada, ou lembrete do DAS. */
        FORECAST
    }

    /**
     * @param amount {@code null} só no DAS sem estimativa de imposto cadastrada para a competência
     * @param type no DAS é sempre despesa
     * @param transactionId só em {@link EntryKind#TRANSACTION}
     * @param recurringTransactionId na transação gerada por recorrência e na ocorrência prevista
     * @param competence só no DAS: mês de referência do imposto
     */
    public record Entry(
            EntryKind kind,
            LocalDate date,
            String description,
            BigDecimal amount,
            CategoryType type,
            EntryStatus status,
            Long transactionId,
            Long recurringTransactionId,
            Long accountId,
            Long categoryId,
            Long clientId,
            YearMonth competence) {

        boolean isOpen() {
            return status != EntryStatus.PAID;
        }
    }

    /** Totais do dia somam todos os lançamentos exibidos nele (DAS sem valor fica de fora). */
    public record Day(LocalDate date, BigDecimal income, BigDecimal expense, List<Entry> entries) {}

    /** Vencimento do DAS que cai no mês, com o valor da estimativa de imposto, se houver. */
    public record DasDue(YearMonth competence, LocalDate dueDate, BigDecimal estimatedValue) {}

    /**
     * @param expectedIncome receitas em aberto no mês (pendentes, atrasadas e previstas)
     * @param expectedExpense despesas em aberto no mês (pendentes, atrasadas, previstas e DAS)
     * @param paidIncome receitas do mês já recebidas
     * @param paidExpense despesas do mês já pagas
     * @param days só os dias do mês que têm algum lançamento, em ordem
     * @param overdue pendentes com data anterior a hoje, de qualquer mês, da mais antiga à mais
     *     nova
     */
    public record Report(
            YearMonth month,
            LocalDate today,
            BigDecimal expectedIncome,
            BigDecimal expectedExpense,
            BigDecimal paidIncome,
            BigDecimal paidExpense,
            List<Day> days,
            List<Entry> overdue,
            BigDecimal overdueIncome,
            BigDecimal overdueExpense) {}

    /**
     * @param transactions transações do usuário, sem transferências (qualquer período)
     * @param recurrences recorrências do usuário (as pausadas são ignoradas)
     * @param dasDue vencimento do DAS no mês, ou {@code null} se o regime não tem DAS
     * @param includePaid se as transações pagas aparecem nos dias (os totais pagos são sempre
     *     calculados)
     */
    public static Report build(
            YearMonth month,
            LocalDate today,
            List<Transaction> transactions,
            List<RecurringTransaction> recurrences,
            DasDue dasDue,
            boolean includePaid) {
        LocalDate firstDay = month.atDay(1);
        LocalDate lastDay = month.atEndOfMonth();

        List<Entry> monthEntries = new ArrayList<>();
        List<Entry> overdue = new ArrayList<>();
        for (Transaction transaction : transactions) {
            Entry entry = fromTransaction(transaction, today);
            if (entry.status() == EntryStatus.OVERDUE) {
                overdue.add(entry);
            }
            if (!entry.date().isBefore(firstDay) && !entry.date().isAfter(lastDay)) {
                monthEntries.add(entry);
            }
        }
        monthEntries.addAll(forecasts(recurrences, today, firstDay, lastDay));
        if (dasDue != null && YearMonth.from(dasDue.dueDate()).equals(month)) {
            monthEntries.add(fromDas(dasDue));
        }

        BigDecimal expectedIncome = BigDecimal.ZERO;
        BigDecimal expectedExpense = BigDecimal.ZERO;
        BigDecimal paidIncome = BigDecimal.ZERO;
        BigDecimal paidExpense = BigDecimal.ZERO;
        Map<LocalDate, List<Entry>> byDate = new TreeMap<>();
        for (Entry entry : monthEntries) {
            BigDecimal amount = entry.amount() == null ? BigDecimal.ZERO : entry.amount();
            boolean income = entry.type() == CategoryType.INCOME;
            if (entry.isOpen()) {
                expectedIncome = income ? expectedIncome.add(amount) : expectedIncome;
                expectedExpense = income ? expectedExpense : expectedExpense.add(amount);
            } else {
                paidIncome = income ? paidIncome.add(amount) : paidIncome;
                paidExpense = income ? paidExpense : paidExpense.add(amount);
            }
            if (includePaid || entry.isOpen()) {
                byDate.computeIfAbsent(entry.date(), date -> new ArrayList<>()).add(entry);
            }
        }

        List<Day> days = new ArrayList<>();
        byDate.forEach((date, entries) -> days.add(toDay(date, entries)));

        overdue.sort(ENTRY_ORDER);
        return new Report(
                month,
                today,
                expectedIncome,
                expectedExpense,
                paidIncome,
                paidExpense,
                days,
                overdue,
                sum(overdue, CategoryType.INCOME),
                sum(overdue, CategoryType.EXPENSE));
    }

    /** Receitas antes de despesas; dentro de cada tipo, do maior valor para o menor. */
    private static final Comparator<Entry> DAY_ORDER =
            Comparator.comparing(Entry::type, Comparator.comparing(FinancialCalendar::typeOrder))
                    .thenComparing(
                            entry -> entry.amount() == null ? BigDecimal.ZERO : entry.amount(),
                            Comparator.reverseOrder())
                    .thenComparing(Entry::description, Comparator.nullsLast(String::compareTo));

    private static final Comparator<Entry> ENTRY_ORDER =
            Comparator.comparing(Entry::date).thenComparing(DAY_ORDER);

    private static int typeOrder(CategoryType type) {
        return type == CategoryType.INCOME ? 0 : 1;
    }

    private static Day toDay(LocalDate date, List<Entry> entries) {
        List<Entry> sorted = entries.stream().sorted(DAY_ORDER).toList();
        return new Day(
                date, sum(sorted, CategoryType.INCOME), sum(sorted, CategoryType.EXPENSE), sorted);
    }

    private static BigDecimal sum(List<Entry> entries, CategoryType type) {
        return entries.stream()
                .filter(entry -> entry.type() == type && entry.amount() != null)
                .map(Entry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Entry fromTransaction(Transaction transaction, LocalDate today) {
        EntryStatus status;
        if (transaction.isPaid()) {
            status = EntryStatus.PAID;
        } else if (transaction.transactionDate().isBefore(today)) {
            status = EntryStatus.OVERDUE;
        } else {
            status = EntryStatus.PENDING;
        }
        return new Entry(
                EntryKind.TRANSACTION,
                transaction.transactionDate(),
                transaction.description(),
                transaction.baseAmount(),
                transaction.type(),
                status,
                transaction.id(),
                transaction.recurringTransactionId(),
                transaction.accountId(),
                transaction.categoryId(),
                transaction.clientId(),
                null);
    }

    /**
     * Ocorrências ainda não lançadas das recorrências ativas que caem no mês, de hoje em diante. As
     * de hoje só aparecem aqui enquanto o agendador diário não as lançou; as de dias passados são
     * recuperadas por ele e não viram previsão.
     */
    private static List<Entry> forecasts(
            List<RecurringTransaction> recurrences,
            LocalDate today,
            LocalDate firstDay,
            LocalDate lastDay) {
        List<Entry> entries = new ArrayList<>();
        if (lastDay.isBefore(today)) {
            return entries;
        }
        for (RecurringTransaction recurrence : recurrences) {
            for (LocalDate date : recurrence.dueOccurrenceDates(lastDay)) {
                if (date.isBefore(firstDay) || date.isBefore(today)) {
                    continue;
                }
                entries.add(
                        new Entry(
                                EntryKind.RECURRING_FORECAST,
                                date,
                                recurrence.description(),
                                recurrence.amount(),
                                recurrence.type(),
                                EntryStatus.FORECAST,
                                null,
                                recurrence.id(),
                                recurrence.accountId(),
                                recurrence.categoryId(),
                                recurrence.clientId(),
                                null));
            }
        }
        return entries;
    }

    private static Entry fromDas(DasDue dasDue) {
        return new Entry(
                EntryKind.DAS,
                dasDue.dueDate(),
                "DAS",
                dasDue.estimatedValue(),
                CategoryType.EXPENSE,
                EntryStatus.FORECAST,
                null,
                null,
                null,
                null,
                null,
                dasDue.competence());
    }
}
