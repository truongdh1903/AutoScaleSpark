FROM ubuntu:18.04

ARG NIFI_VERSION=1.13.2
ARG NIFI_TAR=nifi-${NIFI_VERSION}-bin.tar.gz
ARG NIFI_URL=https://mirror.downloadvn.com/apache/nifi/${NIFI_VERSION}/${NIFI_TAR}

LABEL Description="Nifi" \
      "Nifi Version"="$NIFI_VERSION"

WORKDIR /

USER root

# install supported tools and java-8
RUN apt-get update && apt-get install -y --no-install-recommends\
    curl \
    nano \
    openjdk-8-jdk \
    && rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
ENV PATH $PATH:$JAVA_HOME/bin

# install nifi
RUN set -eux \
    && curl -fSL "$NIFI_URL" -o /tmp/${NIFI_TAR}} \
    && tar -xvf /tmp/${NIFI_TAR}} -C /opt/ \
    && mv /opt/nifi-${NIFI_VERSION} /opt/nifi \
    && rm /tmp/${NIFI_TAR}}*
ENV PATH $PATH:/opt/nifi/bin

#install python
RUN apt-get update && apt-get install -y gcc \
    python3.8 \
    python3-pip
	
RUN pip3 install --upgrade pip \
    && pip3 install scrapy

ADD Investing-Crawler /opt/nifi
ADD conf /opt/nifi

EXPOSE 8080

ADD entrypoint.sh /entrypoint.sh

RUN chmod a+x /entrypoint.sh

ENTRYPOINT [ "/entrypoint.sh" ]
