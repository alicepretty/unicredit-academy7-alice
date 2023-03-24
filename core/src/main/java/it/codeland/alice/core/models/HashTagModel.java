package it.codeland.alice.core.models;



import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.codeland.alice.core.bean.ArticleBean;
@Model(adaptables = {Resource.class}, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class HashTagModel {
    private List<String> items = new ArrayList<>();
    private Logger log = LoggerFactory.getLogger(HashTagModel.class);
    @SlingObject    private Resource currentResource;
    @SlingObject    private ResourceResolver resolver;
    @Inject    private String[] tags;
    private List<ArticleBean> articles = new ArrayList<>();
    private String articlesJson = "";
    @PostConstruct    protected void init() throws Exception {
        try {
            if(tags != null){
                for(String tag : tags){
                    items.add(formatTag(tag));
                }   
            }
            Node currentNode = currentResource.adaptTo(Node.class);
            Property references = currentNode.getProperty("link");     
            Value[] values = references.getValues();
            for (int i=0; i<values.length; i++){
                Resource articleResource= resolver.getResource(values[i].getString());
                 Node articleNode = articleResource.adaptTo(Node.class).getNode("jcr:content");
                   String title = articleNode.getProperty("text").getString();
                   String image = articleNode.getProperty("image").getString();
                   Date date = articleNode.getProperty("date").getDate().getTime();
                   Value[] tagsVal = articleNode.getProperty("cq:tags").getValues();
                   List<String> tagsList = new ArrayList<String>();
                   for (int j=0; j<tagsVal.length; j++){
                      tagsList.add(tagsVal[j].getString());
                   }
                   String[] tags = {formatTag(tagsList.get(0))};
                   String url = articleNode.getParent().getPath() + ".html"; 
                   articles.add(new ArticleBean(title,tags,image,title,new SimpleDateFormat("E dd MMM yyyy").format(date),url));
            }
            articlesJson = new ObjectMapper().writeValueAsString(articles);
        } catch (Exception e) {
            // TODO: handle exception            log.error("HASHTAG ERROR: ", e);
        }
    }
    private String formatTag(String tag) {
        if(tag.contains("/")) return tag.substring(tag.lastIndexOf("/") + 1);
        return tag.substring(tag.lastIndexOf(":") + 1);
    }
    public List<String> getItems() {
        return items;
    }
    public List<ArticleBean> getArticles() {
        return articles;
    }
    public String getArticlesJson() {
        return articlesJson;
    }
}

