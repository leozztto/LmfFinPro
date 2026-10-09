package com.lmf.finpro.application.privacy;

import com.lmf.finpro.application.FlowLog;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Household;
import com.lmf.finpro.domain.model.HouseholdMembership;
import com.lmf.finpro.domain.model.PersonalDataExport;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.model.UserConsent;
import com.lmf.finpro.domain.port.out.HouseholdRepositoryPort;
import com.lmf.finpro.domain.port.out.PersonalDataArchiveWriterPort;
import com.lmf.finpro.domain.port.out.PersonalDataExportPort;
import com.lmf.finpro.domain.port.out.PersonalDataExportPort.StoredAttachment;
import com.lmf.finpro.domain.port.out.UserConsentRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.io.OutputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Portabilidade dos dados (LGPD, art. 18, V): tudo o que o sistema guarda sobre o titular, num ZIP
 * com {@code dados.json}, os anexos, a foto de perfil e um LEIA-ME. Entram o espaço pessoal e os
 * grupos compartilhados de que a pessoa participa (ela já os vê no app); dos outros membros só o
 * nome.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataExportApplicationService {

    static final int FORMAT_VERSION = 1;
    private static final int FILE_NAME_MAX_LENGTH = 80;

    private final UserRepositoryPort userRepositoryPort;
    private final HouseholdRepositoryPort householdRepositoryPort;
    private final PersonalDataExportPort personalDataExportPort;
    private final UserConsentRepositoryPort userConsentRepositoryPort;
    private final PersonalDataArchiveWriterPort personalDataArchiveWriterPort;
    private final Clock clock;

    /** Monta o conteúdo antes de começar a escrever, para erros virarem uma resposta normal. */
    public PersonalDataExport prepare(Long userId) {
        log.debug("Preparando exportação dos dados do usuário={}", userId);
        User user =
                userRepositoryPort
                        .findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        List<PersonalDataExport.File> files = new ArrayList<>();
        List<Map<String, Object>> groups = new ArrayList<>();
        boolean hasSharedGroups = false;
        for (HouseholdMembership membership :
                householdRepositoryPort.findMembershipsByUserId(userId)) {
            Household household =
                    householdRepositoryPort
                            .findById(membership.householdId())
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Grupo não encontrado"));
            hasSharedGroups |= household.isShared();
            groups.add(exportGroup(household, membership, files));
        }

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("versaoDoFormato", FORMAT_VERSION);
        document.put("geradoEm", LocalDateTime.now(clock));
        document.put("usuario", personalDataExportPort.loadUser(userId));
        document.put(
                "preferenciasDeNotificacao",
                personalDataExportPort.loadNotificationPreferences(userId).orElse(null));
        document.put("primeirosPassos", personalDataExportPort.loadOnboarding(userId));
        document.put("primeirosPassos", personalDataExportPort.loadOnboarding(userId));
        document.put("aparelhosComNotificacaoPush", personalDataExportPort.loadPushDevices(userId));
        document.put("consentimentos", consents(userId));
        document.put("grupos", groups);

        if (user.hasPhoto()) {
            files.add(
                    new PersonalDataExport.File(
                            "foto-perfil." + extensionOf(user.photoKey()), user.photoKey()));
        }
        FlowLog.detail("groups", groups.size());
        FlowLog.detail("files", files.size());
        return new PersonalDataExport(document, files, readme(hasSharedGroups));
    }

    public void write(PersonalDataExport export, OutputStream output) {
        log.debug("Escrevendo exportação de dados com {} arquivo(s)", export.files().size());
        personalDataArchiveWriterPort.write(export, output);
    }

    private Map<String, Object> exportGroup(
            Household household,
            HouseholdMembership membership,
            List<PersonalDataExport.File> files) {
        Long householdId = household.id();
        Map<String, List<Map<String, Object>>> data =
                personalDataExportPort.loadHouseholdData(householdId);

        // Cada linha de anexo ganha o caminho do arquivo dentro do ZIP.
        Map<Long, String> pathByAttachmentId = new HashMap<>();
        for (StoredAttachment attachment : personalDataExportPort.loadAttachments(householdId)) {
            String path =
                    "anexos/grupo-"
                            + householdId
                            + "/"
                            + attachment.id()
                            + "-"
                            + safeFileName(attachment.fileName());
            pathByAttachmentId.put(attachment.id(), path);
            files.add(new PersonalDataExport.File(path, attachment.storageKey()));
        }
        List<Map<String, Object>> attachmentRows = new ArrayList<>();
        for (Map<String, Object> row : data.getOrDefault("transaction_attachments", List.of())) {
            Map<String, Object> withPath = new LinkedHashMap<>(row);
            Long id = ((Number) row.get("id")).longValue();
            withPath.put("arquivo_no_zip", pathByAttachmentId.get(id));
            attachmentRows.add(withPath);
        }
        Map<String, Object> tables = new LinkedHashMap<>(data);
        tables.put("transaction_attachments", attachmentRows);

        Map<String, Object> group = new LinkedHashMap<>();
        group.put("id", householdId);
        group.put("nome", household.name());
        group.put("tipo", household.type());
        group.put("seuPapel", membership.role());
        group.put("membros", personalDataExportPort.loadMembers(householdId));
        group.put("dados", tables);
        return group;
    }

    private List<Map<String, Object>> consents(Long userId) {
        List<Map<String, Object>> consents = new ArrayList<>();
        for (UserConsent consent : userConsentRepositoryPort.findByUserId(userId)) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("documento", consent.documentType());
            entry.put("versao", consent.version());
            entry.put("aceitoEm", consent.acceptedAt());
            consents.add(entry);
        }
        return consents;
    }

    /** Nome de arquivo seguro para dentro do ZIP: sem barras, sem ".." e de tamanho limitado. */
    static String safeFileName(String original) {
        String name = original == null ? "" : original.replaceAll("[^A-Za-z0-9._-]", "_");
        name = name.replaceAll("\\.{2,}", ".");
        if (name.length() > FILE_NAME_MAX_LENGTH) {
            name = name.substring(name.length() - FILE_NAME_MAX_LENGTH);
        }
        return name.isBlank() ? "arquivo" : name;
    }

    private static String extensionOf(String key) {
        int dot = key.lastIndexOf('.');
        return dot < 0 ? "bin" : key.substring(dot + 1);
    }

    private static String readme(boolean hasSharedGroups) {
        StringBuilder text = new StringBuilder();
        text.append("FinPro - exportação dos seus dados\n");
        text.append("===================================\n\n");
        text.append(
                "Este pacote reúne os dados que o FinPro guarda sobre você (LGPD, art. 18).\n\n");
        text.append("Conteúdo\n--------\n");
        text.append(
                "- dados.json: cadastro, preferências, aceites dos documentos legais e, para\n");
        text.append("  cada grupo de que você participa, as tabelas com todos os registros\n");
        text.append("  (contas, lançamentos, transferências, categorias, clientes, metas etc.).\n");
        text.append("  Os nomes das tabelas e colunas seguem o banco de dados do sistema.\n");
        text.append(
                "- anexos/: os comprovantes e documentos anexados às transações. Cada registro\n");
        text.append("  de transaction_attachments traz o campo arquivo_no_zip com o caminho.\n");
        text.append("- foto-perfil.*: sua foto de perfil, se houver.\n\n");
        text.append("Observações\n-----------\n");
        text.append("- Datas e horas estão no horário de Brasília, sem indicação de fuso.\n");
        text.append(
                "- Senhas (mesmo em forma de hash), tokens de sessão e chaves de notificação\n");
        text.append("  não são exportados por segurança.\n");
        if (hasSharedGroups) {
            text.append(
                    "- Os grupos compartilhados (casal/família) vêm completos, porque você já\n");
            text.append(
                    "  tem acesso a eles no app. Dos outros membros constam só nome e papel.\n");
        }
        return text.toString();
    }
}
