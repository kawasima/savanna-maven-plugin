# Redundant Print

## 概要

テストメソッド内で `System.out.println()` や `System.err.println()` を使ってデバッグ出力を行っているスメル。

## 典型的なコード例

```java
@Test
void testFindUser() {
    User user = service.create("Alice", "alice@example.com");
    User found = service.findById(user.getId());
    System.out.println("Found user: " + found);  // デバッグ用の出力
    assertNotNull(found);
}
```

## 何が問題か

- テスト実行のたびにコンソールにノイズが出力される
- CIのログが肥大化する
- デバッグ時に一時的に追加したものが放置されている可能性が高い
- テストの検証はアサーションで行うべきであり、目視確認に頼るべきではない

## 修正例

デバッグ出力を削除し、必要な検証はアサーションで行う:

```java
@Test
void testFindUser() {
    User user = service.create("Alice", "alice@example.com");
    User found = service.findById(user.getId());
    assertNotNull(found);
    assertEquals("Alice", found.getName());
}
```

ログ出力をテストしたい場合は、ログフレームワークのテスト機能を使う:

```java
@Test
void testLogging(@LogCapture LogCollector logs) {
    service.create("Alice", "alice@example.com");
    assertThat(logs).contains("User created: Alice");
}
```

## 検出ルール

テストメソッド内で `System.out` または `System.err` に対する `print*` / `write` メソッド呼び出しがある場合に検出。
