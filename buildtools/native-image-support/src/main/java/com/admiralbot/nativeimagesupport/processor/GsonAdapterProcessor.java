package com.admiralbot.nativeimagesupport.processor;


import com.admiralbot.nativeimagesupport.annotation.RegisterGsonType;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypesException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SupportedAnnotationTypes({"com.admiralbot.nativeimagesupport.annotation.RegisterGsonType"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class GsonAdapterProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.forEach(annotation -> {
            String typeList = roundEnv.getElementsAnnotatedWith(annotation).stream()
                    .flatMap(this::getTypesFromElement)
                    .map(typeElement -> ProcessorUtil.getBinaryName(processingEnv, typeElement))
                    .collect(Collectors.joining("\n"));
            new ResourceWriter(processingEnv).writeResourceContent(ResourcePaths.GSON_ADAPTERS_RESOURCE.path(),
                    typeList);
        });
        return true;
    }

    private Stream<TypeElement> getTypesFromElement(Element element) {
        RegisterGsonType annotation = element.getAnnotation(RegisterGsonType.class);
        List<TypeElement> typesToRegister = new ArrayList<>();
        List<DeclaredType> explicitTypes = getTypeMirrorsFromAnnotation(annotation);
        if (annotation.includeThis() || explicitTypes.isEmpty()) {
            typesToRegister.add((TypeElement) element);
        }
        explicitTypes.forEach(type -> {
            typesToRegister.add((TypeElement) type.asElement());
        });
        return typesToRegister.stream();
    }

    @SuppressWarnings("unchecked")
    private static List<DeclaredType> getTypeMirrorsFromAnnotation(RegisterGsonType annotation) {
        try {
            //noinspection ResultOfMethodCallIgnored
            annotation.types();
            throw new RuntimeException("Should never reach here!");
        } catch (MirroredTypesException expectedException) {
            return (List<DeclaredType>) expectedException.getTypeMirrors();
        }
    }
}
