from registry.docker.dev.sogou-inc.com:5000/clouddev/spark-client-sunshine-nm:1.5.3
RUN yum clean all && yum install java-1.8.0-oracle-devel.x86_64 -y && yum clean all
RUN alternatives --set javac /usr/lib/jvm/java-1.7.0-oracle.x86_64/bin/javac
RUN (rm -fr /opt/phoenix && curl -fssL http://gitlab.dev.sogou-inc.com/sogou-spark/phoenix-config/raw/master/install.sh|bash)
RUN yum clean all && yum install bigdatakit.noarch -y && yum clean all
