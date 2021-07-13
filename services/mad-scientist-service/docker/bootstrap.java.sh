#!/bin/sh
echo 'Executing via Java...'
java -Dorg.apache.commons.logging.diagnostics.dest=STDOUT -jar /opt/service.jar "$@"
