"use client";

import { useEffect, useState } from "react";

type OrderEvent = {
  orderId: string;
  eventType: string;
  occurredAt: string;
};

export default function OrderRealtimeNotifications() {
  const [connected, setConnected] = useState(false);
  const [notification, setNotification] = useState<OrderEvent | null>(null);

  useEffect(() => {
    const eventSource = new EventSource(
      "http://127.0.0.1:8080/api/v1/events/orders"
    );

    eventSource.addEventListener("connected", () => {
      setConnected(true);
    });

    eventSource.addEventListener("order-created", (event) => {
      try {
        const orderEvent = JSON.parse(
          (event as MessageEvent).data
        ) as OrderEvent;

        setNotification(orderEvent);
      } catch {
        console.error("Không thể đọc dữ liệu sự kiện đơn hàng");
      }
    });

    eventSource.onerror = () => {
      setConnected(false);
    };

    return () => {
      eventSource.close();
    };
  }, []);

  return (
    <>
      <div className="fixed right-4 top-4 z-50 rounded-full bg-white px-4 py-2 text-sm shadow">
        <span
          className={`mr-2 inline-block h-2 w-2 rounded-full ${
            connected ? "bg-green-500" : "bg-red-500"
          }`}
        />
        {connected ? "Đang kết nối realtime" : "Đang kết nối lại..."}
      </div>

      {notification && (
        <div
          role="alert"
          className="fixed right-4 top-16 z-50 w-80 rounded-xl border border-green-200 bg-white p-4 shadow-lg"
        >
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="font-bold text-green-700">
                Có đơn hàng mới!
              </p>

              <p className="mt-2 text-sm text-slate-700">
                Mã đơn hàng: {notification.orderId}
              </p>

              <p className="mt-1 text-xs text-slate-500">
                {new Date(notification.occurredAt).toLocaleString("vi-VN")}
              </p>
            </div>

            <button
              onClick={() => setNotification(null)}
              className="text-slate-500 hover:text-red-600"
              aria-label="Đóng thông báo"
            >
              ✕
            </button>
          </div>
        </div>
      )}
    </>
  );
}
