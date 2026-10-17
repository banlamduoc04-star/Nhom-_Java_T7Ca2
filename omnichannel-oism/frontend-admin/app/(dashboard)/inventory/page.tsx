'use client';

import React, { useState } from 'react';
import { Table, Button, Tag, Input, Space } from 'antd';
import { PlusCircleOutlined, HistoryOutlined } from '@ant-design/icons';
import Link from 'next/link';
import StockAdjustmentModal from '@/components/inventory/StockAdjustmentModal';

export default function InventoryListPage() {
  const [isAdjustOpen, setIsAdjustOpen] = useState(false);

  const inventoryData = [
    { id: '1', skuCode: 'POLO-RED-L', productName: 'Áo Polo Nam', warehouse: 'Kho Chính', onHand: 50, reserved: 5, available: 45 },
    { id: '2', skuCode: 'POLO-BLK-M', productName: 'Áo Polo Nam', warehouse: 'Kho Chính', onHand: 12, reserved: 8, available: 4 }, // Available < 10 -> Tô đỏ
  ];

  const columns = [
    { title: 'Mã SKU', dataIndex: 'skuCode', key: 'skuCode' },
    { title: 'Sản phẩm', dataIndex: 'productName', key: 'productName' },
    { title: 'Kho hàng', dataIndex: 'warehouse', key: 'warehouse' },
    { title: 'Tồn thực tế (On Hand)', dataIndex: 'onHand', key: 'onHand' },
    { title: 'Đang giữ (Reserved)', dataIndex: 'reserved', key: 'reserved' },
    { 
      title: 'Có thể bán (Available)', 
      dataIndex: 'available', 
      key: 'available',
      render: (val: number) => (
        <Tag color={val < 10 ? 'red' : 'green'} className="font-bold text-sm">
          {val} {val < 10 && ' (Sắp hết)'}
        </Tag>
      )
    },
  ];

  return (
    <div className="p-6 space-y-4 bg-white rounded-lg shadow-sm">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-800">Quản lý Tồn kho</h1>
        <Space>
          <Link href="/inventory/movements">
            <Button icon={<HistoryOutlined />}>Sổ cái lịch sử kho</Button>
          </Link>
          <Button type="primary" icon={<PlusCircleOutlined />} onClick={() => setIsAdjustOpen(true)}>
            Nhập / Điều chỉnh kho
          </Button>
        </Space>
      </div>

      <Table dataSource={inventoryData} columns={columns} rowKey="id" />

      <StockAdjustmentModal 
        open={isAdjustOpen} 
        onCancel={() => setIsAdjustOpen(false)} 
        onSuccess={() => setIsAdjustOpen(false)} 
      />
    </div>
  );
}