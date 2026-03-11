# Verbose Test

## 概要

テストメソッドの行数が多すぎるスメル。長いテストは理解しにくく、複数の関心事をテストしている可能性がある。

## 典型的なコード例

```java
@Test
void testFullOrderWorkflow() {
    // 30行を超えるテストメソッド
    User user = new User("Alice", "alice@example.com");
    // ... 大量のセットアップ ...
    // ... 複数のアクション ...
    // ... 多数のアサーション ...
}
```

## 何が問題か

- テストの意図が読み取りにくい
- 複数のシナリオを1テストにまとめている可能性が高い
- 失敗時にどの部分が原因か特定しにくい
- メンテナンスコストが高い

## 修正例

テストを複数の焦点の絞られたメソッドに分割する:

```java
@Test
void testCreateOrder() {
    Order order = service.placeOrder(request);
    assertEquals(OrderStatus.PLACED, order.getStatus());
}

@Test
void testCalculateOrderTotal() {
    Order order = service.placeOrder(request);
    assertEquals(new BigDecimal("30.00"), order.getTotal());
}

@Test
void testOrderNotification() {
    service.placeOrder(request);
    verify(notificationService).send(any());
}
```

共通のセットアップは `@BeforeEach` やヘルパーメソッドに抽出する。

## 検出ルール

テストメソッドの行数が30行を超える場合に検出（閾値はデフォルト30行）。
