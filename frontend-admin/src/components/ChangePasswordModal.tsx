import { Form, Input, Modal, message } from 'antd'
import { authApi } from '../api/services'

interface Props {
  open: boolean
  onClose: () => void
  onDone: () => void
}

export default function ChangePasswordModal({ open, onClose, onDone }: Props) {
  const [form] = Form.useForm()

  const submit = async () => {
    const v = await form.validateFields()
    try {
      await authApi.changePassword(v.currentPassword, v.newPassword)
      message.success('Đổi mật khẩu thành công, vui lòng đăng nhập lại')
      form.resetFields()
      onDone()
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Không đổi được mật khẩu')
    }
  }

  return (
    <Modal
      title="Đổi mật khẩu"
      open={open}
      onOk={submit}
      onCancel={() => { form.resetFields(); onClose() }}
      okText="Đổi mật khẩu"
      cancelText="Hủy"
    >
      <Form form={form} layout="vertical">
        <Form.Item name="currentPassword" label="Mật khẩu hiện tại"
          rules={[{ required: true, message: 'Nhập mật khẩu hiện tại' }]}>
          <Input.Password />
        </Form.Item>
        <Form.Item name="newPassword" label="Mật khẩu mới"
          rules={[{ required: true, message: 'Nhập mật khẩu mới' }, { min: 8, max: 72, message: 'Mật khẩu từ 8 đến 72 ký tự' }]}>
          <Input.Password />
        </Form.Item>
        <Form.Item name="confirm" label="Nhập lại mật khẩu mới" dependencies={['newPassword']}
          rules={[
            { required: true, message: 'Nhập lại mật khẩu mới' },
            ({ getFieldValue }) => ({
              validator: (_, value) =>
                !value || getFieldValue('newPassword') === value
                  ? Promise.resolve()
                  : Promise.reject(new Error('Mật khẩu nhập lại không khớp')),
            }),
          ]}>
          <Input.Password />
        </Form.Item>
      </Form>
    </Modal>
  )
}