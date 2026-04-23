FROM maven:3.9-amazoncorretto-21
# 添加jar到镜像并命名为user.jar
ADD yu-picture-backend-0.0.1-SNAPSHOT.jar yu-picture-backend.jar
# 镜像启动后暴露的端口
EXPOSE 8124
# jar运行命令，参数使用逗号隔开
ENTRYPOINT ["java","--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar","yu-picture-backend.jar","--spring.profiles.active=prod"]