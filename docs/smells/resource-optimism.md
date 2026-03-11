# Resource Optimism

## 概要

テスト内でファイルやリソースにアクセスする際、その存在を確認せずに楽観的にアクセスしているスメル。

## 典型的なコード例

```java
@Test
void testReadConfig() {
    File configFile = new File("config/settings.properties");
    Properties props = new Properties();
    props.load(new FileInputStream(configFile));  // ファイルが存在しなければ例外
    assertEquals("test", props.getProperty("env"));
}
```

## 何が問題か

- ファイルが存在しない環境ではテストが `FileNotFoundException` で失敗する
- テストがローカル環境に依存し、CIや他の開発者の環境で再現性がない
- エラーメッセージが「ファイルが見つかりません」となり、テストの本来の失敗理由がわかりにくい

## 修正例

テストリソースとして管理し、クラスパスから読む:

```java
@Test
void testReadConfig() {
    InputStream is = getClass().getResourceAsStream("/settings.properties");
    assertNotNull(is, "test resource should exist");
    Properties props = new Properties();
    props.load(is);
    assertEquals("test", props.getProperty("env"));
}
```

一時ファイルを使う場合は `@TempDir` を活用する:

```java
@Test
void testWriteAndRead(@TempDir Path tempDir) throws IOException {
    Path file = tempDir.resolve("output.txt");
    Files.writeString(file, "hello");
    assertEquals("hello", Files.readString(file));
}
```

## 検出ルール

テストメソッド内で `File`, `FileInputStream`, `FileOutputStream`, `FileReader`, `FileWriter`, `RandomAccessFile` のインスタンスを生成しているが、同一メソッド内で `exists()`, `isFile()`, `isDirectory()` の呼び出しがない場合に検出。
