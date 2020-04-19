package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.commands.AbstractCommandDto;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedutil.reflect.ApiArgumentInfo;
import io.mamish.serverbot2.sharedutil.reflect.SimpleApiDefinition;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public class CommandDefinition extends SimpleApiDefinition {

    private static final String SIGIL = CommonConfig.COMMAND_SIGIL_CHARACTER;

    private String usageString;
    private List<String> argumentDescriptionStrings;

    public CommandDefinition(Method targetCommandMethod) throws ReflectiveOperationException {
        super(targetCommandMethod);

        if (!AbstractCommandDto.class.isAssignableFrom(getRequestDtoType())) {
            throw new IllegalStateException("Illegal request DTO type in generated definition: not a subclass of AbstractCommandDto");
        }

        List<ApiArgumentInfo> argInfoList = getOrderedFieldsInfoView();

        StringBuilder sbUsage = new StringBuilder();
        sbUsage.append(SIGIL).append(getName());
        for (int i = 0; i < argInfoList.size(); i++) {
            String argName = argInfoList.get(i).name();
            sbUsage.append(' ');
            if (i < getNumRequiredFields()) {
                sbUsage.append(argName);
            } else {
                sbUsage.append('[').append(argName).append(']');
            }
        }

        this.usageString = sbUsage.toString();
        this.argumentDescriptionStrings = argInfoList.stream().map(m -> m.name() + ": " + m.description()).collect(Collectors.toList());

    }

    public String getUsageString() {
        return usageString;
    }

    public List<String> getArgumentDescriptionStrings() {
        return argumentDescriptionStrings;
    }
}
