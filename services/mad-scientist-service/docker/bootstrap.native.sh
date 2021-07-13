#!/bin/sh
echo 'Executing via native...'
/service.native -Dorg.apache.commons.logging.diagnostics.dest=STDOUT "$@"
