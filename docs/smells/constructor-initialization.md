# Constructor Initialization

## 概要

テストクラスのコンストラクタでフィールドの初期化を行っているスメル。JUnit 5ではテストメソッドごとにインスタンスが生成されるため動作はするが、セットアップの意図が不明瞭になる。

## 典型的なコード例

```java
class UserServiceTest {
    private final UserService service;

    UserServiceTest() {
        service = new UserService();  // コンストラクタで初期化
    }

    @Test
    void testCreate() {
        User user = service.create("Alice", "alice@example.com");
        assertNotNull(user);
    }
}
```

## 何が問題か

- `@BeforeEach` というJUnit 5の標準的な初期化メカニズムを使わないため、他の開発者がセットアップ処理を見落としやすい
- コンストラクタでの初期化は JUnit のライフサイクルアノテーションと併用すると実行順序が直感的でなくなる
- テストフレームワークの拡張機能（`@ExtendWith` によるDI等）との互換性が低い

## 修正例

```java
class UserServiceTest {
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService();
    }

    @Test
    void testCreate() {
        User user = service.create("Alice", "alice@example.com");
        assertNotNull(user);
    }
}
```

## 検出ルール

テストクラスに本体が空でないコンストラクタが存在する場合に検出。
