package it.codeland.alice.core.jobs;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.text.csv.Csv;
import com.google.common.collect.Lists;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.joda.time.DateTime;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;


@Component( service= JobExecutor.class,
        immediate = true,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Demo to listen on changes in the resource tree",
                JobExecutor.PROPERTY_TOPICS + "=alice/job/topic"
        })
public class JobExecute implements JobExecutor{
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final int FIELD_INDEX_PAGE_TITLE = 0;
    private final int FIELD_INDEX_PAGE_NAME = 1;
    private final int FIELD_INDEX_IMAGE = 2;
    private final int FIELD_INDEX_TAGS = 3;
    private final int FIELD_INDEX_ABSTRACT = 4;

    private class ArticleData{
        String pageTitle = "";
        String pageName = "";
        String image = "";
        String[] tags;
        String abstractText = "";
        String date = "";
    }

    private class Statistics {
        int totalArticle = 0;
        int processedRecords = 0;
        int skippedRecords = 0;
        int errorRecords = 0;
        int percentages = 0;
    }


    @Reference
    private ResourceResolverFactory resolverFactory;

    public JobExecute(){
    }

    @org.osgi.service.component.annotations.Activate
    protected void activate(final ComponentContext componentContext) {
    }

    public ResourceResolver getUserResourceResolver() throws LoginException {
        Map<String,Object> authenticationInfo = new HashMap<>(2);
        authenticationInfo.put(ResourceResolverFactory.USER, "admin");
        authenticationInfo.put(ResourceResolverFactory.PASSWORD, "admin".toCharArray());
        return resolverFactory.getResourceResolver(authenticationInfo);
    }

    @Override
    public JobExecutionResult process(final Job job, JobExecutionContext context){

        String pagePath = "/content/ucs-exercise-alice/magazine";
        String templatePath = "/apps/ucs-exercise-alice/templates/articleTemplate";
        Page articlePage;
        PageManager pageManager;
        ResourceResolver resolver = null;
        Session session = null;
        ArticleData articleData = new ArticleData();
        Statistics statistics = new Statistics();
        JobExecutionResult test =null;

 
        try {
            resolver = getUserResourceResolver();
            Resource resource = resolver.getResource("/content/dam/ucs-exercise-alice/articleFile.csv");
            if(resource == null){
                logger.info("CSV File does not exists!");
                context.log("CSV File does not exists!");
                return context.result().message("Proccessing aborted: CSV file does not exists").cancelled();
            }
            pageManager = resolver.adaptTo(PageManager.class);
            Page articles = pageManager.getPage("/content/ucs-exercise-alice/magazine/");
            if(articles == null){
                logger.error("Articles page does not exists!");
                context.log("Articles page does not exists!");
                return context.result().message("Proccessing aborted: Articles page does not exists!").cancelled();
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
            if((savedDateTime != null) && savedDateTime.equals(fileDateTime)){
                logger.info("CSV file has not been updated");
                context.log("CSV file has not been updated");
                return context.result().message("CSV file has not been updated").succeeded();
            } else {
                logger.info("CSV file has been modified");
                  logger.info("CSV file has been modified");
            }
            session = resolver.adaptTo(Session.class);
            articlesContentNode.setProperty("lastModifiedImport", fileDateTime.toString());
            Rendition rend = asset.getOriginal();
            InputStream inputStream = rend.getStream();

            final Iterator<String[]> tmp = this.getRowsFromCsv(inputStream);
            List list = Lists.newArrayList(tmp);
            context.initProgress(list.size(), -1);
            Iterator<String[]> rows = list.iterator();

           statistics.totalArticle= list.size();
           int i =0;

            Page magazinePage = pageManager.getPage("/content/ucs-exercise-alice/magazine/");
            Node magazineNode = magazinePage.adaptTo(Node.class);
            Node magazineContentNode = magazineNode.getNode("jcr:content");

            while (rows.hasNext()) {
                final String[] row = rows.next();
                if(row.length == 5){
                    articleData.pageName = row[FIELD_INDEX_PAGE_NAME];
                    if(pageManager.getPage("/content/ucs-exercise-alice/magazine/" + articleData.pageName + "/") == null) {
                        articleData.pageTitle = row[FIELD_INDEX_PAGE_TITLE];
                        articleData.image = row[FIELD_INDEX_IMAGE];
                        articleData.tags = Arrays.stream(row[FIELD_INDEX_TAGS].split(",")).map(String::trim).
                                collect(Collectors.toList()).toArray(new String[0]);
                        articleData.abstractText = row[FIELD_INDEX_ABSTRACT];
                         DateTime dt = new DateTime();
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
                        statistics.skippedRecords++;
                        logger.info("Page \"{}\" already exists", articleData.pageName);
                    }
                } else{
                    statistics.errorRecords++;
                    logger.error("Processing failed on row: {}", Arrays.asList(row));
                }
                //stop processing if job was cancelled

                if(context.isStopped()) {
                    logger.info("Job Stopped.");
                    context.log("Job Stopped.");
                    return context.result().message("Job CANCELLED").cancelled();
                }
                context.incrementProgressCount(1);

               statistics.percentages = (int)((i+1) *100.0 / statistics.totalArticle);
               magazineContentNode.setProperty("statsTotal", statistics.totalArticle);
               magazineContentNode.setProperty("statsProcessedRecords", statistics.processedRecords);
               magazineContentNode.setProperty("statsSkippedRecords", statistics.skippedRecords);
               magazineContentNode.setProperty("statsErrorRecords", statistics.errorRecords);
               magazineContentNode.setProperty("statsPercentage", statistics.percentages);
               session.save();
                i++;
            }

            resolver.close();
            logger.info("Records processed: {}", statistics.processedRecords);
            logger.info("Records skipped: {}", statistics.skippedRecords);
            logger.info("Records with errors: {}", statistics.errorRecords);
            logger.info("Total articles: {}", statistics.totalArticle);
            logger.info("Percentage: {}", statistics.percentages);

            context.log("Records ok: {0, number, integer}", statistics.processedRecords);
            context.log("Records skipped: {0, number, integer}", statistics.skippedRecords);
            context.log("Records with errors: {0, number, integer}", statistics.errorRecords);
            test = context.result().message("Job FINISHED").succeeded();

        } catch (Exception e) {
            logger.info("Error occurs : {}", e.getMessage());
            e.printStackTrace();
        }

        logger.info("TEst error {} " ,test);
        return test;
    }

    private Iterator<String[]> getRowsFromCsv(InputStream inputStram) throws IOException {
        final Csv csv = new Csv();
        csv.setFieldDelimiter('\"');
        csv.setFieldSeparatorRead(',');
        return csv.read(inputStram, null);
    }
}
