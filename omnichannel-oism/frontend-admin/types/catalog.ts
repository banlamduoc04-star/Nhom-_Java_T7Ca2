export interface Brand {
  id: number;
  name: string;
  description?: string;
}

export interface Category {
  id: number;
  name: string;
  description?: string;
  parentId?: number;
  parentName?: string;
}

export interface Sku {
  id: number;
  skuCode: string;
  productId: number;
  productName?: string;
  attributeValues?: string;
  barcode?: string;
  costPrice: number;
  sellingPrice: number;
  retailPrice?: number;
  wholesalePrice?: number;
}

export interface Product {
  id: number;
  name: string;
  description?: string;
  imageUrl?: string;
  categoryId: number;
  categoryName?: string;
  brandId?: number;
  brandName?: string;
  isActive: boolean;
}

export interface ProductDetail extends Product {
  skus: Sku[];
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  pageNo: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
