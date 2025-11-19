package util;

import jakarta.servlet.ServletContext;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import annotation.ControllerAnnotation;
import annotation.URLMapping;

public class ControllerScanner {

    public static class ScanResult {
        public final Map<String, Method> urlToMethod = new HashMap<>();
        public final Set<Class<?>> controllerClasses = new HashSet<>();
        public final List<UrlPattern> patterns = new ArrayList<>();
    }

    public static ScanResult scan(ServletContext ctx) {
        ScanResult result = new ScanResult();
        String classesPath = ctx.getRealPath("/WEB-INF/classes");
        if (classesPath == null) return result;
        File root = new File(classesPath);
        if (!root.exists()) return result;
        scanDir(root, root, ctx.getClassLoader(), result);
        return result;
    }

    private static void scanDir(File root, File current, ClassLoader loader, ScanResult result) {
        File[] children = current.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) {
                scanDir(root, f, loader, result);
            } else if (f.getName().endsWith(".class")) {
                String rel = root.toURI().relativize(f.toURI()).getPath();
                if (rel.contains("$")) continue; // ignore inner classes
                String fqcn = rel.replace('/', '.').replace('\\', '.');
                fqcn = fqcn.substring(0, fqcn.length() - ".class".length());
                try {
                    Class<?> cls = loader.loadClass(fqcn);
                    if (cls.isAnnotationPresent(ControllerAnnotation.class)) {
                        result.controllerClasses.add(cls);
                    }
                    for (Method m : cls.getDeclaredMethods()) {
                        if (m.isAnnotationPresent(URLMapping.class)) {
                            URLMapping u = m.getAnnotation(URLMapping.class);
                            String path = u.value();
                            if (!path.startsWith("/")) path = "/" + path;
                            // result.urlToMethod.put(path, m);
                            if (path.contains("{")) {
                                result.patterns.add(new UrlPattern(path, m));
                            } else {
                                result.urlToMethod.put(path, m);
                            }
                        }
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // classe non disponible sur le classpath au runtime ; ignorer
                }
            }
        }
    }
}
