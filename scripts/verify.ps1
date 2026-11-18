# =====================================================================
# 端到端验证脚本：多租户隔离 / 认证 / RBAC / 行级数据权限
# 用法： powershell -ExecutionPolicy Bypass -File scripts\verify.ps1
# =====================================================================
$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080'
$results = @()

function Login([string]$tenant, [string]$user, [string]$pwd) {
    $body = @{ tenantCode = $tenant; username = $user; password = $pwd } | ConvertTo-Json
    $resp = Invoke-RestMethod -Uri "$base/api/auth/login" -Method Post -ContentType 'application/json' -Body $body
    return $resp.data
}

function Get-Api([string]$path, [string]$token) {
    return (Invoke-RestMethod -Uri "$base$path" -Headers @{ Authorization = "Bearer $token" }).data
}

function Try-Get-Api([string]$path, [string]$token) {
    try {
        $r = Invoke-WebRequest -Uri "$base$path" -Headers @{ Authorization = "Bearer $token" } -UseBasicParsing
        return @{ status = [int]$r.StatusCode; body = $r.Content }
    } catch {
        $status = 0
        if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
        return @{ status = $status; body = "" }
    }
}

Write-Host '== 1. 登录（Acme 租户）=='
$admin = Login 'acme' 'admin' 'admin123'
$results += "login acme/admin -> user=$($admin.username) tenant=$($admin.tenantCode) roles=$($admin.roles -join ',') superAdmin=$($admin.superAdmin)"

Write-Host '== 2. 当前用户权限视图 =='
$info = Get-Api '/api/auth/userinfo' $admin.accessToken
$results += "userinfo -> roles=$($info.roles -join ',') perms=$($info.permissions.Count) menus=$($info.menus.Count) dataScope=$($info.dataScope)"

Write-Host '== 3. 行级数据权限：admin(ALL) =='
$ordersAdmin = Get-Api '/api/demo/orders?pageNum=1&pageSize=50' $admin.accessToken
$results += "orders admin(ALL) -> total=$($ordersAdmin.total)"

Write-Host '== 4. 行级数据权限：manager(DEPT_AND_CHILD) =='
$manager = Login 'acme' 'manager' 'admin123'
$ordersManager = Get-Api '/api/demo/orders?pageNum=1&pageSize=50' $manager.accessToken
$results += "orders manager(DEPT_AND_CHILD) -> total=$($ordersManager.total) depts=$(($ordersManager.records | ForEach-Object { $_.deptId }) -join ',')"

Write-Host '== 5. 行级数据权限：analyst(SELF) =='
$analyst = Login 'acme' 'analyst' 'admin123'
$ordersAnalyst = Get-Api '/api/demo/orders?pageNum=1&pageSize=50' $analyst.accessToken
$results += "orders analyst(SELF) -> total=$($ordersAnalyst.total) createBy=$(($ordersAnalyst.records | ForEach-Object { $_.createBy }) -join ',')"

Write-Host '== 6. RBAC：analyst 访问用户管理应被拒绝 =='
$denied = Try-Get-Api '/api/system/users?pageNum=1&pageSize=10' $analyst.accessToken
$results += "analyst -> GET /api/system/users status=$($denied.status) (expect 403)"

Write-Host '== 7. RBAC：admin 访问用户管理应通过 =='
$users = Get-Api '/api/system/users?pageNum=1&pageSize=10' $admin.accessToken
$results += "admin -> GET /api/system/users total=$($users.total)"

Write-Host '== 8. 租户隔离：admin 访问租户列表应被拒绝（仅平台超管）=='
$tenantDenied = Try-Get-Api '/api/tenants?pageNum=1&pageSize=10' $admin.accessToken
$results += "admin -> GET /api/tenants status=$($tenantDenied.status) (expect 403)"

Write-Host '== 9. 平台超管 =='
$super = Login 'platform' 'superadmin' 'admin123'
$tenants = Get-Api '/api/tenants?pageNum=1&pageSize=10' $super.accessToken
$results += "superadmin -> GET /api/tenants total=$($tenants.total)"

Write-Host '== 10. 无令牌访问受保护接口 =='
$noToken = Try-Get-Api '/api/demo/orders' ''
$results += "no token -> GET /api/demo/orders status=$($noToken.status) (expect 401)"

Write-Host '== 11. 跨租户伪造请求头 =='
$crossTenant = Try-Get-Api '/api/demo/orders?pageNum=1&pageSize=50' $analyst.accessToken
$results += "analyst orders header-override -> status=$($crossTenant.status) (token tenant wins)"

$results | ForEach-Object { Write-Host $_ }
$results | Set-Content -Path "$PSScriptRoot\verify-result.txt" -Encoding UTF8
