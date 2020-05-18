package io.mamish.serverbot2.sharedutil.reflect;

import io.mamish.serverbot2.sharedutil.Pair;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // Could make this a Map with some effort, but no need at the moment.
    public List<DtoType> scan() {
        ScanResponse response = ddbClient.scan(r -> r.tableName(tableName).consistentRead(consistentRead));
        if (!response.hasItems()) {
            return List.of();
        } else {
            return response.items().stream().map(this::fromAttributes).collect(Collectors.toList());
        }
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
                Class<?> fieldType = field.getType();

                String mapValue = attributeMap.containsKey(key) ? attributeMap.get(key).s() : null;
                Object valueObject;
                if (fieldType.equals(String.class)) {
                    valueObject = mapValue;
                } else if (Enum.class.isAssignableFrom(fieldType)) {
                    Method valueOf = fieldType.getMethod("valueOf", String.class);
                    valueObject = valueOf.invoke(null, mapValue);
                    //valueObject = Enum.valueOf((Class<Enum>)fieldType, mapValue);
                } else {
                    throw new IllegalArgumentException("DTO type has an illegal field type " + fieldType.getCanonicalName());
                }

                field.set(dto, valueObject);
            }
            return dto;
        } catch (Exception e) {
            // DTO constructors and types should be extremely simple, so no complicated exception handling.
            throw new RuntimeException("Unexpected reflective operation exception", e);
        }
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> clazz, String input) {
        return Enum.valueOf(clazz, input);
    }

    public Map<String,AttributeValue> toAttributes(DtoType dto) {
        try {
            Map<String,AttributeValue> resultAttributes = new HashMap<>();
            for (Map.Entry<String,Field> entry: registeredAttributes.entrySet()) {
                String key = entry.getKey();
                Field field = entry.getValue();
                Class<?> fieldType = field.getType();

                Object fieldValue = field.get(dto);
                String valueString;
                if (fieldType.equals(String.class)) {
                    valueString = (String) fieldValue;
                } else if (Enum.class.isAssignableFrom(fieldType)){
                    valueString = fieldValue.toString();
                } else {
                    throw new IllegalArgumentException("Invalid field type " + fieldType.getCanonicalName());
                }

                AttributeValue ddbValue = AttributeValue.builder().s(valueString).build();
                resultAttributes.put(key, ddbValue);
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
