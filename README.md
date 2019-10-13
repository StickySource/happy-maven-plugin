# happy-maven-plugin

A plugin to validate if an environment is happy or at least will make the developers happy.

Read the docs here https://stickycode.readthedocs.io/en/latest/plugins/happy.html

## Happy Versions

This plugin stores and validates a collection of version urls for application(s)

The file is stored in a jar at META-INF/sticky/happy-versions

The format is `context path:expected version` e.g. `/version:application-1.2`

## Collect the happy versions

First you need to collect the metadata, so in a given application

```
<plugin>
  <groupId>net.stickycode.plugins</groupId>
  <artifactId>happy-maven-plugin</artifactId>
  <version>1.2</version>
  <executions>
    <execution>
      <phase>test</phase>
      <goals>
        <goal>collect</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

This will write a file in META-INF/sticky/happy-versions with a format `context path:expected version` e.g. 

## To validate

This will collect all the versions in the resolved classpath for the project and check the version url for them:
```
<plugin>
  <groupId>net.stickycode.plugins</groupId>
  <artifactId>happy-maven-plugin</artifactId>
  <version>@pom.version@</version>
  <executions>
    <execution>
      <id>collect</id>
      <phase>compile</phase>
      <goals>
        <goal>collect</goal>
      </goals>
    </execution>

    <execution>
      <id>validate</id>
      <phase>test</phase>
      <goals>
        <goal>validate</goal>
      </goals>
      <configuration>
        <failBuild>false</failBuild>
        <retryDurationSeconds>4</retryDurationSeconds>
        <retryPeriodSeconds>1</retryPeriodSeconds>
      </configuration>
    </execution>
  </executions>
</plugin>
```
