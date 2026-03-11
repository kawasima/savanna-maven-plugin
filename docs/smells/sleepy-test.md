# Sleepy Test

## 概要

テストメソッド内で `Thread.sleep()` や `TimeUnit.sleep()` を使って待機しているスメル。テストを遅くし、タイミングに依存する不安定なテストの原因になる。

## 典型的なコード例

```java
@Test
void testAsyncOperation() throws InterruptedException {
    service.submitAsync(task);
    Thread.sleep(2000);  // 2秒待つ
    assertTrue(task.isCompleted());
}
```

```java
@Test
void testWithDelay() throws InterruptedException {
    SECONDS.sleep(1);  // TimeUnit を使った待機
    assertNotNull(cache.get("key"));
}
```

## 何が問題か

- テストスイートの実行時間が大幅に増加する
- 環境（CPU速度、負荷）によって必要な待機時間が変わり、テストが不安定になる
- 待機時間が短すぎると偽の失敗、長すぎると無駄な時間が生じる

## 修正例

`Awaitility` ライブラリでポーリングベースの待機にする:

```java
@Test
void testAsyncOperation() {
    service.submitAsync(task);
    await().atMost(5, SECONDS)
           .untilAsserted(() -> assertTrue(task.isCompleted()));
}
```

`CompletableFuture` を使って同期的にテストする:

```java
@Test
void testAsyncOperation() throws Exception {
    CompletableFuture<Result> future = service.submitAsync(task);
    Result result = future.get(5, SECONDS);
    assertNotNull(result);
}
```

`CountDownLatch` を使う:

```java
@Test
void testAsyncCallback() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    service.onComplete(result -> latch.countDown());
    service.submitAsync(task);
    assertTrue(latch.await(5, SECONDS));
}
```

## 検出ルール

テストメソッド内で `Thread.sleep()` または `TimeUnit.*.sleep()` の呼び出しがある場合に検出。
