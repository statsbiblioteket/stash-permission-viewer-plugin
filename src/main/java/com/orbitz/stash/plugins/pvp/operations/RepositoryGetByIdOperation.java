package com.orbitz.stash.plugins.pvp.operations;

import java.util.*;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.util.UncheckedOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Secure operation that retrieves all of the repos 
 */
public class RepositoryGetByIdOperation implements UncheckedOperation<Repository> {

    private RepositoryService repositoryService;
    private int id;

    public RepositoryGetByIdOperation (
        RepositoryService repositoryService,
        int id
    ) {
        this.repositoryService = repositoryService;
        this.id = id;
    }

    /**
     * Perform the secure operations that retrieves a repository. 
     * @return
     */
    public Repository perform()
    {
        return this.repositoryService.getById(this.id);
    }
}
