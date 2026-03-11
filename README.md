# savanna-maven-plugin

[![Maven Central](https://img.shields.io/maven-central/v/net.unit8.maven.plugins/savanna-maven-plugin.svg)](https://central.sonatype.com/artifact/net.unit8.maven.plugins/savanna-maven-plugin)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/Java-11%2B-orange.svg)](https://openjdk.org/)

A Maven plugin that detects test smells in your Java project and roars like a lion.

## Get started

```xml
<plugin>
    <groupId>net.unit8.maven.plugins</groupId>
    <artifactId>savanna-maven-plugin</artifactId>
    <version>0.2.0</version>
    <executions>
        <execution>
            <goals><goal>roar</goal></goals>
        </execution>
    </executions>
</plugin>
```

## What it detects

30+ test smells including:

- **Assertion Roulette** — Multiple assertions without messages
- **Empty Test** — Test methods with no body
- **Missing Assertion** — Tests without any assertion
- **Sleepy Test** — Tests using `Thread.sleep()`
- **Conditional Test Logic** — `if`/`for`/`while` in tests
- **Magic Number Test** — Unexplained numeric literals in assertions
- **Eager Test** — One test verifying too many things at once
- **General Fixture** — Setup initializing fields not used by most tests
- And many more... See [docs/smells/](docs/smells/) for the full list.

It also detects project-level issues:

- **No Test** — No executable test methods in the project
- **Skip Testing** — Test execution is skipped (`-DskipTests`, surefire config)

## Configuration

```xml
<configuration>
    <!-- Fail the build when smells are detected (default: false) -->
    <failOnSmell>true</failOnSmell>

    <!-- Report format: console (default), json, markdown -->
    <reportFormat>console</reportFormat>

    <!-- Report output directory (for json/markdown) -->
    <reportOutputDirectory>${project.build.directory}/savanna-reports</reportOutputDirectory>

    <!-- Enable only specific smells -->
    <enabledSmells>
        <enabledSmell>EMPTY_TEST</enabledSmell>
        <enabledSmell>MISSING_ASSERTION</enabledSmell>
    </enabledSmells>

    <!-- Or disable specific smells -->
    <disabledSmells>
        <disabledSmell>EAGER_TEST</disabledSmell>
    </disabledSmells>
</configuration>
```

## The Lion

When smells are detected, a lion roars with a message in the style of the @t_wada meme:

```text
　　　　 ,、,,,、,,,
　　 _,,;' '" '' ;;,,
　　（rヽ,;''""''゛゛;,ﾉｒ）
　　 ,; i ___　、___iヽ゛;,　　テストでsleepとかお前それ@t_wadaの前でも同じ事言えんの？
　 ,;'''|ヽ・〉〈・ノ |ﾞ ';,
　 ,;''"|　 　▼　　 |ﾞ゛';,
　 ,;''　ヽ ＿人＿  /　,;'_
 ／ｼ、    ヽ  ⌒⌒  /　 ﾘ　＼
|　　"ｒ,,｀"'''ﾞ´　　,,ﾐ|
|　　 　 ﾘ、　　　　,ﾘ　　 |
|　　i 　゛ｒ、ﾉ,,ｒ" i _ |
|　　｀ー――-----------┴ ⌒´ ）
（ヽ  _____________ ,, ＿´）
 （_⌒_______________ ,, ィ
     T                 |
     |                 |
```
