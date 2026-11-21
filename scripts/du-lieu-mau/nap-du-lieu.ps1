# Sinh du lieu van hanh mau va nap vao CSDL Docker cua OISM.
# CANH BAO: XOA SACH toan bo du lieu nghiep vu hien co trong oism_db (giu lai lich su Flyway).
#   .\scripts\du-lieu-mau\nap-du-lieu.ps1            # seed ngau nhien
#   .\scripts\du-lieu-mau\nap-du-lieu.ps1 -Seed 42   # lap lai dung bo du lieu cu (mat khau van sinh moi)
param(
    [int]$Seed = -1,
    [string]$Container = "oism_postgres",
    [string]$Database = "oism_db"
)
$ErrorActionPreference = "Stop"
$thuMuc = $PSScriptRoot
$sql = Join-Path $thuMuc "du-lieu-mau.sql"

$thamSo = @((Join-Path $thuMuc "sinh_du_lieu.py"), "--out", $sql)
if ($Seed -ge 0) { $thamSo += @("--seed", "$Seed") }
$env:PYTHONIOENCODING = "utf-8"
python @thamSo
if ($LASTEXITCODE -ne 0) { throw "Sinh du lieu that bai" }

docker cp $sql "${Container}:/tmp/du-lieu-mau.sql"
docker exec $Container psql -U postgres -d $Database -v ON_ERROR_STOP=1 -q -f /tmp/du-lieu-mau.sql
$ketQua = $LASTEXITCODE
docker exec $Container rm -f /tmp/du-lieu-mau.sql
if ($ketQua -ne 0) { throw "Nap du lieu that bai (da rollback, du lieu cu con nguyen)" }

Write-Host "Da nap xong. Danh sach tai khoan: $(Join-Path $thuMuc 'tai-khoan.local.md')"
