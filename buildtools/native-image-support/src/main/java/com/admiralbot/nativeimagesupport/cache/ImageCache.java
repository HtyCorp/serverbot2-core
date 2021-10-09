package com.admiralbot.nativeimagesupport.cache;

import com.admiralbot.framework.modelling.ApiDefinitionSet;
import com.google.gson.Gson;
import org.graalvm.nativeimage.ImageSingletons;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class ImageCache {

    private final Gson adaptedGson;
    private final Map<Class<?>, ApiDefinitionSet<?>> apiDefinitionSets;
    private final Map<Class<?>, TableSchema<?>> tableSchemas;

    public ImageCache(Gson adaptedGson, Map<Class<?>, ApiDefinitionSet<?>> apiDefinitionSets, Map<Class<?>,
            TableSchema<?>> tableSchemas) {
        this.adaptedGson = adaptedGson;
        this.apiDefinitionSets = apiDefinitionSets;
        this.tableSchemas = tableSchemas;
    }

    public static Gson getGson() {
        return ImageSingletons.lookup(ImageCache.class).adaptedGson;
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