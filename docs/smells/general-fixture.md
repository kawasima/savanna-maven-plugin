# General Fixture

## 概要

`@BeforeEach` で初期化されるフィールドのうち、一部のテストメソッドでしか使われないものがあるスメル。セットアップが過剰であることを示す。

## 典型的なコード例

```java
class OrderServiceTest {
    private UserService userService;
    private OrderService orderService;
    private EmailService emailService;  // testCalculateTotal では使わない

    @BeforeEach
    void setUp() {
        userService = new UserService();
        orderService = new OrderService();
        emailService = new EmailService();  // 全テストで初期化される
    }

    @Test
    void testCalculateTotal() {
        orderService.placeOrder("Widget", 1, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("10.00"), orderService.calculateTotal());
        // userService, emailService は使わない
    }

    @Test
    void testSendOrderConfirmation() {
        User user = userService.create("Alice", "alice@example.com");
        orderService.placeOrder("Widget", 1, new BigDecimal("10.00"));
        emailService.sendConfirmation(user.getEmail());
        // 全フィールドを使う
    }
}
```

## 何が問題か

- 不要なオブジェクトの初期化でテストが遅くなる
- セットアップが長くなり、テストの前提条件が不明瞭になる
- テスト間の不要な結合が生まれる（emailService の変更が全テストに影響）

## 修正例

テストメソッドごとに必要なセットアップをインラインに移す、またはテストクラスを分割する:

```java
class OrderServiceCalculationTest {
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService();
    }

    @Test
    void testCalculateTotal() {
        orderService.placeOrder("Widget", 1, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("10.00"), orderService.calculateTotal());
    }
}
```

## 検出ルール

`@BeforeEach` メソッド内で代入されるフィールドのうち、テストメソッドの半数未満でしか参照されないフィールドがある場合に検出。
