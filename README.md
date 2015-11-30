# BigdataKit - Simplify Your Bigdata Build Process

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
  <version>1.0.0</version>
</dependency>
```

---

## Developer Guide

### Requirements

* JDK-1.7
* Maven

### Building TGZ

```
$ mvn package
```

* tgz location: bigdatakit-dist/target/bigdatakit-*-bin.tgz

### Building RPM

```
$ mvn package -Prpm
```

* rpm location: bigdatakit-dist/target/rpm/bigdatakit-dist/RPMS/noarch/bigdatakit-dist-*.noarch.rpm

### Deploy To Remote Maven Repository

```
$ mvn deploy
```

---
