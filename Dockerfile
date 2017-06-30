FROM alpine:3.6
MAINTAINER ameizi <sxyx2008@163.com>

WORKDIR /root

RUN echo "http://mirrors.aliyun.com/alpine/v3.6/main" > /etc/apk/repositories \
    && echo "http://mirrors.aliyun.com/alpine/v3.6/community" >> /etc/apk/repositories \
    && apk update upgrade \
    && apk add --no-cache gcc g++ make tzdata openssl openssl-dev perl perl-dev libc-dev curl wget \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && curl -L https://cpanmin.us/ -o /usr/bin/cpanm \
    && chmod +x /usr/bin/cpanm \
    && cpanm -fn Encode::Locale IO::Socket::SSL Mojolicious \
    && wget -q https://github.com/sjdy521/Mojo-Webqq/archive/master.zip -OMojo-Webqq.zip \
    && unzip -qo Mojo-Webqq.zip \
    && cd Mojo-Webqq-master \
    && cpanm -fn . \
    && cd .. \
    && rm -rf Mojo-Webqq-master Mojo-Webqq.zip \
    && rm -rf /var/cache/apk/*

VOLUME /tmp

EXPOSE 5000

CMD perl -MMojo::Webqq -e 'Mojo::Webqq->new(log_encoding=>"utf8")->load(["ShowMsg","UploadQRcode"])->load("Openqq",data=>{listen=>[{port=>$ENV{MOJO_WEBQQ_PLUGIN_OPENQQ_PORT}//5000}],post_api=>$ENV{MOJO_WEBQQ_PLUGIN_OPENQQ_POST_API}})->run'