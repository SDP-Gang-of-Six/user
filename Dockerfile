# 指定基础镜像
FROM amazoncorretto:21.0.3

# 拷贝jdk和java项目的包
COPY ./target/user-1.0-SNAPSHOT.jar /user/user.jar

# 暴露端口
EXPOSE 8080
# 入口，java项目的启动命令
ENTRYPOINT java -server -Xms1024m -Xmx1024m -jar /user/user.jar --spring.profiles.active=pro
