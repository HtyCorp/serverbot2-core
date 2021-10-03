package com.admiralbot.buildtools.nativeimageannotations;

import software.amazon.awssdk.utils.StringUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes({"software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class DynamoDBSchemaProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.forEach(annotation -> roundEnv.getElementsAnnotatedWith(annotation).forEach(beanType ->
                createBeanSchemaSource((TypeElement) beanType)));
        return true;
    }

    private void createBeanSchemaSource(TypeElement beanType) {
        String packageName = getPackageName(beanType);
        String beanSimpleName = beanType.getSimpleName().toString();
        String beanFullName = getQualifiedTypeName(beanType);
        String schemaSimpleName = beanType.getSimpleName().toString() + "Schema";
        String schemaFullName = getQualifiedTypeName(beanType) + "Schema";

        String sourceContent =
                "package " + packageName + ";\n" +
                "import software.amazon.awssdk.enhanced.dynamodb.TableSchema;\n" +
                "import " + beanFullName + ";\n" +
                "public class " + schemaSimpleName + " {\n" +
                "public static final TableSchema<" + beanSimpleName + "> INSTANCE = " +
                "TableSchema.fromBean(" + beanSimpleName + ".class);\n" +
                "}";
        String nativeImagePropertiesContent = "Args=--initialize-at-build-time=" + schemaFullName;

        FileWriter writer = new FileWriter(processingEnv);
        writer.writeSourceString(beanType, schemaFullName, sourceContent);
        writer.writeNativeImageResource(beanType, "native-image.properties", nativeImagePropertiesContent);
    }

    private String getPackageName(TypeElement type) {
        if (type.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
            throw new RuntimeException("Type " + type + " is a nested type");
        }
        String packageName = ((PackageElement) type.getEnclosingElement()).getQualifiedName().toString();
        if (StringUtils.isBlank(packageName)) {
            throw new RuntimeException("Null or empty package name <" + packageName + ">");
        }
        return packageName;
    }

    private String getQualifiedTypeName(TypeElement element) {
        String fullName = element.getQualifiedName().toString();
        if (fullName.isEmpty()) {
            throw new RuntimeException("Couldn't map element '" + element + "' to a qualified name");
        }
        return fullName;
    }
}
