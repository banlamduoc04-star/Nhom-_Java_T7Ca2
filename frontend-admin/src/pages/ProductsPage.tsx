import { useCallback, useEffect, useState } from 'react'
import { App, Button, Card, Divider, Form, Input, InputNumber, Modal, Select, Space, Switch, Table, Tag } from 'antd'
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import { brandApi, categoryApi, productApi, type SkuInput } from '../api/services'
import { errorText } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { Brand, CatalogStatus, Category, ProductSummary } from '../api/types'

/** Quy tac ten san pham cua de bai: chu hoa dau, 3-51 ky tu chu/so/khoang trang khong dau. */
const NAME_RULE = { pattern: /^[A-Z][a-zA-Z0-9\s]{2,50}$/, message: 'Tên phải bắt đầu bằng chữ in hoa, gồm 3–51 ký tự chữ không dấu, số hoặc khoảng trắng' }

interface SkuForm {
  code: string
  color?: string
  size?: string
  barcode?: string
  retailPrice: number
  wholesalePrice?: number
}

interface CreateForm {
  name: string
  description?: string
  categoryId?: string
  brandId?: string
  hasVariants: boolean
  skus: SkuForm[]
}

const money = (v: number | null | undefined) => (v == null ? '' : `${v.toLocaleString('vi-VN')} ₫`)

/** FR-PROD-02, FR-PROD-05: danh sach, tim kiem, loc, phan trang va tao san pham nhieu bien the mot lan. */
export default function ProductsPage() {
  const { message } = App.useApp()
  const { hasRole } = useAuth()
  const canEdit = hasRole('OWNER')

  const [categories, setCategories] = useState<Category[]>([])
  const [brands, setBrands] = useState<Brand[]>([])
  const [rows, setRows] = useState<ProductSummary[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [query, setQuery] = useState<{ keyword?: string; categoryId?: string; brandId?: string; status?: CatalogStatus; page: number }>({ page: 0 })
  const [creating, setCreating] = useState(false)
  const [form] = Form.useForm<CreateForm>()

  useEffect(() => {
    Promise.all([categoryApi.list(), brandApi.list()]).then(([c, b]) => {
      setCategories(c)
      setBrands(b)
    }).catch((e) => message.error(errorText(e)))
  }, [message])

  const load = useCallback(() => {
    setLoading(true)
    productApi.search({ ...query, size: 20 })
      .then((p) => {
        setRows(p.content)
        setTotal(p.totalElements)
      })
      .catch((e) => message.error(errorText(e)))
      .finally(() => setLoading(false))
  }, [query, message])

  useEffect(load, [load])

  const categoryName = (id: string | null) => categories.find((c) => c.id === id)?.name ?? '—'
  const brandName = (id: string | null) => brands.find((b) => b.id === id)?.name ?? '—'

  const openCreate = () => {
    form.setFieldsValue({ name: '', hasVariants: false, skus: [{ code: '', retailPrice: 0 }] })
    setCreating(true)
  }

  const submit = async () => {
    const v = await form.validateFields()
    const skus: SkuInput[] = v.skus.map((s) => ({
      code: s.code,
      barcode: s.barcode || undefined,
      retailPrice: s.retailPrice,
      wholesalePrice: s.wholesalePrice || undefined,
      attributes: Object.fromEntries(Object.entries({ color: s.color, size: s.size }).filter(([, val]) => val)) as Record<string, string>,
    }))
    try {
      await productApi.create({
        name: v.name, description: v.description, categoryId: v.categoryId, brandId: v.brandId,
        hasVariants: v.hasVariants, skus,
      })
      message.success('Đã tạo sản phẩm')
      setCreating(false)
      setQuery((q) => ({ ...q, page: 0 }))
      load()
    } catch (e) {
      message.error(errorText(e))
    }
  }

  return (
    <Card title="Sản phẩm" extra={canEdit && <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Thêm sản phẩm</Button>}>
      <Space wrap style={{ marginBottom: 16 }}>
        <Input.Search allowClear placeholder="Tên, mã SKU hoặc mã vạch" style={{ width: 280 }}
          onSearch={(keyword) => setQuery((q) => ({ ...q, keyword: keyword || undefined, page: 0 }))} />
        <Select allowClear placeholder="Danh mục" style={{ width: 180 }} options={categories.map((c) => ({ value: c.id, label: c.name }))}
          onChange={(categoryId) => setQuery((q) => ({ ...q, categoryId, page: 0 }))} />
        <Select allowClear placeholder="Thương hiệu" style={{ width: 180 }} options={brands.map((b) => ({ value: b.id, label: b.name }))}
          onChange={(brandId) => setQuery((q) => ({ ...q, brandId, page: 0 }))} />
        <Select allowClear placeholder="Trạng thái" style={{ width: 150 }}
          options={[{ value: 'ACTIVE', label: 'Đang bán' }, { value: 'INACTIVE', label: 'Ngừng bán' }]}
          onChange={(status) => setQuery((q) => ({ ...q, status, page: 0 }))} />
      </Space>

      <Table<ProductSummary> rowKey="id" loading={loading} dataSource={rows} locale={{ emptyText: 'Không có sản phẩm nào' }}
        pagination={{ current: query.page + 1, pageSize: 20, total, showSizeChanger: false, onChange: (p) => setQuery((q) => ({ ...q, page: p - 1 })) }}
        columns={[
          { title: 'Tên sản phẩm', dataIndex: 'name', render: (n: string, r) => <Link to={`/products/${r.id}`}>{n}</Link> },
          { title: 'Danh mục', dataIndex: 'categoryId', render: categoryName },
          { title: 'Thương hiệu', dataIndex: 'brandId', render: brandName },
          { title: 'Số SKU', dataIndex: 'skuCount', width: 90 },
          {
            title: 'Giá bán lẻ',
            render: (_: unknown, r) => (r.minRetailPrice === r.maxRetailPrice ? money(r.minRetailPrice) : `${money(r.minRetailPrice)} – ${money(r.maxRetailPrice)}`),
          },
          { title: 'Trạng thái', dataIndex: 'status', render: (s: CatalogStatus) => <Tag color={s === 'ACTIVE' ? 'green' : 'default'}>{s === 'ACTIVE' ? 'Đang bán' : 'Ngừng bán'}</Tag> },
        ]} />

      <Modal open={creating} title="Thêm sản phẩm" width={760} okText="Tạo sản phẩm" cancelText="Hủy" onOk={submit}
        onCancel={() => setCreating(false)} destroyOnHidden>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Tên sản phẩm" rules={[{ required: true, message: 'Nhập tên sản phẩm' }, NAME_RULE]}><Input autoFocus /></Form.Item>
          <Space style={{ display: 'flex' }} align="start">
            <Form.Item name="categoryId" label="Danh mục" style={{ width: 230 }}>
              <Select allowClear options={categories.map((c) => ({ value: c.id, label: c.name }))} />
            </Form.Item>
            <Form.Item name="brandId" label="Thương hiệu" style={{ width: 230 }}>
              <Select allowClear options={brands.map((b) => ({ value: b.id, label: b.name }))} />
            </Form.Item>
            <Form.Item name="hasVariants" label="Có nhiều biến thể" valuePropName="checked"><Switch /></Form.Item>
          </Space>
          <Form.Item name="description" label="Mô tả"><Input.TextArea rows={2} maxLength={2000} /></Form.Item>

          <Divider>Biến thể (SKU)</Divider>
          <Form.Item noStyle shouldUpdate={(a, b) => a.hasVariants !== b.hasVariants}>
            {({ getFieldValue }) => (
              <Form.List name="skus">
                {(fields, { add, remove }) => (
                  <>
                    {fields.map(({ key, name, ...rest }) => (
                      <Space key={key} align="start" wrap style={{ display: 'flex', marginBottom: 8 }}>
                        <Form.Item {...rest} name={[name, 'code']} rules={[{ required: true, message: 'Mã SKU' }]}><Input placeholder="Mã SKU" style={{ width: 130 }} /></Form.Item>
                        <Form.Item {...rest} name={[name, 'color']}><Input placeholder="Màu" style={{ width: 90 }} /></Form.Item>
                        <Form.Item {...rest} name={[name, 'size']}><Input placeholder="Size" style={{ width: 80 }} /></Form.Item>
                        <Form.Item {...rest} name={[name, 'barcode']}><Input placeholder="Mã vạch (tùy chọn)" style={{ width: 150 }} /></Form.Item>
                        <Form.Item {...rest} name={[name, 'retailPrice']} rules={[{ required: true, type: 'number', min: 0.01, message: 'Giá lẻ > 0' }]}>
                          <InputNumber placeholder="Giá lẻ" min={0} style={{ width: 110 }} />
                        </Form.Item>
                        <Form.Item {...rest} name={[name, 'wholesalePrice']}><InputNumber placeholder="Giá sỉ" min={0} style={{ width: 110 }} /></Form.Item>
                        {fields.length > 1 && <MinusCircleOutlined onClick={() => remove(name)} style={{ marginTop: 8 }} />}
                      </Space>
                    ))}
                    {getFieldValue('hasVariants') && (
                      <Button type="dashed" icon={<PlusOutlined />} onClick={() => add({ code: '', retailPrice: 0 })}>Thêm biến thể</Button>
                    )}
                  </>
                )}
              </Form.List>
            )}
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
