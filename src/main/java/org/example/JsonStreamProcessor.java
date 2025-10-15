package org.example;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.util.Iterator;

public class JsonStreamProcessor {

    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();
        Conta conta = new Conta(false, false, 0);
        String arquivo = "saida.txt";
        File f = new File(arquivo);
        if(f.exists()) f.delete();


        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
             PrintStream out = System.out) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue; // Skip empty lines

                try {
                    //System.out.println(line);
                    // Parse incoming JSON line
                    JsonNode jsonNode = mapper.readTree(line);

                    //Consistir Json
                    if (!consistirJson(jsonNode)) throw new RuntimeException(" JSON INVALIDO");

                    //inicializar conta
                    if(jsonNode.fieldNames().next().equals("account")){
                       if(!conta.isActiveCard() && !conta.isInit()){
                            conta.setInit(true);
                            conta.setActiveCard(jsonNode.get("account").get("active-card").asBoolean());
                            conta.setAvailableLimit(jsonNode.get("account").get("available-limit").asLong());
                       }
                       else{
                           conta.adicionarViolacao("account-already-initialized");
                       }
                    } else if(jsonNode.fieldNames().next().equals("transaction") && !conta.isInit()) {
                           conta.adicionarViolacao("account-not-initialized");
                    }

                    if(jsonNode.fieldNames().next().equals("transaction") && conta.isInit()){
                        boolean hasLimit;
                        JsonNode transaNode = jsonNode.get("transaction");
                        Long amount = transaNode.get("amount").asLong();
                        if(!conta.isActiveCard()){
                            conta.adicionarViolacao("card-not-active");
                        }else{
                            hasLimit = conta.transa(amount);
                            if(!hasLimit){
                                conta.adicionarViolacao("insufficient-limit");
                            }
                        }
                    }



                    //saida
                    ObjectMapper mapperSaida = new ObjectMapper();

                    ObjectNode contaNode = mapper.createObjectNode();
                    if(conta.isInit()){
                        contaNode.put("active-card", conta.isActiveCard());
                        contaNode.put("available-limit", conta.getAvailableLimit());
                    }
                    ObjectNode rootNode = mapper.createObjectNode();
                    rootNode.set("account", contaNode);
                    ArrayNode violationsArray = mapper.createArrayNode();
                    String concatViola;
                    if(conta.getViolations() != null && !conta.getViolations().isEmpty()){
                        concatViola = String.join(", ", conta.getViolations());
                        violationsArray.add(concatViola);

                    }
                    rootNode.set("violations", violationsArray);
                    conta.limparViolacoes();
                    //conta.setViolations("");

                    String jsonString;
                    jsonString = mapper.writeValueAsString(rootNode);
                    // Output as JSON line
                    //out.println(mapper.writeValueAsString(jsonString));
                    // Escrever no arquivo
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(arquivo,true))) {
                        writer.write(jsonString);
                        writer.newLine(); // uma linha com o JSON
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (RuntimeException e) {
                    // In case of parsing error, you can decide to log or skip
                    System.err.println("Invalid JSON input: " + line + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JsonNode processJson(JsonNode jsonNode) {
        // Placeholder for processing logic
        // For now, just return the input node
        return jsonNode;
    }

    private static boolean consistirJson(JsonNode node) throws Exception{

        if(node.fieldNames().next().equals("account")){
            Iterator<String> fields = node.get("account").fieldNames();
            while (fields.hasNext()){
                String campo = fields.next();
                if(campo.equals("active-card")){
                    continue;
                } else if (campo.equals("available-limit")) {
                    return true;
                }
            }
        } else if(node.fieldNames().next().equals("transaction")){
            Iterator<String> fields = node.get("transaction").fieldNames();
            while (fields.hasNext()){
                String campo = fields.next();
                if(campo.equals("merchant")){
                    continue;
                } else if (campo.equals("amount")) {
                    continue;
                } else if (campo.equals("time")){
                    return true;
                }
            }
        }
        return false;
    }
}
