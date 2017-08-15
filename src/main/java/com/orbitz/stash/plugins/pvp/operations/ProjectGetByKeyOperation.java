package com.orbitz.stash.plugins.pvp.operations;

import java.util.*;
import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.project.ProjectService;
import com.atlassian.bitbucket.util.UncheckedOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Secure operation that retrieves all of the repos 
 */
public class ProjectGetByKeyOperation implements UncheckedOperation<Project> {

    private ProjectService projectService;
    private String key;

    public ProjectGetByKeyOperation (
        ProjectService projectService,
        String key
    ) {
        this.projectService = projectService;
        this.key = key;
    }

    /**
     * Perform the secure operations that retrieves a project. 
     * @return
     */
    public Project perform()
    {
        return this.projectService.getByKey(this.key);
    }
}
