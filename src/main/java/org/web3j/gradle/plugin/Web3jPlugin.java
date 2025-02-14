/*
 * Copyright 2019 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.web3j.gradle.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.plugins.PluginApplicationException;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.Describables;

import org.web3j.solidity.gradle.plugin.SolidityCompile;
import org.web3j.solidity.gradle.plugin.SolidityPlugin;
import org.web3j.solidity.gradle.plugin.SoliditySourceSet;

import static org.codehaus.groovy.runtime.StringGroovyMethods.capitalize;

/** Gradle plugin class for web3j code generation from Solidity contracts. */
public class Web3jPlugin implements Plugin<Project> {

    public void apply(final Project target) {
        target.getPluginManager().apply(JavaPlugin.class);
        target.getPluginManager().apply(SolidityPlugin.class);
        target.getDependencies().add("implementation", "org.web3j:core:" + getProjectVersion());
        registerExtensions(target);

        final SourceSetContainer sourceSets =
                target.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();

        target.afterEvaluate(p -> sourceSets.all(sourceSet -> configure(target, sourceSet)));
    }

    protected void registerExtensions(Project project) {
        project.getExtensions().create(Web3jExtension.NAME, Web3jExtension.class, project);
    }

    protected String getProjectVersion() {
        final URL versionPropsFile = getClass().getClassLoader().getResource("version.properties");

        if (versionPropsFile == null) {
            throw new PluginApplicationException(
                    Describables.of("No version.properties file found in the classpath."), null);
        } else {
            try {
                final Properties versionProps = new Properties();
                try (InputStream inStream = versionPropsFile.openStream()) {
                    versionProps.load(inStream);
                    return versionProps.getProperty("version");
                }
            } catch (IOException e) {
                throw new PluginApplicationException(
                        Describables.of("Could not read version.properties file."), e);
            }
        }
    }

    /**
     * Configures code generation tasks for the Solidity source sets defined in the project (e.g.
     * main, test).
     *
     * <p>The generated task name for the <code>main</code> source set will be <code>
     * generateContractWrappers</code>, and for <code>test</code> <code>generateTestContractWrappers
     * </code>.
     */
    private void configure(final Project project, final SourceSet sourceSet) {

        final Web3jExtension extension =
                (Web3jExtension) InvokerHelper.getProperty(project, Web3jExtension.NAME);

        final File outputDir = buildSourceDir(extension, sourceSet);

        // Add source set to the project Java source sets
        sourceSet.getJava().srcDir(outputDir);

        final String srcSetName =
                sourceSet.getName().equals("main") ? "" : capitalize(sourceSet.getName());

        final String generateTaskName = "generate" + srcSetName + "ContractWrappers";

        final TaskProvider<GenerateContractWrappers> taskProvider =
                project.getTasks()
                        .register(
                                generateTaskName,
                                GenerateContractWrappers.class,
                                task -> {
                                    // Set the sources for the generation task
                                    task.setSource(buildSourceDirectorySet(project, sourceSet));
                                    task.setDescription(
                                            "Generates "
                                                    + sourceSet.getName()
                                                    + " Java contract wrappers from Solidity ABIs.");

                                    // Set the task output directory
                                    task.getOutputs().dir(outputDir);

                                    // Set the task generated package name, classpath and group
                                    task.setGeneratedJavaPackageName(
                                            extension.getGeneratedPackageName());
                                    task.setUseNativeJavaTypes(extension.getUseNativeJavaTypes());
                                    task.setGenerateBoth(extension.getGenerateBoth());
                                    task.setGroup(Web3jExtension.NAME);

                                    // Set task excluded contracts
                                    task.setExcludedContracts(extension.getExcludedContracts());
                                    task.setIncludedContracts(extension.getIncludedContracts());

                                    // Set the contract addresses length (default 160)
                                    task.setAddressLength(extension.getAddressBitLength());

                                    task.dependsOn(
                                            project.getTasks()
                                                    .withType(SolidityCompile.class)
                                                    .named("compile" + srcSetName + "Solidity"));
                                });

        final TaskProvider<Task> compileJava =
                project.getTasks().named("compile" + srcSetName + "Java");

        compileJava.configure(
                task -> {
                    ((SourceTask) task)
                            .source(taskProvider.get().getOutputs().getFiles().getSingleFile());
                    task.dependsOn(taskProvider);
                });
    }

    protected SourceDirectorySet buildSourceDirectorySet(
            Project project, final SourceSet sourceSet) {

        final String displayName = capitalize(sourceSet.getName()) + " Solidity BIN";

        final SourceDirectorySet directorySet =
                project.getObjects().sourceDirectorySet(sourceSet.getName(), displayName);

        directorySet.srcDir(buildOutputDir(sourceSet));
        directorySet.include("**/*.bin");
        return directorySet;
    }

    private File buildSourceDir(final Web3jExtension extension, final SourceSet sourceSet) {

        if (extension.getGeneratedFilesBaseDir().isEmpty()) {
            throw new InvalidUserDataException("Generated web3j package cannot be empty");
        }

        return new File(
                extension.getGeneratedFilesBaseDir()
                        + File.separator
                        + sourceSet.getName()
                        + File.separator
                        + "java");
    }

    protected File buildOutputDir(final SourceSet sourceSet) {
        final Convention convention =
                (Convention) InvokerHelper.getProperty(sourceSet, "convention");

        final SoliditySourceSet soliditySourceSet =
                (SoliditySourceSet) convention.getPlugins().get(SoliditySourceSet.NAME);

        return soliditySourceSet.getSolidity().getDestinationDirectory().getAsFile().get();
    }
}
