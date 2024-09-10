# Installing and Configuring MuleSoft AI Chain Connector

## Before you Begin

Ensure you have the following:

- Java Development Kit (JDK) 8, 11, or 17
- Apache Maven
- Anypoint Studio

## Download the MuleSoft AI Chain Connector

1. Download the connector:

````
git clone https://github.com/MuleSoft-AI-Chain-Project/mulesoft-ai-chain-connector.git
cd mulesoft-ai-chain-connector
````

## Install the MuleSoft AI Chain Connector

### Install the Connector with Java 8

````bash
export MAVEN_OPTS="--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.util.regex=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.xml/javax.xml.namespace=ALL-UNNAMED"
````

### Install the Connector with Java 11, 17, 21, 22

1. Set the Maven environment variable:

````bash
export MAVEN_OPTS="--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.util.regex=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.xml/javax.xml.namespace=ALL-UNNAMED"
````
2. For Java 11:
    `mvn clean install -Dmaven.test.skip=true -DskipTests -Djdeps.multiRelease=11`
 
3. For Java 17:
    `mvn clean install -Dmaven.test.skip=true -DskipTests -Djdeps.multiRelease=17`
 
4. For Java 21:
    `mvn clean install -Dmaven.test.skip=true -DskipTests -Djdeps.multiRelease=21`
 
5. For Java 22:
    `mvn clean install -Dmaven.test.skip=true -DskipTests -Djdeps.multiRelease=22`


