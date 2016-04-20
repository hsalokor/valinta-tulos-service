#!/bin/bash

if [[ ! -x `which http` ]]; then
  echo "You need to have httpie (http) in path."
  exit 2
fi

if [ -z $2 ]; then
  echo "Usage: $0 <username> <password> [target service URL]"
  exit 3
fi

USERNAME=$1
PASSWORD=$2
if [ -z $3 ]; then
  SERVICE=https://testi.virkailija.opintopolku.fi/valinta-tulos-service
else
  SERVICE=$3
fi

echo "Making the TGT request"
TGT_LOCATION_LINE=`http --pretty none --form --print h POST https://testi.virkailija.opintopolku.fi/cas/v1/tickets username=$USERNAME password=$PASSWORD | grep Location | cut -f 2 -d ' '`
echo "TGT_LOCATION_LINE: $TGT_LOCATION_LINE"
TGT_LOCATION=`echo ${TGT_LOCATION_LINE} | awk '{gsub(/[[:cntrl:]]/,"")}1'`

echo "Making the service ticket request to \"$TGT_LOCATION\" "

http --form --print b POST $TGT_LOCATION service=$SERVICE

