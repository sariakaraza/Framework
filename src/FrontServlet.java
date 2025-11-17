
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.ControllerScanner;
import util.ControllerScanner.ScanResult;

/**
 * This is the servlet that takes all incoming requests targeting the app - If
 * the requested resource exists, it delegates to the default dispatcher - else
 * it shows the requested URL
 */
public class FrontServlet extends HttpServlet {

    RequestDispatcher defaultDispatcher;
    ScanResult scanResult = new ScanResult();

    @Override
    public void init() {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
        scanResult = ControllerScanner.scan(getServletContext());
        // debug : lister les mappings trouvés
        for (Map.Entry<String, Method> e : scanResult.urlToMethod.entrySet()) {
            System.out.println("Mapped URL: " + e.getKey() + " -> " + e.getValue().getDeclaringClass().getName() + "#" + e.getValue().getName());
        }

        getServletContext().setAttribute("scanResult", scanResult);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        /**
         * Example: 
         * If URI is /app/folder/file.html 
         * and context path is /app,
         * then path = /folder/file.html
         */
        String path = req.getRequestURI().substring(req.getContextPath().length());
        
        boolean resourceExists = getServletContext().getResource(path) != null;

        if (resourceExists) {
            defaultServe(req, res);
        } 
        // else {
        //     customServe(req, res);
        // }

        // nouveau comportement : si path correspond à un URLMapping, invoquer / afficher info
        ScanResult scanResultContext = (ScanResult) getServletContext().getAttribute("scanResult");
        Method m = scanResultContext.urlToMethod.get(path);
        if (m != null) {
            Class<?> cls = m.getDeclaringClass();
            boolean isController = scanResultContext.controllerClasses.contains(cls);

            try {
                res.setContentType("text/plain;charset=UTF-8");
                try (PrintWriter out = res.getWriter()) {
                    if (!isController) {
                        out.printf("NON ANNOTEE PAR LE ControllerAnnotation : %s%n", cls.getName());
                        return;
                    }
                    // instancier et invoquer la methode (suppose sans args)
                    Object instance = cls.getDeclaredConstructor().newInstance();
                    
                    out.printf("Classe: %s%n", cls.getName());
                    out.printf("Methode: %s%n", m.getName());

                    try {
                        m.setAccessible(true); // permet d'appeler même si private
                        Object result = m.invoke(instance);

                        // afficher le retour si la méthode renvoie quelque chose
                        // if (result != null) {
                        //     out.printf("Résultat: %s%n", result.toString());
                        // } else {
                        //     out.println("Méthode invoquée avec succès (void)");
                        // }

                        if (result instanceof String) {
                            out.printf("Methode string invoquee : %s", (String) result);
                        } 

                        else if (result instanceof view.ModelView) {
                            view.ModelView mv = (view.ModelView) result;
                            String vue = mv.getView();

                            for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                                req.setAttribute(entry.getKey(), entry.getValue());
                            }
                            // rediriger vers la page JSP
                            RequestDispatcher dispatcher = req.getRequestDispatcher("/" + vue);
                            dispatcher.forward(req, res);
                            return;
                        }

                        else {
                            // out.println("Méthode invoquée (retour ignoré car non-String)");
                        }
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                        out.println("Erreur invocation: " + ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                throw new ServletException(ex);
            }
            return;
        }

        // si aucune route connue, comportement personnalisé existant
        customServe(req, res);
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
