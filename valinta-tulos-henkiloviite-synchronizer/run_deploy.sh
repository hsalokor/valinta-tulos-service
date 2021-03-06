#!/bin/bash
set -e
shopt -s nullglob

SERVICE=valinta-tulos-henkiloviite-synchronizer
BRANCH=master
DEPLOY_ENVIRONMENT=$1
ZIP_PATH=( ./target/${SERVICE}_${BRANCH}_*.zip )
[ ${#ZIP_PATH[@]} -lt 1 ] && { printf "No deploy zip found\n"; exit 1; }
[ ${#ZIP_PATH[@]} -gt 1 ] && { printf "Multiple deploy zips found: %s\n" "${ZIP_PATH[*]}"; exit 1; }
ZIP=$(basename ${ZIP_PATH[0]})
KEY_FILE=/home/bamboo/.ssh/id_rsa
DEPLOY_USER=bamboo
DEPLOY_HOST=liikuntasali.hard.ware.fi
DEPLOY_DIR=/data00/releases/${SERVICE}/${BRANCH}/

scp -C -i "${KEY_FILE}" "${ZIP_PATH}" "${DEPLOY_USER}@${DEPLOY_HOST}:${DEPLOY_DIR}"

DEPLOY_CMD="ssh deploy@deploy.oph.ware.fi \"/home/deploy/deploy-jar.sh \"${SERVICE}\" \"${BRANCH}\" \"${DEPLOY_ENVIRONMENT}\" true \"${ZIP}\"\""
ssh -i "${KEY_FILE}" "${DEPLOY_USER}@${DEPLOY_HOST}" "${DEPLOY_CMD}"
