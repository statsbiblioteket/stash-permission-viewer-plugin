package com.orbitz.stash.plugins.pvp.servlet;

import com.orbitz.stash.plugins.pvp.operations.*;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.user.SecurityService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

@Scanned
public class WithoutPermissionServlet extends HttpServlet{
    private static final Logger log = LoggerFactory.getLogger(WithoutPermissionServlet.class);

    @ComponentImport
    private final RepositoryService repositoryService;
    @ComponentImport
    private final SecurityService securityService;
    @ComponentImport
    private final SoyTemplateRenderer soyTemplateRenderer;

    @Inject
    public WithoutPermissionServlet(
        RepositoryService repositoryService,
        SecurityService securityService,
        SoyTemplateRenderer soyTemplateRenderer
    ) {
        this.repositoryService = repositoryService;
        this.securityService = securityService;
        this.soyTemplateRenderer = soyTemplateRenderer;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        //
        // Need to wrap all of the permission access in an operation called by the security service
        //
        RepositoryListOperation repositoryListOperation = new RepositoryListOperation(this.repositoryService);

        //
        // Retrieve the list of all repos for calculating the diff between
        // those the user has access to
        //
        Set<Repository> repositories = securityService
            .withPermission(Permission.REPO_READ, "Get all repos")
            .call(repositoryListOperation);

        //
        // Now retrieve all repos without the elevated security context
        //
        Set<Repository> repositoriesWithPermission = RepositoryListOperation.getAllRepositories(this.repositoryService);

        // And calculate the diff
        Set<Repository> repositoriesWithoutPermission = new HashSet<Repository>(repositories);
        repositoriesWithoutPermission.removeAll(repositoriesWithPermission);

        // Create the view model for Soy
        ImmutableMap.Builder<String, Object> immutableMapBuilder =  new ImmutableMap.Builder<String, Object>();

        immutableMapBuilder.
                put("repositoriesWithoutPermission", ImmutableList.copyOf(repositoriesWithoutPermission));

        // Now render the tab
        ProjectPermissionsServlet.render(
            this.soyTemplateRenderer, resp, "plugin.permissionviewer.repositoriesWithoutPermissionPage", immutableMapBuilder.build()
        );
    }
}
