package util;

import java.lang.reflect.Field;
import java.util.Map;

public class JsonUtil {

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + escapeJson((String) obj) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(entry.getKey().toString())).append("\":");
                sb.append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        // Pour les objets custom (ex. Employe, Departement), sérialiser les champs via reflection
        if (obj.getClass().getName().startsWith("java.")) {
            // Types Java standard non gérés -> fallback toString (mais évite pour custom)
            return "\"" + escapeJson(obj.toString()) + "\"";
        }
        // Objet custom : sérialiser récursivement
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        try {
            for (Field field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(field.getName())).append("\":");
                sb.append(toJson(value));  // Récursion pour les objets imbriqués
                first = false;
            }
        } catch (Exception e) {
            return "{\"error\": \"Serialization failed: " + e.getMessage() + "\"}";
        }
        sb.append("}");
        return sb.toString();
    }

    // Échapper les caractères spéciaux en JSON
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}