import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import annotation.GetMapping;
import annotation.PostMapping;
import annotation.URLMapping;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.ControllerScanner;
import util.ControllerScanner.ScanResult;
import util.UrlPattern;
import annotation.*;
import java.lang.reflect.*;

public class FrontServlet extends HttpServlet {

    RequestDispatcher defaultDispatcher;
    ScanResult scanResult = new ScanResult();

    @Override
    public void init() {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
        scanResult = ControllerScanner.scan(getServletContext());
        getServletContext().setAttribute("scanResult", scanResult);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        // normaliser : si vide => "/"
        if (path == null || path.isEmpty())
            path = "/";

        // 1) gérer la racine ("/")
        if (handleRoot(path, req, res))
            return;

        // 2) si ressource statique existante, déléguer au default dispatcher
        if (resourceExists(path)) {
            defaultServe(req, res);
            return;
        }

        // 3) vérifier correspondance exacte (urlToMethod)
        if (handleExactMapping(path, req, res))
            return;

        // 4) vérifier patterns (ex: /test/{id})
        if (handlePatternMapping(path, req, res))
            return;

        // 5) aucune route connue -> comportement personnalisé
        customServe(req, res);
    }

    private boolean resourceExists(String path) {
        try {
            return getServletContext().getResource(path) != null;
        } catch (Exception e) {
            return false;
        }
    }

    // handle root: try index.html or index.jsp, otherwise show custom welcome (pas
    // 404)
    private boolean handleRoot(String path, HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!"/".equals(path))
            return false;
        if (resourceExists("/index.html") || resourceExists("/index.jsp")) {
            defaultServe(req, res);
            return true;
        } else {
            try (PrintWriter out = res.getWriter()) {
                res.setContentType("text/html;charset=UTF-8");
                out.println(
                        "<html><head><title>Accueil</title></head><body><h1>Bienvenue</h1><p>Racine de l'application.</p></body></html>");
            }
            return true;
        }
    }

    // handle exact mapping from scanResult.urlToMethod
    private boolean handleExactMapping(String path, HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        ScanResult scanResultContext = (ScanResult) getServletContext().getAttribute("scanResult");

        List<Method> methods = scanResultContext.urlToMethods.get(path);
        if (methods == null)
            return false;

        String httpVerb = req.getMethod();
        Method selected = null;

        for (Method m : methods) {
            if ("GET".equals(httpVerb) && m.isAnnotationPresent(GetMapping.class)) {
                selected = m;
                break;
            }
            if ("POST".equals(httpVerb) && m.isAnnotationPresent(PostMapping.class)) {
                selected = m;
                break;
            }
            if (m.isAnnotationPresent(URLMapping.class)) {
                selected = m;
                break;
            }
        }
        if (selected == null)
            return false;

        Object[] args = buildArgsFromRequest(selected, req);
        invokeAndRender(selected, req, res, args);
        return true;

    }

    // sprint6 ter deja gere ici
    // handle pattern mappings like /test/{id}
    private boolean handlePatternMapping(String path, HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        ScanResult scanResultContext = (ScanResult) getServletContext().getAttribute("scanResult");
        List<UrlPattern> patterns = scanResultContext.patterns;
        String httpVerb = req.getMethod();
        for (UrlPattern p : patterns) {
            if ("GET".equalsIgnoreCase(httpVerb) && p.method.isAnnotationPresent(PostMapping.class))
                continue;
            if ("POST".equalsIgnoreCase(httpVerb) && p.method.isAnnotationPresent(GetMapping.class))
                continue;
            Matcher matcher = p.regex.matcher(path);
            if (matcher.matches()) {
                Class<?>[] paramTypes = p.method.getParameterTypes();
                Object[] args = new Object[paramTypes.length];
                for (int i = 0; i < paramTypes.length && i < matcher.groupCount(); i++) {
                    String val = matcher.group(i + 1);
                    args[i] = convertString(val, paramTypes[i]);
                    req.setAttribute("pathVar" + (i + 1), val);
                }
                invokeAndRender(p.method, req, res, args);
                return true;
            }
        }
        return false;
    }

    // Invocation centrale qui instancie la classe, appelle la méthode et gère les
    // retours (String / ModelView / void)
    private void invokeAndRender(Method m, HttpServletRequest req, HttpServletResponse res, Object[] args)
            throws ServletException, IOException {

        ScanResult scanResultContext = (ScanResult) getServletContext().getAttribute("scanResult");
        Class<?> cls = m.getDeclaringClass();
        boolean isController = scanResultContext.controllerClasses.contains(cls);
        boolean isJson = m.isAnnotationPresent(annotation.JSON.class);

        try {
            // ✅ NE PAS écrire de contentType ici pour le JSON
            if (!isJson) {
                res.setContentType("text/plain;charset=UTF-8");
            }

            try (PrintWriter out = res.getWriter()) {

                if (!isController) {
                    out.printf("NON ANNOTEE PAR LE ControllerAnnotation : %s%n", cls.getName());
                    return;
                }

                Object instance = cls.getDeclaredConstructor().newInstance();

                // ✅ Logs UNIQUEMENT si PAS JSON
                if (!isJson) {
                    out.printf("Classe: %s%n", cls.getName());
                    out.printf("Methode: %s%n", m.getName());
                }

                try {
                    m.setAccessible(true);
                    Object result = (args == null || args.length == 0)
                            ? m.invoke(instance)
                            : m.invoke(instance, args);

                    /* =================== JSON =================== */
                    if (isJson) {
                        res.setContentType("application/json;charset=UTF-8");

                        String json = util.JsonUtil.toJson(result);
                        out.print(json);
                        return;
                    }

                    /* =================== TON CODE ACTUEL =================== */
                    if (result instanceof String) {
                        out.printf("Methode string invoquee : %s", (String) result);

                    } else if (result instanceof view.ModelView) {

                        view.ModelView mv = (view.ModelView) result;
                        String vue = mv.getView();

                        for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                            req.setAttribute(entry.getKey(), entry.getValue());
                        }

                        RequestDispatcher dispatcher = req.getRequestDispatcher("/" + vue);
                        dispatcher.forward(req, res);
                        return;
                    }
                    // void ou autre : rien à faire

                } catch (IllegalAccessException | InvocationTargetException ex) {
                    out.println("Erreur invocation: " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    // convertit une chaîne vers un type basique supporté
    private Object convertString(String val, Class<?> t) {
        if (t == String.class)
            return val;
        if (t == int.class || t == Integer.class) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        if (t == long.class || t == Long.class) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        // fallback -> renvoyer la chaîne
        return val;
    }

    private Object[] buildArgsFromRequest(Method m, HttpServletRequest req) {
        Parameter[] params = m.getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            Class<?> type = p.getType();

            // 1) Cas spécial : paramètre Map<String, Object>
            if (Map.class.isAssignableFrom(type)) {
                Map<String, String[]> rawMap = req.getParameterMap();
                Map<String, Object> result = new java.util.HashMap<>();

                rawMap.forEach((key, values) -> {
                    if (values != null && values.length == 1) {
                        // conversion simple
                        result.put(key, autoConvert(values[0]));
                    } else {
                        result.put(key, values);
                    }
                });

                args[i] = result;
                continue;
            }

            if (!type.isPrimitive() && !type.getName().startsWith("java.")) {
                String prefix = p.getName();
                args[i] = buildBeanFromPrefix(prefix, type, req);
                continue;
            }

            // 2) Cas normal : @RequestParam ou nom du paramètre
            annotation.RequestParam rp = p.getAnnotation(annotation.RequestParam.class);
            String paramName = (rp != null) ? rp.value() : p.getName();
            String rawValue = req.getParameter(paramName);

            args[i] = convertString(rawValue, type);
        }

        return args;
    }

    private Object buildBeanFromPrefix(String prefix, Class<?> beanClass, HttpServletRequest req) {
        try {
            Object bean = beanClass.getDeclaredConstructor().newInstance();
            Map<String, String[]> params = req.getParameterMap();

            for (String key : params.keySet()) {
                if (key.startsWith(prefix + ".")) {

                    // e.departement.nom → ["departement", "nom"]
                    String[] parts = key.substring(prefix.length() + 1).split("\\.");

                    Object current = bean;
                    Class<?> currentClass = beanClass;

                    for (int i = 0; i < parts.length; i++) {

                        String fieldName = parts[i];
                        java.lang.reflect.Field field = currentClass.getDeclaredField(fieldName);
                        field.setAccessible(true);

                        if (i == parts.length - 1) {
                            // Dernier élément → setter simple
                            String value = req.getParameter(key);
                            Object converted = autoConvert(value);
                            field.set(current, converted);
                        } else {
                            // On descend dans l'objet imbriqué
                            Object nested = field.get(current);
                            if (nested == null) {
                                nested = field.getType().getDeclaredConstructor().newInstance();
                                field.set(current, nested);
                            }
                            current = nested;
                            currentClass = field.getType();
                        }
                    }
                }
            }

            return bean;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Object autoConvert(String val) {
        if (val == null)
            return null;

        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
        }

        try {
            return Long.parseLong(val);
        } catch (Exception e) {
        }

        return val;
    }

    private void customServe(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try (PrintWriter out = res.getWriter()) {
            String uri = req.getRequestURI();
            String responseBody = """
                    <html>
                        <head><title>Resource Not Found</title></head>
                        <body>
                            <h1>Unknown resource</h1>
                            <p>The requested URL was not found: <strong>%s</strong></p>
                        </body>
                    </html>
                    """.formatted(uri);

            res.setContentType("text/html;charset=UTF-8");
            out.println(responseBody);
        }
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        defaultDispatcher.forward(req, res);
    }

}