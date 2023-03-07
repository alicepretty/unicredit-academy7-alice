package it.codeland.alice.core.models;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

@Model(adaptables = Resource.class)
public class HeaderNav {

        @SlingObject
    Resource componentResource;

    public Page getFirstLevelItems() {
        PageManager pageManager = componentResource.getResourceResolver().adaptTo(PageManager.class);
        Page currentPage = pageManager.getContainingPage(componentResource);
        return currentPage.getAbsoluteParent(2);
    }

  
}

    

