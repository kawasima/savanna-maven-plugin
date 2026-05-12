# Test Run War

## 概要

テストが OS レベルの共有資源 (固定ポート番号や `/tmp` 配下の固定パスなど) をハードコードしているスメル。2つのテスト実行が同じマシンで並行すると同じ資源を取り合って衝突する。

## 典型的なコード例

```java
class ServerTest {
    @Test
    void testServer() throws Exception {
        ServerSocket s = new ServerSocket(8080);  // 固定ポート
        // ...
    }

    @Test
    void testTempFile() throws IOException {
        File f = new File("/tmp/test-data.txt");  // 共有 FS パス
        // ...
    }
}
```

## 何が問題か

- 同じテストを2つの JVM で同時に実行すると、後発がポートを取れず失敗する
- CI 上で並列実行 (Maven Surefire の `forkCount` や Gradle の `maxParallelForks` など) を有効にした瞬間に flaky になる
- `/tmp` などの共有パスは別ユーザー・別ジョブから書き換えられる可能性があり、テスト同士でなくても衝突する

## 修正例

ポートは `0` (OS が空きポートを割り当てる) を使う:

```java
class ServerTest {
    @Test
    void testServer() throws Exception {
        ServerSocket s = new ServerSocket(0);
        int port = s.getLocalPort();
        // ...
    }
}
```

ファイルは JUnit 5 の `@TempDir` を使う:

```java
class TempFileTest {
    @Test
    void testTempFile(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("test-data.txt");
        // ...
    }
}
```

## 検出ルール

- `ServerSocket` / `DatagramSocket` / `Socket` のコンストラクタに整数リテラル (0以外) が渡されている場合
- `File` / `FileInputStream` / `FileOutputStream` / `FileReader` / `FileWriter` / `RandomAccessFile` のコンストラクタに `/tmp`、`/var`、`/dev`、`C:\Temp`、`C:\Windows\Temp` 配下を指す文字列リテラルが渡されている場合

なお `static` 非 `final` フィールドによるテスト間干渉は、別スメル「Order Dependent Test」が検出する。
