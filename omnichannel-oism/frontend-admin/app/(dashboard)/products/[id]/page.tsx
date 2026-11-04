'use client';

import React, { useState } from 'react';
import { Button, Table, Card, Tag, Descriptions } from 'antd';
import { PlusOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import useRouter from 'next/navigation';
import Link from 'next/link';
import SkuFormModal from '@/components/products/SkuFormModal';

export default function ProductDetailPage({ params }: { params: { id: string } }) {
  const [isSkuModalOpen, setIsSkuModalOpen] = useState(false);

  // Mẫu dữ liệu biến thể SKU
  const skus = [
    { id: 'sku-1', skuCode: 'POLO-RED-L', barcode: '893600111', color: 'Đỏ', size: 'L', costPrice: 120000, price: 250000 },
    { id: 'sku-2', skuCode: 'POLO-BLK-M', barcode: '893600112', color: 'Đen', size: 'M', costPrice: 120000, price: 250000 },
  ];

  const columns = [
    { title: 'Mã SKU', dataIndex: 'skuCode', key: 'skuCode', render: (text: string) => <strong>{text}</strong> },
    { title: 'Mã vạch (Barcode)', dataIndex: 'barcode', key: 'barcode' },
    { title: 'Màu sắc', dataIndex: 'color', key: 'color' },
    { title: 'Kích cỡ (Size)', dataIndex: 'size', key: 'size' },
    { title: 'Giá vốn (COGS)', dataIndex: 'costPrice', key: 'costPrice', render: (v: number) => `${v.toLocaleString()} đ` },
    { title: 'Giá bán niêm yết', dataIndex: 'price', key: 'price', render: (v: number) => `${v.toLocaleString()} đ` },
  ];

  return (
    <div className="p-6 space-y-6">
      <Link href="/products">
        <Button icon={<ArrowLeftOutlined />}>Quay lại danh sách</Button>
      </Link>

      <Card title="Thông tin Sản phẩm">
        <Descriptions column={3}>
          <Descriptions.Item label="Tên sản phẩm">Áo Polo Nam Co Giãn</Descriptions.Item>
          <Descriptions.Item label="Danh mục">Áo Nam</Descriptions.Item>
          <Descriptions.Item label="Trạng thái"><Tag color="green">ACTIVE</Tag></Descriptions.Item>
        </Descriptions>
      </Card>

      <div className="bg-white p-6 rounded-lg shadow-sm space-y-4">
        <div className="flex justify-between items-center">
          <h2 className="text-xl font-semibold">Danh sách Biến thể SKU</h2>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsSkuModalOpen(true)}>
            Thêm biến thể SKU
          </Button>
        </div>

        <Table dataSource={skus} columns={columns} rowKey="id" />
      </div>

      <SkuFormModal 
        open={isSkuModalOpen} 
        productId={params.id}
        onCancel={() => setIsSkuModalOpen(false)} 
        onSuccess={() => setIsSkuModalOpen(false)} 
      />
    </div>
  );
}