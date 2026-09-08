# BotLand — ecommerce-project

機器人購物網站的練習專案。前後端分離，含一般使用者購物流程與管理後台。

## 功能

**使用者**
- 註冊 / 登入（JWT）
- 瀏覽商品、依類型（Household / Outdoor / Office）篩選、看單一商品
- 加入購物車、結帳下單、查看自己的訂單

**管理者（ADMIN）**
- 商品上架 / 編輯（含圖片上傳）
- 查看與更新所有訂單狀態

## 技術

| 層 | 技術 |
|---|---|
| 後端 | Java 11、Spring Boot 2.6.5、Spring Security（JWT, jjwt 0.9.1）、MyBatis-Plus |
| 資料庫 | MySQL 8 |
| 前端 | React 18 + Vite、React Router、Bootstrap / react-bootstrap、axios |
| 部署 | Docker（後端 jar image、前端 Vite、MySQL）|

## 專案結構

```
src/main/java/com/example/ecommerceproject/
  controller/   REST endpoints（Auth / Product / Order / User）
  service/      商業邏輯（介面 + service/Impl）
  mapper/       MyBatis-Plus mapper
  entity/ model/ DTO 與 entity
  security/     JwtUtil、JwtAuthorizationFilter、SpringSecurityConfig、CustomUserDetailsService
  config/       EcommerceFilter（CORS）
src/main/resources/application.properties
```

前端 client（`ecommerce-app-client`）為獨立 repo / 目錄，主要元件在 `src/components/`（auth / product / order / cart / admin / layout）。

## 執行（Docker）

三個 container：

| 服務 | Port |
|---|---|
| 前端 | http://localhost:5173 |
| 後端 API | http://localhost:8083 |
| MySQL | localhost:33061 |

後端需要環境變數：`ALLOWED_ORIGIN`（CORS 來源）、DB 連線由 `application.properties` 的 `db:3306/ecommerce_db` 指定。
後端 image 需先本機 `mvn package` 產出 `target/*.jar`。

## 主要 API

| Method | Path | 權限 |
|---|---|---|
| POST | `/auth/register-new-user` | 公開 |
| POST | `/auth/login` | 公開 |
| GET | `/products/get/allProducts` | 公開 |
| GET | `/products/get/product/{id}` | 公開 |
| POST | `/products/add/Product` | ADMIN |
| PUT | `/products/update/{id}` | ADMIN |
| POST | `/orders/save/order` | USER / ADMIN |
| GET | `/orders/get/user-order/{userId}` | USER / ADMIN |
| GET | `/orders/all-orders-items` | ADMIN |
| PUT | `/orders/changeStatus/{id}` | ADMIN |

認證：登入取得 JWT，後續請求帶 `Authorization: Bearer <token>`。

## 已知問題

見 [docs/CODE-REVIEW.md](docs/CODE-REVIEW.md) —— 目前**角色授權未實際生效**、上傳有路徑穿越、JWT 金鑰硬編等，尚待修復。
