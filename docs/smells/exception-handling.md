# Exception Handling

## 概要

テストメソッド内で `try-catch` ブロックを使って例外を捕捉しているスメル。JUnit 5 の `assertThrows` を使うべき場面で手動の例外ハンドリングを行っている。

## 典型的なコード例

```java
@Test
void testInvalidUser() {
    try {
        service.create(null, "email@example.com");
        fail("Should have thrown exception");
    } catch (IllegalArgumentException e) {
        assertEquals("Name must not be null", e.getMessage());
    }
}
```

## 何が問題か

- `fail()` の呼び出しを忘れると、例外が発生しなくてもテストがパスしてしまう
- テストの意図（「この操作は例外を投げるべき」）が `try-catch` の構造に埋もれる
- `catch` ブロック内のアサーションは条件付き実行になり、Rotten Green Test に繋がる

## 修正例

```java
@Test
void testInvalidUser() {
    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> service.create(null, "email@example.com")
    );
    assertEquals("Name must not be null", ex.getMessage());
}
```

## 検出ルール

テストメソッド内に `catch` 句を持つ `try` ブロックが存在する場合に検出。`try-with-resources`（`catch` なし）は対象外。
