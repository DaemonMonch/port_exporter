# port exporter
prometheus  tcp 端口监控 exporter 基于 vertx

build:
  gradlew shadowJar
run :
  java -jar {buildName}.jar -c [config_path]




