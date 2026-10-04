# KeplerianTelemetry

Keplerian Space Discovery（以下KSD）のテレメトリを受信するSpring Boot サーバ。  
WebSocket 経由で KSDから各天体／宇宙機の軌道情報（カルテシアン要素・ケプラー要素）を収集し、REST API で提供する。

本リポジトリはサーバおよび Web クライアントの**参照実装**である。WebSocket / REST API の仕様に準拠していれば、サーバ・クライアントともに独自実装に置き換えることができる。

---

## 目次

- [ビルド方法](#ビルド方法)
- [実行方法](#実行方法)
- [Web ダッシュボード](#web-ダッシュボード)
- [WebSocket API](#websocket-api)
- [REST API](#rest-api)
- [データモデル](#データモデル)

---

## ビルド方法

**前提条件:** Maven 3、Java 25

### 開発用ビルド

```bash
mvn package
```

### 配布用バンドルビルド（JRE 同梱）

```bash
mvn package -P bundle
```

`target/dist/KeplerianTelemetry/` 以下に以下が生成される。

| ファイル / フォルダ | 内容 |
|---|---|
| `telemetry-1.0.0.jar` | アプリケーション JAR |
| `start.bat` | 起動スクリプト（Windows） |
| `jdk-25.0.3.9/` | 同梱 Eclipse Adoptium JDK |
| `LICENSES/` | ライセンスファイル |

テストをスキップする場合:

```bash
mvn package -P bundle -DskipTests
```

---

## 実行方法

### 開発環境（ホットリロードあり）

```bash
mvn spring-boot:run
```

### JAR を直接実行

```bash
java -jar target/telemetry-1.0.0.jar
```

### ポートを変更して起動

```bash
java -jar target/telemetry-1.0.0.jar --server.port=9090
```

### 配布バンドルを実行（Windows）

```
target\dist\KeplerianTelemetry\start.bat
```

### 設定

`src/main/resources/application.properties` で変更可能。

| プロパティ | デフォルト値 | 説明 |
|---|---|---|
| `server.port` | `8080` | リスンポート |
| `logging.level.net.keplerian.telemetry.websocket.KsdWebSocketHandler` | `INFO` | WebSocket ハンドラのログレベル |

---

## Web ダッシュボード

`src/main/resources/static/index.html` は参照実装として同梱されている。起動後、ブラウザで `http://localhost:8080/` にアクセスすると確認できる。

地球を中心に固定した表示で、地球を周回する宇宙機・月だけを描く。

- 選択したオブジェクトの直交座標要素（地球相対）・ケプラー要素の表示
- 実際の太陽の方向から照らした地球
- KSD 本体と同じ軌道線の描画（`orbitRev` が変わったオブジェクトだけ `?orbits=true` で取り直す）
- KSD で選択中の宇宙機の対地速度・高度のグラフ（`GET /api/history`。画面左下に表示。横軸は選択からのゲーム内経過時間で、100 秒 → 300 秒 → 600 秒 → 1000 秒 → 直近 1000 秒のスライド表示。縦軸は表示中の値が収まる最小のスケールを選び、速度は 2.5 → 5 → 10 km/s、高度は 100 → 300 → 600 km と切り替わる（上限を超えた値はグラフの上端で切れる）。目盛りは左右とも最大値の 5 等分で、横線を共有する）
- KSD で選択中の宇宙機の過去の軌跡（`GET /api/track`。軌道投入前のみ。地表に固定した座標で描くので、地球が自転しても射点から地表に沿って伸びる）
- KSD で選択が変わると、画面側の選択と注視点もその機体（または地球）に合わせる。次に KSD で選択が変わるまでは、画面上で別の機体を選べる
- 選ばれたのが軌道投入前の宇宙機（打ち上げ中など）なら、カメラが自動で機体を追う。最初は地球中心・機体・カメラが一直線に並ぶ真上（高度約 7000 km）から見下ろし（射程方向が画面右）、高度 60 km を超えるとピッチ 30 度までピッチダウンして、飛行の軌跡を斜め横から見る。移動はすべて連続的で、画面をドラッグ・ズームしても自動制御は続く
- REST API を 1 秒ごと（固定）にポーリングして自動更新

### カスタムクライアントの実装

独自の HTML クライアントを実装する場合、`index.html` を参考にしながら REST API を利用するだけでよい。

- **データ取得:** `GET /api/objects` を任意の間隔でポーリングする
- **単体取得:** `GET /api/objects/{id}` で特定オブジェクトのみ取得できる
- **軌道線:** 通常のレスポンスには軌道線の版 `orbitRev` だけが入る。初回と、`orbitRev` が前回取得時から変わったオブジェクトだけ `?orbits=true` を付けて軌道線（`orbitLegs`）を取得する。描画は「親天体の `pos` ＋ 各点」を結ぶだけでよい（[軌道線の描き方](#軌道線の描き方)）
- **更新頻度:** サーバは KSD 接続中、1 秒ごとに KSD へ `QueryTelemetry` を送ってテレメトリを更新する。REST の呼び出しは KSD への問い合わせを伴わず、サーバが保持している最新値を返す

WebSocket への直接接続は不要で、REST API だけで完結する。`index.html` を `static/` フォルダに置けばサーバから配信されるが、別ホストで動かして CORS なしで利用することも可能（サーバは全オリジンを許可している）。

---

## WebSocket API

以下に定めるメッセージ仕様に準拠すれば、本サーバを独自実装のサーバに置き換えることができる。KSD はサーバの実装に依存せず、メッセージの種別・フォーマットのみに依存する。

**エンドポイント:** `ws://localhost:8080/ksd`

### 接続シーケンス

```
KSD                                  サーバ
    |                                   |
    |--- (接続確立) ------------------->|
    |                                   |
    |<-- QueryObjects ------------------|  オブジェクト情報を要求
    |                                   |
    |--- ObjectList ------------------->|  オブジェクト情報を返す
    |                                   |
    |<-- QueryTelemetry ----------------|  テレメトリを要求
    |                                   |
    |--- Telemetry -------------------->|  テレメトリを返す
    |                                   |
    :  以降、サーバが 1 秒ごとに         :
    :  QueryTelemetry を送信する          :
```

KSD は軌道線（`orbitRev` / `orbitLegs`）を、**接続後の最初の `Telemetry` では全オブジェクト分**、以降は**前回送った内容から変わったオブジェクトだけ**送る。
サーバは受け取った軌道線を保持し、軌道線の付いていない `Telemetry` を受けても消さないこと。

---

### サーバ → KSD メッセージ

#### QueryObjects

接続確立時にサーバが送信する。オブジェクトメタデータ（`ObjectList`）を要求する。

```json
{
  "messageType": "QueryObjects"
}
```

#### QueryTelemetry

テレメトリ（`Telemetry`）を要求する。接続確立時と、以降 1 秒ごとにKSDへ送信される。

```json
{
  "messageType": "QueryTelemetry"
}
```

---

### KSD → サーバ メッセージ

#### ObjectList

宇宙オブジェクトのメタデータを送信する。

`QueryObjects` への応答のほか、**オブジェクトの集合か親子関係が前回の `ObjectList` から変わったとき**（機体の生成・消滅、SOI 遷移で親天体が変わったときなど）にも、KSD が `Telemetry` の直前に自分から送り直す。
`ObjectList` は現存するオブジェクトの完全な一覧である。サーバは、一覧に載っていないオブジェクトを保持している場合は削除すること。

```json
{
  "messageType": "ObjectList",
  "spaceObjects": [
    {
      "id": 1,
      "name": "Sun",
      "type": "planet",
      "parentId": null,
      "radius": 695700000.0
    },
    {
      "id": 3,
      "name": "Earth",
      "type": "planet",
      "parentId": 1,
      "radius": 6371000.0
    },
    {
      "id": 5,
      "name": "ISS",
      "type": "satellite",
      "parentId": 3,
      "radius": 50.0
    }
  ]
}
```

| フィールド | 型 | 説明 |
|---|---|---|
| `messageType` | string | 固定値 `"ObjectList"` |
| `spaceObjects[].id` | number | オブジェクト固有 ID |
| `spaceObjects[].name` | string | オブジェクト名 |
| `spaceObjects[].type` | string | 種別（`"planet"`, `"satellite"` 等） |
| `spaceObjects[].parentId` | number \| null | 親オブジェクトの ID。なければ `null` |
| `spaceObjects[].radius` | number | 天体の半径（メートル） |

#### Telemetry

各オブジェクトの現在の軌道状態を送信する。
軌道線（`orbitRev` / `orbitLegs`）は変わったオブジェクトにだけ付く（上記「接続シーケンス」参照）。

```json
{
  "messageType": "Telemetry",
  "currentTime": 1609459200,
  "selectedId": 3,
  "selectedState": { "surfaceSpeed": 7650.2, "altitude": 412000.5, "inOrbit": true },
  "spaceObjects": [
    {
      "id": 3,
      "cart": {
        "pos": { "x": 1.496e11, "y": 0.0, "z": 0.0 },
        "vel": { "x": 0.0,      "y": 29784.0, "z": 0.0 }
      },
      "kep": {
        "ep":   1609459200,
        "a":    1.496e11,
        "e":    0.0167086,
        "i":    0.0,
        "raan": 0.0,
        "argp": 102.9373,
        "ma":   100.4646
      },
      "orbitRev": 3735928559,
      "orbitLegs": [
        {
          "parentId": 1,
          "segments": [
            [[-149600000000, 0, 0], [-149577215362, -2610800215, 0]]
          ]
        }
      ]
    }
  ]
}
```

| フィールド | 型 | 説明 |
|---|---|---|
| `messageType` | string | 固定値 `"Telemetry"` |
| `currentTime` | number | シミュレーション時刻（Unix 秒） |
| `selectedId` | number \| null | KSD で選択中のオブジェクト ID。選択は常に1つだけ。未選択なら `null` |
| `selectedState` | object \| null | 選択中の宇宙機の飛行状態。宇宙機以外が選択されているとき・未選択なら `null` |
| `selectedState.surfaceSpeed` | number | 対地速度（m/s）。親天体の自転と共回転する座標系での速さで、地上では 0 |
| `selectedState.altitude` | number | 親天体の基準半径からの高度（メートル） |
| `selectedState.inOrbit` | boolean | 周回軌道上か（近点高度がカーマンラインより上） |
| `spaceObjects[].id` | number | オブジェクト ID |
| `spaceObjects[].cart.pos` | Vector3 | 位置（メートル） |
| `spaceObjects[].cart.vel` | Vector3 | 親天体に対する相対速度（m/s）。座標軸は `pos` と同じ |
| `spaceObjects[].kep.ep` | number | エポック（Unix 秒） |
| `spaceObjects[].kep.a` | number | 長半径（メートル） |
| `spaceObjects[].kep.e` | number | 離心率 |
| `spaceObjects[].kep.i` | number | 軌道傾斜角（度） |
| `spaceObjects[].kep.raan` | number | 昇交点赤経（度） |
| `spaceObjects[].kep.argp` | number | 近点引数（度） |
| `spaceObjects[].kep.ma` | number | 平均近点角（度） |
| `spaceObjects[].orbitRev` | number | 軌道線の版（省略可）。内容が変わると変わる。等しいかどうかの比較にだけ使う |
| `spaceObjects[].orbitLegs` | OrbitLeg[] | 軌道線（省略可。`orbitRev` と同時にだけ付く）。空配列は「軌道線なし」 |
| `spaceObjects[].orientation` | Orientation | 天体の向き（地軸の傾きと自転角）。天体のときだけ付く |

> **ケプラー要素について:** `kep` は親天体の赤道面を基準とした軌道要素で、`pos` とは座標系が異なる（KSD 内部で親天体の赤道傾斜などの変換を経て `pos` になる）。軌道線は `kep` から計算せず、`orbitLegs` を使うこと。

> **注意:** KSDが送出する `nan`、`-nan(ind)`、`inf`、`-inf` 等の非数値はサーバ側で JSON の `null` に変換される。

---

## REST API

**ベース URL:** `http://localhost:8080/api`

---

### GET /api/objects

登録されているすべての宇宙オブジェクトの情報とテレメトリを取得する。  
KSD への問い合わせは行わず、サーバが保持している最新値を返す。

**リクエスト**

```
GET /api/objects
GET /api/objects?orbits=true
```

| パラメータ | 既定値 | 説明 |
|---|---|---|
| `orbits` | `false` | `true` のとき各オブジェクトに軌道線（`orbitLegs`）を含める。`false` のときは `orbitRev` だけを返す |

**レスポンス（200 OK）**

```json
{
  "currentTime": 1609459200,
  "selectedId": 3,
  "selectedInOrbit": null,
  "objects": [
    {
      "id": 1,
      "name": "Sun",
      "type": "planet",
      "parentId": null,
      "radius": 695700000.0,
      "cart": {
        "pos": { "x": 0.0, "y": 0.0, "z": 0.0 },
        "vel": { "x": 0.0, "y": 0.0, "z": 0.0 }
      },
      "kep": {
        "ep": 1609459200,
        "a": 0.0,
        "e": 0.0,
        "i": 0.0,
        "raan": 0.0,
        "argp": 0.0,
        "ma": 0.0
      }
    },
    {
      "id": 3,
      "name": "Earth",
      "type": "planet",
      "parentId": 1,
      "radius": 6371000.0,
      "cart": {
        "pos": { "x": 1.496e11, "y": 0.0, "z": 0.0 },
        "vel": { "x": 0.0, "y": 29784.0, "z": 0.0 }
      },
      "kep": {
        "ep": 1609459200,
        "a": 1.496e11,
        "e": 0.0167086,
        "i": 0.0,
        "raan": 0.0,
        "argp": 102.9373,
        "ma": 100.4646
      }
    }
  ]
}
```

---

### GET /api/history

KSD で選択中の宇宙機の、対地速度と高度の履歴を取得する。

- 選択が切り替わると、サーバは保持していた履歴を破棄して記録し直す
- 記録はサーバが KSD からテレメトリを受け取るたび（1 秒ごと）。ダッシュボードを開いていなくても記録される
- 直近 1000 秒（ゲーム内時刻）ぶんだけ保持する。ゲーム内時刻が巻き戻ったら記録し直す
- 宇宙機以外が選択されているとき・未選択のときは `samples` が空

**レスポンス（200 OK）**

```json
{
  "selectedId": 100102,
  "samples": [
    { "t": 0.0, "surfaceSpeed": 0.0,   "altitude": 21.3 },
    { "t": 1.0, "surfaceSpeed": 12.4,  "altitude": 27.9 }
  ]
}
```

| フィールド | 型 | 説明 |
|---|---|---|
| `selectedId` | number \| null | 履歴の対象（KSD で選択中のオブジェクト ID） |
| `samples[].t` | number | 記録開始（選択）からのゲーム内経過時間（秒） |
| `samples[].surfaceSpeed` | number | 対地速度（m/s） |
| `samples[].altitude` | number | 高度（メートル） |

---

### GET /api/track

KSD で選択中の宇宙機の過去の軌跡を取得する。KSD の実績軌道と同じく、**軌道投入前（打ち上げ・弾道飛行）だけ**を扱う。

- サーバは KSD からテレメトリを受け取るたび（1 秒ごと）に、選択中の宇宙機の位置を親天体に固定した座標で記録する
- 選択が切り替わったとき・軌道に投入されたとき（`selectedState.inOrbit` が true）・ゲーム内時刻が巻き戻ったときに破棄する
- 前の点から 1m 未満しか動いていなければ記録しない（射点で待機している間など）。最大 3600 点

**レスポンス（200 OK）**

```json
{
  "selectedId": 100102,
  "inOrbit": false,
  "samples": [
    { "t": 1792851981.0, "parentId": 4, "fixed": { "x": -3608123.4, "y": 4150321.9, "z": 3226471.3 } }
  ]
}
```

| フィールド | 型 | 説明 |
|---|---|---|
| `selectedId` | number \| null | 軌跡の対象（KSD で選択中のオブジェクト ID） |
| `inOrbit` | boolean \| null | 選択中の宇宙機が周回軌道上か（最新値）。宇宙機以外の選択・未選択なら `null` |
| `samples[].t` | number | ゲーム内時刻（Unix 秒） |
| `samples[].parentId` | number | 記録時の親天体 ID |
| `samples[].fixed` | Vector3 | 親天体からの相対位置を、親天体の向き（`primeMeridian`, `east`, `north` の3軸）で表した成分（メートル） |

描画するときは、親天体の**現在の** `pos` と `orientation` を使って `pos + x·primeMeridian + y·east + z·north` に戻す。

---

### GET /api/objects/{id}

指定 ID の宇宙オブジェクトを取得する。KSD への問い合わせは行わず、サーバが保持している値を返す。

**リクエスト**

```
GET /api/objects/3
GET /api/objects/3?orbits=true
```

| パラメータ | 既定値 | 説明 |
|---|---|---|
| `orbits` | `false` | `true` のとき軌道線（`orbitLegs`）を含める |

**レスポンス（200 OK）**

```json
{
  "id": 3,
  "name": "Earth",
  "type": "planet",
  "parentId": 1,
  "radius": 6371000.0,
  "cart": {
    "pos": { "x": 1.496e11, "y": 0.0, "z": 0.0 },
    "vel": { "x": 0.0, "y": 29784.0, "z": 0.0 }
  },
  "kep": {
    "ep": 1609459200,
    "a": 1.496e11,
    "e": 0.0167086,
    "i": 0.0,
    "raan": 0.0,
    "argp": 102.9373,
    "ma": 100.4646
  }
}
```

**レスポンス（404 Not Found）**

指定 ID が存在しない場合。ボディなし。

---

## データモデル

### Vector3

| フィールド | 型 | 説明 |
|---|---|---|
| `x` | double | X 成分 |
| `y` | double | Y 成分 |
| `z` | double | Z 成分 |

### CartesianElements

| フィールド | 型 | 説明 |
|---|---|---|
| `pos` | Vector3 | 位置（メートル）。太陽系の座標原点からの絶対座標 |
| `vel` | Vector3 | 親天体に対する相対速度（m/s）。座標軸は `pos` と同じ |

### KeplerianElements

| フィールド | 型 | 説明 |
|---|---|---|
| `ep` | long | エポック（Unix 秒） |
| `a` | double | 長半径（メートル） |
| `e` | double | 離心率（0〜1） |
| `i` | double | 軌道傾斜角（度） |
| `raan` | double | 昇交点赤経（度） |
| `argp` | double | 近点引数（度） |
| `ma` | double | 平均近点角（度） |

### TelemetryResponse（REST レスポンス）

| フィールド | 型 | 説明 |
|---|---|---|
| `currentTime` | Long | 現在のシミュレーション時刻（Unix 秒） |
| `selectedId` | Long \| null | KSD で選択中のオブジェクト ID。未選択なら `null` |
| `selectedInOrbit` | Boolean \| null | 選択中の宇宙機が周回軌道上か（`selectedId` と同じ時点の値）。宇宙機以外の選択・未選択なら `null` |
| `objects` | SpaceObject[] | 全オブジェクトの配列 |

### SpaceObject

| フィールド | 型 | 説明 |
|---|---|---|
| `id` | long | 固有 ID |
| `name` | string | オブジェクト名 |
| `type` | string | 種別 |
| `parentId` | Long \| null | 親オブジェクト ID |
| `radius` | Double \| null | 天体の半径（メートル） |
| `cart` | CartesianElements | 直交座標要素 |
| `kep` | KeplerianElements | ケプラー要素 |
| `orbitRev` | number \| null | 軌道線の版。未受信なら `null` |
| `orbitLegs` | OrbitLeg[] | 軌道線。`?orbits=true` のときだけ含まれる |
| `orientation` | Orientation | 天体の向き。天体のときだけ含まれる |

### Orientation

天体の向き（地軸の傾きと自転角）を、`pos` と同じ座標軸の単位ベクトル3本で表す。毎回の Telemetry で最新の向きが届く。

| フィールド | 型 | 説明 |
|---|---|---|
| `north` | Vector3 | 北極の方向（地軸。傾きを含む） |
| `primeMeridian` | Vector3 | 緯度 0・経度 0 の方向（自転角を含む） |
| `east` | Vector3 | 緯度 0・東経 90 度の方向 |

緯度・経度は KSD 本体の表示と同じ約束（北緯・東経が正）。正距円筒図法（経度 0 が中央）のテクスチャを貼った球を、
`primeMeridian` が経度 0、`north` が北極、`east` が東経 90 度を向くように回せば、KSD の天体と同じ向きになる。
3本とも `pos` と同じ変換（Unreal Engine の左手系 → 表示側の座標系）をかけてから使うこと。

### OrbitLeg

軌道線の1区間。SOI（作用圏）の遷移を含む軌道では、遷移ごとに別のレッグになる。

| フィールド | 型 | 説明 |
|---|---|---|
| `parentId` | number | このレッグの親天体 ID（オブジェクト自身の `parentId` と異なることがある） |
| `segments` | number[][][] | 途切れずに結ぶ点列 `[[x, y, z], ...]` の配列。点は親天体の `pos` からの相対位置（メートル、1m 単位に丸め済み、座標軸は `pos` と同じ） |

### 軌道線の描き方

各 `segments` の点列を、`parentId` の天体の `pos` を足して折れ線（閉じない）で結ぶ。
KSD 本体の描画と同じ線になり、オブジェクトの `pos` はこの線上に乗る。

- 点にはKSD 側で必要な座標変換（親天体の赤道傾斜、打ち上げ・弾道飛行中の対地座標系表示など）が適用済み
- 楕円軌道は始点と終点が同じ点なので、閉じた線になる
- 親天体が受信データに無い場合、`parentId` が `0`（太陽系共通重心）なら原点とみなす
- `pos` と同様、座標系は Unreal Engine の左手系（Z が上）
