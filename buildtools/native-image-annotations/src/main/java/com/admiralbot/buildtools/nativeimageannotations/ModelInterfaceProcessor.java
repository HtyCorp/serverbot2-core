package com.admiralbot.buildtools.nativeimageannotations;

import com.admiralbot.sharedutil.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Stream;

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

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotationElement : annotations) {
            for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(annotationElement)) {
                processAnnotatedElement(annotatedElement);
            }
        }
        return true;
    }

    private void processAnnotatedElement(Element annotatedElement) {
        if (annotatedElement.getKind() != ElementKind.INTERFACE) {
            throw new RuntimeException("Annotated type isn't an interface");
        }
        processAnnotatedInterface((TypeElement) annotatedElement);
    }

    private void processAnnotatedInterface(TypeElement interfaceType) {
        Set<JsonObject> classConfigEntries = new HashSet<>();

        // Add actual interface type
        classConfigEntries.add(createReflectConfigEntry(getQualifiedTypeName(interfaceType), INTERFACE_REFLECT_FLAGS));

        // Add types for each operation (method) within the interface
        interfaceType.getEnclosedElements().stream()
                .filter(element -> element.getKind().equals(ElementKind.METHOD))
                .forEach(element -> registerOperation((ExecutableElement) element, classConfigEntries));

        // Create proxy and reflect config files under META-INF
        createProxyConfigResourceFile(interfaceType, interfaceType.getQualifiedName().toString());
        createReflectConfigResourceFile(interfaceType, classConfigEntries);
    }

    private void registerOperation(ExecutableElement method, Set<JsonObject> classConfigEntries) {
        if (method.getParameters().size() != 1) {
            throw new RuntimeException("Operation method must have a single request argument");
        }
        TypeElement requestType = getTypeElement(method.getParameters().get(0).asType());
        TypeElement responseType = getTypeElement(method.getReturnType());
        registerFieldTypes(requestType, classConfigEntries);
        registerFieldTypes(responseType, classConfigEntries);
    }

    private void registerFieldTypes(TypeElement type, Set<JsonObject> classConfigEntries) {
        JsonObject classConfigEntry = createReflectConfigEntry(getQualifiedTypeName(type), FIELD_REFLECT_FLAGS);
        boolean notSeenBefore = classConfigEntries.add(classConfigEntry);
        // Don't recurse into this type if we have seen it before
        if (notSeenBefore) {
            type.getEnclosedElements().stream()
                    .filter(this::isInstanceFieldElement)
                    .flatMap(element -> getTypesOfInterest(element.asType()))
                    .map(this::getTypeElement)
                    .forEach(fieldType -> registerFieldTypes(fieldType, classConfigEntries));
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
        if (type.getKind().isPrimitive()) {
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

    private static JsonObject createReflectConfigEntry(String className, List<String> flagSet) {
        JsonObject interfaceConfigEntry = new JsonObject();
        interfaceConfigEntry.addProperty("name", className);
        flagSet.forEach(flag -> interfaceConfigEntry.addProperty(flag, true));
        return interfaceConfigEntry;
    }

    private void createProxyConfigResourceFile(TypeElement originElement, String interfaceClassName) {
        String proxyConfigJson = "[[\"" + interfaceClassName + "\"]]";
        writeNativeImageResourceFile(originElement, "proxy-config.json", proxyConfigJson);
    }

    private void createReflectConfigResourceFile(TypeElement originElement, Set<JsonObject> classConfigEntries) {
        List<JsonObject> finalEntries = new ArrayList<>(classConfigEntries);
        finalEntries.sort(Comparator.comparing(e -> e.get("name").getAsString()));
        writeNativeImageResourceFile(originElement, "reflect-config.json", gson.toJson(finalEntries));
    }

    private void writeNativeImageResourceFile(TypeElement originElement, String fileName, String fileContent) {
        String interfaceName = originElement.getQualifiedName().toString();
        try (Writer resourceFileWriter = processingEnv.getFiler().createResource(
                StandardLocation.CLASS_OUTPUT,
                "",
                Joiner.slash("META-INF", "native-image", interfaceName, fileName),
                originElement)
                .openWriter()
        ){
            resourceFileWriter.write(fileContent);
        } catch (IOException ioe) {
            throw new RuntimeException("Resource file write failed: " + ioe.getMessage());
        }
    }
}
