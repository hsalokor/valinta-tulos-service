#!/bin/bash
set -e

SERVICE=valinta-tulos-henkiloviite-synchronizer
BRANCH=master
DEPLOY_ENVIRONMENT=$1
ZIP_PATH=${PWD}/target/${SERVICE}_${BRANCH}_*.zip
ZIP=$(basename ${ZIP_PATH})
KEY_FILE=/home/bamboo/.ssh/id_rsa
DEPLOY_USER=bamboo
DEPLOY_HOST=liikuntasali.hard.ware.fi
DEPLOY_DIR=/data00/releases/${SERVICE}/${BRANCH}/

scp -C -i "${KEY_FILE}" "${ZIP_PATH}" "${DEPLOY_USER}@${DEPLOY_HOST}:${DEPLOY_DIR}"

DEPLOY_CMD="ssh deploy@deploy.oph.ware.fi \"/home/deploy/deploy-jar.sh \"${SERVICE}\" \"${BRANCH}\" \"${DEPLOY_ENVIRONMENT}\" true \"${ZIP}\"\""
ssh -i "${KEY_FILE}" "${DEPLOY_USER}@${DEPLOY_HOST}" "${DEPLOY_CMD}"
