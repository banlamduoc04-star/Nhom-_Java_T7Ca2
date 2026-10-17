'use client';

import React from 'react';
import { Modal, Form, Select, InputNumber, Input, message } from 'antd';

export default function StockAdjustmentModal({ open, onCancel, onSuccess }: any) {
  const [form] = Form.useForm();

  const handleSubmit = async (values: any) => {
    try {
      // Gọi API từ TV3 (PURCHASE_IN hoặc ADJUSTMENT)
      message.success('Cập nhật tồn kho thành công!');
      form.resetFields();
      onSuccess();
    } catch (e) {
      message.error('Thất bại');
    }
  };

  return (
    <Modal title="Nhập hàng / Điều chỉnh Kho" open={open} onCancel={onCancel} onOk={() => form.submit()}>
      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <Form.Item name="skuId" label="Chọn biến thể SKU" rules={[{ required: true }]}>
          <Select placeholder="Chọn SKU">
            <Select.Option value="sku-1">POLO-RED-L - Áo Polo Red L</Select.Option>
            <Select.Option value="sku-2">POLO-BLK-M - Áo Polo Black M</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item name="type" label="Loại giao dịch" rules={[{ required: true }]}>
          <Select placeholder="Chọn loại">
            <Select.Option value="PURCHASE_IN">Nhập hàng bán (PURCHASE_IN)</Select.Option>
            <Select.Option value="ADJUSTMENT">Điều chỉnh kiểm kê (ADJUSTMENT)</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item name="quantity" label="Số lượng biến động" rules={[{ required: true }]}>
          <InputNumber className="w-full" min={1} placeholder="Nhập số lượng" />
        </Form.Item>
        <Form.Item name="reason" label="Ghi chú / Lý do">
          <Input.TextArea rows={2} placeholder="Ví dụ: Nhập hàng từ NCC A hoặc Kiểm kê bù hàng hỏng..." />
        </Form.Item>
      </Form>
    </Modal>
  );
}