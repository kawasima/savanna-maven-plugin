# Rotten Green Test

## 概要

テスト内のすべてのアサーションが条件分岐（`if` や `catch`）の内部にあり、条件が満たされなければアサーションが一切実行されずにテストがパスしてしまうスメル。

## 典型的なコード例

```java
@Test
void testProcess() {
    try {
        Result result = service.process(input);
        assertNotNull(result);           // catch に入ると実行されない
        assertEquals("OK", result.getStatus());
    } catch (Exception e) {
        fail("Unexpected exception: " + e.getMessage());
    }
}
```

```java
@Test
void testConditional() {
    Result result = service.process(input);
    if (result != null) {
        assertEquals("OK", result.getStatus());  // result が null ならスキップ
    }
}
```

## 何が問題か

- テストが常にグリーン（パス）になるが、実際には何も検証していない場合がある
- バグがあっても検知できない「腐った」テスト
- テストカバレッジレポート上はカバーされているように見える

## 修正例

アサーションを条件分岐の外に出す:

```java
@Test
void testProcess() {
    Result result = service.process(input);
    assertNotNull(result);
    assertEquals("OK", result.getStatus());
}
```

例外テストは `assertThrows` を使う:

```java
@Test
void testProcessThrows() {
    assertThrows(ProcessException.class, () -> service.process(invalidInput));
}
```

## 検出ルール

テストメソッド内のアサーションがすべて `if` 文または `try-catch` ブロック内にあり、条件分岐の外にアサーションが存在しない場合に検出（ヒューリスティック）。
