# demo
git clone https://github.com/withthelightout/demo.git
mvn install
cd server/target
JAVA_HOME/bin/java -jar server-0.0.1-SNAPSHOT.jar

同时再开一个命令行窗口（可以选在另一台电脑上 要求在局域网,外网需要手动修改源码或者配置）
cd client 
JAVA_HOME/bin/java  -jar client-0.0.1-SNAPSHOT-exec.jar

客户端可以发送消息 服务端可以接收到
同时服务端也可以广播客户端也可以接收到 

