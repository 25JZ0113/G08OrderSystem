# 喫茶 生成り テーブルオーダーシステム(G08)

喫茶店「生成り」のテーブルオーダーシステムです。お客様がテーブルの端末から注文し、キッチン・ホール・レジ・管理者の各端末で注文を受けて処理します。

- Java Servlet / JSP(Eclipse の動的 Web プロジェクト)
- Tomcat 11(Jakarta EE 11。`jakarta.*` を使う)・JDK 25
- Oracle(学校の DB)

## はじめに(各自の PC で 1 回だけ)

### 1. DB の接続情報を書く(`DBManager.java`)

接続情報(本物の ID・パスワード)は GitHub に上げないので、ひな形をコピーして各自で作ります。

1. `src/main/java/db/DBManager.java.example` をコピーし、同じフォルダに `DBManager.java` という名前で置く
2. `HOST_NAME`・`SERVICE_NAME`・`USER`・`PASSWORD` を自分の接続情報に書き換える

`DBManager.java` は `.gitignore` で Git の対象外にしてあります。**絶対にコミットしないでください。**

### 2. 商品画像の保存先を書く(`upload.properties`)

メニュー管理画面でアップロードした商品画像は、次の 2 か所に保存されます。

- Tomcat の配備先の `images`(保存してすぐ画面に出る。Eclipse で「クリーン」すると消える)
- プロジェクトの `src/main/webapp/images`(「クリーン」しても消えない。Git でコミットできる)

プロジェクトの場所は PC ごとに違うので、設定ファイルに書きます。

1. `src/main/webapp/WEB-INF/upload.properties.example` をコピーし、同じフォルダに `upload.properties` という名前で置く
2. `project.images.dir=` の行を、自分の PC のプロジェクトの `src\main\webapp\images` のパスに書き換える(`\` のままで大丈夫)

   ```
   project.images.dir=C:\Users\(ユーザー名)\eclipse-workspace\G08OrderSystem\src\main\webapp\images
   ```

`upload.properties` も Git の対象外です。作らなかった場合は配備先だけに保存され、登録後の画面に「※画像はプロジェクトの images に保存していません」と表示されます。

あわせて、Eclipse の「ウィンドウ → 設定 → 一般 → ワークスペース」で「ネイティブ・フックまたはポーリングを使用してリフレッシュ」にチェックを入れてください。入っていないと、アップロードした画像に Eclipse が気づかず、次の「クリーン」で画面から消えます(「リフレッシュ」すれば戻ります)。

### 3. DB を作る(SQL)

`sql` フォルダの 3 つのファイルを、**この順番で** SQL Developer(VS Code の SQL Developer 拡張でも可)で実行します。

1. `g08erInit.sql` … テーブルを作る
2. `g08seqInit.sql` … ID の自動採番用のシーケンスを作る
3. `g08demoData.sql` … テストデータを入れる

初回は `DROP` のところでエラー(ORA-00942・ORA-02289)が出ますが、まだ消すものがないだけなので無視して大丈夫です。テストデータの SQL は Google ドライブの SQL フォルダにも同じものを置いています。

### 4. Eclipse で動かす

1. プロジェクトを右クリック →「リフレッシュ」
2. Tomcat 11 のサーバーにプロジェクトを追加し、「公開」→「開始(再始動)」
3. ブラウザで http://localhost:8080/OrderSystem/ を開く(どの端末として使うかを選ぶ画面が出る)

## テスト用のアカウント

| ログイン ID | パスワード | 権限 | 備考 |
|---|---|---|---|
| admin | admin123 | 管理者 | 管理者端末(`/login?terminal=admin`)に入れる |
| staff01 | staff123 | 社員 | レジ端末(`/login`) |
| staff02 | staff123 | 社員 | |
| staff03 | staff123 | 社員 | 利用停止中(ログインできない) |

パスワードは BCrypt(jBCrypt・コスト 10)でハッシュ化して保存しています。

## 変更を取り込むとき(`git pull` のあと)

1. Eclipse でプロジェクトを「リフレッシュ」
2. サーバーを「公開」→「再始動」
3. ブラウザで Ctrl+F5(CSS・JavaScript を読み直す)

画面がおかしいときは、サーバーを右クリック →「クリーン」してから再始動してください。

`sql` フォルダのファイルが変わったときは、上の「3. DB を作る」の 3 つをもう一度順番に実行してください(DB の中身はテストデータに戻ります)。

## フォルダ構成

```
sql/                         DB を作る SQL(テーブル・シーケンス・テストデータ)
src/main/java/
  controller/                サーブレット・フィルター
  dao/                       DB を読み書きするクラス
  db/                        DB への接続(DBManager.java は各自で作る)
  model/                     データを入れるクラス
  util/                      パスワードのハッシュ化(PasswordUtil)・商品画像の保存(ImageStore)
src/main/webapp/
  WEB-INF/views/             JSP(画面)
  WEB-INF/lib/               ライブラリ(JSTL・Oracle JDBC・jBCrypt)
  css/  js/                  CSS・JavaScript
  images/                    商品画像
  img/                       ファビコン
```

## Git に上げないファイル

| ファイル | 中身 | ひな形 |
|---|---|---|
| `src/main/java/db/DBManager.java` | 学校の Oracle の接続情報 | `DBManager.java.example` |
| `src/main/webapp/WEB-INF/upload.properties` | 商品画像の保存先(PC ごとに違う) | `upload.properties.example` |
| `CLAUDE.local.md` | Claude Code 用のメモ(資料の URL など) | なし(班内で共有) |
