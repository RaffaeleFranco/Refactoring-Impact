# Refactoring Impact [![Build Status](https://github.com/muesli/markscribe/workflows/build/badge.svg)](https://github.com/RaffaeleFranco/Refactoring-Impact/actions)
This repository contains source code of a toolchain developed for a Unisannio exam for the analysis of refactoring on code smells and influence on technical debt in Java software repositories.
The objective of the study is to evaluate the impact of refactoring on code smells and calculate the technical debt by analyzing various open source systems written in Java.
In particular, for each code smell removed, the technical debt relating to the smell before and after the refactoring is considered, in order to calculate the difference and establish whether the technical debt is improved, pejorative or stable.
### Requirements
<ul>
<li>Java project repository to analyzer</li>
<li><a href="https://github.com/gradle/gradle">Gradle</a> for dependences and build automation</li>
<li><a href="https://github.com/tushartushar/DesigniteJava">DesigniteJava</a> community version for code smells detection</li>
<li><a href="https://github.com/SonarSource/sonarqube">SonarQube</a> and <a href="https://github.com/SonarSource/sonar-scanner-cli">SonarScanner</a> to calculate technical debt</li>
</ul>

### Getting Started
Clone the project:
```
git clone https://github.com/raffaelefranco/refactoring-impact
```
Import project into IDE (e.g. Eclipse or Intellij) and download Gradle dependences.

### Configurations
There is a file for configurations into `src/main/resources` where already exists a template called `application-template.conf`.

__A new configuration file can be created (by copying one of those already present)
 and renaming it as you want (e.g. `application.conf`).__
 
