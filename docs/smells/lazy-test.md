# Lazy Test

## 概要

3つ以上のテストメソッドが同一のプロダクションメソッドを呼び出しているスメル。テストの観点が重複している可能性を示す。

## 典型的なコード例

```java
class UserServiceTest {
    @Test
    void testCreateReturnsUser() {
        User user = service.create("Alice", "alice@example.com");
        assertNotNull(user);
    }

    @Test
    void testCreateSetsName() {
        User user = service.create("Alice", "alice@example.com");
        assertEquals("Alice", user.getName());
    }

    @Test
    void testCreateSetsEmail() {
        User user = service.create("Alice", "alice@example.com");
        assertEquals("alice@example.com", user.getEmail());
    }
}
```

## 何が問題か

- 同じメソッドを何度も呼び出すテストはセットアップの重複が多く、保守コストが高い
- プロダクションメソッドの仕様変更時に、似たようなテストを何箇所も修正する必要がある
- テストの実行時間が不要に長くなる

## 修正例

関連するアサーションを1つのテストにまとめる:

```java
@Test
void testCreateUser() {
    User user = service.create("Alice", "alice@example.com");
    assertNotNull(user, "should return non-null user");
    assertEquals("Alice", user.getName(), "name should match");
    assertEquals("alice@example.com", user.getEmail(), "email should match");
}
```

異なる入力パターンをテストする場合は `@ParameterizedTest` を使う:

```java
@ParameterizedTest
@CsvSource({"Alice,alice@example.com", "Bob,bob@example.com"})
void testCreate(String name, String email) {
    User user = service.create(name, email);
    assertEquals(name, user.getName());
}
```

## 検出ルール

同一の `scope.method()` 呼び出しが3つ以上のテストメソッドから行われている場合に検出（ヒューリスティック）。アサーション系・モック系メソッドは対象外。
