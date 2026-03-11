# Dead Field

## 概要

テストクラスにフィールドが宣言されているが、どのテストメソッドからもセットアップメソッドからも参照されていないスメル。

## 典型的なコード例

```java
class UserServiceTest {
    private UserService service;
    private Logger logger;          // どこからも使われていない
    private static final int MAX = 100;  // 定数は対象外

    @BeforeEach
    void setUp() {
        service = new UserService();
    }

    @Test
    void testCreate() {
        User user = service.create("Alice", "alice@example.com");
        assertNotNull(user);
    }
}
```

## 何が問題か

- コードの読者に不要な情報を提供し、混乱を招く
- 以前使われていたが、リファクタリングで参照が消えた残骸の可能性がある
- テストクラスの可読性が下がる

## 修正例

使われていないフィールドを削除する:

```java
class UserServiceTest {
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService();
    }

    @Test
    void testCreate() {
        User user = service.create("Alice", "alice@example.com");
        assertNotNull(user);
    }
}
```

## 検出ルール

テストクラスのインスタンスフィールド（`static final` 定数を除く）のうち、テストメソッドおよびセットアップメソッドのいずれからも `NameExpr` で参照されていないフィールドを検出。
