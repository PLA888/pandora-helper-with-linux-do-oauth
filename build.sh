docker buildx build --platform linux/amd64 -t kylsky/pandora_helper_v2:latest --load .
docker save -o pandora.tar kylsky/pandora_helper_v2:latest
#docker push kylsky/pandora_helper_v2:latest