package io.github.pedrodevsi.fleetagent.knowledge.utils;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class HashDocument {

    public static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); //Cria gerador de hash

            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8)); //Transforma o texto em bytes

            return HexFormat.of().formatHex(hashBytes); // Transforma os bytes em texto legível
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 nao encontrado", e);
        }
    }
}
