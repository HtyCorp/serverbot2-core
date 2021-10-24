package com.admiralbot.nativeimagesupport.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes({
        "com.admiralbot.framework.modelling.FrameworkApiModel",
        "com.admiralbot.framework.modelling.ApiEndpointInfo"
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class ModelInterfaceProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        annotations.forEach(annotation -> {
            List<String> modelNames = roundEnv.getElementsAnnotatedWith(annotation).stream()
                    .map(modelClass -> ProcessorUtil.getBinaryName(processingEnv, (TypeElement) modelClass))
                    .collect(Collectors.toList());

            ResourceWriter writer = new ResourceWriter(processingEnv);

            // Write model list to be initialized by ImageCachePreloadFeature
            writer.writeResourceContent(ResourcePaths.API_DEFINITION_SETS_RESOURCE.path(),
                    String.join("\n", modelNames));

            // Write proxy-config.json list (of lists) to be used directly by native-image
            writer.writeNativeImageResourceJson("proxy-config.json", modelNames.stream()
                    .map(List::of)
                    .collect(Collectors.toList()));
        });

        return true;
    }

}
