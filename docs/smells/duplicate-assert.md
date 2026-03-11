# Duplicate Assert

## 概要

同一テストメソッド内にまったく同じアサーション式が重複して存在するスメル。冗長であり、コピペミスの可能性を示唆する。

## 典型的なコード例

```java
@Test
void testOrderTotal() {
    Order order = service.placeOrder("Widget", 5, new BigDecimal("10.00"));
    assertEquals(new BigDecimal("50.00"), order.getTotal());
    assertEquals(new BigDecimal("50.00"), service.calculateTotal());
    assertEquals(new BigDecimal("50.00"), order.getTotal());  // 1行目と完全に重複
}
```

## 何が問題か

- 同じアサーションを2回実行しても品質は上がらない
- コピペの消し忘れの可能性が高く、本来は別の値を検証すべきだったかもしれない
- テストの可読性が下がる

## 修正例

重複を除去し、本来検証すべき内容を明確にする:

```java
@Test
void testOrderTotal() {
    Order order = service.placeOrder("Widget", 5, new BigDecimal("10.00"));
    assertEquals(new BigDecimal("50.00"), order.getTotal(), "single order total");
    assertEquals(new BigDecimal("50.00"), service.calculateTotal(), "service total");
}
```

## 検出ルール

同一テストメソッド内で、構文的に同一のアサーション式が2回以上出現する場合に検出。
