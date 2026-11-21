# -*- coding: utf-8 -*-
"""Danh muc cua hang, nhan su, nha cung cap va san pham dung cho bo sinh du lieu mau.

Moi san pham: (ten, danh muc con, thuong hieu, ma goc, gia ban le, ty le gia von, cac bien the, mo ta, do pho bien).
Bien the: list cac dict thuoc tinh; None = san pham khong co bien the.
"""

SIZE_NAM = ["39", "40", "41", "42", "43"]
SIZE_NU = ["35", "36", "37", "38", "39"]
SIZE_UNISEX = ["36", "37", "38", "39", "40", "41", "42", "43"]
SIZE_AO = ["S", "M", "L", "XL"]
SIZE_QUAN = ["28", "29", "30", "31", "32"]


def bien_the(mau, sizes):
    return [{"Màu": m, "Size": s} for m in mau for s in sizes]


CUA_HANG = [
    {
        "key": "HT",
        "ten": "Giày Dép Hưng Thịnh",
        "ngay_tao": "2026-08-18 10:12",
        "giu_hang_phut": 30,
        "mien": "bac",
        "chi_nhanh": [
            {"key": "CG", "ten": "Hưng Thịnh Cầu Giấy", "loai": "STORE",
             "dia_chi": "128 Trần Thái Tông, Dịch Vọng Hậu, Cầu Giấy, Hà Nội", "mo": 9.0, "dong": 21.75},
            {"key": "PH", "ten": "Hưng Thịnh Phố Huế", "loai": "STORE",
             "dia_chi": "245 Phố Huế, Phố Huế, Hai Bà Trưng, Hà Nội", "mo": 8.5, "dong": 21.5},
            {"key": "KHO", "ten": "Kho Long Biên", "loai": "WAREHOUSE",
             "dia_chi": "Ngõ 72 Ngọc Thụy, Ngọc Thụy, Long Biên, Hà Nội"},
        ],
        "nhan_su": [
            {"ten": "Trần Văn Hưng", "email": "hung.tran@giayhungthinh.vn", "vai_tro": "OWNER", "tu_ngay": 0},
            {"ten": "Nguyễn Thị Thu Hà", "email": "thuha.kho@giayhungthinh.vn", "vai_tro": "STAFF", "tu_ngay": 1},
            {"ten": "Lê Minh Đức", "email": "duc.lm@giayhungthinh.vn", "vai_tro": "STAFF", "tu_ngay": 9},
            {"ten": "Phạm Ngọc Ánh", "email": "ngocanh.cg@giayhungthinh.vn", "vai_tro": "CASHIER", "chi_nhanh": "CG", "tu_ngay": 2},
            {"ten": "Đỗ Quang Huy", "email": "huy.do@giayhungthinh.vn", "vai_tro": "CASHIER", "chi_nhanh": "CG", "tu_ngay": 2},
            {"ten": "Vũ Thị Mai", "email": "mai.vu@giayhungthinh.vn", "vai_tro": "CASHIER", "chi_nhanh": "PH", "tu_ngay": 3},
            {"ten": "Hoàng Anh Tuấn", "email": "tuan.ha@giayhungthinh.vn", "vai_tro": "CASHIER", "chi_nhanh": "PH", "tu_ngay": 3, "nghi_ngay": 47},
            {"ten": "Bùi Khánh Linh", "email": "khanhlinh.ph@giayhungthinh.vn", "vai_tro": "CASHIER", "chi_nhanh": "PH", "tu_ngay": 45},
        ],
        "nha_cung_cap": [
            {"key": "BITIS", "ten": "Công ty TNHH Sản xuất Hàng tiêu dùng Bình Tiên", "dia_chi": "22 Lý Chiêu Hoàng, Phường 10, Quận 6, TP.HCM"},
            {"key": "NPP", "ten": "Nhà phân phối Giày Thể Thao Minh Phát", "dia_chi": "Lô B12 KCN Quang Minh, Mê Linh, Hà Nội"},
            {"key": "XUONG", "ten": "Xưởng giày da Thanh Bình", "dia_chi": "Thôn Phú Yên, Phú Xuyên, Hà Nội"},
            {"key": "PK", "ten": "Đại lý phụ kiện Tâm An", "dia_chi": "36 Hàng Đường, Hoàn Kiếm, Hà Nội"},
        ],
        "danh_muc": {
            "Giày nam": ["Giày thể thao nam", "Giày tây", "Giày lười"],
            "Giày nữ": ["Giày búp bê", "Sandal nữ", "Giày thể thao nữ"],
            "Dép": [],
            "Phụ kiện": ["Tất", "Chăm sóc giày"],
        },
        "san_pham": [
            ("Biti's Hunter X Festive", "Giày thể thao nam", "Biti's", "BH-FES", 1059000, 0.62,
             bien_the(["Đen", "Trắng kem"], SIZE_NAM), "Đế Phylon nhẹ, quai dệt thoáng khí.", 9, "BITIS"),
            ("Biti's Hunter Street", "Giày thể thao nữ", "Biti's", "BH-STR", 899000, 0.62,
             bien_the(["Hồng nhạt", "Trắng"], SIZE_NU), "Phiên bản nữ, đế cao 3,5 cm.", 6, "BITIS"),
            ("Ananas Basas Bumper Gum Low", "Giày thể thao nam", "Ananas", "ANA-BSG", 590000, 0.58,
             bien_the(["Kem", "Đen"], SIZE_UNISEX), "Canvas dày, đế cao su gum.", 8, "NPP"),
            ("Nike Air Force 1 '07", "Giày thể thao nam", "Nike", "NK-AF1", 2929000, 0.71,
             bien_the(["Trắng"], SIZE_UNISEX), "Da thật, đế Air.", 7, "NPP"),
            ("Adidas Samba OG", "Giày thể thao nam", "Adidas", "AD-SMB", 2800000, 0.70,
             bien_the(["Trắng sọc đen", "Đen"], SIZE_NAM), None, 6, "NPP"),
            ("Converse Chuck Taylor All Star Classic", "Giày thể thao nữ", "Converse", "CV-CTC", 1500000, 0.66,
             bien_the(["Đen", "Trắng"], SIZE_UNISEX), "Cổ thấp, vải canvas.", 7, "NPP"),
            ("Vans Old Skool", "Giày thể thao nam", "Vans", "VN-OSK", 1750000, 0.68,
             bien_the(["Đen trắng"], SIZE_UNISEX), None, 5, "NPP"),
            ("Giày tây Oxford da bò", "Giày tây", None, "OXF-DB", 890000, 0.52,
             bien_the(["Đen", "Nâu"], SIZE_NAM), "Da bò nguyên tấm, đế cao su khâu.", 4, "XUONG"),
            ("Giày lười da lộn", "Giày lười", None, "LUOI-DL", 650000, 0.50,
             bien_the(["Xanh navy", "Nâu bò"], SIZE_NAM), None, 3, "XUONG"),
            ("Giày búp bê mũi nhọn", "Giày búp bê", None, "BB-MN", 345000, 0.48,
             bien_the(["Đen", "Nude", "Đỏ đô"], SIZE_NU), "Da PU mềm, lót êm.", 6, "XUONG"),
            ("Sandal nữ quai mảnh", "Sandal nữ", None, "SDL-QM", 279000, 0.47,
             bien_the(["Đen", "Be"], SIZE_NU), None, 5, "XUONG"),
            ("Dép quai ngang Adilette", "Dép", "Adidas", "AD-ADL", 890000, 0.66,
             bien_the(["Đen", "Xanh navy"], ["39", "40", "41", "42", "43"]), None, 4, "NPP"),
            ("Biti's dép lê nam", "Dép", "Biti's", "BT-DLN", 159000, 0.60,
             bien_the(["Đen", "Xám"], ["39", "40", "41", "42", "43"]), None, 5, "BITIS"),
            ("Tất cổ ngắn cotton (3 đôi)", "Tất", None, "TAT-3D", 59000, 0.45, None, None, 7, "PK"),
            ("Tất cổ cao thể thao", "Tất", None, "TAT-CC", 35000, 0.43, None, None, 4, "PK"),
            ("Xi đánh giày Kiwi đen 45ml", "Chăm sóc giày", "Kiwi", "KIWI-45", 38000, 0.70, None, None, 2, "PK"),
            ("Bình xịt vệ sinh giày", "Chăm sóc giày", None, "VS-XIT", 89000, 0.50, None, "Dung tích 250ml.", 3, "PK"),
            ("Lót giày êm chân", "Chăm sóc giày", None, "LOT-EM", 45000, 0.40, None, None, 3, "PK"),
        ],
        "ngung_ban": ["LUOI-DL"],
    },
    {
        "key": "ML",
        "ten": "Thời trang Mộc Lan",
        "ngay_tao": "2026-08-24 14:40",
        "giu_hang_phut": 45,
        "mien": "nam",
        "chi_nhanh": [
            {"key": "NT", "ten": "Mộc Lan Nguyễn Trãi", "loai": "STORE",
             "dia_chi": "312 Nguyễn Trãi, Phường 8, Quận 5, TP.HCM", "mo": 9.0, "dong": 22.0},
            {"key": "TD", "ten": "Mộc Lan Thủ Đức", "loai": "STORE",
             "dia_chi": "45 Võ Văn Ngân, Linh Chiểu, TP. Thủ Đức, TP.HCM", "mo": 9.0, "dong": 21.5},
            {"key": "KHO", "ten": "Kho Gò Vấp", "loai": "WAREHOUSE",
             "dia_chi": "88/12 Lê Đức Thọ, Phường 6, Gò Vấp, TP.HCM"},
        ],
        "nhan_su": [
            {"ten": "Lâm Ngọc Lan", "email": "lan.lam@moclanfashion.vn", "vai_tro": "OWNER", "tu_ngay": 0},
            {"ten": "Trịnh Bảo Ngọc", "email": "baongoc@moclanfashion.vn", "vai_tro": "STAFF", "tu_ngay": 1},
            {"ten": "Nguyễn Hoàng Phúc", "email": "phuc.nh@moclanfashion.vn", "vai_tro": "STAFF", "tu_ngay": 5},
            {"ten": "Đặng Thùy Linh", "email": "thuylinh.nt@moclanfashion.vn", "vai_tro": "CASHIER", "chi_nhanh": "NT", "tu_ngay": 2},
            {"ten": "Huỳnh Gia Bảo", "email": "giabao.h@moclanfashion.vn", "vai_tro": "CASHIER", "chi_nhanh": "NT", "tu_ngay": 12},
            {"ten": "Lý Thanh Trúc", "email": "truc.ly@moclanfashion.vn", "vai_tro": "CASHIER", "chi_nhanh": "TD", "tu_ngay": 3},
            {"ten": "Tô Minh Khang", "email": "khang.to@moclanfashion.vn", "vai_tro": "CASHIER", "chi_nhanh": "TD", "tu_ngay": 3},
        ],
        "nha_cung_cap": [
            {"key": "MAY", "ten": "Xưởng may Phương Nam", "dia_chi": "214/9 Lũy Bán Bích, Tân Thới Hòa, Tân Phú, TP.HCM"},
            {"key": "DENIM", "ten": "Công ty TNHH Denim Sài Gòn", "dia_chi": "KCN Tân Tạo, Bình Tân, TP.HCM"},
            {"key": "NPP", "ten": "Đại lý thời trang Gia Khang", "dia_chi": "Chợ An Đông, 34 An Dương Vương, Quận 5, TP.HCM"},
            {"key": "PK", "ten": "Xưởng da Thành Đạt", "dia_chi": "61 Bà Hom, Phường 13, Quận 6, TP.HCM"},
        ],
        "danh_muc": {
            "Áo": ["Áo thun", "Áo sơ mi", "Áo khoác"],
            "Quần": ["Quần jeans", "Quần tây", "Quần short"],
            "Váy đầm": [],
            "Phụ kiện": ["Túi xách", "Thắt lưng"],
        },
        "san_pham": [
            ("Áo thun cotton basic", "Áo thun", "Mộc Lan", "ML-ATB", 149000, 0.42,
             bien_the(["Trắng", "Đen", "Xám tiêu", "Be"], SIZE_AO), "Cotton 100%, định lượng 220gsm.", 10, "MAY"),
            ("Áo thun oversize in chữ", "Áo thun", "Mộc Lan", "ML-AOS", 219000, 0.44,
             bien_the(["Đen", "Kem"], ["M", "L", "XL"]), None, 7, "MAY"),
            ("Áo polo nam cổ dệt", "Áo thun", "Coolmate", "CM-PLO", 299000, 0.55,
             bien_the(["Xanh navy", "Trắng"], SIZE_AO), None, 5, "NPP"),
            ("Áo sơ mi linen tay dài", "Áo sơ mi", "Mộc Lan", "ML-SML", 389000, 0.45,
             bien_the(["Trắng", "Xanh pastel"], SIZE_AO), "Linen pha, mặc mát.", 6, "MAY"),
            ("Áo sơ mi Oxford nam", "Áo sơ mi", "Routine", "RT-SMO", 445000, 0.58,
             bien_the(["Trắng", "Xanh nhạt"], ["M", "L", "XL"]), None, 4, "NPP"),
            ("Áo khoác dù 2 lớp", "Áo khoác", "Mộc Lan", "ML-AKD", 459000, 0.47,
             bien_the(["Đen", "Rêu"], ["M", "L", "XL"]), "Chống nước nhẹ, có mũ.", 4, "MAY"),
            ("Cardigan len mỏng", "Áo khoác", "Mộc Lan", "ML-CDG", 329000, 0.46,
             bien_the(["Kem", "Nâu"], ["S", "M", "L"]), None, 4, "MAY"),
            ("Quần jeans ống suông", "Quần jeans", "Mộc Lan", "ML-QJS", 429000, 0.50,
             bien_the(["Xanh nhạt", "Xanh đậm"], SIZE_QUAN), None, 7, "DENIM"),
            ("Quần jeans Levi's 511 Slim", "Quần jeans", "Levi's", "LV-511", 1290000, 0.68,
             bien_the(["Xanh đậm"], SIZE_QUAN), None, 3, "NPP"),
            ("Quần tây công sở ống đứng", "Quần tây", "Mộc Lan", "ML-QTD", 359000, 0.47,
             bien_the(["Đen", "Xám"], SIZE_QUAN), None, 5, "MAY"),
            ("Quần short kaki", "Quần short", "Mộc Lan", "ML-QSK", 199000, 0.45,
             bien_the(["Be", "Đen", "Xanh rêu"], SIZE_QUAN), None, 6, "MAY"),
            ("Đầm suông hoa nhí", "Váy đầm", "Mộc Lan", "ML-DSH", 399000, 0.46,
             bien_the(["Hoa nhí xanh", "Hoa nhí đỏ"], ["S", "M", "L"]), "Voan lụa, có lớp lót.", 6, "MAY"),
            ("Chân váy chữ A", "Váy đầm", "Mộc Lan", "ML-CVA", 249000, 0.45,
             bien_the(["Đen", "Be"], ["S", "M", "L"]), None, 5, "MAY"),
            ("Túi tote canvas", "Túi xách", "Mộc Lan", "ML-TOT", 129000, 0.40, None, None, 4, "PK"),
            ("Túi đeo chéo da PU", "Túi xách", None, "TUI-DC", 259000, 0.45,
             [{"Màu": "Đen"}, {"Màu": "Nâu"}], None, 3, "PK"),
            ("Thắt lưng da bò khóa kim", "Thắt lưng", None, "TL-DB", 279000, 0.48,
             [{"Màu": "Đen"}, {"Màu": "Nâu"}], None, 2, "PK"),
        ],
        "ngung_ban": ["RT-SMO"],
    },
    {
        "key": "PA",
        "ten": "Tạp hóa Phúc An",
        "ngay_tao": "2026-09-02 07:55",
        "giu_hang_phut": 60,
        "mien": "trung",
        "chi_nhanh": [
            {"key": "HC", "ten": "Phúc An Hải Châu", "loai": "STORE",
             "dia_chi": "57 Lê Đình Dương, Phước Ninh, Hải Châu, Đà Nẵng", "mo": 6.5, "dong": 22.0},
        ],
        "nhan_su": [
            {"ten": "Hồ Văn Phúc", "email": "phuc.ho@taphoaphucan.vn", "vai_tro": "OWNER", "tu_ngay": 0},
            {"ten": "Lê Thị Kim Oanh", "email": "kimoanh@taphoaphucan.vn", "vai_tro": "STAFF", "tu_ngay": 4},
            {"ten": "Hồ Thị An", "email": "an.ho@taphoaphucan.vn", "vai_tro": "CASHIER", "chi_nhanh": "HC", "tu_ngay": 0},
            {"ten": "Ngô Văn Tài", "email": "tai.ngo@taphoaphucan.vn", "vai_tro": "CASHIER", "chi_nhanh": "HC", "tu_ngay": 6},
        ],
        "nha_cung_cap": [
            {"key": "TH", "ten": "NPP Tổng hợp Hòa Khánh", "dia_chi": "Kiệt 45 Âu Cơ, Hòa Khánh Bắc, Liên Chiểu, Đà Nẵng"},
            {"key": "SUA", "ten": "Đại lý sữa Thanh Hương", "dia_chi": "102 Ông Ích Khiêm, Thanh Khê, Đà Nẵng"},
            {"key": "BIA", "ten": "Đại lý bia nước giải khát Quang Trung", "dia_chi": "18 Điện Biên Phủ, Thanh Khê, Đà Nẵng"},
            {"key": "GAO", "ten": "Cửa hàng gạo Hai Lúa", "dia_chi": "Chợ Hòa Cường, Hải Châu, Đà Nẵng"},
        ],
        "danh_muc": {
            "Thực phẩm khô": ["Mì gói", "Gia vị", "Gạo"],
            "Đồ uống": ["Nước ngọt", "Bia", "Sữa"],
            "Hóa mỹ phẩm": [],
            "Bánh kẹo": [],
        },
        "san_pham": [
            ("Mì Hảo Hảo tôm chua cay", "Mì gói", "Acecook", "HH-TCC", 4500, 0.80,
             [{"Quy cách": "Gói 75g"}, {"Quy cách": "Thùng 30 gói"}], None, 10, "TH"),
            ("Mì Omachi xốt bò hầm", "Mì gói", "Masan", "OMC-BH", 8000, 0.79, None, None, 5, "TH"),
            ("Nước mắm Nam Ngư 500ml", "Gia vị", "Masan", "NN-500", 38000, 0.78, None, None, 5, "TH"),
            ("Dầu ăn Neptune Gold 1L", "Gia vị", "Neptune", "NEP-1L", 62000, 0.82, None, None, 5, "TH"),
            ("Đường Biên Hòa 1kg", "Gia vị", "Biên Hòa", "DBH-1K", 32000, 0.84, None, None, 4, "TH"),
            ("Bột ngọt Ajinomoto 400g", "Gia vị", "Ajinomoto", "AJI-400", 36000, 0.81, None, None, 3, "TH"),
            ("Gạo ST25 túi 5kg", "Gạo", None, "ST25-5K", 185000, 0.83, None, "Gạo Sóc Trăng, vụ mới.", 4, "GAO"),
            ("Gạo thơm Jasmine túi 5kg", "Gạo", None, "JAS-5K", 125000, 0.84, None, None, 3, "GAO"),
            ("Coca-Cola lon 320ml", "Nước ngọt", "Coca-Cola", "COCA-320", 10000, 0.78,
             [{"Quy cách": "Lon"}, {"Quy cách": "Lốc 6 lon"}], None, 8, "BIA"),
            ("Nước suối Lavie 500ml", "Nước ngọt", "Lavie", "LAV-500", 6000, 0.65, None, None, 7, "BIA"),
            ("Trà xanh Không Độ 455ml", "Nước ngọt", "Tân Hiệp Phát", "KD-455", 11000, 0.72, None, None, 5, "BIA"),
            ("Bia Larue lon 330ml", "Bia", "Larue", "LARUE-330", 13000, 0.82,
             [{"Quy cách": "Lon"}, {"Quy cách": "Thùng 24 lon"}], None, 9, "BIA"),
            ("Bia Huda lon 330ml", "Bia", "Huda", "HUDA-330", 14000, 0.83,
             [{"Quy cách": "Lon"}, {"Quy cách": "Thùng 24 lon"}], None, 6, "BIA"),
            ("Sữa tươi Vinamilk 1L", "Sữa", "Vinamilk", "VNM-1L", 34000, 0.85,
             [{"Loại": "Có đường"}, {"Loại": "Không đường"}], None, 6, "SUA"),
            ("Sữa đặc Ông Thọ lon 380g", "Sữa", "Vinamilk", "OT-380", 26000, 0.84, None, None, 4, "SUA"),
            ("Sữa chua Vinamilk lốc 4 hộp", "Sữa", "Vinamilk", "SC-L4", 28000, 0.80, None, None, 5, "SUA"),
            ("Bột giặt OMO 2,9kg", "Hóa mỹ phẩm", "OMO", "OMO-29", 159000, 0.83, None, None, 3, "TH"),
            ("Nước rửa chén Sunlight 750g", "Hóa mỹ phẩm", "Sunlight", "SUN-750", 29000, 0.80, None, None, 4, "TH"),
            ("Kem đánh răng P/S 230g", "Hóa mỹ phẩm", "P/S", "PS-230", 35000, 0.78, None, None, 3, "TH"),
            ("Dầu gội Clear mát lạnh 630g", "Hóa mỹ phẩm", "Clear", "CLR-630", 169000, 0.80, None, None, 2, "TH"),
            ("Bánh Chocopie hộp 12 cái", "Bánh kẹo", "Orion", "CHP-12", 59000, 0.80, None, None, 4, "TH"),
            ("Bánh quy Cosy Marie 144g", "Bánh kẹo", "Kinh Đô", "COSY-144", 18000, 0.76, None, None, 3, "TH"),
            ("Kẹo cao su Doublemint", "Bánh kẹo", "Wrigley", "DM-THANH", 5000, 0.70, None, None, 3, "TH"),
            ("Snack Oishi tôm cay 40g", "Bánh kẹo", "Oishi", "OIS-40", 6000, 0.72, None, None, 5, "TH"),
        ],
        "ngung_ban": ["DM-THANH"],
    },
]

HO = ["Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô",
      "Dương", "Lý", "Trương", "Đinh", "Mai", "Lâm", "Tạ", "Cao", "Hà", "Kiều"]
HO_W = [32, 12, 9, 7, 5, 4, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1]
DEM_NAM = ["Văn", "Minh", "Quốc", "Đức", "Hữu", "Thành", "Anh", "Gia", "Hoàng", "Công", "Tuấn", ""]
DEM_NU = ["Thị", "Ngọc", "Thu", "Thanh", "Phương", "Bảo", "Khánh", "Mỹ", "Kim", "Hoài", ""]
TEN_NAM = ["Hùng", "Dũng", "Nam", "Long", "Tùng", "Khoa", "Phong", "Hiếu", "Trung", "Quân", "Sơn", "Thắng",
           "Vinh", "Đạt", "Hải", "Kiên", "Lộc", "Nghĩa", "Tâm", "Bình", "Khải", "Toàn", "Nhân", "Thịnh"]
TEN_NU = ["Lan", "Hương", "Trang", "Linh", "Ngọc", "Thảo", "Hà", "Vy", "Nhi", "Quỳnh", "Yến", "Hạnh",
          "Nhung", "Phương", "Dung", "Tuyết", "Uyên", "Giang", "Châu", "My", "Trâm", "Hiền", "Loan", "Xuân"]

DIA_CHI = {
    "bac": [
        ("Cầu Giấy", "Hà Nội", ["Xuân Thủy", "Hoàng Quốc Việt", "Trần Duy Hưng", "Nguyễn Khang"]),
        ("Đống Đa", "Hà Nội", ["Tây Sơn", "Thái Hà", "Chùa Bộc", "Xã Đàn"]),
        ("Thanh Xuân", "Hà Nội", ["Nguyễn Trãi", "Khuất Duy Tiến", "Lê Văn Lương"]),
        ("Hà Đông", "Hà Nội", ["Quang Trung", "Tô Hiệu", "Vạn Phúc"]),
        ("Ngô Quyền", "Hải Phòng", ["Lạch Tray", "Văn Cao"]),
        ("TP. Bắc Ninh", "Bắc Ninh", ["Ngô Gia Tự", "Lý Thái Tổ"]),
        ("TP. Nam Định", "Nam Định", ["Trần Hưng Đạo", "Hàn Thuyên"]),
        ("Quận 7", "TP.HCM", ["Nguyễn Thị Thập", "Huỳnh Tấn Phát"]),
        ("TP. Vinh", "Nghệ An", ["Lê Lợi", "Nguyễn Văn Cừ"]),
    ],
    "nam": [
        ("Quận 3", "TP.HCM", ["Võ Văn Tần", "Cách Mạng Tháng Tám", "Nam Kỳ Khởi Nghĩa"]),
        ("Bình Thạnh", "TP.HCM", ["Xô Viết Nghệ Tĩnh", "Điện Biên Phủ", "Bạch Đằng"]),
        ("Gò Vấp", "TP.HCM", ["Quang Trung", "Phan Văn Trị", "Nguyễn Oanh"]),
        ("Tân Bình", "TP.HCM", ["Cộng Hòa", "Trường Chinh", "Hoàng Văn Thụ"]),
        ("TP. Thủ Đức", "TP.HCM", ["Kha Vạn Cân", "Đặng Văn Bi", "Lê Văn Việt"]),
        ("TP. Biên Hòa", "Đồng Nai", ["Phạm Văn Thuận", "Đồng Khởi"]),
        ("TP. Thủ Dầu Một", "Bình Dương", ["Yersin", "Đại lộ Bình Dương"]),
        ("Ninh Kiều", "Cần Thơ", ["30 Tháng 4", "Mậu Thân"]),
        ("Hoàn Kiếm", "Hà Nội", ["Hàng Bài", "Lý Thường Kiệt"]),
    ],
    "trung": [
        ("Hải Châu", "Đà Nẵng", ["Lê Đình Dương", "Hoàng Diệu", "Núi Thành", "Phan Châu Trinh"]),
        ("Thanh Khê", "Đà Nẵng", ["Hà Huy Tập", "Lê Duẩn"]),
        ("Sơn Trà", "Đà Nẵng", ["Ngô Quyền", "Hồ Nghinh"]),
    ],
}

LY_DO_HUY_KHACH = [
    "Khách đổi ý, không mua nữa", "Khách đặt nhầm size", "Khách hủy trên sàn", "Khách muốn đổi màu khác",
    "Không liên lạc được với khách", "Khách đặt trùng đơn", "Khách chê phí ship cao",
]
LY_DO_HUY_SAU_GIAO = [
    "Giao hàng không thành công, hoàn về kho", "Khách từ chối nhận hàng", "Khách trả hàng do không vừa size",
    "Shipper báo khách không nghe máy 3 lần",
]
