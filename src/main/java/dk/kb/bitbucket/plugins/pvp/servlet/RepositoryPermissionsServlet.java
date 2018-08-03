package dk.kb.bitbucket.plugins.pvp.servlet;

import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermissionAdminService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.repository.ref.restriction.RefRestrictionService;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.EscalatedSecurityContext;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.user.UserService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dk.kb.bitbucket.plugins.pvp.operations.BranchRow;
import dk.kb.bitbucket.plugins.pvp.operations.PermissionAdminOperation;
import dk.kb.bitbucket.plugins.pvp.operations.RestrictionAdminOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RepositoryPermissionsServlet extends HttpServlet{
    private static final Logger log = LoggerFactory.getLogger(RepositoryPermissionsServlet.class);

    private final RepositoryService repositoryService;
    private final SecurityService securityService;
    private final PermissionAdminService permissionAdminService;
    private final UserService userService;
    private final RefRestrictionService refRestrictionService;
    private final SoyTemplateRenderer soyTemplateRenderer;
    
    
    
    
    @Autowired
    public RepositoryPermissionsServlet(
            @ComponentImport RepositoryService repositoryService,
            @ComponentImport SecurityService securityService,
            @ComponentImport PermissionAdminService permissionAdminService,
            @ComponentImport UserService userService,
            @ComponentImport RefRestrictionService refRestrictionService,
            @ComponentImport SoyTemplateRenderer soyTemplateRenderer) {
        this.repositoryService = repositoryService;
        this.securityService = securityService;
        this.permissionAdminService = permissionAdminService;
        this.userService = userService;
        this.refRestrictionService = refRestrictionService;
        this.soyTemplateRenderer = soyTemplateRenderer;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // Get userSlug from path
        String pathInfo = req.getPathInfo();

        String repositoryId = pathInfo.substring(1); // Strip leading slash
        Repository repository = repositoryService.getById(Integer.valueOf(repositoryId));

        //
        // Need to wrap all of the permission access in an operation called by the security service
        //
        PermissionAdminOperation permissionAdminOperation = new PermissionAdminOperation(permissionAdminService, userService, repository.getProject(), repository);

        //
        // Build a map using Permission as the key that's value is a list of all the groups and users
        //
        EscalatedSecurityContext esc = securityService.withPermission(Permission.PROJECT_ADMIN, "Get groups and users to display");
        Map<Permission, List<String>> identityMap = esc.call(permissionAdminOperation);

        //
        // Build a map of branch restrictions
        //
        RestrictionAdminOperation restrictionAdminOperation = new RestrictionAdminOperation(refRestrictionService, userService, repository);
        Map<String, BranchRow> branchRowMap = esc.call(restrictionAdminOperation);

        // Create the view model for Soy
        ImmutableMap.Builder<String, Object> immutableMapBuilder =  new ImmutableMap.Builder<String, Object>();

        immutableMapBuilder.
                put("repository", repository).
                put("repositoryAdmin", ImmutableList.copyOf(identityMap.get(Permission.REPO_ADMIN))).
                put("repositoryWrite", ImmutableList.copyOf(identityMap.get(Permission.REPO_WRITE))).
                put("repositoryRead", ImmutableList.copyOf(identityMap.get(Permission.REPO_READ))).
                put("projectAdmin", ImmutableList.copyOf(identityMap.get(Permission.PROJECT_ADMIN))).
                put("projectWrite", ImmutableList.copyOf(identityMap.get(Permission.PROJECT_WRITE))).
                put("projectRead", ImmutableList.copyOf(identityMap.get(Permission.PROJECT_READ))).
                put("branchRowList", ImmutableList.copyOf(branchRowMap.values()));


        // Now render the tab
        render(resp, "plugin.permissionviewer.repositoryPermissionsTab", immutableMapBuilder.build());
    }

    // Generic soy render method
    private void render(HttpServletResponse resp, String templateName, Map<String, Object> data) throws IOException, ServletException {
        resp.setContentType("text/html;charset=UTF-8");
        try {
            soyTemplateRenderer.render(resp.getWriter(),
                    "dk.kb.bitbucket.plugins.permission-viewer-plugin:soy-templates",
                    templateName,
                    data);
        } catch (SoyException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new ServletException(e);
        }
    }
}