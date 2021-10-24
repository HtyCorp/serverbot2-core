package com.admiralbot.nativeimagesupport.cache;

import com.admiralbot.framework.modelling.ApiDefinitionSet;
import com.google.gson.Gson;
import org.graalvm.nativeimage.ImageInfo;
import org.graalvm.nativeimage.ImageSingletons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class ImageCache {

    private static final Logger log = LoggerFactory.getLogger(ImageCache.class);
    private static final Gson FALLBACK_GSON = new Gson();

    private final Gson adaptedGson;
    private final Map<Class<?>, ApiDefinitionSet<?>> apiDefinitionSets;
    private final Map<Class<?>, TableSchema<?>> tableSchemas;

    /*
     * Whether this code is executing from a GraalVM native-image.
     * Catching NoClassDefFoundError is unusual, but it's necessary to handle every possible outcome:
     * 1) We are building or running from a native image (inImageCode returns true)
     * 2) We are running a Graal JVM (inImageCode returns false)
     * 3) We are running a non-Graal JVM (ImageInfo class load fails, defaults to false)
     */
    private static final boolean isInImageCode;
    static {
        boolean inImageCode;
        try {
            inImageCode = ImageInfo.inImageCode();
        } catch (NoClassDefFoundError e) {
            log.info("Graal SDK load failed (no class def found: {}). Assuming we are running on a standard JVM",
                    e.getMessage());
            inImageCode = false;
        }
        isInImageCode = inImageCode;
    }

    public ImageCache(Gson adaptedGson, Map<Class<?>, ApiDefinitionSet<?>> apiDefinitionSets, Map<Class<?>,
            TableSchema<?>> tableSchemas) {
        this.adaptedGson = adaptedGson;
        this.apiDefinitionSets = apiDefinitionSets;
        this.tableSchemas = tableSchemas;
    }

    public static Gson getGson() {
        if (isInImageCode) {
            return ImageSingletons.lookup(ImageCache.class).adaptedGson;
        }
        return FALLBACK_GSON;
    }

    @SuppressWarnings("unchecked")
    public static <T> ApiDefinitionSet<T> getApiDefinitionSet(Class<T> modelInterface) throws NoSuchElementException {
        if (isInImageCode) {
            return (ApiDefinitionSet<T>) getFromCache(c -> c.apiDefinitionSets, modelInterface);
        }
        return new ApiDefinitionSet<>(modelInterface);
    }

    @SuppressWarnings("unchecked")
    public static <T> TableSchema<T> getTableSchema(Class<T> beanClass) throws NoSuchElementException {
        if (isInImageCode) {
            return (TableSchema<T>) getFromCache(c -> c.tableSchemas, beanClass);
        }
        return TableSchema.fromBean(beanClass);
    }

    private static <K,V> V getFromCache(Function<ImageCache,Map<K,V>> mapGetter, K key) {
        ImageCache cache = ImageSingletons.lookup(ImageCache.class);
        Map<K,V> map = mapGetter.apply(cache);
        V item = map.get(key);
        if (item == null) {
            throw new NoSuchElementException("No value for key " + key);
        }
        return item;
    }
}