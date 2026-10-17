'use client';

import React, { useState } from 'react';
import { Table, Button, Input, Tag, Space } from 'antd';
import { PlusOutlined, SearchOutlined, EyeOutlined } from '@ant-design/icons';
import Link from 'next/link';
import ProductFormModal from '@/components/products/ProductFormModal';

export default function ProductListPage() {
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Mẫu dữ liệu hiển thị (Sau này đổi thành state gọi API từ TV2)
  const products = [
    { id: '1', name: 'Áo Polo Nam Co Giãn', categoryName: 'Áo Nam', status: 'ACTIVE', skuCount: 4, createdAt: '2026-10-10' },
    { id: '2', name: 'Quần Jean Slimfit', categoryName: 'Quần Nam', status: 'ACTIVE', skuCount: 3, createdAt: '2026-10-11' },
  ];

  const columns = [
    { title: 'Tên Sản phẩm', dataIndex: 'name', key: 'name' },
    { title: 'Danh mục', dataIndex: 'categoryName', key: 'categoryName' },
    { title: 'Số biến thể (SKU)', dataIndex: 'skuCount', key: 'skuCount' },
    { 
      title: 'Trạng thái', 
      dataIndex: 'status', 
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'ACTIVE' ? 'green' : 'red'}>
          {status === 'ACTIVE' ? 'Đang kinh doanh' : 'Ngừng kinh doanh'}
        </Tag>
      )
    },
    {
      title: 'Thao tác',
      key: 'action',
      render: (_: any, record: any) => (
        <Link href={`/products/${record.id}`}>
          <Button icon={<EyeOutlined />} type="link">Chi tiết & SKU</Button>
        </Link>
      ),
    },
  ];

  return (
    <div className="p-6 space-y-4 bg-white rounded-lg shadow-sm">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-800">Quản lý Sản phẩm</h1>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsModalOpen(true)}>
          Thêm sản phẩm
        </Button>
      </div>

      <div className="flex gap-4">
        <Input placeholder="Tìm theo tên sản phẩm..." prefix={<SearchOutlined />} className="w-72" />
      </div>

      <Table dataSource={products} columns={columns} rowKey="id" />

      <ProductFormModal 
        open={isModalOpen} 
        onCancel={() => setIsModalOpen(false)} 
        onSuccess={() => setIsModalOpen(false)} 
      />
    </div>
  );
}