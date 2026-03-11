# Hidden Dependency

## 概要

テストメソッド内でシングルトンや静的ファクトリメソッド（`getInstance()`, `getDefault()` 等）を呼び出しているスメル。テストの依存関係が隠蔽されており、テストの制御が難しい。

## 典型的なコード例

```java
@Test
void testProcess() {
    Config config = Config.getInstance();       // 隠れた依存
    Database db = Database.getDefault();         // 隠れた依存
    service.process(input);
    assertEquals("OK", service.getStatus());
}
```

## 何が問題か

- テストの前提条件がコンストラクタや `@BeforeEach` から読み取れない
- シングルトンの状態がテスト間で共有され、テストの独立性が損なわれる
- モック化が困難（static メソッドの差し替えが必要）
- テストの再現性が低い

## 修正例

依存をコンストラクタインジェクションにする:

```java
class Service {
    private final Config config;
    private final Database db;

    Service(Config config, Database db) {
        this.config = config;
        this.db = db;
    }
}

@Test
void testProcess() {
    Config config = new Config("test-settings");
    Database db = new InMemoryDatabase();
    Service service = new Service(config, db);
    service.process(input);
    assertEquals("OK", service.getStatus());
}
```

## 検出ルール

テストメソッド内で `getInstance()`, `getDefault()`, `getSingleton()`, `getContext()`, `newInstance()`, `current()`, `getEnvironment()` などのシングルトンパターンに合致するメソッド呼び出しがある場合に検出。
