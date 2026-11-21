# -*- coding: utf-8 -*-
"""Sinh du lieu van hanh mau cho OISM bang cach mo phong hoat dong cua hang theo thoi gian.

Khong chen so lieu "dep": moi ngay co so don ngau nhien (thu Bay/Chu nhat dong hon, ngay mua sale 10/10, 11/11,
20/10 tang vot, ngay mua giam), gio ban theo khung cao diem, khach chon size theo phan bo thuc te, het hang thi
doi size hoac bo. Ton kho, gia von binh quan, so cai, giu hang, canh bao ton thap deu tinh dung theo nghiep vu
cua backend (PurchaseReceiptService, ReservationService, StockTransferService, StocktakeService, StockAlertService).

Chay:  python sinh_du_lieu.py [--seed N] [--out du-lieu-mau.sql] [--tai-khoan tai-khoan.local.md]
"""
import argparse
import heapq
import itertools
import json
import math
import random
import secrets
import string
import uuid
from datetime import datetime, timedelta, timezone
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path

import bcrypt

from danh_muc import (CUA_HANG, DIA_CHI, HO, HO_W, DEM_NAM, DEM_NU, TEN_NAM, TEN_NU,
                      LY_DO_HUY_KHACH, LY_DO_HUY_SAU_GIAO)

VN = timezone(timedelta(hours=7))
UTC = timezone.utc
D2 = Decimal("0.01")

TRONG_SO_SIZE = {
    "39": 0.12, "40": 0.22, "41": 0.28, "42": 0.25, "43": 0.13,  # nam
    "35": 0.10, "36": 0.22, "37": 0.30, "38": 0.25,  # nu (39 dung chung)
    "S": 0.20, "M": 0.35, "L": 0.30, "XL": 0.15,
    "28": 0.15, "29": 0.25, "30": 0.27, "31": 0.20, "32": 0.13,
}
GIO_CUA_HANG = {9: .4, 10: .6, 11: .8, 12: 1.0, 13: .7, 14: .6, 15: .7, 16: .9, 17: 1.2, 18: 1.5, 19: 1.7,
                20: 1.5, 21: .8, 8: .3, 22: .2}
GIO_TAP_HOA = {6: .8, 7: 1.4, 8: 1.0, 9: .6, 10: .6, 11: 1.0, 12: .8, 13: .5, 14: .5, 15: .6, 16: 1.0,
               17: 1.5, 18: 1.6, 19: 1.4, 20: 1.1, 21: .7}
GIO_ONLINE = {0: .5, 1: .25, 2: .1, 3: .05, 4: .05, 5: .1, 6: .3, 7: .5, 8: .7, 9: .9, 10: 1, 11: 1.1, 12: 1.2,
              13: 1, 14: .9, 15: .9, 16: .9, 17: .9, 18: 1, 19: 1.3, 20: 1.6, 21: 1.8, 22: 1.5, 23: 1.0}
THU = [0.8, 0.85, 0.9, 0.95, 1.1, 1.45, 1.35]

# so don POS / ngay theo chi nhanh, so don online / ngay theo cua hang
LAMBDA_POS = {"HT": {"CG": 7.5, "PH": 5.0}, "ML": {"NT": 9.0, "TD": 6.5}, "PA": {"HC": 36.0}}
LAMBDA_ONLINE = {"HT": 6.0, "ML": 9.5, "PA": 1.6}
KENH_ONLINE = {"HT": [("SHOPEE", .45), ("TIKTOK", .33), ("MANUAL", .22)],
               "ML": [("SHOPEE", .34), ("TIKTOK", .48), ("MANUAL", .18)],
               "PA": [("MANUAL", 1.0)]}
TY_LE_QR = {"HT": .45, "ML": .58, "PA": .22}


class Raw(str):
    """Chuoi SQL chen thang (khong dat trong nhay)."""


def sql_val(v):
    if v is None:
        return "NULL"
    if isinstance(v, Raw):
        return str(v)
    if isinstance(v, bool):
        return "TRUE" if v else "FALSE"
    if isinstance(v, int):
        return str(v)
    if isinstance(v, Decimal):
        return format(v.quantize(D2), "f")
    if isinstance(v, uuid.UUID):
        return f"'{v}'"
    if isinstance(v, datetime):
        return "'" + v.isoformat(sep=" ", timespec="microseconds") + "'"
    if isinstance(v, dict):
        return "'" + json.dumps(v, ensure_ascii=False).replace("'", "''") + "'::jsonb"
    return "'" + str(v).replace("'", "''") + "'"


def lam_tron(x, buoc):
    return int(Decimal(x / buoc).quantize(Decimal(1), ROUND_HALF_UP)) * buoc


def ean13(rng, tien_to="893"):
    so = tien_to + "".join(str(rng.randint(0, 9)) for _ in range(12 - len(tien_to)))
    tong = sum(int(c) * (3 if i % 2 else 1) for i, c in enumerate(so))
    return so + str((10 - tong % 10) % 10)


def ma_mau(mau):
    bang = str.maketrans("ÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬĐÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴ",
                         "AAAAAAAAAAAAAAAAADEEEEEEEEEEEIIIIIOOOOOOOOOOOOOOOOOUUUUUUUUUUUYYYYY")
    tu = mau.upper().translate(bang).replace("/", " ").split()
    return "".join(t[0] for t in tu) if len(tu) > 1 else tu[0][:3]


class BoSinh:
    def __init__(self, seed, now, mat_khau="admin123"):
        self.rng = random.Random(seed)
        self.mat_khau_chung = mat_khau
        self.now = now
        self.rows = {t: [] for t in [
            "tenant", "branch", "app_user", "category", "brand", "product", "sku", "supplier",
            "purchase_receipt", "purchase_receipt_item", "stock_transfer", "stock_transfer_item",
            "stocktake", "stocktake_item", "orders", "order_item", "order_status_history", "payment",
            "inventory_transaction", "stock_balance", "stock_alert", "channel_credential"]}
        self.tai_khoan = []
        self.sdt_da_dung = set()

    # ---------- tien ich ----------
    def uid(self):
        return uuid.UUID(int=self.rng.getrandbits(128), version=4)

    def sdt(self):
        while True:
            dau = self.rng.choice(["090", "091", "093", "094", "096", "097", "098", "086", "088", "089",
                                   "032", "033", "034", "035", "036", "037", "038", "039", "070", "076",
                                   "077", "078", "079", "081", "083", "084", "085", "056", "058"])
            so = dau + "".join(str(self.rng.randint(0, 9)) for _ in range(7))
            if so not in self.sdt_da_dung:
                self.sdt_da_dung.add(so)
                return so

    def ho_ten(self, nu=None):
        if nu is None:
            nu = self.rng.random() < 0.55
        ho = self.rng.choices(HO, HO_W)[0]
        dem = self.rng.choice(DEM_NU if nu else DEM_NAM)
        ten = self.rng.choice(TEN_NU if nu else TEN_NAM)
        return " ".join(x for x in [ho, dem, ten] if x)

    def dia_chi(self, mien):
        quan, tinh, duong = self.rng.choice(DIA_CHI[mien])
        r = self.rng.random()
        if r < 0.55:
            so = str(self.rng.randint(1, 420))
        elif r < 0.85:
            so = f"{self.rng.randint(2, 180)}/{self.rng.randint(1, 60)}"
        else:
            so = f"Ngõ {self.rng.randint(3, 250)}, số {self.rng.randint(1, 40)}"
        them = ""
        if self.rng.random() < 0.12:
            them = self.rng.choice([" (gọi trước khi giao)", " (giao giờ hành chính)", " - tầng 3",
                                    " (gửi bảo vệ tòa nhà)"])
        return f"{so} {duong}, {quan}, {tinh}{them}"

    def poisson(self, lam):
        if lam <= 0:
            return 0
        if lam > 30:
            return max(0, int(round(self.rng.gauss(lam, math.sqrt(lam)))))
        l, k, p = math.exp(-lam), 0, 1.0
        while True:
            p *= self.rng.random()
            if p <= l:
                return k
            k += 1

    def gio_ngau_nhien(self, ngay, profile, mo=None, dong=None):
        gio = [h for h in profile if (mo is None or h >= int(mo)) and (dong is None or h < dong)]
        h = self.rng.choices(gio, [profile[x] for x in gio])[0]
        phut_dau = 0
        if mo is not None and h == int(mo):
            phut_dau = int((mo - int(mo)) * 60)
        phut_cuoi = 59
        if dong is not None and h == int(dong):
            phut_cuoi = int((dong - int(dong)) * 60)
        return ngay.replace(hour=h, minute=self.rng.randint(phut_dau, phut_cuoi), second=self.rng.randint(0, 59),
                            microsecond=self.rng.randint(0, 999999))

    def mat_khau(self):
        chu = string.ascii_letters + string.digits
        return (secrets.choice(string.ascii_uppercase) + "".join(secrets.choice(chu) for _ in range(9))
                + secrets.choice("@#!$%") + secrets.choice(string.digits))

    # ---------- mo phong mot cua hang ----------
    def mo_phong(self, ch):
        S = CuaHang(self, ch)
        S.chay()


class CuaHang:
    def __init__(self, g: BoSinh, ch):
        self.g, self.rng, self.ch = g, g.rng, ch
        self.key = ch["key"]
        self.t0 = datetime.strptime(ch["ngay_tao"], "%Y-%m-%d %H:%M").replace(tzinfo=VN)
        self.now = g.now
        self.id = g.uid()
        self.q = []
        self.seq = itertools.count()
        self.on_hand, self.reserved = {}, {}
        self.bal_row = {}
        self.alert_open = {}
        self.ma_da_dung = set()

    # --- hang doi su kien ---
    def hen(self, t, fn, *args):
        if t <= self.now:
            heapq.heappush(self.q, (t, next(self.seq), fn, args))
            return True
        return False

    def chay(self):
        self.tao_nen()
        ngay = self.t0.replace(hour=0, minute=0, second=0, microsecond=0)
        d = 0
        while ngay <= self.now:
            self.len_lich_ngay(ngay, d)
            ngay += timedelta(days=1)
            d += 1
        while self.q:
            t, _, fn, args = heapq.heappop(self.q)
            fn(t, *args)
        self.ket_thuc()

    # --- du lieu nen ---
    def tao_nen(self):
        g, rng, t0 = self.g, self.rng, self.t0
        g.rows["tenant"].append((self.id, self.ch["ten"], "ACTIVE", self.ch["giu_hang_phut"], t0,
                                 t0 + timedelta(days=rng.randint(3, 20), minutes=rng.randint(0, 600))))
        self.branches = {}
        for i, b in enumerate(self.ch["chi_nhanh"]):
            bid = g.uid()
            tc = t0 + timedelta(minutes=3 + i * rng.randint(2, 9), seconds=rng.randint(0, 59))
            self.branches[b["key"]] = dict(b, id=bid)
            g.rows["branch"].append((self.id, bid, b["ten"], b["loai"], b["dia_chi"], True, tc, tc))
        self.kho = self.branches.get("KHO") or next(iter(self.branches.values()))
        self.stores = [b for b in self.branches.values() if b["loai"] == "STORE"]

        self.users = []
        for u in self.ch["nhan_su"]:
            uid = g.uid()
            tc = t0 + timedelta(days=u["tu_ngay"], hours=rng.uniform(0.1, 6) if u["tu_ngay"] else 0,
                                minutes=rng.randint(0, 59) if u["tu_ngay"] else 0)
            nghi = t0 + timedelta(days=u["nghi_ngay"], hours=rng.uniform(8, 11)) if "nghi_ngay" in u else None
            if nghi and nghi > self.now:
                nghi = None
            sdt = g.sdt() if rng.random() < 0.85 else None
            mk = g.mat_khau_chung or g.mat_khau()
            ph = bcrypt.hashpw(mk.encode(), bcrypt.gensalt(10)).decode()
            bid = self.branches[u["chi_nhanh"]]["id"] if u.get("chi_nhanh") else None
            row = dict(id=uid, ten=u["ten"], role=u["vai_tro"], branch=u.get("chi_nhanh"), tu=tc, nghi=nghi)
            self.users.append(row)
            g.rows["app_user"].append((uid, self.id, u["email"], sdt, ph, u["ten"], u["vai_tro"], bid,
                                       nghi is None, tc, nghi or tc))
            g.tai_khoan.append((self.ch["ten"], u["vai_tro"], u.get("chi_nhanh") and self.branches[u["chi_nhanh"]]["ten"],
                                u["ten"], u["email"], sdt, mk, nghi is None))
        self.owner = self.users[0]

        # danh muc
        self.cat = {}
        tc = t0 + timedelta(hours=rng.uniform(1, 5))
        for cha, cons in self.ch["danh_muc"].items():
            cid = g.uid()
            self.cat[cha] = cid
            g.rows["category"].append((cid, self.id, None, cha, tc, tc))
            for con in cons:
                tc += timedelta(seconds=rng.randint(15, 240))
                ccid = g.uid()
                self.cat[con] = ccid
                g.rows["category"].append((ccid, self.id, cid, con, tc, tc))
            tc += timedelta(seconds=rng.randint(20, 300))

        self.brand = {}
        for p in self.ch["san_pham"]:
            if p[2] and p[2] not in self.brand:
                tc += timedelta(seconds=rng.randint(10, 120))
                bid = g.uid()
                self.brand[p[2]] = bid
                g.rows["brand"].append((bid, self.id, p[2], tc, tc))

        self.ncc = {}
        for n in self.ch["nha_cung_cap"]:
            tc += timedelta(minutes=rng.randint(1, 30))
            nid = g.uid()
            self.ncc[n["key"]] = nid
            g.rows["supplier"].append((nid, self.id, n["ten"], g.sdt() if rng.random() < .9 else None,
                                       n["dia_chi"], True, tc, tc))

        # san pham + SKU
        self.skus = []
        self.products = []
        tp = t0 + timedelta(days=rng.choice([0, 1]), hours=rng.uniform(2, 8))
        for (ten, cat, brand, ma, gia, tyle, variants, mota, pho_bien, ncc) in self.ch["san_pham"]:
            tp += timedelta(minutes=rng.randint(3, 25), seconds=rng.randint(0, 59))
            pid = g.uid()
            ngung = ma in self.ch.get("ngung_ban", [])
            pho_bien = pho_bien * rng.uniform(0.7, 1.35)
            prod = dict(id=pid, ten=ten, pho_bien=pho_bien, skus=[], ncc=ncc)
            self.products.append(prod)
            ngay_ngung = None
            if ngung:
                ngay_ngung = self.t0 + timedelta(days=rng.randint(38, 55), hours=rng.uniform(9, 17))
            g.rows["product"].append((pid, self.id, self.cat[cat], self.brand.get(brand), ten, mota,
                                      variants is not None, "INACTIVE" if ngung and ngay_ngung <= self.now else "ACTIVE",
                                      tp, ngay_ngung if ngung and ngay_ngung <= self.now else tp))
            for attrs in (variants or [{}]):
                code = ma
                w = 1.0
                for k_, v_ in attrs.items():
                    if k_ == "Size":
                        code += "-" + v_
                        w *= TRONG_SO_SIZE.get(v_, 0.15)
                    elif k_ == "Màu":
                        code += "-" + ma_mau(v_)
                        w *= rng.uniform(0.55, 1.45)
                    else:
                        code += "-" + ma_mau(v_)
                        w *= rng.uniform(0.6, 1.4)
                quy_cach = attrs.get("Quy cách", "")
                gia_sku = gia
                if quy_cach.startswith("Thùng") or quy_cach.startswith("Lốc"):
                    n_ = int("".join(c for c in quy_cach if c.isdigit()))
                    gia_sku = lam_tron(gia * n_ * rng.uniform(0.9, 0.95), 1000)
                    w *= 0.12
                buoc = 500 if gia_sku < 50000 else 1000
                von = lam_tron(gia_sku * tyle * rng.uniform(0.97, 1.03), buoc if gia_sku > 20000 else 100)
                si = None if gia_sku < 50000 and rng.random() < 0.5 else lam_tron(gia_sku * rng.uniform(0.84, 0.92), buoc)
                if gia_sku >= 1000000:
                    rp = rng.choice([1, 2, 2, 2])
                elif gia_sku >= 120000:
                    rp = rng.choice([2, 2, 3, 3, 4])
                else:
                    rp = rng.choice([5, 6, 8, 10, 12])
                if self.key == "PA":
                    rp = rng.choice([6, 8, 10, 12, 15, 20]) if gia_sku < 50000 else rng.choice([3, 4, 5])
                if quy_cach.startswith("Thùng") or quy_cach.startswith("Lốc"):
                    rp = rng.choice([1, 2, 2])
                if "Size" in attrs:
                    rp = rng.choice([1, 1, 1, 2])
                if rng.random() < 0.08:
                    rp = 0
                tien_to = "893" if (brand in (None, "Biti's", "Ananas", "Mộc Lan", "Coolmate", "Routine", "Acecook",
                                              "Masan", "Vinamilk", "Biên Hòa", "Kinh Đô", "Tân Hiệp Phát", "Huda",
                                              "Larue", "Lavie", "Neptune")) else rng.choice(["885", "889", "400", "050", "888"])
                sid = g.uid()
                sku = dict(id=sid, code=code, attrs=attrs, barcode=ean13(rng, tien_to) if rng.random() < .93 else None,
                           gia=gia_sku, si=si, von_goc=von, avg=Decimal(0), rp=rp, prod=prod, w=w,
                           ngung=ngay_ngung, tao=tp + timedelta(seconds=rng.randint(5, 200)), ncc=ncc,
                           le=gia_sku < 15000)
                prod["skus"].append(sku)
                self.skus.append(sku)
        self.tinh_nhu_cau()

        # khoa kenh san
        if self.key in ("HT", "ML"):
            for kenh in ("SHOPEE", "TIKTOK"):
                tk = self.t0 + timedelta(days=rng.randint(4, 12), hours=rng.uniform(8, 20))
                if kenh == "SHOPEE" and rng.random() < 0.7:
                    cu = g.uid()
                    g.rows["channel_credential"].append((cu, self.id, kenh, secrets.token_hex(32), False, tk,
                                                         tk + timedelta(days=rng.randint(10, 30))))
                    tk = tk + timedelta(days=rng.randint(10, 30))
                g.rows["channel_credential"].append((g.uid(), self.id, kenh, secrets.token_hex(32), True, tk, tk))

    def tinh_nhu_cau(self):
        """Uoc luong so luong ban / ngay cua tung SKU de dat muc ton, muc nhap."""
        tong_p = sum(p["pho_bien"] for p in self.products)
        for s in self.skus:
            p = s["prod"]
            tong_w = sum(x["w"] for x in p["skus"])
            s["p"] = p["pho_bien"] / tong_p * s["w"] / tong_w
            sl_tb = 3.0 if s["le"] else 1.15
            dong = 1.6 if self.key == "PA" else 1.15
            s["can_store"] = {}
            for b in self.stores:
                lam = LAMBDA_POS[self.key][b["key"]] * 1.05
                s["can_store"][b["key"]] = lam * dong * s["p"] * sl_tb
            s["can_online"] = LAMBDA_ONLINE[self.key] * 1.1 * s["p"] * sl_tb

    # --- ton kho ---
    def kb(self, b, s):
        return (b["id"], s["id"])

    def co_san(self, b, s):
        k = self.kb(b, s)
        return self.on_hand.get(k, 0) - self.reserved.get(k, 0)

    def cham_ton(self, t, b, s):
        k = self.kb(b, s)
        if k not in self.bal_row:
            self.bal_row[k] = [self.g.uid(), t, t]
            self.on_hand.setdefault(k, 0)
            self.reserved.setdefault(k, 0)
        self.bal_row[k][2] = t
        # FR-REP-03 canh bao ton thap
        av = self.co_san(b, s)
        low = s["rp"] > 0 and av <= s["rp"]
        if low and k not in self.alert_open:
            self.alert_open[k] = [self.g.uid(), s["rp"], av, t]
        elif not low and k in self.alert_open:
            aid, th, av0, tc = self.alert_open.pop(k)
            self.g.rows["stock_alert"].append((aid, self.id, b["id"], s["id"], "CLOSED", th, av0, tc, t, t))

    def ghi_so(self, t, b, s, huong, ly_do, sl, gia, ref_type, ref_id, nguoi):
        k = self.kb(b, s)
        self.on_hand[k] = self.on_hand.get(k, 0) + (sl if huong == "IN" else -sl)
        assert self.on_hand[k] >= 0 and self.on_hand[k] >= self.reserved.get(k, 0), (s["code"], ly_do)
        self.g.rows["inventory_transaction"].append((self.g.uid(), self.id, b["id"], s["id"], huong, ly_do, sl,
                                                     self.on_hand[k], gia, ref_type, ref_id, nguoi, t))
        self.cham_ton(t, b, s)

    def tong_ton(self, s):
        return sum(v for (bid, sid), v in self.on_hand.items() if sid == s["id"])

    # --- nguoi thao tac ---
    def dang_lam(self, t, role, branch=None):
        return [u for u in self.users if u["role"] == role and u["tu"] <= t and (u["nghi"] is None or u["nghi"] > t)
                and (branch is None or u["branch"] == branch)]

    def nhan_vien(self, t):
        ds = self.dang_lam(t, "STAFF")
        if not ds or self.rng.random() < 0.15:
            return self.owner
        return self.rng.choice(ds)

    def thu_ngan(self, t, b):
        ds = sorted(self.dang_lam(t, "CASHIER", b["key"]), key=lambda u: u["tu"])
        if not ds or self.rng.random() < (0.3 if self.key == "PA" else 0.05):
            return self.owner
        if len(ds) == 1:
            return ds[0]
        ca = 0 if t.hour < 15 else 1
        if self.rng.random() < 0.15:
            ca = 1 - ca
        return ds[min(ca, len(ds) - 1)] if len(ds) == 2 else self.rng.choice(ds)

    def ma(self, tien_to, t):
        ngay = t.astimezone(UTC).strftime("%Y%m%d")
        while True:
            c = f"{tien_to}-{ngay}-{self.rng.randint(0, 9999):04d}"
            if c not in self.ma_da_dung:
                self.ma_da_dung.add(c)
                return c

    def con_ban(self, s, t):
        return s["ngung"] is None or s["ngung"] > t

    # ---------- lich hang ngay ----------
    def he_so_ngay(self, ngay, d, online):
        rng = self.rng
        f = THU[ngay.weekday()]
        f *= min(1.0, 0.45 + d / 20)
        if ngay.day in (1, 2, 3, 15, 16):
            f *= 1.12
        md = (ngay.month, ngay.day)
        if md == (9, 2):
            f *= 1.5
        if md == (10, 20):
            f *= 1.9 if self.key == "ML" else 1.3
        if md in ((10, 10), (11, 11)) and online and self.key != "PA":
            f *= 2.3 if md == (10, 10) else 3.6
        if md in ((10, 9), (11, 10)) and online and self.key != "PA":
            f *= 0.75  # khach cho sale
        if md == (11, 20) and self.key == "ML":
            f *= 1.25
        if self.key == "HT" and ngay.month == 9 and ngay.day < 20:
            f *= 1.12  # mua tuu truong
        f *= math.exp(rng.gauss(0, 0.22))
        if not online and rng.random() < 0.09:
            f *= rng.uniform(0.45, 0.7)  # mua lon, vang khach
        return f

    def len_lich_ngay(self, ngay, d):
        rng = self.rng
        if d == 0:
            return
        # nhap hang
        if d == 1:
            self.hen(ngay.replace(hour=9) + timedelta(minutes=rng.randint(0, 90)), self.kiem_nhap, True)
        elif self.key == "PA":
            if d % rng.choice([3, 3, 4]) == 0:
                self.hen(ngay.replace(hour=6, minute=rng.randint(0, 40)), self.kiem_nhap, False)
        elif ngay.weekday() in (0, 3) or rng.random() < 0.06:
            self.hen(ngay.replace(hour=8) + timedelta(minutes=rng.randint(10, 150)), self.kiem_nhap, False)
        # chuyen kho ra cua hang
        if self.kho["loai"] == "WAREHOUSE" and d >= 2:
            for b in self.stores:
                if d == 2 or rng.random() < 0.38:
                    self.hen(ngay.replace(hour=9) + timedelta(minutes=rng.randint(0, 240)), self.kiem_chuyen, b)
        # ban hang
        if d >= 3 or (self.kho["loai"] == "STORE" and d >= 2):
            for b in self.stores:
                n = self.g.poisson(LAMBDA_POS[self.key][b["key"]] * self.he_so_ngay(ngay, d, False))
                prof = GIO_TAP_HOA if self.key == "PA" else GIO_CUA_HANG
                for _ in range(n):
                    self.hen(self.gio_ngau_nhien_cn(ngay, prof, b), self.ban_pos, b)
            n = self.g.poisson(LAMBDA_ONLINE[self.key] * self.he_so_ngay(ngay, d, True))
            for _ in range(n):
                kenh = rng.choices([k for k, _ in KENH_ONLINE[self.key]], [w for _, w in KENH_ONLINE[self.key]])[0]
                prof = GIO_TAP_HOA if kenh == "MANUAL" and self.key == "PA" else GIO_ONLINE
                t = self.g.gio_ngau_nhien(ngay, prof)
                if kenh == "MANUAL" and t.hour < 8:
                    t = t.replace(hour=rng.randint(8, 20))
                self.hen(t, self.don_online, kenh)
        # kiem ke cuoi thang
        mai = ngay + timedelta(days=1)
        if mai.day == 1 and d > 10:
            for b in self.stores:
                self.hen(ngay.replace(hour=int(b["dong"]), minute=rng.randint(5, 25)) + timedelta(minutes=20),
                         self.kiem_ke, b, True)
        if self.kho["loai"] == "WAREHOUSE" and ngay.month == 10 and ngay.day == 14:
            self.hen(ngay.replace(hour=14, minute=rng.randint(0, 50)), self.kiem_ke, self.kho, True)
        # phieu kiem ke dang lam do hom nay
        if self.key == "ML" and ngay.date() == self.now.date():
            t = self.now - timedelta(minutes=rng.randint(70, 140))
            self.hen(t, self.kiem_ke, self.stores[0], False)

    def gio_ngau_nhien_cn(self, ngay, prof, b):
        return self.g.gio_ngau_nhien(ngay, prof, b["mo"], b["dong"])

    # ---------- nhap hang ----------
    def kiem_nhap(self, t, lan_dau):
        rng, g = self.rng, self.g
        kho = self.kho
        theo_ncc = {}
        for s in self.skus:
            if not self.con_ban(s, t + timedelta(days=5)):
                continue
            if kho["loai"] == "WAREHOUSE":
                ngay_can = s["can_online"] + sum(s["can_store"].values())
                muc_dat = max(s["rp"] + 4, math.ceil(ngay_can * rng.uniform(16, 24)))
                muc_thap = max(s["rp"] + 2, math.ceil(ngay_can * 7))
            else:
                ngay_can = s["can_store"][kho["key"]] + s["can_online"]
                muc_dat = max(s["rp"] + 6, math.ceil(ngay_can * rng.uniform(6, 9)))
                muc_thap = max(s["rp"] + 2, math.ceil(ngay_can * 2.5))
            hien = self.on_hand.get(self.kb(kho, s), 0)
            if lan_dau or hien <= muc_thap:
                sl = muc_dat - hien
                if sl <= 0:
                    continue
                if sl > 20:
                    sl = lam_tron(sl, 5 if sl < 100 else 10)
                if sl > 0:
                    theo_ncc.setdefault(s["ncc"], []).append((s, sl, hien <= s["rp"]))
        # it dong va chua co mon nao can han thi de lan sau dat gop
        theo_ncc = {k: [(s, sl) for s, sl, _ in v] for k, v in theo_ncc.items()
                    if lan_dau or len(v) >= 4 or any(gap for _, _, gap in v)}
        nguoi = self.owner if lan_dau else (self.owner if self.key == "PA" and rng.random() < .6 else self.nhan_vien(t))
        for ncc, dong in theo_ncc.items():
            rid = g.uid()
            tc = t + timedelta(minutes=rng.randint(1, 25), seconds=rng.randint(0, 59))
            code = self.ma("PN", tc)
            items = []
            for s, sl in dong:
                bien = s["von_goc"] * (1 + rng.gauss(0, 0.015) + (0.03 if t.month >= 11 and rng.random() < .5 else 0))
                gia = Decimal(lam_tron(bien, 500 if s["gia"] >= 20000 else 100))
                items.append((s, sl, gia))
                g.rows["purchase_receipt_item"].append((g.uid(), self.id, rid, s["id"], sl, gia, tc, tc))
            if self.key == "PA":
                tn = t + timedelta(hours=rng.uniform(1, 5))
            else:
                tn = t + timedelta(hours=rng.uniform(3, 30) if not lan_dau else rng.uniform(2, 8))
                if tn.hour < 8 or tn.hour >= 19:
                    tn = tn.replace(hour=rng.randint(8, 17)) + timedelta(days=1 if tn.hour >= 19 else 0)
            row = dict(id=rid, ncc=self.ncc[ncc], code=code, tc=tc, nguoi=nguoi, items=items)
            if tn <= self.now:
                self.hen(tn, self.xac_nhan_nhap, row)
            else:
                g.rows["purchase_receipt"].append((rid, self.id, kho["id"], row["ncc"], code, "DRAFT", None, None,
                                                   nguoi["id"], tc, tc))

    def xac_nhan_nhap(self, t, row):
        g = self.g
        nguoi = self.nhan_vien(t) if self.rng.random() < .7 else row["nguoi"]
        for s, sl, gia in sorted(row["items"], key=lambda x: str(x[0]["id"])):
            cu = self.tong_ton(s)
            if cu <= 0:
                s["avg"] = gia.quantize(D2)
            else:
                s["avg"] = ((Decimal(cu) * s["avg"] + Decimal(sl) * gia) / Decimal(cu + sl)).quantize(D2, ROUND_HALF_UP)
            self.ghi_so(t, self.kho, s, "IN", "PURCHASE", sl, gia, "PURCHASE_RECEIPT", row["id"], nguoi["id"])
        g.rows["purchase_receipt"].append((row["id"], self.id, self.kho["id"], row["ncc"], row["code"], "CONFIRMED",
                                           t, nguoi["id"], row["nguoi"]["id"], row["tc"], t))

    # ---------- chuyen kho ----------
    def kiem_chuyen(self, t, b):
        rng, g = self.rng, self.g
        dong = []
        for s in self.skus:
            if not self.con_ban(s, t):
                continue
            can = s["can_store"][b["key"]]
            muc_dat = max(s["rp"] + 2, math.ceil(can * rng.uniform(5, 8)))
            muc_thap = s["rp"] + math.ceil(can * 2)
            hien = self.on_hand.get(self.kb(b, s), 0)
            if hien <= muc_thap and muc_dat - hien > 0:
                sl = min(muc_dat - hien, self.co_san(self.kho, s))
                if sl > 0:
                    dong.append((s, sl))
        if not dong:
            return
        nguoi = self.nhan_vien(t)
        tid = g.uid()
        code = self.ma("CK", t)
        for s, sl in dong:
            g.rows["stock_transfer_item"].append((g.uid(), self.id, tid, s["id"], sl, t, t))
        row = dict(id=tid, b=b, code=code, tc=t, dong=dong, nguoi=nguoi)
        if rng.random() < 0.04:
            th = t + timedelta(minutes=rng.randint(10, 300))
            if th <= self.now:
                g.rows["stock_transfer"].append((tid, self.id, self.kho["id"], b["id"], code, "CANCELLED", t, th))
                return
        tx = t + timedelta(minutes=rng.randint(25, 200))
        if tx > self.now:
            g.rows["stock_transfer"].append((tid, self.id, self.kho["id"], b["id"], code, "DRAFT", t, t))
            return
        self.hen(tx, self.xuat_chuyen, row)

    def xuat_chuyen(self, t, row):
        for s, sl in row["dong"]:
            sl2 = min(sl, self.co_san(self.kho, s))
            if sl2 <= 0:
                continue
            self.ghi_so(t, self.kho, s, "OUT", "TRANSFER_OUT", sl2, s["avg"], "TRANSFER", row["id"], row["nguoi"]["id"])
            row.setdefault("thuc", []).append((s, sl2))
        tn = t + timedelta(hours=self.rng.uniform(3, 26))
        if tn.hour >= int(row["b"]["dong"]) or tn.hour < 8:
            tn = tn.replace(hour=self.rng.randint(8, 11)) + timedelta(days=1 if tn.hour >= 12 else 0)
        if tn > self.now:
            self.g.rows["stock_transfer"].append((row["id"], self.id, self.kho["id"], row["b"]["id"], row["code"],
                                                  "IN_TRANSIT", row["tc"], t))
            return
        self.hen(tn, self.nhan_chuyen, row)

    def nhan_chuyen(self, t, row):
        nguoi = self.thu_ngan(t, row["b"])
        for s, sl in row.get("thuc", []):
            self.ghi_so(t, row["b"], s, "IN", "TRANSFER_IN", sl, s["avg"], "TRANSFER", row["id"], nguoi["id"])
        self.g.rows["stock_transfer"].append((row["id"], self.id, self.kho["id"], row["b"]["id"], row["code"],
                                              "RECEIVED", row["tc"], t))

    # ---------- kiem ke ----------
    def kiem_ke(self, t, b, duyet):
        rng, g = self.rng, self.g
        kid = g.uid()
        code = self.ma("KK", t)
        dong = [s for s in self.skus if self.on_hand.get(self.kb(b, s), 0) > 0 or (self.kb(b, s) in self.bal_row)]
        if not duyet:
            dong = rng.sample(dong, k=min(len(dong), rng.randint(12, 20)))
        items = []
        for s in dong:
            he_thong = self.on_hand.get(self.kb(b, s), 0)
            r = rng.random()
            lech = 0
            if he_thong > 0 and r < 0.07:
                lech = -rng.choice([1, 1, 1, 2])
            elif r < 0.09:
                lech = 1
            items.append([s, he_thong, he_thong + lech])
        if not duyet:
            for s, ht, dem in items:
                da_dem = rng.random() < 0.6
                tu = t + timedelta(minutes=rng.randint(5, 60))
                g.rows["stocktake_item"].append((g.uid(), self.id, kid, s["id"], ht, dem if da_dem else None,
                                                 (dem - ht) if da_dem else None, t, tu if da_dem else t))
            g.rows["stocktake"].append((kid, self.id, b["id"], code, "OPEN", None, t, t))
            return
        td = t + timedelta(minutes=rng.randint(35, 95))
        if not self.hen(td, self.duyet_kiem_ke, kid, code, b, items, t):
            for s, ht, dem in items:
                g.rows["stocktake_item"].append((g.uid(), self.id, kid, s["id"], ht, dem, dem - ht, t, t))
            g.rows["stocktake"].append((kid, self.id, b["id"], code, "OPEN", None, t, t))

    def duyet_kiem_ke(self, t, kid, code, b, items, tc):
        g = self.g
        for it in items:
            s, ht, dem = it
            ht_now = self.on_hand.get(self.kb(b, s), 0)
            lech = dem - ht
            if lech < 0:
                lech = -min(-lech, ht_now - self.reserved.get(self.kb(b, s), 0))
            dem = ht + lech
            g.rows["stocktake_item"].append((g.uid(), self.id, kid, s["id"], ht, dem, lech, tc,
                                             tc + timedelta(minutes=self.rng.randint(5, 30))))
            if lech > 0:
                self.ghi_so(t, b, s, "IN", "ADJUST", lech, s["avg"], "STOCKTAKE", kid, self.owner["id"])
            elif lech < 0:
                self.ghi_so(t, b, s, "OUT", "ADJUST", -lech, s["avg"], "STOCKTAKE", kid, self.owner["id"])
        g.rows["stocktake"].append((kid, self.id, b["id"], code, "APPROVED", self.owner["id"], tc, t))

    # ---------- ban hang ----------
    def chon_hang(self, t, b, so_dong):
        rng = self.rng
        ds_sp = [p for p in self.products if any(self.con_ban(s, t) for s in p["skus"])]
        chon = {}
        for _ in range(so_dong):
            p = rng.choices(ds_sp, [x["pho_bien"] for x in ds_sp])[0]
            skus = [s for s in p["skus"] if self.con_ban(s, t)]
            s = rng.choices(skus, [x["w"] for x in skus])[0]
            if self.co_san(b, s) - chon.get(s["id"], (None, 0))[1] <= 0:
                # het size/mau: khach thu bien the gan nhat con hang, khong co thi thoi
                con = [x for x in skus if self.co_san(b, x) - chon.get(x["id"], (None, 0))[1] > 0]
                if not con or rng.random() < 0.45:
                    continue
                s = rng.choices(con, [x["w"] for x in con])[0]
            if s["le"]:
                sl = min(rng.choice([1, 1, 1, 2, 2, 3, 4, 5, 6, 10]), 12)
            elif s["gia"] < 120000:
                sl = rng.choice([1, 1, 1, 2, 2, 3])
            else:
                sl = 1 if rng.random() < 0.93 else 2
            sl = min(sl, self.co_san(b, s) - chon.get(s["id"], (None, 0))[1])
            if sl <= 0:
                continue
            chon[s["id"]] = (s, chon.get(s["id"], (None, 0))[1] + sl)
        return list(chon.values())

    def so_dong(self):
        if self.key == "PA":
            return self.rng.choices([1, 2, 3, 4, 5, 6, 8], [30, 24, 17, 11, 8, 6, 4])[0]
        return self.rng.choices([1, 2, 3], [74, 21, 5])[0]

    def don(self, oid, b, code, kenh, ext, trang_thai, ten, sdt, dc, tong, giu_den, nguoi, tc, tu):
        self.g.rows["orders"].append((oid, self.id, b["id"], code, kenh, ext, trang_thai, ten, sdt, dc,
                                      Decimal(tong), giu_den, nguoi, tc, tu))

    def ls(self, oid, tu, den, nguoi, ly_do, t):
        self.g.rows["order_status_history"].append((self.g.uid(), self.id, oid, tu, den, nguoi, ly_do, t, t))

    def ban_pos(self, t, b):
        rng, g = self.rng, self.g
        hang = self.chon_hang(t, b, self.so_dong())
        if not hang:
            return
        nv = self.thu_ngan(t, b)
        oid = g.uid()
        code = self.ma("DH", t)
        tong = 0
        t1 = t + timedelta(seconds=rng.randint(20, 240))
        for s, sl in sorted(hang, key=lambda x: str(x[0]["id"])):
            tong += s["gia"] * sl
            g.rows["order_item"].append((g.uid(), self.id, oid, s["id"], sl, Decimal(s["gia"]), s["avg"], t1, t1))
            self.ghi_so(t1, b, s, "OUT", "SALE", sl, s["avg"], "ORDER", oid, nv["id"])
        ten = sdt = None
        if rng.random() < (0.08 if self.key == "PA" else 0.27):
            ten, sdt = g.ho_ten(), g.sdt() if rng.random() < .9 else None
        qr = rng.random() < TY_LE_QR[self.key] * (1.3 if tong > 1000000 else 1)
        if qr:
            nhan = thoi = None
        else:
            cac_muc = sorted({math.ceil(tong / m) * m for m in (1000, 10000, 50000, 100000, 200000, 500000)})
            nhan = rng.choices(cac_muc, [5, 3, 4, 3, 1.5, 1][:len(cac_muc)])[0]
            if nhan < tong:
                nhan = tong
            nhan, thoi = Decimal(nhan), Decimal(nhan - tong)
        self.don(oid, b, code, "POS", None, "COMPLETED", ten, sdt, None, tong, None, nv["id"], t, t1)
        g.rows["payment"].append((g.uid(), self.id, oid, "QR" if qr else "CASH", Decimal(tong), nhan, thoi, t1, t1))
        self.ls(oid, None, "DRAFT", nv["id"], "Tạo đơn POS", t1)
        self.ls(oid, "DRAFT", "RESERVED", nv["id"], "Giữ hàng", t1)
        self.ls(oid, "RESERVED", "CONFIRMED", nv["id"], "Xác nhận trừ kho", t1)
        self.ls(oid, "CONFIRMED", "COMPLETED", nv["id"], "Thanh toán POS thành công", t1)

    def ma_san(self, kenh, t):
        rng = self.rng
        if kenh == "SHOPEE":
            return t.strftime("%y%m%d") + "".join(rng.choice(string.ascii_uppercase + string.digits) for _ in range(8))
        return "57" + "".join(str(rng.randint(0, 9)) for _ in range(16))

    def don_online(self, t, kenh):
        rng, g = self.rng, self.g
        b = self.kho
        so = self.rng.choices([1, 2, 3], [70, 23, 7])[0] if self.key != "PA" else self.so_dong() + 2
        hang = self.chon_hang(t, b, so)
        if not hang:
            return
        oid = g.uid()
        code = self.ma("DH", t)
        ten = g.ho_ten()
        sdt = g.sdt()
        dc = g.dia_chi(self.ch["mien"]) if (kenh != "MANUAL" or rng.random() < .85) else None
        ext = self.ma_san(kenh, t) if kenh != "MANUAL" else None
        nguoi_tao = None if kenh != "MANUAL" else self.nhan_vien(t)["id"]
        giam = 1.0
        if kenh != "MANUAL" and (t.month, t.day) in ((10, 10), (11, 11)):
            giam = rng.choice([0.85, 0.88, 0.9])
        gia = {s["id"]: lam_tron(s["gia"] * giam, 1000) if giam < 1 else s["gia"] for s, _ in hang}
        tong = sum(gia[s["id"]] * sl for s, sl in hang)

        # don nhap thu cong (khach hoi gia qua dien thoai)
        if kenh == "MANUAL" and rng.random() < 0.08:
            for s, sl in hang:
                g.rows["order_item"].append((g.uid(), self.id, oid, s["id"], sl, Decimal(gia[s["id"]]), None, t, t))
            self.ls(oid, None, "DRAFT", nguoi_tao, "Tạo đơn nháp thủ công", t)
            th = t + timedelta(hours=rng.uniform(20, 60))
            if th <= self.now and rng.random() < 0.7:
                nv = self.nhan_vien(th)["id"]
                self.ls(oid, "DRAFT", "CANCELLED", nv, rng.choice(["Khách không chốt đơn", "Khách hẹn mua sau",
                                                                  "Báo giá xong khách không phản hồi"]), th)
                self.don(oid, b, code, kenh, ext, "CANCELLED", ten, sdt, dc, tong, None, nguoi_tao, t, th)
            else:
                self.don(oid, b, code, kenh, ext, "DRAFT", ten, sdt, dc, tong, None, nguoi_tao, t, t)
            return

        phut = self.ch["giu_hang_phut"]
        giu_den = t + timedelta(minutes=phut)
        for s, sl in hang:
            self.reserved[self.kb(b, s)] += sl
            self.cham_ton(t, b, s)
        items = []
        for s, sl in hang:
            items.append([g.uid(), s, sl, Decimal(gia[s["id"]])])
        self.ls(oid, None, "DRAFT", nguoi_tao, "Tạo đơn", t)
        self.ls(oid, "DRAFT", "RESERVED", nguoi_tao, "Giữ hàng thành công", t)
        o = dict(id=oid, b=b, code=code, kenh=kenh, ext=ext, ten=ten, sdt=sdt, dc=dc, tong=tong, giu_den=giu_den,
                 nguoi=nguoi_tao, tc=t, items=items)
        r = rng.random()
        if r < 0.055:
            da_hen = self.hen(giu_den + timedelta(seconds=rng.randint(20, 120)), self.huy_giu, o, None,
                              "Quá hạn giữ hàng (tự động nhả tồn)")
        elif r < 0.10:
            da_hen = self.hen(t + timedelta(minutes=rng.uniform(2, phut - 2)), self.huy_giu, o, "nv",
                              rng.choice(LY_DO_HUY_KHACH))
        else:
            da_hen = self.hen(t + timedelta(minutes=rng.uniform(2, phut - 3)), self.xac_nhan_don, o)
        if not da_hen:
            # don vua dat, dang trong thoi gian giu hang
            self.ghi_item(o, False)
            self.don(oid, b, code, kenh, ext, "RESERVED", ten, sdt, dc, tong, giu_den, nguoi_tao, t, t)

    def ghi_item(self, o, co_von):
        for iid, s, sl, gia in o["items"]:
            self.g.rows["order_item"].append((iid, self.id, o["id"], s["id"], sl, gia, s.get("_von_" + str(o["id"])),
                                              o["tc"], o.get("tx", o["tc"])))

    def huy_giu(self, t, o, nv, ly_do):
        b = o["b"]
        for _, s, sl, _ in o["items"]:
            self.reserved[self.kb(b, s)] -= sl
            self.cham_ton(t, b, s)
        nid = self.nhan_vien(t)["id"] if nv else None
        self.ls(o["id"], "RESERVED", "CANCELLED", nid, ly_do, t)
        self.ghi_item(o, False)
        self.don(o["id"], b, o["code"], o["kenh"], o["ext"], "CANCELLED", o["ten"], o["sdt"], o["dc"], o["tong"],
                 o["giu_den"], o["nguoi"], o["tc"], t)

    def xac_nhan_don(self, t, o):
        b, g, rng = o["b"], self.g, self.rng
        nv = self.nhan_vien(t)["id"] if self.key != "PA" else (self.owner["id"] if rng.random() < .6 else self.nhan_vien(t)["id"])
        for _, s, sl, _ in sorted(o["items"], key=lambda x: str(x[1]["id"])):
            self.reserved[self.kb(b, s)] -= sl
            s["_von_" + str(o["id"])] = s["avg"]
            self.ghi_so(t, b, s, "OUT", "SALE", sl, s["avg"], "ORDER", o["id"], nv)
        o["tx"] = t
        self.ls(o["id"], "RESERVED", "CONFIRMED", nv, "Duyệt đơn và trừ kho", t)
        if self.key == "PA":
            tg = t + timedelta(minutes=rng.randint(25, 150))
        else:
            tg = t + timedelta(hours=rng.uniform(30, 110))
            if tg.hour < 8 or tg.hour > 20:
                tg = tg.replace(hour=rng.randint(9, 18))
        if tg > self.now:
            self.ghi_item(o, True)
            self.don(o["id"], b, o["code"], o["kenh"], o["ext"], "CONFIRMED", o["ten"], o["sdt"], o["dc"], o["tong"],
                     o["giu_den"], o["nguoi"], o["tc"], t)
            return
        if rng.random() < (0.045 if self.key != "PA" else 0.01):
            self.hen(tg, self.hoan_don, o)
        else:
            self.hen(tg, self.hoan_tat, o)

    def hoan_tat(self, t, o):
        nv = self.nhan_vien(t)["id"]
        self.ls(o["id"], "CONFIRMED", "COMPLETED", nv, "Giao hàng và hoàn tất đơn", t)
        self.ghi_item(o, True)
        self.don(o["id"], o["b"], o["code"], o["kenh"], o["ext"], "COMPLETED", o["ten"], o["sdt"], o["dc"], o["tong"],
                 o["giu_den"], o["nguoi"], o["tc"], t)

    def hoan_don(self, t, o):
        nv = self.nhan_vien(t)["id"]
        for _, s, sl, _ in sorted(o["items"], key=lambda x: str(x[1]["id"])):
            von = s.get("_von_" + str(o["id"])) or Decimal(0)
            self.ghi_so(t, o["b"], s, "IN", "RETURN", sl, von, "ORDER", o["id"], nv)
        self.ls(o["id"], "CONFIRMED", "CANCELLED", nv, self.rng.choice(LY_DO_HUY_SAU_GIAO), t)
        self.ghi_item(o, True)
        self.don(o["id"], o["b"], o["code"], o["kenh"], o["ext"], "CANCELLED", o["ten"], o["sdt"], o["dc"], o["tong"],
                 o["giu_den"], o["nguoi"], o["tc"], t)

    # ---------- ket thuc ----------
    def ket_thuc(self):
        g = self.g
        for k, (bid, t1, t2) in self.bal_row.items():
            g.rows["stock_balance"].append((bid, self.id, k[0], k[1], self.on_hand[k], self.reserved[k], t1, t2))
        for k, (aid, th, av, tc) in self.alert_open.items():
            g.rows["stock_alert"].append((aid, self.id, k[0], k[1], "OPEN", th, av, tc, tc, None))
        for s in self.skus:
            trang = "ACTIVE" if self.con_ban(s, self.now) else "INACTIVE"
            g.rows["sku"].append((s["id"], self.id, s["prod"]["id"], s["code"], s["attrs"], s["barcode"],
                                  Decimal(s["gia"]), Decimal(s["si"]) if s["si"] else None, s["avg"], s["rp"],
                                  trang, s["tao"], s["ngung"] if trang == "INACTIVE" else s["tao"]))


COLS = {
    "tenant": "id, name, status, reservation_minutes, created_at, updated_at",
    "branch": "tenant_id, id, name, type, address, active, created_at, updated_at",
    "app_user": "id, tenant_id, email, phone, password_hash, full_name, role, branch_id, active, created_at, updated_at",
    "category": "id, tenant_id, parent_id, name, created_at, updated_at",
    "brand": "id, tenant_id, name, created_at, updated_at",
    "supplier": "id, tenant_id, name, phone, address, active, created_at, updated_at",
    "product": "id, tenant_id, category_id, brand_id, name, description, has_variants, status, created_at, updated_at",
    "sku": "id, tenant_id, product_id, code, attributes, barcode, retail_price, wholesale_price, avg_cost, reorder_point, status, created_at, updated_at",
    "purchase_receipt": "id, tenant_id, branch_id, supplier_id, code, status, confirmed_at, confirmed_by, created_by, created_at, updated_at",
    "purchase_receipt_item": "id, tenant_id, receipt_id, sku_id, quantity, unit_price, created_at, updated_at",
    "stock_transfer": "id, tenant_id, from_branch_id, to_branch_id, code, status, created_at, updated_at",
    "stock_transfer_item": "id, tenant_id, transfer_id, sku_id, quantity, created_at, updated_at",
    "stocktake": "id, tenant_id, branch_id, code, status, approved_by, created_at, updated_at",
    "stocktake_item": "id, tenant_id, stocktake_id, sku_id, system_qty, counted_qty, difference, created_at, updated_at",
    "orders": "id, tenant_id, branch_id, code, channel, external_order_id, status, customer_name, customer_phone, shipping_address, total_amount, reserved_until, created_by, created_at, updated_at",
    "order_item": "id, tenant_id, order_id, sku_id, quantity, unit_price, cost_price, created_at, updated_at",
    "order_status_history": "id, tenant_id, order_id, from_status, to_status, changed_by, reason, created_at, updated_at",
    "payment": "id, tenant_id, order_id, method, amount, cash_received, change_amount, created_at, updated_at",
    "inventory_transaction": "id, tenant_id, branch_id, sku_id, direction, reason, quantity, balance_after, unit_cost, ref_type, ref_id, created_by, created_at",
    "stock_balance": "id, tenant_id, branch_id, sku_id, on_hand, reserved, created_at, updated_at",
    "stock_alert": "id, tenant_id, branch_id, sku_id, status, threshold, available, created_at, updated_at, closed_at",
    "channel_credential": "id, tenant_id, channel, api_key_hash, active, created_at, updated_at",
}
THU_TU = ["tenant", "branch", "app_user", "category", "brand", "supplier", "product", "sku", "channel_credential",
          "purchase_receipt", "purchase_receipt_item", "stock_transfer", "stock_transfer_item", "stocktake",
          "stocktake_item", "orders", "order_item", "order_status_history", "payment", "inventory_transaction",
          "stock_balance", "stock_alert"]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--seed", type=int, default=None)
    ap.add_argument("--mat-khau", default="admin123",
                    help="mat khau chung cho moi tai khoan (dev/demo); de rong '' de sinh ngau nhien tung nguoi")
    ap.add_argument("--out", default=str(Path(__file__).with_name("du-lieu-mau.sql")))
    ap.add_argument("--tai-khoan", default=str(Path(__file__).with_name("tai-khoan.local.md")))
    a = ap.parse_args()
    seed = a.seed if a.seed is not None else secrets.randbelow(10 ** 9)
    now = datetime.now(VN) - timedelta(minutes=2)
    g = BoSinh(seed, now, a.mat_khau)
    for ch in CUA_HANG:
        g.mo_phong(ch)

    # category: cha truoc con
    g.rows["category"].sort(key=lambda r: r[2] is not None)
    out = ["-- Du lieu mau OISM sinh boi scripts/du-lieu-mau/sinh_du_lieu.py",
           f"-- seed={seed}, thoi diem={now.isoformat(timespec='seconds')}",
           "BEGIN;",
           "TRUNCATE TABLE " + ", ".join(THU_TU + ["daily_report", "refresh_token"]) + " RESTART IDENTITY CASCADE;"]
    for t in THU_TU:
        rows = g.rows[t]
        for i in range(0, len(rows), 400):
            chunk = rows[i:i + 400]
            out.append(f"INSERT INTO {t} ({COLS[t]}) VALUES\n" +
                       ",\n".join("(" + ", ".join(sql_val(v) for v in r) + ")" for r in chunk) + ";")
    out.append("""
-- Chot so ngay (FR-SIM-03) cho cac ngay da qua: don Confirmed/Completed theo ngay UTC, chi nhanh, kenh
INSERT INTO daily_report (id, tenant_id, branch_id, report_date, channel, order_count, revenue, cogs, gross_profit, created_at, updated_at)
SELECT gen_random_uuid(), x.tenant_id, x.branch_id, x.d, x.channel, x.n, x.rev, x.cogs, x.rev - x.cogs, x.t, x.t
FROM (
    SELECT o.tenant_id, o.branch_id, (o.created_at AT TIME ZONE 'UTC')::date AS d, o.channel,
           count(DISTINCT o.id) AS n,
           sum(oi.quantity * oi.unit_price) AS rev,
           sum(oi.quantity * coalesce(oi.cost_price, 0)) AS cogs,
           (((o.created_at AT TIME ZONE 'UTC')::date + 1) + time '00:05' + (random() * interval '40 seconds')) AT TIME ZONE 'UTC' AS t
    FROM orders o JOIN order_item oi ON oi.order_id = o.id
    WHERE o.status IN ('CONFIRMED', 'COMPLETED')
      AND (o.created_at AT TIME ZONE 'UTC')::date < (now() AT TIME ZONE 'UTC')::date
    GROUP BY o.tenant_id, o.branch_id, (o.created_at AT TIME ZONE 'UTC')::date, o.channel
) x;
COMMIT;
ANALYZE;""")
    Path(a.out).write_text("\n".join(out) + "\n", encoding="utf-8")

    tk = ["# Tài khoản dữ liệu mẫu (chỉ lưu trên máy, KHÔNG đưa lên Git)", "",
          f"Sinh lúc {now:%d/%m/%Y %H:%M} (seed {seed}).", "",
          "| Cửa hàng | Vai trò | Chi nhánh | Họ tên | Email | SĐT | Mật khẩu | Trạng thái |", "|---|---|---|---|---|---|---|---|"]
    for ten, role, cn, ho_ten, email, sdt, mk, act in g.tai_khoan:
        tk.append(f"| {ten} | {role} | {cn or ''} | {ho_ten} | `{email}` | {sdt or ''} | `{mk}` | {'Đang làm' if act else 'Đã nghỉ (khóa)'} |")
    Path(a.tai_khoan).write_text("\n".join(tk) + "\n", encoding="utf-8")
    print(f"seed={seed}")
    for t in THU_TU:
        print(f"  {t:24s} {len(g.rows[t])}")


if __name__ == "__main__":
    main()
