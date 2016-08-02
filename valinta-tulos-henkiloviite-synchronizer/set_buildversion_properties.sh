#!/bin/bash
set -e
shopt -s nullglob

PROPERTIES=$1
VERSION=$2
BRANCH=$(git rev-parse --abbrev-ref HEAD | sed 's+/+-+g')
COMMIT=$(git rev-parse HEAD)
TIMESTAMP=$(date "+%FT%T%:z")

set_property() {
    sed -E -i.bak "s/($1)=.*/\1=$2/" "${PROPERTIES}"
    rm "${PROPERTIES}.bak"
    if ! grep -q "$1" "${PROPERTIES}"
    then
        ENTRY=`printf "$1=%s\n" "$2"`
        echo "$(basename $0): Writing $ENTRY to $PROPERTIES"
        echo "$ENTRY" >> "${PROPERTIES}"
    else
        echo "$(basename $0): Skipping $ENTRY already in $PROPERTIES"
    fi
}

if [ -e "${PROPERTIES}" ]
then
    set_property "henkiloviite.buildversion.version" "${VERSION}"
    set_property "henkiloviite.buildversion.branch" "${BRANCH}"
    set_property "henkiloviite.buildversion.commit" "${COMMIT}"
    set_property "henkiloviite.buildversion.timestamp" "${TIMESTAMP}"
fi
