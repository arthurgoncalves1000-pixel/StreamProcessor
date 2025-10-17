package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.model.Conta;
import org.example.service.ImprimirJson;
import org.example.service.LeitorJson;
import org.example.service.ProcessadorJson;

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
