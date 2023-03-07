package it.codeland.alice.core.models;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;


@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class HashTagModel {
    private List<String> items = new ArrayList<>();

    @SlingObject
    private Resource currentResource;

    @Inject
    private String[] tags;

    @PostConstruct
    protected void init() {
        if(tags != null){
            for(String tag : tags){  
                if(tag.contains("/")){
                    items.add(tag.substring(tag.lastIndexOf("/") + 1));
                } 
                else items.add(tag.substring(tag.lastIndexOf(":") + 1));
            }   
        }
    }

    public List<String> getItems() {
        return items;
    }
}
