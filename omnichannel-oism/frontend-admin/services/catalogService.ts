import { ApiResponse, Brand, Category, PageResponse, Product, ProductDetail, Sku } from "@/types/catalog";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

export const brandService = {
  async getAllBrands(): Promise<Brand[]> {
    const res = await fetch(`${API_BASE_URL}/brands`);
    if (!res.ok) throw new Error("Khong the tai danh sach thuong hieu");
    const json: ApiResponse<Brand[]> = await res.json();
    return json.data || [];
  },

  async createBrand(data: { name: string; description?: string }): Promise<Brand> {
    const res = await fetch(`${API_BASE_URL}/brands`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error("Khong the tao thuong hieu");
    const json: ApiResponse<Brand> = await res.json();
    return json.data;
  },
};

export const categoryService = {
  async getAllCategories(): Promise<Category[]> {
    const res = await fetch(`${API_BASE_URL}/categories`);
    if (!res.ok) throw new Error("Khong the tai danh muc");
    const json: ApiResponse<Category[]> = await res.json();
    return json.data || [];
  },

  async createCategory(data: { name: string; description?: string; parentId?: number }): Promise<Category> {
    const res = await fetch(`${API_BASE_URL}/categories`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error("Khong the tao danh muc");
    const json: ApiResponse<Category> = await res.json();
    return json.data;
  },
};

export const productService = {
  async searchProducts(keyword = "", categoryId?: number, page = 0, size = 10): Promise<PageResponse<Product>> {
    const params = new URLSearchParams({
      keyword,
      page: page.toString(),
      size: size.toString(),
    });
    if (categoryId) params.append("categoryId", categoryId.toString());

    const res = await fetch(`${API_BASE_URL}/products/search?${params.toString()}`);
    if (!res.ok) throw new Error("Khong the tim kiem san pham");
    const json: ApiResponse<PageResponse<Product>> = await res.json();
    return json.data;
  },

  async getProductById(id: number): Promise<ProductDetail> {
    const res = await fetch(`${API_BASE_URL}/products/${id}`);
    if (!res.ok) throw new Error("Khong the lay thong tin san pham");
    const json: ApiResponse<ProductDetail> = await res.json();
    return json.data;
  },

  async createProduct(data: {
    name: string;
    description?: string;
    imageUrl?: string;
    categoryId: number;
    brandId?: number;
  }): Promise<Product> {
    const res = await fetch(`${API_BASE_URL}/products`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error("Khong the tao san pham");
    const json: ApiResponse<Product> = await res.json();
    return json.data;
  },

  async deleteProduct(id: number): Promise<void> {
    const res = await fetch(`${API_BASE_URL}/products/${id}`, {
      method: "DELETE",
    });
    if (!res.ok) throw new Error("Khong the xoa san pham");
  },
};

export const skuService = {
  async addSku(productId: number, data: {
    skuCode: string;
    attributeValues?: string;
    barcode?: string;
    costPrice: number;
    sellingPrice: number;
    retailPrice?: number;
    wholesalePrice?: number;
  }): Promise<Sku> {
    const res = await fetch(`${API_BASE_URL}/products/${productId}/skus`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error("Khong the them bien the SKU");
    const json: ApiResponse<Sku> = await res.json();
    return json.data;
  },

  async getSkusByProduct(productId: number): Promise<Sku[]> {
    const res = await fetch(`${API_BASE_URL}/products/${productId}/skus`);
    if (!res.ok) throw new Error("Khong the lay danh sach bien the");
    const json: ApiResponse<Sku[]> = await res.json();
    return json.data || [];
  },

  async getSkuByBarcode(barcode: string): Promise<Sku> {
    const res = await fetch(`${API_BASE_URL}/skus/barcode/${barcode}`);
    if (!res.ok) throw new Error("Khong tim thay bien the theo ma vach");
    const json: ApiResponse<Sku> = await res.json();
    return json.data;
  },

  async updateSku(skuId: number, data: Partial<Sku>): Promise<Sku> {
    const res = await fetch(`${API_BASE_URL}/skus/${skuId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error("Khong the cap nhat bien the");
    const json: ApiResponse<Sku> = await res.json();
    return json.data;
  },

  async deleteSku(skuId: number): Promise<void> {
    const res = await fetch(`${API_BASE_URL}/skus/${skuId}`, {
      method: "DELETE",
    });
    if (!res.ok) throw new Error("Khong the xoa bien the");
  },
};
