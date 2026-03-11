# Empty Test

## 概要

テストメソッドの本体が空のスメル。何も検証していないテストが存在することを示す。

## 典型的なコード例

```java
@Test
void testSomething() {
    // TODO: implement
}
```

## 何が問題か

- テストカバレッジを見かけ上水増しする（テスト数には含まれるが何も検証しない）
- 「このメソッドはテスト済み」と誤解させる
- 実装忘れのまま放置されるリスクが高い

## 修正例

テストの意図を明確にして実装する:

```java
@Test
void createUser_returnsUserWithCorrectName() {
    User user = service.create("Alice", "alice@example.com");
    assertEquals("Alice", user.getName());
}
```

実装が後回しの場合は `@Disabled` で明示する:

```java
@Disabled("TODO: implement concurrent access test")
@Test
void testConcurrentAccess() {
}
```

## 検出ルール

`@Test` が付与されたメソッドの本体（`BlockStmt`）が空の場合に検出。
