package com.foo.bar;

import java.util.Iterator;
import java.util.List;

import javax.jcr.Repository;

import com.google.common.collect.Iterators;

/**
 * The circular repository iterator.
 * 
 * @author Illia Khokholkov
 *
 */
public class CircularRepositoryIterator {
    
    private final Iterator<Repository> repositoryIterator;
    
    public CircularRepositoryIterator(List<Repository> repositoryList) {
        this.repositoryIterator = Iterators.cycle(repositoryList);
    }
    
    public Repository next() {
        synchronized (repositoryIterator) {
            return repositoryIterator.next();
        }
    }
}
