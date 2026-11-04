'use client';

import React from 'react';
import { Table, Tag, Button } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import Link from 'next/link';

export default function MovementHistoryPage() {
  const movements = [
    { id: 'm1', createdAt: '2026-10-12 10:30', skuCode: 'POLO-RED-L', type: 'PURCHASE_IN', qty: 50, balanceAfter: 50, refId: 'REC-001' },
    { id: 'm2', createdAt: '2026-10-12 11:15', skuCode: 'POLO-RED-L', type: 'RESERVE', qty: 2, balanceAfter: 50, refId: 'ORD-9921' },
    { id: 'm3', createdAt: '2026-10-12 11:20', skuCode: 'POLO-RED-L', type: 'SALE_OUT', qty: -2, balanceAfter: 48, refId: 'ORD-9921' },
  ];

  const columns = [
    { title: 'Thời gian', dataIndex: 'createdAt', key: 'createdAt' },
    { title: 'Mã SKU', dataIndex: 'skuCode', key: 'skuCode' },
    { 
      title: 'Loại biến động', 
      dataIndex: 'type', 
      key: 'type',
      render: (type: string) => {
        const colorMap: any = { PURCHASE_IN: 'blue', SALE_OUT: 'green', RESERVE: 'orange', RELEASE: 'purple' };
        return <Tag color={colorMap[type] || 'default'}>{type}</Tag>;
      }
    },
    { 
      title: 'Số lượng', 
      dataIndex: 'qty', 
      key: 'qty',
      render: (qty: number) => (
        <span className={qty > 0 ? 'text-green-600 font-bold' : 'text-red-600 font-bold'}>
          {qty > 0 ? `+${qty}` : qty}
        </span>
      )
    },
    { title: 'Tồn sau biến động', dataIndex: 'balanceAfter', key: 'balanceAfter' },
    { title: 'Mã tham chiếu (Ref ID)', dataIndex: 'refId', key: 'refId' },
  ];

  return (
    <div className="p-6 space-y-4 bg-white rounded-lg shadow-sm">
      <Link href="/inventory">
        <Button icon={<ArrowLeftOutlined />}>Quay lại Tồn kho</Button>
      </Link>
      <h1 className="text-2xl font-bold text-gray-800">Sổ cái Lịch sử Kho (Append-Only Ledger)</h1>
      <Table dataSource={movements} columns={columns} rowKey="id" />
    </div>
  );
}