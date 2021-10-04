package com.admiralbot.buildtools.nativeimagesupport;

import software.amazon.awssdk.utils.StringUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes({"com.admiralbot.sharedutil.annotation.ForceClassInitializeAtBuildTime"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class ForceBuildTimeInitProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.forEach(annotation ->
                roundEnv.getElementsAnnotatedWith(annotation).forEach(element ->
                        // This is a safe cast since the annotation has @Target(TYPE) set
                        processType((TypeElement) element)));
        return false;
    }

    private void processType(TypeElement annotatedType) {
        String typeName = annotatedType.getQualifiedName().toString();
        if (StringUtils.isBlank(typeName)) {
            throw new RuntimeException("Empty qualified name for type <" + annotatedType + ">");
        }
        String classInitOption = "Args=--initialize-at-build-time=" + typeName;
        FileWriter resourceWriter = new FileWriter(processingEnv);
        resourceWriter.writeNativeImageResource(annotatedType, "native-image.properties", classInitOption);
    }
}
