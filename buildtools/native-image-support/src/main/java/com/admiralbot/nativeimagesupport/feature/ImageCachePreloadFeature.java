package com.admiralbot.nativeimagesupport.feature;

import com.admiralbot.framework.modelling.ApiDefinitionSet;
import com.admiralbot.nativeimagesupport.cache.ImageCache;
import com.admiralbot.nativeimagesupport.processor.ResourcePaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.utils.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unused")
public class ImageCachePreloadFeature implements Feature {

    private static final Gson GSON = new Gson();

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        ClassLoader appClassLoader = access.getApplicationClassLoader();
        Gson adaptedGson = createAdaptedGson(appClassLoader);
        Map<Class<?>, ApiDefinitionSet<?>> apiDefinitionSets = createApiDefinitionSets(appClassLoader);
        Map<Class<?>, TableSchema<?>> tableSchemas = createTableSchemas(appClassLoader);

        // Placeholder for API defs, still need to implement that
        ImageCache runtimeCache = new ImageCache(adaptedGson, apiDefinitionSets, tableSchemas);

        ImageSingletons.add(ImageCache.class, runtimeCache);
    }

    private Gson createAdaptedGson(ClassLoader appClassLoader) {
        Map<Class<?>, TypeAdapter<?>> typeAdapters = preloadInstances(appClassLoader,
                ResourcePaths.GSON_ADAPTERS_RESOURCE.path(), GSON::getAdapter);
        var gson = new GsonBuilder();
        typeAdapters.forEach(gson::registerTypeAdapter);
        return gson.create();
    }

    private Map<Class<?>, ApiDefinitionSet<?>> createApiDefinitionSets(ClassLoader appClassLoader) {
        return preloadInstances(appClassLoader, ResourcePaths.API_DEFINITION_SETS_RESOURCE.path(),
                ApiDefinitionSet::new);
    }

    private Map<Class<?>, TableSchema<?>> createTableSchemas(ClassLoader appClassLoader) {
        return preloadInstances(appClassLoader, ResourcePaths.TABLE_SCHEMAS_RESOURCE.path(),
                TableSchema::fromBean);
    }

    private <V> Map<Class<?>,V> preloadInstances(ClassLoader appClassLoader, String resourcePath,
                                                        Function<Class<?>, V> instanceGenerator) {
        final Map<Class<?>, V> tempMap = new HashMap<>();
        forAllLines(appClassLoader, resourcePath, classNameLine -> {
            Class<?> classForLine;
            try {
                classForLine = appClassLoader.loadClass(classNameLine);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Could not locate indicated class <" + classNameLine + ">", e);
            }
            V instance = instanceGenerator.apply(classForLine);
            tempMap.put(classForLine, instance);
        });
        return tempMap;
    }

    private void forAllLines(ClassLoader appClassLoader, String resourcePath, Consumer<String> lineConsumer) {
        try {
            Iterator<URL> iterator = appClassLoader.getResources(resourcePath).asIterator();
            while (iterator.hasNext()) {
                URL resourceUrl = iterator.next();
                var lineReader = new BufferedReader(new InputStreamReader(resourceUrl.openStream()));
                String line;
                while ((line = lineReader.readLine()) != null) {
                    if (StringUtils.isNotBlank(line)) {
                        lineConsumer.accept(line);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource lines at <" + resourcePath + ">", e);
        }
    }

}
