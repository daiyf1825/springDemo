#!/usr/bin/env bash
echo '====compile and package source======'
mvn clean package -Dmaven.test.skip

echo 'upload...'
scp  target/sanya-admin-1.0.0-SNAPSHOT.jar sanyaAdmin@115.29.214.59:/home/sanyaAdmin/builds/sanya-admin.jar
