FROM ubuntu:18.04

ARG HADOOP_VERSION=3.2.2
ARG HADOOP_TAR=hadoop-$HADOOP_VERSION.tar.gz
ARG HADOOP_URL=https://www.apache.org/dist/hadoop/common/hadoop-$HADOOP_VERSION/${HADOOP_TAR}

LABEL Description="Hadoop Dev" \
    "Hadoop Version"="$HADOOP_VERSION"

WORKDIR /

USER root

# install supported tools
RUN apt-get update && apt-get install -y --no-install-recommends\
    curl \
    net-tools \
    nano \
    ssh \
    && rm -rf /var/lib/apt/lists/*

# install java-8
RUN apt-get update && apt-get install -y openjdk-8-jdk
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
ENV PATH $PATH:$JAVA_HOME/bin

# install hadoop
RUN set -x \
    && curl -fSL "$HADOOP_URL" -o /tmp/${HADOOP_TAR} \
    && tar -xvf /tmp/${HADOOP_TAR} -C /opt/ \
    && rm /tmp/${HADOOP_TAR}*

RUN mv /opt/hadoop-${HADOOP_VERSION} /opt/hadoop \
    && ln -s /opt/hadoop/etc/hadoop /etc/hadoop \
    && mkdir /opt/hadoop/logs \
    && mkdir -p /dfs/data /dfs/name

# passwordless ssh
ENV PDSH_RCMD_TYPE=ssh
RUN rm -f /root/.ssh/id_rsa
RUN ssh-keygen -q -N "" -t rsa -f /root/.ssh/id_rsa
RUN cp /root/.ssh/id_rsa.pub /root/.ssh/authorized_keys
RUN chmod 0600 ~/.ssh/authorized_keys
RUN /etc/init.d/ssh start
# set hadoop enviroment
ENV HADOOP_HOME=/opt/hadoop
ENV PATH $HADOOP_HOME/bin/:$HADOOP_HOME/sbin/:$PATH
ENV HDFS_NAMENODE_USER=root
ENV HDFS_DATANODE_USER=root
ENV HDFS_SECONDARYNAMENODE_USER=root
ENV YARN_RESOURCEMANAGER_USER=root
ENV YARN_NODEMANAGER_USER=root
# add JAVA_HOME to hadoop .env
RUN sed -i '/^# export JAVA_HOME/ s:.*:export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64:' $HADOOP_HOME/etc/hadoop/hadoop-env.sh

# add config
ADD conf/core-site.xml $HADOOP_HOME/etc/hadoop/
ADD conf/hdfs-site.xml $HADOOP_HOME/etc/hadoop/
ADD conf/yarn-site.xml $HADOOP_HOME/etc/hadoop/
ADD conf/mapred-site.xml $HADOOP_HOME/etc/hadoop/
ADD conf/workers $HADOOP_HOME/etc/hadoop/

RUN sed -i ~/.profile -e 's/mesg n || true/tty -s \&\& mesg n/g'

EXPOSE  50070 50075 8042 8088 9000 9870

ADD entrypoint.sh /entrypoint.sh

RUN chmod a+x /entrypoint.sh

ENTRYPOINT [ "/entrypoint.sh" ]
