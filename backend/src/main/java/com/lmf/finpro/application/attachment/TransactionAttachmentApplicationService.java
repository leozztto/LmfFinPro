package com.lmf.finpro.application.attachment;

import com.lmf.finpro.domain.exception.AttachmentInvalidException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.AttachmentDocumentType;
import com.lmf.finpro.domain.model.AttachmentFileType;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionAttachment;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.FileStoragePort;
import com.lmf.finpro.domain.port.out.TransactionAttachmentRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Anexos de transação (comprovante, nota fiscal, recibo). O conteúdo vai para o {@link
 * FileStoragePort}; o registro, para o banco. Não depende do {@code TransactionApplicationService}
 * (que chama a limpeza daqui ao excluir uma transação), só dos ports.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionAttachmentApplicationService {

    public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    public static final int MAX_PER_TRANSACTION = 10;
    private static final int MAX_FILE_NAME_LENGTH = 255;

    private final TransactionAttachmentRepositoryPort attachmentRepositoryPort;
    private final FileStoragePort fileStoragePort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;

    public record AttachmentContent(TransactionAttachment attachment, byte[] content) {}

    public List<TransactionAttachment> list(Long currentHouseholdId, Long transactionId) {
        log.debug(
                "Listando anexos da transação={} do usuário={}", transactionId, currentHouseholdId);
        requireOwnedTransaction(currentHouseholdId, transactionId);
        return attachmentRepositoryPort.findAllByTransactionId(transactionId);
    }

    /**
     * O formato é reconhecido pelo conteúdo (PDF, JPG, PNG ou WEBP), nunca pelo nome ou pelo tipo
     * informado pelo navegador. Grava o arquivo antes do registro; se o registro falhar, apaga o
     * arquivo para não deixar lixo no disco.
     */
    public TransactionAttachment upload(
            Long currentHouseholdId,
            Long transactionId,
            AttachmentDocumentType documentType,
            String originalFileName,
            byte[] content) {
        log.debug(
                "Enviando anexo para a transação={} do usuário={}",
                transactionId,
                currentHouseholdId);
        requireOwnedTransaction(currentHouseholdId, transactionId);
        if (content == null || content.length == 0) {
            throw new AttachmentInvalidException("O arquivo enviado está vazio.");
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new AttachmentInvalidException(
                    "O arquivo é grande demais. O limite é de 10 MB por arquivo.");
        }
        if (attachmentRepositoryPort.findAllByTransactionId(transactionId).size()
                >= MAX_PER_TRANSACTION) {
            throw new AttachmentInvalidException(
                    "Esta transação já tem "
                            + MAX_PER_TRANSACTION
                            + " anexos, o máximo permitido. Exclua algum antes de enviar outro.");
        }
        AttachmentFileType fileType =
                AttachmentFileType.detect(Arrays.copyOf(content, Math.min(content.length, 12)))
                        .orElseThrow(
                                () ->
                                        new AttachmentInvalidException(
                                                "Formato não aceito. Envie um PDF ou uma imagem"
                                                        + " JPG, PNG ou WEBP."));

        String storageKey = UUID.randomUUID() + "." + fileType.extension();
        fileStoragePort.store(storageKey, content);
        try {
            TransactionAttachment saved =
                    attachmentRepositoryPort.save(
                            TransactionAttachment.create(
                                    transactionId,
                                    currentHouseholdId,
                                    documentType == null
                                            ? AttachmentDocumentType.OTHER
                                            : documentType,
                                    sanitizeFileName(originalFileName, fileType),
                                    fileType.contentType(),
                                    content.length,
                                    storageKey));
            log.info(
                    "Anexo={} salvo na transação={} tipo={} tamanhoBytes={}",
                    saved.id(),
                    transactionId,
                    fileType.contentType(),
                    content.length);
            return saved;
        } catch (RuntimeException e) {
            log.error(
                    "Falha ao salvar o anexo da transação={}; arquivo removido do armazenamento"
                            + " (erro={})",
                    transactionId,
                    e.getClass().getSimpleName());
            fileStoragePort.delete(storageKey);
            throw e;
        }
    }

    public AttachmentContent download(
            Long currentHouseholdId, Long transactionId, Long attachmentId) {
        log.debug(
                "Baixando anexo={} da transação={} do usuário={}",
                attachmentId,
                transactionId,
                currentHouseholdId);
        TransactionAttachment attachment =
                findOwnedOrThrow(currentHouseholdId, transactionId, attachmentId);
        return new AttachmentContent(attachment, fileStoragePort.load(attachment.storageKey()));
    }

    public void delete(Long currentHouseholdId, Long transactionId, Long attachmentId) {
        log.debug(
                "Removendo anexo={} da transação={} do usuário={}",
                attachmentId,
                transactionId,
                currentHouseholdId);
        TransactionAttachment attachment =
                findOwnedOrThrow(currentHouseholdId, transactionId, attachmentId);
        attachmentRepositoryPort.deleteById(attachment.id());
        fileStoragePort.delete(attachment.storageKey());
    }

    public Map<Long, Long> countByTransactionIds(Collection<Long> transactionIds) {
        return attachmentRepositoryPort.countByTransactionIds(transactionIds);
    }

    /**
     * Chaves dos arquivos das transações informadas. Usada por quem exclui transações: pegar as
     * chaves antes (o banco apaga os registros em cascata) e chamar {@link #deleteStoredFiles}
     * depois.
     */
    public List<String> storageKeysOf(Collection<Long> transactionIds) {
        return attachmentRepositoryPort.findAllByTransactionIds(transactionIds).stream()
                .map(TransactionAttachment::storageKey)
                .toList();
    }

    public void deleteStoredFiles(Collection<String> storageKeys) {
        log.debug("Removendo {} arquivo(s) de anexo do armazenamento", storageKeys.size());
        storageKeys.forEach(fileStoragePort::delete);
    }

    /**
     * Nome só para exibição/download: sem caminho, sem caracteres de controle ou reservados, com a
     * extensão do formato real.
     */
    static String sanitizeFileName(String originalFileName, AttachmentFileType fileType) {
        String name = originalFileName == null ? "" : originalFileName;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[\\p{Cntrl}\"<>:|?*]", "").trim();
        String extension = "." + fileType.extension();
        if (name.isEmpty()) {
            name = "anexo" + extension;
        } else if (!name.toLowerCase().endsWith(extension)
                && !(fileType == AttachmentFileType.JPEG && name.toLowerCase().endsWith(".jpeg"))) {
            name = name + extension;
        }
        return name.length() > MAX_FILE_NAME_LENGTH
                ? name.substring(name.length() - MAX_FILE_NAME_LENGTH)
                : name;
    }

    private TransactionAttachment findOwnedOrThrow(
            Long currentHouseholdId, Long transactionId, Long attachmentId) {
        requireOwnedTransaction(currentHouseholdId, transactionId);
        return attachmentRepositoryPort
                .findById(attachmentId)
                .filter(attachment -> attachment.transactionId().equals(transactionId))
                .filter(attachment -> attachment.belongsTo(currentHouseholdId))
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Anexo não encontrado: " + attachmentId));
    }

    /** Transação de outro usuário é tratada como inexistente (404), como no resto do app. */
    private void requireOwnedTransaction(Long currentHouseholdId, Long transactionId) {
        Transaction transaction =
                transactionRepositoryPort
                        .findById(transactionId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Transação não encontrada: " + transactionId));
        accountRepositoryPort
                .findById(transaction.accountId())
                .filter(account -> account.belongsTo(currentHouseholdId))
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Transação não encontrada: " + transactionId));
    }
}
