package it.codeland.alice.core.models;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;

import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Model(adaptables = Resource.class)
public class cardContainer {
    private final Logger LOG = LoggerFactory.getLogger(cardContainer.class);

    @SlingObject
    private Resource currentResource;

    @Inject
    private int cardsNumber;

    public int[] getCardsNumber(){
        LOG.debug("VVVVVVVVVVVVVVVVVVV {}", cardsNumber);

        return new int[cardsNumber];

    }

    @PostConstruct
    public void init(){
           LOG.debug("VVVVVVVVVVVVVVVVVVV {}", cardsNumber);
    }
}
