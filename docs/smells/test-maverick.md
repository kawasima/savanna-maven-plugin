# Test Maverick

## 概要

テストクラス内の共有フィクスチャ（フィールド）をまったく使用しないテストメソッドが存在するスメル。そのメソッドは別のテストクラスに属すべき可能性がある。

## 典型的なコード例

```java
class UserServiceTest {
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService();
    }

    @Test
    void testCreateUser() {
        User user = service.create("Alice", "alice@example.com");
        assertNotNull(user);
    }

    @Test
    void testEmailValidation() {
        // service フィールドを使わない独立したテスト
        assertTrue(EmailValidator.isValid("test@example.com"));
        assertFalse(EmailValidator.isValid("invalid"));
    }
}
```

## 何が問題か

- 共有フィクスチャを使わないテストがあると、テストクラスの凝集度が下がる
- `@BeforeEach` で不要なセットアップが実行される（General Fixture と関連）
- テストの配置場所が不適切であることを示す

## 修正例

独立したテストを適切なテストクラスに移動する:

```java
class UserServiceTest {
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService();
    }

    @Test
    void testCreateUser() {
        User user = service.create("Alice", "alice@example.com");
        assertNotNull(user);
    }
}

class EmailValidatorTest {
    @Test
    void testEmailValidation() {
        assertTrue(EmailValidator.isValid("test@example.com"));
        assertFalse(EmailValidator.isValid("invalid"));
    }
}
```

## 検出ルール

`@BeforeEach` メソッドが存在するテストクラスで、クラスのインスタンスフィールドをどれも参照しないテストメソッドがある場合に検出。
