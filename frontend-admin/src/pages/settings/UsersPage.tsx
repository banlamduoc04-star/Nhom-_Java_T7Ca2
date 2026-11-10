import { useEffect, useState } from 'react'
import { Button, Form, Input, Modal, Select, Space, Table, Tag, Typography, message } from 'antd'
import { LockOutlined, PlusOutlined, UnlockOutlined } from '@ant-design/icons'
import { branchApi, userAdminApi } from '../../api/services'
import type { Branch, Role, UserView } from '../../api/types'
import { errorText } from '../../api/client'

export default function UsersPage() {
  const [users, setUsers] = useState<UserView[]>([])
  const [branches, setBranches] = useState<Branch[]>([])
  const [loading, setLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [pwModalOpen, setPwModalOpen] = useState(false)
  const [selectedUser, setSelectedUser] = useState<UserView | null>(null)
  const [form] = Form.useForm()
  const [pwForm] = Form.useForm()

  const loadData = async () => {
    setLoading(true)
    try {
      const [uList, bList] = await Promise.all([
        userAdminApi.list(),
        branchApi.list(),
      ])
      setUsers(uList)
      setBranches(bList)
    } catch {
      message.error('Không thể tải danh sách tài khoản')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const handleCreate = async () => {
    try {
      const values = await form.validateFields()
      await userAdminApi.create(values)
      message.success('Đã tạo tài khoản nhân viên thành công')
      setModalOpen(false)
      form.resetFields()
      loadData()
    } catch (e) {
      message.error(errorText(e))
    }
  }

  const toggleStatus = async (user: UserView) => {
    try {
      await userAdminApi.setStatus(user.id, !user.active)
      message.success(user.active ? 'Đã khóa tài khoản' : 'Đã mở khóa tài khoản')
      loadData()
    } catch {
      message.error('Lỗi khi thay đổi trạng thái tài khoản')
    }
  }

  const handleResetPassword = async () => {
    if (!selectedUser) return
    try {
      const values = await pwForm.validateFields()
      await userAdminApi.resetPassword(selectedUser.id, values.password)
      message.success('Đã đặt lại mật khẩu thành công')
      setPwModalOpen(false)
      pwForm.resetFields()
    } catch {
      message.error('Lỗi khi đặt lại mật khẩu')
    }
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Typography.Title level={3} style={{ margin: 0 }}>Quản lý nhân viên & tài khoản</Typography.Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>Thêm nhân viên</Button>
      </div>

      <Table<UserView>
        loading={loading}
        dataSource={users}
        rowKey="id"
        columns={[
          { title: 'Họ và tên', dataIndex: 'fullName', key: 'fullName' },
          { title: 'Email', dataIndex: 'email', key: 'email' },
          { title: 'Số điện thoại', dataIndex: 'phone', key: 'phone', render: (v) => v || '—' },
          {
            title: 'Vai trò',
            dataIndex: 'role',
            key: 'role',
            render: (role: Role) => {
              const color = role === 'OWNER' ? 'gold' : role === 'STAFF' ? 'blue' : 'green'
              const text = role === 'OWNER' ? 'Chủ cửa hàng' : role === 'STAFF' ? 'Nhân viên kho' : 'Thu ngân (POS)'
              return <Tag color={color}>{text}</Tag>
            },
          },
          { title: 'Chi nhánh', dataIndex: 'branchName', key: 'branchName', render: (v) => v || 'Tất cả' },
          {
            title: 'Trạng thái',
            dataIndex: 'active',
            key: 'active',
            render: (active: boolean) => (
              <Tag color={active ? 'success' : 'error'}>{active ? 'Đang hoạt động' : 'Bị khóa'}</Tag>
            ),
          },
          {
            title: 'Thao tác',
            key: 'actions',
            render: (_, r) => (
              <Space>
                {r.role !== 'OWNER' && (
                  <Button
                    size="small"
                    danger={r.active}
                    icon={r.active ? <LockOutlined /> : <UnlockOutlined />}
                    onClick={() => toggleStatus(r)}
                  >
                    {r.active ? 'Khóa' : 'Mở khóa'}
                  </Button>
                )}
                <Button
                  size="small"
                  onClick={() => {
                    setSelectedUser(r)
                    setPwModalOpen(true)
                  }}
                >
                  Đổi mật khẩu
                </Button>
              </Space>
            ),
          },
        ]}
      />

      <Modal
        title="Thêm tài khoản nhân viên mới"
        open={modalOpen}
        onOk={handleCreate}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="fullName" label="Họ và tên" rules={[{ required: true, message: 'Vui lòng nhập họ tên' }]}>
            <Input placeholder="Nguyễn Văn A" />
          </Form.Item>
          <Form.Item name="email" label="Email đăng nhập" rules={[{ required: true, type: 'email', message: 'Email không hợp lệ' }]}>
            <Input placeholder="staff@oism.vn" />
          </Form.Item>
          <Form.Item name="phone" label="Số điện thoại">
            <Input placeholder="0901234567" />
          </Form.Item>
          <Form.Item name="password" label="Mật khẩu" rules={[{ required: true, min: 6, message: 'Tối thiểu 6 ký tự' }]}>
            <Input.Password placeholder="••••••" />
          </Form.Item>
          <Form.Item name="role" label="Vai trò" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'STAFF', label: 'Nhân viên (Kho & Đơn hàng)' },
                { value: 'CASHIER', label: 'Thu ngân (Chỉ bán POS)' },
              ]}
            />
          </Form.Item>
          <Form.Item name="branchId" label="Chi nhánh gán (Thu ngân bắt buộc gán chi nhánh)">
            <Select
              allowClear
              placeholder="Chọn chi nhánh làm việc"
              options={branches.map((b) => ({ value: b.id, label: b.name }))}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`Đặt lại mật khẩu cho ${selectedUser?.fullName}`}
        open={pwModalOpen}
        onOk={handleResetPassword}
        onCancel={() => setPwModalOpen(false)}
        destroyOnClose
      >
        <Form form={pwForm} layout="vertical">
          <Form.Item name="password" label="Mật khẩu mới" rules={[{ required: true, min: 6, message: 'Tối thiểu 6 ký tự' }]}>
            <Input.Password placeholder="Mật khẩu mới..." />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
