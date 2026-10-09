package com.lmf.finpro.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Pacote de portabilidade dos dados de um titular (LGPD, art. 18, V): o documento JSON com tudo o
 * que o sistema guarda sobre ele, os arquivos (anexos e foto) e o texto do LEIA-ME. Os arquivos
 * ficam como referências para o escritor do ZIP lê-los um a um, sem carregar o pacote na memória.
 *
 * @param document o conteúdo do {@code dados.json}
 * @param files os arquivos a incluir, com o caminho dentro do ZIP
 * @param readme o texto do {@code LEIA-ME.txt}
 */
public record PersonalDataExport(Map<String, Object> document, List<File> files, String readme) {

    /**
     * @param path caminho dentro do ZIP; @param storageKey chave no armazenamento de arquivos
     */
    public record File(String path, String storageKey) {}
}
