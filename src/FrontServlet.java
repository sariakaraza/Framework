
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public class FrontServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String url = req.getRequestURI();
        resp.setContentType("text/plain");
        resp.getWriter().println("URL demandée ooooo : " + url);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }
}