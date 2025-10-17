package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;

public class ValidadorJson {

    public static boolean consistirJson(JsonNode node) {

        if (node.has("account")) {
            Iterator<String> campos = node.get("account").fieldNames();
            while (campos.hasNext()) {
                String campo = campos.next();
                if (campo.equals("active-card")) {
                    continue;
                } else if (campo.equals("available-limit")) {
                    return true;
                }
            }
        } else if (node.has("transaction")) {
            Iterator<String> campos = node.get("transaction").fieldNames();
            while (campos.hasNext()) {
                String campo = campos.next();
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
}
