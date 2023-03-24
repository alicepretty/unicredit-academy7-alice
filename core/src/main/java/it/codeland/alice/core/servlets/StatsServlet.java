package  it.codeland.alice.core.servlets;

import com.day.cq.wcm.api.PageManager;

import com.day.cq.wcm.api.Page;
import javax.jcr.Node;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import it.codeland.alice.core.utils.ResourceResolverUtils;

@Component(service = { Servlet.class })
@SlingServletPaths(value = { "/bin/servlets/stats" })
@ServiceDescription("Import CSV file and creates article pages automatically")
public class StatsServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(StatsServlet.class);

    String pagePath = "/content/ucs-exercise-alice/magazine";

    @Reference
    private JobManager jobManager;

    PageManager pageManager;

    @Reference
    protected ResourceResolverFactory resourceResolverFactory;

    @Override
    protected void doGet(final SlingHttpServletRequest req, final SlingHttpServletResponse resp) {
        try {
            ResourceResolverUtils resolverUtil = new ResourceResolverUtils();
            ResourceResolver resourceResolver = resolverUtil.getUserResourceResolver(resourceResolverFactory);
            pageManager = resourceResolver.adaptTo(PageManager.class);

            Page magazinePage = pageManager.getPage("/content/ucs-exercise-alice/magazine");
            Node magazineNode = magazinePage.adaptTo(Node.class);
            Node magazineContentNode = magazineNode.getNode("jcr:content");

            JSONObject json = new JSONObject();
            json.put("statsSkippedRecords", magazineContentNode.hasProperty("statsSkippedRecords")? magazineContentNode.getProperty("statsSkippedRecords").getLong(): 0);
            json.put("statsProcessedRecords", magazineContentNode.hasProperty("statsProcessedRecords")? magazineContentNode.getProperty("statsProcessedRecords").getLong(): 0);
            json.put("statsSkippedRecords", magazineContentNode.hasProperty("statsSkippedRecords")? magazineContentNode.getProperty("statsSkippedRecords").getLong(): 0);
            json.put("statsErrorRecords", magazineContentNode.hasProperty("statsErrorRecords")? magazineContentNode.getProperty("statsErrorRecords").getLong(): 0);
            json.put("statsPercentage", magazineContentNode.hasProperty("statsPercentage")? magazineContentNode.getProperty("statsPercentage").getLong(): 0);

            resp.setContentType("application/json");
            resp.getWriter().println(json);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("\n \n  > error [ImportJobCheckServlet.doPost] " + ExceptionUtils.getStackTrace(e) + " < \n \n ");
        }
    }

}
