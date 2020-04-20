package io.mamish.serverbot2.sharedutil;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedutil.reflect.SerializationException;
import io.mamish.serverbot2.sharedutil.reflect.UnparsableInputException;

public class AnnotatedGson {

    private Gson gson = new Gson();

    public Gson getGson() {
        return gson;
    }

    public <T> T fromJson(String json, Class<T> typeOfT) {
        return gson.fromJson(json, typeOfT);
    }

    public Pair<String,JsonObject> fromJsonWithTargetName(String jsonString) {
        try {
            JsonObject obj = JsonParser.parseString(jsonString).getAsJsonObject();
            String targetName = obj.remove(CommonConfig.JSON_API_TARGET_KEY).getAsString();
            return new Pair<>(targetName, obj);
        } catch (RuntimeException e) {
            throw new UnparsableInputException("Could not parse request target name from JSON", e);
        }
    }

    public String toJson(Object request) {
        return gson.toJson(request);
    }

    public String toJsonWithTargetName(Object request, String name) throws SerializationException {
        JsonElement jsonTree = gson.toJsonTree(request);
        if (!jsonTree.isJsonObject()) {
            throw new SerializationException("Response object is not a JSON object. Java type is " + request.getClass());
        }
        JsonObject jsonObject = jsonTree.getAsJsonObject();

        String KEY = CommonConfig.JSON_API_TARGET_KEY;
        if (jsonObject.has(KEY)) {
            throw new SerializationException("Response object already has a " + KEY + "property set.");
        }
        jsonObject.addProperty(KEY, name);
        return gson.toJson(jsonObject);
    }

}
