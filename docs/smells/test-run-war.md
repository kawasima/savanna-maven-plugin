# Test Run War

## 概要

テストクラスに `static` かつ非 `final` なフィールドが存在するスメル。テストの実行順序やスレッドによって値が変わり、テスト間で干渉が起きる原因になる。

## 典型的なコード例

```java
class CounterTest {
    private static int counter = 0;  // テスト間で共有される

    @Test
    void testIncrement() {
        counter++;
        assertEquals(1, counter);
    }

    @Test
    void testDecrement() {
        counter--;
        assertEquals(-1, counter);  // testIncrement が先に実行されると 0 になる
    }
}
```

## 何が問題か

- テストの実行順序に依存する結果になる（JUnit 5 はデフォルトでメソッド順序が不定）
- 並列テスト実行でデータ競合が発生する
- テストの独立性が完全に損なわれる

## 修正例

`static` フィールドをインスタンスフィールドにする（JUnit 5はメソッドごとにインスタンスを生成）:

```java
class CounterTest {
    private int counter = 0;  // テストごとに初期化される

    @Test
    void testIncrement() {
        counter++;
        assertEquals(1, counter);
    }

    @Test
    void testDecrement() {
        counter--;
        assertEquals(-1, counter);
    }
}
```

## 検出ルール

テストクラスに `static` かつ非 `final` なフィールドが存在する場合に検出。`static final` は定数として扱い、対象外。
