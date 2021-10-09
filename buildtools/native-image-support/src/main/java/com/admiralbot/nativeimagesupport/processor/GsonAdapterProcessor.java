package com.admiralbot.nativeimagesupport.processor;


import com.admiralbot.sharedutil.Utils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes({"com.admiralbot.nativeimagesupport.annotation.RegisterGsonType"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class GsonAdapterProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.forEach(annotation -> {
            String typeList = roundEnv.getElementsAnnotatedWith(annotation).stream()
                    .map(this::getTargetTypeFromElement)
                    .map(typeElement -> ProcessorUtil.getBinaryName(processingEnv, typeElement))
                    .collect(Collectors.joining("\n"));
            new ResourceWriter(processingEnv).writeResourceContent(ResourcePaths.GSON_ADAPTERS_RESOURCE.path(),
                    typeList);
        });

        return true;
    }

    // TODO: Needs better inspection ability over generics
    // TODO: Needs to be able to use field type (i.e. annotation on JSON object field rather than on TypeAdapter)
    private TypeElement getTargetTypeFromElement(Element element) {
        if (Utils.equalsAny(element.getKind(),
                ElementKind.CLASS, ElementKind.ENUM)) {
            return (TypeElement) element;
        } else if (Utils.equalsAny(element.getKind(),
                ElementKind.FIELD, ElementKind.LOCAL_VARIABLE, ElementKind.PARAMETER)) {
            DeclaredType type = (DeclaredType) element.asType();
            String baseQualifiedName = processingEnv.getTypeUtils().erasure(type).toString();
            return processingEnv.getElementUtils().getTypeElement(baseQualifiedName);
        } else {
            throw new RuntimeException("Unexpected element kind: " + element.getKind());
        }
    }
}
