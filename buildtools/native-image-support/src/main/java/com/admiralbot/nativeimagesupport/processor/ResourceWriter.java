package com.admiralbot.nativeimagesupport.processor;

import com.admiralbot.sharedutil.ExceptionUtils;
import com.admiralbot.sharedutil.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.binary.Hex;
import software.amazon.awssdk.core.SdkBytes;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.security.MessageDigest;

/**
 * Warning: Don't instantiate this in any processor constructor since processingEnv may be null.
 * This should be instantiated during the processing round, when actually needed to write files.
 */
public class ResourceWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final ProcessingEnvironment processingEnv;

    public ResourceWriter(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public void writeNativeImageResourceJson(String fileName, Object contentJson) {
        String fileContent = GSON.toJson(contentJson);
        writeNativeImageResourceContent(fileName, fileContent);
    }

    public void writeNativeImageResourceContent(String fileName, String fileContent) {
        String filePath = Joiner.slash("META-INF", "native-image", getContentHash(fileContent), fileName);
        writeResourceContent(filePath, fileContent);
    }

    private static String getContentHash(String fileContent) {
        MessageDigest md5 = ExceptionUtils.cantFail(() -> MessageDigest.getInstance("MD5"));
        byte[] digestBytes = md5.digest(SdkBytes.fromUtf8String(fileContent).asByteArray());
        return Hex.encodeHexString(digestBytes);
    }

    public void writeResourceContent(String filePath, String fileContent) {
        // Technically we are supposed to include "origin elements" to help tools figure out when files need to be
        // regenerated based on their sources changing, but it's not required with a typical `mvn clean install` cycle.
        try (Writer resourceFileWriter = processingEnv.getFiler().createResource(
                StandardLocation.CLASS_OUTPUT,
                "",
                filePath)
                .openWriter()
        ){
            resourceFileWriter.write(fileContent);
        } catch (IOException ioe) {
            throw new RuntimeException("Resource file write failed: " + ioe.getMessage());
        }
    }
}
