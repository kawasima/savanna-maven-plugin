# Resource Leakage

## 概要

テスト内で `Closeable` / `AutoCloseable` なリソースを生成しているが、`try-with-resources` で適切にクローズしていないスメル。

## 典型的なコード例

```java
@Test
void testReadFile() throws IOException {
    FileInputStream fis = new FileInputStream("test.txt");
    BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
    String line = reader.readLine();
    assertEquals("hello", line);
    // reader も fis もクローズされない
}
```

## 何が問題か

- テスト実行後にファイルハンドルやコネクションがリークする
- テストスイート全体の実行中にリソースが枯渇する可能性がある（特にDBコネクション）
- テスト失敗時（例外発生時）にクリーンアップが行われない
- OS レベルのリソースリークは他のテストに影響する

## 修正例

`try-with-resources` を使う:

```java
@Test
void testReadFile() throws IOException {
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new FileInputStream("test.txt")))) {
        String line = reader.readLine();
        assertEquals("hello", line);
    }
}
```

JUnit 5 の `@TempDir` と NIO を使う:

```java
@Test
void testReadFile(@TempDir Path tempDir) throws IOException {
    Path file = tempDir.resolve("test.txt");
    Files.writeString(file, "hello");
    assertEquals("hello", Files.readString(file));
}
```

## 検出ルール

テストメソッド内で `FileInputStream`, `FileOutputStream`, `BufferedReader`, `BufferedWriter`, `Connection`, `PreparedStatement`, `ResultSet`, `Socket` 等のインスタンスを生成しているが、`try-with-resources` ブロック内にない場合に検出。
