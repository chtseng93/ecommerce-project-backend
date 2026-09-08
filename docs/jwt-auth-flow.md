# JWT 驗證流程

## 登入發 Token

`POST /auth/login`（`AuthController.java:60`）

1. `AuthenticationManager.authenticate()` → `CustomUserDetailsService.loadUserByUsername()` 用 email 從 DB 撈使用者，`BCryptPasswordEncoder` 比對密碼。
2. 成功後 `JwtUtil.createToken()`（`JwtUtil.java:44`）：
   - `subject` = email，另放 `firstName` / `lastName`（**沒有放 roles**）
   - HS256 簽章，金鑰寫死 `"botlandsecretkey"`
   - 有效期 `TimeUnit.MINUTES.toMillis(60*60*1000)` ≈ **6.8 年**（註解本意是 1 小時，寫錯了）
3. 回傳 `UserRes`，前端收 `jwtToken` 自行保存，之後放 `Authorization: Bearer <token>`。

## 每次請求驗證

`JwtAuthorizationFilter`（`OncePerRequestFilter`，掛在 `UsernamePasswordAuthenticationFilter` 之前，session 為 `STATELESS`）（`JwtAuthorizationFilter.java:37`）

1. 從 `Authorization` header 取 `Bearer ` 後面的 token；沒有就直接放行（匿名，只能過 `permitAll`）。
2. `parseClaimsJws()` 驗簽 + 解析；簽章錯或過期會丟例外。
3. `claims != null & isExpiredClaims(claims)` 為真時（`isExpiredClaims` 其實回傳「還沒過期」，命名相反），建立 `UsernamePasswordAuthenticationToken(email, "", new ArrayList<>())` 塞進 `SecurityContextHolder`。
4. `SpringSecurityConfig` 依 URL 做 `hasRole` / `hasAnyRole` 授權（`SpringSecurityConfig.java:39`）。

## 這個流程目前壞掉的地方

- **權限一律失敗**：filter 建 auth 時 authorities 傳空 `ArrayList`，token 裡也沒有 roles。凡是需要 `hasRole("USER"/"ADMIN")` 的 endpoint（`/orders/**`、`/products/add/**`、`/users/get/user/**`）對所有人都 403，只有 `permitAll` 的能用。
- **catch 後重複 `filterChain.doFilter()`**：驗證失敗寫完 403 JSON 後，最外層又呼叫一次 `doFilter`，response 已 commit 會再爆錯。
- `&` 應為 `&&`；`isExpiredClaims` 命名與回傳相反。
- 金鑰硬編碼、Token 效期算錯（≈6.8 年）。

## 為什麼要自己寫 login controller，而不是靠 Spring Security filter chain

Spring Security 內建在 filter chain 裡的登入（`UsernamePasswordAuthenticationFilter` / `.formLogin()`）是給 **session + 表單 + redirect** 的傳統網站用的：它攔 `POST /login`、驗完把 `Authentication` 存進 HttpSession、然後 302 導頁。

這個專案是 **前後端分離 + STATELESS + JWT**，那套不合用：

- 要回的是 **JSON body（含 token、使用者資料）**，不是 redirect。
- STATELESS 之下不存 session，內建 filter 驗完沒地方放。
- 這份 config 沒開 `.formLogin()`，所以 chain 裡的 `UsernamePasswordAuthenticationFilter` 其實是空轉的，JWT filter 只是借它的位置當定位點。

真正做驗證的是 `AuthenticationManager`。內建 filter 也只是「從 request 撈帳密 → 呼叫 `AuthenticationManager` → 處理結果」的薄殼。`AuthController.login()` 就是把這個薄殼自己手寫一遍，換掉不適合 SPA 的 session/redirect 行為，改成自己組 token 回 JSON。

另一種寫法是自訂 filter 取代 `UsernamePasswordAuthenticationFilter`，在 successHandler 裡寫 JWT 回應——「留在 chain 裡」但要多寫 success/failure handler。用 `@RestController` 手動呼叫 `AuthenticationManager` 更直接、response DTO 好塑形，是 JWT 專案最常見的做法。
