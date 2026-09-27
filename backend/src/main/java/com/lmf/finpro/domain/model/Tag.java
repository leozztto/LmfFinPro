package com.lmf.finpro.domain.model;

import com.lmf.finpro.domain.exception.InvalidTagException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Tag do usuário: uma classificação livre e transversal às categorias (ex.: {@code
 * projeto-site-acme}, {@code dedutível}). Uma transação pode ter várias.
 *
 * @param name nome normalizado por {@link #normalizeName}, sem o "#" (que é só de exibição)
 * @param color cor em hexadecimal ({@code #RRGGBB}) ou {@code null} para a cor neutra
 */
public record Tag(Long id, Long userId, String name, String color, LocalDateTime createdAt) {

    public static final int MAX_NAME_LENGTH = 40;

    /** Limite por transação ou recorrência — o bastante para classificar sem virar bagunça. */
    public static final int MAX_TAGS_PER_ITEM = 10;

    private static final Pattern VALID_NAME = Pattern.compile("[\\p{L}\\p{N}_-]+");
    private static final Pattern VALID_COLOR = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public Tag {
        name = normalizeName(name);
        color = normalizeColor(color);
    }

    public static Tag create(Long userId, String name, String color) {
        return new Tag(null, userId, name, color, LocalDateTime.now());
    }

    public Tag withDetails(String newName, String newColor) {
        return new Tag(id, userId, newName, newColor, createdAt);
    }

    public boolean belongsTo(Long candidateUserId) {
        return userId.equals(candidateUserId);
    }

    /** Como a tag aparece para o usuário: {@code #nome}. */
    public String label() {
        return "#" + name;
    }

    /** Tags lado a lado para colunas de relatório: "#site-acme #dedutível" ("" sem tags). */
    public static String joinLabels(Collection<Tag> tags) {
        return tags.stream().map(Tag::label).sorted().collect(Collectors.joining(" "));
    }

    /**
     * Forma única de um nome, para "#Site Acme", "site-acme" e " SITE-ACME " serem a mesma tag: sem
     * "#" no início, minúsculas, espaços viram "-". Aceita letras (com acento), números, "-" e "_".
     */
    public static String normalizeName(String raw) {
        String name = raw == null ? "" : raw.strip();
        while (name.startsWith("#")) {
            name = name.substring(1).strip();
        }
        name = WHITESPACE.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("-");
        if (name.isEmpty()) {
            throw new InvalidTagException("Informe o nome da tag.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new InvalidTagException(
                    "O nome da tag pode ter no máximo " + MAX_NAME_LENGTH + " caracteres.");
        }
        if (!VALID_NAME.matcher(name).matches()) {
            throw new InvalidTagException(
                    "A tag \"#"
                            + name
                            + "\" tem caracteres inválidos. Use letras, números, \"-\" ou \"_\".");
        }
        return name;
    }

    private static String normalizeColor(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String color = raw.strip();
        if (!VALID_COLOR.matcher(color).matches()) {
            throw new InvalidTagException("Cor inválida. Use o formato #RRGGBB.");
        }
        return color.toLowerCase(Locale.ROOT);
    }
}
