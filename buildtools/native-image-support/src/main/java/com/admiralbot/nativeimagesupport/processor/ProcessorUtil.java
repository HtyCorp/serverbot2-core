package com.admiralbot.nativeimagesupport.processor;

import software.amazon.awssdk.utils.StringUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

public final class ProcessorUtil {

    private ProcessorUtil() {}

    public static String getQualifiedName(TypeElement type) throws IllegalArgumentException {
        String name = type.getQualifiedName().toString();
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("No valid qualified name for type <" + type + ">");
        }
        return name;
    }

    public static String getBinaryName(ProcessingEnvironment env, TypeElement type) throws IllegalArgumentException {
        String name = env.getElementUtils().getBinaryName(type).toString();
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("No valid binary name for type <" + type + ">");
        }
        return name;
    }
}
