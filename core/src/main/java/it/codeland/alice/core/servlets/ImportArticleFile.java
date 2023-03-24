package  it.codeland.alice.core.servlets;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.google.gson.Gson;
import org.apache.commons.io.FilenameUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

@Component(service = {Servlet.class})
@SlingServletPaths(value = {"/bin/servlets/importjobstart2"})
@ServiceDescription("Import CSV file and creates article pages")
public class ImportArticleFile extends SlingAllMethodsServlet {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String CSV_MIME_TYPE = "text/csv";
    private static final String CSV_FILE_EXTENSION = ".csv";

    @Reference
    private JobManager jobManager;
    @Reference
    protected ResourceResolverFactory resolverFactory;

    private class JobData{
        String id;
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response){
        final Map<String, RequestParameter[]> params = request.getRequestParameterMap();
        Iterator<Map.Entry<String, RequestParameter[]>> paramsIterator = params.entrySet().iterator();

        Map.Entry<String, RequestParameter[]> pairs = paramsIterator.next();
        RequestParameter[] parameterArray = pairs.getValue();

        //Uploaded file
        RequestParameter file = parameterArray[0];

        try {
            InputStream stream = file.getInputStream();

            Asset fileInJCR = storeFileInJCR("/content/dam/ucs-exercise-alice/articleFile.csv", FilenameUtils.getBaseName(file.getFileName()).concat(CSV_FILE_EXTENSION), stream);
            if(Objects.nonNull(fileInJCR)){
                doGet(request, response);
            } else {
                logger.error("Asset not created");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Import failed");
            }
        } catch (IOException | LoginException e) {
            try {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Import failed");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            logger.error("Asset not created: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(final SlingHttpServletRequest req,
                         final SlingHttpServletResponse resp) {
        final Map<String, Object> props = new HashMap<String, Object>();
        Job job = jobManager.addJob("alice/job/topic", props);

        // JSONObject jsonObj = new JSONObject();
        JobData jobData = new JobData();
        jobData.id = job.getId();
        String json = new Gson().toJson(jobData);
        resp.setContentType("application/json");
        try {
            resp.getWriter().println(json);
        } catch (IOException e) {
            logger.error("Job NOT created: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private ResourceResolver getUserResourceResolver() throws LoginException {
        Map<String,Object> authenticationInfo = new HashMap<>(2);
        authenticationInfo.put(ResourceResolverFactory.USER, "admin");
        authenticationInfo.put(ResourceResolverFactory.PASSWORD, "admin".toCharArray());
        return resolverFactory.getResourceResolver(authenticationInfo);

    }

    private Asset storeFileInJCR(String destinationPath, String fileName, InputStream jsonStream)
            throws LoginException {
        ResourceResolver resolver = getUserResourceResolver();;
        AssetManager assetManager = resolver.adaptTo(AssetManager.class);
        if (Objects.nonNull(assetManager)) {
            return assetManager.createAsset(
                    destinationPath,
                    jsonStream,
                    CSV_MIME_TYPE,
                    true);
        } else {
            return null;
        }
    }
}
