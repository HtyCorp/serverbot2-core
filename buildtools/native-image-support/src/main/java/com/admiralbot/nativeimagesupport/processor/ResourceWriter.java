package com.admiralbot.nativeimagesupport.processor;

import com.admiralbot.sharedutil.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;

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

    public void writeNativeImageResource(TypeElement originElement, String fileName, String contentString) {
        writeNativeImageResourceContent(originElement, fileName, contentString);
    }

    public void writeNativeImageResourceJson(TypeElement originElement, String fileName, Object contentJson) {
        String fileContent = GSON.toJson(contentJson);
        writeNativeImageResourceContent(originElement, fileName, fileContent);
    }

    public void writeNativeImageResourceContent(TypeElement originElement, String fileName, String fileContent) {
        String originName = ProcessorUtil.getQualifiedName(originElement);
        String filePath = Joiner.slash("META-INF", "native-image", originName, fileName);
        writeResourceContent(filePath, fileContent);
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
