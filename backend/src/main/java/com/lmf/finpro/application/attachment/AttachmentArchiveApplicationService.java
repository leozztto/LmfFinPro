package com.lmf.finpro.application.attachment;

import com.lmf.finpro.application.tag.TagApplicationService;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AttachmentArchiveData;
import com.lmf.finpro.domain.model.AttachmentFileType;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.Tag;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionAttachment;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.AttachmentArchiveWriterPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionAttachmentRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.io.OutputStream;
import java.text.Normalizer;
import java.time.Year;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Pacote de comprovantes do ano para o IR: um ZIP com os anexos das transações do ano, em pastas
 * por mês, e um índice CSV ligando cada arquivo à transação (data, descrição, valor, categoria,
 * cliente). Vale a data da transação, não a do upload.
 */
@Service
@RequiredArgsConstructor
public class AttachmentArchiveApplicationService {

    private static final int SLUG_MAX_LENGTH = 40;

    private final AccountRepositoryPort accountRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final TransactionAttachmentRepositoryPort attachmentRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final AttachmentArchiveWriterPort attachmentArchiveWriterPort;
    private final TagApplicationService tagApplicationService;

    /**
     * Monta o conteúdo antes de começar a escrever o ZIP, para que um ano sem comprovantes vire um
     * 404 com mensagem clara (e não um arquivo vazio).
     */
    public AttachmentArchiveData prepare(Long currentUserId, Year year) {
        Map<Long, Account> accountById =
                accountRepositoryPort.findAllByUserId(currentUserId).stream()
                        .collect(Collectors.toMap(Account::id, Function.identity()));
        Map<Long, Transaction> transactionById =
                accountById.isEmpty()
                        ? Map.of()
                        : transactionRepositoryPort
                                .findAllByAccountIds(List.copyOf(accountById.keySet()))
                                .stream()
                                .filter(
                                        transaction ->
                                                transaction.transactionDate().getYear()
                                                        == year.getValue())
                                .collect(Collectors.toMap(Transaction::id, Function.identity()));

        List<TransactionAttachment> attachments =
                attachmentRepositoryPort.findAllByTransactionIds(transactionById.keySet());
        if (attachments.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Nenhum comprovante anexado às transações de " + year + ".");
        }

        Map<Long, String> categoryNameById =
                categoryRepositoryPort.findAllVisibleToUser(currentUserId).stream()
                        .collect(Collectors.toMap(Category::id, Category::name));
        Map<Long, String> clientNameById =
                clientRepositoryPort.findAllByUserId(currentUserId).stream()
                        .collect(Collectors.toMap(Client::id, Client::name));
        Map<Long, List<Tag>> tagsByTransaction =
                tagApplicationService.tagsByTransactionIds(
                        currentUserId,
                        attachments.stream()
                                .map(TransactionAttachment::transactionId)
                                .distinct()
                                .toList());

        List<AttachmentArchiveData.Entry> entries =
                attachments.stream()
                        .sorted(
                                Comparator.comparing(
                                                (TransactionAttachment attachment) ->
                                                        transactionById
                                                                .get(attachment.transactionId())
                                                                .transactionDate())
                                        .thenComparing(TransactionAttachment::id))
                        .map(
                                attachment ->
                                        toEntry(
                                                attachment,
                                                transactionById.get(attachment.transactionId()),
                                                accountById,
                                                categoryNameById,
                                                clientNameById,
                                                Tag.joinLabels(
                                                        tagsByTransaction.getOrDefault(
                                                                attachment.transactionId(),
                                                                List.of()))))
                        .toList();
        return new AttachmentArchiveData(year, entries);
    }

    public void write(AttachmentArchiveData data, OutputStream output) {
        attachmentArchiveWriterPort.write(data, output);
    }

    private static AttachmentArchiveData.Entry toEntry(
            TransactionAttachment attachment,
            Transaction transaction,
            Map<Long, Account> accountById,
            Map<Long, String> categoryNameById,
            Map<Long, String> clientNameById,
            String tags) {
        String extension =
                AttachmentFileType.fromContentType(attachment.contentType())
                        .map(AttachmentFileType::extension)
                        .orElse("bin");
        String path =
                YearMonth.from(transaction.transactionDate())
                        + "/"
                        + transaction.transactionDate()
                        + "_"
                        + slug(transaction.description())
                        + "_"
                        + attachment.id()
                        + "."
                        + extension;
        Account account = accountById.get(transaction.accountId());
        return new AttachmentArchiveData.Entry(
                path,
                attachment.storageKey(),
                transaction.transactionDate(),
                transaction.description(),
                transaction.type(),
                transaction.amount(),
                transaction.status(),
                account == null ? "" : account.name(),
                transaction.categoryId() == null
                        ? "Sem categoria"
                        : categoryNameById.getOrDefault(
                                transaction.categoryId(), "Categoria removida"),
                transaction.clientId() == null
                        ? ""
                        : clientNameById.getOrDefault(transaction.clientId(), "Cliente removido"),
                attachment.documentType(),
                attachment.fileName(),
                tags);
    }

    /** "Café & Cia — março" → "cafe-cia-marco": nome de arquivo seguro em qualquer sistema. */
    static String slug(String text) {
        String normalized =
                Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("(^-|-$)", "");
        if (normalized.isEmpty()) {
            return "transacao";
        }
        return normalized.length() > SLUG_MAX_LENGTH
                ? normalized.substring(0, SLUG_MAX_LENGTH).replaceAll("-$", "")
                : normalized;
    }
}
