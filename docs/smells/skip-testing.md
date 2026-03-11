# Skip Testing

## 概要

プロジェクトのテスト実行がスキップされているスメル。`-DskipTests`、`-Dmaven.test.skip=true`、または `maven-surefire-plugin` の設定で `<skipTests>true</skipTests>` が指定されている場合に検出される。

## 典型的なコード例

### pom.xml での設定

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <skipTests>true</skipTests>  <!-- テスト実行をスキップ -->
    </configuration>
</plugin>
```

### コマンドラインでの指定

```bash
mvn package -DskipTests
mvn package -Dmaven.test.skip=true
```

## 何が問題か

- テストが書かれていても実行されないため、リグレッションを検知できない
- 「一時的にスキップ」のつもりが恒久的にスキップされたまま放置される
- CIパイプラインで `mvn package -DskipTests` が常態化すると、テストの存在意義がなくなる
- テストの失敗を修正せずにスキップで回避するのは技術的負債の蓄積

## 修正例

テストをスキップしている原因を特定して修正する:

1. **テストが遅い場合**: テストを高速化する（インメモリDB、モック化、並列実行）
2. **テストが不安定な場合**: Flaky Test を修正する
3. **テストがビルドを壊す場合**: テストを修正する（スキップではなく）
4. **CIの一部フェーズでスキップしたい場合**: プロファイルで制御し、テスト実行フェーズは必ず含める

```xml
<!-- プロファイルで制御する場合 -->
<profile>
    <id>quick-build</id>
    <properties>
        <skipTests>true</skipTests>
    </properties>
</profile>
<!-- CI では常にテスト実行 -->
<!-- mvn verify -P !quick-build -->
```

## 検出ルール

以下のいずれかに該当する場合にプロジェクトレベルで検出:

1. `maven-surefire-plugin` の `<configuration>` に `<skipTests>true</skipTests>` が設定されている
2. システムプロパティ `skipTests` が設定されている（`-DskipTests`）
3. システムプロパティ `maven.test.skip` が `true` に設定されている
