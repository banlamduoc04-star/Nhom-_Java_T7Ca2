import { useCallback, useEffect, useMemo, useState } from 'react'
import { App, Button, Card, Form, Input, Modal, Popconfirm, Select, Space, Table } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { categoryApi } from '../api/services'
import { errorText } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { Category } from '../api/types'

interface Node extends Category {
  children?: Node[]
}

function buildTree(list: Category[]): Node[] {
  const byParent = new Map<string | null, Category[]>()
  list.forEach((c) => byParent.set(c.parentId, [...(byParent.get(c.parentId) ?? []), c]))
  const make = (parentId: string | null): Node[] =>
    (byParent.get(parentId) ?? []).map((c) => {
      const children = make(c.id)
      return children.length ? { ...c, children } : { ...c }
    })
  return make(null)
}

/** FR-PROD-01: danh muc nhieu cap (Owner sua; Staff chi xem). */
export default function CategoriesPage() {
  const { message } = App.useApp()
  const { hasRole } = useAuth()
  const canEdit = hasRole('OWNER')
  const [list, setList] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<Category | 'new' | null>(null)
  const [form] = Form.useForm<{ name: string; parentId?: string }>()

  const reload = useCallback(() => {
    setLoading(true)
    categoryApi.list().then(setList).catch((e) => message.error(errorText(e))).finally(() => setLoading(false))
  }, [message])

  useEffect(reload, [reload])

  const tree = useMemo(() => buildTree(list), [list])
  const parentOptions = list
    .filter((c) => editing === 'new' || editing === null || c.id !== editing.id)
    .map((c) => ({ value: c.id, label: c.name }))

  const open = (value: Category | 'new') => {
    setEditing(value)
    form.setFieldsValue(value === 'new' ? { name: '', parentId: undefined } : { name: value.name, parentId: value.parentId ?? undefined })
  }

  const save = async () => {
    const values = await form.validateFields()
    const body = { name: values.name, parentId: values.parentId ?? null }
    try {
      if (editing === 'new') {
        await categoryApi.create(body)
      } else if (editing) {
        await categoryApi.update(editing.id, body)
      }
      message.success('Đã lưu danh mục')
      setEditing(null)
      reload()
    } catch (e) {
      message.error(errorText(e))
    }
  }

  const remove = async (id: string) => {
    try {
      await categoryApi.remove(id)
      message.success('Đã xóa danh mục')
      reload()
    } catch (e) {
      message.error(errorText(e))
    }
  }

  return (
    <Card title="Danh mục sản phẩm"
      extra={canEdit && <Button type="primary" icon={<PlusOutlined />} onClick={() => open('new')}>Thêm danh mục</Button>}>
      <Table<Node> rowKey="id" loading={loading} dataSource={tree} pagination={false} locale={{ emptyText: 'Chưa có danh mục nào' }}
        columns={[
          { title: 'Tên danh mục', dataIndex: 'name' },
          ...(canEdit ? [{
            title: 'Thao tác', width: 180,
            render: (_: unknown, row: Node) => (
              <Space>
                <Button size="small" onClick={() => open(row)}>Sửa</Button>
                <Popconfirm title="Xóa danh mục này?" okText="Xóa" cancelText="Hủy" onConfirm={() => remove(row.id)}>
                  <Button size="small" danger>Xóa</Button>
                </Popconfirm>
              </Space>
            ),
          }] : []),
        ]} />
      <Modal open={editing !== null} title={editing === 'new' ? 'Thêm danh mục' : 'Sửa danh mục'} okText="Lưu" cancelText="Hủy"
        onOk={save} onCancel={() => setEditing(null)} destroyOnHidden>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Tên danh mục" rules={[{ required: true, message: 'Nhập tên danh mục' }, { max: 150 }]}>
            <Input autoFocus />
          </Form.Item>
          <Form.Item name="parentId" label="Danh mục cha (để trống nếu là cấp gốc)">
            <Select allowClear options={parentOptions} placeholder="Không có" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
