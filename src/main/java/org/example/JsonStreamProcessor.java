package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JsonStreamProcessor {
    public static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        List<String> entrada;
        Conta conta = new Conta(false, false, 0);
        //Leitura do arquivo de entrada
        entrada = lerArquivo();
        escreverArquivo(entrada, conta);
    }

    private static boolean consistirJson(JsonNode node) {

        if (node.has("account")) {
            Iterator<String> fields = node.get("account").fieldNames();
            while (fields.hasNext()) {
                String campo = fields.next();
                if (campo.equals("active-card")) {
                    continue;
                } else if (campo.equals("available-limit")) {
                    return true;
                }
            }
        } else if (node.has("transaction")) {
            Iterator<String> fields = node.get("transaction").fieldNames();
            while (fields.hasNext()) {
                String campo = fields.next();
                if (campo.equals("merchant")) {
                    continue;
                } else if (campo.equals("amount")) {
                    continue;
                } else if (campo.equals("time")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<String> lerArquivo(){
        List<String> entrada = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
             PrintStream ignored = System.out) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                if (linha.trim().isEmpty()) break; // Encerra a leitura

                try {
                    JsonNode jsonNode = mapper.readTree(linha);
                    //Consistir Json
                    if (!consistirJson(jsonNode)) throw new RuntimeException("JSON INVALIDO");

                    //adiciona linha de entrada caso json seja valido
                    entrada.add(mapper.writeValueAsString(jsonNode));

                } catch (JsonProcessingException e) {
                    System.err.println("Invalid JSON input: " + linha + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo: " + e.getMessage());
        }
        return entrada;
    }

    private static boolean inicializaConta(JsonNode jsonNode, Conta conta, List<String> entrada, int linha){
        if (jsonNode.has("account")) {
            if (!conta.isActiveCard() && !conta.isInit()) {
                conta.setInit(true);
                conta.setActiveCard(jsonNode.get("account").get("active-card").asBoolean());
                conta.setAvailableLimit(jsonNode.get("account").get("available-limit").asLong());
                entrada.remove(linha);
                return true;
            } else {
                conta.adicionarViolacao("account-already-initialized");
            }
        } else if (jsonNode.has("transaction") && !conta.isInit()) {
            conta.adicionarViolacao("account-not-initialized");
            entrada.remove(linha);
        }
        return false;
    }
    private static void validarTransacao(JsonNode transaNode, Conta conta, List<String> entrada, int linha) {
        long amount = transaNode.get("amount").asLong();

        if (!conta.isActiveCard()) {
            conta.adicionarViolacao("card-not-active");
            //return false;
        }

        int countTransacoes = consisteTransaProxima(entrada, linha, transaNode);
        boolean limitePorFreq = countTransacoes >= 3;

        boolean duplicada = consisteTransaDuplicada(entrada, linha, transaNode, amount);
        boolean limiteSuficiente = (conta.getAvailableLimit() - amount) >= 0;

        if (limitePorFreq) {
            conta.adicionarViolacao("high-frequency-small-interval");
        }
        if (duplicada) {
            conta.adicionarViolacao("doubled-transaction");
        }

        if (!limiteSuficiente) {
            conta.adicionarViolacao("insufficient-limit");
        }

        boolean podeRealizarTransacao = !limitePorFreq && !duplicada && limiteSuficiente && conta.isActiveCard();

        if (podeRealizarTransacao) {
            conta.transa(amount);
        } else {
            // Remove a linha do array de entrada caso alguma violação impeça a transação
            entrada.remove(linha);
        }
    }

    private static void escreverArquivo(List<String> entrada, Conta conta){
        String arquivo = "saida.txt";
        File f = new File(arquivo);
        if (f.exists()) f.delete();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(arquivo, true))) {
            int linha = 0;
            while (linha < entrada.size()) {

                String linhaJson = entrada.get(linha);
                JsonNode jsonNode = mapper.readTree(linhaJson);

                if (!inicializaConta(jsonNode, conta, entrada, linha)) {
                    if(jsonNode.has("transaction") && conta.isInit()){
                        JsonNode transaNode = jsonNode.get("transaction");
                        validarTransacao(transaNode, conta, entrada, linha);
                    }
                }

                //saida
                ObjectNode contaNode = mapper.createObjectNode();
                if (conta.isInit()) {
                    contaNode.put("active-card", conta.isActiveCard());
                    contaNode.put("available-limit", conta.getAvailableLimit());
                }
                ObjectNode rootNode = mapper.createObjectNode();
                rootNode.set("account", contaNode);
                ArrayNode violationsArray = mapper.createArrayNode();
                String concatViolacoes;
                if (conta.getViolations() != null && !conta.getViolations().isEmpty()) {
                    concatViolacoes = String.join(", ", conta.getViolations());
                    violationsArray.add(concatViolacoes);
                }
                rootNode.set("violations", violationsArray);

                String jsonString;
                jsonString = mapper.writeValueAsString(rootNode);

                writer.write(jsonString);
                writer.newLine(); // uma linha com o JSON
                if (!jsonNode.has("account") && conta.isInit() && conta.getViolations() == null)
                    linha += 1;

                conta.limparViolacoes();
            }

        } catch (IOException e) {
            System.err.println("Erro ao gravar arquivo: " + e.getMessage());
        }
    }

    private static Duration diffData(String anterior, String atual){
        OffsetDateTime dataAnterior = OffsetDateTime.parse(anterior);
        OffsetDateTime dataAtual = OffsetDateTime.parse(atual);
        return Duration.between(dataAnterior, dataAtual);
    }

    private static boolean consisteTransaDuplicada(List<String> entrada, int linha, JsonNode transaNode, long amount){
        try {
            for (int j = 0; j < entrada.size(); j++) {
                if (j == linha) break;
                JsonNode anterior = mapper.readTree(entrada.get(j)).get("transaction");
                Duration diff = diffData(anterior.get("time").asText(), transaNode.get("time").asText());
                if (anterior.get("merchant").asText().equals(transaNode.get("merchant").asText())
                        && anterior.get("amount").asInt() == amount
                        && Math.abs(diff.toMinutes()) <= 2) {
                    return true;
                }
            }
        }
        catch (Exception e){
            System.err.println("Erro ao consistir transacoes: "+ e.getMessage());
        }
        return false;
    }

    private static int consisteTransaProxima(List<String> entrada, int linha, JsonNode transaNode){
        int countAnterior = 0;
        try{
        for (int j = 0; j < linha; j++) {
            JsonNode anterior = mapper.readTree(entrada.get(j)).get("transaction");
            Duration diff = diffData(anterior.get("time").asText(), transaNode.get("time").asText());
            if (!diff.isNegative() && diff.toMinutes() < 2)
                countAnterior++;
        }
        }
        catch (Exception e){
            System.err.println("Erro ao consistir transacoes: "+ e.getMessage());
        }
        return countAnterior;
    }
}
