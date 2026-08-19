# Runtime secrets

Only this file is tracked. Generate the required files with `./ops/generate-secrets.sh`; never commit their contents. Database secrets intentionally use a restricted character set so the one-time MySQL bootstrap can validate them before safely creating users.

Required files: `mysql_root_password`, `mysql_game_password`, `mysql_admin_password`, `mysql_backup_password`, `admin_password_hash`, `restic_password`, `s3_access_key`, and `s3_secret_key`.
