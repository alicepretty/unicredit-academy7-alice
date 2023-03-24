package it.codeland.alice.core.schedulers;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.text.csv.Csv;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.joda.time.DateTime;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Designate(ocd=ImportScheduler.Config.class)
@Component(service=Runnable.class)
public class ImportScheduler implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final int FIELDS_MAX = 5;
    private final int FIELD_INDEX_PAGE_TITLE = 0;
    private final int FIELD_INDEX_PAGE_NAME = 1;
    private final int FIELD_INDEX_IMAGE = 2;
    private final int FIELD_INDEX_TAGS = 3;
    private final int FIELD_INDEX_ABSTRACT = 4;

    @ObjectClassDefinition(name="UCS Import Job",
            description = "Import CSV containing articles data")
    public static @interface Config {
        String SCHEDULERNAME = "ImportJob";

        @AttributeDefinition(name = "Cron-job expression",
            description="Cron-job expression",
            type=AttributeType.STRING)
        String scheduler_expression() default "0/10 * * ? * *"; 

        @AttributeDefinition(name = "Concurrent task",
            description = "Whether or not to schedule this task concurrently",
            type = AttributeType.STRING)
        boolean scheduler_concurrent() default false;

        @AttributeDefinition(name = "CSV file path",
            description = "CSV file path in DAM",
            type = AttributeType.STRING)
        String CSVFilePath() default "/content/dam/ucs-exercise-alice/articleFile.csv";

        @AttributeDefinition(name = "User",
            description = "User to access DAM",
            type = AttributeType.STRING)
        String User() default "admin";

        @AttributeDefinition(name = "Password",
            description = "Password for the given user",
            type = AttributeType.PASSWORD)
        String Password() default "admin";
    }

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resolverFactory;

    private int schedulerId;
    private String CSVFilePath;
    private String user;
    private String password;

    @Activate
    protected void activate(Config config){
        logger.debug("Starting job");
        schedulerId = config.SCHEDULERNAME.hashCode();
        CSVFilePath = config.CSVFilePath();
        user = config.User();
        password = config.Password();
        addScheduler(config);
    }

    private void addScheduler(Config config) {
        logger.debug("Adding scheduler");
        ScheduleOptions options = scheduler.EXPR(config.scheduler_expression());
        options.name(String.valueOf(schedulerId));
        options.canRunConcurrently(false);
        scheduler.schedule(this, options);
    }

    @Deactivate
    protected void deactivate(Config config){
        logger.debug("Stopping job");
        removeScheduler();
    }

    private void removeScheduler() {
        logger.debug("Removing scheduler");
        scheduler.unschedule(String.valueOf(schedulerId));
    }

    // Gets resource resolver with user privileges
    public ResourceResolver getUserResourceResolver() throws LoginException {
        Map<String,Object> authenticationInfo = new HashMap<>(2);
        authenticationInfo.put(ResourceResolverFactory.USER, user);
        authenticationInfo.put(ResourceResolverFactory.PASSWORD, password.toCharArray());
        return resolverFactory.getResourceResolver(authenticationInfo);
    }

    private class ArticleData{
        String pageTitle = "";
        String pageName = "";
        String image = "";
        String[] tags;
        String abstractText = "";
        String date = "";
    }

    private class Statistics{
        Integer processedRecords = 0;
        Integer skippedRecords = 0;
        Integer errorRecords = 0;
    }

    @Override
    public void run() {
        String pagePath = "/content/ucs-exercise-alice/magazine";
        String templatePath = "/apps/ucs-exercise-alice/templates/articleTemplate";
        Page articlePage;
        PageManager pageManager;
        ResourceResolver resolver = null;
        Session session = null;
        ArticleData articleData = new ArticleData();
        Statistics statistics = new Statistics();

        try {
            resolver = getUserResourceResolver();
            Resource resource = resolver.getResource(CSVFilePath);
            if(resource == null){
                logger.info("CSV File does not exists!");
                return;
            }
            pageManager = resolver.adaptTo(PageManager.class);
            Page articles = pageManager.getPage("/content/ucs-exercise-alice/magazine/");
            if(articles == null){
                logger.error("Articles page does not exists!");
                return;
            }
            Resource articlesContent = articles.getContentResource();
            Node articlesContentNode = articlesContent.adaptTo(Node.class);
            DateTime savedDateTime = null;
            try {
                savedDateTime = new DateTime(articlesContentNode.getProperty("lastModifiedImport").getValue().toString());
            } catch (Exception e) {
                logger.info("First iteration");
            }
            Asset asset = resource.adaptTo(Asset.class);
            DateTime fileDateTime = new DateTime(asset.getLastModified());
             logger.info("CSV file start");
            session = resolver.adaptTo(Session.class);
            articlesContentNode.setProperty("lastModifiedImport", fileDateTime.toString());
            Rendition rend = asset.getOriginal();
            InputStream inputStream = rend.getStream();

            final Iterator<String[]> rows = this.getRowsFromCsv(inputStream);
            while (rows.hasNext()) {
                final String[] row = rows.next();
                // Every row MUST have FIELDS_MAX fields
                if(row.length == FIELDS_MAX){
                    // Extract data from CSV
                    articleData.pageName = row[FIELD_INDEX_PAGE_NAME];
                    // Check if page is already existing
                    if(pageManager.getPage("/content/ucs-exercise-alice/magazine/" + articleData.pageName + "/") != null){
                        statistics.skippedRecords++;
                        logger.info("Page \"{}\" already exists", articleData.pageName);
                        continue;
                    }
                    articleData.pageTitle = row[FIELD_INDEX_PAGE_TITLE];
                    articleData.image = row[FIELD_INDEX_IMAGE];
                    articleData.tags = Arrays.stream(row[FIELD_INDEX_TAGS].split(",")).map(String::trim).
                            collect(Collectors.toList()).toArray(new String[0]);
                    articleData.abstractText = row[FIELD_INDEX_ABSTRACT];
                    LocalDate dt = java.time.LocalDate.now();
                    articleData.date = dt.toString();
                    // Create page and set properties in jcr:content node
                    articlePage = pageManager.create(pagePath, articleData.pageName, templatePath, articleData.pageTitle);
                    if (articlePage != null) {
                        Node articleNode = articlePage.adaptTo(Node.class);
                        Node articleContentNode = articleNode.getNode("jcr:content");
                        if (articleContentNode != null) {
                            articleContentNode.setProperty("image", articleData.image);
                            articleContentNode.setProperty("text", articleData.abstractText);
                            articleContentNode.setProperty("cq:tags", articleData.tags);
                            articleContentNode.setProperty("date", articleData.date);
                            statistics.processedRecords++;
                        } else {
                            logger.error("There is no \"{}\" content!", articleData.pageName);
                        }
                     } else {
                        logger.error("Cannot create {} page!", articleData.pageName);
                    }
                } else {
                    statistics.errorRecords++;
                    logger.error("Processing failed on row: {}", Arrays.asList(row));
                }
            }
            session.save();
            resolver.close();
              logger.info("CSV file stopped");
            logger.debug("\n");
            logger.debug("New Articles : {}", statistics.processedRecords);
            logger.debug("Articles skipped: {}", statistics.skippedRecords);
            logger.debug("Articles with with errors: {}", statistics.errorRecords);

            logger.debug("\n");
        } catch (Exception e) {
            logger.error("Error occurs: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private Iterator<String[]> getRowsFromCsv(InputStream is) throws IOException {
        final Csv csv = new Csv();
        csv.setFieldDelimiter('\"');
        csv.setFieldSeparatorRead(',');
        return csv.read(is, null);
    }
};
