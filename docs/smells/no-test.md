# No Test

## 概要

プロジェクト全体で実行可能なテストが1つも存在しないスメル。テストファイルがない、テストメソッドが定義されていない、またはすべてのテストメソッドが `@Disabled` になっている場合に検出される。

## 典型的なコード例

### ケース1: テストファイルが存在しない

```
src/test/java/
  (空)
```

### ケース2: テストファイルはあるが @Test メソッドがない

```java
class UserServiceTest {
    // テストメソッドが1つもない
    void helperMethod() {
        // ...
    }
}
```

### ケース3: 全テストが @Disabled

```java
class UserServiceTest {
    @Disabled("TODO")
    @Test
    void testCreate() { /* ... */ }

    @Disabled("TODO")
    @Test
    void testDelete() { /* ... */ }
}
```

## 何が問題か

- プロダクションコードに対する自動テストが一切実行されない
- リグレッション（既存機能の破壊）を検知する手段がない
- CIパイプラインが「テスト成功」と報告するが、実際には何も検証していない
- コードの品質に対する信頼性がゼロ

## 修正例

テスト対象のクラスに対してテストを作成する:

```java
class UserServiceTest {
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService();
    }

    @Test
    void createUser_returnsUserWithCorrectName() {
        User user = service.create("Alice", "alice@example.com");
        assertEquals("Alice", user.getName());
    }

    @Test
    void findById_returnsNullForUnknownId() {
        assertNull(service.findById(999L));
    }
}
```

`@Disabled` テストは修正して有効化するか、Issue を作って計画的に対応する。

## 検出ルール

以下のいずれかに該当する場合にプロジェクトレベルで検出:

1. テストソースディレクトリにテストファイル（`*Test.java`, `*Tests.java`, `*IT.java`）が1つも存在しない
2. テストファイルは存在するが、`@Test` アノテーションが付与されたメソッドが1つもない
3. `@Test` メソッドは存在するが、すべて `@Disabled` で実行可能なテストが0個
