package edu.hm.ccwi.matilda.dataextractor.service.reader;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.*;
import org.apache.maven.model.composition.DefaultDependencyManagementImporter;
import org.apache.maven.model.management.DefaultDependencyManagementInjector;
import org.apache.maven.model.management.DefaultPluginManagementInjector;
import org.apache.maven.model.plugin.DefaultPluginConfigurationExpander;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolver to find the repository a given Maven artifact should be fetched from
 */
public class DefaultPomModelResolver implements ModelResolver {

    private final Set<Repository> repositories;
    private final Map<String, ModelSource> ruleNameToModelSource;
    private final DefaultModelBuilder modelBuilder;

    public  DefaultPomModelResolver() {
        repositories = Sets.newHashSet();
        ruleNameToModelSource = Maps.newHashMap();
        modelBuilder = new DefaultModelBuilderFactory().newInstance()
                .setProfileSelector(new DefaultProfileSelector())
                .setPluginConfigurationExpander(new DefaultPluginConfigurationExpander())
                .setPluginManagementInjector(new DefaultPluginManagementInjector())
                .setDependencyManagementImporter(new DefaultDependencyManagementImporter())
                .setDependencyManagementInjector(new DefaultDependencyManagementInjector());
    }

    private  DefaultPomModelResolver(Set<Repository> repositories, Map<String, ModelSource> ruleNameToModelSource,
            DefaultModelBuilder modelBuilder) {
        this.repositories = repositories;
        this.ruleNameToModelSource = ruleNameToModelSource;
        this.modelBuilder = modelBuilder;
    }

    public  ModelSource resolveModel(Artifact artifact) throws UnresolvableModelException {
        return resolveModel(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
    }

    @Override
    public  ModelSource resolveModel(String groupId, String artifactId, String version) throws UnresolvableModelException {
        String ruleName = Rule.name(groupId, artifactId);
        if (ruleNameToModelSource.containsKey(ruleName)) {
            return ruleNameToModelSource.get(ruleName);
        }
        for (Repository repository : repositories) {
            UrlModelSource modelSource = getModelSource(
                    repository.getUrl(), groupId, artifactId, version);
            if (modelSource != null) {
                return modelSource;
            }
        }

        // use Java 8 features to make this a one-liner.
        List<String> attemptedUrls = Lists.newArrayList();
        for (Repository repository : repositories) {
            attemptedUrls.add(repository.getUrl());
        }
        throw new UnresolvableModelException("Could not find any repositories that knew how to "
                + "resolve " + groupId + ":" + artifactId + ":" + version + " (checked "
                + Joiner.on(", ").join(attemptedUrls) + ")", groupId, artifactId, version);
    }

    // make this work with local repositories.
    private  UrlModelSource getModelSource(
            String url, String groupId, String artifactId, String version)
            throws UnresolvableModelException {
        try {
            if (!url.endsWith("/")) {
                url += "/";
            }
            URL urlUrl = new URL(url
                    + groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + version + "/" + artifactId
                    + "-" + version + ".pom");
            if (pomFileExists(urlUrl)) {
                UrlModelSource urlModelSource = new UrlModelSource(urlUrl);
                ruleNameToModelSource.put(Rule.name(groupId, artifactId), urlModelSource);
                return urlModelSource;
            }
        } catch (MalformedURLException e) {
            throw new UnresolvableModelException("Bad URL " + url + ": " + e.getMessage(), groupId,
                    artifactId, version, e);
        }
        return null;
    }

    private  boolean pomFileExists(URL url) {
        try {
            URLConnection urlConnection = url.openConnection();
            if (!(urlConnection instanceof HttpURLConnection)) {
                return false;
            }

            HttpURLConnection connection = (HttpURLConnection) urlConnection;
            connection.setRequestMethod("HEAD");
            connection.setInstanceFollowRedirects(true);
            connection.connect();

            int code = connection.getResponseCode();
            if (code == 200) {
                return true;
            }
        } catch (IOException e) {
            // Something went wrong, fall through.
        }
        return false;
    }

    // For compatibility with older versions of ModelResolver which don't have this method, don't add @Override.
    public  ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    @Override
    public ModelSource resolveModel(Dependency dependency) throws UnresolvableModelException {
        return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    }

    // For compatibility with older versions of ModelResolver which don't have this method, don't add @Override.
    public  void addRepository(Repository repository) {
        repositories.add(repository);
    }

    @Override
    public  void addRepository(Repository repository, boolean replace) {
        addRepository(repository);
    }

    @Override
    public  ModelResolver newCopy() {
        return new DefaultPomModelResolver(repositories, ruleNameToModelSource, modelBuilder);
    }

    public  boolean putModelSource(String groupId, String artifactId, ModelSource modelSource) {
        String key = Rule.name(groupId, artifactId);
        if (!ruleNameToModelSource.containsKey(key)) {
            ruleNameToModelSource.put(key, modelSource);
            return true;
        }
        return false;
    }

    public  Model getEffectiveModel(ModelSource modelSource) {
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelResolver(this);
        request.setModelSource(modelSource);
        try {
            return modelBuilder.build(request).getEffectiveModel();
        } catch (ModelBuildingException | IllegalArgumentException e) {
            // IllegalArg can be thrown if the parent POM cannot be resolved.
            //handler.handle(Event.error("Unable to resolve Maven model from " + modelSource.getLocation()
            //        + ": " + e.getMessage()));
            return null;
        }
    }

    public  Model getRawModel(FileModelSource fileModelSource) {
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelResolver(this);
        request.setModelSource(fileModelSource);
        try {
            ModelBuildingResult result = modelBuilder.build(request);
            return result.getRawModel();
        } catch (ModelBuildingException | IllegalArgumentException e) {
            // IllegalArg can be thrown if the parent POM cannot be resolved.
            // handler.handle(Event.error("Unable to resolve raw Maven model from "
            //        + fileModelSource.getLocation() + ": " + e.getMessage()));
            return null;
        }
    }
}