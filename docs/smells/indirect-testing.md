# Indirect Testing

## 概要

テストクラス名から推定されるプロダクションクラスとは別のクラスを主にテストしているスメル。テストの配置場所が不適切であることを示す。

## 典型的なコード例

```java
class UserServiceTest {
    @Test
    void testUserCreation() {
        // UserServiceTest なのに、主に repository を操作している
        repo.save(new User("Alice", "alice@example.com"));
        repo.save(new User("Bob", "bob@example.com"));
        repo.findById(1L);
        repo.findAll();
        userService.count();  // userService は1回だけ
    }
}
```

## 何が問題か

- テストクラス名が示す対象と実際のテスト内容が乖離しており、テストの発見性が低い
- プロダクションクラスの変更時に、どのテストを確認すべきか判断しにくい
- テストクラスの責務が不明確になる

## 修正例

テスト対象のクラスに合ったテストクラスに移動する:

```java
class UserRepositoryTest {
    @Test
    void testSaveAndFind() {
        repo.save(new User("Alice", "alice@example.com"));
        User found = repo.findById(1L);
        assertNotNull(found);
    }
}

class UserServiceTest {
    @Test
    void testCount() {
        userService.create("Alice", "alice@example.com");
        assertEquals(1, userService.count());
    }
}
```

## 検出ルール

テストクラス名から期待されるプロダクションクラスを導出し（`UserServiceTest` → `UserService`）、テストメソッド内の呼び出しのうち期待クラスへの呼び出しが全体の30%未満の場合に検出（ヒューリスティック）。フィールドの型名も考慮し、変数名が異なっていても型が一致すれば期待クラスと認識する。
