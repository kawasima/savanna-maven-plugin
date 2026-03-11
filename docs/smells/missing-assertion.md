# Missing Assertion

## 概要

テストメソッド内にアサーションが1つも存在しないスメル。「Unknown Test」や「Assertionless Test」とも呼ばれる。プロダクションコードを実行するだけで、結果を検証していない。

## 典型的なコード例

```java
@Test
void testCreateUser() {
    User user = service.create("Alice", "alice@example.com");
    user.getName();  // 呼び出すだけ、検証なし
}
```

```java
@Test
void testProcess() {
    service.process(input);
    // 何もアサートしない。「例外が出なければOK」という意図？
}
```

## 何が問題か

- テストが何も検証していないため、プロダクションコードが壊れても検知できない
- 「テストが通っている」＝「正しく動作している」という誤った安心感を与える
- コードカバレッジだけが上がり、実際のテスト品質は低い

## 修正例

戻り値を検証する:

```java
@Test
void testCreateUser() {
    User user = service.create("Alice", "alice@example.com");
    assertNotNull(user);
    assertEquals("Alice", user.getName());
}
```

副作用を検証する:

```java
@Test
void testProcess() {
    service.process(input);
    assertEquals(1, repository.count(), "should have persisted one record");
}
```

例外が出ないことを確認したい場合は `assertDoesNotThrow` を使う:

```java
@Test
void testProcessDoesNotThrow() {
    assertDoesNotThrow(() -> service.process(input));
}
```

## 検出ルール

テストメソッド内に JUnit 5 / AssertJ / Hamcrest のアサーションメソッド呼び出しが1つもない場合に検出。`fail()`, `assertThrows()`, `assertDoesNotThrow()` もアサーションとしてカウント。
