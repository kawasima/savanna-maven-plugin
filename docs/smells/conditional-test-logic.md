# Conditional Test Logic

## 概要

テストメソッド内に `if`、`switch`、`for`、`while` などの制御フロー文が含まれるスメル。テストが条件によって異なる経路を通るため、テストの意図が不明瞭になる。

## 典型的なコード例

```java
@Test
void testClearOrders() {
    service.placeOrder("Widget", 1, new BigDecimal("10.00"));
    if (service.getOrderCount() > 0) {  // テストに条件分岐は不要
        service.clear();
    }
    assertEquals(0, service.getOrderCount());
}
```

```java
@Test
void testAllStatuses() {
    for (Status status : Status.values()) {  // ループで複数ケースをまとめている
        Order order = new Order();
        order.setStatus(status);
        assertNotNull(order.getStatusLabel());
    }
}
```

## 何が問題か

- テストは「このケースではこうなるべき」を明示的に記述すべきもの。条件分岐があるとテスト自体にバグが潜む可能性がある
- 条件が偽の場合、アサーションがスキップされるリスクがある（Rotten Green Test に繋がる）
- ループでまとめると、どの値で失敗したか特定しにくい

## 修正例

条件分岐を除去し、前提条件を明示する:

```java
@Test
void testClearOrders() {
    service.placeOrder("Widget", 1, new BigDecimal("10.00"));
    assertEquals(1, service.getOrderCount(), "precondition: 1 order placed");
    service.clear();
    assertEquals(0, service.getOrderCount());
}
```

ループは `@ParameterizedTest` に置き換える:

```java
@ParameterizedTest
@EnumSource(Status.class)
void testStatusLabel(Status status) {
    Order order = new Order();
    order.setStatus(status);
    assertNotNull(order.getStatusLabel());
}
```

## 検出ルール

テストメソッド内に `if`、`switch`、`for`、`forEach`、`while` 文が存在する場合に検出。
