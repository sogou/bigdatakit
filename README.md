# BigdataKit - Simplify Your Bigdata Build Process

---

BigdataKit is a tool kit to simplify your big data build process.

[http://gitlab.dev.sogou-inc.com/sogou-spark/bigdatakit](http://gitlab.dev.sogou-inc.com/sogou-spark/bigdatakit)

---

## User Guide

### Install BigdataKit

```
$ yum install bigdatakit
```

### Maven Dependency

```
<dependency>
  <groupId>com.sogou.bigdatakit</groupId>
  <artifactId>bigdatakit-sdk</artifactId>
  <version>1.1.0</version>
</dependency>
```

---

## Developer Guide

### Requirements

* JDK-1.7
* Maven

### Building TGZ

```
$ make build
```

* tgz location: bigdatakit-dist/target/bigdatakit-*-bin.tgz

### Building RPM

```
$ mvn rpm-build
```

* rpm location: bigdatakit-dist/target/rpm/bigdatakit-dist/RPMS/noarch/bigdatakit-dist-*.noarch.rpm

### Deploy To Remote Maven Repository

```
$ make maven-deploy
```

### Building Docker

```
$ make docker-build
```

### Push To Docker Registry

```
$ make docker-push
```

### Update Version

```
mvn versions:set -DnewVersion=1.1.0
```

---
