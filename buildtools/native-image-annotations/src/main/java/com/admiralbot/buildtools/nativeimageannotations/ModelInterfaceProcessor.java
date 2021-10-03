package com.admiralbot.buildtools.nativeimageannotations;

import com.google.gson.JsonObject;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

@SupportedAnnotationTypes({
        "com.admiralbot.framework.common.FrameworkApiModel",
        "com.admiralbot.framework.common.ApiEndpointInfo"
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class ModelInterfaceProcessor extends AbstractProcessor {

    // Ref: https://www.graalvm.org/reference-manual/native-image/Reflection/
    private static final List<String> FIELD_REFLECT_FLAGS = List.of(
            "allDeclaredClasses",
            "allPublicClasses",
            "allDeclaredConstructors",
            "allPublicConstructors",
            "allDeclaredFields",
            "allPublicFields"
    );
    private static final List<String> INTERFACE_REFLECT_FLAGS = List.of(
            "allDeclaredClasses",
            "allPublicClasses",
            "allDeclaredMethods",
            "allPublicMethods"
    );

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.forEach(annotation -> roundEnv.getElementsAnnotatedWith(annotation)
                .forEach(this::processAnnotatedElement));
        return true;
    }

    private void processAnnotatedElement(Element annotatedElement) {
        if (annotatedElement.getKind() != ElementKind.INTERFACE) {
            throw new RuntimeException("Annotated type isn't an interface");
        }
        processAnnotatedInterface((TypeElement) annotatedElement);
    }

    private void processAnnotatedInterface(TypeElement interfaceType) {
        String interfaceName = getQualifiedTypeName(interfaceType);
        SortedSet<JsonObject> sortedConfigEntries = new TreeSet<>(comparing(e -> e.get("name").getAsString()));

        // Add actual interface type
        sortedConfigEntries.add(createReflectConfigEntry(interfaceName, INTERFACE_REFLECT_FLAGS));

        // Add types for each method (i.e. API operation) within the interface
        interfaceType.getEnclosedElements().stream()
                .filter(element -> element.getKind().equals(ElementKind.METHOD))
                .forEach(methodElement -> addTypesForMethod((ExecutableElement) methodElement, sortedConfigEntries));

        // Create proxy and reflect config files under META-INF
        NativeImageResourceWriter resourceWriter = new NativeImageResourceWriter(processingEnv);
        resourceWriter.writeString(interfaceType, "proxy-config.json", "[[\"" + interfaceName + "\"]]");
        resourceWriter.writeJson(interfaceType, "reflect-config.json", sortedConfigEntries);
    }

    private void addTypesForMethod(ExecutableElement method, SortedSet<JsonObject> sortedConfigEntries) {
        if (method.getParameters().size() != 1) {
            throw new RuntimeException("Operation method must have a single request argument");
        }
        TypeElement requestType = getTypeElement(method.getParameters().get(0).asType());
        TypeElement responseType = getTypeElement(method.getReturnType());
        registerFieldTypes(requestType, sortedConfigEntries);
        registerFieldTypes(responseType, sortedConfigEntries);
    }

    private void registerFieldTypes(TypeElement type, SortedSet<JsonObject> sortedConfigEntries) {
        JsonObject classConfigEntry = createReflectConfigEntry(getQualifiedTypeName(type), FIELD_REFLECT_FLAGS);
        boolean notSeenBefore = sortedConfigEntries.add(classConfigEntry);
        // Don't recurse into this type if we have seen it before
        if (notSeenBefore) {
            type.getEnclosedElements().stream()
                    .filter(this::isInstanceFieldElement)
                    .flatMap(element -> getTypesOfInterest(element.asType()))
                    .map(this::getTypeElement)
                    .forEach(fieldType -> registerFieldTypes(fieldType, sortedConfigEntries));
        }
    }

    private boolean isInstanceFieldElement(Element element) {
        return element.getKind().isField()
                && !element.getModifiers().contains(Modifier.STATIC);
    }

    private Stream<TypeMirror> getTypesOfInterest(TypeMirror type) {
        if (type.getKind().equals(TypeKind.ARRAY)) {
            ArrayType arrayType = (ArrayType) type;
            return getTypesOfInterest(arrayType.getComponentType());
        }
        if (type.getKind().equals(TypeKind.DECLARED)) {
            DeclaredType declaredType = (DeclaredType) type;
            TypeMirror rawType = processingEnv.getTypeUtils().erasure(declaredType);
            Stream<TypeMirror> argTypes = declaredType.getTypeArguments().stream().flatMap(this::getTypesOfInterest);
            return Stream.concat(Stream.of(rawType), argTypes);
        }
        if (type.getKind().isPrimitive() || type.getKind().equals(TypeKind.VOID)) {
            return Stream.empty();
        }
        throw new RuntimeException(type + ": not a supported type kind " + type.getKind());
    }

    private TypeElement getTypeElement(TypeMirror type) {
        TypeElement element = processingEnv.getElementUtils().getTypeElement(type.toString());
        if (element == null) {
            throw new RuntimeException("Couldn't map type '" + type + "' to a unique type element");
        }
        return element;
    }

    private String getQualifiedTypeName(TypeElement element) {
        String fullName = element.getQualifiedName().toString();
        if (fullName.isEmpty()) {
            throw new RuntimeException("Couldn't map element '" + element + "' to a qualified name");
        }
        return fullName;
    }

    private static JsonObject createReflectConfigEntry(String className, List<String> configFlags) {
        JsonObject classConfigEntry = new JsonObject();
        classConfigEntry.addProperty("name", className);
        configFlags.forEach(flag -> classConfigEntry.addProperty(flag, true));
        return classConfigEntry;
    }
}
