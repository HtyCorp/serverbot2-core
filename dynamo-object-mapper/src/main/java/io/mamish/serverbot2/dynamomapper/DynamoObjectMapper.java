package io.mamish.serverbot2.dynamomapper;

import io.mamish.serverbot2.sharedutil.Pair;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
public class DynamoObjectMapper<DtoType> {

    private Constructor<DtoType> constructor;
    private Map<String, Field> registeredAttributes = new HashMap<>();
    private Pair<String, Field> partitionKeyField;
    private Pair<String, Field> sortKeyField;

    private final String tableName;
    private final boolean consistentRead;

    private final DynamoDbClient ddbClient = DynamoDbClient.create();
    private final ValueParser valueParser = new ValueParser();
    private final ValuePrinter valuePrinter = new ValuePrinter();

    // TODO: Obviously this doesn't work because not all fields are strings... Redo.
    // Possible redesign: POJOs with explicit Dynamo attribute descriptors as member fields
    // Stores field names to allow more reliable references (i.e. not strings) in conditions etc, e.g:
    // new EqualsCondition(MyObject::getState, State.READY);
    private final DtoType referenceNameObject;

    public DynamoObjectMapper(String tableName, Class<DtoType> dtoClass) {
        this(tableName, dtoClass, true);
    }

    public DynamoObjectMapper(String tableName, Class<DtoType> dtoClass, boolean consistentRead) {

        this.tableName = tableName;
        this.consistentRead = consistentRead;

        try {
            referenceNameObject = dtoClass.getConstructor().newInstance();

            for (Field field: dtoClass.getDeclaredFields()) {
                String name = field.getName();

                field.setAccessible(true);
                field.set(referenceNameObject, name);

                registeredAttributes.put(name, field);
                DynamoKey attr = field.getAnnotation(DynamoKey.class);
                if (attr != null) {
                    if (attr.value().equals(DynamoKeyType.PARTITION)) {
                        partitionKeyField = new Pair<>(name, field);
                    } else if (attr.value().equals(DynamoKeyType.SORT)) {
                        sortKeyField = new Pair<>(name, field);
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Error while performing initial DTO type processing", e);
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
        put(item, null);
    }

    public void put(DtoType item, EqualsCondition condition) throws ConditionalCheckFailedException {
        Map<String,AttributeValue> attributes = toAttributes(item);
        PutItemRequest.Builder putItem = PutItemRequest.builder().tableName(tableName).item(attributes);
        if (condition != null) {
            addConditionExpression(condition, putItem);
        }
        ddbClient.putItem(putItem.build());
    }

    private void addConditionExpression(EqualsCondition condition, PutItemRequest.Builder request) {
        request.expressionAttributeValues(Map.of(
                ":expected", mkString(valuePrinter.printObject(condition.getExpectedValue()))
        )).expressionAttributeNames(Map.of(
                "#C", condition.getAttributeName()
        )).conditionExpression("#C = :expected");
    }

    public void update(String pkey, UpdateSetter setter) {
        Map<String, AttributeValue> keyMap = Map.of(partitionKeyField.fst(), mkString(pkey));
        ddbClient.updateItem(r -> r.key(keyMap).expressionAttributeValues(Map.of(
                ":setvalue", mkString(valuePrinter.printObject(setter.getNewValue()))
        )).expressionAttributeNames(Map.of(
                "#V", setter.getAttributeName()
        )).conditionExpression("#C = :expected").updateExpression("SET #V = :setvalue"));
    }

    public void update(String pkey, EqualsCondition condition, UpdateSetter setter) {
        Map<String, AttributeValue> keyMap = Map.of(partitionKeyField.fst(), mkString(pkey));
        ddbClient.updateItem(r -> r.key(keyMap).expressionAttributeValues(Map.of(
                ":expected", mkString(valuePrinter.printObject(condition.getExpectedValue())),
                ":setvalue", mkString(valuePrinter.printObject(setter.getNewValue()))
        )).expressionAttributeNames(Map.of(
                "#C", condition.getAttributeName(),
                "#V", setter.getAttributeName()
        )).conditionExpression("#C = :expected").updateExpression("SET #V = :setvalue"));
    }

    public DtoType fromAttributes(Map<String,AttributeValue> attributeMap) {
        try {
            DtoType dto = constructor.newInstance();
            for (Map.Entry<String,Field> entry: registeredAttributes.entrySet()) {
                String key = entry.getKey();
                Field field = entry.getValue();
                Class<?> fieldType = field.getType();

                String mapValue = attributeMap.containsKey(key) ? attributeMap.get(key).s() : null;
                Object valueObject = valueParser.parseString(mapValue, fieldType);
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

                String valueString = valuePrinter.printObject(field.get(dto));
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
