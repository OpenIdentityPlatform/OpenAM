echo "Stopping docker containers..."
docker stop openam-idp openam-sp

echo "Removing network..."
docker network rm openam-saml
echo "Finished"