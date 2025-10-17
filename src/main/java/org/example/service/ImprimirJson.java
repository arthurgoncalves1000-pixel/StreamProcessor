package org.example.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ImprimirJson {

    public static void escreverArquivo(List<String> saida) {
        String arquivo = "saida.txt";
        File f = new File(arquivo);
        if (f.exists()) f.delete();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(arquivo))) {
            for (String linhaJsonSaida : saida) {
                writer.write(linhaJsonSaida);
                writer.newLine(); // uma linha com o JSON
            }

        } catch (IOException e) {
            System.err.println("Erro ao gravar arquivo: " + e.getMessage());
        }
    }
}
