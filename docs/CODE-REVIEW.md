# Code Review — ecommerce-project (BotLand)

日期：2026-09-07
範圍：整個專案（`src/main/java` + 前端 `ecommerce-app-client` + Docker/設定）
方法：`/code-review` 掃 diff + runtime 實測驗證（container: `ecommerce-app-backend` :8083 / `unruffled_ishizaka` :5173 / `ecommerce_sql` :33061）

> 注意：`/code-review` 的初步推論說「角色規則太嚴，所有人含 admin 都被 403」，**實測相反** —— 規則完全沒生效（見 #1）。

---

## 🔴 Critical — 安全，需優先處理

### 1. 角色授權完全失效：任何有效 JWT = 全站管理權限

`SpringSecurityConfig` 的 `hasRole("ADMIN")` / `hasAnyRole` / `denyAll` 完全沒作用。

實測（USER token，roles = `["USER"]`）：

| 請求 | 設定規則 | 實際結果 |
|---|---|---|
| `GET /orders/all-orders-items` | ADMIN only | **200**（拿到全部客戶訂單） |
| `PUT /orders/changeStatus/7` | ADMIN only | **200**（可改任意訂單狀態） |
| `POST /products/add/Product` | ADMIN only | 進到 controller（500，非 403） |
| 有效 token 打未定義路徑 `/nope/nope` | `denyAll` | **404**（非 403） |
| 無 token | — | 403 ✓（唯一有效的門檻） |

根因：
- `security/JwtAuthorizationFilter.java` 第 ~52 行：`new UsernamePasswordAuthenticationToken(email, "", new ArrayList<>())` —— **authorities 是空 list**
- `security/JwtUtil.java` `createToken()` —— 從沒把 roles 寫進 token
- 疑似 `@EnableWebMvc`（見 #11）干擾 Spring Security filter chain，導致 `authorizeRequests()` 路由規則整組沒被評估

修法方向：
- filter 從 DB（`CustomUserDetailsService`）或 token claim 取角色 → `new SimpleGrantedAuthority("ROLE_" + role)`
- `createToken()` 加入 `roles` claim
- 移除 `EcommerceFilter` 的 `@EnableWebMvc`
- 加整合測試：`USER token 打 ADMIN 端點應回 403`、`無 token 打受保護端點應回 401/403`

---

### 2. 檔案上傳路徑穿越 → 任意檔案寫入

`controller/ProductController.java` `saveFileToServer()`：
```java
new FileOutputStream(filePath + imageName)   // imageName 直接來自 client
```

實測：`imageName=../pwned.txt` → 成功把上傳內容寫到 images 目錄的**外層**目錄（回 200）。配合 #1，**任何登入者**（非只 admin）都能利用 → 可覆寫應用檔案。

修法方向：
- `Paths.get(baseDir).resolve(FilenameUtils.getName(imageName)).normalize()`，並驗證結果仍在 `baseDir` 底下
- 檔名改用 server 端產生的 UUID，不信任 client 傳來的檔名

---

### 3. JWT 簽章金鑰硬編在原始碼

`security/JwtUtil.java`：`private String jwt_key = "botlandsecretkey";`

已進 git 版控。字串短、可暴力破解 → 任何人可自簽任意 token（含偽造 admin）。

修法方向：移到環境變數 / secret manager；長度 ≥ 256-bit；用 `Keys.hmacShaKeyFor(...)`（需搭配 #6 升級 jjwt）。

---

### 4. IDOR：無資源擁有權檢查

`controller/OrderController.java` `getOrdersWithItemsByUserId(@PathVariable int userId)`
`controller/UserController.java` `/users/get/user/{userId}`

只看角色，不檢查 `{userId}` 是否為登入者本人。即使 #1 修好，USER=7 仍可讀 USER=8 的訂單與個資（姓名、email、地址）。

修法方向：controller 比對 `SecurityContextHolder` 的 principal email 與 path userId 對應的 user，或改成「取自己的訂單」端點（userId 由 token 決定，不吃 path）。

---

## 🟠 High

### 5. JWT 有效期約 6.8 年

`security/JwtUtil.java`：
```java
private long tokenValidTime = 60*60*1000;                       // 註解本意：60 分鐘
... TimeUnit.MINUTES.toMillis(tokenValidTime) ...               // 把「毫秒」當「分鐘」再轉毫秒
```

實測登入拿到的 token `exp` 落在 2033 年。且無 refresh token 機制。

修法：`tokenValidTime` 設為分鐘數（如 `60`），或直接算毫秒不要再過 `TimeUnit`。

---

### 6. jjwt 0.9.1（pom.xml）

2018 年版本，已停止維護、有已知 CVE，Java 11 需靠 `jaxb-api` 硬撐。

修法：升級到 `io.jsonwebtoken:jjwt-api` / `jjwt-impl` / `jjwt-jackson` 0.12.x，API 需改寫。

---

### 7. CORS 設定脆弱

`config/EcommerceFilter.java`：
```java
String allowOrgin = System.getenv("ALLOWED_ORIGIN");
config.addAllowedOrigin(allowOrgin);
config.setAllowCredentials(true);
```

- 後端 `Dockerfile` 有 `ENV ALLOWED_ORIGIN=""` → `addAllowedOrigin("")`
- 未設環境變數時 `System.getenv` 回 `null`
- 安全的預設 `http://localhost:5173` 被註解掉了
- 目前能運作只因 docker-compose 有塞值；本機直接跑 / 跑測試會壞
- `setAllowCredentials(true)` 之下絕不可用萬用字元

修法：從 `application-*.properties` 讀允許清單（可多個），提供合理預設，缺值時 fail-fast 或用開發預設。

---

### 8. multipart 上限未設 → 上傳大圖靜默失敗

`application.properties` 沒有設定 multipart 大小，套 Spring Boot 預設 **1 MB**。上傳 >1MB 圖片 → 後端回 500 `MaxUploadSizeExceededException`，前端**完全無提示**（表單不清空，使用者以為沒反應）。

修法：
```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```
並在前端 `AddProduct.jsx` 處理 error response 顯示訊息。

---

### 9. DB 帳密與環境設定

`application.properties`：
- `spring.datasource.primary.username=root` / `password=root` 明碼進版控
- 沒有 `application-dev` / `application-prod` profile 分離
- 三段被註解的 datasource URL 混雜

修法：帳密走環境變數；建立 profile；`.properties` 只留非敏感預設。

---

### 10. 前端 token / 授權處理

- token 存 `localStorage`（XSS 可竊）
- `utils/ApiFuncs.jsx` 的 axios instance 沒有 response 攔截器：token 過期 / 401 時不會自動登出或導回登入頁
- `auth/RequireAuth.jsx` 只檢查 `localStorage` 有沒有 `botland-userId`
- `layout/NavBar.jsx` 用 `localStorage` 的 `botland-userRole` 決定是否顯示 Admin 連結 —— client 自行改值就能看到 admin UI（真正該擋的後端又沒擋，見 #1）

修法：加 axios 401 攔截器 → 清 localStorage + redirect；前端角色控制只當 UX，安全靠後端。

---

## 🟡 Medium

| # | 位置 | 問題 |
|---|---|---|
| 11 | `config/EcommerceFilter.java` | `@EnableWebMvc` 會關掉 Spring Boot MVC autoconfig（訊息轉換器、靜態資源對應等），幾乎確定非預期，且疑似連帶弄壞 security / CORS filter chain |
| 12 | `security/JwtAuthorizationFilter.java` | invalid token 進 catch 寫完 403 response 後，方法末尾**又呼叫一次** `filterChain.doFilter`；`claims != null & jwtUtil.isExpiredClaims(claims)` 用了 bitwise `&`；`isExpiredClaims()` 命名與語意相反（未過期時回 true） |
| 13 | `product/ShoppingCart.jsx`, `product/AddProduct.jsx` | production UI 裡有 `alert("Submit Order!")`、`alert("Add Product Submit")`；結帳流程寫死 `await delay(10)` —— 每次都假的等 10 秒「付款處理中」 |
| 14 | `controller/ProductController.java` `getAllProducts()` | 每次商品列表都把**所有**商品圖檔從磁碟讀出、base64 編碼塞進回應；無伺服器端分頁 → N 次 file IO + 巨大 payload |
| 15 | `utils/ApiFuncs.jsx` `registerUser()` | `if (error.reeponse && error.response.data)` 拼錯 `reeponse`，這段錯誤處理永遠不觸發，一律落到泛用訊息 |
| 16 | `controller/ProductController.java` add / update | 上傳無檔案類型 / MIME 驗證，可傳任意檔案 |
| 17 | `controller/ProductController.java` | `new BigDecimal(productPrice)` 對 client 傳來的字串不驗證 → `NumberFormatException` → 500 |
| 18 | 圖片儲存 | 存在 container 本地檔案系統（`/clientside/...`），沒有 volume 掛載時 container 重啟即遺失 |

---

## 🔵 Low / 整潔

- **版控雜物**：`.project` / `.classpath` / `.settings/`（Eclipse 專案檔）進了 git；`bin/` 未被忽略。`.gitignore` 規則寫錯：`*target`（應為 `target/`）、`*.mvn`、`*.classpath`
- **debug 輸出**：`System.out.println` 散落在 `EcommerceFilter`（含 typo 變數名 `allowOrgin`）、`OrderController`（`System.out.println("order"+order)`）
- **前端 Dockerfile**：正式 image 用 `npm run dev`（Vite dev server）當服務，應 `npm run build` 後用 nginx / 靜態伺服
- **後端 Dockerfile**：非 multi-stage，需先在本機 `mvn package` 產出 jar
- **測試**：只有預設的 `EcommerceProjectApplicationTests`（context load），等於沒有測試
- **其他**：`App.jsx` 一堆未使用 import（`HashRouter`、`reactLogo`、`viteLogo`…）；package 命名 `com.example.ecommerceproject.service.Impl` 大寫 `I`；`ProductCard.jsx` 圖片一律硬加 `data:image/png;base64` 前綴（jpg 靠瀏覽器嗅探）；`JwtUtil` 的 `TOKEN_PREFIX` / `tokenValidTime` 可 `@Value` 化

---

## 建議修復順序

1. **#1 角色授權**（連帶 #11 `@EnableWebMvc`、#4 IDOR）—— 目前等於完全沒有授權
2. **#2 路徑穿越**、**#3 金鑰外部化**
3. #5 token 過期時間、#7 CORS、#8 上傳上限
4. #6 jjwt 升級、#9 設定 profile 分離
5. #10 前端 401 處理
6. 其餘 Medium / Low

---

## 附錄：已知可用測試帳號（本機環境）

- ADMIN：`testadmin@kmail.com` / `Test1234!`（測試時建立並手動升級 role）
- 一般註冊：Sign-Up 或 `POST /auth/register-new-user`，一律 USER role
- MySQL：`docker exec -it ecommerce_sql mysql -uroot -proot ecommerce_db`
