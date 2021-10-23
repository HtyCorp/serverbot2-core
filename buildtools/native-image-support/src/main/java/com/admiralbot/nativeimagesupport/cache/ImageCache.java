package com.admiralbot.nativeimagesupport.cache;

import com.admiralbot.framework.modelling.ApiDefinitionSet;
import com.google.gson.Gson;
import org.graalvm.nativeimage.ImageInfo;
import org.graalvm.nativeimage.ImageSingletons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class ImageCache {

    private static final Logger log = LoggerFactory.getLogger(ImageCache.class);
    private static final Gson FALLBACK_GSON = new Gson();

    private static Boolean isInImageCode;

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
        if (isExecutingFromImage()) {
            return ImageSingletons.lookup(ImageCache.class).adaptedGson;
        }
        return FALLBACK_GSON;
    }

    @SuppressWarnings("unchecked")
    public static <T> TableSchema<T> getTableSchema(Class<T> beanClass) throws NoSuchElementException {
        if (isExecutingFromImage()) {
            return (TableSchema<T>) getFromCache(c -> c.tableSchemas, beanClass);
        }
        return TableSchema.fromBean(beanClass);
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

    private static boolean isExecutingFromImage() {
        if (isInImageCode == null) {
            synchronized (ImageCache.class) {
                try {
                    Class<?> imageInfo = Class.forName("org.graalvm.nativeimage.ImageInfo");
                    Method inImageCodeMethod = imageInfo.getDeclaredMethod("inImageCode");
                    isInImageCode = (Boolean) inImageCodeMethod.invoke(null);
                    log.info("Detected GraalVM, native image mode = {}", isInImageCode);
                } catch (ReflectiveOperationException e) {
                    log.info("Not running in GraalVM, instances will be runtime-generated");
                    isInImageCode = Boolean.FALSE;
                }
            }
        }
        return isInImageCode;
    }
}