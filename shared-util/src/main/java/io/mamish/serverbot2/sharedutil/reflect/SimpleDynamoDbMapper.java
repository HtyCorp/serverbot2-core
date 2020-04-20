package io.mamish.serverbot2.sharedutil.reflect;

import io.mamish.serverbot2.sharedutil.Pair;

import org.w3c.dom.Attr;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimpleDynamoDbMapper<DtoType> {

    private Constructor<DtoType> constructor;
    private List<Pair<String, Field>> namedFields;

    public SimpleDynamoDbMapper(Class<DtoType> dtoClass) {
        namedFields = Arrays.stream(dtoClass.getDeclaredFields())
                .map(f -> new Pair<>(f.getName(),f))
                .collect(Collectors.toList());
        namedFields.forEach(p -> {
            assert p.snd().getType().equals(String.class);
            p.snd().setAccessible(true);
        });
    }

    public DtoType fromAttributes(Map<String,AttributeValue> attributeMap) {
        try {
            DtoType dto = constructor.newInstance();
            for (Pair<String,Field> p: namedFields) {
                String key = p.fst();
                Field field = p.snd();
                // Obviously this assume String types.
                String mapValue = attributeMap.containsKey(key) ? attributeMap.get(key).s() : null;
                field.set(dto, mapValue);
            }
            return dto;
        } catch (Exception e) {
            // DTO constructors and types should be extremely simple, so no complicated exception handling.
            throw new RuntimeException("DTO constructor somehow failed", e);
        }
    }

    public Map<String,AttributeValue> toAttributes(DtoType dto) {
        try {
            Map<String,AttributeValue> instance = new HashMap<>();
            for (Pair<String,Field> p: namedFields) {
                String key = p.fst();
                String valueString = (String) p.snd().get(dto);
                AttributeValue value = AttributeValue.builder().s(valueString).build();
                instance.put(key, value);
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("DTO constructor somehow failed", e);
        }
    }

}
