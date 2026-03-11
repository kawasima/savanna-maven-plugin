# Order-Dependent Test

## 概要

テストの実行順序に依存するスメル。`@TestMethodOrder` で明示的に順序を指定している場合や、テストメソッドが共有インスタンスフィールドを変更している場合に検出される。

## 典型的なコード例

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceTest {
    private int userCount = 0;

    @Test @Order(1)
    void testCreate() {
        userCount++;
        assertEquals(1, userCount);
    }

    @Test @Order(2)
    void testCreateAnother() {
        userCount++;
        assertEquals(2, userCount);  // testCreate が先に実行される前提
    }
}
```

## 何が問題か

- テストの独立性が損なわれ、1つのテストの失敗が連鎖する
- テストの並列実行ができない
- リファクタリングや新テスト追加時に既存の順序依存を壊すリスクがある
- テストが仕様のドキュメントとして機能しない（順序を理解しないと読めない）

## 修正例

各テストを独立させ、必要な状態を各テスト内またはセットアップで構築する:

```java
class UserServiceTest {
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService();
    }

    @Test
    void testCreate() {
        service.create("Alice", "alice@example.com");
        assertEquals(1, service.count());
    }

    @Test
    void testCreateMultiple() {
        service.create("Alice", "alice@example.com");
        service.create("Bob", "bob@example.com");
        assertEquals(2, service.count());
    }
}
```

## 検出ルール

以下のいずれかに該当する場合に検出:
- テストクラスに `@TestMethodOrder` アノテーションがある
- テストメソッド内でインスタンスフィールドへの代入（`AssignExpr`）がある
