package com.orbitz.stash.plugins.pvp.servlet;

import com.orbitz.stash.plugins.pvp.operations.*;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.project.ProjectService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermissionService;
import com.atlassian.bitbucket.permission.PermissionAdminService;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.user.UserService;
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

import javax.inject.Inject;

@Scanned
public class RepositoryPermissionsServlet extends HttpServlet{
    private static final Logger log = LoggerFactory.getLogger(RepositoryPermissionsServlet.class);

    @ComponentImport
    private final RepositoryService repositoryService;
    @ComponentImport
    private final SecurityService securityService;
    @ComponentImport
    private final PermissionAdminService permissionAdminService;
    @ComponentImport
    private final PermissionService permissionService;
    @ComponentImport
    private final UserService userService;
    @ComponentImport
    private final SoyTemplateRenderer soyTemplateRenderer;

    @Inject
    public RepositoryPermissionsServlet(
        RepositoryService repositoryService,
        SecurityService securityService,
        PermissionAdminService permissionAdminService,
        UserService userService,
        PermissionService permissionService,
        SoyTemplateRenderer soyTemplateRenderer
    ) {
        this.repositoryService = repositoryService;
        this.securityService = securityService;
        this.permissionAdminService = permissionAdminService;
        this.permissionService = permissionService;
        this.userService = userService;
        this.soyTemplateRenderer = soyTemplateRenderer;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // Get userSlug from path
        String pathInfo = req.getPathInfo();

        String repositoryId = pathInfo.substring(1); // Strip leading slash
        RepositoryGetByIdOperation repositoryGetByIdOperation = new RepositoryGetByIdOperation(repositoryService, Integer.valueOf(repositoryId));
        Repository repository = securityService
            .withPermission(Permission.PROJECT_ADMIN, "Get repository info")
            .call(repositoryGetByIdOperation);

        //
        // Need to wrap all of the permission access in an operation called by the security service
        //
        PermissionAdminOperation permissionAdminOperation = new PermissionAdminOperation(permissionAdminService, userService, repository.getProject(), repository);

        //
        // Build a map using Permission as the key that's value is a list of all the groups and users
        //
        Map<Permission, List<String>> identityMap = securityService
            .withPermission(Permission.PROJECT_ADMIN, "Get groups and users to display")
            .call(permissionAdminOperation);


        // Create the view model for Soy
        ImmutableMap.Builder<String, Object> immutableMapBuilder =  new ImmutableMap.Builder<String, Object>();

        immutableMapBuilder.
                put("repository", repository).
                put("authenticated", this.permissionService.isRepositoryAccessible(repository)).
                put("repositoryAdmin", ImmutableList.copyOf(identityMap.get(Permission.REPO_ADMIN))).
                put("repositoryWrite", ImmutableList.copyOf(identityMap.get(Permission.REPO_WRITE))).
                put("repositoryRead", ImmutableList.copyOf(identityMap.get(Permission.REPO_READ))).
                put("projectAdmin", ImmutableList.copyOf(identityMap.get(Permission.PROJECT_ADMIN))).
                put("projectWrite", ImmutableList.copyOf(identityMap.get(Permission.PROJECT_WRITE))).
                put("projectRead", ImmutableList.copyOf(identityMap.get(Permission.PROJECT_READ)));

        // Now render the tab
        ProjectPermissionsServlet.render(
            this.soyTemplateRenderer, resp, "plugin.permissionviewer.repositoryPermissionsTab", immutableMapBuilder.build()
        );
    }
}
