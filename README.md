# port exporter
	prometheus  tcp 端口监控 exporter.用来检查目标端口是否正常打开及建立连接

#### required:

  jdk 1.8+

#### build:

  `gradlew shadowJar`

#### run :

  `java -jar {buildName}.jar -c [config_path]`

  [exporter url](http://localhost:9333/metrics)

#### 配置文件格式(yml):
  ```
  checkInterval: 30000 #端口检查 间隔  单位 ms 默认10秒
  host: 0.0.0.0
  port: 9333
  targets:  #检查端口列表
   -
    name: name1    名称
    addr: localhost:8080
   -
    name: name2    名称
    addr: localhost:8080
  ```

#### metrics
##### port_status
   值大于0 表示该端口 监听正常






