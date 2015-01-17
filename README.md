cmis-upload-maven-plugin
========================

A Maven plugin that helps automating unit testing of CMIS compliant repositories.

In order to use the plugin checkout the source code and build it in a standard way:

`mvn install`

Then edit ~/.m2/settings.xml file and add following snippet:

```
    <pluginGroups>
        <pluginGroup>com.metasys</pluginGroup>
    </pluginGroups>
```

Then, in your pom.xml file add following plugin definition:

```            
      <plugin>
          <groupId>com.metasys</groupId>
          <artifactId>cmis-upload-maven-plugin</artifactId>
          <version>1.0-SNAPSHOT</version>
          <configuration>
            <localPath>/tmp/testfolder</localPath>
            <destPath>/</destPath>
            <username>admin</username>
            <password>admin</password>
            <url>http://cmis.alfresco.com/cmisatom</url>
          </configuration>
          <executions>
            <execution>
              <phase>package</phase>
              <goals>
                <goal>cmis-upload</goal>
              </goals>
            </execution>
          </executions>
      </plugin>
```

There are a few options that you can define:

* localPath - this is the path of the local folder that you want to upload to a CMIS repository. All folders and files inside of that folder will be imported to destPath location specificed below.
* destPath - this is the destination path in the repository.
* username - login to the repository.
* password - the password.
* url - the URL to the ATOM CMIS binding.
* overwrite - true/false whether to overwrite existing content or not.

