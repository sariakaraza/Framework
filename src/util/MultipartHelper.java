package util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultipartHelper {

    public static boolean isMultipart(HttpServletRequest req) {
        String ct = req.getContentType();
        return ct != null && ct.toLowerCase().startsWith("multipart/");
    }

    public static Map<String, List<UploadedFile>> filesGroupedByInput(HttpServletRequest req) throws Exception {
        Map<String, List<UploadedFile>> map = new HashMap<>();
        if (!isMultipart(req)) return map;
        for (Part p : req.getParts()) {
            String submitted = p.getSubmittedFileName();
            if (submitted == null) continue; // champ texte
            String inputName = p.getName();   // ex: "fichiers" ou "monfichier"
            UploadedFile uf = UploadedFile.fromPart(p);
            map.computeIfAbsent(inputName, k -> new ArrayList<>()).add(uf);
        }
        return map;
    }
}