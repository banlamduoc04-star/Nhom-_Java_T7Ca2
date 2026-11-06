import { useCallback, useEffect, useState } from 'react'
import { App, Button, Card, Descriptions, Form, Input, InputNumber, Modal, Popconfirm, Space, Table, Tag } from 'antd'
import { ArrowLeftOutlined, BarcodeOutlined, PlusOutlined, PrinterOutlined } from '@ant-design/icons'
import { Link, useParams } from 'react-router-dom'
import { productApi, skuApi } from '../../api/services'
import { errorText } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import BarcodeLabel, { printLabel } from '../../components/BarcodeLabel'
import type { ProductDetail, Sku } from '../../api/types'

const money = (v: number | null | undefined) => (v == null ? '' : `${v.toLocaleString('vi-VN')} ₫`)

/** Chi tiet san pham: SKU, gia, sinh ma vach EAN-13, in tem, ngung ban. */
export default function ProductDetailPage() {
  const { id = '' } = useParams()
  const { message } = App.useApp()
  const { hasRole } = useAuth()
  const isOwner = hasRole('OWNER')
  const canBarcode = hasRole('OWNER', 'STAFF')

  const [product, setProduct] = useState<ProductDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [pricing, setPricing] = useState<Sku | null>(null)
  const [labelSku, setLabelSku] = useState<Sku | null>(null)
  const [addingSku, setAddingSku] = useState(false)
  const [priceForm] = Form.useForm<{ retailPrice: number; wholesalePrice?: number }>()
  const [skuForm] = Form.useForm<{ code: string; color?: string; size?: string; retailPrice: number; wholesalePrice?: number }>()

  const reload = useCallback(() => {
    setLoading(true)
    productApi.get(id).then(setProduct).catch((e) => message.error(errorText(e))).finally(() => setLoading(false))
  }, [id, message])

  useEffect(reload, [reload])

  const run = async (action: () => Promise<unknown>, success: string) => {
    try {
      await action()
      message.success(success)
      reload()
    } catch (e) {
      message.error(errorText(e))
    }
  }

  const savePrice = async () => {
    const v = await priceForm.validateFields()
    if (pricing) {
      await run(() => skuApi.changePrices(pricing.id, { retailPrice: v.retailPrice, wholesalePrice: v.wholesalePrice || null }), 'Đã đổi giá')
      setPricing(null)
    }
  }

  const saveSku = async () => {
    const v = await skuForm.validateFields()
    const attributes = Object.fromEntries(Object.entries({ color: v.color, size: v.size }).filter(([, val]) => val)) as Record<string, string>
    await run(() => productApi.addSku(id, { code: v.code, attributes, retailPrice: v.retailPrice, wholesalePrice: v.wholesalePrice || undefined }), 'Đã thêm SKU')
    setAddingSku(false)
  }

  if (!product) {
    return <Card loading={loading}>Không tìm thấy sản phẩm</Card>
  }

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Link to="/products"><Button icon={<ArrowLeftOutlined />}>Quay lại danh sách</Button></Link>
      <Card title={product.name} loading={loading}
        extra={isOwner && (
          <Popconfirm title={product.status === 'ACTIVE' ? 'Ngừng bán sản phẩm này?' : 'Bán lại sản phẩm này?'} okText="Đồng ý" cancelText="Hủy"
            onConfirm={() => run(() => productApi.setStatus(id, product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'), 'Đã cập nhật trạng thái')}>
            <Button>{product.status === 'ACTIVE' ? 'Ngừng bán' : 'Bán lại'}</Button>
          </Popconfirm>
        )}>
        <Descriptions column={{ xs: 1, md: 3 }}>
          <Descriptions.Item label="Loại">{product.hasVariants ? 'Nhiều biến thể' : 'Sản phẩm đơn'}</Descriptions.Item>
          <Descriptions.Item label="Trạng thái"><Tag color={product.status === 'ACTIVE' ? 'green' : 'default'}>{product.status === 'ACTIVE' ? 'Đang bán' : 'Ngừng bán'}</Tag></Descriptions.Item>
          <Descriptions.Item label="Mô tả">{product.description ?? '—'}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="Biến thể (SKU)"
        extra={isOwner && product.hasVariants && <Button type="primary" icon={<PlusOutlined />} onClick={() => { skuForm.resetFields(); setAddingSku(true) }}>Thêm SKU</Button>}>
        <Table<Sku> rowKey="id" dataSource={product.skus} pagination={false} scroll={{ x: 'max-content' }}
          columns={[
            { title: 'Mã SKU', dataIndex: 'code', render: (c: string) => <strong>{c}</strong> },
            { title: 'Thuộc tính', dataIndex: 'attributes', render: (a: Record<string, string>) => Object.entries(a).map(([k, v]) => `${k}: ${v}`).join(', ') },
            { title: 'Mã vạch', dataIndex: 'barcode', render: (b: string | null) => b ?? '—' },
            { title: 'Giá lẻ', dataIndex: 'retailPrice', render: money },
            { title: 'Giá sỉ', dataIndex: 'wholesalePrice', render: money },
            { title: 'Giá vốn BQ', dataIndex: 'avgCost', render: money },
            { title: 'Trạng thái', dataIndex: 'status', render: (s: string) => <Tag color={s === 'ACTIVE' ? 'green' : 'default'}>{s === 'ACTIVE' ? 'Đang bán' : 'Ngừng bán'}</Tag> },
            {
              title: 'Thao tác',
              render: (_: unknown, sku: Sku) => (
                <Space wrap>
                  {isOwner && <Button size="small" onClick={() => { priceForm.setFieldsValue({ retailPrice: sku.retailPrice, wholesalePrice: sku.wholesalePrice ?? undefined }); setPricing(sku) }}>Đổi giá</Button>}
                  {canBarcode && !sku.barcode && <Button size="small" icon={<BarcodeOutlined />} onClick={() => run(() => skuApi.generateBarcode(sku.id), 'Đã sinh mã vạch EAN-13')}>Sinh mã vạch</Button>}
                  {sku.barcode && <Button size="small" icon={<PrinterOutlined />} onClick={() => setLabelSku(sku)}>In tem</Button>}
                  {isOwner && (
                    <Button size="small" onClick={() => run(() => skuApi.update(sku.id, { status: sku.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' }), 'Đã cập nhật SKU')}>
                      {sku.status === 'ACTIVE' ? 'Ngừng bán' : 'Bán lại'}
                    </Button>
                  )}
                </Space>
              ),
            },
          ]} />
      </Card>

      <Modal open={pricing !== null} title={`Đổi giá ${pricing?.code ?? ''}`} okText="Lưu" cancelText="Hủy" onOk={savePrice} onCancel={() => setPricing(null)} destroyOnHidden>
        <Form form={priceForm} layout="vertical">
          <Form.Item name="retailPrice" label="Giá bán lẻ (> 0)" rules={[{ required: true, type: 'number', min: 0.01, message: 'Giá lẻ phải lớn hơn 0' }]}><InputNumber style={{ width: '100%' }} min={0} /></Form.Item>
          <Form.Item name="wholesalePrice" label="Giá bán sỉ (để trống nếu không có)"><InputNumber style={{ width: '100%' }} min={0} /></Form.Item>
        </Form>
      </Modal>

      <Modal open={addingSku} title="Thêm SKU" okText="Lưu" cancelText="Hủy" onOk={saveSku} onCancel={() => setAddingSku(false)} destroyOnHidden>
        <Form form={skuForm} layout="vertical">
          <Form.Item name="code" label="Mã SKU (duy nhất)" rules={[{ required: true, message: 'Nhập mã SKU' }, { max: 64 }]}><Input autoFocus /></Form.Item>
          <Space>
            <Form.Item name="color" label="Màu"><Input /></Form.Item>
            <Form.Item name="size" label="Size"><Input /></Form.Item>
          </Space>
          <Form.Item name="retailPrice" label="Giá bán lẻ" rules={[{ required: true, type: 'number', min: 0.01, message: 'Giá lẻ phải lớn hơn 0' }]}><InputNumber style={{ width: '100%' }} min={0} /></Form.Item>
          <Form.Item name="wholesalePrice" label="Giá bán sỉ"><InputNumber style={{ width: '100%' }} min={0} /></Form.Item>
        </Form>
      </Modal>

      <Modal open={labelSku !== null} title="Tem mã vạch" okText="In tem" cancelText="Đóng" onCancel={() => setLabelSku(null)}
        onOk={() => printLabel('barcode-print')} destroyOnHidden>
        {labelSku?.barcode && (
          <div id="barcode-print"><BarcodeLabel value={labelSku.barcode} label={`${product.name} — ${labelSku.code}`} /></div>
        )}
      </Modal>
    </Space>
  )
}
