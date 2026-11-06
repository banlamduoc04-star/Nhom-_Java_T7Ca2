import { useCallback, useEffect, useState } from 'react'
import { App, Button, Card, Form, Input, Modal, Popconfirm, Space, Table } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { brandApi } from '../../api/services'
import { errorText } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import type { Brand } from '../../api/types'

/** FR-PROD-01: thuong hieu (Owner sua; Staff chi xem). */
export default function BrandsPage() {
  const { message } = App.useApp()
  const { hasRole } = useAuth()
  const canEdit = hasRole('OWNER')
  const [list, setList] = useState<Brand[]>([])
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<Brand | 'new' | null>(null)
  const [form] = Form.useForm<{ name: string }>()

  const reload = useCallback(() => {
    setLoading(true)
    brandApi.list().then(setList).catch((e) => message.error(errorText(e))).finally(() => setLoading(false))
  }, [message])

  useEffect(reload, [reload])

  const open = (value: Brand | 'new') => {
    setEditing(value)
    form.setFieldsValue({ name: value === 'new' ? '' : value.name })
  }

  const save = async () => {
    const { name } = await form.validateFields()
    try {
      if (editing === 'new') {
        await brandApi.create({ name })
      } else if (editing) {
        await brandApi.update(editing.id, { name })
      }
      message.success('Đã lưu thương hiệu')
      setEditing(null)
      reload()
    } catch (e) {
      message.error(errorText(e))
    }
  }

  const remove = async (id: string) => {
    try {
      await brandApi.remove(id)
      message.success('Đã xóa thương hiệu')
      reload()
    } catch (e) {
      message.error(errorText(e))
    }
  }

  return (
    <Card title="Thương hiệu"
      extra={canEdit && <Button type="primary" icon={<PlusOutlined />} onClick={() => open('new')}>Thêm thương hiệu</Button>}>
      <Table<Brand> rowKey="id" loading={loading} dataSource={list} locale={{ emptyText: 'Chưa có thương hiệu nào' }}
        columns={[
          { title: 'Tên thương hiệu', dataIndex: 'name' },
          ...(canEdit ? [{
            title: 'Thao tác', width: 180,
            render: (_: unknown, row: Brand) => (
              <Space>
                <Button size="small" onClick={() => open(row)}>Sửa</Button>
                <Popconfirm title="Xóa thương hiệu này?" okText="Xóa" cancelText="Hủy" onConfirm={() => remove(row.id)}>
                  <Button size="small" danger>Xóa</Button>
                </Popconfirm>
              </Space>
            ),
          }] : []),
        ]} />
      <Modal open={editing !== null} title={editing === 'new' ? 'Thêm thương hiệu' : 'Sửa thương hiệu'} okText="Lưu" cancelText="Hủy"
        onOk={save} onCancel={() => setEditing(null)} destroyOnHidden>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Tên thương hiệu" rules={[{ required: true, message: 'Nhập tên thương hiệu' }, { max: 150 }]}>
            <Input autoFocus />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
