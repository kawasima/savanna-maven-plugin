# Assertion Roulette

## 概要

1つのテストメソッド内に、説明メッセージのないアサーションが複数存在するスメル。テストが失敗した際に、どのアサーションが原因か特定しにくくなる。

## 典型的なコード例

```java
@Test
void testCreateUser() {
    User user = service.create("Alice", "alice@example.com");
    assertNotNull(user);           // どれが失敗した？
    assertEquals(1, user.getId()); // どれが失敗した？
    assertEquals("Alice", user.getName());
    assertEquals("alice@example.com", user.getEmail());
}
```

## 何が問題か

- テスト失敗時に「expected \<1\> but was \<0\>」のようなメッセージしか出ず、4つのアサーションのどれが失敗したか行番号を見るまでわからない
- CI環境では行番号が見づらいことが多く、原因特定に時間がかかる
- アサーションが増えるほど問題が深刻化する

## 修正例

```java
@Test
void testCreateUser() {
    User user = service.create("Alice", "alice@example.com");
    assertNotNull(user, "create should return non-null user");
    assertEquals(1, user.getId(), "first user should have id 1");
    assertEquals("Alice", user.getName(), "name should match input");
    assertEquals("alice@example.com", user.getEmail(), "email should match input");
}
```

AssertJを使う場合は `as()` でメッセージを付与する:

```java
@Test
void testCreateUser() {
    User user = service.create("Alice", "alice@example.com");
    assertThat(user).as("created user").isNotNull();
    assertThat(user.getId()).as("user id").isEqualTo(1);
    assertThat(user.getName()).as("user name").isEqualTo("Alice");
}
```

## 検出ルール

- JUnit 5: メッセージ引数なしのアサーション（`assertEquals(a, b)` の2引数形式）が2つ以上ある場合
- AssertJ: `as()` / `describedAs()` のないアサーションチェーンが2つ以上ある場合
