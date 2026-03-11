# Obscure In-Line Setup

## 概要

テストメソッド内で最初のアサーションに到達するまでのセットアップコードが長すぎるスメル。テストの意図が大量のセットアップに埋もれてしまう。

## 典型的なコード例

```java
@Test
void testPlaceOrder() {
    UserService userService = new UserService();
    User user = userService.create("Alice", "alice@example.com");
    Address address = new Address("123 Main St", "Tokyo", "100-0001");
    user.setAddress(address);
    PaymentMethod payment = new CreditCard("4111111111111111", "12/25");
    user.setPaymentMethod(payment);
    Product widget = new Product("Widget", new BigDecimal("10.00"));
    Cart cart = new Cart(user);
    cart.add(widget, 3);
    ShippingOption shipping = ShippingOption.STANDARD;
    OrderRequest request = new OrderRequest(cart, shipping);
    TaxCalculator tax = new TaxCalculator("JP");
    request.setTaxCalculator(tax);
    // ここまで12行のセットアップ。テストの本題はここから:
    Order order = orderService.placeOrder(request);
    assertEquals(OrderStatus.PLACED, order.getStatus());
}
```

## 何が問題か

- テストの「何を検証しているか」が大量のセットアップに隠れて読み取りにくい
- セットアップの変更が多くのテストに波及する
- テストが長くなり、保守コストが高い

## 修正例

セットアップをヘルパーメソッドや `@BeforeEach` に抽出する:

```java
@BeforeEach
void setUp() {
    user = createUserWithAddress("Alice", "alice@example.com");
    cart = createCartWith(user, "Widget", 3);
}

@Test
void testPlaceOrder() {
    OrderRequest request = new OrderRequest(cart, ShippingOption.STANDARD);
    Order order = orderService.placeOrder(request);
    assertEquals(OrderStatus.PLACED, order.getStatus());
}
```

テストデータビルダーパターンを使う:

```java
@Test
void testPlaceOrder() {
    OrderRequest request = anOrderRequest()
        .withUser("Alice")
        .withItem("Widget", 3)
        .withShipping(STANDARD)
        .build();
    Order order = orderService.placeOrder(request);
    assertEquals(OrderStatus.PLACED, order.getStatus());
}
```

## 検出ルール

テストメソッド内で最初のアサーション呼び出しまでの文（statement）数が10を超える場合に検出。
