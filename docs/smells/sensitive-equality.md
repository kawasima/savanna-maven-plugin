# Sensitive Equality

## 概要

アサーション内で `toString()` の戻り値を使って等価比較を行っているスメル。オブジェクトの文字列表現に依存する脆い比較。

## 典型的なコード例

```java
@Test
void testUser() {
    User user = service.create("Alice", "alice@example.com");
    assertEquals(
        "User{id=1, name='Alice', email='alice@example.com'}",
        user.toString()
    );
}
```

## 何が問題か

- `toString()` の出力形式はドキュメント化されない実装詳細であり、予告なく変更される可能性がある
- フィールドの追加・並び替えでテストが壊れる
- ロケール依存の書式（日付、数値）で環境ごとに結果が異なる可能性がある
- 本来検証すべきフィールドが文字列に埋もれて見えにくい

## 修正例

個別のフィールドを直接検証する:

```java
@Test
void testUser() {
    User user = service.create("Alice", "alice@example.com");
    assertEquals(1, user.getId());
    assertEquals("Alice", user.getName());
    assertEquals("alice@example.com", user.getEmail());
}
```

AssertJ を使えばより表現豊かに書ける:

```java
@Test
void testUser() {
    User user = service.create("Alice", "alice@example.com");
    assertThat(user)
        .extracting(User::getName, User::getEmail)
        .containsExactly("Alice", "alice@example.com");
}
```

## 検出ルール

`assertEquals` / `assertNotEquals` の引数内で `toString()` メソッド呼び出しがある場合に検出。
