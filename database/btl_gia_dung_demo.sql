-- MySQL 8.0.16+. Chạy toàn bộ trên database mới. Mật khẩu web: 12345678.
-- 0: đúng 5 dòng ở từng bảng. 1: thêm 4 serial còn bán để demo CN07-CN09.
SET @them_ton_demo = 1;
CREATE DATABASE btl_gia_dung CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE btl_gia_dung;
SET NAMES utf8mb4;

CREATE TABLE TAIKHOAN (
 ma_tk INT AUTO_INCREMENT PRIMARY KEY, ho_ten VARCHAR(100) NOT NULL,
 email VARCHAR(100) NOT NULL UNIQUE, so_dien_thoai VARCHAR(15) NOT NULL,
 mat_khau VARCHAR(255) NOT NULL, dia_chi VARCHAR(255),
 loai_tai_khoan VARCHAR(20) NOT NULL CHECK (loai_tai_khoan IN ('CUSTOMER','STAFF','ADMIN','DEALER'))
) ENGINE=InnoDB;
CREATE TABLE LOAI_SP (
 ma_loai INT AUTO_INCREMENT PRIMARY KEY, ten_loai VARCHAR(100) NOT NULL, mo_ta VARCHAR(255)
) ENGINE=InnoDB;
CREATE TABLE THUONG_HIEU (
 ma_thuong_hieu INT AUTO_INCREMENT PRIMARY KEY, ten VARCHAR(100) NOT NULL, quoc_gia VARCHAR(50)
) ENGINE=InnoDB;
CREATE TABLE SAN_PHAM (
 ma_sp INT AUTO_INCREMENT PRIMARY KEY, ten VARCHAR(200) NOT NULL, hinh_anh VARCHAR(255), mo_ta TEXT,
 don_gia_ban DECIMAL(15,2) CHECK (don_gia_ban>0), bao_hanh_thang INT CHECK (bao_hanh_thang>=0),
 ma_loai INT NOT NULL, ma_thuong_hieu INT, ma_sp_cha INT,
 FOREIGN KEY(ma_loai) REFERENCES LOAI_SP(ma_loai),
 FOREIGN KEY(ma_thuong_hieu) REFERENCES THUONG_HIEU(ma_thuong_hieu),
 FOREIGN KEY(ma_sp_cha) REFERENCES SAN_PHAM(ma_sp)
) ENGINE=InnoDB;
CREATE TABLE LOAI_DAC_TRUNG (
 ma_loai_dac_trung INT AUTO_INCREMENT PRIMARY KEY, ten_loai VARCHAR(100) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE DAC_TRUNG (
 ma_dac_trung INT AUTO_INCREMENT PRIMARY KEY, gia_tri VARCHAR(100) NOT NULL,
 don_vi VARCHAR(20), y_nghia VARCHAR(255), ma_loai_dac_trung INT NOT NULL,
 FOREIGN KEY(ma_loai_dac_trung) REFERENCES LOAI_DAC_TRUNG(ma_loai_dac_trung)
) ENGINE=InnoDB;
CREATE TABLE DT_SP (
 ma_sp INT NOT NULL, ma_dac_trung INT NOT NULL, mo_ta VARCHAR(255),
 PRIMARY KEY(ma_sp,ma_dac_trung), FOREIGN KEY(ma_sp) REFERENCES SAN_PHAM(ma_sp),
 FOREIGN KEY(ma_dac_trung) REFERENCES DAC_TRUNG(ma_dac_trung)
) ENGINE=InnoDB;
CREATE TABLE PHIEU_NHAP_KHO (
 ma_phieu INT AUTO_INCREMENT PRIMARY KEY, ngay_gio DATETIME NOT NULL,
 trang_thai VARCHAR(20) NOT NULL CHECK (trang_thai IN ('PENDING','RECEIVED')),
 ma_dai_ly INT NOT NULL, ma_nguoi_lap INT NOT NULL,
 FOREIGN KEY(ma_dai_ly) REFERENCES TAIKHOAN(ma_tk), FOREIGN KEY(ma_nguoi_lap) REFERENCES TAIKHOAN(ma_tk)
) ENGINE=InnoDB;
CREATE TABLE CHI_TIET_PNK (
 ma_phieu INT NOT NULL, ma_sp INT NOT NULL, so_luong INT NOT NULL CHECK(so_luong>0),
 don_gia_nhap DECIMAL(15,2) NOT NULL CHECK(don_gia_nhap>0),
 PRIMARY KEY(ma_phieu,ma_sp), FOREIGN KEY(ma_phieu) REFERENCES PHIEU_NHAP_KHO(ma_phieu),
 FOREIGN KEY(ma_sp) REFERENCES SAN_PHAM(ma_sp)
) ENGINE=InnoDB;
CREATE TABLE DON (
 ma_don INT AUTO_INCREMENT PRIMARY KEY, ngay_gio DATETIME NOT NULL, dia_chi_giao_hang VARCHAR(255) NOT NULL,
 trang_thai VARCHAR(20) NOT NULL CHECK(trang_thai IN ('PENDING','CONFIRMED','SHIPPING','COMPLETED','CANCELLED')),
 ma_nguoi_dat INT NOT NULL, ma_nguoi_xu_ly INT,
 FOREIGN KEY(ma_nguoi_dat) REFERENCES TAIKHOAN(ma_tk), FOREIGN KEY(ma_nguoi_xu_ly) REFERENCES TAIKHOAN(ma_tk)
) ENGINE=InnoDB;
CREATE TABLE CHI_TIET_DON (
 ma_don INT NOT NULL, ma_sp INT NOT NULL, so_luong INT NOT NULL CHECK(so_luong>0),
 don_gia DECIMAL(15,2) NOT NULL CHECK(don_gia>0), PRIMARY KEY(ma_don,ma_sp),
 FOREIGN KEY(ma_don) REFERENCES DON(ma_don), FOREIGN KEY(ma_sp) REFERENCES SAN_PHAM(ma_sp)
) ENGINE=InnoDB;
CREATE TABLE CHI_TIET_SP (
 ma_chi_tiet_sp INT AUTO_INCREMENT PRIMARY KEY, so_serial VARCHAR(50) NOT NULL UNIQUE, ngay_het_han_bh DATE,
 ma_sp INT NOT NULL, ma_phieu INT NOT NULL, ma_don INT,
 FOREIGN KEY(ma_sp) REFERENCES SAN_PHAM(ma_sp),
 FOREIGN KEY(ma_phieu,ma_sp) REFERENCES CHI_TIET_PNK(ma_phieu,ma_sp),
 FOREIGN KEY(ma_don,ma_sp) REFERENCES CHI_TIET_DON(ma_don,ma_sp),
 INDEX idx_serial_phan_bo(ma_sp,ma_don,ma_phieu)
) ENGINE=InnoDB;
CREATE TABLE DANH_GIA (
 ma_tk INT NOT NULL, ma_sp INT NOT NULL, so_sao TINYINT NOT NULL CHECK(so_sao BETWEEN 1 AND 5),
 loi_binh VARCHAR(500), ngay_gio DATETIME NOT NULL, PRIMARY KEY(ma_tk,ma_sp),
 FOREIGN KEY(ma_tk) REFERENCES TAIKHOAN(ma_tk), FOREIGN KEY(ma_sp) REFERENCES SAN_PHAM(ma_sp)
) ENGINE=InnoDB;

START TRANSACTION;
INSERT INTO TAIKHOAN(ma_tk,ho_ten,email,so_dien_thoai,mat_khau,dia_chi,loai_tai_khoan) VALUES
(1,'Trần Minh','quantrivien@gmail.com','0900000001','$2a$10$96SoE6I1M9f3.HoKEhfj4eIk6ETXB.YQjy9ZwMsfMBUTEE6LuYF2m',NULL,'ADMIN'),
(2,'Lê Hà','nhanvien@gmail.com','0900000002','$2a$10$bb7u6OSiSyEjZDM/vTU7vOs3W0w0/aoRHtctv1freOya0wMWpwJ3e',NULL,'STAFF'),
(3,'Đại lý An Gia','daily@gmail.com','0900000003','$2a$10$YLddgnVpwULJoazPq/Un8eD3iJaugreNpQKr7Mv57177VAJLDQdpm',NULL,'DEALER'),
(4,'Nguyễn An','khachhang1@gmail.com','0900000004','$2a$10$14zsOVY7G3W3/wVSHRP5qOTeeHDdPgJiAj.LM3kG63855agbVOq7a','12 Nguyễn Trãi','CUSTOMER'),
(5,'Đỗ Bình','khachhang2@gmail.com','0900000005','$2a$10$WwrVSC0E933/8KC64EGIleiCRbPftC1pPBKx4b6PtrC4QFuQQ4b6y','28 Lê Lợi','CUSTOMER');
INSERT INTO LOAI_SP(ma_loai,ten_loai) VALUES (1,'Ấm điện'),(2,'Nồi điện'),(3,'Đồ sứ'),(4,'Chảo'),(5,'Bình nước');
INSERT INTO THUONG_HIEU(ma_thuong_hieu,ten) VALUES (1,'Bếp An'),(2,'Minh Gia'),(3,'Sứ Lam'),(4,'Bếp Việt'),(5,'Mộc');
INSERT INTO SAN_PHAM(ma_sp,ten,don_gia_ban,bao_hanh_thang,ma_loai,ma_thuong_hieu) VALUES
(1,'Ấm siêu tốc 1,7 lít',390000,12,1,1),(2,'Nồi cơm điện 1,8 lít',890000,12,2,2),
(3,'Bộ chén sứ 4 chiếc',180000,0,3,3),(4,'Chảo chống dính 24 cm',320000,6,4,4),(5,'Bình giữ nhiệt 500 ml',210000,6,5,5);
INSERT INTO LOAI_DAC_TRUNG VALUES (1,'Công suất'),(2,'Dung tích'),(3,'Chất liệu'),(4,'Đường kính'),(5,'Khối lượng');
INSERT INTO DAC_TRUNG VALUES (1,'1500','W',NULL,1),(2,'1.8','lít',NULL,2),(3,'Sứ',NULL,NULL,3),(4,'24','cm',NULL,4),(5,'280','g',NULL,5);
INSERT INTO DT_SP VALUES (1,1,NULL),(2,2,NULL),(3,3,NULL),(4,4,NULL),(5,5,NULL);
INSERT INTO PHIEU_NHAP_KHO VALUES (1,'2026-10-01','RECEIVED',3,2),(2,'2026-10-02','RECEIVED',3,2),
(3,'2026-10-03','RECEIVED',3,2),(4,'2026-10-04','RECEIVED',3,2),(5,'2026-10-05','RECEIVED',3,2);
INSERT INTO CHI_TIET_PNK VALUES (1,1,1+@them_ton_demo,300000),(2,2,1+2*@them_ton_demo,700000),
(3,3,1+@them_ton_demo,110000),(4,4,1,250000),(5,5,1,155000);
INSERT INTO DON VALUES (1,'2026-10-10','12 Nguyễn Trãi','COMPLETED',4,2),(2,'2026-10-11','28 Lê Lợi','COMPLETED',5,2),
(3,'2026-10-12','12 Nguyễn Trãi','COMPLETED',4,2),(4,'2026-10-13','28 Lê Lợi','COMPLETED',5,2),(5,'2026-10-14','12 Nguyễn Trãi','COMPLETED',4,2);
INSERT INTO CHI_TIET_DON VALUES (1,1,1,390000),(2,2,1,890000),(3,3,1,180000),(4,4,1,320000),(5,5,1,210000);
INSERT INTO CHI_TIET_SP(so_serial,ma_sp,ma_phieu,ma_don,ngay_het_han_bh) VALUES
('AM-001',1,1,1,'2027-10-12'),('NOI-001',2,2,2,'2027-10-13'),('CHEN-001',3,3,3,NULL),
('CHAO-001',4,4,4,'2027-04-15'),('BINH-001',5,5,5,'2027-04-16');
INSERT INTO CHI_TIET_SP(so_serial,ma_sp,ma_phieu)
SELECT serial,sp,sp FROM (
 SELECT 'AM-002' AS serial,1 AS sp UNION ALL SELECT 'NOI-002',2
 UNION ALL SELECT 'NOI-003',2 UNION ALL SELECT 'CHEN-002',3
) AS kho_demo WHERE @them_ton_demo=1;
INSERT INTO DANH_GIA VALUES (4,1,5,'Ấm đun nhanh.','2026-10-18'),(5,2,4,'Nồi dễ vệ sinh.','2026-10-19'),
(4,3,5,'Chén vừa tay.','2026-10-20'),(5,4,4,'Chảo nóng đều.','2026-10-21'),(4,5,5,'Bình kín nước.','2026-10-22');
COMMIT;
