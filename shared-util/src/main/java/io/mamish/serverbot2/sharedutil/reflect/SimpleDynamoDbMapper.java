package io.mamish.serverbot2.sharedutil.reflect;

import com.sun.source.tree.CatchTree;
import io.mamish.serverbot2.sharedutil.Pair;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple mapper between items in a DynamoDB table and Java DTOs. Support string types only.
 *
 * Note exception handling for reflection isn't really there. It assumes nothing will go wrong since that would be a
 * programming error more than an unexpected runtime condition.
 *
 * @param <DtoType> The type of the DTO object generated and processed by the mapper.
 */
public class SimpleDynamoDbMapper<DtoType> {

    private Constructor<DtoType> constructor;
    private Map<String, Field> registeredAttributes = new HashMap<>();
    private Pair<String, Field> partitionKeyField;
    private Pair<String, Field> sortKeyField;

    private String tableName;
    private boolean consistentRead;

    private DynamoDbClient ddbClient = DynamoDbClient.create();

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

    public boolean has(String pkey) {
        return has(pkey, null);
    }

    public boolean has(String pkey, String skey) {
        return ddbGetAttributes(pkey, skey) != null;
    }

    public DtoType get(String pkey) {
        return get(pkey, null);
    }

    public DtoType get(String pkey, String skey) {
        Map<String,AttributeValue> attributes = ddbGetAttributes(pkey, skey);
        if (attributes != null) {
            return fromAttributes(attributes);
        }
        return null;
    }

    public void put(DtoType item) {
        Map<String,AttributeValue> attributes = toAttributes(item);
        ddbClient.putItem(r -> r.tableName(tableName).item(attributes));
    }

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
            throw new RuntimeException("Unexpected reflective operation exception", e);
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
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unexpected reflective operation exception", e);
        }
    }

    private Map<String,AttributeValue> ddbGetAttributes(String pkey, String skey) {
        Map<String,AttributeValue> keyMap;
        if (skey == null) {
            keyMap = Map.of(partitionKeyField.fst(), mkString(pkey));
        } else {
            keyMap = Map.of(partitionKeyField.fst(), mkString(pkey), sortKeyField.fst(), mkString(skey));
        }

        GetItemResponse response = ddbClient.getItem(r -> r.tableName(tableName)
                .key(keyMap)
                .consistentRead(consistentRead));

        if (!response.hasItem()) {
            return null;
        }
        return response.item();
    }

    private AttributeValue mkString(String s) {
        return AttributeValue.builder().s(s).build();
    }

}
