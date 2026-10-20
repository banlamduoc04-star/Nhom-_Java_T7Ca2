"use client";

import { useEffect, useState } from "react";
import { Brand, Category, Product, ProductDetail } from "@/types/catalog";
import { brandService, categoryService, productService, skuService } from "@/services/catalogService";

export default function ProductsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<number | undefined>(undefined);
  const [keyword, setKeyword] = useState<string>("");
  const [loading, setLoading] = useState<boolean>(true);

  const [selectedProduct, setSelectedProduct] = useState<ProductDetail | null>(null);

  const [showAddModal, setShowAddModal] = useState<boolean>(false);
  const [newProductName, setNewProductName] = useState("");
  const [newProductDesc, setNewProductDesc] = useState("");
  const [newProductCatId, setNewProductCatId] = useState<number>(0);
  const [newProductBrandId, setNewProductBrandId] = useState<number | undefined>(undefined);

  const [showAddSkuModal, setShowAddSkuModal] = useState<boolean>(false);
  const [skuCode, setSkuCode] = useState("");
  const [skuAttr, setSkuAttr] = useState("");
  const [skuBarcode, setSkuBarcode] = useState("");
  const [skuCost, setSkuCost] = useState<number>(0);
  const [skuRetailPrice, setSkuRetailPrice] = useState<number>(0);
  const [skuWholesalePrice, setSkuWholesalePrice] = useState<number>(0);

  useEffect(() => {
    loadCategories();
    loadBrands();
    loadProducts();
  }, []);

  const loadCategories = async () => {
    try {
      const data = await categoryService.getAllCategories();
      setCategories(data);
      if (data.length > 0) setNewProductCatId(data[0].id);
    } catch {
      setCategories([
        { id: 1, name: "Áo Polo", description: "Áo có cổ" },
        { id: 2, name: "Quần Jeans", description: "Quần jeans thời trang" },
      ]);
    }
  };

  const loadBrands = async () => {
    try {
      const data = await brandService.getAllBrands();
      setBrands(data);
    } catch {
      setBrands([
        { id: 1, name: "Nike", description: "Thời trang thể thao" },
        { id: 2, name: "Adidas", description: "Thời trang thể thao" },
      ]);
    }
  };

  const loadProducts = async (searchKw = keyword, catId = selectedCategory) => {
    setLoading(true);
    try {
      const res = await productService.searchProducts(searchKw, catId);
      setProducts(res.content || []);
    } catch {
      setProducts([
        { id: 101, name: "Áo Polo Cotton Nam", categoryName: "Áo Polo", categoryId: 1, brandName: "Nike", brandId: 1, isActive: true },
        { id: 102, name: "Quần Jeans Slimfit", categoryName: "Quần Jeans", categoryId: 2, brandName: "Adidas", brandId: 2, isActive: true },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    loadProducts(keyword, selectedCategory);
  };

  const handleViewDetail = async (productId: number) => {
    try {
      const detail = await productService.getProductById(productId);
      const skus = await skuService.getSkusByProduct(productId);
      setSelectedProduct({ ...detail, skus });
    } catch {
      setSelectedProduct({
        id: productId,
        name: "Sản phẩm mẫu",
        categoryId: 1,
        categoryName: "Áo Polo",
        brandName: "Nike",
        isActive: true,
        skus: [
          { id: 1, skuCode: "POLO-TRANG-L", costPrice: 120000, sellingPrice: 220000, retailPrice: 220000, wholesalePrice: 180000, barcode: "8930001", attributeValues: "Size L, Màu Trắng", productId },
          { id: 2, skuCode: "POLO-DEN-M", costPrice: 120000, sellingPrice: 220000, retailPrice: 220000, wholesalePrice: 180000, barcode: "8930002", attributeValues: "Size M, Màu Đen", productId },
        ],
      });
    }
  };

  const handleCreateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newProductName.trim()) return alert("Vui lòng nhập tên sản phẩm");
    try {
      await productService.createProduct({
        name: newProductName,
        description: newProductDesc,
        categoryId: newProductCatId,
        brandId: newProductBrandId,
      });
      alert("Tạo sản phẩm thành công");
      setShowAddModal(false);
      setNewProductName("");
      setNewProductDesc("");
      loadProducts();
    } catch (err: unknown) {
      alert("Lỗi: " + (err instanceof Error ? err.message : String(err)));
    }
  };

  const handleCreateSku = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProduct) return;
    try {
      await skuService.addSku(selectedProduct.id, {
        skuCode,
        attributeValues: skuAttr,
        barcode: skuBarcode,
        costPrice: Number(skuCost),
        sellingPrice: Number(skuRetailPrice),
        retailPrice: Number(skuRetailPrice),
        wholesalePrice: Number(skuWholesalePrice),
      });
      alert("Thêm biến thể SKU thành công");
      setShowAddSkuModal(false);
      setSkuCode("");
      setSkuAttr("");
      setSkuBarcode("");
      setSkuCost(0);
      setSkuRetailPrice(0);
      setSkuWholesalePrice(0);
      handleViewDetail(selectedProduct.id);
    } catch (err: unknown) {
      alert("Lỗi: " + (err instanceof Error ? err.message : String(err)));
    }
  };

  const handleDeleteProduct = async (id: number) => {
    if (!confirm("Bạn có chắc chắn muốn xóa sản phẩm này?")) return;
    try {
      await productService.deleteProduct(id);
      loadProducts();
      if (selectedProduct?.id === id) setSelectedProduct(null);
    } catch (err: unknown) {
      alert("Lỗi xóa sản phẩm: " + (err instanceof Error ? err.message : String(err)));
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 p-6 md:p-8">
      <div className="max-w-7xl mx-auto space-y-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Quản lý Sản phẩm & Biến thể SKU</h1>
            <p className="text-sm text-slate-500 mt-1">
              Quản lý danh mục hàng hóa, thương hiệu, sản phẩm và biến thể đa kênh
            </p>
          </div>
          <button
            onClick={() => setShowAddModal(true)}
            className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2.5 rounded-xl font-medium shadow-sm transition"
          >
            <span>+</span> Thêm sản phẩm mới
          </button>
        </div>

        <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-200 flex flex-col md:flex-row gap-4 items-center justify-between">
          <form onSubmit={handleSearch} className="flex gap-2 w-full md:w-96">
            <input
              type="text"
              placeholder="Tìm theo tên sản phẩm..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              className="w-full px-4 py-2 border rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <button
              type="submit"
              className="bg-slate-800 hover:bg-slate-900 text-white px-4 py-2 rounded-xl text-sm font-medium transition"
            >
              Tìm
            </button>
          </form>

          <div className="flex gap-2 w-full md:w-auto">
            <select
              value={selectedCategory || ""}
              onChange={(e) => {
                const val = e.target.value ? Number(e.target.value) : undefined;
                setSelectedCategory(val);
                loadProducts(keyword, val);
              }}
              className="px-3 py-2 border rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Tất cả danh mục</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
            <div className="p-4 border-b border-slate-100 flex justify-between items-center">
              <h2 className="font-semibold text-slate-800">Danh sách sản phẩm</h2>
              <span className="text-xs text-slate-500">{products.length} sản phẩm</span>
            </div>
            {loading ? (
              <div className="p-8 text-center text-slate-400">Đang tải dữ liệu...</div>
            ) : products.length === 0 ? (
              <div className="p-8 text-center text-slate-400">Không tìm thấy sản phẩm nào</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="bg-slate-50 text-slate-600 font-medium border-b border-slate-200">
                    <tr>
                      <th className="p-3">ID</th>
                      <th className="p-3">Tên sản phẩm</th>
                      <th className="p-3">Danh mục</th>
                      <th className="p-3">Thương hiệu</th>
                      <th className="p-3 text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {products.map((p) => (
                      <tr
                        key={p.id}
                        className={`hover:bg-slate-50/80 transition cursor-pointer ${
                          selectedProduct?.id === p.id ? "bg-blue-50/60" : ""
                        }`}
                        onClick={() => handleViewDetail(p.id)}
                      >
                        <td className="p-3 text-slate-400">#{p.id}</td>
                        <td className="p-3 font-medium text-slate-900">{p.name}</td>
                        <td className="p-3">
                          <span className="inline-block px-2 py-0.5 bg-slate-100 text-slate-700 text-xs rounded-md">
                            {p.categoryName || "N/A"}
                          </span>
                        </td>
                        <td className="p-3">
                          <span className="inline-block px-2 py-0.5 bg-slate-100 text-slate-700 text-xs rounded-md">
                            {p.brandName || "N/A"}
                          </span>
                        </td>
                        <td className="p-3 text-right space-x-2">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              handleViewDetail(p.id);
                            }}
                            className="text-blue-600 hover:text-blue-800 text-xs font-medium"
                          >
                            Xem biến thể
                          </button>
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              handleDeleteProduct(p.id);
                            }}
                            className="text-red-600 hover:text-red-800 text-xs font-medium"
                          >
                            Xóa
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 space-y-4">
            {selectedProduct ? (
              <>
                <div className="flex items-start justify-between">
                  <div>
                    <h3 className="font-bold text-lg text-slate-900">{selectedProduct.name}</h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Danh mục: {selectedProduct.categoryName || "N/A"} | Thương hiệu: {selectedProduct.brandName || "N/A"}
                    </p>
                  </div>
                  <button
                    onClick={() => setShowAddSkuModal(true)}
                    className="bg-emerald-600 hover:bg-emerald-700 text-white text-xs px-3 py-1.5 rounded-lg font-medium shadow-sm transition"
                  >
                    + Thêm SKU
                  </button>
                </div>

                <div className="border-t pt-4">
                  <h4 className="text-xs font-semibold uppercase text-slate-400 tracking-wider mb-2">
                    Danh sách biến thể SKU ({selectedProduct.skus?.length || 0})
                  </h4>

                  {(!selectedProduct.skus || selectedProduct.skus.length === 0) ? (
                    <div className="text-center py-6 text-slate-400 text-sm">
                      Chưa có biến thể SKU nào. Hãy thêm SKU đầu tiên.
                    </div>
                  ) : (
                    <div className="space-y-3">
                      {selectedProduct.skus.map((sku) => (
                        <div key={sku.id} className="p-3 rounded-xl border border-slate-100 bg-slate-50/50 space-y-1">
                          <div className="flex justify-between items-center">
                            <span className="font-mono font-semibold text-xs text-blue-700 bg-blue-100/70 px-2 py-0.5 rounded">
                              {sku.skuCode}
                            </span>
                            <span className="font-semibold text-sm text-slate-900">
                              {(sku.retailPrice || sku.sellingPrice).toLocaleString()} đ
                            </span>
                          </div>
                          <div className="text-xs text-slate-600">
                            {sku.attributeValues || "Tiêu chuẩn"}
                          </div>
                          <div className="flex justify-between items-center text-xs text-slate-500 pt-1 border-t border-slate-200/50">
                            <span>Giá nhập: {sku.costPrice.toLocaleString()} đ</span>
                            {sku.wholesalePrice && <span>Giá sỉ: {sku.wholesalePrice.toLocaleString()} đ</span>}
                            <span>Mã vạch: {sku.barcode || "Chưa có"}</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </>
            ) : (
              <div className="text-center py-16 text-slate-400 text-sm">
                Chọn một sản phẩm bên trái để xem và quản lý các biến thể SKU
              </div>
            )}
          </div>
        </div>

        {showAddModal && (
          <div className="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl space-y-4">
              <h3 className="text-lg font-bold text-slate-900">Thêm sản phẩm mới</h3>
              <form onSubmit={handleCreateProduct} className="space-y-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Tên sản phẩm *</label>
                  <input
                    type="text"
                    required
                    placeholder="VD: Áo Thun Cổ Tròn"
                    value={newProductName}
                    onChange={(e) => setNewProductName(e.target.value)}
                    className="w-full px-3 py-2 border rounded-xl text-sm focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Danh mục *</label>
                  <select
                    value={newProductCatId}
                    onChange={(e) => setNewProductCatId(Number(e.target.value))}
                    className="w-full px-3 py-2 border rounded-xl text-sm focus:ring-2 focus:ring-blue-500"
                  >
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Thương hiệu</label>
                  <select
                    value={newProductBrandId || ""}
                    onChange={(e) => setNewProductBrandId(e.target.value ? Number(e.target.value) : undefined)}
                    className="w-full px-3 py-2 border rounded-xl text-sm focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">Không chọn</option>
                    {brands.map((b) => (
                      <option key={b.id} value={b.id}>
                        {b.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Mô tả</label>
                  <textarea
                    value={newProductDesc}
                    onChange={(e) => setNewProductDesc(e.target.value)}
                    rows={3}
                    placeholder="Mô tả thông tin chi tiết..."
                    className="w-full px-3 py-2 border rounded-xl text-sm focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div className="flex justify-end gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setShowAddModal(false)}
                    className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-sm font-medium"
                  >
                    Hủy
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-sm font-medium"
                  >
                    Lưu sản phẩm
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {showAddSkuModal && selectedProduct && (
          <div className="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl space-y-4">
              <h3 className="text-lg font-bold text-slate-900">Thêm SKU cho: {selectedProduct.name}</h3>
              <form onSubmit={handleCreateSku} className="space-y-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Mã SKU *</label>
                  <input
                    type="text"
                    required
                    placeholder="VD: POLO-DEN-M"
                    value={skuCode}
                    onChange={(e) => setSkuCode(e.target.value)}
                    className="w-full px-3 py-2 border rounded-xl text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Thuộc tính (Màu / Size)</label>
                  <input
                    type="text"
                    placeholder="VD: Màu Đen, Size M"
                    value={skuAttr}
                    onChange={(e) => setSkuAttr(e.target.value)}
                    className="w-full px-3 py-2 border rounded-xl text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Mã vạch Barcode</label>
                  <input
                    type="text"
                    placeholder="VD: 89350000001"
                    value={skuBarcode}
                    onChange={(e) => setSkuBarcode(e.target.value)}
                    className="w-full px-3 py-2 border rounded-xl text-sm"
                  />
                </div>
                <div className="grid grid-cols-3 gap-2">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Giá nhập *</label>
                    <input
                      type="number"
                      required
                      min={0}
                      value={skuCost}
                      onChange={(e) => setSkuCost(Number(e.target.value))}
                      className="w-full px-2.5 py-2 border rounded-xl text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Giá bán lẻ *</label>
                    <input
                      type="number"
                      required
                      min={1}
                      value={skuRetailPrice}
                      onChange={(e) => setSkuRetailPrice(Number(e.target.value))}
                      className="w-full px-2.5 py-2 border rounded-xl text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Giá bán sỉ</label>
                    <input
                      type="number"
                      min={0}
                      value={skuWholesalePrice}
                      onChange={(e) => setSkuWholesalePrice(Number(e.target.value))}
                      className="w-full px-2.5 py-2 border rounded-xl text-sm"
                    />
                  </div>
                </div>
                <div className="flex justify-end gap-2 pt-3">
                  <button
                    type="button"
                    onClick={() => setShowAddSkuModal(false)}
                    className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-sm font-medium"
                  >
                    Hủy
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-sm font-medium"
                  >
                    Lưu SKU
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
