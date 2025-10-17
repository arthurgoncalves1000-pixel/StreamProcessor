package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

public class JsonStreamProcessor {
    public static void main(String[] args) throws JsonProcessingException {
        List<String> entrada, saida;
        Conta conta = new Conta(false, false, 0);
        //Leitura do arquivo de entrada
        entrada = LeitorJson.lerEntrada();
        saida = ProcessadorJson.validarArquivo(entrada, conta);
        ImprimirJson.escreverArquivo(saida);
    }
}
