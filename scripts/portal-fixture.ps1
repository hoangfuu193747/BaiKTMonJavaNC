param([ValidateSet('create','cleanup','inspect')][string]$Action,[string]$Prefix,[switch]$ForVisual,[switch]$ForManagement,[string]$ManagementRoom,[string]$OriginalZoneName,[string]$OriginalAdminHash,[string]$OriginalAdminUsername)
$ErrorActionPreference='Stop'
if ($Prefix -notmatch '^PT[A-F0-9]{6}$') { throw 'Invalid fixture prefix' }
$projectDirectory=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$fixtureConnection=New-Object System.Data.SqlClient.SqlConnection 'Server=localhost\SQLEXPRESS;Database=QuanLyKyTucXa;Integrated Security=True;TrustServerCertificate=True;Connection Timeout=5'
function Execute-FixtureSql([string]$Sql,[hashtable]$Values) {
    $fixtureCommand=$fixtureConnection.CreateCommand()
    $fixtureCommand.CommandText=$Sql
    if($script:fixtureTransaction){$fixtureCommand.Transaction=$script:fixtureTransaction}
    foreach($key in $Values.Keys){[void]$fixtureCommand.Parameters.AddWithValue('@'+$key,$Values[$key])}
    $fixtureAdapter=New-Object System.Data.SqlClient.SqlDataAdapter $fixtureCommand
    $fixtureTable=New-Object System.Data.DataTable
    [void]$fixtureAdapter.Fill($fixtureTable)
    return ,$fixtureTable
}
try {
    $fixtureConnection.Open()
    if($Action -eq 'create') {
        $classpath=(Join-Path $projectDirectory 'target\test-classes')+';'+(Join-Path $projectDirectory 'target\classes')+';'+(Join-Path $projectDirectory 'target\QuanLyKyTucXa\WEB-INF\lib\jbcrypt-0.4.jar')
        $hash=& java -cp $classpath 'com.qlktx.util.PortalHashTool'
        if($LASTEXITCODE -ne 0){throw 'Cannot hash fixture password'}
        $roomTable=Execute-FixtureSql "SELECT TOP(1) p.MaPhong,p.MaKhu,k.TenKhu FROM dbo.Phong p JOIN dbo.KhuNha k ON k.MaKhu=p.MaKhu WHERE p.TrangThai='HOAT_DONG' AND k.TrangThai=1 AND k.GioiTinh=N'Nam' AND NOT EXISTS(SELECT 1 FROM dbo.HopDong h WHERE h.MaPhong=p.MaPhong AND h.TrangThai='DANG_O') ORDER BY p.MaPhong DESC" @{}
        if($roomTable.Rows.Count -eq 0){throw 'No empty room available for isolated fixtures'}
        $script:fixtureTransaction=$fixtureConnection.BeginTransaction()
        for($n=1;$n -le 6;$n++) {
            $id=$Prefix+('{0:D2}' -f $n)
            $gender=if($n -eq 6){'N'+[char]0x1EEF}else{'Nam'}
            [void](Execute-FixtureSql "INSERT INTO dbo.SinhVienTruong(MaSV,HoTen,NgaySinh,GioiTinh,Lop,Khoa,Email,TrangThai) VALUES (@id,@name,'2005-01-01',@gender,'QA','QA',@email,1); INSERT INTO dbo.TaiKhoan(TenDangNhap,MatKhauHash,VaiTro,MaSV,TrangThai) VALUES(@id,@hash,'SINH_VIEN',@id,1); INSERT INTO dbo.SinhVien(MaSV,MaTK) VALUES(@id,CONVERT(int,SCOPE_IDENTITY()));" @{id=$id;name="QA Student $id";gender=$gender;email="$id@example.test";hash=[string]$hash})
        }
        $adminUsername=$null
        $adminHash=$null
        if($ForManagement){
            $adminTable=Execute-FixtureSql "SELECT TOP(1) TenDangNhap,MatKhauHash FROM dbo.TaiKhoan WHERE VaiTro='ADMIN' AND TrangThai=1 ORDER BY MaTK" @{}
            if($adminTable.Rows.Count -eq 0){throw 'No active administrator available'}
            $adminUsername=[string]$adminTable.Rows[0].TenDangNhap
            $adminHash=[string]$adminTable.Rows[0].MatKhauHash
        }
        $visualInvoice=$null
        $reportBilled='0'
        $reportPending='0'
        if($ForVisual){
            $baseline=Execute-FixtureSql "SELECT CONVERT(varchar(40),COALESCE(SUM(i.TongTien),0)) AS billed,CONVERT(varchar(40),COALESCE(SUM(pending.amount),0)) AS pending FROM dbo.HoaDon i JOIN dbo.HopDong h ON h.MaHDong=i.MaHDong JOIN dbo.Phong p ON p.MaPhong=h.MaPhong OUTER APPLY (SELECT SUM(t.SoTien) AS amount FROM dbo.ThanhToan t WHERE t.MaHoaDon=i.MaHoaDon AND t.TrangThai='CHO_XAC_NHAN') pending WHERE p.MaKhu=@zone AND i.Thang=MONTH(SYSDATETIME()) AND i.Nam=YEAR(SYSDATETIME())" @{zone=[string]$roomTable.Rows[0].MaKhu}
            $reportBilled=[string]$baseline.Rows[0].billed
            $reportPending=[string]$baseline.Rows[0].pending
            $visualRows=Execute-FixtureSql "DECLARE @admin int=(SELECT TOP(1) MaTK FROM dbo.TaiKhoan WHERE VaiTro='ADMIN' AND TrangThai=1 ORDER BY MaTK); INSERT INTO dbo.DangKyPhong(MaSV,MaPhong,TrangThai,NguoiDuyet,NgayDuyet) VALUES(@id,@room,'DA_DUYET',@admin,SYSDATETIME()); DECLARE @registration int=CONVERT(int,SCOPE_IDENTITY()); INSERT INTO dbo.HopDong(MaDK,MaSV,MaPhong,NgayBatDau) VALUES(@registration,@id,@room,CONVERT(date,SYSDATETIME())); DECLARE @contract int=CONVERT(int,SCOPE_IDENTITY()); INSERT INTO dbo.HoaDon(MaHDong,Thang,Nam,TienPhong,TienDien,TienNuoc,HanThanhToan) VALUES(@contract,MONTH(SYSDATETIME()),YEAR(SYSDATETIME()),700000,50000,25000,CONVERT(date,SYSDATETIME())); DECLARE @invoice int=CONVERT(int,SCOPE_IDENTITY()); INSERT INTO dbo.ThanhToan(MaHoaDon,SoTien,PhuongThuc,MaGiaoDich) VALUES(@invoice,100000,'CHUYEN_KHOAN',@reference); SELECT @invoice AS invoice;" @{id=$Prefix+'01';room=[string]$roomTable.Rows[0].MaPhong;reference='DEMO-'+$Prefix}
            $visualInvoice=[int]$visualRows.Rows[0].invoice
        }
        $script:fixtureTransaction.Commit();$script:fixtureTransaction=$null
        @{room=[string]$roomTable.Rows[0].MaPhong;zone=[string]$roomTable.Rows[0].MaKhu;zoneName=[string]$roomTable.Rows[0].TenKhu;prefix=$Prefix;password='Portal-test-2026!';invoice=$visualInvoice;adminUsername=$adminUsername;adminHash=$adminHash;reportBilled=$reportBilled;reportPending=$reportPending} | ConvertTo-Json -Compress | Write-Output
    } elseif($Action -eq 'cleanup') {
        $script:fixtureTransaction=$fixtureConnection.BeginTransaction()
        if($ForManagement -and $ManagementRoom){
            if($ManagementRoom -notmatch '^[ABCD][1-5](0[1-9]|10)$' -or -not $OriginalZoneName){throw 'Invalid management cleanup metadata'}
            [void](Execute-FixtureSql "UPDATE dbo.Phong SET TrangThai='HOAT_DONG' WHERE MaPhong=@room; UPDATE dbo.KhuNha SET TenKhu=@name,TrangThai=1 WHERE MaKhu=@zone" @{room=$ManagementRoom;zone=$ManagementRoom.Substring(0,1);name=$OriginalZoneName})
        }
        [void](Execute-FixtureSql "DELETE y FROM dbo.YeuCauSuaChua y WHERE y.MaSV LIKE @mask; DELETE t FROM dbo.ThanhToan t JOIN dbo.HoaDon i ON i.MaHoaDon=t.MaHoaDon JOIN dbo.HopDong h ON h.MaHDong=i.MaHDong WHERE h.MaSV LIKE @mask; DELETE i FROM dbo.HoaDon i JOIN dbo.HopDong h ON h.MaHDong=i.MaHDong WHERE h.MaSV LIKE @mask; DELETE FROM dbo.HopDong WHERE MaSV LIKE @mask; DELETE FROM dbo.DangKyPhong WHERE MaSV LIKE @mask; DELETE FROM dbo.SinhVien WHERE MaSV LIKE @mask; DELETE FROM dbo.TaiKhoan WHERE MaSV LIKE @mask; DELETE FROM dbo.SinhVienTruong WHERE MaSV LIKE @mask; DELETE FROM dbo.ThongBao WHERE TieuDe=@title;" @{mask=$Prefix+'[0-9][0-9]';title='Portal '+$Prefix})
        if($ForManagement -and $OriginalAdminHash){
            if($OriginalAdminHash -notmatch '^\$2a\$[0-9]{2}\$[./A-Za-z0-9]{53}$' -or -not $OriginalAdminUsername){throw 'Invalid administrator restore metadata'}
            [void](Execute-FixtureSql "UPDATE dbo.TaiKhoan SET MatKhauHash=@hash WHERE TenDangNhap=@username AND VaiTro='ADMIN' AND MaSV IS NULL" @{hash=$OriginalAdminHash;username=$OriginalAdminUsername})
        }
        $script:fixtureTransaction.Commit();$script:fixtureTransaction=$null
        Write-Output 'SCOPED_FIXTURES_REMOVED'
    } else {
        $table=Execute-FixtureSql "SELECT r.MaSV,r.MaDK,r.TrangThai FROM dbo.DangKyPhong r WHERE r.MaSV LIKE @mask ORDER BY r.MaSV,r.MaDK" @{mask=$Prefix+'[0-9][0-9]'}
        $table | ConvertTo-Json -Compress -Depth 3 | Write-Output
    }
} catch {
    if($script:fixtureTransaction){$script:fixtureTransaction.Rollback()}
    throw
} finally {$fixtureConnection.Dispose()}
