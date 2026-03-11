# Ignored Test

## 概要

`@Disabled` アノテーションが付与され、実行がスキップされるテストメソッドが存在するスメル。

## 典型的なコード例

```java
@Disabled("TODO: fix later")
@Test
void testConcurrentAccess() {
    // ...
}
```

```java
@Disabled
@Test
void testEdgeCase() {
    // 理由の記載なし
}
```

## 何が問題か

- 無効化されたテストは実質的にテストカバレッジの穴になる
- 「一時的に無効化」のつもりが恒久的に放置されがち
- 理由が記載されていない場合、なぜ無効化されたか後から判断できない

## 修正例

テストを修正して有効化する:

```java
@Test
void testConcurrentAccess() {
    ExecutorService executor = Executors.newFixedThreadPool(4);
    // 並行アクセスのテストを実装
}
```

修正に時間がかかる場合は、Issue を作成して理由を明記する:

```java
@Disabled("Issue #123: requires thread-safe UserService implementation")
@Test
void testConcurrentAccess() {
    // ...
}
```

## 検出ルール

テストメソッドに `@Disabled` アノテーションが付与されている場合に検出。
