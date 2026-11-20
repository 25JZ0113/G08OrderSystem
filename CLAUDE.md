# CLAUDE.md

喫茶「生成り」のテーブルオーダーシステム(進級制作 G08)。Claude Code がこのリポジトリで作業するときの決まりごとをまとめる。
セットアップ手順は `README.md` を参照。

> このリポジトリは公開されている。資料の URL・学校のサーバーの情報・個人名などは書かず、
> Git に上げない `CLAUDE.local.md`(各自の PC に置く)に書く。

## 返答・作業の進め方

- 返答は日本語。学生向けなので、専門用語には短い説明を添える
- ユーザーの PC(Windows・Eclipse・学校の Oracle)で動かす必要がある操作は、ユーザーに手順を示す
  (クラウドのセッションからは PC の Eclipse・学校の DB に届かない)
- 機能追加・修正は、ブランチを作って **下書きの PR** にする。ユーザーが PC で確かめて「マージして」と言うまでマージしない
- コミットの作者は `25JZ0113 <25JZ0113@users.noreply.github.com>`。個人のメールアドレスは使わない
- PR の説明には、変更内容・PC でやること(SQL の実行、Eclipse の「リフレッシュ」→「公開 → 再始動」など)・確認したことを書く

## 絶対に守ること

- `src/main/java/db/DBManager.java`(学校の Oracle の接続情報)は **絶対にコミットしない**。
  リポジトリには `DBManager.java.example` だけを置く
- `src/main/webapp/WEB-INF/upload.properties`(PC ごとの画像の保存先)もコミットしない。ひな形は `upload.properties.example`
- どちらも `.gitignore` で除外済み。`git add -A` などで混ざらないよう、コミット前に `git status` で確かめる

## 先生のコーディングルール(進級制作ノートより)

- **SQL はテキストブロック(`"""`)で書く**。`"""` の後ろには何も書かない
- **JOIN した列には必ず別名を付ける**(例:`c.name AS cname`)。そのままでは Java 側で値を取り出せない
- **別名に Oracle の予約語を使わない**(`uid`・`day`・`month` など)。例:`u.id AS uuser_id`
- **DATE 型の列は `rs.getTimestamp("列名")` で読み、`java.sql.Timestamp` で持つ**(SQL で `TO_CHAR` しない)
- **DB への接続は try-with-resources**、SQL は **`PreparedStatement` と `?`**(文字列の連結で値を埋め込まない)
- DB の接続は `DBManager.getConnection()` にまとめ、SQL は DAO に集める(**MVC**:Servlet = Controller、JSP = View、model・dao = Model)
- JSP は `src/main/webapp/WEB-INF/views` に置き、Servlet から forward して表示する(ブラウザから直接開かせない)
- 主キーは `ID`。シーケンス(`SEQ_テーブル名`、1000 から採番)で振る。1〜999 はテストデータ用
- テーブル・列の命名(ER 図):テーブル名は英語の複数形、列名は英大文字と `_`、外部キーは「参照先の単数形_ID」、
  列名に `テーブル名_` を付けない、日時・金額・状態は役割が分かる名前(`VISIT_DATETIME`・`TOTAL_AMOUNT`)
- 日時の列は DATE 型(秒まで記録できる)
- パスワードは BCrypt(講義「ログイン処理とパスワードのハッシュ化」)。72 バイトまで

## 構成

- Java(JDK 25)/ Jakarta Servlet・JSP / Tomcat 11 / Oracle / Eclipse(動的 Web プロジェクト、コンテキストパスは `/OrderSystem`)
- ライブラリ(`src/main/webapp/WEB-INF/lib`):JSTL 3.0、Oracle JDBC `ojdbc17.jar`、jBCrypt 0.4
- パッケージ:`controller`(Servlet・Filter)、`dao`、`db`(DBManager)、`model`、`util`(`PasswordUtil`・`ImageStore`)
- 画面(JSP):`WEB-INF/views`。共通部品は `WEB-INF/views/common`(`head.jspf` など)
- CSS は `src/main/webapp/css/style.css` 1 つ、JS は `src/main/webapp/js`、商品画像は `src/main/webapp/images`(4:3 の JPG)

### 端末と URL

| 端末 | 入口 | 備考 |
|---|---|---|
| テーブル端末 | `/tableConfirm` → `/guestCount` → `/productList` | テーブル番号は Cookie(端末コード)で覚える。`TableSessionFilter` |
| キッチン | `/kitchen` | ログインなし |
| ホール | `/hall`(席状況)・`/serving`(提供待ち) | ログインなし |
| レジ | `/login` → `/register/table` | `/register/*` は `LoginFilter` |
| 管理者 | `/login?terminal=admin` → `/admin/menu` | `/admin/*` は `AdminFilter`(管理者だけ) |

### 画面づくりの決まり

- 登録・変更・削除の POST のあとは GET にリダイレクトする(PRG)。エラーのときだけ forward し、
  JSP で `history.replaceState` して再読み込みでフォームが送り直されないようにする(`adminProducts.jsp`・`adminUsers.jsp`)
- 金額は税込で持つ。状態の値は日本語の文字列(例:`空席`・`利用中`・`準備中`、`注文受付済み`〜`キャンセル(請求あり)`)
- 色・余白は `style.css` の CSS 変数を使う。ダークモードにも対応している

## データベース

- SQL は `sql/` の 3 つを **`g08erInit.sql` → `g08seqInit.sql` → `g08demoData.sql`** の順に実行する
  (`g08erInit.sql` は改行コード CRLF のまま保つ)
- テーブル:CATEGORIES・TABLES・USERS・PRODUCTS・PRODUCT_TOPPINGS・ORDERS・ORDER_DETAILS・TERMINAL_SETTINGS
- テスト用アカウント:`admin / admin123`(管理者)、`staff01`〜`staff03 / staff123`(社員。staff03 は利用停止)
- テストデータ:テーブル 1〜4 は利用中、5 は準備中、6 以降は空席
- テーブル定義を変えたら、次もそろえる:テストデータ、設計書(Google ドキュメント版と Notion 版の 21 章)、
  ER 図(astah、ユーザーが手元で直す)、Google ドライブの SQL フォルダの SQL
- テーブル定義・テストデータを変えた PR は、全員の PC で SQL の実行し直しが必要になることを PR に書く

## 確かめ方

- コンパイル:`DBManager.java.example` を一時的に `DBManager.java` としてコピーし、`javac` で全ソースをコンパイルする
  (コピーはコミットしない。リポジトリの外で作業する)
- 画面の確認:組み込み Tomcat + H2(Oracle 互換モード)+ Playwright で動かせる。ただし H2 では次が動かない
  - ホールの席状況の SQL(`KEEP (DENSE_RANK LAST ...)`)
  - 曜日別・月別の売上(`TRUNC(日付, 'IW')`・`TRUNC(日付, 'MM')`)
  - `9/24` のような整数の割り算(H2 では 0 になる)
- 最終確認は、ユーザーが学校の Oracle と Eclipse で行う。ブラウザは Ctrl+F5 で読み直してもらう

## 設計書・資料

- 設計書は Google ドキュメント版と Notion 版の 2 つ。内容を変えたら **両方** を直す
- 機能の追加・修正の予定は Notion の「新機能追加・修正事項リスト」で管理している。済んだらチェックを入れる
- 資料・設計書・Notion の場所は `CLAUDE.local.md` を参照

## 日程(進級制作ノートより)

- グループ別レビュー 1:11/30(月)3・4 限
- グループ別レビュー 2:1/13(水)3・4 限
- 進級・卒業制作発表会:1/29(金)・1/30(土)、2/12(金)・2/13(土)
- 作成物:設計書・システム・利用手順書
