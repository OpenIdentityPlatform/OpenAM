# How-to:
Build docker image:

    docker build . -t openidentityplatform/openam

Run image

    docker run -h openam-01.domain.com -p 8080:8080 --name openam-01 openidentityplatform/openam
