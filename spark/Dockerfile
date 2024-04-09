FROM ubuntu:18.04

ARG SPARK_VERSION=3.1.1
ARG HADOOP_VERSION=3.2
ARG ZEPPELIN_VERSION=0.9.0
ARG SPARK_TAR=spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz
ARG SPARK_URL=https://mirror.downloadvn.com/apache/spark/spark-${SPARK_VERSION}/${SPARK_TAR}
ARG ZEPPELIN_TAR=zeppelin-${ZEPPELIN_VERSION}-bin-all.tgz
ARG ZEPPELIN_URL=https://mirror.downloadvn.com/apache/zeppelin/zeppelin-${ZEPPELIN_VERSION}/${ZEPPELIN_TAR}

LABEL Description="Apache Spark" \
    "Spark Version"="$SPARK_VERSION"

WORKDIR /

USER root

# install supported tools and java-8
RUN apt-get update && apt-get install -y --no-install-recommends\
    curl \
    nano \
    ssh \
    openjdk-8-jdk \
    && rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
ENV PATH $PATH:$JAVA_HOME/bin

# passwordless ssh
ENV PDSH_RCMD_TYPE=ssh
RUN rm -f /root/.ssh/id_rsa
RUN ssh-keygen -q -N "" -t rsa -f /root/.ssh/id_rsa
RUN cp /root/.ssh/id_rsa.pub /root/.ssh/authorized_keys
RUN chmod 0600 ~/.ssh/authorized_keys
RUN /etc/init.d/ssh start

RUN set -x \
    && curl -fSL "$SPARK_URL" -o /tmp/${SPARK_TAR} \
    && tar -xvf /tmp/${SPARK_TAR} -C /opt/ \
    && rm /tmp/${SPARK_TAR}*

ENV PATH $PATH:/opt/spark/bin

#install python
RUN apt-get update && apt-get install -y gcc \
    python3.8 \
    python3-pip

RUN pip3 install --upgrade pip \
    && pip3 install pyspark

RUN mv /opt/spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION} /opt/spark
ADD conf_hdfs /opt/spark/conf

EXPOSE 4040 7077 8080 8081 18080 34047

ADD entrypoint.sh /entrypoint.sh
RUN chmod a+x /entrypoint.sh
ENTRYPOINT [ "/entrypoint.sh" ]
