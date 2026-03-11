# Redundant Assertion

## 概要

自明に真となるアサーションが含まれるスメル。`assertEquals(true, true)` や `assertNotNull("literal")` のように、常に成功するアサーションは何も検証していない。

## 典型的なコード例

```java
@Test
void testServiceExists() {
    assertNotNull(service);
    assertEquals(true, true);       // 常に成功
    assertEquals("abc", "abc");     // 常に成功
}
```

```java
@Test
void testBasic() {
    assertTrue(true);     // 常に成功
    assertFalse(false);   // 常に成功
}
```

## 何が問題か

- 何も検証していないのにテストが「パス」する
- テストカバレッジを水増しする
- 「とりあえずテストを書いた」という誤った安心感を与える

## 修正例

意味のある検証に置き換える:

```java
@Test
void testServiceInitialization() {
    assertNotNull(service);
    assertEquals(0, service.count(), "initial count should be zero");
}
```

## 検出ルール

以下のパターンを検出:
- `assertTrue(true)` / `assertFalse(false)`
- `assertNull(null)`
- `assertEquals(a, a)` で両引数が同一リテラル
