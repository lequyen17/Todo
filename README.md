# Todo

Ứng dụng Todo gồm:

| Service    | Image                    | Vai trò                                        |
| ---------- | ------------------------ | ---------------------------------------------- |
| `nginx`    | `nginx:1.27-alpine`      | Cổng duy nhất mở ra ngoài (port 80)            |
| `ui`       | `103487/todo-ui:v1.0`    | Giao diện React                                |
| `api`      | `103487/todo-api:v1.0`   | Backend Spring Boot                            |
| `postgres` | `postgres:17-alpine`     | Cơ sở dữ liệu                                  |
| `redis`    | `redis:7-alpine`         | OTP, rate limit, danh sách token đã thu hồi    |

Request vào `http://<host>/api/...` được chuyển sang `api`, còn lại được chuyển sang `ui`.

## Yêu cầu

- Docker và Docker Compose v2 (`docker compose version`)
- Port 80 trên máy đang trống

## Cách 1: Chạy bằng image có sẵn trên Docker Hub

Không cần mã nguồn. Chỉ cần 3 file sau trong cùng một thư mục:

```
todo/
├── docker-compose.yml
├── .env                 # tạo từ .env.example
└── nginx/
    └── nginx.conf
```

Các bước:

```bash
# 1. Tạo file cấu hình rồi sửa giá trị (xem mục "Cấu hình .env")
cp .env.example .env

# 2. Tải image về
docker compose pull

# 3. Chạy (--no-build: không build lại vì máy không có mã nguồn)
docker compose up -d --no-build
```

Mở http://localhost.

## Cách 2: Build từ mã nguồn

Dùng khi bạn sửa code và muốn chạy bản vừa sửa:

```bash
cp .env.example .env
docker compose up -d --build
```

Để đẩy image mới lên Docker Hub: tăng tag trong `docker-compose.yml` (vd. `v1.0` → `v1.1`), rồi chạy:

```bash
docker login
docker compose build api ui
docker compose push api ui
```

Phải ghi rõ `api ui`. Nếu không, lệnh `push` sẽ cố đẩy cả các image chính thức `postgres`, `redis`, `nginx` và báo lỗi.

## Cấu hình `.env`

| Biến                | Bắt buộc | Ý nghĩa |
| ------------------- | -------- | ------- |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Có | Tài khoản database. Postgres chỉ dùng các giá trị này **lần chạy đầu tiên** (xem phần Xử lý sự cố) |
| `JWT_SECRET`        | Có | Chuỗi ngẫu nhiên ≥ 32 ký tự, tạo bằng `openssl rand -base64 48` |
| `APP_FRONTEND_URL`  | Có | URL người dùng mở trên trình duyệt (vd. `http://localhost`), dùng để tạo link đặt lại mật khẩu trong email |
| `LOGIN_OTP_ENABLED` | Không | `true` (mặc định): đăng nhập phải nhập thêm mã OTP gửi qua email. `false`: chỉ cần email + mật khẩu |
| `MAIL_ENABLED`      | Có | `false`: không gửi email thật, mã OTP và link reset được in ra log (tiện khi chạy thử) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | Khi `MAIL_ENABLED=true` | Thông tin SMTP. Với Gmail: `smtp.gmail.com`, port `587`, mật khẩu là [App Password](https://myaccount.google.com/apppasswords) |
| `GOOGLE_CLIENT_ID`  | Không | Để trống thì ẩn nút "Sign in with Google" |

### Bật đăng nhập Google

1. Vào [Google Cloud Console → Credentials](https://console.cloud.google.com/apis/credentials), tạo **OAuth client ID** loại **Web application**.
2. Ở **Authorized JavaScript origins**, thêm đúng URL bạn mở ứng dụng, vd. `http://localhost`. Khi deploy lên domain thật thì thêm cả `https://your-domain.com`.
3. Điền client ID vào `GOOGLE_CLIENT_ID` trong `.env` rồi chạy `docker compose up -d` để áp dụng.

## Lệnh thường dùng

```bash
docker compose ps                 # trạng thái các container
docker compose logs -f api        # xem log backend (cả OTP khi MAIL_ENABLED=false)
docker compose restart api        # khởi động lại một service
docker compose down               # dừng và xoá container (dữ liệu vẫn giữ)
docker compose down -v            # dừng và XOÁ LUÔN dữ liệu postgres + redis
```

Tài liệu API (Swagger): http://localhost/api/swagger-ui.html

### Cập nhật lên phiên bản mới

1. Sửa tag image trong `docker-compose.yml`.
2. Chạy:

```bash
docker compose pull
docker compose up -d --no-build
```

Schema database được tạo và cập nhật tự động khi `api` khởi động, không cần chạy migration bằng tay.

## Xử lý sự cố

**Không mở được http://localhost / lỗi `port is already allocated`**
Port 80 đang bị chương trình khác dùng (IIS, Skype, một web server khác...). Có thể đổi sang port khác, vd. trong `docker-compose.yml` sửa `"80:80"` thành `"8080:80"`. Khi đó cũng phải cập nhật `APP_FRONTEND_URL` và Google JavaScript origin thành `http://localhost:8080`.

**`api` khởi động lỗi hoặc liên tục restart**
Xem `docker compose logs api`. Nguyên nhân hay gặp:
- Thiếu biến trong `.env`.
- `JWT_SECRET` ngắn hơn 32 ký tự.
- Postgres chưa sẵn sàng. Lỗi này tự hết sau vài giây, chạy lại `docker compose up -d`.

**Đổi `POSTGRES_PASSWORD` xong thì `api` không kết nối được database**
Postgres chỉ đọc tài khoản trong `.env` khi tạo dữ liệu lần đầu. Có hai cách:
- Đặt lại mật khẩu cũ trong `.env`.
- Chạy `docker compose down -v` rồi chạy lại. Cách này **xoá toàn bộ dữ liệu**.

**Không nhận được email OTP**
Kiểm tra cấu hình SMTP trong `.env` và log `docker compose logs api`. Nếu chỉ chạy thử, đặt `MAIL_ENABLED=false` rồi lấy mã OTP trong log.

**Không thấy nút Google / bấm vào báo lỗi `origin_mismatch`**
- Không thấy nút: kiểm tra `GOOGLE_CLIENT_ID` đã được điền và đã chạy lại `docker compose up -d`.
- Lỗi `origin_mismatch`: URL trên thanh địa chỉ (gồm cả port) chưa được thêm vào Authorized JavaScript origins.
