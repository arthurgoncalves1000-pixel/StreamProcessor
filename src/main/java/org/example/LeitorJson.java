package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class LeitorJson {
    private static ObjectMapper mapper = new ObjectMapper();

    public static List<String> lerEntrada() {
        List<String> entrada = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                if (linha.trim().isEmpty()) break; // Encerra a leitura

                try {
                    JsonNode jsonNode = mapper.readTree(linha);
                    //Consistir Json
                    if (!ValidadorJson.consistirJson(jsonNode)) throw new RuntimeException("JSON INVALIDO");

                    //adiciona linha de entrada caso json seja valido
                    entrada.add(mapper.writeValueAsString(jsonNode));

                } catch (JsonProcessingException e) {
                    System.err.println("Invalid JSON input: " + linha + " " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo: " + e.getMessage());
        }
        return entrada;
    }
}
