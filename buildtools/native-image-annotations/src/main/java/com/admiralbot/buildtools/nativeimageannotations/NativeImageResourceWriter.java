package com.admiralbot.buildtools.nativeimageannotations;

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
public class NativeImageResourceWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final ProcessingEnvironment processingEnvironment;

    public NativeImageResourceWriter(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    public void writeString(TypeElement originElement, String fileName, String contentString) {
        doWrite(originElement, fileName, contentString);
    }

    public void writeJson(TypeElement originElement, String fileName, Object contentJson) {
        String fileContent = GSON.toJson(contentJson);
        doWrite(originElement, fileName, fileContent);
    }

    private void doWrite(TypeElement originElement, String fileName, String fileContent) {
        // 'Origin' is a vague term for the originally annotated object that triggered this processing,
        // which is used by IDEs and such to determine when to run the processor again.
        // We use it as an origin and as a way to differentiate config files under META-INF
        String originName = originElement.getQualifiedName().toString();
        try (Writer resourceFileWriter = processingEnvironment.getFiler().createResource(
                StandardLocation.CLASS_OUTPUT,
                "",
                Joiner.slash("META-INF", "native-image", originName, fileName),
                originElement)
                .openWriter()
        ){
            resourceFileWriter.write(fileContent);
        } catch (IOException ioe) {
            throw new RuntimeException("Resource file write failed: " + ioe.getMessage());
        }
    }
}
