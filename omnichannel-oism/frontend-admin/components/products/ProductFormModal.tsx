'use client';

import React from 'react';
import { Modal, Form, Input, Select, message } from 'antd';

interface Props {
  open: boolean;
  onCancel: () => void;
  onSuccess: () => void;
}

export default function ProductFormModal({ open, onCancel, onSuccess }: Props) {
  const [form] = Form.useForm();

  const handleSubmit = async (values: any) => {
    try {
      // Gọi API từ Backend (TV2)
      // await axiosInstance.post('/api/v1/products', values);
      message.success('Tạo sản phẩm thành công!');
      form.resetFields();
      onSuccess();
    } catch (error) {
      message.error('Lỗi khi tạo sản phẩm');
    }
  };

  return (
    <Modal
      title="Thêm Sản phẩm Mới"
      open={open}
      onCancel={onCancel}
      onOk={() => form.submit()}
      okText="Lưu sản phẩm"
      cancelText="Hủy"
    >
      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <Form.Item name="name" label="Tên sản phẩm" rules={[{ required: true, message: 'Nhập tên sản phẩm' }]}>
          <Input placeholder="Ví dụ: Áo Polo Nam Premium" />
        </Form.Item>
        <Form.Item name="categoryId" label="Danh mục" rules={[{ required: true, message: 'Chọn danh mục' }]}>
          <Select placeholder="Chọn danh mục">
            <Select.Option value="cat-1">Áo Nam</Select.Option>
            <Select.Option value="cat-2">Quần Nam</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item name="description" label="Mô tả">
          <Input.TextArea rows={3} placeholder="Mô tả chi tiết sản phẩm..." />
        </Form.Item>
      </Form>
    </Modal>
  );
}