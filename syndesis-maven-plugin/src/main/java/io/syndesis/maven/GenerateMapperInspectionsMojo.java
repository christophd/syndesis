/**
 * Copyright (C) 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.syndesis.maven;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.atlasmap.maven.GenerateInspectionsMojo;
import io.syndesis.dao.init.ModelData;
import io.syndesis.dao.init.ReadApiClientData;
import io.syndesis.model.Kind;
import io.syndesis.model.connection.Action;
import io.syndesis.model.connection.Connector;
import io.syndesis.model.connection.DataShape;
import io.syndesis.model.connection.DataShapeKinds;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

@Mojo(name = "generate-mapper-inspections", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateMapperInspectionsMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/atlasmap")
    private File outputDir;

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject project;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> remoteRepos;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "static/mapper/v1/java-inspections")
    private String resourceDir;

    @Component
    private RepositorySystem system;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {

            final Resource resource = new Resource();
            resource.setDirectory(outputDir.getCanonicalPath());
            project.addResource(resource);

            final Set<File> generated = new HashSet<>();

            final ReadApiClientData reader = new ReadApiClientData();
            final List<ModelData<?>> modelList = reader.readDataFromFile("io/syndesis/dao/deployment.json");
            for (final ModelData<?> model : modelList) {
                if (model.getKind() == Kind.Connector) {
                    final Connector connector = (Connector) model.getData();

                    for (final Action action : connector.getActions()) {

                        process(generated, connector, action, action.getInputDataShape());
                        process(generated, connector, action, action.getOutputDataShape());

                    }

                }
            }
        } catch (final IOException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    private void process(final Set<File> generated, final Connector connector, final Action action,
        final Optional<DataShape> maybeShape) throws MojoFailureException, MojoExecutionException {
        if (!maybeShape.isPresent()) {
            return;
        }
        if (!connector.getId().isPresent()) {
            return;
        }

        if (!DataShapeKinds.JAVA.equals(maybeShape.get().getKind())) {
            return;
        }

        final DataShape shape = maybeShape.get();
        getLog().info("Generating for connector: " + connector.getId().get() + ", and type: " + shape.getType());
        final File outputFile = new File(outputDir,
            resourceDir + "/" + connector.getId().get() + "/" + shape.getType() + ".json");
        if (generated.contains(outputFile)) {
            return;
        }

        if (outputFile.getParentFile().mkdirs()) {
            getLog().debug("Created dir: " + outputFile.getParentFile());
        }

        final GenerateInspectionsMojo generateInspectionsMojo = new GenerateInspectionsMojo();
        generateInspectionsMojo.setLog(getLog());
        generateInspectionsMojo.setPluginContext(getPluginContext());
        generateInspectionsMojo.setSystem(system);
        generateInspectionsMojo.setRemoteRepos(remoteRepos);
        generateInspectionsMojo.setRepoSession(repoSession);
        generateInspectionsMojo.setGav(action.getCamelConnectorGAV());
        generateInspectionsMojo.setClassName(shape.getType());
        generateInspectionsMojo.setOutputFile(outputFile);
        generateInspectionsMojo.execute();
        generated.add(outputFile);
    }

}
