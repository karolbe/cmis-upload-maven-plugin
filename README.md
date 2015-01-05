cmis-upload-maven-plugin
========================

A Maven plugin that helps automating unit testing of CMIS compliant repositories.

In order to use the plugin checkout the source code, and build it using

`mvn install`

Then edit ~/.m2/settings.xml file and add to it:

```
    <pluginGroups>
        <pluginGroup>com.metasys</pluginGroup>
    </pluginGroups>
```

Then in your pom.xml file add following plugin definition:

```            
      <plugin>
          <groupId>com.metasys</groupId>
          <artifactId>cmis-upload-maven-plugin</artifactId>
          <version>1.0-SNAPSHOT</version>
          <configuration>
            <localPath>/tmp/testfolder</localPath>
            <skipPathComponents>1</skipPathComponents>
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

* localPath - this is the path of the local folder that you want to upload to a CMIS repository
* destPath - this is the path in the repository
* skippathComponents - how many components of the local path should be skipped, for example, you have your files in
/home/kbryd/work/someproject/data and under that path you have a few levels of folders with files, obviously you
don't want to create such long paths in the repository, so you can set skipPathComponents to 4 so it will skip
/home/kbryd/work/somproject and only data folder will be created.
* username - login to the repository
* password - the password
* url - the URL to the ATOM CMIS binding.