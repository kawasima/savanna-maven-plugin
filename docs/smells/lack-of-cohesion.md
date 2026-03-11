# Lack of Cohesion of Test Cases

## 概要

テストクラス内のテストメソッドが互いに無関係なオブジェクトを操作しており、クラスとしての凝集度が低いスメル。テストクラスが複数の責務を持っている。

## 典型的なコード例

```java
class MiscTest {
    @Test
    void testUserCreation() {
        userService.create("Alice", "alice@example.com");
        assertNotNull(userService.findById(1L));
    }

    @Test
    void testOrderCalculation() {
        orderService.placeOrder("Widget", 1, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("10.00"), orderService.calculateTotal());
    }

    @Test
    void testEmailSending() {
        emailService.send("test@example.com", "Hello");
        assertTrue(emailService.isSent());
    }
}
```

## 何が問題か

- テストクラスから何をテストしているか読み取れない
- テストメソッド間でフィクスチャを共有できず、各テストが独立したセットアップを必要とする
- クラスが肥大化しやすい
- テスト対象の変更時に、どのテストクラスを修正すべきか判断しにくい

## 修正例

テスト対象のクラスごとにテストクラスを分割する:

```java
class UserServiceTest {
    @Test
    void testUserCreation() {
        userService.create("Alice", "alice@example.com");
        assertNotNull(userService.findById(1L));
    }
}

class OrderServiceTest {
    @Test
    void testOrderCalculation() {
        orderService.placeOrder("Widget", 1, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("10.00"), orderService.calculateTotal());
    }
}
```

## 検出ルール

各テストメソッドが操作するオブジェクトスコープの共通度を計算し、テストメソッドのペア間で30%未満のスコープしか共有していない場合に検出（ヒューリスティック）。
