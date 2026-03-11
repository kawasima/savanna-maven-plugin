# Mystery Guest

## 概要

テストメソッドがファイルシステム、データベース、ネットワークなどの外部リソースに直接アクセスしているスメル。テストの実行結果が外部環境に依存する。

## 典型的なコード例

```java
@Test
void testLoadConfig() {
    File configFile = new File("/etc/app/config.properties");
    Properties props = new Properties();
    props.load(new FileInputStream(configFile));
    assertEquals("production", props.getProperty("env"));
}
```

```java
@Test
void testFetchUser() {
    Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/testdb");
    // ...
}
```

## 何が問題か

- 外部リソースが存在しない環境（CI、別の開発マシン等）ではテストが失敗する
- テストの実行順序やタイミングで結果が変わる可能性がある
- テストが遅くなる（ファイルI/O、ネットワーク通信）
- テストの独立性が損なわれる

## 修正例

テストデータをインラインで定義する:

```java
@Test
void testLoadConfig() {
    Properties props = new Properties();
    props.setProperty("env", "production");
    assertEquals("production", props.getProperty("env"));
}
```

外部リソースが必要な場合はテストリソースとして管理する:

```java
@Test
void testLoadConfig() {
    InputStream is = getClass().getResourceAsStream("/test-config.properties");
    Properties props = new Properties();
    props.load(is);
    assertEquals("test", props.getProperty("env"));
}
```

データベースにはインメモリDB（H2等）を使う:

```java
@BeforeEach
void setUp() {
    dataSource = new EmbeddedDatabaseBuilder()
        .setType(EmbeddedDatabaseType.H2)
        .addScript("schema.sql")
        .build();
}
```

## 検出ルール

テストメソッド内で `File`, `FileInputStream`, `FileOutputStream`, `Socket`, `URL`, `HttpURLConnection`, `Connection` 等の I/O 系クラスのインスタンスを生成するか、`DriverManager.getConnection()` / `Files.*` を呼び出す場合に検出。
