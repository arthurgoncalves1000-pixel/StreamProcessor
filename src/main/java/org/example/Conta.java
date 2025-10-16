package org.example;

import java.util.ArrayList;
import java.util.List;

public class Conta {
    private boolean init;
    private boolean activeCard;
    private long    availableLimit;
    private List<String> violations;

    public Conta(boolean init, boolean activeCard, long availableLimit){
        this.init           = init;
        this.activeCard     = activeCard;
        this.availableLimit = availableLimit;
        this.violations     = new ArrayList<>();
    }


    public void debitar(long valor){
        if (this.availableLimit - valor >= 0){
            this.availableLimit -= valor;
        }
    }

    public void adicionarViolacao(String novaViolacao) {
        if(this.violations == null)
            this.violations = new ArrayList<>();
        this.violations.add(novaViolacao);
    }

    public void limparViolacoes() {
        this.violations.clear();
    }

    public boolean verificarLimite(long valor){
        return this.availableLimit - valor >= 0;
    }

    public boolean isActiveCard() {
        return activeCard;
    }

    public void setActiveCard(boolean activeCard) {
        this.activeCard = activeCard;
    }

    public long getAvailableLimit() {
        return availableLimit;
    }

    public void setAvailableLimit(long availableLimit) {
        this.availableLimit = availableLimit;
    }

    public List<String> getViolations() {
        return violations;
    }

    public boolean isInit() {
        return init;
    }

    public void setInit(boolean init) {
        this.init = init;
    }
}