# e-print-admin

`e-print-admin` is a Spring Boot MVC admin console for managing e-print HTML templates.

## Features

- Single administrator login
- Template list with `templateCode` and `status` filters
- Create and edit template metadata in `E_PRINT_TEMPLATE`
- Upload or edit HTML content
- Store template files in MinIO
- Preview templates in an iframe
- Disable templates by setting `STATUS = 0`

## Local Run

Requirements:

- Java 21
- Oracle database with `E_PRINT_TEMPLATE` and `SEQ_E_PRINT_TEMPLATE`
- MinIO bucket access

```bash
cd e-print-admin
mvn spring-boot:run
```

Default local URL:

```text
http://localhost:9091
```

Default local login:

```text
eprint / eprint123
```

## Configuration

The application uses Spring profiles. The default profile is `loc`.

```powershell
$env:E_PRINT_ADMIN_PROFILE="uat"
java -jar e-print-admin.jar
```

Key environment variables:

| Variable | Purpose |
| --- | --- |
| `E_PRINT_ADMIN_PROFILE` | Active profile, default `loc` |
| `E_PRINT_ADMIN_PORT` | HTTP port, default `9091` |
| `E_PRINT_ADMIN_USERNAME` | Admin username |
| `E_PRINT_ADMIN_PASSWORD` | Admin password |
| `E_PRINT_DB_URL` | Oracle JDBC URL |
| `E_PRINT_DB_USERNAME` | Oracle username |
| `E_PRINT_DB_PASSWORD` | Oracle password |
| `E_PRINT_MINIO_ENDPOINT` | MinIO endpoint |
| `E_PRINT_MINIO_ACCESS_KEY` | MinIO access key |
| `E_PRINT_MINIO_SECRET_KEY` | MinIO secret key |
| `E_PRINT_TEMPLATE_BUCKET` | Default template bucket, default `e-print` |
| `E_PRINT_TEMPLATE_OBJECT_PREFIX` | Default object prefix, default `templates/print` |

## Pages

| Page | Purpose |
| --- | --- |
| `/login` | Admin login |
| `/admin/templates` | Template list |
| `/admin/templates/new` | Create template |
| `/admin/templates/{id}/edit` | Edit template |
| `/admin/templates/{id}/preview` | Preview template |

## Verification

```bash
mvn -DskipTests compile
```
