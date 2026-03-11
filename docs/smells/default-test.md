# Default Test

## 概要

IDEが自動生成したデフォルトのテストクラス名がそのまま残っているスメル。テストの対象が不明確であることを示す。

## 典型的なコード例

```java
class ExampleTest {  // IDEが生成したデフォルト名
    @Test
    void test1() {
        assertTrue(true);
    }
}
```

```java
class MyClassTest {  // 具体性のないクラス名
    @Test
    void testMethod() {
        // ...
    }
}
```

## 何が問題か

- テストクラス名から何をテストしているか推測できない
- IDEが生成した雛形のまま放置されている可能性がある
- テストの対象となるプロダクションクラスとの対応付けができない

## 修正例

テスト対象のクラス名を反映した名前にリネームする:

```java
class UserServiceTest {  // テスト対象が明確
    @Test
    void createUser_returnsNewUser() {
        // ...
    }
}
```

## 検出ルール

テストクラス名が以下のようなIDEデフォルト名に一致する場合に検出:
`ExampleTest`, `SampleTest`, `MyTest`, `TestClass`, `MainTest`, `AppTest`, `DemoTest` など。
