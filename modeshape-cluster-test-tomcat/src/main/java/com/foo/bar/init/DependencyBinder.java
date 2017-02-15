package com.foo.bar.init;

import javax.inject.Singleton;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;

import com.foo.bar.factory.RepositoryFactory;
import com.foo.bar.factory.SessionFactory;

public class DependencyBinder extends AbstractBinder {

    @Override
    protected void configure() {
        bindFactory(RepositoryFactory.class).to(Repository.class).in(Singleton.class);
        bindFactory(SessionFactory.class).to(Session.class).in(RequestScoped.class);
    }
}
