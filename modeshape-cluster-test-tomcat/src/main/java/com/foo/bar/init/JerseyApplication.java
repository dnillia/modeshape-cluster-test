package com.foo.bar.init;

import java.util.TimeZone;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;

import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/")
public class JerseyApplication extends ResourceConfig implements PreDestroy {

    private final ServiceLocator locator;

    @Inject
    public JerseyApplication(ServiceLocator locator) {
        ServiceLocatorUtilities.enableLookupExceptions(locator);
        
        packages(true, "com.foo.bar");
        register(new DependencyBinder());
        
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        this.locator = locator;
    }

    @Override
    public void preDestroy() {
        locator.shutdown();
    }
}
