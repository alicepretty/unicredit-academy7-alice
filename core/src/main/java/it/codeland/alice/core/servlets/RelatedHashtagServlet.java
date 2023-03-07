package  it.codeland.alice.core.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.codeland.alice.core.bean.ArticleBean;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 
@Component(service = { Servlet.class }) 
@SlingServletPaths(
            value={"/bin/relatedtags"})
@ServiceDescription("Related hashtag for articles Servlet")
public class RelatedHashtagServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(RelatedHashtagServlet.class);
   
    @Override
    protected void doGet(final SlingHttpServletRequest req, final SlingHttpServletResponse resp) throws ServletException, IOException {
                Session session = req.getResourceResolver().adaptTo(Session.class);
                try {      
                        String tag = req.getParameter("tag"); 
                         String length = req.getParameter("len");
                        String order = req.getParameter("order");
                         String sort = req.getParameter("sort");
                    List<Node> nodes = new ArrayList<>();
                    int maxLength = (length.length() > 0) ? Integer.parseInt(length) : 20;
                    List<ArticleBean> articles = new ArrayList<>();
                    QueryManager queryMgr = session.getWorkspace().getQueryManager();

                    String queryText = "SELECT * FROM [cq:PageContent] AS nodes WHERE ISDESCENDANTNODE ([/content/ucs-exercise-alice/magazine]) AND nodes.[cq:tags] LIKE '%"+tag+"' ORDER BY ["+order+"] "+sort+"";
                    Query query = queryMgr.createQuery(queryText, "JCR-SQL2");
                    query.setLimit(maxLength);
                    QueryResult result = query.execute();
                    
                    Iterator<Node> resultNodes = result.getNodes();
                    while (resultNodes.hasNext()) nodes.add(resultNodes.next());
                    
                    for (Node item : nodes) {
                        String title = item.getProperty("jcr:title").getString();
                        Date date = item.getProperty("date").getDate().getTime();
                        String image = item.getProperty("image").getString();
                        String text = item.getProperty("text").getString();
                        String url = item.getParent().getPath()+".html";

                        String[] tags = { tag };
                        ArticleBean article = new ArticleBean(title, tags, image, text, new SimpleDateFormat("E dd MMM yyyy").format(date), url);
                        articles.add(article);
                    }
        
                    resp.setCharacterEncoding("UTF-8");
                    resp.setContentType("application/json");
                    resp.getWriter().write(new ObjectMapper().writeValueAsString(articles));
                } catch (Exception e) {
                    log.error("\n ERROR while getting link Details {} ",e.getMessage());
                    log.error(e.getMessage(), e);
                } finally {
                    session.logout();
                }
    }
}
