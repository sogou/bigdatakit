PROJECT_VERSION=$(shell cat pom.xml  | grep '<version>' | head -n 1 | awk -F'<version>' '{print $$2}' | awk -F'</version>' '{print $$1}')

IMAGE_MAIN_NAME='clouddev/bigdatakit'
IMAGE_VERSION=$(PROJECT_VERSION)
IMAGE=$(IMAGE_MAIN_NAME):$(IMAGE_VERSION)

REGISTRY='registry.docker.dev.sogou-inc.com:5000'

ifdef NO_CACHE
	BUILD_PARAM='--no-cache=true'
else
	BUILD_PARAM=
endif

all: build

clean:
	mvn clean

build:
	mvn package

rpm-build:
	mvn package -Prpm

maven-deploy: build
	mvn deploy

docker-build:
	docker build $(BUILD_PARAM) -t $(IMAGE_MAIN_NAME) .
	docker tag -f $(IMAGE_MAIN_NAME) $(REGISTRY)/$(IMAGE)

docker-push: docker-build
	docker push $(REGISTRY)/$(IMAGE)
