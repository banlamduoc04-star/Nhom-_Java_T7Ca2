import Link from "next/link";

export default function Home() {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center p-6 text-gray-800">
      <div className="max-w-xl w-full bg-white rounded-2xl shadow-md p-8 text-center border border-gray-100">
        <div className="w-16 h-16 bg-blue-100 text-blue-600 rounded-2xl flex items-center justify-center text-3xl mx-auto mb-4">
          🏪
        </div>
        <h1 className="text-3xl font-extrabold text-gray-900 mb-2">Hệ thống OISM Admin</h1>
        <p className="text-gray-500 mb-8">
          Quản lý bán hàng và tồn kho đa kênh (Omnichannel Inventory and Sales Management)
        </p>

        <div className="grid grid-cols-1 gap-4">
          <Link
            href="/products"
            className="flex items-center justify-between p-4 bg-blue-50 hover:bg-blue-100 border border-blue-200 rounded-xl transition text-left group"
          >
            <div>
              <h3 className="font-bold text-blue-900 group-hover:text-blue-700">📦 Quản lý Sản phẩm & Biến thể SKU</h3>
              <p className="text-xs text-blue-600">Danh mục, thương hiệu, sản phẩm và biến thể SKU</p>
            </div>
            <span className="text-blue-600 text-xl font-bold">→</span>
          </Link>
        </div>

        <div className="mt-8 pt-6 border-t text-xs text-gray-400">
          OISM - Hệ thống quản lý bán hàng & tồn kho đa kênh
        </div>
      </div>
    </div>
  );
}
