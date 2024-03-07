#Build the Maven project
FROM maven:3.9.6-amazoncorretto-21-al2023 as builder
COPY . /usr/src/app
WORKDIR /usr/src/app
RUN mvn clean install

#Build the Tomcat container
FROM tomcat:jre21
#RUN apk add openssh \
#     && echo "root:Docker!" | chpasswd
#
## Copy the sshd_config file to the /etc/ssh/ directory
#COPY sshd_config /etc/ssh/
#
## Copy and configure the ssh_setup file
#RUN mkdir -p /tmp
#COPY ssh_setup.sh /tmp
#RUN chmod +x /tmp/ssh_setup.sh \
#    && (sleep 1;/tmp/ssh_setup.sh 2>&1 > /dev/null) \

RUN apt update
RUN apt install openssh-server -y \
     && echo "root:Docker!" | chpasswd

# Copy the sshd_config file to the /etc/ssh/ directory
COPY sshd_config /etc/ssh/

# Copy and configure the ssh_setup file
RUN mkdir -p /tmp
COPY ssh_setup.sh /tmp
RUN chmod +x /tmp/ssh_setup.sh \
    && (sleep 1;/tmp/ssh_setup.sh 2>&1 > /dev/null)

# Copy war file to webapps.
COPY --from=builder /usr/src/app/registry-fhir-server/target/registry-fhir-server.war $CATALINA_HOME/webapps/ROOT.war

EXPOSE 8080 2222

CMD ["sh", "-c", "catalina.sh run ; /usr/sbin/sshd"]
