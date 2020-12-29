package com.admiralbot.urlshortener;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public abstract class BaseUrlInfoBean {

    protected int schemaVersion;
    protected String id;

    public BaseUrlInfoBean() { }

    public BaseUrlInfoBean(int schemaVersion, String id) {
        this.schemaVersion = schemaVersion;
        this.id = id;
    }

    @DynamoDbPartitionKey
    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    @DynamoDbSortKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
