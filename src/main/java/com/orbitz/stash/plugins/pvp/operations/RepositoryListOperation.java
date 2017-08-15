package com.orbitz.stash.plugins.pvp.operations;

import java.util.*;
import com.atlassian.soy.renderer.SoyException;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.project.ProjectType;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.bitbucket.util.PageRequestImpl;
import com.atlassian.bitbucket.util.UncheckedOperation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Secure operation that retrieves all of the repos 
 */
public class RepositoryListOperation implements UncheckedOperation<Set<Repository>> {

    private RepositoryService repositoryService;

    public RepositoryListOperation (
        RepositoryService repositoryService
    ) {
        this.repositoryService = repositoryService;
    }

    /**
     * Perform the secure operations that retrieves all of the repositories 
     * @return
     */
    public Set<Repository> perform()
    {
        return RepositoryListOperation.getAllRepositories(this.repositoryService);
    }

    public static Set<Repository> getAllRepositories(RepositoryService repositoryService)
    {
        Set<Repository> repositories = new HashSet<Repository>();

        PageRequest pageRequest = new PageRequestImpl(0, 1000);
        Page<Repository> page;
        do {
            page = repositoryService.findAll(pageRequest);
            for (Repository repository : page.getValues()) {
                if (repository.getProject().getType() == ProjectType.NORMAL) {
                    repositories.add(repository);
                }
            }
            pageRequest = page.getNextPageRequest();
        } while (!page.getIsLastPage());

        return repositories;
    }
}
