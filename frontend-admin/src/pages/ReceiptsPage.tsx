import { useCallback, useEffect, useState } from 'react'
import { App, Button, Card, Drawer, Form, InputNumber, Popconfirm, Select, Space, Table, Tag } from 'antd'
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons'
import { branchApi, receiptApi, supplierApi } from '../api/services'
import { errorText } from '../api/client'
import SkuSelect from '../components/SkuSelect'
import type { Branch, Receipt, ReceiptStatus, Supplier } from '../api/types'

interface FormValues {
  branchId: string
  supplierId?: string
  items: { skuId?: string; quantity?: number; unitPrice?: number }[]
}

const money = (v: number) => `${v.toLocaleString('vi-VN')} ₫`

/** FR-INV-02: phieu nhap kho Nhap -> Da xac nhan. Chi xac nhan moi ghi so cai, tang ton va tinh lai gia von. */
export default function ReceiptsPage() {
  const { message } = App.useApp()
  const [branches, setBranches] = useState<Branch[]>([])
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [rows, setRows] = useState<Receipt[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<ReceiptStatus | undefined>()
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<Receipt | 'new' | null>(null)
  const [form] = Form.useForm<FormValues>()

  useEffect(() => {
    Promise.all([branchApi.list(), supplierApi.list()]).then(([b, s]) => {
      setBranches(b)
      setSuppliers(s)
    }).catch((e) => message.error(errorText(e)))
  }, [message])

  const load = useCallback(() => {
    setLoading(true)
    receiptApi.list({ status, page, size: 20 })
      .then((p) => {
        setRows(p.content)
        setTotal(p.totalElements)
      })
      .catch((e) => message.error(errorText(e)))
      .finally(() => setLoading(false))
  }, [status, page, message])

  useEffect(load, [load])

  const branchName = (id: string) => branches.find((b) => b.id === id)?.name ?? id
  const supplierName = (id: string | null) => suppliers.find((s) => s.id === id)?.name ?? '—'

  const open = (value: Receipt | 'new') => {
    setEditing(value)
    form.setFieldsValue(value === 'new'
      ? { branchId: branches.find((b) => b.active)?.id, supplierId: undefined, items: [{}] }
      : {
        branchId: value.branchId, supplierId: value.supplierId ?? undefined,
        items: value.items.map((i) => ({ skuId: i.skuId, quantity: i.quantity, unitPrice: i.unitPrice })),
      })
  }

  const save = async () => {
    const v = await form.validateFields()
    const body = {
      branchId: v.branchId, supplierId: v.supplierId ?? null,
      items: v.items.map((i) => ({ skuId: i.skuId!, quantity: i.quantity!, unitPrice: i.unitPrice! })),
    }
    try {
      if (editing === 'new') {
        await receiptApi.create(body)
      } else if (editing) {
        await receiptApi.update(editing.id, body)
      }
      message.success('Đã lưu phiếu nhập (Nháp)')
      setEditing(null)
      load()
    } catch (e) {
      message.error(errorText(e))
    }
  }

  const confirm = async (id: string) => {
    try {
      await receiptApi.confirm(id)
      message.success('Đã xác nhận phiếu: ghi sổ cái, tăng tồn kho, tính lại giá vốn')
      load()
    } catch (e) {
      message.error(errorText(e))
    }
  }

  return (
    <Card title="Phiếu nhập kho" extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => open('new')}>Tạo phiếu nhập</Button>}>
      <Space style={{ marginBottom: 16 }}>
        <Select allowClear placeholder="Trạng thái" style={{ width: 170 }} onChange={(s) => { setStatus(s); setPage(0) }}
          options={[{ value: 'DRAFT', label: 'Nháp' }, { value: 'CONFIRMED', label: 'Đã xác nhận' }]} />
      </Space>
      <Table<Receipt> rowKey="id" loading={loading} dataSource={rows} locale={{ emptyText: 'Chưa có phiếu nhập nào' }}
        pagination={{ current: page + 1, pageSize: 20, total, showSizeChanger: false, onChange: (p) => setPage(p - 1) }}
        expandable={{
          expandedRowRender: (r) => (
            <Table size="small" rowKey="skuId" pagination={false} dataSource={r.items}
              columns={[
                { title: 'Mã SKU', dataIndex: 'skuCode' },
                { title: 'Số lượng', dataIndex: 'quantity' },
                { title: 'Đơn giá nhập', dataIndex: 'unitPrice', render: money },
                { title: 'Thành tiền', dataIndex: 'lineTotal', render: money },
              ]} />
          ),
        }}
        columns={[
          { title: 'Mã phiếu', dataIndex: 'code' },
          { title: 'Chi nhánh', dataIndex: 'branchId', render: branchName },
          { title: 'Nhà cung cấp', dataIndex: 'supplierId', render: supplierName },
          { title: 'Tổng tiền', dataIndex: 'totalAmount', render: money },
          { title: 'Trạng thái', dataIndex: 'status', render: (s: ReceiptStatus) => <Tag color={s === 'CONFIRMED' ? 'green' : 'orange'}>{s === 'CONFIRMED' ? 'Đã xác nhận' : 'Nháp'}</Tag> },
          { title: 'Ngày tạo', dataIndex: 'createdAt', render: (d: string) => new Date(d).toLocaleString('vi-VN') },
          {
            title: 'Thao tác',
            render: (_: unknown, r: Receipt) => r.status === 'DRAFT' && (
              <Space>
                <Button size="small" onClick={() => open(r)}>Sửa</Button>
                <Popconfirm title="Xác nhận phiếu? Sau khi xác nhận không sửa được." okText="Xác nhận" cancelText="Hủy" onConfirm={() => confirm(r.id)}>
                  <Button size="small" type="primary">Xác nhận</Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]} />

      <Drawer open={editing !== null} size={680} title={editing === 'new' ? 'Tạo phiếu nhập' : 'Sửa phiếu nhập'} onClose={() => setEditing(null)} destroyOnHidden
        extra={<Button type="primary" onClick={save}>Lưu nháp</Button>}>
        <Form form={form} layout="vertical">
          <Form.Item name="branchId" label="Chi nhánh nhập" rules={[{ required: true, message: 'Chọn chi nhánh' }]}>
            <Select options={branches.filter((b) => b.active).map((b) => ({ value: b.id, label: b.name }))} />
          </Form.Item>
          <Form.Item name="supplierId" label="Nhà cung cấp">
            <Select allowClear options={suppliers.filter((s) => s.active).map((s) => ({ value: s.id, label: s.name }))} />
          </Form.Item>
          <Form.List name="items">
            {(fields, { add, remove }) => (
              <>
                {fields.map(({ key, name, ...rest }) => (
                  <Space key={key} align="start" style={{ display: 'flex' }}>
                    <Form.Item {...rest} name={[name, 'skuId']} rules={[{ required: true, message: 'Chọn SKU' }]} style={{ width: 280 }}><SkuSelect /></Form.Item>
                    <Form.Item {...rest} name={[name, 'quantity']} rules={[{ required: true, type: 'number', min: 1, message: 'SL ≥ 1' }]}>
                      <InputNumber placeholder="Số lượng" min={1} precision={0} style={{ width: 110 }} />
                    </Form.Item>
                    <Form.Item {...rest} name={[name, 'unitPrice']} rules={[{ required: true, type: 'number', min: 0, message: 'Giá ≥ 0' }]}>
                      <InputNumber placeholder="Đơn giá nhập" min={0} style={{ width: 130 }} />
                    </Form.Item>
                    {fields.length > 1 && <MinusCircleOutlined onClick={() => remove(name)} style={{ marginTop: 8 }} />}
                  </Space>
                ))}
                <Button type="dashed" icon={<PlusOutlined />} onClick={() => add({})}>Thêm dòng hàng</Button>
              </>
            )}
          </Form.List>
        </Form>
      </Drawer>
    </Card>
  )
}
