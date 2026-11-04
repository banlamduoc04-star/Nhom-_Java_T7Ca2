'use client';

import React from 'react';
import { Modal, Form, Input, InputNumber, message } from 'antd';

export default function SkuFormModal({ open, productId, onCancel, onSuccess }: any) {
  const [form] = Form.useForm();

  const handleSubmit = async (values: any) => {
    try {
      // Gọi API POST /api/v1/products/{id}/skus từ TV2
      message.success('Thêm biến thể SKU thành công!');
      form.resetFields();
      onSuccess();
    } catch (e) {
      message.error('Lỗi khi thêm SKU');
    }
  };

  return (
    <Modal title="Thêm Biến Thể SKU Mới" open={open} onCancel={onCancel} onOk={() => form.submit()}>
      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <Form.Item name="skuCode" label="Mã SKU (Duy nhất)" rules={[{ required: true }]}>
          <Input placeholder="VD: POLO-RED-XL" />
        </Form.Item>
        <Form.Item name="barcode" label="Mã vạch Barcode" rules={[{ required: true }]}>
          <Input placeholder="VD: 893600123" />
        </Form.Item>
        <div className="grid grid-cols-2 gap-4">
          <Form.Item name="color" label="Màu sắc"><Input placeholder="Đỏ" /></Form.Item>
          <Form.Item name="size" label="Size"><Input placeholder="XL" /></Form.Item>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <Form.Item name="costPrice" label="Giá nhập cơ sở"><InputNumber className="w-full" min={0} /></Form.Item>
          <Form.Item name="price" label="Giá bán"><InputNumber className="w-full" min={0} /></Form.Item>
        </div>
      </Form>
    </Modal>
  );
}