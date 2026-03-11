# Flaky Test

## 概要

テストの実行結果が不安定になる要因を複数含むスメル。乱数生成、時刻依存、スリープなどが組み合わさり、実行のたびに結果が変わるリスクが高い。

## 典型的なコード例

```java
@Test
void testTimedRandomOperation() throws InterruptedException {
    Random random = new Random();
    int value = random.nextInt(100);
    service.process(value);
    Thread.sleep(500);                         // タイミング依存
    Instant now = Instant.now();               // 時刻依存
    assertTrue(service.getLastProcessed().isBefore(now));
}
```

## 何が問題か

- CIで断続的に失敗し、開発チームの信頼を損なう
- 失敗の再現が困難で、デバッグに多大な時間がかかる
- 「たまに失敗するけど再実行すれば通る」というアンチパターンを助長する
- テスト結果への信頼が下がり、本物のバグを見逃す

## 修正例

乱数を制御可能にする:

```java
@Test
void testDeterministicProcess() {
    // 固定シードで再現可能にする
    Random random = new Random(42);
    int value = random.nextInt(100);
    service.process(value);
    assertEquals(expectedResult, service.getResult());
}
```

時刻を注入可能にする:

```java
@Test
void testWithFixedClock() {
    Clock fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    Service service = new Service(fixedClock);
    service.process(input);
    assertEquals(Instant.parse("2024-01-01T00:00:00Z"), service.getLastProcessed());
}
```

スリープを除去する（[Sleepy Test](sleepy-test.md) 参照）。

## 検出ルール

以下の要因のうち2つ以上が同一テストメソッド内に存在する場合に検出:
1. 乱数生成（`Random` のインスタンス化、`Math.random()` 呼び出し）
2. 時刻依存（`Instant.now()`, `System.currentTimeMillis()`, `System.nanoTime()`）
3. スリープ（`Thread.sleep()`, `TimeUnit.sleep()`）
