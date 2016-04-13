#!/bin/bash

if [[ ! -x `which http` ]]; then
  echo "You need to have httpie (http) in path."
  exit 2
fi

if [ -z $2 ]; then
  echo "Usage: $0 <username> <password>"
  exit 3
fi

USERNAME=$1
PASSWORD=$2

echo "Making the TGT request"
TGT_LOCATION_LINE=`http --pretty none --form --print h POST https://testi.virkailija.opintopolku.fi/cas/v1/tickets username=$USERNAME password=$PASSWORD | grep Location | cut -f 2 -d ' '`
TGT_LOCATION=${TGT_LOCATION_LINE::-1}

echo "Making the service ticket request to \"$TGT_LOCATION\" "

http --form --print b POST $TGT_LOCATION service=https://testi.virkailija.opintopolku.fi/valinta-tulos-service

