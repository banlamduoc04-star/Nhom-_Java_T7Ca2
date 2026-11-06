import { useCallback, useEffect, useState } from 'react'
import { App, Button, Card, Form, Input, Modal, Switch, Table, Tag } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { supplierApi } from '../../api/services'
import { errorText } from '../../api/client'
import type { Supplier } from '../../api/types'

interface Values {
  name: string
  phone?: string
  address?: string
  active: boolean
}

/** FR-INV-05: nha cung cap (Owner, Staff). Ngung dung bang cong tac, khong xoa. */
export default function SuppliersPage() {
  const { message } = App.useApp()
  const [list, setList] = useState<Supplier[]>([])
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<Supplier | 'new' | null>(null)
  const [form] = Form.useForm<Values>()

  const reload = useCallback(() => {
    setLoading(true)
    supplierApi.list().then(setList).catch((e) => message.error(errorText(e))).finally(() => setLoading(false))
  }, [message])

  useEffect(reload, [reload])

  const open = (value: Supplier | 'new') => {
    setEditing(value)
    form.setFieldsValue(value === 'new' ? { name: '', phone: '', address: '', active: true }
      : { name: value.name, phone: value.phone ?? '', address: value.address ?? '', active: value.active })
  }

  const save = async () => {
    const v = await form.validateFields()
    try {
      if (editing === 'new') {
        await supplierApi.create({ name: v.name, phone: v.phone, address: v.address })
      } else if (editing) {
        await supplierApi.update(editing.id, v)
      }
      message.success('Đã lưu nhà cung cấp')
      setEditing(null)
      reload()
    } catch (e) {
      message.error(errorText(e))
    }
  }

  return (
    <Card title="Nhà cung cấp" extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => open('new')}>Thêm nhà cung cấp</Button>}>
      <Table<Supplier> rowKey="id" loading={loading} dataSource={list} locale={{ emptyText: 'Chưa có nhà cung cấp nào' }}
        columns={[
          { title: 'Tên', dataIndex: 'name' },
          { title: 'Điện thoại', dataIndex: 'phone' },
          { title: 'Địa chỉ', dataIndex: 'address' },
          { title: 'Trạng thái', dataIndex: 'active', render: (a: boolean) => <Tag color={a ? 'green' : 'default'}>{a ? 'Đang dùng' : 'Ngưng dùng'}</Tag> },
          { title: 'Thao tác', width: 100, render: (_: unknown, row: Supplier) => <Button size="small" onClick={() => open(row)}>Sửa</Button> },
        ]} />
      <Modal open={editing !== null} title={editing === 'new' ? 'Thêm nhà cung cấp' : 'Sửa nhà cung cấp'} okText="Lưu" cancelText="Hủy"
        onOk={save} onCancel={() => setEditing(null)} destroyOnHidden>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Tên nhà cung cấp" rules={[{ required: true, message: 'Nhập tên nhà cung cấp' }, { max: 255 }]}><Input autoFocus /></Form.Item>
          <Form.Item name="phone" label="Điện thoại" rules={[{ pattern: /^$|^[0-9+]{8,15}$/, message: 'Số điện thoại không hợp lệ' }]}><Input /></Form.Item>
          <Form.Item name="address" label="Địa chỉ" rules={[{ max: 500 }]}><Input /></Form.Item>
          <Form.Item name="active" label="Đang sử dụng" valuePropName="checked"><Switch /></Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
