package jp.momiji.seed

/**
 * 投入するテストデータ（brand / product）の定義。
 *
 * 画像は [ImageUploader.IMAGES_DIR]/<ブランドフォルダ>/<ファイル> に置く。 商品名・説明・値段・在庫は
 * 画像を 1 枚ずつ見て起こした値。 実行は [main] を参照（gRPC command 経由で投入）。
 */

/**
 * 1 商品ぶんの投入データ。 [imageFile] は images/<ブランドフォルダ>/ 直下のファイル名。
 * [stock] が 0 なら入庫せず在庫切れ（在庫行が無い＝クエリ側で 0 扱い）の例になる。
 * [discontinued] を true にすると、 作成後に生産終了（DISCONTINUED）にする（ライフサイクル確認用）。
 */
internal data class ProductSeed(
    val imageFile: String,
    val name: String,
    val description: String,
    val price: Int,
    val stock: Int,
    val discontinued: Boolean = false,
)

/**
 * 1 ブランドと、 それに紐づく商品群。 [folder] は images/ 配下のフォルダ名（= ブランド名）。
 * [archived] を true にすると、 商品作成後にアーカイブ（ARCHIVED）にする（紐づく商品は残る）。
 */
internal data class BrandSeed(
    val folder: String,
    val name: String,
    val description: String,
    val products: List<ProductSeed>,
    val archived: Boolean = false,
)

internal val BRANDS =
    listOf(
        BrandSeed(
            folder = "ASUS",
            name = "ASUS",
            description = "台湾発の総合PC・周辺機器メーカー。 ROG / TUF Gaming ブランドを中心に、 マザーボードやグラフィックボードから完成品PCまで幅広く展開する。",
            products =
                listOf(
                    ProductSeed(
                        imageFile = "513YfJHU9dL._AC_SL1000_.jpg",
                        name = "ASUS TUF Gaming AX6000 デュアルバンド WiFi 6 ゲーミングルーター",
                        description = "6本のアンテナで広範囲をカバーする WiFi 6 対応ゲーミングルーター。 デュアルバンドで高速通信に対応し、 オンラインゲームを低遅延で楽しめます。",
                        price = 19800,
                        stock = 25,
                    ),
                    ProductSeed(
                        imageFile = "618yzdZXwpL._AC_SL1500_.jpg",
                        name = "ASUS ROG 開放型ゲーミングヘッドセット",
                        description = "開放型ドライバーによる広がりのあるサウンドが特徴の ROG ゲーミングヘッドセット。 着脱式マイクを備え、 長時間でも快適な装着感を実現します。",
                        price = 29800,
                        stock = 40,
                    ),
                    ProductSeed(
                        imageFile = "61PwstkLnOL._AC_SL1496_.jpg",
                        name = "ASUS ROG Ally ポータブルゲーミングPC (Ryzen AI Z2 Extreme / 24GB / 1TB)",
                        description = "AMD Ryzen AI Z2 Extreme を搭載した7型フルHD・120Hz対応のポータブルゲーミングPC。 24GBメモリと1TB SSDを備え、 持ち運びながら本格的なPCゲームをプレイできます。",
                        price = 139800,
                        stock = 0, // 在庫切れの例
                    ),
                    ProductSeed(
                        imageFile = "61gQnoarLQL._AC_SL1500_.jpg",
                        name = "ASUS Vivobook 15 ノートパソコン (Ryzen 7 / 16GB / 512GB SSD / 15.6型)",
                        description = "AMD Ryzen 7 プロセッサを搭載した15.6型フルHDノートパソコン。 16GBメモリと512GB SSDで、 日常作業からビジネスまで快適にこなせます。",
                        price = 89800,
                        stock = 15,
                    ),
                    ProductSeed(
                        imageFile = "61uyTpT3F0L._AC_SL1500_.jpg",
                        name = "ASUS TUF Gaming Z890-PRO WIFI マザーボード",
                        description = "Intel Z890チップセット搭載の TUF Gaming マザーボード。 堅牢な電源回路とWiFiを備え、 白基調のデザインが自作PCを引き立てます。",
                        price = 42800,
                        stock = 20,
                    ),
                    ProductSeed(
                        imageFile = "61xZ9PCsuXL._AC_SL1000_.jpg",
                        name = "ASUS TUF Gaming B760M-PLUS WIFI D4 マザーボード",
                        description = "Intel B760チップセット搭載のMicroATXマザーボード。 DDR4メモリとWiFiに対応し、 コストを抑えつつ安定したゲーミング環境を構築できます。",
                        price = 23800,
                        stock = 30,
                    ),
                    ProductSeed(
                        imageFile = "71SHq048GwL._AC_SL1500_.jpg",
                        name = "ASUS ROG ワイヤレスメカニカルキーボード (OLED搭載)",
                        description = "OLEDディスプレイを搭載した ROG のワイヤレスメカニカルキーボード。 付属のパームレストと鮮やかなRGBライティングで、 快適かつ没入感のあるタイピングを実現します。",
                        price = 34800,
                        stock = 18,
                    ),
                    ProductSeed(
                        imageFile = "71hhyOqLuZL._AC_SL1500_.jpg",
                        name = "ASUS TUF Gaming 200Hz Fast IPS ゲーミングモニター",
                        description = "200Hz・Fast IPSパネルを採用した TUF Gaming ゲーミングモニター。 応答速度0.3msとAMD FreeSync Premiumで、 残像の少ない滑らかな映像を描き出します。",
                        price = 39800,
                        stock = 22,
                    ),
                    ProductSeed(
                        imageFile = "71nK6fGxM6L._AC_SL1500_.jpg",
                        name = "ASUS Aura Sync アドレサブルRGB ケースファン",
                        description = "Aura Sync対応のアドレサブルRGBケースファン。 鮮やかな発光で PC 内部を彩りつつ、 効率的なエアフローで冷却をサポートします。",
                        price = 3980,
                        stock = 60,
                    ),
                    ProductSeed(
                        imageFile = "815fm88gq2L._AC_SL1500_.jpg",
                        name = "ASUS TUF Gaming 有線ゲーミングマウス",
                        description = "軽量設計の TUF Gaming 有線ゲーミングマウス。 耐久性の高いスイッチと握りやすいフォルムで、 長時間のプレイでも安定した操作を実現します。",
                        price = 3480,
                        stock = 50,
                        discontinued = true,
                    ),
                    ProductSeed(
                        imageFile = "81m-C5XpV5L._AC_SL1500_.jpg",
                        name = "ASUS DUAL GeForce RTX 5060 Ti 16GB グラフィックボード",
                        description = "GeForce RTX 5060 Ti を搭載した ASUS DUAL グラフィックボード。 16GB GDDR7メモリとPCIe 5.0対応で、 最新ゲームを高フレームレートで楽しめます。",
                        price = 84800,
                        stock = 12,
                    ),
                ),
        ),
        BrandSeed(
            folder = "MSI",
            name = "MSI",
            description = "台湾発のゲーミングデバイスメーカー。 マザーボード・グラフィックボードからゲーミングノートPCまで、 ゲーマー向けの高性能パーツを幅広く手がける。",
            products =
                listOf(
                    ProductSeed(
                        imageFile = "61j7CRRViIL._AC_SL1000_.jpg",
                        name = "MSI Cyborg 15 ゲーミングノートPC (Core i7-13620H / RTX 5060 Laptop / 16GB / 512GB / 15.6型 144Hz)",
                        description = "第13世代 Intel Core i7 と GeForce RTX 5060 Laptop GPU を搭載した15.6型ゲーミングノートPC。 144Hz フルHDディスプレイと DDR5 メモリで、 最新ゲームを滑らかに描画します。",
                        price = 179800,
                        stock = 10,
                    ),
                    ProductSeed(
                        imageFile = "71yZ+zHNPxL._AC_SL1500_.jpg",
                        name = "MSI GeForce RTX 5060 Ti VENTUS 2X OC 8GB グラフィックボード",
                        description = "GeForce RTX 5060 Ti を搭載した VENTUS 2X グラフィックボード。 8GB GDDR7 メモリと静音性に優れたデュアルファン設計で、 安定した高フレームレートを実現します。",
                        price = 69800,
                        stock = 14,
                    ),
                    ProductSeed(
                        imageFile = "81bXLFvt0CL._AC_SL1500_.jpg",
                        name = "MSI B850M GAMING PLUS WIFI6E マザーボード (AMD AM5 / DDR5)",
                        description = "AMD Ryzen 9000 シリーズに対応する Socket AM5 の MicroATX マザーボード。 DDR5 と Wi-Fi 6E に対応し、 白基調のデザインで自作PCを引き立てます。",
                        price = 28800,
                        stock = 20,
                    ),
                ),
            // ARCHIVED ブランドの例（紐づく商品は残る）。 商品作成後にアーカイブする。
            archived = true,
        ),
        BrandSeed(
            folder = "Sony",
            name = "Sony",
            description = "日本を代表する総合エレクトロニクスメーカー。 オーディオ・カメラ・スマートフォンまで、 高い技術力に裏打ちされた製品を世界中に展開する。",
            products =
                listOf(
                    ProductSeed(
                        imageFile = "41b9k9u3L+L._AC_SL1100_.jpg",
                        name = "Sony WF-1000XM5 ワイヤレスノイズキャンセリングイヤホン",
                        description = "業界最高クラスのノイズキャンセリングを備えた完全ワイヤレスイヤホン。 高音質と快適な装着感で、 通勤から在宅ワークまで静寂に包まれた音楽体験を届けます。",
                        price = 39800,
                        stock = 30,
                    ),
                    ProductSeed(
                        imageFile = "51INgO9AVdL._AC_SL1100_.jpg",
                        name = "Sony SRS-XB100 ポータブルワイヤレススピーカー",
                        description = "手のひらサイズのコンパクトなワイヤレススピーカー。 防水・防塵対応で持ち運びやすく、 アウトドアでもパワフルなサウンドを楽しめます。",
                        price = 8800,
                        stock = 45,
                    ),
                    ProductSeed(
                        imageFile = "51kA5kjolaL._AC_SL1001_.jpg",
                        name = "Sony α7 III ミラーレス一眼カメラ ボディ (ILCE-7M3)",
                        description = "有効約2420万画素のフルサイズミラーレス一眼カメラ（ボディ単体）。 高速AFと優れた高感度性能で、 静止画から動画まで幅広い撮影に応えます。",
                        price = 219800,
                        stock = 0, // 在庫切れの例
                    ),
                    ProductSeed(
                        imageFile = "51sgfTld29L._AC_SL1000_.jpg",
                        name = "Sony ULT FIELD 1 ポータブルワイヤレススピーカー",
                        description = "ULT ボタンで迫力の重低音を楽しめるポータブルワイヤレススピーカー。 防水・防塵・防錆に対応し、 ショルダーストラップ付きで持ち運びも快適です。",
                        price = 14800,
                        stock = 28,
                    ),
                    ProductSeed(
                        imageFile = "617bgur6fRL._AC_SL1000_.jpg",
                        name = "Sony RX100 VII コンパクトデジタルカメラ (DSC-RX100M7)",
                        description = "1.0型センサーと ZEISS レンズを搭載した高性能コンパクトデジタルカメラ。 高速AFと24-200mm相当のズームで、 ポケットサイズながら本格的な撮影が可能です。",
                        price = 149800,
                        stock = 9,
                        discontinued = true,
                    ),
                    ProductSeed(
                        imageFile = "81bEIx8-ZZL._AC_SL1500_.jpg",
                        name = "Sony Xperia 1 スマートフォン (バーガンディ)",
                        description = "ZEISS 監修のトリプルカメラを備えたフラッグシップスマートフォン。 有機ELディスプレイと高い処理性能で、 撮影・ゲーム・動画視聴を上質に楽しめます。",
                        price = 159800,
                        stock = 12,
                    ),
                    ProductSeed(
                        imageFile = "51VMerPDasL._AC_SL1500_.jpg",
                        name = "Sony PlayStation Portal リモートプレーヤー",
                        description = "PS5 のゲームを Wi-Fi 経由でリモートプレイできる専用リモートプレーヤー。 8型フルHD液晶と DualSense の機能を備え、 離れた部屋でも快適にプレイできます。",
                        price = 29980,
                        stock = 20,
                    ),
                    ProductSeed(
                        imageFile = "51XfJ5EK19L._AC_SL1100_.jpg",
                        name = "Sony WH-1000XM5 ワイヤレスノイズキャンセリングヘッドホン",
                        description = "業界最高クラスのノイズキャンセリングを搭載したワイヤレスヘッドホン。 上質な装着感と高音質で、 移動中も自宅でも没入感のある音楽体験を届けます。",
                        price = 49800,
                        stock = 25,
                    ),
                    ProductSeed(
                        imageFile = "61+MAKnnUgL._AC_SL1500_.jpg",
                        name = "Sony BRAVIA 4K液晶テレビ (Google TV)",
                        description = "Google TV を搭載した4K液晶テレビ BRAVIA。 高精細な映像と豊富な配信アプリ対応で、 映画もゲームも大画面で楽しめます。",
                        price = 89800,
                        stock = 10,
                    ),
                    ProductSeed(
                        imageFile = "617Zlp1QeEL._AC_SL1000_.jpg",
                        name = "Sony VLOGCAM ZV-1 II コンパクトカメラ",
                        description = "動画配信・Vlog撮影に最適化されたコンパクトカメラ。 広角ズームレンズと高性能マイクを備え、 手軽に高品質な動画を撮影できます。",
                        price = 98800,
                        stock = 12,
                    ),
                    ProductSeed(
                        imageFile = "61E91mtBnTL._AC_SL1500_.jpg",
                        name = "Sony PlayStation 3 本体 500GB",
                        description = "根強い人気を誇る据置型ゲーム機 PlayStation 3（500GBモデル）。 豊富なゲームタイトルとブルーレイ再生に対応します。",
                        price = 24800,
                        stock = 8,
                    ),
                    ProductSeed(
                        imageFile = "61aZwB-EH0L._AC_SL1500_.jpg",
                        name = "Sony PlayStation 5 デジタルエディション",
                        description = "高速SSDと4K・レイトレーシングに対応した次世代ゲーム機 PlayStation 5（デジタルエディション）。 ディスクドライブ非搭載で、 ダウンロード版タイトルを楽しめます。",
                        price = 59980,
                        stock = 14,
                    ),
                    ProductSeed(
                        imageFile = "61klVqt8XJL._AC_SL1500_.jpg",
                        name = "Sony PlayStation 4 Pro 1TB",
                        description = "4K・HDR に対応した高性能ゲーム機 PlayStation 4 Pro（1TB）。 美しい映像と安定した処理性能で、 幅広いタイトルを快適にプレイできます。",
                        price = 39800,
                        stock = 10,
                    ),
                    ProductSeed(
                        imageFile = "61sjWix2kvL._AC_SL1000_.jpg",
                        name = "Sony MDR-CD900ST モニターヘッドホン",
                        description = "プロの制作現場で長年定番のスタジオモニターヘッドホン。 正確でフラットな音質再現により、 モニタリングやミキシングに応えます。",
                        price = 16500,
                        stock = 30,
                    ),
                    ProductSeed(
                        imageFile = "71XHp83gvyL._AC_SL1500_.jpg",
                        name = "Sony PlayStation Vita 3G/Wi-Fi モデル",
                        description = "有機ELタッチスクリーンを備えた携帯型ゲーム機 PlayStation Vita（3G/Wi-Fiモデル）。 高精細な画面で、 外出先でも本格的なゲームを楽しめます。",
                        price = 24800,
                        stock = 15,
                    ),
                ),
        ),
        BrandSeed(
            folder = "Panasonic",
            name = "Panasonic",
            description = "日本を代表する総合家電メーカー。 生活家電から美容・映像・カメラ（LUMIX）まで、 暮らしを支える幅広い製品を展開する。",
            products =
                listOf(
                    ProductSeed(
                        imageFile = "61+0ArsNG7L._AC_SL1500_.jpg",
                        name = "Panasonic ラムダッシュ メンズシェーバー (3枚刃 / WET・DRY)",
                        description = "リニアモーター駆動の3枚刃で深剃りを実現するメンズシェーバー。 WATER THROUGH 構造で水洗いでき、 お風呂剃りにも対応します。",
                        price = 22800,
                        stock = 25,
                    ),
                    ProductSeed(
                        imageFile = "61BjuM83obL._AC_SL1500_.jpg",
                        name = "Panasonic ヘアドライヤー ナノケア",
                        description = "高浸透ナノイーを搭載したヘアドライヤー ナノケア。 髪にうるおいを与えながら素早く乾かし、 スカルプ・スキンモードも備えます。",
                        price = 27800,
                        stock = 20,
                    ),
                    ProductSeed(
                        imageFile = "61pezdErQCL._AC_SL1500_.jpg",
                        name = "Panasonic VIERA 32V型 液晶テレビ (TH-32J300)",
                        description = "使いやすさにこだわった32V型ハイビジョン液晶テレビ VIERA。 コンパクトで、 寝室や一人暮らしの部屋にちょうど良いサイズです。",
                        price = 39800,
                        stock = 12,
                    ),
                    ProductSeed(
                        imageFile = "71JV0tPMgAL._AC_SL1500_.jpg",
                        name = "Panasonic LUMIX S9 ミラーレスカメラ (18-40mm レンズキット)",
                        description = "軽量コンパクトなフルサイズミラーレスカメラ LUMIX S9（18-40mmレンズキット）。 リアルタイムLUTで色作りを楽しめ、 日常も旅も上質に切り取れます。",
                        price = 199800,
                        stock = 7,
                    ),
                    ProductSeed(
                        imageFile = "81CbBTzczYL._AC_SL1500_.jpg",
                        name = "Panasonic EVOLTA NEO 単3形アルカリ乾電池",
                        description = "パナソニック最高峰の長もちアルカリ乾電池 EVOLTA NEO（単3形）。 リモコンや時計、 おもちゃなど幅広い機器に使えます。",
                        price = 1480,
                        stock = 80,
                    ),
                ),
        ),
        BrandSeed(
            folder = "マキタ",
            name = "マキタ",
            description = "日本発の電動工具メーカー。 プロの現場で信頼される充電式インパクトドライバやクリーナーなど、 高品質な電動工具を世界中に届ける。",
            products =
                listOf(
                    ProductSeed(
                        imageFile = "41qJID9xkML._AC_SL1000_.jpg",
                        name = "マキタ 充電式クリーナー（本体のみ）",
                        description = "軽量で取り回しの良いマキタの充電式クリーナー（本体のみ）。 サッと使えるコードレス設計で、 家庭から現場の清掃まで活躍します。",
                        price = 13800,
                        stock = 30,
                    ),
                    ProductSeed(
                        imageFile = "516OSOAzsdL._AC_SL1000_.jpg",
                        name = "マキタ 充電式ディスクグラインダー（本体のみ）",
                        description = "金属の研削・切断に使えるマキタの充電式ディスクグラインダー（本体のみ）。 コードレスで取り回しやすく、 現場作業を効率化します。",
                        price = 12800,
                        stock = 22,
                    ),
                    ProductSeed(
                        imageFile = "61XUSS+X3ZL._AC_SL1500_.jpg",
                        name = "マキタ 充電式インパクトドライバ ブラシレス（本体のみ）",
                        description = "ブラシレスモーター搭載のマキタ充電式インパクトドライバ（本体のみ）。 高いトルクと耐久性で、 ネジ締めや組立作業をパワフルにこなします。",
                        price = 24800,
                        stock = 18,
                    ),
                    ProductSeed(
                        imageFile = "61Xpi2fZC0L._AC_SL1000_.jpg",
                        name = "マキタ インパクトドライバ用 ビットセット",
                        description = "インパクトドライバに対応したマキタのビットセット。 各種ドライバービット・ドリルを収納ケースにまとめ、 幅広い作業に対応します。",
                        price = 3980,
                        stock = 50,
                    ),
                    ProductSeed(
                        imageFile = "61wY395V8mL._AC_SL1280_.jpg",
                        name = "マキタ 急速充電器 DC18RF",
                        description = "マキタ 14.4V〜18V バッテリ対応の急速充電器 DC18RF。 短時間で充電でき、 冷却機能でバッテリをいたわります。",
                        price = 8800,
                        stock = 35,
                    ),
                    ProductSeed(
                        imageFile = "61wr4+tTzoL._AC_SL1500_.jpg",
                        name = "マキタ 充電式ブロワ（本体のみ）",
                        description = "落ち葉やホコリの吹き飛ばしに便利なマキタの充電式ブロワ（本体のみ）。 コードレスで軽快に扱え、 清掃作業を効率化します。",
                        price = 9800,
                        stock = 24,
                    ),
                    ProductSeed(
                        imageFile = "714xqdm6LLL._AC_SL1500_.jpg",
                        name = "マキタ 充電式インパクトレンチ セット（バッテリ2個＋急速充電器）",
                        description = "強力な締付トルクを誇るマキタの充電式インパクトレンチ。 バッテリ2個・急速充電器・ケースが付属し、 すぐに現場で使えます。",
                        price = 54800,
                        stock = 9,
                    ),
                    ProductSeed(
                        imageFile = "71r6m96KeQL._AC_SL1181_.jpg",
                        name = "マキタ 電気マルノコ 165mm",
                        description = "木材の切断に定番のマキタ電気マルノコ（165mm）。 安定した切断性能で、 DIYから本格的な木工作業まで対応します。",
                        price = 18800,
                        stock = 16,
                    ),
                    ProductSeed(
                        imageFile = "81-sIT2ZzRL._AC_SL1500_.jpg",
                        name = "マキタ 充電式LEDワークライト（USB付）",
                        description = "バッテリを装着して使うマキタの充電式LEDワークライト。 USB給電にも対応し、 現場や停電時の明かりとして活躍します。",
                        price = 4980,
                        stock = 40,
                    ),
                    ProductSeed(
                        imageFile = "81AW5iIUBcL._AC_SL1500_.jpg",
                        name = "マキタ 充電式ブロワ セット（バッテリ＋急速充電器＋収納バッグ）",
                        description = "マキタの充電式ブロワに、 バッテリ・急速充電器・収納バッグが付いたセット。 届いてすぐに清掃作業を始められます。",
                        price = 16800,
                        stock = 13,
                    ),
                    ProductSeed(
                        imageFile = "81LEIDMAn6L._AC_SL1500_.jpg",
                        name = "マキタ 充電式ファン（卓上）",
                        description = "持ち運びやすいマキタの充電式ファン（卓上）。 コードレスで首振り・風量調整に対応し、 現場や屋外を涼しくします。",
                        price = 12800,
                        stock = 20,
                    ),
                ),
        ),
    )
