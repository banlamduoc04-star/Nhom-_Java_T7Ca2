export interface Category {
  id: string;
  name: string;
}

export interface Product {
  id: string;
  name: string;
  categoryId: string;
  categoryName: string;
  description?: string;
  status: 'ACTIVE' | 'INACTIVE';
  skuCount?: number;
  createdAt: string;
}