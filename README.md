
# MuleSoft AI Chain Connector
![Maven Central](https://img.shields.io/maven-central/v/cloud.anypoint/mule-ai-chain-connector)


## <img src="icon/icon.svg" width="6%" alt="banner">   [MuleSoft AI Chain Connector](https://mac-project.ai/docs/mulechain-ai)

MuleSoft AI Chain is a MuleSoft custom connector (𝘣𝘢𝘴𝘦𝘥 on 𝘓𝘢𝘯𝘨𝘊𝘩𝘢𝘪𝘯4𝘫) to provide a complete framework for MuleSoft users to design, build, and manage the lifecycle of AI Agents fully in the Anypoint Platform. It is part of the MuleSoft AI Chain Project (aka MAC Project) with the overall goal to provide capabilities, examples, etc. for MuleSoft Developers.

### Requirements

- The maximum supported version for Java SDK is JDK 17. You can use JDK 17 only for running your application.
- Compilation with Java SDK must be done with JDK 8.

### Installation (using Cloud.Anypoint Dependency)

```xml
<dependency>
   <groupId>cloud.anypoint</groupId>
   <artifactId>mule-aichain-connector</artifactId>
   <version>1.0.0</version>
   <classifier>mule-plugin</classifier>
</dependency>
```

### Installation (building locally)

To use this connector, first [build and install](https://mac-project.ai/docs/mulechain-ai/getting-started) the connector into your local maven repository. 
Then add the following dependency to your application's `pom.xml`:

```xml
<dependency>
   <groupId>com.mulesoft.connectors</groupId>
   <artifactId>mule4-aichain-connector</artifactId>
   <version>{version}</version>
   <classifier>mule-plugin</classifier>
</dependency>
```

### Installation into private Anypoint Exchange

You can also make this connector available as an asset in your Anyooint Exchange.

This process will require you to build the connector as above, but additionally you will need
to make some changes to the `pom.xml`.  For this reason, we recommend you fork the repository.

Then, follow the MuleSoft [documentation](https://docs.mulesoft.com/exchange/to-publish-assets-maven) to modify and publish the asset.


### Documentation
- Check out the complete documentation on [mac-project.ai](https://mac-project.ai/docs/mulechain-ai).
- Learn from the [Getting Started YouTube Playlist](https://www.youtube.com/playlist?list=PLnuJGpEBF6ZAV1JfID1SRKN6OmGORvgv6)

---

### Stay tuned!

- 🌐 **Website**: [mac-project.ai](https://mac-project.ai)
- 📺 **YouTube**: [@MuleSoft-MAC-Project](https://www.youtube.com/@MuleSoft-MAC-Project)
- 💼 **LinkedIn**: [MAC Project Group](https://lnkd.in/gW3eZrbF)
