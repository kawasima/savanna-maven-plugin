# Eager Test

## 概要

1つのテストメソッドが3つ以上の異なるコラボレータ（サービス、リポジトリ等）のメソッドを呼び出しているスメル。テストが一度に多くのことを検証しようとしていることを示す。

## 典型的なコード例

```java
@Test
void testFullWorkflow() {
    userService.create("Alice", "alice@example.com");
    orderService.placeOrder("Widget", 1, new BigDecimal("10.00"));
    emailService.sendConfirmation("alice@example.com");
    assertEquals(1, auditService.getLogCount());
}
```

## 何が問題か

- テストが失敗した際、どのコラボレータに問題があるか特定しにくい
- テストの前提条件が複雑になり、セットアップが肥大化する
- 1つのコラボレータの変更で無関係なテストが壊れる（脆いテスト）

## 修正例

テスト対象のコラボレータを1つに絞り、他はモック化する:

```java
@Test
void testPlaceOrder() {
    when(userService.findById(1L)).thenReturn(new User(1L, "Alice", "alice@example.com"));
    orderService.placeOrder("Widget", 1, new BigDecimal("10.00"));
    assertEquals(1, orderService.getOrderCount());
}

@Test
void testSendConfirmation() {
    emailService.sendConfirmation("alice@example.com");
    verify(emailService).sendConfirmation("alice@example.com");
}
```

## 検出ルール

テストメソッド内で3つ以上の異なるオブジェクトに対してメソッド呼び出しがある場合に検出。ただし、メソッドの戻り値を受けるローカル変数（例: `User user = service.create()`）は独立したコラボレータとしてカウントしない。ユーティリティクラス（`System`, `Assert`, `Mockito` 等）も除外。
