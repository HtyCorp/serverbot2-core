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

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
        SortedSet<JsonObject> sortedConfigEntries = new TreeSet<>(comparing(e -> e.get("name").getAsString()));

        // Add actual interface type
        sortedConfigEntries.add(createReflectConfigEntry(getQualifiedTypeName(interfaceType), INTERFACE_REFLECT_FLAGS));

        // Add types for each method (i.e. API operation) within the interface
        interfaceType.getEnclosedElements().stream()
                .filter(element -> element.getKind().equals(ElementKind.METHOD))
                .forEach(methodElement -> addTypesForMethod((ExecutableElement) methodElement, sortedConfigEntries));

        // Create proxy and reflect config files under META-INF
        createProxyConfigResourceFile(interfaceType, getQualifiedTypeName(interfaceType));
        createReflectConfigResourceFile(interfaceType, sortedConfigEntries);
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

    private void createProxyConfigResourceFile(TypeElement originElement, String interfaceClassName) {
        String proxyConfigJson = "[[\"" + interfaceClassName + "\"]]";
        writeNativeImageResourceFile(originElement, "proxy-config.json", proxyConfigJson);
    }

    private void createReflectConfigResourceFile(TypeElement originElement, SortedSet<JsonObject> sortedConfigEntries) {
        writeNativeImageResourceFile(originElement, "reflect-config.json", GSON.toJson(sortedConfigEntries));
    }

    private void writeNativeImageResourceFile(TypeElement originElement, String fileName, String fileContent) {
        // 'Origin' is a vague term for the originally annotated object that triggered this processing,
        // which is used by IDEs and such to determine when to run the processor again.
        // We use it as an origin and as a way to differentiate config files under META-INF
        String originName = originElement.getQualifiedName().toString();
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
}
