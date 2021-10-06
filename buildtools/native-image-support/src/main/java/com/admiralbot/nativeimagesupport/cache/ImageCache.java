package com.admiralbot.nativeimagesupport.cache;

import com.admiralbot.framework.modelling.ApiDefinitionSet;
import com.google.gson.TypeAdapter;
import org.graalvm.nativeimage.ImageSingletons;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class ImageCache {

    private final Map<Class<?>, TypeAdapter<?>> gsonAdapters;
    private final Map<Class<?>, ApiDefinitionSet<?>> apiDefinitionSets;
    private final Map<Class<?>, TableSchema<?>> tableSchemas;

    public ImageCache(Map<Class<?>, TypeAdapter<?>> gsonAdapters, Map<Class<?>, ApiDefinitionSet<?>> apiDefinitionSets,
                      Map<Class<?>, TableSchema<?>> tableSchemas) {
        this.gsonAdapters = Collections.unmodifiableMap(gsonAdapters);
        this.apiDefinitionSets = Collections.unmodifiableMap(apiDefinitionSets);
        this.tableSchemas = Collections.unmodifiableMap(tableSchemas);
    }

    @SuppressWarnings("unchecked")
    public static <T> TableSchema<T> getTableSchema(Class<T> beanClass) throws NoSuchElementException {
        return (TableSchema<T>) getFromCache(c -> c.tableSchemas, beanClass);
    }

    private static Object getFromCache(Function<ImageCache,Map<?,?>> mapGetter, Object key) {
        ImageCache cache = ImageSingletons.lookup(ImageCache.class);
        Map<?,?> map = mapGetter.apply(cache);
        Object item = map.get(key);
        if (item == null) {
            throw new NoSuchElementException("No value for key " + key);
        }
        return item;
    }
}