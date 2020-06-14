# 仕様変更チュートリアル

こちらは、実際に図書館アプリの仕様変更を実践し、

CCSR手法での変更容易性を実感して頂くためのチュートリアルです。

JIGレポートの説明、ドメインオブジェクトのマッピングの仕方（画面、DB、JSON）の詳細については、
[ccsr-object-mapping](https://github.com/system-sekkei/ccsr-object-mapping) のリポジトリを参照して下さい


# 1. 現状の仕様を確認できる状態にする

## アプリを起動する
- Gradleタスク bootRunを実行する

```shell script
gradle bootRun
```

- localhost:8080 にブラウザでアクセスする。

- メニューが出てくればOKです。

![image](https://user-images.githubusercontent.com/33717710/84584738-f766b780-ae42-11ea-978e-41c6c64091f9.png)

## JIGレポートを出力する
- Gradleタスク build jigを実行する

```shell script
gradle jigReports
```

- `build/jig/` 配下に、JIGレポートが出てくることを確認する

![image](https://user-images.githubusercontent.com/33717710/84584776-7825b380-ae43-11ea-9dca-8469d5fa3e15.png)

## DataBaseの確認をする

- `localhost:8080/h2-console/` にアクセスする

【接続画面】
![image](https://user-images.githubusercontent.com/33717710/84585073-bffa0a00-ae46-11ea-8e08-dc5e7b917923.png)


# 2. 貸出制限の変更をする

RDRAにある`蔵書の貸出を登録する`の貸出制限が変更された場合を考えてみます。


![composit-usecase-loan](https://user-images.githubusercontent.com/3654676/83082272-e6394f00-a0bd-11ea-97c1-7e2e4cc3299c.png)


## 現在の貸出ルールを確認する

- `localhost:8080` にアクセス
- 大人の会員で、`遅延日数３日未満なら、貸出５冊まで` のルールが実装されているか確認してみます
    - 会員番号 1
    - 蔵書番号 2-A、2-B、2-C、2-D、2-E
        - 初回起動時、会員番号1の方は、蔵書番号 1-A を貸出中です
- 2-Eを借りようとした時点で、以下のエラーが出ることを確認できます。

![image](https://user-images.githubusercontent.com/33717710/84587528-f5105780-ae5a-11ea-9e9d-ee61d19cea2c.png)

## 貸出ルールの在り処を探し、変更する

図書館の貸出運用の見直しが入り、大人は遅延なしの場合、5冊→3冊までの制限に変更したい、となったとします。

アプリの中で貸出制限を実装している箇所を探します。

- ①JIGレポートの`service-method-call-hierarchy.svg`を開く
    - こちらは、どのコントローラからコーディネーター（サービスを取りまとめる役）、サービスが呼ばれ、どのリポジトリを参照しているかを図示したものです
    - 変更対象の画面（コントローラ）がハッキリしている場合は、こちらの図を参照するとわかりやすいでしょう


- ②貸出の登録画面を確認し、以下4つのユースケース（コーディネーター）が繋がっていることを確認します
    - 貸出状況を提示する
    - 貸出制限を判断する
    - 貸し出す
    - 会員番号の有効性を確認する

- `貸出制限を判断する` というユースケースが、それっぽいです

![image](https://user-images.githubusercontent.com/33717710/84586990-7fa28800-ae56-11ea-8455-bfb63a3c4055.png)

- ③ `貸出制限を判断する` に紐づくドメインオブジェクトを確認していく
    - JIGレポートの `category-usage.svg` を開く
    - `貸出制限を判断する` からは、 `貸出状況`、`貸出制限` と線が繋がっていることを確認します
    - `貸出制限` の先には、`貸出制限の表条件` も繋がっていることを確認します

![image](https://user-images.githubusercontent.com/33717710/84587745-e9be2b80-ae5c-11ea-8899-1babdfe515f1.png)

- ④`貸出制限` を確認する
    - `Restriction.java` を開く
    - `貸出制限` は、 `貸出制限の表条件` を使用し、何冊まで貸し出せるかを判断しています
    
```java
/**
 * 貸出制限
 */
class Restriction {
    Member member;
    Loans loans;
    CurrentDate date;

    Restriction(Member member, Loans loans, CurrentDate date) {
        this.member = member;
        this.loans = loans;
        this.date = date;
    }
    // 貸出制限の表条件
    static final RestrictionMap map = new RestrictionMap();

    RestrictionOfQuantity ofQuantity() {
        DelayStatus delayStatus = loans.worst(date);
        DelayOfMember delayOfMember = new DelayOfMember(delayStatus, member.type());
        // 表情件に、遅延状況と会員種別を渡して判断
        return map.of(delayOfMember);
    }
}
```

- ⑤`貸出制限の表条件` を確認する
    - `RestrictionMap.java` を開く
    - 大人で、遅延日数3日未満の場合、貸出5冊までというルールが確認できます
   
```java
Map<DelayOfMember, RestrictionOfQuantity> map = new HashMap<>();

{
    // このルール
    define(遅延日数３日未満, 大人, 貸出５冊まで);
    define(遅延日数３日未満, 子供, 貸出７冊まで);

    define(遅延日数７日未満, 大人, 貸出不可);
    define(遅延日数７日未満, 子供, 貸出４冊まで);

    define(それ以外, 大人, 貸出不可);
    define(それ以外, 子供, 貸出不可);
}
```

- `category-usage.svg` で確認すると、`貸出制限の表条件` では、`冊数制限(判定結果)`、`遅延状態`、`会員種別` の3つの区分を使用してるのが確認できます

![image](https://user-images.githubusercontent.com/33717710/84588216-7f0eef00-ae60-11ea-8b57-bd133887af58.png)

- ⑥`貸出制限の表条件` の大人、遅延日数3日未満の場合は、貸出３冊までに変更する
    - `冊数制限(判定結果)` に `貸出３冊まで` という区分は存在しないため、追加をします
    
```java
/**
 * *冊数制限(判定結果)
 */
enum RestrictionOfQuantity {
    貸出５冊まで(5),
    貸出７冊まで(7),
    貸出４冊まで(4),
    // 新しく追加
    貸出３冊まで(3),
    貸出不可(0);

```


```java
Map<DelayOfMember, RestrictionOfQuantity> map = new HashMap<>();

{
    // 変更
    define(遅延日数３日未満, 大人, 貸出３冊まで);
    define(遅延日数３日未満, 子供, 貸出７冊まで);

    define(遅延日数７日未満, 大人, 貸出不可);
    define(遅延日数７日未満, 子供, 貸出４冊まで);

    define(それ以外, 大人, 貸出不可);
    define(それ以外, 子供, 貸出不可);
}
```

- ⑦動作確認
    - `localhost:8080` にアクセス
    - 実際に借りてみて、3冊までの制限になっているかを確認します
        - 会員番号 1
        - 蔵書番号 2-A、2-B、2-C
            - 初回起動時、会員番号1の方は、蔵書番号 1-A を貸出中です
    - 2-Cを借りようとした時点で、以下のエラーが出ることを確認できます。

![image](https://user-images.githubusercontent.com/33717710/84588308-52a7a280-ae61-11ea-8add-162b0f19f2a5.png)

- ⑧JIGレポートで追加した区分と、依存関係を再確認する
    - Gradleタスク `jigReports` を実行する
    - `category.svg` を確認する
    - `冊数制限(判定結果)` の区分に、 `貸出３冊まで` が追加されたのを確認する
    - `category-usage.svg` を確認する
    - `冊数制限(判定結果)` の区分を参照しているオブジェクトが、以下のみであることを確認する
        - `貸出制限`
        - `貸出状況`
        - `貸出制限の表条件`
    - `冊数制限(判定結果)` は、②で確認したコーディネーター、サービスメソッドのみが参照（正確には、`冊数制限(判定結果)` を参照している `貸出状況`）していることが確認できます。
        - `冊数制限(判定結果)` に区分を追加したことにより、影響が出る箇所が、JIGレポートで俯瞰しやすくなっています
    

---

以上で、チュートリアルは終わりです。

お疲れ様でした。