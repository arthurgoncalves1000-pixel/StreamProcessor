package example;

import org.example.model.Conta;
import org.example.service.ProcessadorJson;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class TestesJunit {

    @Test
    public void deveProcessarTransacaoValida() throws Exception {
        List<String> entrada = new ArrayList<>();
        entrada.add("{\"account\": {\"active-card\": true, \"available-limit\": 200}}");
        entrada.add("{\"transaction\": {\"merchant\": \"Uber\", \"amount\": 50, \"time\": \"2025-10-16T10:00:00Z\"}}");
        entrada.add("{\"transaction\": {\"merchant\": \"Uber\", \"amount\": 25, \"time\": \"2025-10-16T10:00:00Z\"}}");
        Conta conta = new Conta(false, false, 0);
        List<String> resultado = ProcessadorJson.validarArquivo(entrada, conta);
        for (String s : resultado) {
            System.out.println(s);
        }

        assertTrue(resultado.stream().anyMatch(s -> s.contains("\"available-limit\":125")));
        assertEquals(125, conta.getAvailableLimit());
    }

    @Test
    public void deveIdentificarLimiteInsuficiente() throws Exception {
        List<String> entrada = new ArrayList<>();
        entrada.add("{\"account\": {\"active-card\": true, \"available-limit\": 50}}");
        entrada.add("{\"transaction\": {\"merchant\": \"Amazon\", \"amount\": 100, \"time\": \"2025-10-16T10:00:00Z\"}}");
        Conta conta = new Conta(false, false, 0);
        List<String> resultado = ProcessadorJson.validarArquivo(entrada, conta);

        assertTrue(resultado.stream().anyMatch(s -> s.contains("insufficient-limit")));

    }

    @Test
    public void deveDetectarTransacaoDobra() throws Exception {
        List<String> entrada = new ArrayList<>();
        entrada.add("{\"account\": {\"active-card\": true, \"available-limit\": 500}}");
        entrada.add("{\"transaction\": {\"merchant\": \"Amazon\", \"amount\": 100, \"time\": \"2025-10-16T10:00:00Z\"}}");
        entrada.add("{\"transaction\": {\"merchant\": \"Amazon\", \"amount\": 100, \"time\": \"2025-10-16T10:01:30Z\"}}");
        Conta conta = new Conta(false, false, 0);
        List<String> resultado = ProcessadorJson.validarArquivo(entrada, conta);
        assertTrue(resultado.stream().anyMatch(s -> s.contains("doubled-transaction")));

    }

    @Test
    public void deveDetectarMultiplasViolacoes() throws Exception {
        List<String> entrada = new ArrayList<>();
        // Conta com limite baixo
        entrada.add("{\"account\": {\"active-card\": true, \"available-limit\": 50}}");
        // Transação que ultrapassa limite
        entrada.add("{\"transaction\": {\"merchant\": \"Uber\", \"amount\": 100, \"time\": \"2025-10-16T10:00:00Z\"}}");
        // Transações frequentes em 2 minutos
        entrada.add("{\"transaction\": {\"merchant\": \"Uber\", \"amount\": 10, \"time\": \"2025-10-16T10:01:00Z\"}}");
        entrada.add("{\"transaction\": {\"merchant\": \"Uber\", \"amount\": 10, \"time\": \"2025-10-16T10:01:30Z\"}}");
        entrada.add("{\"transaction\": {\"merchant\": \"Uber\", \"amount\": 10, \"time\": \"2025-10-16T10:01:45Z\"}}");
        // Transação com duplicidade (mesmo merchant, valor e dentro de 2 min)
        entrada.add("{\"transaction\": {\"merchant\": \"Uber\", \"amount\": 10, \"time\": \"2025-10-16T10:00:30Z\"}}");

        Conta conta = new Conta(false, false, 0);
        List<String> saida = ProcessadorJson.validarArquivo(entrada, conta);
        String jsonSaida = saida.get(saida.size() - 1);
        assertTrue(jsonSaida.contains("doubled-transaction"));
    }
}
