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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.gradle.api.Project;

import org.web3j.abi.datatypes.Address;

/** web3j extension for plugin configuration. */
public class Web3jExtension {

    /** Extension name used in Gradle build files. */
    public static final String NAME = "web3j";

    private static final String DEFAULT_GENERATED_PACKAGE = "org.web3j.{0}";

    /**
     * Generated package name for web3j contract wrappers. Accepts a {@link java.text.MessageFormat}
     * string with a unique parameter (i.e. {0} ), formatted as the contract name in lower case.
     */
    private String generatedPackageName;

    /** Base directory for generated Java files. */
    private String generatedFilesBaseDir;

    /** Generate smart contract wrappers using native Java types. */
    private Boolean useNativeJavaTypes;

    private Boolean generateBoth;

    /** Excluded contract names from wrapper generation. */
    private List<String> excludedContracts;

    /** Included contract names from wrapper generation. */
    private List<String> includedContracts;

    /** Bit length for network addresses. */
    private int addressBitLength;

    public String getGeneratedPackageName() {
        return generatedPackageName;
    }

    public void setGeneratedPackageName(final String generatedPackageName) {
        Objects.requireNonNull(generatedPackageName);
        this.generatedPackageName = generatedPackageName;
    }

    public String getGeneratedFilesBaseDir() {
        return generatedFilesBaseDir;
    }

    public void setGeneratedFilesBaseDir(final String generatedFilesBaseDir) {
        Objects.requireNonNull(generatedFilesBaseDir);
        this.generatedFilesBaseDir = generatedFilesBaseDir;
    }

    public Boolean getUseNativeJavaTypes() {
        return useNativeJavaTypes;
    }

    public void setUseNativeJavaTypes(final Boolean useNativeJavaTypes) {
        this.useNativeJavaTypes = useNativeJavaTypes;
    }

    public Boolean getGenerateBoth() {
        return generateBoth;
    }

    public void setGenerateBoth(Boolean generateBoth) {
        this.generateBoth = generateBoth;
    }

    public List<String> getExcludedContracts() {
        return excludedContracts;
    }

    public void setExcludedContracts(final List<String> excludedContracts) {
        this.excludedContracts = excludedContracts;
    }

    public List<String> getIncludedContracts() {
        return includedContracts;
    }

    public void setIncludedContracts(final List<String> includedContracts) {
        this.includedContracts = includedContracts;
    }

    public int getAddressBitLength() {
        return addressBitLength;
    }

    public void setAddressBitLength(final int addressBitLength) {
        this.addressBitLength = addressBitLength;
    }

    public Web3jExtension(final Project project) {
        generatedFilesBaseDir =
                project.getLayout().getBuildDirectory().getAsFile().get().getAbsolutePath()
                        + "/generated/sources/"
                        + NAME;

        // Use the project's group name in generated package
        generatedPackageName = getDefaultGeneratedPackageName(project);

        useNativeJavaTypes = true;
        excludedContracts = new ArrayList<>();
        includedContracts = new ArrayList<>();
        addressBitLength = Address.DEFAULT_LENGTH / Byte.SIZE;
        generateBoth = false;
    }

    protected String getDefaultGeneratedPackageName(Project project) {
        String defaultPackageName;
        final String projectGroup = project.getGroup().toString();
        if (!projectGroup.isEmpty()) {
            defaultPackageName = projectGroup + "." + NAME;
        } else {
            defaultPackageName = DEFAULT_GENERATED_PACKAGE;
        }
        return defaultPackageName;
    }
}
