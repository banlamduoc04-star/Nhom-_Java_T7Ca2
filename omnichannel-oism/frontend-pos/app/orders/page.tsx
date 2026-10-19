
"use client";

import { useState } from "react";

type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "SHIPPED"
  | "DELIVERED"
  | "CANCELLED";

type Order = {
  id: string;
  customer: string;
  date: string;
  total: number;
  channel: "POS" | "SHOPEE" | "TIKTOK";
  status: OrderStatus;
};

const sampleOrders: Order[] = [
  {
    id: "ORD-1001",
    customer: "Nguyễn Văn An",
    date: "2026-10-16",
    total: 318000,
    channel: "POS",
    status: "DELIVERED",
  },
  {
    id: "ORD-1002",
    customer: "Trần Thị Bình",
    date: "2026-10-16",
    total: 599000,
    channel: "SHOPEE",
    status: "PENDING",
  },
  {
    id: "ORD-1003",
    customer: "Lê Minh Khang",
    date: "2026-10-15",
    total: 249000,
    channel: "TIKTOK",
    status: "CONFIRMED",
  },
  {
    id: "ORD-1004",
    customer: "Phạm Thu Hà",
    date: "2026-10-15",
    total: 658000,
    channel: "POS",
    status: "SHIPPED",
  },
  {
    id: "ORD-1005",
    customer: "Khách lẻ",
    date: "2026-10-14",
    total: 159000,
    channel: "POS",
    status: "CANCELLED",
  },
];

const statusLabels: Record<OrderStatus, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  SHIPPED: "Đang giao",
  DELIVERED: "Đã giao",
  CANCELLED: "Đã hủy",
};

const statusColors: Record<OrderStatus, string> = {
  PENDING: "bg-amber-100 text-amber-800",
  CONFIRMED: "bg-blue-100 text-blue-800",
  SHIPPED: "bg-purple-100 text-purple-800",
  DELIVERED: "bg-green-100 text-green-800",
  CANCELLED: "bg-red-100 text-red-800",
};

const money = (value: number) =>
  value.toLocaleString("vi-VN") + " ₫";

export default function OrderListPage() {
  const [keyword, setKeyword] = useState("");
  const [channel, setChannel] = useState("ALL");
  const [status, setStatus] = useState("ALL");

  const filteredOrders = sampleOrders.filter((order) => {
    const matchesKeyword =
      order.id.toLowerCase().includes(keyword.toLowerCase()) ||
      order.customer.toLowerCase().includes(keyword.toLowerCase());

    const matchesChannel =
      channel === "ALL" || order.channel === channel;

    const matchesStatus =
      status === "ALL" || order.status === status;

    return matchesKeyword && matchesChannel && matchesStatus;
  });

  return (
    <main className="min-h-screen bg-slate-100 p-5 text-slate-800 md:p-8">
      <div className="mx-auto max-w-7xl">
        <header className="mb-8 flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold text-blue-700">
              OISM - Quản lý đơn hàng
            </h1>
            <p className="mt-2 text-slate-500">
              Theo dõi đơn hàng và trạng thái xử lý
            </p>
          </div>

          <a
            href="/"
            className="rounded-lg bg-blue-600 px-5 py-3 font-semibold text-white hover:bg-blue-700"
          >
            ← Quay lại POS
          </a>
        </header>

        <section className="mb-6 grid gap-4 sm:grid-cols-3">
          <div className="rounded-xl bg-white p-5 shadow-sm">
            <p className="text-sm text-slate-500">Tổng số đơn mẫu</p>
            <p className="mt-2 text-2xl font-bold">
              {sampleOrders.length}
            </p>
          </div>

          <div className="rounded-xl bg-white p-5 shadow-sm">
            <p className="text-sm text-slate-500">Đã giao</p>
            <p className="mt-2 text-2xl font-bold text-green-600">
              {sampleOrders.filter(
                (order) => order.status === "DELIVERED"
              ).length}
            </p>
          </div>

          <div className="rounded-xl bg-white p-5 shadow-sm">
            <p className="text-sm text-slate-500">Đã hủy</p>
            <p className="mt-2 text-2xl font-bold text-red-600">
              {sampleOrders.filter(
                (order) => order.status === "CANCELLED"
              ).length}
            </p>
          </div>
        </section>

        <section className="mb-6 rounded-xl bg-white p-5 shadow-sm">
          <h2 className="mb-4 text-lg font-bold">
            Tìm kiếm và lọc đơn hàng
          </h2>

          <div className="grid gap-4 md:grid-cols-3">
            <input
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="Nhập mã đơn hoặc tên khách hàng..."
              className="rounded-lg border border-slate-300 p-3 outline-none focus:border-blue-500"
            />

            <select
              value={channel}
              onChange={(e) => setChannel(e.target.value)}
              className="rounded-lg border border-slate-300 p-3"
            >
              <option value="ALL">Tất cả kênh bán</option>
              <option value="POS">POS - Tại quầy</option>
              <option value="SHOPEE">Shopee</option>
              <option value="TIKTOK">TikTok</option>
            </select>

            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              className="rounded-lg border border-slate-300 p-3"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="PENDING">Chờ xác nhận</option>
              <option value="CONFIRMED">Đã xác nhận</option>
              <option value="SHIPPED">Đang giao</option>
              <option value="DELIVERED">Đã giao</option>
              <option value="CANCELLED">Đã hủy</option>
            </select>
          </div>

          <button
            onClick={() => {
              setKeyword("");
              setChannel("ALL");
              setStatus("ALL");
            }}
            className="mt-4 rounded-lg border border-slate-300 px-4 py-2 hover:bg-slate-100"
          >
            Xóa bộ lọc
          </button>
        </section>

        <section className="overflow-hidden rounded-xl bg-white shadow-sm">
          <div className="border-b p-5">
            <h2 className="text-lg font-bold">Danh sách đơn hàng</h2>
            <p className="mt-1 text-sm text-slate-500">
              Tìm thấy {filteredOrders.length} đơn hàng
            </p>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-slate-600">
                <tr>
                  <th className="whitespace-nowrap p-4">Mã đơn</th>
                  <th className="whitespace-nowrap p-4">Khách hàng</th>
                  <th className="whitespace-nowrap p-4">Ngày tạo</th>
                  <th className="whitespace-nowrap p-4">Tổng tiền</th>
                  <th className="whitespace-nowrap p-4">Kênh bán</th>
                  <th className="whitespace-nowrap p-4">Trạng thái</th>
                </tr>
              </thead>

              <tbody>
                {filteredOrders.map((order) => (
                  <tr
                    key={order.id}
                    className="border-t hover:bg-slate-50"
                  >
                    <td className="whitespace-nowrap p-4 font-semibold text-blue-700">
                      {order.id}
                    </td>
                    <td className="whitespace-nowrap p-4">
                      {order.customer}
                    </td>
                    <td className="whitespace-nowrap p-4">
                      {order.date}
                    </td>
                    <td className="whitespace-nowrap p-4 font-semibold">
                      {money(order.total)}
                    </td>
                    <td className="whitespace-nowrap p-4">
                      {order.channel}
                    </td>
                    <td className="whitespace-nowrap p-4">
                      <span
                        className={`rounded-full px-3 py-1 text-xs font-semibold ${
                          statusColors[order.status]
                        }`}
                      >
                        {statusLabels[order.status]}
                      </span>
                    </td>
                  </tr>
                ))}

                {filteredOrders.length === 0 && (
                  <tr>
                    <td
                      colSpan={6}
                      className="p-8 text-center text-slate-500"
                    >
                      Không tìm thấy đơn hàng phù hợp.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        <p className="mt-5 text-sm text-slate-500">
          Dữ liệu hiện tại là dữ liệu mẫu, chưa kết nối API backend.
        </p>
      </div>
    </main>
  );
}
