package it.codeland.alice.core.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;


public class ResourceResolverUtils {
  private String user = "admin";
  private String password = "admin";

  public ResourceResolver getUserResourceResolver(ResourceResolverFactory resolverFactory) throws LoginException {
    Map<String,Object> authenticationInfo = new HashMap<>(2);
    authenticationInfo.put(ResourceResolverFactory.USER, user);
    authenticationInfo.put(ResourceResolverFactory.PASSWORD, password.toCharArray());
    return resolverFactory.getResourceResolver(authenticationInfo);
  }

}