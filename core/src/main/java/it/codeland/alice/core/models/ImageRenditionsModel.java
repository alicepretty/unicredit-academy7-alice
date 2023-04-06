package it.codeland.alice.core.models;


import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import  it.codeland.alice.core.utils.*;

@Model(adaptables = {SlingHttpServletRequest.class, Resource.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)

public class ImageRenditionsModel {

    @SlingObject
    private ResourceResolver resourceResolver;

    @ScriptVariable
    Page currentPage;

    @SlingObject
    private Resource currentResource;

    @Inject
    @Source("osgi-services")
    protected MimeTypeService mimeTypeService;

    @RequestAttribute
	@Optional
	private String fileReference;

    @RequestAttribute
	@Optional
	private String fileRefMobile;

    @RequestAttribute
	@Optional
	private String desktopRend;

	@RequestAttribute
	@Optional
	private String desktopRendx2;

	@RequestAttribute
	@Optional
	private String mobileRend;

	@RequestAttribute
	@Optional
	private String mobileRendx2;

    String desktopRenditionPath;
    String mobileRenditionPath;
    String desktopRetinaRenditionPath;
    String mobileRetinaRenditionPath;

    protected final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @PostConstruct
    public void init() {

    	if(StringUtils.isNotBlank(fileReference)) {

    		if(StringUtils.isBlank(fileRefMobile)) {
    			fileRefMobile = fileReference;

    			if(StringUtils.isNotBlank(desktopRend)){
    				this.desktopRenditionPath = this.getPrimaryImageRenditions(fileReference, desktopRend);
				}
    			if(StringUtils.isNotBlank(mobileRend)){
    				this.mobileRenditionPath = this.getPrimaryImageRenditions(fileRefMobile, mobileRend);
				}
    			if(StringUtils.isNotBlank(desktopRendx2)){
    				this.desktopRetinaRenditionPath = this.getPrimaryImageRenditions(fileReference, desktopRendx2);
				}
    			if(StringUtils.isNotBlank(mobileRendx2)){
    				this.mobileRetinaRenditionPath = this.getPrimaryImageRenditions(fileRefMobile, mobileRendx2);
				}
    		}

    	}else {
	        if (!ResourceUtil.isSyntheticResource(currentResource)) {
	            String desktopRendition = StringUtils.EMPTY;
	            String mobileRendition = StringUtils.EMPTY;
	            String desktopRenditionR = StringUtils.EMPTY;
	            String mobileRenditionR = StringUtils.EMPTY;
	            ContentPolicyManager policyManager = resourceResolver.adaptTo(ContentPolicyManager.class);
	            try {
	                if (policyManager != null) {
	                    ContentPolicy contentPolicy = policyManager.getPolicy(this.currentResource);
	                    if (contentPolicy != null) {
	                        switch (JcrUtils.getStringProperty(currentResource.adaptTo(Node.class),
	                                "sling:resourceType", "")) {
	                            default:
	                                desktopRendition = contentPolicy.getProperties().get("desktop", StringUtils.EMPTY);
	                                mobileRendition = contentPolicy.getProperties().get("mobile", StringUtils.EMPTY);
	                                desktopRenditionR = contentPolicy.getProperties().get("desktopRetina", StringUtils.EMPTY);
	                                mobileRenditionR = contentPolicy.getProperties().get("mobileRetina", StringUtils.EMPTY);
	                        }
	                    }
	                }
	                String assetUrl =
	                        JcrUtils.getStringProperty(currentResource.adaptTo(Node.class), "fileReference", "");
	                String assetUrlMob =
	                        JcrUtils.getStringProperty(currentResource.adaptTo(Node.class), "fileRefMobile", "");
	                this.desktopRenditionPath = this.getPrimaryImageRenditions(assetUrl, desktopRendition);
	                this.mobileRenditionPath = this.getPrimaryImageRenditions(assetUrlMob, mobileRendition);
	                this.desktopRetinaRenditionPath = this.getPrimaryImageRenditions(assetUrl, desktopRenditionR);
	                this.mobileRetinaRenditionPath = this.getPrimaryImageRenditions(assetUrlMob, mobileRenditionR);
	            } catch (RepositoryException e) {
	                e.printStackTrace();
	            }
	        }
    	}
    }

    public String getPrimaryImageRenditions(String imagePath, String rendition) {

        Resource previewResource = resourceResolver.getResource(imagePath);
        return Rendition.getRenditionByPolicy(previewResource, rendition, mimeTypeService);
    }

    public String getDesktopRenditionPath() {
        return desktopRenditionPath;
    }

    public String getMobileRenditionPath() {
        return mobileRenditionPath;
    }

	public String getDesktopRetinaRenditionPath() {
		return desktopRetinaRenditionPath;
	}

	public String getMobileRetinaRenditionPath() {
		return mobileRetinaRenditionPath;
	}

}
