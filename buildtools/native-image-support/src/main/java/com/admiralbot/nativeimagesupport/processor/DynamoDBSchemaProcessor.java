package com.admiralbot.nativeimagesupport.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes({"software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class DynamoDBSchemaProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        annotations.forEach(annotation -> {
            String typeList = roundEnv.getElementsAnnotatedWith(annotation).stream()
                    .map(beanType -> ProcessorUtil.getBinaryName(processingEnv, (TypeElement) beanType))
                    .collect(Collectors.joining("\n"));
            new ResourceWriter(processingEnv).writeResourceContent(ResourcePaths.TABLE_SCHEMAS_RESOURCE.path(),
                    typeList);
        });

        return true;
    }

}
