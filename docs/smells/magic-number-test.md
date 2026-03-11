# Magic Number Test

## 概要

アサーション内に意味の分からない数値リテラル（マジックナンバー）が直接記述されているスメル。

## 典型的なコード例

```java
@Test
void testCalculateDiscount() {
    BigDecimal result = service.calculateDiscount(order);
    assertEquals(new BigDecimal("1247"), result);  // 1247 の意味は？
}

@Test
void testUserAge() {
    assertEquals(42, user.getAge());  // なぜ 42？
}
```

## 何が問題か

- 数値リテラルだけでは期待値の根拠がわからない
- テストが仕様のドキュメントとして機能しない
- 計算ロジックが変更されたとき、数値を変えるべきか判断できない

## 修正例

名前付き定数やコメントで意味を明示する:

```java
private static final int EXPECTED_DEFAULT_AGE = 42;

@Test
void testUserAge() {
    assertEquals(EXPECTED_DEFAULT_AGE, user.getAge());
}
```

または期待値の計算過程を示す:

```java
@Test
void testCalculateDiscount() {
    // 10% discount on 12470 = 1247
    BigDecimal basePrice = new BigDecimal("12470");
    BigDecimal expectedDiscount = basePrice.multiply(new BigDecimal("0.1"));
    assertEquals(expectedDiscount, service.calculateDiscount(order));
}
```

## 検出ルール

アサーションの引数に `0`, `1`, `2`, `-1`, `-2`, `10`, `100`, `1000` 以外の数値リテラルが直接記述されている場合に検出。
