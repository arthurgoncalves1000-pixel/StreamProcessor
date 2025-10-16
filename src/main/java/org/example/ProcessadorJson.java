package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProcessadorJson {
    private static ObjectMapper mapper = new ObjectMapper();

    private static boolean inicializaConta(JsonNode jsonNode, Conta conta, List<String> entrada, int linha) {
        if (jsonNode.has("account")) {
            if (!conta.isActiveCard() && !conta.isInit()) {
                conta.setInit(true);
                conta.setActiveCard(jsonNode.get("account").get("active-card").asBoolean());
                conta.setAvailableLimit(jsonNode.get("account").get("available-limit").asLong());
                //entrada.remove(linha);
                return true;
            } else {
                conta.adicionarViolacao("account-already-initialized");
            }
        } else if (jsonNode.has("transaction") && !conta.isInit()) {
            conta.adicionarViolacao("account-not-initialized");
           // entrada.remove(linha);
        }
        return false;
    }

    private static void validarTransacao(JsonNode transaNode, Conta conta, List<String> entrada, int linha) {
        long valor = transaNode.get("amount").asLong();

        if (!conta.isActiveCard()) {
            conta.adicionarViolacao("card-not-active");
            //return false;
        }else{
            if (contarTransacoesRecentes(entrada, linha, transaNode)) {
                conta.adicionarViolacao("high-frequency-small-interval");
            }
            if (consisteTransacoesDuplicada(entrada, linha, transaNode, valor)) {
                conta.adicionarViolacao("doubled-transaction");
            }
            if (!conta.verificarLimite(valor)) {
                conta.adicionarViolacao("insufficient-limit");
            }
        }

        if (conta.getViolations().isEmpty() && conta.isActiveCard()) {
            conta.debitar(valor);
        } //else {
            // Remove a linha do array de entrada caso alguma violação impeça a transação
           // entrada.remove(linha);
        //}
    }

    public static List<String> validarArquivo(List<String> entrada, Conta conta) throws JsonProcessingException {
        int linha = 0;
        List<String> listaDeSaida = new ArrayList<>();
        while (linha < entrada.size()) {
            String linhaJson = entrada.get(linha);
            JsonNode jsonNode = mapper.readTree(linhaJson);
            if (!inicializaConta(jsonNode, conta, entrada, linha)) {
                if (jsonNode.has("transaction") && conta.isInit()) {
                    JsonNode transaNode = jsonNode.get("transaction");
                    validarTransacao(transaNode, conta, entrada, linha);
                }
            }
            listaDeSaida.add(escreverLinhaDeSaida(conta));
            //caso a conta esteja inicializada e sem violaçoes, incrementa, caso contrario remove
            if (!jsonNode.has("account") && conta.isInit() && conta.getViolations().isEmpty())
                linha++;
            else
                apagarLinha(entrada, linha);

            conta.limparViolacoes();
        }
        return listaDeSaida;
    }

    private static void apagarLinha(List<String> entrada, int linha){
        entrada.remove(linha);
    }

    private static String escreverLinhaDeSaida(Conta conta) throws JsonProcessingException {
        ObjectNode contaNode = mapper.createObjectNode();
        if (conta.isInit()) {
            contaNode.put("active-card", conta.isActiveCard());
            contaNode.put("available-limit", conta.getAvailableLimit());
        }
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("account", contaNode);
        ArrayNode violationsArray = mapper.createArrayNode();
        String concatViolacoes;
        if (!conta.getViolations().isEmpty()) {
            concatViolacoes = String.join(", ", conta.getViolations());
            violationsArray.add(concatViolacoes);
        }
        rootNode.set("violations", violationsArray);

        return mapper.writeValueAsString(rootNode);
    }

    private static Duration diffData(String anterior, String atual) {
        OffsetDateTime dataAnterior = OffsetDateTime.parse(anterior);
        OffsetDateTime dataAtual = OffsetDateTime.parse(atual);
        return Duration.between(dataAnterior, dataAtual);
    }

    private static boolean consisteTransacoesDuplicada(List<String> entrada, int linha, JsonNode transaNode, long amount) {
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
        } catch (Exception e) {
            System.err.println("Erro ao consistir transacoes: " + e.getMessage());
        }
        return false;
    }

    private static boolean contarTransacoesRecentes(List<String> entrada, int linha, JsonNode transaNode) {
        int countAnterior = 0;
        try {
            for (int j = 0; j < linha; j++) {
                JsonNode anterior = mapper.readTree(entrada.get(j)).get("transaction");
                Duration diff = diffData(anterior.get("time").asText(), transaNode.get("time").asText());
                if (!diff.isNegative() && diff.toMinutes() < 2)
                    countAnterior++;
            }
        } catch (Exception e) {
            System.err.println("Erro ao consistir transacoes: " + e.getMessage());
        }
        return countAnterior >= 3;
    }
}
