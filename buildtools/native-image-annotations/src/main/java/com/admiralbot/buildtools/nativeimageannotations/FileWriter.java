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
public class FileWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final ProcessingEnvironment processingEnv;

    public FileWriter(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public void writeSourceString(TypeElement originElement, String className, String sourceContent) {
        writeSourceFile(originElement, className, sourceContent);
    }

    public void writeNativeImageResource(TypeElement originElement, String fileName, String contentString) {
        writeNativeImageResourceContent(originElement, fileName, contentString);
    }

    public void writeNativeImageResourceJson(TypeElement originElement, String fileName, Object contentJson) {
        String fileContent = GSON.toJson(contentJson);
        writeNativeImageResourceContent(originElement, fileName, fileContent);
    }

    private void writeSourceFile(TypeElement originElement, String className, String sourceContent) {
        try (var sourceWriter = processingEnv.getFiler().createSourceFile(className, originElement).openWriter()) {
            sourceWriter.write(sourceContent);
        } catch (IOException ioe) {
            throw new RuntimeException("Source file write failed: " + ioe.getMessage());
        }
    }

    private void writeNativeImageResourceContent(TypeElement originElement, String fileName, String fileContent) {
        // 'Origin' is a vague term for the originally annotated object that triggered this processing,
        // which is used by IDEs and such to determine when to run the processor again.
        // We use it as an origin and as a way to differentiate config files under META-INF
        String originName = getQualifiedTypeName(originElement);
        try (Writer resourceFileWriter = processingEnv.getFiler().createResource(
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

    private String getQualifiedTypeName(TypeElement element) {
        String fullName = element.getQualifiedName().toString();
        if (fullName.isEmpty()) {
            throw new RuntimeException("Couldn't map element '" + element + "' to a qualified name");
        }
        return fullName;
    }
}
