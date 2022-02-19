#!/usr/bin/env bash

# Slightly hacky script to work around the 256MB input artifact size limit for CFN pipeline actions.
# Ref: https://docs.aws.amazon.com/codepipeline/latest/userguide/limits.html
#
# Since assets are what makes the artifact so big, we use a separate, cloned artifact with assets removed.
# Relevant GitHub issue: https://github.com/aws/aws-cdk/issues/9917

# Make a copy of the cloud assembly but exclude any asset files
rsync -av gen/cloud_assembly/ gen/cloud_assembly_no_assets/ --exclude '/asset.*'

# Prep a JQ expression to update all pipeline CFN change-set-replace actions to use the no-assets artifact
JQ_PIPELINE_ACTIONS_PATH='.Resources.DeploymentPipelineD53681EF.Properties.Stages[].Actions[]'
JQ_ACTION_IS_CFN_REPLACE='.ActionTypeId.Provider == "CloudFormation" and .Configuration.ActionMode == "CHANGE_SET_REPLACE"'
JQ_OVERWRITE_INPUT_ARTIFACT='.InputArtifacts = [{Name:"cloud_assembly_no_assets"}]'
JQ_OVERWRITE_TEMPLATE_PATH='.Configuration.TemplatePath |= sub("cloud_assembly::";"cloud_assembly_no_assets::")'
JQ_EXPR="$JQ_PIPELINE_ACTIONS_PATH |= if ($JQ_ACTION_IS_CFN_REPLACE) then (($JQ_OVERWRITE_INPUT_ARTIFACT) | ($JQ_OVERWRITE_TEMPLATE_PATH)) else . end"

# Edit the pipeline stack template file in-place
PIPELINE_STACK_FILE='gen/cloud_assembly/DeploymentPipelineStack.template.json'
jq "$JQ_EXPR" $PIPELINE_STACK_FILE | sponge $PIPELINE_STACK_FILE