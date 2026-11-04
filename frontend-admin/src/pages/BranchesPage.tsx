import { useEffect, useState } from 'react'
import { Button, Form, Input, Modal, Select, Space, Table, Tag, Typography, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { branchAdminApi } from '../api/services'
import type { Branch } from '../api/types'

export default function BranchesPage() {
  const [branches, setBranches] = useState<Branch[]>([])
  const [loading, setLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingBranch, setEditingBranch] = useState<Branch | null>(null)
  const [form] = Form.useForm()

  const loadData = async () => {
    setLoading(true)
    try {
      const data = await branchAdminApi.list()
      setBranches(data)
    } catch {
      message.error('Không thể tải danh sách chi nhánh')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingBranch) {
        await branchAdminApi.update(editingBranch.id, values)
        message.success('Đã cập nhật chi nhánh')
      } else {
        await branchAdminApi.create(values)
        message.success('Đã tạo chi nhánh mới')
      }
      setModalOpen(false)
      form.resetFields()
      loadData()
    } catch (e: any) {
      if (e?.response?.data?.message) {
        message.error(e.response.data.message)
      }
    }
  }

  const toggleStatus = async (branch: Branch) => {
    try {
      await branchAdminApi.setStatus(branch.id, !branch.active)
      message.success(branch.active ? 'Đã ẩn chi nhánh' : 'Đã hiện chi nhánh')
      loadData()
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Không thể thay đổi trạng thái chi nhánh (có thể còn tồn kho hoặc đơn hàng)')
    }
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Typography.Title level={3} style={{ margin: 0 }}>Quản lý chi nhánh & kho hàng</Typography.Title>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => {
            setEditingBranch(null)
            form.resetFields()
            setModalOpen(true)
          }}
        >
          Thêm chi nhánh/kho
        </Button>
      </div>

      <Table<Branch>
        loading={loading}
        dataSource={branches}
        rowKey="id"
        columns={[
          { title: 'Tên chi nhánh / Kho', dataIndex: 'name', key: 'name' },
          {
            title: 'Loại',
            dataIndex: 'type',
            key: 'type',
            render: (type) => (
              <Tag color={type === 'STORE' ? 'blue' : 'purple'}>
                {type === 'STORE' ? 'Cửa hàng bán lẻ' : 'Kho trung chuyển'}
              </Tag>
            ),
          },
          { title: 'Địa chỉ', dataIndex: 'address', key: 'address', render: (v) => v || '—' },
          {
            title: 'Trạng thái',
            dataIndex: 'active',
            key: 'active',
            render: (active) => (
              <Tag color={active ? 'success' : 'default'}>{active ? 'Hoạt động' : 'Đã ẩn'}</Tag>
            ),
          },
          {
            title: 'Thao tác',
            key: 'actions',
            render: (_, r) => (
              <Space>
                <Button
                  size="small"
                  onClick={() => {
                    setEditingBranch(r)
                    form.setFieldsValue(r)
                    setModalOpen(true)
                  }}
                >
                  Sửa
                </Button>
                <Button size="small" onClick={() => toggleStatus(r)}>
                  {r.active ? 'Ẩn' : 'Hiện'}
                </Button>
              </Space>
            ),
          },
        ]}
      />

      <Modal
        title={editingBranch ? 'Cập nhật chi nhánh' : 'Thêm chi nhánh / Kho mới'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Tên chi nhánh/kho" rules={[{ required: true, message: 'Vui lòng nhập tên' }]}>
            <Input placeholder="Chi nhánh Quận 1" />
          </Form.Item>
          <Form.Item name="type" label="Loại hình" initialValue="STORE" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'STORE', label: 'Cửa hàng (Bán trực tiếp & POS)' },
                { value: 'WAREHOUSE', label: 'Kho (Chỉ lưu trữ & xuất nhập)' },
              ]}
            />
          </Form.Item>
          <Form.Item name="address" label="Địa chỉ">
            <Input placeholder="123 Nguyễn Huệ, Phường Bến Nghé, Quận 1" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
