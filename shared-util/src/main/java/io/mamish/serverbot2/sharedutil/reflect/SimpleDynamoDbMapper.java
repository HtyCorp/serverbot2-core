package io.mamish.serverbot2.sharedutil.reflect;

import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedutil.Pair;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class SimpleDynamoDbMapper<DtoType> {

    /*
     * Need to work out the scope of this thing. It's broadly useful, but I don't want to make DDB a dependency in this
     * module. Maybe split into a separate utility module.
     */

    private Constructor<DtoType> constructor;
    private Map<String, Field> registeredAttributes = new HashMap<>();
    private Pair<String, Field> partitionKeyField;
    private Pair<String, Field> sortKeyField;

    private String tableName;
    private boolean consistentRead;

    public SimpleDynamoDbMapper(String tableName, Class<DtoType> dtoClass) {
        this(tableName, dtoClass, true);
    }

    public SimpleDynamoDbMapper(String tableName, Class<DtoType> dtoClass, boolean consistentRead) {

        this.tableName = tableName;
        this.consistentRead = consistentRead;

        for (Field field: dtoClass.getDeclaredFields()) {
            DdbAttribute attr = field.getAnnotation(DdbAttribute.class);
            if (attr != null) {
                registeredAttributes.put(attr.value(), field);
                if (attr.keyType().equals(DdbKeyType.PARTITION)) {
                    partitionKeyField = new Pair<>(attr.value(), field);
                } else if (attr.keyType().equals(DdbKeyType.SORT)) {
                    sortKeyField = new Pair<>(attr.value(), field);
                }
            }
        }

    }

//    public DtoType fromPrimaryKey(String pkey) {
//        // See note at top.
//    }
//
//    public DtoType fromPrimaryKey(String pkey, String skey) {
//        // See note at top.
//    }

    public DtoType fromAttributes(Map<String,AttributeValue> attributeMap) {
        try {
            DtoType dto = constructor.newInstance();
            for (Map.Entry<String,Field> entry: registeredAttributes.entrySet()) {
                String key = entry.getKey();
                Field field = entry.getValue();
                // Obviously this assumes String types.
                String mapValue = attributeMap.containsKey(key) ? attributeMap.get(key).s() : null;
                field.set(dto, mapValue);
            }
            return dto;
        } catch (Exception e) {
            // DTO constructors and types should be extremely simple, so no complicated exception handling.
            throw new RuntimeException("DTO constructor failed", e);
        }
    }

    public Map<String,AttributeValue> toAttributes(DtoType dto) {
        try {
            Map<String,AttributeValue> resultAttributes = new HashMap<>();
            for (Map.Entry<String,Field> entry: registeredAttributes.entrySet()) {
                String key = entry.getKey();
                String valueString = (String) entry.getValue().get(dto);
                AttributeValue value = AttributeValue.builder().s(valueString).build();
                resultAttributes.put(key, value);
            }
            return resultAttributes;
        } catch (Exception e) {
            throw new RuntimeException("DTO constructor failed", e);
        }
    }

}
